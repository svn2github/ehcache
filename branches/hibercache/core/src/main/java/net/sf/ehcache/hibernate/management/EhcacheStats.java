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

package net.sf.ehcache.hibernate.management;

import java.util.Map;

import javax.management.openmbean.TabularData;

/**
 * Interface for ehcache related statistics of hibernate second level cache
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public interface EhcacheStats {

    /**
     * Returns true if statistics collection is enabled
     * 
     * @return true if statistics collection is enabled
     */
    boolean isStatisticsEnabled();

    /**
     * Enable/Disable statistics collection for all cache of the related session-factory
     * 
     * @param flag
     */
    void setStatisticsEnabled(boolean flag);

    /**
     * Enables statistics collection
     */
    void enableStats();

    /**
     * Disables statistics collection
     */
    void disableStats();

    /**
     * Clears current statistics, resets all counters to zero
     */
    void clearStats();

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
     * Returns true if eviciton statistics is enabled for the region
     * 
     * @param region
     * @return Returns true if eviciton statistics is enabled
     */
    boolean isEvictionStatisticsEnabled(String region);

    /**
     * Enable/Disable eviction statistics for the region
     * 
     * @param region
     *            name of the region
     * @param flag
     *            true for enable, otherwise disables
     */
    void setEvictionStatisticsEnabled(String region, boolean flag);

    /**
     * Returns true if cache is enabled for the input region
     * 
     * @param region
     * @return Returns true if cache is enabled for the input region
     */
    boolean isRegionCacheEnabled(String region);

    /**
     * Enable/Disables the cache region and does not flush on either operation
     * 
     * @param region
     * @param flag
     *            if true, enables the cache region otherwise disables the cache region
     */
    void setRegionCacheEnabledNoFlush(String region, boolean flag);

    /**
     * Enable/Disable the cache region. Flushes the cache if it is getting enabled
     * 
     * @param region
     * @param flag
     */
    void setRegionCacheEnabledFlushOnEnable(String region, boolean flag);

    /**
     * Enable/Disable the cache region. Flushes the cache if it is getting disabled
     * 
     * @param region
     * @param flag
     */
    void setRegionCacheEnabledFlushOnDisable(String region, boolean flag);

    /**
     * Returns true if all the cache regions are enabled
     * 
     * @return Returns true if all the cache regions are enabled
     */
    boolean isRegionCachesEnabled();

    /**
     * Enable/Disable all the cache region without flushing the cache contents
     * 
     * @param flag
     */
    void setRegionCachesEnabledNoFlush(boolean flag);

    /**
     * Enable/Disable all the cache regions. If cache region are getting enabled, flushes all the caches
     * 
     * @param flag
     */
    void setRegionCachesEnabledFlushOnEnable(boolean flag);

    /**
     * Enable/Disable all the cache regions. If cache region are getting disabled, flushes all the caches
     * 
     * @param flag
     */
    void setRegionCachesEnabledFlushOnDisable(boolean flag);

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
     * Enable/Disable orphan eviction for the input cache region
     * 
     * @param region
     * @param orphanEvictionEnabled
     */
    void setRegionCacheOrphanEvictionEnabled(String region, boolean orphanEvictionEnabled);

    /**
     * Returns the orphan eviction period for the input cache region.
     * 
     * @param region
     * @return Returns the orphan eviction period for the input cache region.
     */
    int getRegionCacheOrphanEvictionPeriod(String region);

    /**
     * Sets the orphan eviction period for the input cache region.
     * 
     * @param region
     * @param orphanEvictionPeriod
     */
    void setRegionCacheOrphanEvictionPeriod(String region, int orphanEvictionPeriod);

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
     * Returns a {@link TabularData} containing the cache region stats
     * 
     * @return Returns a {@link TabularData} containing the cache region stats
     */
    TabularData getCacheRegionStats();

    /**
     * Returns number of elements in-memory in the cache for the input region
     * 
     * @param region
     * @return Returns number of elements in-memory in the cache for the input region
     */
    int getNumberOfElementsInMemory(String region);

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
