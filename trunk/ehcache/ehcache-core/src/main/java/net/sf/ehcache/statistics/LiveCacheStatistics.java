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
package net.sf.ehcache.statistics;

/**
 * Interface for usage statistics of a Cache.
 *
 * <p />
 * Implementations of this interface is different from {@link net.sf.ehcache.Statistics} in the way that values returned from this interface
 * implementations will reflect the current state of the cache and not a snapshot of the cache when the api's were called (which is the
 * behavior of {@link net.sf.ehcache.Statistics})
 * <p />
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface LiveCacheStatistics {

    /**
     * Returns true if statistics is enabled
     *
     * @return true if statistics is enabled
     */
    boolean isStatisticsEnabled();

    /**
     * The number of times a requested item was found in the cache.
     *
     * @return the number of times a requested item was found in the cache
     */
    long getCacheHitCount();

    /**
     * Number of times a requested item was found in the Memory Store.
     *
     * @return the number of times a requested item was found in memory
     */
    long getInMemoryHitCount();

    /**
     * Number of times a requested item was found in the off-heap store.
     *
     * @return the number of times a requested item was found in off-heap
     */
    long getOffHeapHitCount();

    /**
     * Number of times a requested item was found in the Disk Store.
     *
     * @return the number of times a requested item was found on Disk, or 0 if
     *         there is no disk storage configured.
     */
    long getOnDiskHitCount();

    /**
     * Number of times a requested element was not found in the cache.
     *
     * @return the number of times a requested element was not found in the
     *         cache
     */
    long getCacheMissCount();

    /**
     * Number of times a requested item was not found in the Memory Store.
     *
     * @return the number of times a requested item was not found in memory
     */
    long getInMemoryMissCount();

    /**
     * Number of times a requested item was not found in the off-heap store.
     *
     * @return the number of times a requested item was not found in off-heap
     */
    long getOffHeapMissCount();

    /**
     * Number of times a requested item was not found in the Disk Store.
     *
     * @return the number of times a requested item was not found on Disk, or 0 if
     *         there is no disk storage configured.
     */
    long getOnDiskMissCount();

    /**
     * @return the number of times a requested element was not found in the
     *         cache and the reason being the element already expired
     */
    long getCacheMissCountExpired();

    /**
     * Size of the cache based on current accuracy settings.
     *
     * @return The size of the cache based on current accuracy setting
     */
    long getSize();

    /**
     * Number of elements in the MemoryStore
     *
     * @return the number of elements in memory
     * @deprecated use {@link #getLocalHeapSize()}
     */
    @Deprecated
    long getInMemorySize();

    /**
     * Number of elements in the off-heap store
     *
     * @return the number of elements in off-heap
     * @deprecated use {@link #getLocalOffHeapSize()}
     */
    @Deprecated
    long getOffHeapSize();

    /**
     * Number of elements in the DiskStore
     *
     * @return number of elements on disk
     * @deprecated use {@link #getLocalDiskSize()}
     */
    @Deprecated
    long getOnDiskSize();

    /**
     * Number of entries in the MemoryStore
     *
     * @return the number of elements in memory
     */
    long getLocalHeapSize();

    /**
     * Number of entries in the off-heap store
     *
     * @return the number of elements in off-heap
     */
    long getLocalOffHeapSize();

    /**
     * Number of entries in the DiskStore
     *
     * @return number of elements on disk
     */
    long getLocalDiskSize();

    /**
     * Number of of bytes used by entries in the MemoryStore
     *
     * @return the number of of bytes used by elements in memory
     */
    long getLocalHeapSizeInBytes();

    /**
     * Number of of bytes used by entries in the off-heap store
     *
     * @return the number of of bytes used by elements in off-heap
     */
    long getLocalOffHeapSizeInBytes();

    /**
     * Number of of bytes used by entries in the DiskStore
     *
     * @return number of bytes used by elements on disk
     */
    long getLocalDiskSizeInBytes();

    /**
     * Average time in milli seconds taken to get an element from the cache.
     *
     * @return Average time taken for a get operation in milliseconds
     */
    float getAverageGetTimeMillis();

    /**
     * Number of elements evicted from the cache
     *
     * @return Number of elements evicted from the cache
     */
    long getEvictedCount();

    /**
     * Number of puts that has happened in the cache
     *
     * @return Number of puts
     */
    long getPutCount();

    /**
     * Number of updates that as happened in the cache
     *
     * @return Number of updates
     */
    long getUpdateCount();

    /**
     * Number of elements expired since creation or last clear
     *
     * @return Number of expired elements
     */
    long getExpiredCount();

    /**
     * Number of elements removed since creation or last clear
     *
     * @return Number of elements removed
     */
    long getRemovedCount();

    /**
     * Accurately measuring statistics can be expensive. Returns the current
     * accuracy setting.
     *
     * @return one of Statistics.STATISTICS_ACCURACY_BEST_EFFORT,
     *         Statistics.STATISTICS_ACCURACY_GUARANTEED,
     *         Statistics.STATISTICS_ACCURACY_NONE
     */
    int getStatisticsAccuracy();

    /**
     * Accurately measuring statistics can be expensive. Returns the current
     * accuracy setting.
     *
     * @return a human readable description of the accuracy setting. One of
     *         "None", "Best Effort" or "Guaranteed".
     */
    String getStatisticsAccuracyDescription();

    /**
     * @return the name of the Ehcache
     */
    String getCacheName();

    /**
     * Clears statistics of this cache
     */
    void clearStatistics();

    /**
     * Return minimum time taken for a get operation in the cache in milliseconds
     *
     * @return minimum time taken for a get operation in the cache in milliseconds
     */
    long getMinGetTimeMillis();

    /**
     * Return maximum time taken for a get operation in the cache in milliseconds
     *
     * @return maximum time taken for a get operation in the cache in milliseconds
     */
    long getMaxGetTimeMillis();

    /**
     * Gets the size of the write-behind queue, if any.
     * The value is for all local buckets
     * @return Elements waiting to be processed by the write-behind writer. -1 if no write-behind
     */
    long getWriterQueueLength();

    /**
     * Return the Cache's XAResource commit calls count
     * @return the Cache's XAResource commit calls count
     */
    long getXaCommitCount();

    /**
     * Return the Cache's XAResource rollback calls count
     * @return the Cache's XAResource rollback calls count
     */
    long getXaRollbackCount();

}
