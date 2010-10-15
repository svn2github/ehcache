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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.transaction.nonxa.SoftLock;
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


    public ReadCommittedNonXaTransactionalStore(TransactionController transactionController, String cacheName, Store underlyingStore) {
        super(transactionController, cacheName, underlyingStore);
    }

    /* transactional methods */

    public boolean put(Element element) throws CacheException {
        lock.writeLock().lock();
        try {
            Object key = element.getObjectKey();
            SoftLock softLock = softLockMap.get(key);
            if (softLock == null) {
                LOG.debug("put: key [{}] not locked, locking it now", element.getObjectKey());
                softLock = new SoftLock(getCurrentTransactionContext().getTransactionId(), element.getObjectKey(), element);
                softLockMap.put(key, softLock);
                getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                return !underlyingStore.containsKey(key);
            } else {
                if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                    LOG.debug("put: key [{}] locked in current transaction, updating new value", element.getObjectKey());
                    softLock.setNewElement(element);
                    return false;
                } else {
                    LOG.debug("put: key [{}] locked in transaction [{}], waiting until lock gets removed", element.getObjectKey(), softLock.getTransactionID());
                    tryLockSoftLock(softLock);
                    LOG.debug("put: key [{}] unlocked, locking it again", element.getObjectKey());
                    softLock = new SoftLock(getCurrentTransactionContext().getTransactionId(), element.getObjectKey(), element);
                    softLockMap.put(key, softLock);
                    getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                    return !underlyingStore.containsKey(key);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Element get(Object key) {
        return getQuiet(key);
    }

    public Element getQuiet(Object key) {
        lock.readLock().lock();
        try {
            SoftLock softLock = softLockMap.get(key);
            if (softLock == null) {
                return underlyingStore.getQuiet(key);
            }

            if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                return softLock.getNewElement();
            } else {
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
                LOG.debug("remove: key [{}] is not locked and is not in cache, nothing to do", key);
                return null;
            } else if (softLock == null && underlyingStore.getQuiet(key) != null) {
                LOG.debug("remove: key [{}] not locked and in cache, locking it now", key);
                softLock = new SoftLock(getCurrentTransactionContext().getTransactionId(), key, null);
                softLockMap.put(key, softLock);
                getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                return underlyingStore.getQuiet(key);
            } else {
                // softLock cannot be null here
                if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                    if (underlyingStore.getQuiet(key) == null) {
                        LOG.debug("remove: key [{}] locked and put in current transaction, removing and unlocking it", key);
                        softLockMap.remove(softLock);
                        getCurrentTransactionContext().unregisterSoftLock(cacheName, this, softLock);
                        softLock.unlock();
                        return softLock.getNewElement();
                    } else {
                        LOG.debug("remove: key [{}] locked in another transaction, removing it", key);
                        Element currentElement = softLock.getNewElement();
                        softLock.setNewElement(null);
                        return currentElement;
                    }
                } else {
                    LOG.debug("remove: element [{}] locked in transaction [{}], waiting until lock gets removed", key, softLock.getTransactionID());
                    tryLockSoftLock(softLock);
                    LOG.debug("remove: key [{}] unlocked, locking it again", key);
                    softLock = new SoftLock(getCurrentTransactionContext().getTransactionId(), key, null);
                    softLockMap.put(key, softLock);
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

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        throw new UnsupportedOperationException();
        //return underlyingStore.putWithWriter(element, writerManager);
    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        throw new UnsupportedOperationException();
        //return underlyingStore.removeWithWriter(key, writerManager);
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        throw new UnsupportedOperationException();
        //return underlyingStore.putIfAbsent(element);
    }

    public Element removeElement(Element element) throws NullPointerException {
        throw new UnsupportedOperationException();
        //return underlyingStore.removeElement(element);
    }

    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        throw new UnsupportedOperationException();
        //return underlyingStore.replace(old, element);
    }

    public Element replace(Element element) throws NullPointerException {
        throw new UnsupportedOperationException();
        //return underlyingStore.replace(element);
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

}
