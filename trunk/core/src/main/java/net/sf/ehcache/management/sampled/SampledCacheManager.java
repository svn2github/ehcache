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

import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.statistics.SampledCacheUsageStatistics;

/**
 * An implementation of {@link SampledCacheManagerMBean}
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCacheManager implements SampledCacheManagerMBean {

    private final CacheManager cacheManager;
    private String mbeanRegisteredName;
    private volatile boolean mbeanRegisteredNameSet;

    /**
     * Constructor taking the backing {@link CacheManager}
     * 
     * @param cacheManager
     */
    public SampledCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    /**
     * Set the name used to register this mbean. Can be called only once.
     * Package protected method
     */
    void setMBeanRegisteredName(String name) {
        if (mbeanRegisteredNameSet) {
            throw new IllegalStateException("Name used for registering this mbean is already set");
        }
        mbeanRegisteredNameSet = true;
        mbeanRegisteredName = name;
    }

    /**
     * {@inheritDoc}
     */
    public void clearAll() {
        cacheManager.clearAll();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getCacheNames() throws IllegalStateException {
        return cacheManager.getCacheNames();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        return cacheManager.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        //no-op
    }


    /**
     * @return map of cache metrics (hits, misses)
     */
    public Map<String, long[]> getCacheMetrics() {
        Map<String, long[]> result = new HashMap<String, long[]>();
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Cache cache = cacheManager.getCache(cacheName);
            SampledCacheUsageStatistics stats = cache.getSampledCacheUsageStatistics();
            result.put(cacheName, new long[] {stats.getCacheHitMostRecentSample(),
                    stats.getCacheMissMostRecentSample(), stats.getCacheElementPutMostRecentSample(), });
        }
        return result;
    }
    
    /**
     * @return aggregate hit rate
     */
    public long getCacheHitRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Cache cache = cacheManager.getCache(cacheName);
            SampledCacheUsageStatistics stats = cache.getSampledCacheUsageStatistics();
            result += stats.getCacheHitMostRecentSample();
        }
        return result;
    }
  
    /**
     * @return aggregate miss rate
     */
    public long getCacheMissRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Cache cache = cacheManager.getCache(cacheName);
            SampledCacheUsageStatistics stats = cache.getSampledCacheUsageStatistics();
            result += stats.getCacheMissMostRecentSample();
        }
        return result;
    }
 
    /**
     * @return aggregate put rate
     */
    public long getCachePutRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Cache cache = cacheManager.getCache(cacheName);
            SampledCacheUsageStatistics stats = cache.getSampledCacheUsageStatistics();
            result += stats.getCacheElementPutMostRecentSample();
        }
        return result;
    }
   
    /**
     * @return aggregate update rate
     */
    public long getCacheUpdateRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Cache cache = cacheManager.getCache(cacheName);
            SampledCacheUsageStatistics stats = cache.getSampledCacheUsageStatistics();
            result += stats.getCacheElementUpdatedMostRecentSample();
        }
        return result;
    }
    
    /**
     * @return aggregate eviction rate
     */
    public long getCacheEvictionRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Cache cache = cacheManager.getCache(cacheName);
            SampledCacheUsageStatistics stats = cache.getSampledCacheUsageStatistics();
            result += stats.getCacheElementEvictedMostRecentSample();
        }
        return result;
    }
    
    /**
     * @return aggregate expiration rate
     */
    public long getCacheExpirationRate() {
        long result = 0;
        String[] caches = getCacheNames();
        for (String cacheName : caches) {
            Cache cache = cacheManager.getCache(cacheName);
            SampledCacheUsageStatistics stats = cache.getSampledCacheUsageStatistics();
            result += stats.getCacheElementExpiredMostRecentSample();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheManagerMBean#getName()
     */
    public String getName() {
        return cacheManager.getName();
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheManagerMBean#getName()
     */
    public String getMBeanRegisteredName() {
        return this.mbeanRegisteredName;
    }
    
}
