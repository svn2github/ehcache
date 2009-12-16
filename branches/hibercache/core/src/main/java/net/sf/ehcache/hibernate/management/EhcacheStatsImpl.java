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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.TabularData;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.management.sampled.SampledCacheManager;

/**
 * Implementation of {@link EhcacheStats}
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public class EhcacheStatsImpl implements EhcacheStats {

    private static final long MILLIS_PER_SECOND = 1000;

    private final SampledCacheManager sampledCacheManager;
    private final CacheManager cacheManager;
    private long statsSince = System.currentTimeMillis();

    /**
     * Constructor accepting the backing {@link CacheManager}
     */
    public EhcacheStatsImpl(CacheManager manager) {
        this.sampledCacheManager = new SampledCacheManager(manager);
        this.cacheManager = manager;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void clearStats() {
        sampledCacheManager.clearStatistics();
        statsSince = System.currentTimeMillis();
    }

    /**
     * {@inheritDoc}
     */
    public void disableStats() {
        setStatisticsEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    public void enableStats() {
        setStatisticsEnabled(true);
    }

    /**
     * {@inheritDoc}
     */
    public void flushRegionCache(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flushRegionCaches() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = this.cacheManager.getCache(name);
            if (cache != null) {
                cache.flush();
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    public String generateActiveConfigDeclaration() {
        throw new UnsupportedOperationException("need to implement");
    }

    /**
     * {@inheritDoc}
     */
    public String generateActiveConfigDeclaration(String region) {
        throw new UnsupportedOperationException("need to implement");
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitCount() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getLiveCacheStatistics().getCacheHitCount();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public double getCacheHitRate() {
        long now = System.currentTimeMillis();
        double deltaSecs = (double) (now - statsSince) / MILLIS_PER_SECOND;
        return getCacheHitCount() / deltaSecs;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitSample() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getSampledCacheStatistics().getCacheHitMostRecentSample();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissCount() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getLiveCacheStatistics().getCacheMissCount();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public double getCacheMissRate() {
        long now = System.currentTimeMillis();
        double deltaSecs = (double) (now - statsSince) / MILLIS_PER_SECOND;
        return getCacheMissCount() / deltaSecs;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissSample() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getSampledCacheStatistics().getCacheMissMostRecentSample();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public long getCachePutCount() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getLiveCacheStatistics().getPutCount();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public double getCachePutRate() {
        long now = System.currentTimeMillis();
        double deltaSecs = (double) (now - statsSince) / MILLIS_PER_SECOND;
        return getCachePutCount() / deltaSecs;
    }

    /**
     * {@inheritDoc}
     */
    public long getCachePutSample() {
        long count = 0;
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                count += cache.getSampledCacheStatistics().getCacheElementPutMostRecentSample();
            }
        }
        return count;
    }

    /**
     * {@inheritDoc}
     */
    public String getOriginalConfigDeclaration() {
        throw new UnsupportedOperationException("need to implement");
    }

    /**
     * {@inheritDoc}
     */
    public String getOriginalConfigDeclaration(String region) {
        throw new UnsupportedOperationException("need to implement");
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Map<String, Object>> getRegionCacheAttributes() {
        throw new UnsupportedOperationException("need to implement");
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Object> getRegionCacheAttributes(String regionName) {
        throw new UnsupportedOperationException("need to implement");
    }

    /**
     * {@inheritDoc}
     */
    public int getRegionCacheMaxTTISeconds(String region) {
        Cache cache = cacheManager.getCache(region);
        if (cache != null) {
            return (int) cache.getCacheConfiguration().getTimeToIdleSeconds();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getRegionCacheMaxTTLSeconds(String region) {
        Cache cache = cacheManager.getCache(region);
        if (cache != null) {
            return (int) cache.getCacheConfiguration().getTimeToLiveSeconds();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getRegionCacheOrphanEvictionPeriod(String region) {
        throw new UnsupportedOperationException("not supported yet");
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, int[]> getRegionCacheSamples() {
        Map<String, int[]> rv = new HashMap<String, int[]>();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                rv.put(name, new int[] {(int) cache.getSampledCacheStatistics().getCacheHitMostRecentSample(),
                        (int) cache.getSampledCacheStatistics().getCacheMissMostRecentSample(),
                        (int) cache.getSampledCacheStatistics().getCacheElementPutMostRecentSample(), });
            }
        }
        return rv;
    }

    /**
     * {@inheritDoc}
     */
    public TabularData getCacheRegionStats() {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public int getRegionCacheTargetMaxInMemoryCount(String region) {
        Cache cache = cacheManager.getCache(region);
        if (cache != null) {
            return cache.getCacheConfiguration().getMaxElementsInMemory();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getRegionCacheTargetMaxTotalCount(String region) {
        Cache cache = cacheManager.getCache(region);
        if (cache != null) {
            return cache.getCacheConfiguration().getMaxElementsInMemory();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getTerracottaHibernateCacheRegionNames() {
        ArrayList<String> rv = new ArrayList<String>();
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                if (cache.getCacheConfiguration().isTerracottaClustered()) {
                    rv.add(name);
                }
            }
        }
        return rv.toArray(new String[] {});
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEvictionStatisticsEnabled(String region) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegionCacheEnabled(String region) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegionCacheLoggingEnabled(String region) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegionCacheOrphanEvictionEnabled(String region) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegionCachesEnabled() {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTerracottaHibernateCache(String region) {
        Cache cache = cacheManager.getCache(region);
        if (cache != null) {
            return cache.getCacheConfiguration().isTerracottaClustered();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setEvictionStatisticsEnabled(String region, boolean flag) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCacheEnabledFlushOnDisable(String region, boolean flag) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCacheEnabledFlushOnEnable(String region, boolean flag) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCacheEnabledNoFlush(String region, boolean flag) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCacheLoggingEnabled(String region, boolean loggingEnabled) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCacheMaxTTISeconds(String region, int maxTTISeconds) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.getCacheConfiguration().setTimeToIdleSeconds(maxTTISeconds);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCacheMaxTTLSeconds(String region, int maxTTLSeconds) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.getCacheConfiguration().setTimeToLiveSeconds(maxTTLSeconds);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCacheOrphanEvictionEnabled(String region, boolean orphanEvictionEnabled) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCacheOrphanEvictionPeriod(String region, int orphanEvictionPeriod) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCachesEnabledFlushOnDisable(boolean flag) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCachesEnabledFlushOnEnable(boolean flag) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCachesEnabledNoFlush(boolean flag) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCacheTargetMaxInMemoryCount(String region, int targetMaxInMemoryCount) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.getCacheConfiguration().setMaxElementsInMemory(targetMaxInMemoryCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRegionCacheTargetMaxTotalCount(String region, int targetMaxTotalCount) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            cache.getCacheConfiguration().setMaxElementsOnDisk(targetMaxTotalCount);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.EhcacheStats#getNumberOfElementsInMemory(java.lang.String)
     */
    public int getNumberOfElementsInMemory(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            return (int) cache.getMemoryStoreSize();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.hibernate.management.EhcacheStats#getNumberOfElementsOnDisk(java.lang.String)
     */
    public int getNumberOfElementsOnDisk(String region) {
        Cache cache = this.cacheManager.getCache(region);
        if (cache != null) {
            return cache.getDiskStoreSize();
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsEnabled(boolean flag) {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.setStatisticsEnabled(flag);
            }
        }
        if (flag) {
            clearStats();
        }
    }

}
