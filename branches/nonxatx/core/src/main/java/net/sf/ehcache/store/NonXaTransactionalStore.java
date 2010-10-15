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
import net.sf.ehcache.store.chm.ConcurrentHashMap;
import net.sf.ehcache.transaction.nonxa.SoftLock;
import net.sf.ehcache.transaction.nonxa.TransactionContext;
import net.sf.ehcache.transaction.nonxa.TransactionException;
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Ludovic Orban
 */
public class NonXaTransactionalStore extends AbstractStore {

    private static final Logger LOG = LoggerFactory.getLogger(NonXaTransactionalStore.class.getName());

    private TransactionController transactionController;
    private final String cacheName;
    private final Store underlyingStore;
    private final Lock lock = new ReentrantLock();
    private final ConcurrentMap<Object, SoftLock> softLockMap = new ConcurrentHashMap<Object, SoftLock>();

    public NonXaTransactionalStore(TransactionController transactionController, String cacheName, Store underlyingStore) {
        this.transactionController = transactionController;
        this.cacheName = cacheName;
        this.underlyingStore = underlyingStore;
    }

    private TransactionContext getCurrentTransactionContext() {
        return transactionController.getCurrentTransactionContext();
    }

    public void store(Object key, Element element) throws CacheException {
        if (element != null) {
            underlyingStore.put(element);
        } else {
            underlyingStore.remove(key);
        }
    }

    public void remove(SoftLock softLock) {
        softLockMap.remove(softLock.getNewElement().getObjectKey());
    }

    public boolean put(Element element) throws CacheException {
        lock.lock();
        try {
            Object key = element.getObjectKey();
            SoftLock softLock = softLockMap.get(key);
            if (softLock == null) {
                softLock = new SoftLock(getCurrentTransactionContext().getTransactionId(), element);
                softLockMap.put(key, softLock);
                getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                return !underlyingStore.containsKey(key);
            } else {
                if (softLock.getTransactionID().equals(getCurrentTransactionContext().getTransactionId())) {
                    softLock.setNewElement(element);
                    return false;
                } else {
                    lock.unlock();
                    while (true) {
                        try {
                            boolean locked = softLock.tryLock(getCurrentTransactionContext().getTransactionTimeout());
                            lock.lock();
                            if (!locked) {
                                throw new TransactionException("deadlock detected on " + softLock);
                            }
                            break;
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }

                    softLock = new SoftLock(getCurrentTransactionContext().getTransactionId(), element);
                    softLockMap.put(key, softLock);
                    getCurrentTransactionContext().registerSoftLock(cacheName, this, softLock);
                    return !underlyingStore.containsKey(key);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return underlyingStore.putWithWriter(element, writerManager);
    }

    public Element get(Object key) {
        return getQuiet(key);
    }

    public Element getQuiet(Object key) {
        lock.lock();
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
            lock.unlock();
        }
    }

    public List getKeys() {
        return underlyingStore.getKeys();
    }

    public Element remove(Object key) {
        return underlyingStore.remove(key);
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

    public void dispose() {
        underlyingStore.dispose();
    }

    public int getSize() {
        return underlyingStore.getSize();
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

    public int getTerracottaClusteredSize() {
        return underlyingStore.getTerracottaClusteredSize();
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

    public boolean containsKey(Object key) {
        return underlyingStore.containsKey(key);
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
