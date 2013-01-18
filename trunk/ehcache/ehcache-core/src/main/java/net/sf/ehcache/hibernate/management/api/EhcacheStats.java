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

package net.sf.ehcache.hibernate.management.api;

import java.util.Map;

import javax.management.NotificationEmitter;

/**
 * Interface for ehcache related statistics of hibernate second level cache
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public interface EhcacheStats extends NotificationEmitter {
    /**
     * CACHE_ENABLED
     */
    public static final String CACHE_ENABLED = "CacheEnabled";
    
    /**
     * CACHE_REGION_CHANGED
     */
    public static final String CACHE_REGION_CHANGED = "CacheRegionChanged";
    
    /**
     * CACHE_FLUSHED
     */
    public static final String CACHE_FLUSHED = "CacheFlushed";
    
    /**
     * CACHE_REGION_FLUSHED
     */
    public static final String CACHE_REGION_FLUSHED = "CacheRegionFlushed";
    
    /**
     * CACHE_STATISTICS_ENABLED
     */
    public static final String CACHE_STATISTICS_ENABLED = "CacheStatisticsEnabled";
    
    /**
     * CACHE_STATISTICS_RESET
     */
    public static final String CACHE_STATISTICS_RESET = "CacheStatisticsReset";

    /**
     * Get the original cache configuration
     * 
     * @return the original cache configuration
     */
    String getOriginalConfigDeclaration();

    /**
     * Returns the original cache configuration for the supplied region
     * 
     * @param region
     *            for which the configuration is required
     * @return the original cache configuration for the supplied region
     */
    String getOriginalConfigDeclaration(String region);

    /**
     * Returns the currently active cache configuration
     * 
     * @return the currently active cache configuration
     */
    String generateActiveConfigDeclaration();

    /**
     * Returns the currently active cache configuration for the supplied region
     * 
     * @param region
     * @return Returns the currently active cache configuration for the supplied region
     */
    String generateActiveConfigDeclaration(String region);

    /**
     * Returns true if the input region is clustered with terracotta
     * 
     * @param region
     * @return Returns true if the input region is clustered with terracotta
     */
    boolean isTerracottaHibernateCache(String region);

    /**
     * Returns the region names which are clustered with terracotta
     * 
     * @return Returns the region names which are clustered with terracotta
     */
    String[] getTerracottaHibernateCacheRegionNames();

    /**
     * Returns a map containing attributes of the cache for the input cache region name
     * 
     * @param regionName
     * @return Returns a map containing attributes of the cache for the input cache region name
     */
    Map<String, Object> getRegionCacheAttributes(String regionName);

    /**
     * Returns a map containing mapping of all cache region names to their attributes
     * 
     * @return Returns a map containing mapping of all cache region names to their attributes
     */
    Map<String, Map<String, Object>> getRegionCacheAttributes();

    /**
     * Returns true if cache is enabled for the input region
     * 
     * @param region
     * @return Returns true if cache is enabled for the input region
     */
    boolean isRegionCacheEnabled(String region);

    
    /**
     * Enables/disables a particular region
     * 
     * @param region
     * @param enabled
     */
    void setRegionCacheEnabled(String region, boolean enabled);
    
    /**
     * Returns true if all the cache regions are enabled. If even one cache is disabled, it will return false
     * 
     * @return Returns true if all the cache regions are enabled. If even one cache is disabled, it will return false
     */
    boolean isRegionCachesEnabled();

    /**
     * Enable/disable all the cache regions.
     */
    void setRegionCachesEnabled(boolean enabled);
    
    /**
     * Returns the time to idle for the input cache region
     * 
     * @param region
     * @return Returns the time to live for the input cache region
     */
    int getRegionCacheMaxTTISeconds(String region);

    /**
     * Sets the time to idle for the input cache region
     * 
     * @param region
     * @param maxTTISeconds
     *            Returns the time to idle for the input cache region
     */
    void setRegionCacheMaxTTISeconds(String region, int maxTTISeconds);

    /**
     * Returns the time to live for the input cache region
     * 
     * @param region
     * @return Returns the time to live for the input cache region
     */
    int getRegionCacheMaxTTLSeconds(String region);

    /**
     * Sets the time to live for the input cache region
     * 
     * @param region
     * @param maxTTLSeconds
     */
    void setRegionCacheMaxTTLSeconds(String region, int maxTTLSeconds);

    /**
     * Returns the maxElementsInMemory of the input cache region
     * 
     * @param region
     * @return Returns the maxElementsInMemory of the input cache region
     */
    int getRegionCacheTargetMaxInMemoryCount(String region);

    /**
     * Sets the maxElementsInMemory of the input cache region
     * 
     * @param region
     * @param targetMaxInMemoryCount
     */
    void setRegionCacheTargetMaxInMemoryCount(String region, int targetMaxInMemoryCount);

    /**
     * Returns the maxElementsOnDisk of the input cache region
     * 
     * @param region
     * @return Returns the maxElementsOnDisk of the input cache region
     */
    int getRegionCacheTargetMaxTotalCount(String region);

    /**
     * Sets the maxElementsOnDisk of the input cache region
     * 
     * @param region
     * @param targetMaxTotalCount
     */
    void setRegionCacheTargetMaxTotalCount(String region, int targetMaxTotalCount);

    /**
     * Returns true if logging is enabled for the input cache region
     * 
     * @param region
     * @return Returns true if logging is enabled for the input cache region
     */
    boolean isRegionCacheLoggingEnabled(String region);

    /**
     * Enable/Disable logging for the input cache region
     * 
     * @param region
     * @param loggingEnabled
     */
    void setRegionCacheLoggingEnabled(String region, boolean loggingEnabled);

    /**
     * Returns true if orphan eviction is enabled for the region otherwise false
     * 
     * @param region
     * @return Returns true if orphan eviction is enabled for the region otherwise false
     */
    boolean isRegionCacheOrphanEvictionEnabled(String region);

    /**
     * Returns the orphan eviction period for the input cache region.
     * 
     * @param region
     * @return Returns the orphan eviction period for the input cache region.
     */
    int getRegionCacheOrphanEvictionPeriod(String region);

    /**
     * Flushes the cache for the input region
     * 
     * @param region
     */
    void flushRegionCache(String region);

    /**
     * Flushes all the caches for all the regions
     */
    void flushRegionCaches();

    /**
     * Returns hit count for all the caches
     * 
     * @return Returns hit count for all the caches
     */
    long getCacheHitCount();

    /**
     * Returns hit count sample for all the caches
     * 
     * @return Returns hit count sample for all the caches
     */
    long getCacheHitSample();

    /**
     * Returns hit rate for all the caches
     * 
     * @return Returns hit rate for all the caches
     */
    double getCacheHitRate();

    /**
     * Returns miss count for all the caches
     * 
     * @return Returns miss count for all the caches
     */
    long getCacheMissCount();

    /**
     * Returns miss count sample for all the caches
     * 
     * @return Returns miss count sample for all the caches
     */
    long getCacheMissSample();

    /**
     * Returns miss rate for all the caches
     * 
     * @return Returns miss rate for all the caches
     */
    double getCacheMissRate();

    /**
     * Returns put count sample for all the caches
     * 
     * @return Returns put count sample for all the caches
     */
    long getCachePutSample();

    /**
     * Returns put count for all the caches
     * 
     * @return Returns put count for all the caches
     */
    long getCachePutCount();

    /**
     * Returns put rate for all the caches
     * 
     * @return Returns put rate for all the caches
     */
    double getCachePutRate();

    /**
     * Returns a map containing mapping between cache names and an array containing hit, miss and put count samples
     * 
     * @return Returns a map containing mapping between cache names and an array containing hit, miss and put count samples
     */
    Map<String, int[]> getRegionCacheSamples();

    /**
     * Returns number of elements in-memory in the cache for the input region
     * 
     * @param region
     * @return Returns number of elements in-memory in the cache for the input region
     */
    int getNumberOfElementsInMemory(String region);

    /**
     * Returns number of elements off-heap in the cache for the input region
     *
     * @param region
     * @return Returns number of elements off-heap in the cache for the input region
     */
    int getNumberOfElementsOffHeap(String region);

    /**
     * Returns number of elements on-disk in the cache for the input region
     * 
     * @param region
     * @return Returns number of elements on-disk in the cache for the input region
     */
    int getNumberOfElementsOnDisk(String region);

    /**
     * Return minimum time taken for a get operation in the cache in milliseconds
     * 
     * @return minimum time taken for a get operation in the cache in milliseconds
     */
    public long getMinGetTimeMillis();

    /**
     * Return maximum time taken in milliseconds for a get operation
     * 
     * @return Return maximum time taken in milliseconds for a get operation
     */
    public long getMaxGetTimeMillis();

    /**
     * Return average time taken in milliseconds for a get operation for the input cache name
     * 
     * @return Return average time taken in milliseconds for a get operation for the input cache name
     */
    public float getAverageGetTimeMillis(String region);

    /**
     * Return minimum time taken in milliseconds for a get operation for the input cache name
     * 
     * @return Return minimum time taken in milliseconds for a get operation for the input cache name
     */
    public long getMinGetTimeMillis(String cacheName);

    /**
     * Return maximum time taken in milliseconds for a get operation for the input cache name
     * 
     * @return Return maximum time taken in milliseconds for a get operation for the input cache name
     */
    public long getMaxGetTimeMillis(String cacheName);

}
