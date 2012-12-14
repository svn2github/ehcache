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
 * <p />
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface SampledCacheStatistics {

    /**
     * Get most recent value for cache hit
     *
     * @return Most recent sample for cache hit count
     */
    long getCacheHitMostRecentSample();

    /**
     * Get most recent value for in-memory cache hit
     *
     * @return Most recent sample for cache hit count in memory
     */
    long getCacheHitInMemoryMostRecentSample();

    /**
     * Get most recent value for off-heap cache hit
     *
     * @return Most recent sample for cache hit count in off-heap
     */
    long getCacheHitOffHeapMostRecentSample();

    /**
     * Get most recent value for on-disk cache hit
     *
     * @return Most recent sample for cache hit count on disk
     */
    long getCacheHitOnDiskMostRecentSample();

    /**
     * Get most recent value for cache miss
     *
     * @return Most recent sample for cache miss count
     */
    long getCacheMissMostRecentSample();

    /**
     * Get most recent value for in-memory cache miss
     *
     * @return Most recent sample for cache miss count in memory
     */
    long getCacheMissInMemoryMostRecentSample();

    /**
     * Get most recent value for off-heap cache miss
     *
     * @return Most recent sample for cache miss count in off-heap
     */
    long getCacheMissOffHeapMostRecentSample();

    /**
     * Get most recent value for on-disk cache miss
     *
     * @return Most recent sample for cache miss count on disk
     */
    long getCacheMissOnDiskMostRecentSample();

    /**
     * Get most recent value for cache miss as result of the element getting
     * expired
     *
     * @return Most recent sample for cache miss count and the reason for miss
     *         being the element got expired
     */
    long getCacheMissExpiredMostRecentSample();

    /**
     * Get most recent value for cache miss as result of the element not found
     * in cache
     *
     * @return Most recent sample for cache miss not found count
     */
    long getCacheMissNotFoundMostRecentSample();

    /**
     * Get most recent value for cache hit ratio
     *
     * @return Most recent value for cache hit ratio
     */
    int getCacheHitRatioMostRecentSample();

    /**
     * Get most recent value element evicted from cache
     *
     * @return Most recent sample for element evicted count
     */
    long getCacheElementEvictedMostRecentSample();

    /**
     * Get most recent value element removed from cache
     *
     * @return Most recent sample for element removed count
     */
    long getCacheElementRemovedMostRecentSample();

    /**
     * Get most recent value element expired from cache
     *
     * @return Most recent value for element expired count
     */
    long getCacheElementExpiredMostRecentSample();

    /**
     * Get most recent value element puts in the cache
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
     * cache
     *
     * @return Most recent sample of average get time taken for a get operation
     */
    long getAverageGetTimeNanosMostRecentSample();

    /**
     * Returns true if statistics collection is enabled for cache, otherwise
     * false
     *
     * @return true if sampled statistics is enabled, false otherwise
     */
    boolean isSampledStatisticsEnabled();

    /**
     * Method used to dispose this statistics
     */
    void dispose();

    /**
     * Clears sampled statistics for this cache
     */
    void clearStatistics();

    /**
     * Get the average search execution time for searches finishing within the last sample period
     */
    long getAverageSearchTimeNanos();

    /**
     * Get the number of searches that have finished execution in the last second
     */
    long getSearchesPerSecond();

    /**
     * Get most recent value of XA commits
     */
    long getCacheXaCommitsMostRecentSample();

    /**
     * Get most recent value of XA rollbacks
     */
    long getCacheXaRollbacksMostRecentSample();
}
