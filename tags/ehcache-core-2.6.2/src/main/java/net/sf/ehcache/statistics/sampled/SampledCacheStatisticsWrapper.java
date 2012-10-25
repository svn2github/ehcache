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

package net.sf.ehcache.statistics.sampled;

import net.sf.ehcache.statistics.CacheUsageListener;
import net.sf.ehcache.util.FailSafeTimer;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.SampledCounterConfig;
import net.sf.ehcache.util.counter.sampled.SampledRateCounter;
import net.sf.ehcache.util.counter.sampled.SampledRateCounterConfig;

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
public class SampledCacheStatisticsWrapper implements CacheUsageListener, CacheStatisticsSampler {

    private static final NullSampledCacheStatistics NULL_SAMPLED_CACHE_STATISTICS = new NullSampledCacheStatistics();

    private volatile SampledCacheStatistics delegate;

    private volatile CacheStatisticsSampler samplerDelegate;

    /**
     * Default constructor.
     */
    public SampledCacheStatisticsWrapper() {
        delegate = new NullSampledCacheStatistics();
    }

    /**
     * Enabled sampled statistics with submitted {@link FailSafeTimer} and {@link SampledCounter} default configurations
     *
     * @param timer the {@code FailSafeTimer} for sampling
     */
    public void enableSampledStatistics(FailSafeTimer timer) {
        enableSampledStatistics(new SampledCacheStatisticsImpl(timer));
    }

    /**
     * Enabled sampled statistics with submitted {@link FailSafeTimer} and {@link SampledCounter} the specified configurations
     *
     * @param timer the {@code FailSafeTimer} for sampling
     * @param config the {@code SampledCounterConfig} for sampling
     * @param rateGetConfig the {@code SampledRateCounterConfig} for sampling average time of cache gets
     * @param rateSearchConfig the {@code SampledCounterConfig} for sampling average time of cache searches
     */
    public void enableSampledStatistics(FailSafeTimer timer,
                                        SampledCounterConfig config,
                                        SampledRateCounterConfig rateGetConfig,
                                        SampledRateCounterConfig rateSearchConfig) {
        enableSampledStatistics(new SampledCacheStatisticsImpl(timer, config, rateGetConfig, rateSearchConfig));
    }

    /**
     * Enable sampled statistics collection
     *
     * @param timer
     */
    private void enableSampledStatistics(SampledCacheStatisticsImpl sampledCacheStats) {
        delegate.dispose();
        samplerDelegate = sampledCacheStats;
        delegate = sampledCacheStats;
    }

    /**
     * Disable sampled statistics collection
     */
    public void disableSampledStatistics() {
        delegate.dispose();
        delegate = NULL_SAMPLED_CACHE_STATISTICS;
        samplerDelegate = null;
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


    @Override
    public long getAverageGetTimeNanosMostRecentSample() {
        return delegate.getAverageGetTimeNanosMostRecentSample();
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
        /**/
    }

    /**
     * {@inheritDoc}
     *
     */
    public void notifyGetTimeNanos(long nanos) {
        getDelegateAsListener().notifyGetTimeNanos(nanos);
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

    /**
     * {@inheritDoc}
     */
    public void notifyXaCommit() {
        getDelegateAsListener().notifyXaCommit();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyXaRollback() {
        getDelegateAsListener().notifyXaRollback();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheXaCommitsMostRecentSample() {
        return delegate.getCacheXaCommitsMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheXaRollbacksMostRecentSample() {
        return delegate.getCacheXaRollbacksMostRecentSample();
    }

    @Override
    public SampledCounter getCacheHitSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheHitSample();
    }

    @Override
    public SampledCounter getCacheHitInMemorySample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheHitInMemorySample();
    }

    @Override
    public SampledCounter getCacheHitOffHeapSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheHitOffHeapSample();
    }

    @Override
    public SampledCounter getCacheHitOnDiskSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheHitOnDiskSample();
    }

    @Override
    public SampledCounter getCacheMissSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheMissSample();
    }

    @Override
    public SampledCounter getCacheMissInMemorySample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheHitInMemorySample();
    }

    @Override
    public SampledCounter getCacheMissOffHeapSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheMissOffHeapSample();
    }

    @Override
    public SampledCounter getCacheMissOnDiskSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheMissOnDiskSample();
    }

    @Override
    public SampledCounter getCacheMissExpiredSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheMissExpiredSample();
    }

    @Override
    public SampledCounter getCacheMissNotFoundSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheMissNotFoundSample();
    }

    @Override
    public SampledCounter getCacheElementEvictedSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheElementEvictedSample();
    }

    @Override
    public SampledCounter getCacheElementRemovedSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheElementRemovedSample();
    }

    @Override
    public SampledCounter getCacheElementExpiredSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheElementExpiredSample();
    }

    @Override
    public SampledCounter getCacheElementPutSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheElementPutSample();
    }

    @Override
    public SampledCounter getCacheElementUpdatedSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheElementUpdatedSample();
    }

    @Override
    public SampledRateCounter getAverageGetTimeSample() {
        return samplerDelegate == null ? null : samplerDelegate.getAverageGetTimeSample();
    }

    @Override
    public SampledRateCounter getAverageGetTimeNanosSample() {
        return samplerDelegate == null ? null : samplerDelegate.getAverageGetTimeNanosSample();
    }

    @Override
    public SampledRateCounter getAverageSearchTimeSample() {
        return samplerDelegate == null ? null : samplerDelegate.getAverageSearchTimeSample();
    }

    @Override
    public SampledCounter getSearchesPerSecondSample() {
        return samplerDelegate == null ? null : samplerDelegate.getSearchesPerSecondSample();
    }

    @Override
    public SampledCounter getCacheXaCommitsSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheXaCommitsSample();
    }

    @Override
    public SampledCounter getCacheXaRollbacksSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheXaRollbacksSample();
    }

    @Override
    public int getCacheHitRatioMostRecentSample() {
        return delegate.getCacheHitRatioMostRecentSample();
    }

    @Override
    public SampledCounter getCacheHitRatioSample() {
        return samplerDelegate == null ? null : samplerDelegate.getCacheHitRatioSample();
    }
}
