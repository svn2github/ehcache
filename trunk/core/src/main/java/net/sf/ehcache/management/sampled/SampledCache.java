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
    private final String immutableCacheName;

    /**
     * Constructor accepting the backing {@link Ehcache}
     * 
     * @param cache
     */
    public SampledCache(Ehcache cache) {
        this.cache = cache;
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
    public String getCacheName() {
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
        return cache.getSampledCacheUsageStatistics()
                .getAverageGetTimeMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementEvictedMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheElementEvictedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementExpiredMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheElementExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementPutMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheElementPutMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementRemovedMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheElementRemovedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementUpdatedMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheElementUpdatedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitInMemoryMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheHitInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheHitMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitOnDiskMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheHitOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissExpiredMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheMissExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheMissMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissNotFoundMostRecentSample() {
        return cache.getSampledCacheUsageStatistics()
                .getCacheMissNotFoundMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatisticsAccuracy() {
        return cache.getSampledCacheUsageStatistics().getStatisticsAccuracy();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatisticsAccuracyDescription() {
        return cache.getSampledCacheUsageStatistics()
                .getStatisticsAccuracyDescription();
    }

    /**
     * {@inheritDoc}
     */
    public void clearStatistics() {
        cache.clearStatistics();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return cache.isStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSampledStatisticsEnabled() {
        return cache.getSampledCacheUsageStatistics()
                .isSampledStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTerracottaClustered() {
        return this.cache.getCacheConfiguration().isTerracottaClustered();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#enableStatistics()
     */
    public void enableStatistics() {
        cache.setStatisticsEnabled(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#disableStatistics()
     */
    public void disableStatistics() {
        cache.setStatisticsEnabled(false);

    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#enableSampledStatistics()
     */
    public void enableSampledStatistics() {
        cache.setSampledStatisticsEnabled(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#disableSampledStatistics
     *      ()
     */
    public void disableSampledStatistics() {
        cache.setSampledStatisticsEnabled(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getAverageGetTimeMillis()
     */
    public float getAverageGetTimeMillis() {
        return cache.getCacheUsageStatistics().getAverageGetTimeMillis();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheHitCount()
     */
    public long getCacheHitCount() {
        return cache.getCacheUsageStatistics().getCacheHitCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheMissCount()
     */
    public long getCacheMissCount() {
        return cache.getCacheUsageStatistics().getCacheMissCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheMissCountExpired()
     */
    public long getCacheMissCountExpired() {
        return cache.getCacheUsageStatistics().getCacheMissCountExpired();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigDiskExpiryThreadIntervalSeconds()
     */
    public long getConfigDiskExpiryThreadIntervalSeconds() {
        return cache.getCacheConfiguration()
                .getDiskExpiryThreadIntervalSeconds();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigMaxElementsInMemory()
     */
    public int getConfigMaxElementsInMemory() {
        return cache.getCacheConfiguration().getMaxElementsInMemory();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigMaxElementsOnDisk()
     */
    public int getConfigMaxElementsOnDisk() {
        return cache.getCacheConfiguration().getMaxElementsOnDisk();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigMemoryStoreEvictionPolicy()
     */
    public String getConfigMemoryStoreEvictionPolicy() {
        return cache.getCacheConfiguration().getMemoryStoreEvictionPolicy()
                .toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigTimeToIdleSeconds()
     */
    public long getConfigTimeToIdleSeconds() {
        return cache.getCacheConfiguration().getTimeToIdleSeconds();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigTimeToLiveSeconds()
     */
    public long getConfigTimeToLiveSeconds() {
        return cache.getCacheConfiguration().getTimeToLiveSeconds();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getEvictedCount()
     */
    public long getEvictedCount() {
        return cache.getCacheUsageStatistics().getEvictedCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getExpiredCount()
     */
    public long getExpiredCount() {
        return cache.getCacheUsageStatistics().getExpiredCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getInMemoryHitCount()
     */
    public long getInMemoryHitCount() {
        return cache.getCacheUsageStatistics().getInMemoryHitCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getInMemorySize()
     */
    public long getInMemorySize() {
        return cache.getCacheUsageStatistics().getInMemorySize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOnDiskHitCount()
     */
    public long getOnDiskHitCount() {
        return cache.getCacheUsageStatistics().getOnDiskHitCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOnDiskSize()
     */
    public long getOnDiskSize() {
        return cache.getCacheUsageStatistics().getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getPutCount()
     */
    public long getPutCount() {
        return cache.getCacheUsageStatistics().getPutCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getRemovedCount()
     */
    public long getRemovedCount() {
        return cache.getCacheUsageStatistics().getRemovedCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getSize()
     */
    public long getSize() {
        return cache.getCacheUsageStatistics().getSize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getUpdateCount()
     */
    public long getUpdateCount() {
        return cache.getCacheUsageStatistics().getUpdateCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isConfigDiskPersistent()
     */
    public boolean isConfigDiskPersistent() {
        return cache.getCacheConfiguration().isDiskPersistent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isConfigEternal()
     */
    public boolean isConfigEternal() {
        return cache.getCacheConfiguration().isEternal();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isConfigOverflowToDisk()
     */
    public boolean isConfigOverflowToDisk() {
        return cache.getCacheConfiguration().isOverflowToDisk();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.SampledCacheUsageStatistics#dispose()
     */
    public void dispose() {
        // no-op
    }

}
