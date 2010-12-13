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

package net.sf.ehcache.statistics.sampled;

import net.sf.ehcache.statistics.CacheUsageListener;
import net.sf.ehcache.util.FailSafeTimer;

/**
 * An implementation of {@link SampledCacheStatistics} and also implements {@link CacheUsageListener} and depends on the notification
 * received from
 * these to update the stats. Uses separate delegates depending on whether
 * sampled statistics is enabled or not.
 * <p />
 * To collect statistics data, instances of this class should be registered as a {@link CacheUsageListener} to a Cache
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCacheStatisticsWrapper implements CacheUsageListener, SampledCacheStatistics {

    private static final NullSampledCacheStatistics NULL_SAMPLED_CACHE_STATISTICS = new NullSampledCacheStatistics();

    private volatile SampledCacheStatistics delegate;

    /**
     * Default constructor.
     */
    public SampledCacheStatisticsWrapper() {
        delegate = new NullSampledCacheStatistics();
    }

    /**
     * Enable sampled statistics collection
     *
     * @param timer
     */
    public void enableSampledStatistics(FailSafeTimer timer) {
        delegate.dispose();
        delegate = new SampledCacheStatisticsImpl(timer);
    }

    /**
     * Disable sampled statistics collection
     */
    public void disableSampledStatistics() {
        delegate.dispose();
        delegate = NULL_SAMPLED_CACHE_STATISTICS;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSampledStatisticsEnabled() {
        return delegate instanceof SampledCacheStatisticsImpl;
    }

    /**
     * {@inheritDoc}
     *
     */
    public void dispose() {
        delegate.dispose();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getAverageGetTimeMostRecentSample() {
        return delegate.getAverageGetTimeMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheElementEvictedMostRecentSample() {
        return delegate.getCacheElementEvictedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheElementExpiredMostRecentSample() {
        return delegate.getCacheElementExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheElementPutMostRecentSample() {
        return delegate.getCacheElementPutMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheElementRemovedMostRecentSample() {
        return delegate.getCacheElementRemovedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheElementUpdatedMostRecentSample() {
        return delegate.getCacheElementUpdatedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheHitInMemoryMostRecentSample() {
        return delegate.getCacheHitInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheHitOffHeapMostRecentSample() {
        return delegate.getCacheHitOffHeapMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheHitMostRecentSample() {
        return delegate.getCacheHitMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheHitOnDiskMostRecentSample() {
        return delegate.getCacheHitOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheMissExpiredMostRecentSample() {
        return delegate.getCacheMissExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheMissMostRecentSample() {
        return delegate.getCacheMissMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheMissInMemoryMostRecentSample() {
        return delegate.getCacheMissInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheMissOffHeapMostRecentSample() {
        return delegate.getCacheMissOffHeapMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheMissOnDiskMostRecentSample() {
        return delegate.getCacheMissOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public long getCacheMissNotFoundMostRecentSample() {
        return delegate.getCacheMissNotFoundMostRecentSample();
    }

    /**
     * {@inheritDoc}
     *
     */
    public int getStatisticsAccuracy() {
        return delegate.getStatisticsAccuracy();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.sampled.SampledCacheStatistics#clearStatistics()
     */
    public void clearStatistics() {
        delegate.clearStatistics();
    }

    /**
     * {@inheritDoc}
     *
     */
    public String getStatisticsAccuracyDescription() {
        return delegate.getStatisticsAccuracyDescription();
    }

    private CacheUsageListener getDelegateAsListener() {
        return (CacheUsageListener) delegate;
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheElementEvicted() {
        getDelegateAsListener().notifyCacheElementEvicted();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheElementExpired() {
        getDelegateAsListener().notifyCacheElementExpired();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheElementPut() {
        getDelegateAsListener().notifyCacheElementPut();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheElementRemoved() {
        getDelegateAsListener().notifyCacheElementRemoved();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheElementUpdated() {
        getDelegateAsListener().notifyCacheElementUpdated();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheHitInMemory() {
        getDelegateAsListener().notifyCacheHitInMemory();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheHitOffHeap() {
        getDelegateAsListener().notifyCacheHitOffHeap();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheHitOnDisk() {
        getDelegateAsListener().notifyCacheHitOnDisk();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheMissedWithExpired() {
        getDelegateAsListener().notifyCacheMissedWithExpired();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheMissedWithNotFound() {
        getDelegateAsListener().notifyCacheMissedWithNotFound();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheMissInMemory() {
        getDelegateAsListener().notifyCacheMissInMemory();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheMissOffHeap() {
        getDelegateAsListener().notifyCacheMissOffHeap();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyCacheMissOnDisk() {
        getDelegateAsListener().notifyCacheMissOnDisk();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyRemoveAll() {
        getDelegateAsListener().notifyRemoveAll();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyStatisticsAccuracyChanged(int statisticsAccuracy) {
        getDelegateAsListener().notifyStatisticsAccuracyChanged(statisticsAccuracy);
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyStatisticsCleared() {
        getDelegateAsListener().notifyStatisticsCleared();
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyStatisticsEnabledChanged(boolean enableStatistics) {
        getDelegateAsListener().notifyStatisticsEnabledChanged(enableStatistics);
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyTimeTakenForGet(long millis) {
        getDelegateAsListener().notifyTimeTakenForGet(millis);
    }

    /**
     * {@inheritDoc}
     */
    public long getAverageSearchTime() {
        return delegate.getAverageSearchTime();
    }

    /**
     * {@inheritDoc}
     */
    public long getSearchesPerSecond() {
        return delegate.getSearchesPerSecond();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheSearch(long executeTime) {
        getDelegateAsListener().notifyCacheSearch(executeTime);
    }
}
