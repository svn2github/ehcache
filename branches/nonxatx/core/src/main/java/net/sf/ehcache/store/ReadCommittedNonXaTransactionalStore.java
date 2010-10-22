/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.store;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.transaction.nonxa.SoftLock;
import net.sf.ehcache.transaction.nonxa.SoftLockStore;
import net.sf.ehcache.transaction.nonxa.TransactionListener;
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
 *
 * @author Ludovic Orban
 */
public class ReadCommittedNonXaTransactionalStore extends AbstractNonXaTransactionalStore {

    private static final Logger LOG = LoggerFactory.getLogger(ReadCommittedNonXaTransactionalStore.class.getName());

    public ReadCommittedNonXaTransactionalStore(TransactionController transactionController, SoftLockStore softLockStore, String cacheName, Store underlyingStore) {
        super(transactionController, softLockStore, cacheName, underlyingStore);
    }

    /* transactional methods */

    public boolean put(Element element) throws CacheException {
        lock.writeLock().lock();
        try {
            Object key = element.getObjectKey();
            SoftLock softLock = softLockMap.get(key);
            if (softLock == null) {
                LOG.debug("put: cache [{}] key [{}] not locked, locking it now", cacheName, element.getObjectKey());
                softLock = softLockStore.createSoftLock(getCurrentTransactionContext().getTransactionId(), element.getObjectKey(), element);
                softLockMap.put(key, softLock);
                softLock.lock(); // lock must be stored in a TC managed object before it can be locked
                getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                return !underlyingStore.containsKey(key);
            } else {
                if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                    LOG.debug("put: cache [{}] key [{}] locked in current transaction, updating new value", cacheName, element.getObjectKey());
                    softLock.setNewElement(element);
                    return false;
                } else {
                    LOG.debug("put: cache [{}] key [{}] locked in transaction [{}], waiting until lock gets removed", new Object[] {cacheName, element.getObjectKey(), softLock.getTransactionID()});
                    tryLockSoftLock(softLock);
                    LOG.debug("put: cache [{}] key [{}] unlocked, locking it again", cacheName, element.getObjectKey());
                    softLock = softLockStore.createSoftLock(getCurrentTransactionContext().getTransactionId(), element.getObjectKey(), element);
                    softLockMap.put(key, softLock);
                    softLock.lock(); // lock must be stored in a TC managed object before it can be locked
                    getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                    return !underlyingStore.containsKey(key);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Element get(Object key) {
        lock.readLock().lock();
        try {
            SoftLock softLock = softLockMap.get(key);
            if (softLock == null) {
                LOG.debug("get: cache [{}] key [{}] not locked, returning value", cacheName, key);
                return underlyingStore.get(key);
            }

            if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                LOG.debug("get: cache [{}] key [{}] locked in current transaction, returning new value", cacheName, key);
                return softLock.getNewElement();
            } else {
                LOG.debug("get: cache [{}] key [{}] locked in transaction [{}], returning old value", new Object[] {cacheName, key, softLock.getTransactionID()});
                return underlyingStore.get(key);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public Element getQuiet(Object key) {
        lock.readLock().lock();
        try {
            SoftLock softLock = softLockMap.get(key);
            if (softLock == null) {
                LOG.debug("getQuiet: cache [{}] key [{}] not locked, returning value", cacheName, key);
                return underlyingStore.getQuiet(key);
            }

            if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                LOG.debug("getQuiet: cache [{}] key [{}] locked in current transaction, returning new value", cacheName, key);
                return softLock.getNewElement();
            } else {
                LOG.debug("getQuiet: cache [{}] key [{}] locked in transaction [{}], returning old value", new Object[] {cacheName, key, softLock.getTransactionID()});
                return underlyingStore.getQuiet(key);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public List getKeys() {
        lock.readLock().lock();
        try {
            Set<Object> keys = new LargeSet<Object>() {
                @Override
                public int sourceSize() {
                    return underlyingStore.getSize();
                }

                @Override
                public Iterator<Object> sourceIterator() {
                    @SuppressWarnings("unchecked")
                    Iterator iterator = underlyingStore.getKeys().iterator();
                    return iterator;
                }
            };
            keys.addAll(getCurrentTransactionContext().getPutKeys(cacheName));
            keys.removeAll(getCurrentTransactionContext().getRemovedKeys(cacheName));
            return new SetWrapperList(keys);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Element remove(Object key) {
        lock.writeLock().lock();
        try {
            SoftLock softLock = softLockMap.get(key);

            if (softLock == null && underlyingStore.getQuiet(key) == null) {
                LOG.debug("remove: cache [{}] key [{}] is not locked and is not in cache, nothing to do", cacheName, key);
                return null;
            } else if (softLock == null && underlyingStore.getQuiet(key) != null) {
                LOG.debug("remove: cache [{}] key [{}] not locked and in cache, locking it now", cacheName, key);
                softLock = softLockStore.createSoftLock(getCurrentTransactionContext().getTransactionId(), key, null);
                softLockMap.put(key, softLock);
                softLock.lock(); // lock must be stored in a TC managed object before it can be locked
                getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                return underlyingStore.getQuiet(key);
            } else {
                // softLock cannot be null here
                if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                    if (underlyingStore.getQuiet(key) == null) {
                        LOG.debug("remove: cache [{}] key [{}] locked and put in current transaction, removing and unlocking it", cacheName, key);
                        softLockMap.remove(softLock);
                        getCurrentTransactionContext().unregisterSoftLock(cacheName, this, softLock);
                        softLock.unlock();
                        return softLock.getNewElement();
                    } else {
                        LOG.debug("remove: cache [{}] key [{}] locked in another transaction, removing it", cacheName, key);
                        Element currentElement = softLock.getNewElement();
                        softLock.setNewElement(null);
                        return currentElement;
                    }
                } else {
                    LOG.debug("remove: cache [{}] key [{}] locked in transaction [{}], waiting until lock gets removed", new Object[] {cacheName, key, softLock.getTransactionID()});
                    tryLockSoftLock(softLock);
                    LOG.debug("remove: cache [{}] key [{}] unlocked, locking it again", cacheName, key);
                    softLock = softLockStore.createSoftLock(getCurrentTransactionContext().getTransactionId(), key, null);
                    softLockMap.put(key, softLock);
                    softLock.lock(); // lock must be stored in a TC managed object before it can be locked
                    getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                    return underlyingStore.getQuiet(key);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeAll() throws CacheException {
        lock.writeLock().lock();
        try {
            List keys = getKeys();
            for (Object key : keys) {
                remove(key);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        lock.writeLock().lock();
        try {
            boolean rc = put(element);
            if (writerManager != null) {
                getCurrentTransactionContext().addListener(new TransactionListener() {
                    public void beforeCommit() {
                        writerManager.put(element);
                    }

                    public void afterCommit() {
                    }

                    public void afterRollback() {
                    }
                });
            }
            return rc;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        lock.writeLock().lock();
        try {
            Element rc = remove(key);
            if (writerManager != null) {
                getCurrentTransactionContext().addListener(new TransactionListener() {
                    public void beforeCommit() {
                        writerManager.remove(new CacheEntry(key, getQuiet(key)));
                    }

                    public void afterCommit() {
                    }

                    public void afterRollback() {
                    }
                });
            }
            return rc;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        if (element == null || element.getObjectKey() == null) {
            throw new NullPointerException("element and element key cannot be null");
        }

        lock.writeLock().lock();
        try {
            Element oldElement = getQuiet(element.getObjectKey());
            if (oldElement == null) {
                put(element);
                return null;
            } else {
                return oldElement;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Element removeElement(Element element) throws NullPointerException {
        if (element == null || element.getObjectKey() == null) {
            throw new NullPointerException("element and element key cannot be null");
        }

        lock.writeLock().lock();
        try {
            Element oldElement = getQuiet(element.getObjectKey());
            if (oldElement == null) {
                return null;
            }
            if ((oldElement.getObjectValue() == null && element.getObjectValue() == null) ||
                    (oldElement.getObjectValue().equals(element.getObjectValue()))) {
                return remove(element.getObjectKey());
            } else {
                return null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        if (old == null || old.getObjectKey() == null) {
            throw new NullPointerException("old element and element key cannot be null");
        }
        if (element == null || element.getObjectKey() == null) {
            throw new NullPointerException("element and element key cannot be null");
        }
        if (!old.getObjectKey().equals(element.getObjectKey())) {
            throw new NullPointerException("old and new element keys are not equal");
        }

        lock.writeLock().lock();
        try {
            Element oldElement = getQuiet(element.getObjectKey());
            if (oldElement == null) {
                return false;
            }
            if ((oldElement.getObjectValue() == null && old.getObjectValue() == null) ||
                    (oldElement.getObjectValue().equals(old.getObjectValue()))) {
                remove(old.getObjectKey());
                put(element);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Element replace(Element element) throws NullPointerException {
        if (element == null || element.getObjectKey() == null) {
            throw new NullPointerException("element and element key cannot be null");
        }

        lock.writeLock().lock();
        try {
            Element oldElement = getQuiet(element.getObjectKey());
            if (oldElement == null) {
                return null;
            }
            if ((oldElement.getObjectValue() == null && element.getObjectValue() == null) ||
                    (oldElement.getObjectValue().equals(element.getObjectValue()))) {
                Element removed = remove(element.getObjectKey());
                put(element);
                return removed;
            } else {
                return null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getSize() {
        lock.readLock().lock();
        try {
            int sizeModifier = 0;
            sizeModifier += getCurrentTransactionContext().getPutKeys(cacheName).size();
            sizeModifier -= getCurrentTransactionContext().getRemovedKeys(cacheName).size();
            return underlyingStore.getSize() + sizeModifier;
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getTerracottaClusteredSize() {
        lock.readLock().lock();
        try {
            int sizeModifier = 0;
            sizeModifier += getCurrentTransactionContext().getPutKeys(cacheName).size();
            sizeModifier -= getCurrentTransactionContext().getRemovedKeys(cacheName).size();
            return underlyingStore.getTerracottaClusteredSize() + sizeModifier;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean containsKey(Object key) {
        lock.readLock().lock();
        try {
            getCurrentTransactionContext().getPutKeys(cacheName);
            return !getCurrentTransactionContext().getRemovedKeys(cacheName).contains(key) &&
                   getCurrentTransactionContext().getPutKeys(cacheName).contains(key) ||
                   underlyingStore.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /* non-transactional methods */

    public void dispose() {
        underlyingStore.dispose();
    }

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

    public Status getStatus() {
        return underlyingStore.getStatus();
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
}
