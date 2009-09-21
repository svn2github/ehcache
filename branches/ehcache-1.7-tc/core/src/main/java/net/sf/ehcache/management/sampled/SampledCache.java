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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.statistics.SampledCacheUsageStatistics;

/**
 * An implementation of {@link SampledCacheMBean}
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCache implements SampledCacheMBean {

    private final Ehcache cache;
    private final SampledCacheUsageStatistics sampledCacheUsageStatistics;
    private final String immutableCacheName;

    /**
     * Constructor accepting the backing {@link Ehcache}
     * 
     * @param cache
     */
    public SampledCache(Ehcache cache) {
        this.cache = cache;
        this.sampledCacheUsageStatistics = cache
                .getSampledCacheUsageStatistics();
        immutableCacheName = cache.getName();
    }

    /**
     * Method which returns the name of the cache at construction time.
     * Package protected method.
     * 
     * @return
     */
    String getImmutableCacheName() {
        return immutableCacheName;
    }

    /**
     * {@inheritDoc}
     */
    public void flush() {
        cache.flush();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return cache.getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        return cache.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() {
        cache.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    public long getAverageGetTimeMostRecentSample() {
        return sampledCacheUsageStatistics.getAverageGetTimeMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementEvictedMostRecentSample() {
        return sampledCacheUsageStatistics
                .getCacheElementEvictedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementExpiredMostRecentSample() {
        return sampledCacheUsageStatistics
                .getCacheElementExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementPutMostRecentSample() {
        return sampledCacheUsageStatistics.getCacheElementPutMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementRemovedMostRecentSample() {
        return sampledCacheUsageStatistics
                .getCacheElementRemovedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementUpdatedMostRecentSample() {
        return sampledCacheUsageStatistics
                .getCacheElementUpdatedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitInMemoryMostRecentSample() {
        return sampledCacheUsageStatistics
                .getCacheHitInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitMostRecentSample() {
        return sampledCacheUsageStatistics.getCacheHitMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitOnDiskMostRecentSample() {
        return sampledCacheUsageStatistics.getCacheHitOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissExpiredMostRecentSample() {
        return sampledCacheUsageStatistics
                .getCacheMissExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissMostRecentSample() {
        return sampledCacheUsageStatistics.getCacheMissMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissNotFoundMostRecentSample() {
        return sampledCacheUsageStatistics
                .getCacheMissNotFoundMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatisticsAccuracy() {
        return sampledCacheUsageStatistics.getStatisticsAccuracy();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatisticsAccuracyDescription() {
        return sampledCacheUsageStatistics.getStatisticsAccuracyDescription();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return sampledCacheUsageStatistics.isStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTerracottaClustered() {
        return this.cache.getCacheConfiguration().isTerracottaClustered();
    }

}
