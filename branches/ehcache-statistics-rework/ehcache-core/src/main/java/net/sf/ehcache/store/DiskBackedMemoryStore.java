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
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.store.cachingtier.CountBasedBackEnd;
import net.sf.ehcache.store.cachingtier.HeapCacheBackEnd;
import net.sf.ehcache.store.cachingtier.OnHeapCachingTier;
import net.sf.ehcache.store.cachingtier.PooledBasedBackEnd;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
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

        final HeapCacheBackEnd<Object, Object> memCacheBackEnd;
        final Policy memoryEvictionPolicy = MemoryStore.determineEvictionPolicy(cache);
        if (cache.getCacheConfiguration().isCountBasedTuned()) {
            final long maxEntriesLocalHeap = getCachingTierMaxEntryCount(cache);
            memCacheBackEnd = new CountBasedBackEnd<Object, Object>(maxEntriesLocalHeap, memoryEvictionPolicy);
        } else {
            final PooledBasedBackEnd<Object, Object> pooledBasedBackEnd = new PooledBasedBackEnd<Object, Object>(memoryEvictionPolicy);

            pooledBasedBackEnd.registerAccessor(
                onHeapPool.createPoolAccessor(new PooledBasedBackEnd.PoolParticipant(pooledBasedBackEnd),
                    SizeOfPolicyConfiguration.resolveMaxDepth(cache),
                    SizeOfPolicyConfiguration.resolveBehavior(cache)
                        .equals(SizeOfPolicyConfiguration.MaxDepthExceededBehavior.ABORT)));

            memCacheBackEnd = pooledBasedBackEnd;
        }

        return wrapIfCopy(new CacheStore(
            new OnHeapCachingTier<Object, Element>(
                memCacheBackEnd),
            diskStore, cache.getCacheConfiguration()
        ), cache.getCacheConfiguration());
    }

    private static long getCachingTierMaxEntryCount(final Ehcache cache) {
        final PinningConfiguration pinningConfiguration = cache.getCacheConfiguration().getPinningConfiguration();
        if (pinningConfiguration != null && pinningConfiguration.getStore() != PinningConfiguration.Store.INCACHE) {
            return 0;
        }
        return cache.getCacheConfiguration().getMaxEntriesLocalHeap();
    }

    private static Store wrapIfCopy(final CacheStore diskCacheStore, final CacheConfiguration cacheConfiguration) {
        if (cacheConfiguration.isCopyOnRead() || cacheConfiguration.isCopyOnWrite()) {
            final ReadWriteCopyStrategy<Element> copyStrategyInstance = cacheConfiguration.getCopyStrategyConfiguration()
                .getCopyStrategyInstance();
            return new CopyingCacheStore(diskCacheStore, cacheConfiguration.isCopyOnRead(), cacheConfiguration.isCopyOnWrite(), copyStrategyInstance);
        }
        return diskCacheStore;
    }

    private static MemoryStore createMemoryStore(Ehcache cache, Pool onHeapPool) {
        return MemoryStore.create(cache, onHeapPool);
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
