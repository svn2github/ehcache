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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;

/**
 * Abstract class for BootstrapCacheLoader implementers that should alter their load behavior (probably stop loading)
 * whenever the cache being bootstrapped has reached its in-memory limit (off- or on-heap)
 *
 * @author Alex Snaps
 */
public abstract class MemoryLimitedCacheLoader implements BootstrapCacheLoader, Cloneable {

    /**
     * Checks whether the cache has reached the limit configured for in-memory storage
     *
     * @param cache          The cache being loaded and to be checked for limit being reached
     * @param loadedElements amounts of elements loaded so far
     * @return true if on-heap or off-heap limit has been reached, false otherwise
     */
    protected boolean isInMemoryLimitReached(final Ehcache cache, final int loadedElements) {

        long maxBytesInMem;
        long maxElementsInMem;
        final boolean overflowToOffHeap = cache.getCacheConfiguration().isOverflowToOffHeap();
        maxElementsInMem = cache.getCacheConfiguration().getMaxEntriesLocalHeap() == 0
                ? Integer.MAX_VALUE : cache.getCacheConfiguration().getMaxEntriesLocalHeap();
        if (overflowToOffHeap) {
            maxBytesInMem = cache.getCacheConfiguration().getMaxBytesLocalOffHeap();
        } else {
            maxBytesInMem = cache.getCacheConfiguration().getMaxBytesLocalHeap();
        }

        if (maxBytesInMem != 0) {
            final long inMemoryCount = overflowToOffHeap ? cache.getStatistics().getLocalOffHeapSize() : cache.getStatistics().getLocalHeapSize();
            if (inMemoryCount == 0L) {
                return false;
            } else {
                final long inMemorySizeInBytes = overflowToOffHeap ? cache.getStatistics().getLocalOffHeapSizeInBytes() : cache.getStatistics()
                        .getLocalHeapSizeInBytes();
                final long avgSize = inMemorySizeInBytes
                        / inMemoryCount;
                return inMemorySizeInBytes + (avgSize * 2) >= maxBytesInMem;
            }
        } else {
            return loadedElements >= maxElementsInMem;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
