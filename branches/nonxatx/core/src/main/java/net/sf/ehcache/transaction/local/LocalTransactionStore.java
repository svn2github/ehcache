package net.sf.ehcache.transaction.local;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.AbstractStore;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.util.LargeSet;
import net.sf.ehcache.util.SetWrapperList;
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
            Object key = element.getObjectKey();
            Element oldElement = underlyingStore.getQuiet(key);

            if (oldElement == null) {
                SoftLock softLock = softLockFactory.createSoftLock(getCurrentTransactionContext().getTransactionId(), key, element, oldElement);
                softLock.lock();
                Element newElement = createElement(key, softLock);
                oldElement = underlyingStore.putIfAbsent(newElement);
                if (oldElement == null) {
                    // CAS succeeded, soft lock is in store, job done.
                    getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                    LOG.debug("put: cache [{}] key [{}] was not in, soft lock inserted", cacheName, key);
                    return true;
                } else {
                    // CAS failed, something with that key may now be in store, restart.
                    softLock.unlock();
                    LOG.debug("put: cache [{}] key [{}] was not in, soft lock insertion failed, retrying...", cacheName, key);
                    continue;
                }
            } else {
                Object value = oldElement.getObjectValue();
                if (value instanceof SoftLock) {
                    SoftLock softLock = (SoftLock) value;

                    if (softLock.isExpired()) {
                        underlyingStore.replace(oldElement, softLock.getOldElement());
                        // expired soft lock cleaned up or not, restart.
                        LOG.debug("put: cache [{}] key [{}] guarded by expired soft lock, cleaned up soft lock", cacheName, key);
                        continue;
                    }

                    if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        softLock.setNewElement(element);
                        underlyingStore.put(oldElement);
                        getCurrentTransactionContext().updateSoftLock(cacheName, softLock);

                        LOG.debug("put: cache [{}] key [{}] soft locked in current transaction, replaced old value with new one under soft lock", cacheName, key);
                        // replaced old value with new one under soft lock, job done.
                        return false;
                    } else {
                        LOG.debug("put: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                        try {
                            boolean locked = softLock.tryLock(timeBeforeTimeout());
                            if (!locked) {
                                LOG.debug("put: cache [{}] key [{}] soft locked in foreign transaction and not released before current transaction timeout", cacheName, key);
                                throw new DeadLockException("deadlock detected");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        LOG.debug("put: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...", cacheName, key);
                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        continue;
                    }
                } else {
                    SoftLock softLock = softLockFactory.createSoftLock(getCurrentTransactionContext().getTransactionId(), key, element, oldElement);
                    softLock.lock();
                    Element newElement = createElement(key, softLock);
                    boolean replaced = underlyingStore.replace(oldElement, newElement);
                    if (replaced) {
                        // CAS succeeded, value replaced with soft lock, job done.
                        getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                        LOG.debug("put: cache [{}] key [{}] was in, replaced with soft lock", cacheName, key);
                        return false;
                    } else {
                        // CAS failed, something else with that key is now in store or the key disappeared, restart.
                        softLock.unlock();
                        LOG.debug("put: cache [{}] key [{}] was in, replacement by soft lock failed, retrying... ", cacheName, key);
                        continue;
                    }
                }
            }

        } // while
    }

    public Element getQuiet(Object key) {
        Element oldElement = underlyingStore.getQuiet(key);
        if (oldElement == null) {
            LOG.debug("getQuiet: cache [{}] key [{}] is not present", cacheName, key);
            return null;
        }

        Object value = oldElement.getObjectValue();
        if (value instanceof SoftLock) {
            SoftLock softLock = (SoftLock) value;
            LOG.debug("getQuiet: cache [{}] key [{}] soft locked, returning soft locked element", cacheName, key);
            return softLock.getElement();
        } else {
            LOG.debug("getQuiet: cache [{}] key [{}] not soft locked, returning underlying element", cacheName, key);
            return oldElement;
        }
    }

    public Element get(Object key) {
        Element oldElement = underlyingStore.get(key);
        if (oldElement == null) {
            LOG.debug("get: cache [{}] key [{}] is not present", cacheName, key);
            return null;
        }

        Object value = oldElement.getObjectValue();
        if (value instanceof SoftLock) {
            SoftLock softLock = (SoftLock) value;
            LOG.debug("get: cache [{}] key [{}] soft locked, returning soft locked element", cacheName, key);
            return softLock.getElement();
        } else {
            LOG.debug("get: cache [{}] key [{}] not soft locked, returning underlying element", cacheName, key);
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
                    LOG.debug("remove: cache [{}] key [{}] was not in, soft lock inserted", cacheName, key);
                    return null;
                } else {
                    // CAS failed, something with that key may now be in store, restart.
                    softLock.unlock();
                    LOG.debug("remove: cache [{}] key [{}] was not in, soft lock insertion failed, retrying...", cacheName, key);
                    continue;
                }
            } else {
                Object value = oldElement.getObjectValue();
                if (value instanceof SoftLock) {
                    SoftLock softLock = (SoftLock) value;

                    if (softLock.isExpired()) {
                        underlyingStore.replace(oldElement, softLock.getOldElement());
                        // expired soft lock cleaned up or not, restart.
                        LOG.debug("remove: cache [{}] key [{}] guarded by expired soft lock, cleaned up soft lock", cacheName, key);
                        continue;
                    }

                    if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        Element removed = softLock.getNewElement();
                        softLock.setNewElement(null);
                        underlyingStore.put(oldElement);
                        getCurrentTransactionContext().updateSoftLock(cacheName, softLock);

                        // replaced old value with new one under soft lock, job done.
                        LOG.debug("remove: cache [{}] key [{}] soft locked in current transaction, replaced old value with new one under soft lock", cacheName, key);
                        return removed;
                    } else {
                        try {
                            LOG.debug("remove: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                            boolean locked = softLock.tryLock(timeBeforeTimeout());
                            if (!locked) {
                                LOG.debug("remove: cache [{}] key [{}] soft locked in foreign transaction and not released before current transaction timeout", cacheName, key);
                                throw new DeadLockException("deadlock detected");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        LOG.debug("remove: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...", cacheName, key);
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
                        LOG.debug("remove: cache [{}] key [{}] was in, replaced with soft lock", cacheName, key);
                        return oldElement;
                    } else {
                        // CAS failed, something else with that key is now in store or the key disappeared, restart.
                        softLock.unlock();
                        LOG.debug("remove: cache [{}] key [{}] was in, replacement by soft lock failed, retrying...", cacheName, key);
                        continue;
                    }
                }
            }

        } // while
    }

    public List getKeys() {
        Set<Object> keys = new LargeSet<Object>() {
            @Override
            public int sourceSize() {
                return underlyingStore.getSize();
            }

            @Override
            public Iterator<Object> sourceIterator() {
                @SuppressWarnings("unchecked")
                Iterator<Object> iterator = underlyingStore.getKeys().iterator();
                return iterator;
            }
        };

        //todo the following is specific to read-committed isolation
        Set<Object> foreignTransactionNewKeys = new HashSet<Object>();
        foreignTransactionNewKeys.addAll(softLockFactory.getNewKeys());
        foreignTransactionNewKeys.removeAll(getCurrentTransactionContext().getNewKeys(cacheName));

        keys.removeAll(getCurrentTransactionContext().getRemovedKeys(cacheName));
        keys.removeAll(foreignTransactionNewKeys);

        return new SetWrapperList(keys);
    }

    public int getSize() {
        //todo the following is specific to read-committed isolation
        int sizeModifier = 0;
        sizeModifier -= softLockFactory.getNewKeys().size();
        sizeModifier += getCurrentTransactionContext().getNewKeys(cacheName).size();
        sizeModifier -= getCurrentTransactionContext().getRemovedKeys(cacheName).size();
        return underlyingStore.getSize() + sizeModifier;
    }

    public int getTerracottaClusteredSize() {
        return getSize();
    }

    public boolean containsKey(Object key) {
        return getKeys().contains(key);
    }


    // todo rework all these transactional methods

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return underlyingStore.putWithWriter(element, writerManager);
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

            if (softLock.getOldElement() == null) {
                softLockFactory.clearNewKey(softLock.getKey());
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

            if (softLock.getOldElement() == null) {
                softLockFactory.clearNewKey(softLock.getKey());
            }
        }
    }
    
}
