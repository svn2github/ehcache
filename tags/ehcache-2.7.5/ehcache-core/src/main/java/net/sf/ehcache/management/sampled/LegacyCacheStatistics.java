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
package net.sf.ehcache.management.sampled;

/**
 * Interface for <strong>sampled</strong> usage statistics of a Cache
 * 
 * <p />.
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface LegacyCacheStatistics {

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
     * Gets the cache miss count expired.
     *
     * @return the number of times a requested element was not found in the
     * cache and the reason being the element already expired
     */
    long getCacheMissCountExpired();

    /**
     * The ratio of hits to accesses (hits + misses).
     *
     * @return the ratio of hits to (hits + misses) (0 - 100)
     */
    int getCacheHitRatio();

    /**
     * Get the ratio of nonstop timeouts+rejoin timeouts to operations
     * @return the ratio of timeouts to operation (0 - 100)
     */
    int getNonstopTimeoutRatio();

    /**
     * Size of the cache based on current accuracy settings.
     *
     * @return The size of the cache based on current accuracy setting
     */
    long getSize();

    /**
     * Number of elements in the MemoryStore.
     *
     * @return the number of elements in memory
     * @deprecated use {@link #getLocalHeapSize()}
     */
    @Deprecated
    long getInMemorySize();

    /**
     * Number of elements in the off-heap store.
     *
     * @return the number of elements in off-heap
     * @deprecated use {@link #getLocalOffHeapSize()}
     */
    @Deprecated
    long getOffHeapSize();

    /**
     * Number of elements in the DiskStore.
     *
     * @return number of elements on disk
     * @deprecated use {@link #getLocalDiskSize()}
     */
    @Deprecated
    long getOnDiskSize();

    /**
     * Number of entries in the MemoryStore.
     *
     * @return the number of elements in memory
     */
    long getLocalHeapSize();

    /**
     * Number of entries in the off-heap store.
     *
     * @return the number of elements in off-heap
     */
    long getLocalOffHeapSize();

    /**
     * Number of entries in the DiskStore.
     *
     * @return number of elements on disk
     */
    long getLocalDiskSize();

    /**
     * Number of of bytes used by entries in the MemoryStore.
     *
     * @return the number of of bytes used by elements in memory
     */
    long getLocalHeapSizeInBytes();

    /**
     * Number of of bytes used by entries in the off-heap store.
     *
     * @return the number of of bytes used by elements in off-heap
     */
    long getLocalOffHeapSizeInBytes();

    /**
     * Number of of bytes used by entries in the DiskStore.
     *
     * @return number of bytes used by elements on disk
     */
    long getLocalDiskSizeInBytes();

    /**
     * Average time in nanoseconds taken to get an element from the cache.
     *
     * @return Average time taken for a get operation in nanoseconds
     */
    long getAverageGetTimeNanos();

    /**
     * Number of elements evicted from the cache.
     *
     * @return Number of elements evicted from the cache
     */
    long getEvictedCount();

    /**
     * Number of puts that has happened in the cache.
     *
     * @return Number of puts
     */
    long getPutCount();

    /**
     * Number of updates that as happened in the cache.
     *
     * @return Number of updates
     */
    long getUpdateCount();

    /**
     * Number of elements expired since creation or last clear.
     *
     * @return Number of expired elements
     */
    long getExpiredCount();

    /**
     * Number of elements removed since creation or last clear.
     *
     * @return Number of elements removed
     */
    long getRemovedCount();

    /**
     * Count of cluster offline events for this node.
     *
     * @return count
     */
    long getCacheClusterOfflineCount();
    
    /**
     * Count of cluster rejoin events for this node.
     *
     * @return count
     */
    long getCacheClusterRejoinCount();
    
    /**
     * Count of cluster online events for this node.
     *
     * @return count
     */
    long getCacheClusterOnlineCount();

    /**
     * Gets the cache name.
     *
     * @return the name of the Ehcache
     */
    String getCacheName();

    /**
     * Return maximum time taken for a get operation in the cache in nanoseconds.
     *
     * @return maximum time taken for a get operation in the cache in nanoseconds
     */
    Long getMaxGetTimeNanos();

    /**
     * Return minimum time taken for a get operation in the cache in nanoseconds.
     *
     * @return minimum time taken for a get operation in the cache in nanoseconds
     */
    Long getMinGetTimeNanos();

    /**
     * Gets the size of the write-behind queue, if any.
     * The value is for all local buckets
     * @return Elements waiting to be processed by the write-behind writer.
     */
    long getWriterQueueLength();

    /**
     * Return the Cache's XAResource commit calls count.
     *
     * @return the Cache's XAResource commit calls count
     */
    long getXaCommitCount();

    /**
     * Return the Cache's XAResource rollback calls count.
     *
     * @return the Cache's XAResource rollback calls count
     */
    long getXaRollbackCount();

    /**
     * Return the Cache's XAResource recovered XIDs count.
     *
     * @return the Cache's XAResource recovered XIDs count
     */
    long getXaRecoveredCount();
    
    /**
     * Get most recent value for cache hit.
     *
     * @return Most recent sample for cache hit count
     */
    long getCacheHitMostRecentSample();

    /**
     * Get most recent value for in-memory cache hit.
     *
     * @return Most recent sample for cache hit count in memory
     */
    long getCacheHitInMemoryMostRecentSample();

    /**
     * Get most recent value for off-heap cache hit.
     *
     * @return Most recent sample for cache hit count in off-heap
     */
    long getCacheHitOffHeapMostRecentSample();

    /**
     * Get most recent value for on-disk cache hit.
     *
     * @return Most recent sample for cache hit count on disk
     */
    long getCacheHitOnDiskMostRecentSample();

    /**
     * Get most recent value for cache miss.
     *
     * @return Most recent sample for cache miss count
     */
    long getCacheMissMostRecentSample();

    /**
     * Get most recent value for in-memory cache miss.
     *
     * @return Most recent sample for cache miss count in memory
     */
    long getCacheMissInMemoryMostRecentSample();

    /**
     * Get most recent value for off-heap cache miss.
     *
     * @return Most recent sample for cache miss count in off-heap
     */
    long getCacheMissOffHeapMostRecentSample();

    /**
     * Get most recent value for on-disk cache miss.
     *
     * @return Most recent sample for cache miss count on disk
     */
    long getCacheMissOnDiskMostRecentSample();

    /**
     * Get most recent value for cache miss as result of the element getting
     * expired.
     *
     * @return Most recent sample for cache miss count and the reason for miss
     * being the element got expired
     */
    long getCacheMissExpiredMostRecentSample();

    /**
     * Get most recent value for cache miss as result of the element not found
     * in cache.
     *
     * @return Most recent sample for cache miss not found count
     */
    long getCacheMissNotFoundMostRecentSample();

    /**
     * Get most recent value for cache hit ratio.
     *
     * @return Most recent value for cache hit ratio
     */
    int getCacheHitRatioMostRecentSample();

    /**
     * Get most recent value element evicted from cache.
     *
     * @return Most recent sample for element evicted count
     */
    long getCacheElementEvictedMostRecentSample();

    /**
     * Get most recent value element removed from cache.
     *
     * @return Most recent sample for element removed count
     */
    long getCacheElementRemovedMostRecentSample();

    /**
     * Get most recent value element expired from cache.
     *
     * @return Most recent value for element expired count
     */
    long getCacheElementExpiredMostRecentSample();

    /**
     * Get most recent value element puts in the cache.
     *
     * @return Most recent sample for number of element puts
     */
    long getCacheElementPutMostRecentSample();

    /**
     * Get most recent value element updates , i.e. put() on elements with
     * already existing keys in the cache
     *
     * @return Most recent sampled value for element update count
     */
    long getCacheElementUpdatedMostRecentSample();

    /**
     * Get most recent value for average time taken for get() operation in the
     * cache.
     *
     * @return Most recent sample of average get time taken for a get operation
     */
    long getAverageGetTimeNanosMostRecentSample();

    /**
     * Method used to dispose this statistics.
     */
    void dispose();

    /**
     * Get the average search execution time for searches finishing within the last sample period.
     *
     * @return the average search time nanos
     */
    long getAverageSearchTimeNanos();

    /**
     * Get the number of searches that have finished execution in the last second.
     *
     * @return the searches per second
     */
    long getSearchesPerSecond();

    /**
     * Get most recent value of XA commits.
     *
     * @return the cache xa commits most recent sample
     */
    long getCacheXaCommitsMostRecentSample();

    /**
     * Get most recent value of XA rollbacks.
     *
     * @return the cache xa rollbacks most recent sample
     */
    long getCacheXaRollbacksMostRecentSample();

    /**
     * Check if the local heap is measured with ARC or in element count.
     *
     * @return true, if is local heap count based
     */
    boolean isLocalHeapCountBased();

    /**
     * Gets the cache cluster offline most recent sample.
     *
     * @return the cache cluster offline most recent sample
     */
    long getCacheClusterOfflineMostRecentSample();
    
    /**
     * Gets the cache cluster rejoin most recent sample.
     *
     * @return the cache cluster rejoin most recent sample
     */
    long getCacheClusterRejoinMostRecentSample();
    
    /**
     * Gets the cache cluster online most recent sample.
     *
     * @return the cache cluster online most recent sample
     */
    long getCacheClusterOnlineMostRecentSample();
    
    /**
     * Gets the non stop success count.
     *
     * @return the non stop success count
     */
    long getNonStopSuccessCount();
    
    /**
     * Gets the non stop failure count.
     *
     * @return the non stop failure count
     */
    long getNonStopFailureCount();
    
    /**
     * Gets the non stop rejoin timeout count.
     *
     * @return the non stop rejoin timeout count
     */
    long getNonStopRejoinTimeoutCount();
    
    /**
     * Gets the non stop timeout count.
     *
     * @return the non stop timeout count
     */
    long getNonStopTimeoutCount();
    
    /**
     * Gets the non stop success most recent sample.
     *
     * @return the non stop success most recent sample
     */
    long getNonStopSuccessMostRecentSample();
    
    /**
     * Gets the non stop failure most recent sample.
     *
     * @return the non stop failure most recent sample
     */
    long getNonStopFailureMostRecentSample();
    
    /**
     * Gets the non stop rejoin most recent sample.
     *
     * @return the non stop rejoin most recent sample
     */
    long getNonStopRejoinTimeoutMostRecentSample();
    
    /**
     * Gets the non stop timeout most recent sample.
     *
     * @return the non stop timeout most recent sample
     */
    long getNonStopTimeoutMostRecentSample();
}
