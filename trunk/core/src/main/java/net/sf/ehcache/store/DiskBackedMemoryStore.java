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
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.store.disk.DiskStore;

/**
 * A tiered store using an in-memory cache of elements stored on disk.
 *
 * @author Ludovic Orban
 */
public final class DiskBackedMemoryStore extends FrontEndCacheTier<MemoryStore, DiskStore> {

    private DiskBackedMemoryStore(CacheConfiguration cacheConfiguration, MemoryStore cache, DiskStore authority) {
        super(cache, authority, cacheConfiguration.getCopyStrategy(), cacheConfiguration.isCopyOnWrite(), cacheConfiguration.isCopyOnRead());
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
}
