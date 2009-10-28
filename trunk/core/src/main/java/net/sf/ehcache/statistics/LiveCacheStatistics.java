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
    public boolean isStatisticsEnabled();

    /**
     * The number of times a requested item was found in the cache.
     * 
     * @return the number of times a requested item was found in the cache
     */
    public long getCacheHitCount();

    /**
     * Number of times a requested item was found in the Memory Store.
     * 
     * @return the number of times a requested item was found in memory
     */
    public long getInMemoryHitCount();

    /**
     * Number of times a requested item was found in the Disk Store.
     * 
     * @return the number of times a requested item was found on Disk, or 0 if
     *         there is no disk storage configured.
     */
    public long getOnDiskHitCount();

    /**
     * @return the number of times a requested element was not found in the
     *         cache
     */
    public long getCacheMissCount();

    /**
     * @return the number of times a requested element was not found in the
     *         cache and the reason being the element already expired
     */
    public long getCacheMissCountExpired();

    /**
     * Size of the cache based on current accuracy settings.
     * 
     * @return The size of the cache based on currect accuracy setting
     */
    public long getSize();

    /**
     * Number of elements in the MemoryStore
     * 
     * @return the number of elements in memory
     */
    public long getInMemorySize();

    /**
     * Number of elements in the DiskStore
     * 
     * @return number of elements on disk
     */
    public long getOnDiskSize();

    /**
     * Average time in milli seconds taken to get an element from the cache.
     * 
     * @return Average time taken for a get operation in milliseconds
     */
    public float getAverageGetTimeMillis();

    /**
     * Number of elements evicted from the cache
     * 
     * @return Number of elements evicted from the cache
     */
    public long getEvictedCount();

    /**
     * Number of puts that has happened in the cache
     * 
     * @return Number of puts
     */
    public long getPutCount();

    /**
     * Number of updates that as happened in the cache
     * 
     * @return Number of updates
     */
    public long getUpdateCount();

    /**
     * Number of elements expired since creation or last clear
     * 
     * @return Number of expired elements
     */
    public long getExpiredCount();

    /**
     * Number of elements removed since creation or last clear
     * 
     * @return Number of elements removed
     */
    public long getRemovedCount();

    /**
     * Accurately measuring statistics can be expensive. Returns the current
     * accuracy setting.
     * 
     * @return one of Statistics.STATISTICS_ACCURACY_BEST_EFFORT,
     *         Statistics.STATISTICS_ACCURACY_GUARANTEED,
     *         Statistics.STATISTICS_ACCURACY_NONE
     */
    public int getStatisticsAccuracy();

    /**
     * Accurately measuring statistics can be expensive. Returns the current
     * accuracy setting.
     * 
     * @return a human readable description of the accuracy setting. One of
     *         "None", "Best Effort" or "Guaranteed".
     */
    public String getStatisticsAccuracyDescription();

    /**
     * @return the name of the Ehcache
     */
    public String getCacheName();

}
