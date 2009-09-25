/**
 *  Copyright 2003-2009 Terracotta, Inc.
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
 * Interface for <strong>sampled</strong> usage statistics of a Cache
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface SampledCacheUsageStatistics {

    /**
     * Get most recent value for cache hit
     * 
     * @return
     */
    public long getCacheHitMostRecentSample();

    /**
     * Get most recent value for in-memory cache hit
     * 
     * @return
     */
    public long getCacheHitInMemoryMostRecentSample();

    /**
     * Get most recent value for on-disk cache hit
     * 
     * @return
     */
    public long getCacheHitOnDiskMostRecentSample();

    /**
     * Get most recent value for cache miss
     * 
     * @return
     */
    public long getCacheMissMostRecentSample();

    /**
     * Get most recent value for cache miss as result of the element getting
     * expired
     * 
     * @return
     */
    public long getCacheMissExpiredMostRecentSample();

    /**
     * Get most recent value for cache miss as result of the element not found
     * in cache
     * 
     * @return
     */
    public long getCacheMissNotFoundMostRecentSample();

    /**
     * Get most recent value element evicted from cache
     * 
     * @return
     */
    public long getCacheElementEvictedMostRecentSample();

    /**
     * Get most recent value element removed from cache
     * 
     * @return
     */
    public long getCacheElementRemovedMostRecentSample();

    /**
     * Get most recent value element expired from cache
     * 
     * @return
     */
    public long getCacheElementExpiredMostRecentSample();

    /**
     * Get most recent value element puts in the cache
     * 
     * @return
     */
    public long getCacheElementPutMostRecentSample();

    /**
     * Get most recent value element updates , i.e. put() on elements with
     * already existing keys in the cache
     * 
     * @return
     */
    public long getCacheElementUpdatedMostRecentSample();

    /**
     * Get most recent value for average time taken for get() operation in the
     * cache
     * 
     * @return
     */
    public long getAverageGetTimeMostRecentSample();

    /**
     * Get value for statisticsAccuracy
     * 
     * @return one of {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT},
     *         {@link Statistics#STATISTICS_ACCURACY_GUARANTEED},
     *         {@link Statistics#STATISTICS_ACCURACY_NONE}
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
     * @return
     */
    public boolean isSampledStatisticsEnabled();

    /**
     * Method used to dispose this statistics
     */
    public void dispose();

}
