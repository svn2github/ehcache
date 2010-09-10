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
package net.sf.ehcache.statistics.sampled;

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
    public long getCacheHitMostRecentSample();

    /**
     * Get most recent value for in-memory cache hit
     * 
     * @return Most recent sample for cache hit count in memory
     */
    public long getCacheHitInMemoryMostRecentSample();

    /**
     * Get most recent value for off-heap cache hit
     *
     * @return Most recent sample for cache hit count in off-heap
     */
    public long getCacheHitOffHeapMostRecentSample();

    /**
     * Get most recent value for on-disk cache hit
     * 
     * @return Most recent sample for cache hit count on disk
     */
    public long getCacheHitOnDiskMostRecentSample();

    /**
     * Get most recent value for cache miss
     * 
     * @return Most recent sample for cache miss count
     */
    public long getCacheMissMostRecentSample();

    /**
     * Get most recent value for in-memory cache miss
     * 
     * @return Most recent sample for cache miss count in memory
     */
    public long getCacheMissInMemoryMostRecentSample();

    /**
     * Get most recent value for off-heap cache miss
     *
     * @return Most recent sample for cache miss count in off-heap
     */
    public long getCacheMissOffHeapMostRecentSample();

    /**
     * Get most recent value for on-disk cache miss
     * 
     * @return Most recent sample for cache miss count on disk
     */
    public long getCacheMissOnDiskMostRecentSample();
    
    /**
     * Get most recent value for cache miss as result of the element getting
     * expired
     * 
     * @return Most recent sample for cache miss count and the reason for miss
     *         being the element got expired
     */
    public long getCacheMissExpiredMostRecentSample();

    /**
     * Get most recent value for cache miss as result of the element not found
     * in cache
     * 
     * @return Most recent sample for cache miss not found count
     */
    public long getCacheMissNotFoundMostRecentSample();

    /**
     * Get most recent value element evicted from cache
     * 
     * @return Most recent sample for element evicted count
     */
    public long getCacheElementEvictedMostRecentSample();

    /**
     * Get most recent value element removed from cache
     * 
     * @return Most recent sample for element removed count
     */
    public long getCacheElementRemovedMostRecentSample();

    /**
     * Get most recent value element expired from cache
     * 
     * @return Most recent value for element expired count
     */
    public long getCacheElementExpiredMostRecentSample();

    /**
     * Get most recent value element puts in the cache
     * 
     * @return Most recent sample for number of element puts
     */
    public long getCacheElementPutMostRecentSample();

    /**
     * Get most recent value element updates , i.e. put() on elements with
     * already existing keys in the cache
     * 
     * @return Most recent sampled value for element update count
     */
    public long getCacheElementUpdatedMostRecentSample();

    /**
     * Get most recent value for average time taken for get() operation in the
     * cache
     * 
     * @return Most recent sample of average get time taken for a get operation
     */
    public long getAverageGetTimeMostRecentSample();

    /**
     * Get value for statisticsAccuracy
     * 
     * @return one of Statistics#STATISTICS_ACCURACY_BEST_EFFORT,
     *         Statistics#STATISTICS_ACCURACY_GUARANTEED,
     *         Statistics#STATISTICS_ACCURACY_NONE
     */
    public int getStatisticsAccuracy();

    /**
     * Get Description for statisticsAccuracy
     */
    public String getStatisticsAccuracyDescription();

    /**
     * Returns true if statistics collection is enabled for cache, otherwise
     * false
     * 
     * @return true if sampled statistics is enabled, false otherwise
     */
    public boolean isSampledStatisticsEnabled();

    /**
     * Method used to dispose this statistics
     */
    public void dispose();

    /**
     * Clears sampled statistics for this cache
     */
    public void clearStatistics();

}
