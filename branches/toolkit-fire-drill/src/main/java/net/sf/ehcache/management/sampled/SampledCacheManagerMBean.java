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

package net.sf.ehcache.management.sampled;

import java.util.Map;

/**
 * An MBean for CacheManager exposing sampled cache usage statistics
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface SampledCacheManagerMBean {
    /**
     * CACHES_ENABLED
     */
    public static final String CACHES_ENABLED = "CachesEnabled";
    
    /**
     * CACHES_CLEARED
     */
    public static final String CACHES_CLEARED = "CachesCleared";
    
    /**
     * STATISTICS_RESET
     */
    public static final String STATISTICS_RESET = "StatisticsReset";

    /**
     * STATISTICS_ENABLED
     */
    public static final String STATISTICS_ENABLED = "StatisticsEnabled";
    
    /**
     * Gets the actual name of the cache manager. This may be different from the
     * name used to register this mbean as there can potentially be multiple
     * cache managers with same name
     */
    public String getName();

    /**
     * Gets the name used to register this mbean.
     */
    public String getMBeanRegisteredName();

    /**
     * Gets the status attribute of the Ehcache
     * 
     * @return The status value, as a String from the Status enum class
     */
    public String getStatus();

    /**
     * Enables/disables each cache contained by this CacheManager
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled);
    
    /**
     * Returns if each cache is enabled.
     * 
     * @return boolean indicating that each cache is enabled
     */
    public boolean isEnabled();
    
    /**
     * Shuts down the CacheManager.
     * <p/>
     * If the shutdown occurs on the singleton, then the singleton is removed, so that if a singleton access method is called, a new
     * singleton will be created.
     */
    public void shutdown();

    /**
     * Clears the contents of all caches in the CacheManager, but without
     * removing any caches.
     * <p/>
     * This method is not synchronized. It only guarantees to clear those elements in a cache at the time that the
     * {@link net.sf.ehcache.Ehcache#removeAll()} mehod on each cache is called.
     */
    public void clearAll();

    /**
     * Gets the cache names managed by the CacheManager
     */
    public String[] getCacheNames() throws IllegalStateException;

    /**
     * Get a map of cache name to performance metrics (hits, misses).
     * 
     * @return a map of cache metrics
     */
    public Map<String, long[]> getCacheMetrics();

    /**
     * @return aggregate hit rate
     */
    public long getCacheHitRate();

    /**
     * @return aggregate miss rate
     */
    public long getCacheMissRate();

    /**
     * @return aggregate put rate
     */
    public long getCachePutRate();

    /**
     * @return aggregate update rate
     */
    public long getCacheUpdateRate();

    /**
     * @return aggregate eviction rate
     */
    public long getCacheEvictionRate();

    /**
     * @return aggregate expiration rate
     */
    public long getCacheExpirationRate();

    /**
     * Clears statistics of all caches for the associated cacheManager
     */
    public void clearStatistics();
    
    /**
     * Enable statistics for each cache contained by cacheManager
     */
    public void enableStatistics();
    
    /**
     * Disable statistics for each cache contained by cacheManager
     */
    public void disableStatistics();
    
    /**
     * Enables/disables each contained cache
     */
    public void setStatisticsEnabled(boolean enabled);
    
    /**
     * Returns true iff each contained cache has statistics enabled
     */
    public boolean isStatisticsEnabled();
    
    /**
     * generateActiveConfigDeclaration
     * 
     * @return CacheManager configuration as String
     */
    String generateActiveConfigDeclaration();
    
    /**
     * generateActiveConfigDeclaration
     * 
     * @param cacheName
     * @return Cache configuration as String
     */
    String generateActiveConfigDeclaration(String cacheName); 
}
