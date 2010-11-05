package net.sf.ehcache.transaction.local;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.AbstractStore;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.compound.CopyStrategy;
import net.sf.ehcache.util.LargeSet;
import net.sf.ehcache.util.SetWrapperList;
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private final Ehcache cache;
    private final String cacheName;
    private final Store underlyingStore;
    private final CopyStrategy copyStrategy;
    private final ElementValueComparator comparator;


    public LocalTransactionStore(TransactionController transactionController, SoftLockFactory softLockFactory, Ehcache cache, Store store, CopyStrategy copyStrategy) {
        this.transactionController = transactionController;
        this.softLockFactory = softLockFactory;
        this.cache = cache;
        this.comparator = cache.getCacheConfiguration().getElementValueComparatorConfiguration().getElementComparatorInstance();
        this.cacheName = cache.getName();
        this.underlyingStore = store;
        this.copyStrategy = copyStrategy;
    }


    private TransactionContext getCurrentTransactionContext() {
        TransactionContext currentTransactionContext = transactionController.getCurrentTransactionContext();
        if (currentTransactionContext == null) {
            throw new TransactionException("transaction not started");
        }
        return currentTransactionContext;
    }

    private void assertNotTimedOut() {
        if (getCurrentTransactionContext().timedOut()) {
            throw new TransactionTimeoutException("transaction [" + getCurrentTransactionContext().getTransactionId() + "] timed out");
        }
        if (Thread.interrupted()) {
            throw new TransactionInterruptedException("transaction [" + getCurrentTransactionContext().getTransactionId() + "] interrupted");
        }
    }

    private long timeBeforeTimeout() {
        return Math.max(0, getCurrentTransactionContext().getExpirationTimestamp() - System.currentTimeMillis());
    }

    private Element createElement(Object key, SoftLock softLock) {
        Element element = new Element(key, softLock);
        element.setEternal(true);
        return element;
    }

    private Element copyElement(Element element) {
        return copyStrategy.copy(element);
    }

    private boolean cleanupExpiredSoftLock(Element oldElement, SoftLock softLock) {
        if (softLock.isExpired()) {
            Element frozenElement = softLock.getFrozenElement();
            if (frozenElement != null) {
                underlyingStore.replace(oldElement, frozenElement, comparator);
            } else {
                underlyingStore.removeElement(oldElement, comparator);
            }
            return true;
        }
        return false;
    }

    /* transactional methods */

    public boolean put(Element element) throws CacheException {
        if (element == null) {
            return true;
        }

        element = copyElement(element);
        while (true) {
            assertNotTimedOut();

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

                    if (cleanupExpiredSoftLock(oldElement, softLock)) {
                        LOG.info("put: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}", new Object[] {cacheName, key, softLock});
                        continue;
                    }

                    if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        softLock.updateElement(element);
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
                                throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "] between current transaction [" +
                                        getCurrentTransactionContext().getTransactionId() + "] and foreign transaction [" + softLock.getTransactionID() + "]");
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
                    boolean replaced = underlyingStore.replace(oldElement, newElement, comparator);
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
        if (key == null) {
            return null;
        }

        while (true) {
            assertNotTimedOut();

            Element oldElement = underlyingStore.getQuiet(key);
            if (oldElement == null) {
                LOG.debug("getQuiet: cache [{}] key [{}] is not present", cacheName, key);
                return null;
            }

            Object value = oldElement.getObjectValue();
            if (value instanceof SoftLock) {
                SoftLock softLock = (SoftLock) value;
                if (cleanupExpiredSoftLock(oldElement, softLock)) {
                    LOG.info("getQuiet: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}", new Object[] {cacheName, key, softLock});
                    continue;
                }

                LOG.debug("getQuiet: cache [{}] key [{}] soft locked, returning soft locked element", cacheName, key);
                return copyElement(softLock.getElement(getCurrentTransactionContext().getTransactionId()));
            } else {
                LOG.debug("getQuiet: cache [{}] key [{}] not soft locked, returning underlying element", cacheName, key);
                return copyElement(oldElement);
            }
        }
    }

    public Element get(Object key) {
        if (key == null) {
            return null;
        }

        while (true) {
            assertNotTimedOut();

            Element oldElement = underlyingStore.get(key);
            if (oldElement == null) {
                LOG.debug("get: cache [{}] key [{}] is not present", cacheName, key);
                return null;
            }

            Object value = oldElement.getObjectValue();
            if (value instanceof SoftLock) {
                SoftLock softLock = (SoftLock) value;
                if (cleanupExpiredSoftLock(oldElement, softLock)) {
                    LOG.info("get: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}", new Object[] {cacheName, key, softLock});
                    continue;
                }

                LOG.debug("get: cache [{}] key [{}] soft locked, returning soft locked element", cacheName, key);
                return copyElement(softLock.getElement(getCurrentTransactionContext().getTransactionId()));
            } else {
                LOG.debug("get: cache [{}] key [{}] not soft locked, returning underlying element", cacheName, key);
                return copyElement(oldElement);
            }
        }
    }

    public Element remove(Object key) {
        if (key == null) {
            return null;
        }

        while (true) {
            assertNotTimedOut();

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

                    if (cleanupExpiredSoftLock(oldElement, softLock)) {
                        LOG.info("remove: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}", new Object[] {cacheName, key, softLock});
                        continue;
                    }

                    if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        Element removed = softLock.updateElement(null);
                        underlyingStore.put(oldElement);
                        getCurrentTransactionContext().updateSoftLock(cacheName, softLock);

                        // replaced old value with new one under soft lock, job done.
                        LOG.debug("remove: cache [{}] key [{}] soft locked in current transaction, replaced old value with new one under soft lock", cacheName, key);
                        return copyElement(removed);
                    } else {
                        try {
                            LOG.debug("remove: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                            boolean locked = softLock.tryLock(timeBeforeTimeout());
                            if (!locked) {
                                LOG.debug("remove: cache [{}] key [{}] soft locked in foreign transaction and not released before current transaction timeout", cacheName, key);
                                throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "] between current transaction [" +
                                        getCurrentTransactionContext().getTransactionId() + "] and foreign transaction [" + softLock.getTransactionID() + "]");
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
                    boolean replaced = underlyingStore.replace(oldElement, newElement, comparator);
                    if (replaced) {
                        // CAS succeeded, value replaced with soft lock, job done.
                        getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                        LOG.debug("remove: cache [{}] key [{}] was in, replaced with soft lock", cacheName, key);
                        return copyElement(oldElement);
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
        assertNotTimedOut();

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

        keys.removeAll(softLockFactory.getKeysInvisibleInContext(getCurrentTransactionContext()));

        return new SetWrapperList(keys);
    }

    public int getSize() {
        assertNotTimedOut();

        int sizeModifier = 0;
        sizeModifier -= softLockFactory.getKeysInvisibleInContext(getCurrentTransactionContext()).size();
        return underlyingStore.getSize() + sizeModifier;
    }

    public int getTerracottaClusteredSize() {
        assertNotTimedOut();

        return getSize();
    }

    public boolean containsKey(Object key) {
        assertNotTimedOut();

        return getKeys().contains(key);
    }

    public void removeAll() throws CacheException {
        assertNotTimedOut();

        List keys = getKeys();
        for (Object key : keys) {
            remove(key);
        }
    }

    public boolean putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        if (element == null) {
            return true;
        }
        assertNotTimedOut();

        boolean put = put(element);
        getCurrentTransactionContext().addListener(new TransactionListener() {
            public void beforeCommit() {
                if (writerManager != null) {
                    writerManager.put(element);
                } else {
                    cache.getWriterManager().put(element);
                }
            }
            public void afterCommit() {
            }
            public void afterRollback() {
            }
        });
        return put;
    }

    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        if (key == null) {
            return null;
        }
        assertNotTimedOut();

        Element removed = remove(key);
        final CacheEntry cacheEntry = new CacheEntry(key, getQuiet(key));
        getCurrentTransactionContext().addListener(new TransactionListener() {
            public void beforeCommit() {
                if (writerManager != null) {
                    writerManager.remove(cacheEntry);
                } else {
                    cache.getWriterManager().remove(cacheEntry);
                }
            }
            public void afterCommit() {
            }
            public void afterRollback() {
            }
        });
        return removed;
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        if (element == null) {
            throw new NullPointerException("element cannot be null");
        }
        if (element.getObjectKey() == null) {
            throw new NullPointerException("element key cannot be null");
        }

        element = copyElement(element);
        while (true) {
            assertNotTimedOut();

            Object key = element.getObjectKey();
            Element oldElement = underlyingStore.getQuiet(key);

            if (oldElement == null || !(oldElement.getObjectValue() instanceof SoftLock)) {
                SoftLock softLock = softLockFactory.createSoftLock(getCurrentTransactionContext().getTransactionId(), key, element, oldElement);
                softLock.lock();
                Element newElement = createElement(key, softLock);
                oldElement = underlyingStore.putIfAbsent(newElement);
                if (oldElement == null) {
                    // CAS succeeded, soft lock is in store, job done.
                    getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                    LOG.debug("putIfAbsent: cache [{}] key [{}] was not in, soft lock inserted", cacheName, key);
                    return null;
                } else {
                    // CAS failed, something with that key may now be in store, job done.
                    softLock.unlock();
                    LOG.debug("putIfAbsent: cache [{}] key [{}] was not in, soft lock insertion failed", cacheName, key);

                    // oldElement may contain a soft lock -> check for that case
                    Object oldElementObjectValue = oldElement.getObjectValue();
                    if (oldElementObjectValue instanceof SoftLock) {
                        SoftLock oldElementSoftLock = (SoftLock) oldElementObjectValue;
                        return copyElement(oldElementSoftLock.getElement(getCurrentTransactionContext().getTransactionId()));
                    } else {
                        return copyElement(oldElement);
                    }
                }
            } else {
                SoftLock softLock = (SoftLock) oldElement.getObjectValue();

                if (cleanupExpiredSoftLock(oldElement, softLock)) {
                    LOG.info("putIfAbsent: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}", new Object[] {cacheName, key, softLock});
                    continue;
                }

                if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                    Element currentElement = softLock.getElement(getCurrentTransactionContext().getTransactionId());
                    if (currentElement == null) {
                        softLock.updateElement(element);
                        underlyingStore.put(oldElement);
                        getCurrentTransactionContext().updateSoftLock(cacheName, softLock);

                        LOG.debug("putIfAbsent: cache [{}] key [{}] soft locked in current transaction, replaced null with new element under soft lock", cacheName, key);
                        // replaced null with new one under soft lock, job done.
                        return null;
                    } else {
                        LOG.debug("putIfAbsent: cache [{}] key [{}] soft locked in current transaction, old element is not null", cacheName, key);
                        // not replaced old value with new one, job done.
                        return copyElement(currentElement);
                    }
                } else {
                    LOG.debug("putIfAbsent: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                    try {
                        boolean locked = softLock.tryLock(timeBeforeTimeout());
                        if (!locked) {
                            LOG.debug("putIfAbsent: cache [{}] key [{}] soft locked in foreign transaction and not released before current transaction timeout", cacheName, key);
                            throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "] between current transaction [" +
                                    getCurrentTransactionContext().getTransactionId() + "] and foreign transaction [" + softLock.getTransactionID() + "]");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    LOG.debug("putIfAbsent: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...", cacheName, key);
                    // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                    continue;
                }
            }

        } // while
    }

    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        if (element == null) {
            throw new NullPointerException("element cannot be null");
        }
        if (element.getObjectKey() == null) {
            throw new NullPointerException("element key cannot be null");
        }
        if (comparator == null) {
            throw new NullPointerException("comparator cannot be null");
        }

        Object key = element.getObjectKey();
        while (true) {
            assertNotTimedOut();

            Element oldElement = underlyingStore.getQuiet(key);
            if (oldElement == null) {
                LOG.debug("removeElement: cache [{}] key [{}] was not in, nothing removed", cacheName, key);
                return null;
            } else {
                Object value = oldElement.getObjectValue();
                if (value instanceof SoftLock) {
                    SoftLock softLock = (SoftLock) value;

                    if (cleanupExpiredSoftLock(oldElement, softLock)) {
                        LOG.info("removeElement: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}", new Object[] {cacheName, key, softLock});
                        continue;
                    }

                    if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        Element currentElement = softLock.getElement(getCurrentTransactionContext().getTransactionId());
                        if (comparator.equals(element, currentElement)) {
                            Element removed = softLock.updateElement(null);
                            underlyingStore.put(oldElement);
                            getCurrentTransactionContext().updateSoftLock(cacheName, softLock);

                            // replaced old element with null under soft lock, job done.
                            LOG.debug("removeElement: cache [{}] key [{}] soft locked in current transaction, replaced old element with null under soft lock", cacheName, key);
                            return copyElement(removed);
                        } else {
                            // old element is not equals to element to remove, job done.
                            LOG.debug("removeElement: cache [{}] key [{}] soft locked in current transaction, old element did not match element to remove", cacheName, key);
                            return null;
                        }
                    } else {
                        try {
                            LOG.debug("removeElement: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                            boolean locked = softLock.tryLock(timeBeforeTimeout());
                            if (!locked) {
                                LOG.debug("removeElement: cache [{}] key [{}] soft locked in foreign transaction and not released before current transaction timeout", cacheName, key);
                                throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "] between current transaction [" +
                                        getCurrentTransactionContext().getTransactionId() + "] and foreign transaction [" + softLock.getTransactionID() + "]");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        LOG.debug("removeElement: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...", cacheName, key);
                        continue;
                    }
                } else {
                    SoftLock softLock = softLockFactory.createSoftLock(getCurrentTransactionContext().getTransactionId(), key, null, oldElement);
                    softLock.lock();
                    Element newElement = createElement(key, softLock);

                    boolean replaced = underlyingStore.replace(oldElement, newElement, comparator);
                    if (replaced) {
                        // CAS succeeded, value replaced with soft lock, job done.
                        getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                        LOG.debug("removeElement: cache [{}] key [{}] was in, replaced with soft lock", cacheName, key);
                        return copyElement(oldElement);
                    } else {
                        // CAS failed, something else with that key is now in store or the key disappeared, job done.
                        softLock.unlock();
                        LOG.debug("removeElement: cache [{}] key [{}] was in, replacement by soft lock failed", cacheName, key);
                        return null;
                    }
                }
            }

        } // while
    }

    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
        if (old == null) {
            throw new NullPointerException("old cannot be null");
        }
        if (old.getObjectKey() == null) {
            throw new NullPointerException("old key cannot be null");
        }
        if (element == null) {
            throw new NullPointerException("element cannot be null");
        }
        if (element.getObjectKey() == null) {
            throw new NullPointerException("element key cannot be null");
        }
        if (comparator == null) {
            throw new NullPointerException("comparator cannot be null");
        }
        if (!old.getKey().equals(element.getKey())) {
            throw new IllegalArgumentException("old and element keys are not equal");
        }

        Object key = element.getObjectKey();
        while (true) {
            assertNotTimedOut();

            Element oldElement = underlyingStore.getQuiet(key);
            if (oldElement == null) {
                LOG.debug("replace: cache [{}] key [{}] was not in, nothing replaced", cacheName, key);
                return false;
            } else {
                Object value = oldElement.getObjectValue();
                if (value instanceof SoftLock) {
                    SoftLock softLock = (SoftLock) value;

                    if (cleanupExpiredSoftLock(oldElement, softLock)) {
                        LOG.info("replace: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}", new Object[] {cacheName, key, softLock});
                        continue;
                    }

                    if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        Element currentElement = softLock.getElement(getCurrentTransactionContext().getTransactionId());
                        if (comparator.equals(old, currentElement)) {
                            softLock.updateElement(element);
                            underlyingStore.put(oldElement);
                            getCurrentTransactionContext().updateSoftLock(cacheName, softLock);

                            // replaced old element with new one under soft lock, job done.
                            LOG.debug("replace: cache [{}] key [{}] soft locked in current transaction, replaced old element with new one under soft lock", cacheName, key);
                            return true;
                        } else {
                            // old element is not equals to element to remove, job done.
                            LOG.debug("replace: cache [{}] key [{}] soft locked in current transaction, old element did not match element to replace", cacheName, key);
                            return false;
                        }
                    } else {
                        try {
                            LOG.debug("replace: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                            boolean locked = softLock.tryLock(timeBeforeTimeout());
                            if (!locked) {
                                LOG.debug("replace: cache [{}] key [{}] soft locked in foreign transaction and not released before current transaction timeout", cacheName, key);
                                throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "] between current transaction [" +
                                        getCurrentTransactionContext().getTransactionId() + "] and foreign transaction [" + softLock.getTransactionID() + "]");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        LOG.debug("replace: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...", cacheName, key);
                        continue;
                    }
                } else {
                    SoftLock softLock = softLockFactory.createSoftLock(getCurrentTransactionContext().getTransactionId(), key, element, oldElement);
                    softLock.lock();
                    Element newElement = createElement(key, softLock);

                    boolean replaced = underlyingStore.replace(oldElement, newElement, comparator);
                    if (replaced) {
                        // CAS succeeded, value replaced with soft lock, job done.
                        getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                        LOG.debug("replace: cache [{}] key [{}] was in, replaced with soft lock", cacheName, key);
                        return true;
                    } else {
                        // CAS failed, something else with that key is now in store or the key disappeared, job done.
                        softLock.unlock();
                        LOG.debug("replace: cache [{}] key [{}] was in, replacement by soft lock failed", cacheName, key);
                        return false;
                    }
                }
            }

        } // while
    }

    public Element replace(Element element) throws NullPointerException {
        if (element == null) {
            throw new NullPointerException("element cannot be null");
        }

        Object key = element.getObjectKey();
        if (key == null) {
            throw new NullPointerException("element key cannot be null");
        }
        while (true) {
            assertNotTimedOut();

            Element oldElement = underlyingStore.getQuiet(key);
            if (oldElement == null) {
                LOG.debug("replace: cache [{}] key [{}] was not in, nothing replaced", cacheName, key);
                return null;
            } else {
                Object value = oldElement.getObjectValue();
                if (value instanceof SoftLock) {
                    SoftLock softLock = (SoftLock) value;

                    if (cleanupExpiredSoftLock(oldElement, softLock)) {
                        LOG.info("replace: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}", new Object[] {cacheName, key, softLock});
                        continue;
                    }

                    if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        Element currentElement = softLock.getElement(getCurrentTransactionContext().getTransactionId());
                        if (currentElement != null) {
                            Element replaced = softLock.updateElement(element);
                            underlyingStore.put(oldElement);
                            getCurrentTransactionContext().updateSoftLock(cacheName, softLock);

                            // replaced old element with new one under soft lock, job done.
                            LOG.debug("replace: cache [{}] key [{}] soft locked in current transaction, replaced old element with new one under soft lock", cacheName, key);
                            return copyElement(replaced);
                        } else {
                            // old element is not equals to element to remove, job done.
                            LOG.debug("replace: cache [{}] key [{}] soft locked in current transaction, old element was null, not replaced", cacheName, key);
                            return null;
                        }
                    } else {
                        try {
                            LOG.debug("replace: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                            boolean locked = softLock.tryLock(timeBeforeTimeout());
                            if (!locked) {
                                LOG.debug("replace: cache [{}] key [{}] soft locked in foreign transaction and not released before current transaction timeout", cacheName, key);
                                throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "] between current transaction [" +
                                        getCurrentTransactionContext().getTransactionId() + "] and foreign transaction [" + softLock.getTransactionID() + "]");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        LOG.debug("replace: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...", cacheName, key);
                        continue;
                    }
                } else {
                    SoftLock softLock = softLockFactory.createSoftLock(getCurrentTransactionContext().getTransactionId(), key, element, oldElement);
                    softLock.lock();
                    Element newElement = createElement(key, softLock);

                    Element replaced = underlyingStore.replace(newElement);
                    if (replaced != null) {
                        // CAS succeeded, value replaced with soft lock, job done.
                        getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                        LOG.debug("replace: cache [{}] key [{}] was in, replaced with soft lock", cacheName, key);
                        return copyElement(replaced);
                    } else {
                        // CAS failed, something else with that key is now in store or the key disappeared, job done.
                        softLock.unlock();
                        LOG.debug("replace: cache [{}] key [{}] was in, replacement by soft lock failed", cacheName, key);
                        return null;
                    }
                }
            }

        } // while
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
            Element element = softLock.getFrozenElement();
            if (element != null) {
                underlyingStore.put(element);
            } else {
                underlyingStore.remove(softLock.getKey());
            }
        }
    }

    public void rollback(List<SoftLock> softLocks) {
        for (SoftLock softLock : softLocks) {
            Element element = softLock.getFrozenElement();
            if (element != null) {
                underlyingStore.put(element);
            } else {
                underlyingStore.remove(softLock.getKey());
            }
        }
    }
    
}
