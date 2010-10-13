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
import net.sf.ehcache.transaction.nonxa.TransactionContext;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.transaction.nonxa.SoftLock;
import net.sf.ehcache.transaction.nonxa.TransactionException;
import net.sf.ehcache.writer.CacheWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Ludovic Orban
 */
public class NonXaTransactionalStore extends AbstractStore {

    private static final Logger LOG = LoggerFactory.getLogger(NonXaTransactionalStore.class.getName());

    private final String cacheName;
    private final Store underlyingStore;
    private final Lock lock = new ReentrantLock();

    public NonXaTransactionalStore(String cacheName, Store underlyingStore) {
        this.cacheName = cacheName;
        this.underlyingStore = underlyingStore;
    }

    public void store(Object key, Element element) throws CacheException {
        if (element != null) {
            underlyingStore.put(element);
        } else {
            underlyingStore.remove(key);
        }
    }

    public boolean put(Element element) throws CacheException {
        lock.lock();
        try {
            Element oldElement;
            while ((oldElement = underlyingStore.getQuiet(element.getKey())) != null && oldElement.getValue() instanceof SoftLock) {
                SoftLock softLock = (SoftLock) oldElement.getValue();

                boolean locked;
                // release the store lock, we don't want all put calls to block
                lock.unlock();
                while (true) {
                    try {
                        locked = softLock.tryLock(getCurrentTransactionContext().getTransactionTimeout());
                        break;
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
                lock.lock();
                if (!locked) {
                    throw new TransactionException("softlock could not be locked before tx timeout: " + softLock);
                }
            }

            // release the softlock as it's going to be GC'ed
            if (oldElement != null && oldElement.getValue() instanceof SoftLock) {
                SoftLock softLock = (SoftLock) oldElement.getValue();
                softLock.unlock();
            }

            // oldElement must be refreshed as another TX may have committed
            oldElement = underlyingStore.getQuiet(element.getKey());

            SoftLock softLock = new SoftLock(getCurrentTransactionContext(), cacheName, oldElement, element);
            LOG.debug("put replacing value with softlock: {}", softLock);
            return underlyingStore.put(new Element(element.getKey(), softLock));
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
            Element element = underlyingStore.getQuiet(key);
            if (element != null && element.getValue() instanceof SoftLock) {
                SoftLock softLock = (SoftLock) element.getValue();

                if (softLock.inContext(getCurrentTransactionContext())) {
                    LOG.debug("get in context, returning new element: {}", softLock.getNewElement());
                    return softLock.getNewElement();
                }

                LOG.debug("get not in context, returning old element: {}", softLock.getOldElement());
                return softLock.getOldElement();
            }

            LOG.debug("no tx, returning actual element: {}", element);
            return element;
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

    public TransactionContext getCurrentTransactionContext() {
        TransactionContext context = TransactionController.getInstance().getCurrentTransactionContext();
        if (context == null) {
            throw new TransactionException("no transaction started");
        }
        return context;
    }

}
