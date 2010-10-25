package net.sf.ehcache.transaction.local;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.AbstractStore;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class LocalTransactionStore extends AbstractStore {

    private static final Logger LOG = LoggerFactory.getLogger(LocalTransactionStore.class.getName());

    private final TransactionController transactionController;
    private final SoftLockFactory softLockFactory;
    private final String cacheName;
    private final Store underlyingStore;

    public LocalTransactionStore(TransactionController transactionController, SoftLockFactory softLockFactory, String cacheName, Store store) {
        this.transactionController = transactionController;
        this.softLockFactory = softLockFactory;
        this.cacheName = cacheName;
        this.underlyingStore = store;
    }


    private TransactionContext getCurrentTransactionContext() {
        TransactionContext currentTransactionContext = transactionController.getCurrentTransactionContext();
        if (currentTransactionContext == null) {
            throw new TransactionException("transaction not started");
        }
        return currentTransactionContext;
    }

    private long timeBeforeTimeout() {
        return Math.max(0, getCurrentTransactionContext().getExpirationTimestamp() - System.currentTimeMillis());
    }

    private Element createElement(Object key, SoftLock softLock) {
        Element element = new Element(key, softLock);
        element.setEternal(true);
        return element;
    }

    /* transactional methods */

    public boolean put(Element element) throws CacheException {
        while (true) {
            Element oldElement = underlyingStore.getQuiet(element.getKey());

            if (oldElement == null) {
                SoftLock softLock = softLockFactory.createSoftLock(getCurrentTransactionContext().getTransactionId(), element.getObjectKey(), element, oldElement);
                softLock.lock();
                Element newElement = createElement(element.getObjectKey(), softLock);
                oldElement = underlyingStore.putIfAbsent(newElement);
                if (oldElement == null) {
                    // CAS succeeded, soft lock is in store, job done.
                    getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                    LOG.debug("put: cache [{}] key [{}] was not in, soft lock inserted", cacheName, element.getKey());
                    return true;
                } else {
                    // CAS failed, something with that key may now be in store, restart.
                    softLock.unlock();
                    LOG.debug("put: cache [{}] key [{}] was not in, soft lock insertion failed, retrying...", cacheName, element.getKey());
                    continue;
                }
            } else {
                Object value = oldElement.getObjectValue();
                if (value instanceof SoftLock) {
                    SoftLock softLock = (SoftLock) value;

                    if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        softLock.setNewElement(element);
                        underlyingStore.put(oldElement);

                        LOG.debug("put: cache [{}] key [{}] soft locked in current transaction, replaced old value with new one under soft lock", cacheName, element.getKey());
                        // replaced old value with new one under soft lock, job done.
                        return false;
                    } else {
                        LOG.debug("put: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to die...", new Object[] {cacheName, element.getKey(), timeBeforeTimeout()});
                        try {
                            boolean locked = softLock.tryLock(timeBeforeTimeout());
                            if (!locked) {
                                LOG.debug("put: cache [{}] key [{}] soft locked in foreign transaction and not released before current transaction timeout", cacheName, element.getKey());
                                throw new DeadLockException("deadlock detected");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        LOG.debug("put: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...", cacheName, element.getKey());
                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        continue;
                    }
                } else {
                    SoftLock softLock = softLockFactory.createSoftLock(getCurrentTransactionContext().getTransactionId(), element.getObjectKey(), element, oldElement);
                    softLock.lock();
                    Element newElement = createElement(element.getObjectKey(), softLock);
                    boolean replaced = underlyingStore.replace(oldElement, newElement);
                    if (replaced) {
                        // CAS succeeded, value replaced with soft lock, job done.
                        getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                        LOG.debug("put: cache [{}] key [{}] was in, replaced with soft lock", cacheName, element.getKey());
                        return false;
                    } else {
                        // CAS failed, something else with that key is now in store or the key disappeared, restart.
                        softLock.unlock();
                        LOG.debug("put: cache [{}] key [{}] was in, soft lock insertion failed, retrying...", cacheName, element.getKey());
                        continue;
                    }
                }
            }

        } // while
    }

    public Element getQuiet(Object key) {
        Element oldElement = underlyingStore.getQuiet(key);
        if (oldElement == null) {
            return null;
        }

        Object value = oldElement.getObjectValue();
        if (value instanceof SoftLock) {
            SoftLock softLock = (SoftLock) value;
            return softLock.getElement();
        } else {
            return oldElement;
        }
    }

    public Element get(Object key) {
        Element oldElement = underlyingStore.get(key);
        if (oldElement == null) {
            return null;
        }

        Object value = oldElement.getObjectValue();
        if (value instanceof SoftLock) {
            SoftLock softLock = (SoftLock) value;
            return softLock.getElement();
        } else {
            return oldElement;
        }
    }

    public Element remove(Object key) {
        while (true) {
            Element oldElement = underlyingStore.getQuiet(key);

            if (oldElement == null) {
                SoftLock softLock = softLockFactory.createSoftLock(getCurrentTransactionContext().getTransactionId(), key, null, oldElement);
                softLock.lock();
                Element newElement = createElement(key, softLock);
                oldElement = underlyingStore.putIfAbsent(newElement);
                if (oldElement == null) {
                    // CAS succeeded, value is in store, job done.
                    getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                    return null;
                } else {
                    // CAS failed, something with that key may now be in store, restart.
                    softLock.unlock();
                    continue;
                }
            } else {
                Object value = oldElement.getObjectValue();
                if (value instanceof SoftLock) {
                    SoftLock softLock = (SoftLock) value;

                    if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        softLock.setNewElement(null);
                        underlyingStore.put(oldElement);

                        // replaced old value with new one under soft lock, job done.
                        return softLock.getOldElement();
                    } else {
                        try {
                            boolean locked = softLock.tryLock(timeBeforeTimeout());
                            if (!locked) {
                                throw new DeadLockException("deadlock detected");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        continue;
                    }
                } else {
                    SoftLock softLock = softLockFactory.createSoftLock(getCurrentTransactionContext().getTransactionId(), key, null, oldElement);
                    softLock.lock();
                    Element newElement = createElement(key, softLock);
                    boolean replaced = underlyingStore.replace(oldElement, newElement);
                    if (replaced) {
                        // CAS succeeded, value replaced with soft lock, job done.
                        getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                        return oldElement;
                    } else {
                        // CAS failed, something else with that key is now in store or the key disappeared, restart.
                        softLock.unlock();
                        continue;
                    }
                }
            }

        } // while
    }

    // todo rework all these transactional methods

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return underlyingStore.putWithWriter(element, writerManager);
    }

    public List getKeys() {
        return underlyingStore.getKeys();
    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return underlyingStore.removeWithWriter(key, writerManager);
    }

    public void removeAll() throws CacheException {
        underlyingStore.removeAll();
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        return underlyingStore.putIfAbsent(element);
    }

    public Element removeElement(Element element) throws NullPointerException {
        return underlyingStore.removeElement(element);
    }

    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        return underlyingStore.replace(old, element);
    }

    public Element replace(Element element) throws NullPointerException {
        return underlyingStore.replace(element);
    }

    public int getSize() {
        return underlyingStore.getSize();
    }

    public int getTerracottaClusteredSize() {
        return underlyingStore.getTerracottaClusteredSize();
    }

    public boolean containsKey(Object key) {
        return underlyingStore.containsKey(key);
    }

    /* non-transactional methods */

    public int getInMemorySize() {
        return underlyingStore.getInMemorySize();
    }

    public int getOffHeapSize() {
        return underlyingStore.getOffHeapSize();
    }

    public int getOnDiskSize() {
        return underlyingStore.getOnDiskSize();
    }

    public long getInMemorySizeInBytes() {
        return underlyingStore.getInMemorySizeInBytes();
    }

    public long getOffHeapSizeInBytes() {
        return underlyingStore.getOffHeapSizeInBytes();
    }

    public long getOnDiskSizeInBytes() {
        return underlyingStore.getOnDiskSizeInBytes();
    }

    public boolean containsKeyOnDisk(Object key) {
        return underlyingStore.containsKeyOnDisk(key);
    }

    public boolean containsKeyOffHeap(Object key) {
        return underlyingStore.containsKeyOffHeap(key);
    }

    public boolean containsKeyInMemory(Object key) {
        return underlyingStore.containsKeyInMemory(key);
    }

    public void dispose() {
        underlyingStore.dispose();
    }

    public Status getStatus() {
        return underlyingStore.getStatus();
    }

    public void expireElements() {
        underlyingStore.expireElements();
    }

    public void flush() throws IOException {
        underlyingStore.flush();
    }

    public boolean bufferFull() {
        return underlyingStore.bufferFull();
    }

    public Policy getInMemoryEvictionPolicy() {
        return underlyingStore.getInMemoryEvictionPolicy();
    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        underlyingStore.setInMemoryEvictionPolicy(policy);
    }

    public Object getInternalContext() {
        return underlyingStore.getInternalContext();
    }

    public Object getMBean() {
        return underlyingStore.getMBean();
    }

    @Override
    public void setNodeCoherent(boolean coherent) {
        underlyingStore.setNodeCoherent(coherent);
    }

    @Override
    public void waitUntilClusterCoherent() {
        underlyingStore.waitUntilClusterCoherent();
    }

    public void commit(List<SoftLock> softLocks) {
        for (SoftLock softLock : softLocks) {
            Element newElement = softLock.getNewElement();
            if (newElement != null) {
                underlyingStore.put(newElement);
            } else {
                underlyingStore.remove(softLock.getKey());
            }
        }
    }

    public void rollback(List<SoftLock> softLocks) {
        for (SoftLock softLock : softLocks) {
            Element oldElement = softLock.getOldElement();
            if (oldElement != null) {
                underlyingStore.put(oldElement);
            } else {
                underlyingStore.remove(softLock.getKey());
            }
        }
    }
}
