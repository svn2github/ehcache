/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.ehcache.transaction.local;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.AbstractTransactionStore;
import net.sf.ehcache.transaction.DeadLockException;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.SoftLockID;
import net.sf.ehcache.transaction.SoftLockManager;
import net.sf.ehcache.transaction.TransactionException;
import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.TransactionInterruptedException;
import net.sf.ehcache.transaction.TransactionTimeoutException;
import net.sf.ehcache.util.LargeSet;
import net.sf.ehcache.util.SetAsList;
import net.sf.ehcache.writer.CacheWriterManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A Store implementation with support for local transactions
 *
 * @author Ludovic Orban
 */
public class LocalTransactionStore extends AbstractTransactionStore {

    private static final Logger LOG = LoggerFactory.getLogger(LocalTransactionStore.class.getName());

    private final TransactionController transactionController;
    private final TransactionIDFactory transactionIdFactory;
    private final SoftLockManager softLockManager;
    private final Ehcache cache;
    private final String cacheName;
    private final ElementValueComparator comparator;


    /**
     * Create a new LocalTransactionStore instance
     * @param transactionController the TransactionController
     * @param softLockManager the SoftLockManager
     * @param cache the cache
     * @param store the underlying store
     * @param comparator the element value comparator
     */
    public LocalTransactionStore(TransactionController transactionController, TransactionIDFactory transactionIdFactory,
            SoftLockManager softLockManager, Ehcache cache, Store store, ElementValueComparator comparator) {
        super(store);
        this.transactionController = transactionController;
        this.transactionIdFactory = transactionIdFactory;
        this.softLockManager = softLockManager;
        this.cache = cache;
        this.comparator = comparator;
        this.cacheName = cache.getName();
        transactionController.getRecoveryManager().register(this);
    }

    @Override
    public void dispose() {
        super.dispose();
        transactionController.getRecoveryManager().unregister(this);
    }

    /**
     * Get the cache using this store
     * @return the cache using this store
     */
    Ehcache getCache() {
        return cache;
    }

