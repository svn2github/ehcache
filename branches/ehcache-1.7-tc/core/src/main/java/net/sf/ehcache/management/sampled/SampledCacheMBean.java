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

package net.sf.ehcache.management.sampled;

/**
 * An MBean for {@link Cache} exposing sampled cache usage statistics
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface SampledCacheMBean {

    /**
     * Removes all cached items.
     */
    void removeAll();

    /**
     * Flushes all cache items from memory to the disk store, and from the
     * DiskStore to disk.
     */
    void flush();

    /**
     * Gets the status attribute of the Cache.
     * 
     * @return The status value from the Status enum class
     */
    String getStatus();

    /**
     * Gets the cache name.
     */
    String getName();
    
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
     *         cache, including expired elements
     */
    public long getCacheMissCount();

    /**
     * @return the number of times a requested element was not found in the
     *         cache, does not include expired elements
     */
    public long getCacheMissCountNotFound();

    /**
     * @return the number of times a requested element was not found in the
     *         cache but had already expired
     */
    public long getCacheMissCountExpired();

    /**
     * Size of the cache based on current accuracy settings.
     * 
     * @return
     */
    public long getSize();

    /**
     * Number of elements in the MemoryStore
     * 
     * @return
     */
    public long getInMemorySize();

    /**
     * Number of elements in the DiskStore
     * 
     * @return
     */
    public long getOnDiskSize();

    /**
     * Average time in milli seconds taken to get an element from the cache.
     * 
     * @return
     */
    public float getAverageGetTimeMillis();

    /**
     * Number of elements evicted from the cache
     * 
     * @return
     */
    public long getEvictedCount();

    /**
     * Number of puts that has happened in the cache
     * 
     * @return
     */
    public long getPutCount();

    /**
     * Number of updates that as happened in the cache
     * 
     * @return
     */
    public long getUpdateCount();

    /**
     * Number of elements expired since creation or last clear
     * 
     * @return
     */
    public long getExpiredCount();

    /**
     * Number of elements removed since creation or last clear
     * 
     * @return
     */
    public long getRemovedCount();
    
    /**
     * Gets the most recent sample for cache hit count
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
    public boolean isStatisticsEnabled();

    /**
     * Returns true if sampled statistics collection is enabled for cache,
     * otherwise
     * false
     * 
     * @return
     */
    public boolean isSampledStatisticsEnabled();

    /**
     * Is the cache configured with Terracotta clustering?
     */
    public boolean isTerracottaClustered();
    
    /**
     * Clear both sampled and cumulative statistics
     */
    public void clearStatistics();

    /**
     * Enables statistics collection
     */
    public void enableStatistics();

    /**
     * Disables statistics collection. Also disables sampled statistics if it is
     * enabled.
     */
    public void disableStatistics();

    /**
     * Enables statistics collection. As it requires that normal statistics
     * collection to be enabled, it enables it if its not already
     */
    public void enableSampledStatistics();

    /**
     * Disables statistics collection
     */
    public void disableSampledStatistics();
    
    /**
     * Configuration property accessor
     */
    public int getConfigMaxElementsInMemory();

    /**
     * Configuration property accessor
     */
    public int getConfigMaxElementsOnDisk();

    /**
     * Configuration property accessor
     * @return a String representation of the policy
     */
    public String getConfigMemoryStoreEvictionPolicy();

    /**
     * Configuration property accessor
     */
    public boolean isConfigEternal();

    /**
     * Configuration property accessor
     */
    public long getConfigTimeToIdleSeconds();

    /**
     * Configuration property accessor
     */
    public long getConfigTimeToLiveSeconds();

    /**
     * Configuration property accessor
     */
    public boolean isConfigOverflowToDisk();

    /**
     * Configuration property accessor
     */
    public boolean isConfigDiskPersistent();

    /**
     * Configuration property accessor
     */
    public long getConfigDiskExpiryThreadIntervalSeconds();

}
