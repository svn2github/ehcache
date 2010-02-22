/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

/**
 * 
 */
package net.sf.ehcache.transaction.xa;

import java.io.IOException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.StripedReadWriteLockSync;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * The class has a CacheLockProvider for entry level write methods.
 * It is used to enforce XA Isolation for standalone mode.
 * @author nelrahma
 *
 */
public class SyncAwareStore implements Store {

    private final Store store;
    private final CacheLockProvider cacheLockProvider;

    /**
     * 
     * @param store
     */
    public SyncAwareStore(Store store) {
        this.store = store;
        this.cacheLockProvider = new StripedReadWriteLockSync(StripedReadWriteLockSync.DEFAULT_NUMBER_OF_MUTEXES);
    }
    
    /**
     * does underlying store.put after acquiring write lock for key
     * @param element
     */
    public boolean put(Element element) throws CacheException {
        if (element == null) {
            return true;
        }
        Object key = element.getObjectKey();
        Object value = element.getObjectValue();

        Sync sync = cacheLockProvider.getSyncForKey(key);

        cacheLockProvider.getSyncForKey(key).lock(LockType.WRITE);
        try {
            return store.put(element);
        } finally {
            cacheLockProvider.getSyncForKey(key).lock(LockType.WRITE);
        }
    }

    /**
     * does underlying store.remove after acquiring write lock for key
     * @param key
     */
    public Element remove(Object key) {
        Sync sync = cacheLockProvider.getSyncForKey(key);

        cacheLockProvider.getSyncForKey(key).lock(LockType.WRITE);
        try {
         return this.store.remove(key);
        } finally {
            cacheLockProvider.getSyncForKey(key).lock(LockType.WRITE);
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return this.store.bufferFull();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return this.store.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        this.store.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        this.store.expireElements();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        this.store.flush();
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        return this.store.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Policy getEvictionPolicy() {
        return this.store.getEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return cacheLockProvider;
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getKeyArray() {
        return this.store.getKeyArray();
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        return this.store.getQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return this.store.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getSizeInBytes() {
        return this.getSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return this.store.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        return this.store.getTerracottaClusteredSize();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCacheCoherent() {
        return this.store.isCacheCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterCoherent() {
        return this.store.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeCoherent() {
        return this.store.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return this.store.putWithWriter(element, writerManager);
    }


    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        this.store.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return this.removeWithWriter(key, writerManager);
    }

    /**
     * {@inheritDoc}
     */
    public void setEvictionPolicy(Policy policy) {
        this.store.setEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        this.store.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        this.store.waitUntilClusterCoherent();
    }

}