    private LocalTransactionContext getCurrentTransactionContext() {
        LocalTransactionContext currentTransactionContext = transactionController.getCurrentTransactionContext();
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
            throw new TransactionInterruptedException("transaction [" + getCurrentTransactionContext().getTransactionId() +
                    "] interrupted");
        }
    }

    private long timeBeforeTimeout() {
        return getCurrentTransactionContext().timeBeforeTimeout();
    }

    private Element createElement(Object key, SoftLockID softLockId) {
        Element element = new Element(key, softLockId);
        element.setEternal(true);
        return element;
    }

    private boolean cleanupExpiredSoftLock(Element oldElement, SoftLockID softLockId) {
        SoftLock softLock = softLockManager.findSoftLockById(softLockId);
        if (softLock == null || !softLock.isExpired()) {
            return false;
        } else {
            softLock.lock();
            softLock.freeze();
            try {
                Element frozenElement;
                if (transactionIdFactory.isDecisionCommit(softLockId.getTransactionID())) {
                    frozenElement = softLockId.getNewElement();
                } else {
                    frozenElement = softLockId.getOldElement();
                }
                if (frozenElement != null) {
                    underlyingStore.replace(oldElement, frozenElement, comparator);
                } else {
                    underlyingStore.removeElement(oldElement, comparator);
                }

            } finally {
                softLock.unfreeze();
                softLock.unlock();
            }
            return true;
        }
    }

    /**
     * Recover and resolve all known soft locks
     * @return a set of transaction IDs that were recovered
     */
    public Set<TransactionID> recover() {
        Set<TransactionID> allOurTransactionIDs = transactionIdFactory.getAllTransactionIDs();

        Set<TransactionID> recoveredIds = new HashSet<TransactionID>(allOurTransactionIDs);
        Iterator<TransactionID> iterator = recoveredIds.iterator();
        while (iterator.hasNext()) {
            TransactionID transactionId = iterator.next();
            if (!transactionIdFactory.isExpired(transactionId)) {
                iterator.remove();
            }
        }

        LOG.debug("recover: {} dead transactions are going to be recovered", recoveredIds.size());
        for (TransactionID transactionId : recoveredIds) {
            Set<SoftLock> softLocks = new HashSet<SoftLock>(softLockManager.collectAllSoftLocksForTransactionID(transactionId));
            Iterator<SoftLock> softLockIterator = softLocks.iterator();
            while (softLockIterator.hasNext()) {
                SoftLock softLock = softLockIterator.next();
                Element element = underlyingStore.getQuiet(softLock.getKey());
                if (element.getObjectValue() instanceof SoftLockID) {
                    SoftLockID softLockId = (SoftLockID)element.getObjectValue();
                    cleanupExpiredSoftLock(element, softLockId);
                } else {
                    softLockIterator.remove();
                }
            }
            LOG.debug("recover: recovered {} soft locks from dead transaction with ID [{}]", softLocks.size(), transactionId);
        }

        return recoveredIds;
    }

    /* transactional methods */

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) throws CacheException {
        if (element == null) {
            return true;
        }

        final Object key = element.getObjectKey();
        while (true) {
            assertNotTimedOut();

            Element oldElement = underlyingStore.getQuiet(key);
            if (oldElement == null) {
                SoftLockID softLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), key,
                    element, null);
                SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                softLock.lock();
                Element newElement = createElement(key, softLockId);
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
                if (value instanceof SoftLockID) {
                    SoftLockID softLockId = (SoftLockID) value;

                    if (cleanupExpiredSoftLock(oldElement, softLockId)) {
                        LOG.debug("put: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}",
                                new Object[] {cacheName, key, softLockId});
                        continue;
                    }

                    if (softLockId.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        SoftLockID newSoftLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), softLockId
                            .getKey(), element, softLockId.getOldElement());
                        Element newElement = createElement(newSoftLockId.getKey(), newSoftLockId);


                        underlyingStore.put(newElement);

                        LOG.debug("put: cache [{}] key [{}] soft locked in current transaction, replaced old value with new one under" +
                                " soft lock", cacheName, key);
                        // replaced old value with new one under soft lock, job done.
                        return false;
                    } else {
                        SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                        if (softLock != null) {
                            LOG.debug("put: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to die...",
                                    new Object[] {cacheName, key, timeBeforeTimeout()});
                            try {
                                if (!softLock.tryLock(timeBeforeTimeout())) {
                                    LOG.debug("put: cache [{}] key [{}] soft locked in foreign transaction and not released before" +
                                            " current transaction timeout", cacheName, key);
                                    if (getCurrentTransactionContext().hasLockedAnything()) {
                                        throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "]" +
                                                " between current transaction [" + getCurrentTransactionContext().getTransactionId() + "]" +
                                                " and foreign transaction [" + softLockId.getTransactionID() + "]");
                                    } else {
                                        continue;
                                    }
                                } else {
                                    softLock.clearTryLock();
                                }
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        LOG.debug("put: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...",
                                cacheName, key);
                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        continue;
                    }
                } else {
                    SoftLockID softLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), key,
                        element, oldElement);
                    SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                    softLock.lock();
                    Element newElement = createElement(key, softLockId);
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

    /**
     * {@inheritDoc}
     */
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
            if (value instanceof SoftLockID) {
                SoftLockID softLockId = (SoftLockID) value;
                if (cleanupExpiredSoftLock(oldElement, softLockId)) {
                    LOG.debug("getQuiet: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}",
                            new Object[] {cacheName, key, softLockId});
                    continue;
                }

                LOG.debug("getQuiet: cache [{}] key [{}] soft locked, returning soft locked element", cacheName, key);
                SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                if (softLock == null) {
                    LOG.debug("getQuiet: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...", cacheName, key);
                    continue;
                } else {
                    return softLock.getElement(getCurrentTransactionContext().getTransactionId(), softLockId);
                }
            } else {
                LOG.debug("getQuiet: cache [{}] key [{}] not soft locked, returning underlying element", cacheName, key);
                return oldElement;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
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
            if (value instanceof SoftLockID) {
                SoftLockID softLockId = (SoftLockID) value;
                if (cleanupExpiredSoftLock(oldElement, softLockId)) {
                    LOG.debug("get: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}",
                            new Object[] {cacheName, key, softLockId});
                    continue;
                }

                LOG.debug("get: cache [{}] key [{}] soft locked, returning soft locked element", cacheName, key);
                SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                if (softLock == null) {
                    LOG.debug("get: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...", cacheName, key);
                    continue;
                } else {
                    return softLock.getElement(getCurrentTransactionContext().getTransactionId(), softLockId);
                }
            } else {
                LOG.debug("get: cache [{}] key [{}] not soft locked, returning underlying element", cacheName, key);
                return oldElement;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        if (key == null) {
            return null;
        }

        while (true) {
            final boolean isPinned = false;
            assertNotTimedOut();

            Element oldElement = underlyingStore.getQuiet(key);
            if (oldElement == null) {
                SoftLockID softLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), key,
                    null, null);
                SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                softLock.lock();
                Element newElement = createElement(key, softLockId);
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
                if (value instanceof SoftLockID) {
                    SoftLockID softLockId = (SoftLockID) value;

                    if (cleanupExpiredSoftLock(oldElement, softLockId)) {
                        LOG.debug("remove: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}",
                                new Object[] {cacheName, key, softLockId});
                        continue;
                    }

                    if (softLockId.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {

                        SoftLockID newSoftLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), softLockId
                            .getKey(), null, softLockId.getOldElement());
                        Element newElement = createElement(newSoftLockId.getKey(), newSoftLockId);


                        underlyingStore.put(newElement);

                        // replaced old value with new one under soft lock, job done.
                        LOG.debug("remove: cache [{}] key [{}] soft locked in current transaction, replaced old value with new one under" +
                                " soft lock", cacheName, key);
                        return softLockId.getNewElement();
                    } else {
                        SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                        if (softLock != null) {
                            LOG.debug("remove: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to" +
                                    " die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                            try {
                                if (softLock.tryLock(timeBeforeTimeout())) {
                                    softLock.clearTryLock();
                                } else {
                                    LOG.debug("remove: cache [{}] key [{}] soft locked in foreign transaction and not released before" +
                                            " current transaction timeout", cacheName, key);
                                    if (getCurrentTransactionContext().hasLockedAnything()) {
                                        throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "]" +
                                                " between current transaction [" + getCurrentTransactionContext().getTransactionId() + "]" +
                                                " and foreign transaction [" + softLockId.getTransactionID() + "]");
                                    } else {
                                        continue;
                                    }
                                }
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        LOG.debug("remove: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...",
                                cacheName, key);
                        continue;
                    }
                } else {
                    SoftLockID softLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), key,
                        null, oldElement);
                    SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                    softLock.lock();
                    Element newElement = createElement(key, softLockId);
                    boolean replaced = underlyingStore.replace(oldElement, newElement, comparator);
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

    /**
     * {@inheritDoc}
     */
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

        keys.removeAll(softLockManager.getKeysInvisibleInContext(getCurrentTransactionContext(), underlyingStore));

        return new SetAsList<Object>(keys);
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        assertNotTimedOut();

        int sizeModifier = 0;
        sizeModifier -= softLockManager.getKeysInvisibleInContext(getCurrentTransactionContext(), underlyingStore).size();
        return Math.max(0, underlyingStore.getSize() + sizeModifier);
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        if (transactionController.getCurrentTransactionContext() == null) {
            return underlyingStore.getTerracottaClusteredSize();
        }

        int sizeModifier = 0;
        sizeModifier -= softLockManager.getKeysInvisibleInContext(getCurrentTransactionContext(), underlyingStore).size();
        return underlyingStore.getTerracottaClusteredSize() + sizeModifier;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        assertNotTimedOut();

        return getKeys().contains(key);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        assertNotTimedOut();

        List keys = getKeys();
        for (Object key : keys) {
            remove(key);
        }
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        if (element == null) {
            throw new NullPointerException("element cannot be null");
        }
        if (element.getObjectKey() == null) {
            throw new NullPointerException("element key cannot be null");
        }

        final Object key = element.getObjectKey();
        while (true) {
            assertNotTimedOut();

            Element oldElement = underlyingStore.getQuiet(key);
            if (oldElement == null) {
                SoftLockID softLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), key,
                    element, oldElement);
                SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                softLock.lock();
                Element newElement = createElement(key, softLockId);
                oldElement = underlyingStore.putIfAbsent(newElement);
                if (oldElement == null) {
                    // CAS succeeded, soft lock is in store, job done.
                    getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                    LOG.debug("putIfAbsent: cache [{}] key [{}] was not in, soft lock inserted", cacheName, key);
                    return null;
                } else {
                    // CAS failed, something with that key may now be in store, lets retry the operation.
                    softLock.unlock();
                    LOG.debug("putIfAbsent: cache [{}] key [{}] was not in, soft lock insertion failed, retrying", cacheName, key);
                    continue;
                }
            } else if (oldElement.getValue() instanceof SoftLockID) {
                SoftLockID softLockId = (SoftLockID) oldElement.getObjectValue();

                if (cleanupExpiredSoftLock(oldElement, softLockId)) {
                    LOG.debug("putIfAbsent: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}",
                            new Object[] {cacheName, key, softLockId});
                    continue;
                }

                if (softLockId.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                    SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                    Element currentElement = softLock.getElement(getCurrentTransactionContext().getTransactionId(), softLockId);
                    if (currentElement == null) {
                        SoftLockID newSoftLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(),
                            softLockId.getKey(), element, softLockId.getOldElement());
                        Element newElement = createElement(newSoftLockId.getKey(), newSoftLockId);
                        underlyingStore.put(newElement);

                        LOG.debug("putIfAbsent: cache [{}] key [{}] soft locked in current transaction, replaced null with new element" +
                                " under soft lock", cacheName, key);
                        // replaced null with new one under soft lock, job done.
                        return null;
                    } else {
                        LOG.debug("putIfAbsent: cache [{}] key [{}] soft locked in current transaction, old element is not null",
                                cacheName, key);
                        // not replaced old value with new one, job done.
                        return currentElement;
                    }
                } else {
                    SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                    if (softLock != null) {
                        LOG.debug("putIfAbsent: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock to die...",
                                new Object[] {cacheName, key, timeBeforeTimeout()});
                        try {
                            if (softLock.tryLock(timeBeforeTimeout())) {
                                softLock.clearTryLock();
                            } else {
                                LOG.debug("putIfAbsent: cache [{}] key [{}] soft locked in foreign transaction and not released before" +
                                        " current transaction timeout", cacheName, key);
                                if (getCurrentTransactionContext().hasLockedAnything()) {
                                    throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "]" +
                                            " between current transaction [" + getCurrentTransactionContext().getTransactionId() + "]" +
                                            " and foreign transaction [" + softLockId.getTransactionID() + "]");
                                } else {
                                    continue;
                                }
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    LOG.debug("putIfAbsent: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...",
                            cacheName, key);
                    // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                    continue;
                }
            } else {
                return oldElement;
            }
        } // while
    }

    /**
     * {@inheritDoc}
     */
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

        final Object key = element.getObjectKey();
        while (true) {
            assertNotTimedOut();

            Element oldElement = underlyingStore.getQuiet(key);
            if (oldElement == null) {
                LOG.debug("removeElement: cache [{}] key [{}] was not in, nothing removed", cacheName, key);
                return null;
            } else {
                Object value = oldElement.getObjectValue();
                if (value instanceof SoftLockID) {
                    SoftLockID softLockId = (SoftLockID) value;

                    if (cleanupExpiredSoftLock(oldElement, softLockId)) {
                        LOG.debug("removeElement: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}",
                                new Object[] {cacheName, key, softLockId});
                        continue;
                    }

                    if (softLockId.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                        Element currentElement = softLock.getElement(getCurrentTransactionContext().getTransactionId(), softLockId);
                        if (comparator.equals(element, currentElement)) {
                            SoftLockID newSoftLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), softLockId
                                .getKey(), element, softLockId.getOldElement());
                            Element newElement = createElement(newSoftLockId.getKey(), newSoftLockId);
                            underlyingStore.put(newElement);

                            // replaced old element with null under soft lock, job done.
                            LOG.debug("removeElement: cache [{}] key [{}] soft locked in current transaction, replaced old element" +
                                    " with null under soft lock", cacheName, key);
                            return softLockId.getNewElement();
                        } else {
                            // old element is not equals to element to remove, job done.
                            LOG.debug("removeElement: cache [{}] key [{}] soft locked in current transaction, old element did not" +
                                    " match element to remove", cacheName, key);
                            return null;
                        }
                    } else {
                        SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                        if (softLock != null) {
                            LOG.debug("removeElement: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft" +
                                    " lock to die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                            try {
                                if (softLock.tryLock(timeBeforeTimeout())) {
                                    softLock.clearTryLock();
                                } else {
                                    LOG.debug("removeElement: cache [{}] key [{}] soft locked in foreign transaction and not released" +
                                            " before current transaction timeout", cacheName, key);
                                    if (getCurrentTransactionContext().hasLockedAnything()) {
                                        throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "]" +
                                                " between current transaction [" + getCurrentTransactionContext().getTransactionId() + "]" +
                                                " and foreign transaction [" + softLockId.getTransactionID() + "]");
                                    } else {
                                        continue;
                                    }
                                }
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        LOG.debug("removeElement: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...",
                                cacheName, key);
                        continue;
                    }
                } else {
                    SoftLockID softLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), key,
                        null, oldElement);
                    SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                    softLock.lock();
                    Element newElement = createElement(key, softLockId);

                    boolean replaced = underlyingStore.replace(oldElement, newElement, comparator);
                    if (replaced) {
                        // CAS succeeded, value replaced with soft lock, job done.
                        getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                        LOG.debug("removeElement: cache [{}] key [{}] was in, replaced with soft lock", cacheName, key);
                        return oldElement;
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

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator)
            throws NullPointerException, IllegalArgumentException {
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

        final Object key = element.getObjectKey();
        while (true) {
            assertNotTimedOut();

            Element oldElement = underlyingStore.getQuiet(key);
            if (oldElement == null) {
                LOG.debug("replace2: cache [{}] key [{}] was not in, nothing replaced", cacheName, key);
                return false;
            } else {
                Object value = oldElement.getObjectValue();
                if (value instanceof SoftLockID) {
                    SoftLockID softLockId = (SoftLockID) value;

                    if (cleanupExpiredSoftLock(oldElement, softLockId)) {
                        LOG.debug("replace2: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}",
                                new Object[] {cacheName, key, softLockId});
                        continue;
                    }

                    if (softLockId.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                        Element currentElement = softLock.getElement(getCurrentTransactionContext().getTransactionId(), softLockId);
                        if (comparator.equals(old, currentElement)) {
                            SoftLockID newSoftLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(),
                                softLockId.getKey(), element, softLockId.getOldElement());
                            Element newElement = createElement(newSoftLockId.getKey(), newSoftLockId);
                            underlyingStore.put(newElement);

                            // replaced old element with new one under soft lock, job done.
                            LOG.debug("replace2: cache [{}] key [{}] soft locked in current transaction, replaced old element with" +
                                    " new one under soft lock", cacheName, key);
                            return true;
                        } else {
                            // old element is not equals to element to remove, job done.
                            LOG.debug("replace2: cache [{}] key [{}] soft locked in current transaction, old element did not match" +
                                    " element to replace", cacheName, key);
                            return false;
                        }
                    } else {
                        SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                        if (softLock != null) {
                            LOG.debug("replace2: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for" +
                                    " soft lock to die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                            try {
                                if (softLock.tryLock(timeBeforeTimeout())) {
                                    softLock.clearTryLock();
                                } else {
                                    LOG.debug("replace2: cache [{}] key [{}] soft locked in foreign transaction and not released before" +
                                            " current transaction timeout", cacheName, key);
                                    if (getCurrentTransactionContext().hasLockedAnything()) {
                                        throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "]" +
                                                " between current transaction [" + getCurrentTransactionContext().getTransactionId() + "]" +
                                                " and foreign transaction [" + softLockId.getTransactionID() + "]");
                                    } else {
                                        continue;
                                    }
                                }
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        LOG.debug("replace2: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...",
                                cacheName, key);
                        continue;
                    }
                } else {
                    SoftLockID softLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), key,
                        element, oldElement);
                    SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                    softLock.lock();
                    Element newElement = createElement(key, softLockId);

                    boolean replaced = underlyingStore.replace(oldElement, newElement, comparator);
                    if (replaced) {
                        // CAS succeeded, value replaced with soft lock, job done.
                        getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                        LOG.debug("replace2: cache [{}] key [{}] was in, replaced with soft lock", cacheName, key);
                        return true;
                    } else {
                        // CAS failed, something else with that key is now in store or the key disappeared, job done.
                        softLock.unlock();
                        LOG.debug("replace2: cache [{}] key [{}] was in, replacement by soft lock failed", cacheName, key);
                        return false;
                    }
                }
            }

        } // while
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        if (element == null) {
            throw new NullPointerException("element cannot be null");
        }

        final Object key = element.getObjectKey();
        if (key == null) {
            throw new NullPointerException("element key cannot be null");
        }
        while (true) {
            assertNotTimedOut();

            Element oldElement = underlyingStore.getQuiet(key);
            if (oldElement == null) {
                LOG.debug("replace1: cache [{}] key [{}] was not in, nothing replaced", cacheName, key);
                return null;
            } else {
                Object value = oldElement.getObjectValue();
                if (value instanceof SoftLockID) {
                    SoftLockID softLockId = (SoftLockID) value;

                    if (cleanupExpiredSoftLock(oldElement, softLockId)) {
                        LOG.debug("replace1: cache [{}] key [{}] guarded by expired soft lock, cleaned up {}",
                                new Object[] {cacheName, key, softLockId});
                        continue;
                    }

                    if (softLockId.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                        SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                        Element currentElement = softLock.getElement(getCurrentTransactionContext().getTransactionId(), softLockId);
                        if (currentElement != null) {
                            SoftLockID newSoftLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), softLockId
                                .getKey(), element, softLockId.getOldElement());
                            Element newElement = createElement(newSoftLockId.getKey(), newSoftLockId);
                            underlyingStore.put(newElement);

                            // replaced old element with new one under soft lock, job done.
                            LOG.debug("replace1: cache [{}] key [{}] soft locked in current transaction, replaced old element with" +
                                    " new one under soft lock", cacheName, key);
                            return softLockId.getNewElement();
                        } else {
                            // old element is not equals to element to remove, job done.
                            LOG.debug("replace1: cache [{}] key [{}] soft locked in current transaction, old element was null," +
                                    " not replaced", cacheName, key);
                            return null;
                        }
                    } else {
                        SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                        if (softLock != null) {
                            LOG.debug("replace1: cache [{}] key [{}] soft locked in foreign transaction, waiting {}ms for soft lock" +
                                    " to die...", new Object[] {cacheName, key, timeBeforeTimeout()});
                            try {
                                if (softLock.tryLock(timeBeforeTimeout())) {
                                    softLock.clearTryLock();
                                } else {
                                    LOG.debug("replace1: cache [{}] key [{}] soft locked in foreign transaction and not released before" +
                                            " current transaction timeout", cacheName, key);
                                    if (getCurrentTransactionContext().hasLockedAnything()) {
                                        throw new DeadLockException("deadlock detected in cache [" + cacheName + "] on key [" + key + "]" +
                                                " between current transaction [" + getCurrentTransactionContext().getTransactionId() + "]" +
                                                " and foreign transaction [" + softLockId.getTransactionID() + "]");
                                    } else {
                                        continue;
                                    }
                                }
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }

                        // once the soft lock got unlocked we don't know what's in the store anymore, restart.
                        LOG.debug("replace1: cache [{}] key [{}] soft locked in foreign transaction, soft lock died, retrying...",
                                cacheName, key);
                        continue;
                    }
                } else {
                    SoftLockID softLockId = softLockManager.createSoftLockID(getCurrentTransactionContext().getTransactionId(), key,
                        element, oldElement);
                    SoftLock softLock = softLockManager.findSoftLockById(softLockId);
                    softLock.lock();
                    Element newElement = createElement(key, softLockId);

                    Element replaced = underlyingStore.replace(newElement);
                    if (replaced != null) {
                        // CAS succeeded, value replaced with soft lock, job done.
                        getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                        LOG.debug("replace1: cache [{}] key [{}] was in, replaced with soft lock", cacheName, key);
                        return replaced;
                    } else {
                        // CAS failed, something else with that key is now in store or the key disappeared, job done.
                        softLock.unlock();
                        LOG.debug("replace1: cache [{}] key [{}] was in, replacement by soft lock failed", cacheName, key);
                        return null;
                    }
                }
            }

        } // while
    }

    /**
     * Commit work of the specified soft locks
     * @param softLocks the soft locks to commit
     * @param transactionId the transaction ID to commit
     */
    void commit(List<SoftLock> softLocks, TransactionID transactionId) {
        LOG.debug("committing {} soft lock(s) in cache {}", softLocks.size(), cache.getName());
        for (SoftLock softLock : softLocks) {
            Element e = underlyingStore.getQuiet(softLock.getKey());
            if (e == null) {
                // the element can be null if it was manually unpinned, see DEV-8308
                LOG.debug("soft lock ID with key '{}' is not present in underlying store, ignoring it", softLock.getKey());
                continue;
            }
            if (!(e.getObjectValue() instanceof SoftLockID)) {
                // potential consequence of the above condition
                LOG.debug("soft lock ID with key '{}' replaced with value in underlying store, ignoring it", softLock.getKey());
                continue;
            }
            SoftLockID softLockId = (SoftLockID)e.getObjectValue();
            if (!softLockId.getTransactionID().equals(transactionId)) {
                LOG.debug("soft lock ID with key '{}' of foreign tx in underlying store, ignoring it", softLock.getKey());
                continue;
            }

            Element element = softLockId.getNewElement();
            if (element != null) {
                underlyingStore.put(element);
            } else {
                underlyingStore.remove(softLock.getKey());
            }
        }
    }

    /**
     * Rollback work of the specified soft locks
     * @param softLocks the soft locks to rollback
     * @param transactionId the transaction ID to rollback
     */
    void rollback(List<SoftLock> softLocks, TransactionID transactionId) {
        LOG.debug("rolling back {} soft lock(s) in cache {}", softLocks.size(), cache.getName());
        for (SoftLock softLock : softLocks) {
            Element e = underlyingStore.getQuiet(softLock.getKey());
            if (e == null) {
                // the element can be null if it was manually unpinned, see DEV-8308
                LOG.debug("soft lock ID with key '{}' is not present in underlying store, ignoring it", softLock.getKey());
                continue;
            }
            if (!(e.getObjectValue() instanceof SoftLockID)) {
                // potential consequence of the above condition
                LOG.debug("soft lock ID with key '{}' replaced with value in underlying store, ignoring it", softLock.getKey());
                continue;
            }
            SoftLockID softLockId = (SoftLockID)e.getObjectValue();
            if (!softLockId.getTransactionID().equals(transactionId)) {
                LOG.debug("soft lock ID with key '{}' of foreign tx in underlying store, ignoring it", softLock.getKey());
                continue;
            }

            Element element = softLockId.getOldElement();
            if (element != null) {
                underlyingStore.put(element);
            } else {
                underlyingStore.remove(softLock.getKey());
            }
        }
    }

}
