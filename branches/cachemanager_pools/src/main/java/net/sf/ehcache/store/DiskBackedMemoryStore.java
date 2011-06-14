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
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.store.disk.DiskStore;

/**
 * A tiered store using an in-memory cache of elements stored on disk.
 *
 * @author Ludovic Orban
 */
public final class DiskBackedMemoryStore extends FrontEndCacheTier<MemoryStore, DiskStore> {

    private final boolean copyOnRead;
    private final boolean copyOnWrite;
    private final ReadWriteCopyStrategy<Element> copyStrategy;

    private final boolean alwaysPutOnHeap;
    private volatile boolean cachePinnedOnHeapOrInMemory;

    private DiskBackedMemoryStore(CacheConfiguration cacheConfiguration, MemoryStore cache, DiskStore authority) {
        super(cache, authority);
        this.cachePinnedOnHeapOrInMemory = determineCachePinnedOnHeapOrInMemory(cacheConfiguration);
        this.alwaysPutOnHeap = getAdvancedBooleanConfigProperty("alwaysPutOnHeap", cacheConfiguration.getName(), false);

        this.copyOnRead = cacheConfiguration.isCopyOnRead();
        this.copyOnWrite = cacheConfiguration.isCopyOnWrite();
        this.copyStrategy = cacheConfiguration.getCopyStrategy();
    }

    private boolean determineCachePinnedOnHeapOrInMemory(CacheConfiguration cacheConfiguration) {
        PinningConfiguration pinningConfiguration = cacheConfiguration.getPinningConfiguration();
        if (pinningConfiguration == null) {
            return false;
        }

        switch (pinningConfiguration.getStorage()) {
            case ONHEAP:
            case INMEMORY:
                return true;

            case INCACHE:
                return false;

            default:
                throw new IllegalArgumentException();
        }
    }

    private static boolean getAdvancedBooleanConfigProperty(String property, String cacheName, boolean defaultValue) {
        String globalPropertyKey = "net.sf.ehcache.store.config." + property;
        String cachePropertyKey = "net.sf.ehcache.store." + cacheName + ".config." + property;
        return Boolean.parseBoolean(System.getProperty(cachePropertyKey, System.getProperty(globalPropertyKey, Boolean.toString(defaultValue))));
    }

    /**
     * Create a DiskBackedMemoryStore instance
     * @param cache the cache
     * @param diskStorePath the path to the folder in which files will be created
     * @param onHeapPool the pool tracking on-heap usage
     * @param onDiskPool the pool tracking on-disk usage
     * @return a DiskBackedMemoryStore instance
     */
    public static Store create(Ehcache cache, String diskStorePath, Pool onHeapPool, Pool onDiskPool) {
        final MemoryStore memoryStore = createMemoryStore(cache, onHeapPool);
        DiskStore diskStore = createDiskStore(cache, diskStorePath, onHeapPool, onDiskPool);

        cache.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter() {
            public void notifyElementEvicted(Ehcache cache, Element element) {
                // if an element can't be serialized by the disk store, it gets evicted
                // and we must propagate the removal to the memory store
                if (element != null) {
                    memoryStore.remove(element.getObjectKey());
                }
            }
        });

        return new DiskBackedMemoryStore(cache.getCacheConfiguration(), memoryStore, diskStore);
    }

    private static MemoryStore createMemoryStore(Ehcache cache, Pool onHeapPool) {
        return MemoryStore.create(cache, onHeapPool);
    }

    private static DiskStore createDiskStore(Ehcache cache, String diskPath, Pool onHeapPool, Pool onDiskPool) {
        CacheConfiguration config = cache.getCacheConfiguration();
        if (config.isDiskPersistent() || config.isOverflowToDisk()) {
            return DiskStore.create(cache, diskPath, onHeapPool, onDiskPool);
        } else {
            throw new CacheException("DiskBackedMemoryStore can only be used when cache overflows to disk or is disk persistent");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Element copyElementForReadIfNeeded(Element element) {
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForRead(element);
        } else if (copyOnRead) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element));
        } else {
            return element;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Element copyElementForWriteIfNeeded(Element element) {
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForWrite(element);
        } else if (copyOnWrite) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element));
        } else {
            return element;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isCacheFull() {
        return !cachePinnedOnHeapOrInMemory && !alwaysPutOnHeap && cache.isFull();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readLock(Object key) {
        authority.readLock(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readUnlock(Object key) {
        authority.readUnlock(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLock(Object key) {
        authority.writeLock(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUnlock(Object key) {
        authority.writeUnlock(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readLock() {
        authority.readLock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readUnlock() {
        authority.readUnlock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLock() {
        authority.writeLock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUnlock() {
        authority.writeUnlock();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        //TODO this might be wrong...
        return authority.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return cache.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        cache.setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return authority.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return authority.getMBean();
    }
}
