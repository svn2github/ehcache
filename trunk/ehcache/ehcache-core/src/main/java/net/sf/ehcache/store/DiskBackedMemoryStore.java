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
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.store.cachingtier.OnHeapCachingTier;
import net.sf.ehcache.store.disk.DiskStore;

/**
 * A tiered store using an in-memory cache of elements stored on disk.
 *
 * @author Ludovic Orban
 */
public abstract class DiskBackedMemoryStore extends AbstractStore {

    /**
     * Create a DiskBackedMemoryStore instance
     * @param cache the cache
     * @param onHeapPool the pool tracking on-heap usage
     * @param onDiskPool the pool tracking on-disk usage
     * @return a DiskBackedMemoryStore instance
     */
    public static Store create(Ehcache cache, Pool onHeapPool, Pool onDiskPool) {
        DiskStore diskStore = createDiskStore(cache, onHeapPool, onDiskPool);

      final OnHeapCachingTier<Object, Element> onHeapCache = OnHeapCachingTier.createOnHeapCache(cache, onHeapPool);
      return CopyingCacheStore.wrapIfCopy(new CacheStore(
          onHeapCache,
            diskStore, cache.getCacheConfiguration()
        ), cache.getCacheConfiguration());
    }

  private static DiskStore createDiskStore(Ehcache cache, Pool onHeapPool, Pool onDiskPool) {
        CacheConfiguration config = cache.getCacheConfiguration();
        if (config.isOverflowToDisk()) {
            return DiskStore.create(cache, onHeapPool, onDiskPool);
        } else {
            throw new CacheException("DiskBackedMemoryStore can only be used for cache overflowing to disk");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return null;
    }
}
