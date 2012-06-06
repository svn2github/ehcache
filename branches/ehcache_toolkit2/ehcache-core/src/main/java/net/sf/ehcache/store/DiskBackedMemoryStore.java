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

package net.sf.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.search.impl.SearchManager;
import net.sf.ehcache.store.disk.DiskStore;

import java.io.Serializable;

/**
 * A tiered store using an in-memory cache of elements stored on disk.
 *
 * @author Ludovic Orban
 */
public final class DiskBackedMemoryStore extends FrontEndCacheTier<MemoryStore, DiskStore> {

    private DiskBackedMemoryStore(CacheConfiguration cacheConfiguration, MemoryStore cache, DiskStore authority, SearchManager searchManager) {
        super(cache, authority, cacheConfiguration.getCopyStrategy(), searchManager,
              cacheConfiguration.isCopyOnWrite(), cacheConfiguration.isCopyOnRead());
    }

    /**
     * Create a DiskBackedMemoryStore instance
     * @param cache the cache
     * @param onHeapPool the pool tracking on-heap usage
     * @param onDiskPool the pool tracking on-disk usage
     * @return a DiskBackedMemoryStore instance
     */
    public static Store create(Ehcache cache, Pool onHeapPool, Pool onDiskPool) {
        final MemoryStore memoryStore = createMemoryStore(cache, onHeapPool);
        DiskStore diskStore = createDiskStore(cache, onHeapPool, onDiskPool);

        return new DiskBackedMemoryStore(cache.getCacheConfiguration(), memoryStore, diskStore, null);
    }

    private static MemoryStore createMemoryStore(Ehcache cache, Pool onHeapPool) {
        return MemoryStore.create(cache, onHeapPool);
    }

    private static DiskStore createDiskStore(Ehcache cache, Pool onHeapPool, Pool onDiskPool) {
        CacheConfiguration config = cache.getCacheConfiguration();
        if (config.isOverflowToDisk()) {
            return DiskStore.create(cache, onHeapPool, onDiskPool);
        } else {
            throw new CacheException("DiskBackedMemoryStore can only be used when cache overflows to disk or is disk persistent");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean notifyEvictionFromCache(final Serializable key) {
        return authority.cleanUpFailedMarker(key);
    }
}
