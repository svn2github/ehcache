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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.statistics.CacheUsageListener;
import net.sf.ehcache.util.FailSafeTimer;
import net.sf.ehcache.util.counter.CounterConfig;
import net.sf.ehcache.util.counter.CounterManager;
import net.sf.ehcache.util.counter.CounterManagerImpl;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.SampledCounterConfig;
import net.sf.ehcache.util.counter.sampled.SampledRateCounter;
import net.sf.ehcache.util.counter.sampled.SampledRateCounterConfig;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An implementation of {@link SampledCacheStatistics} This also implements {@link CacheUsageListener} and depends on the notification
 * received from
 * these to update the stats
 * <p/>
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCacheStatisticsImpl implements CacheUsageListener, CacheStatisticsSampler {
    private static final SampledCounterConfig DEFAULT_SAMPLED_COUNTER_CONFIG = new SampledCounterConfig(DEFAULT_INTERVAL_SECS,
        DEFAULT_HISTORY_SIZE, true, 0L);
    private static final SampledRateCounterConfig DEFAULT_AVG_GET_COUNTER_CONFIG = new SampledRateCounterConfig(
        DEFAULT_INTERVAL_SECS, DEFAULT_HISTORY_SIZE, true);
    private static final SampledRateCounterConfig DEFAULT_AVG_SEARCH_COUNTER_CONFIG = new SampledRateCounterConfig(
        DEFAULT_SEARCH_INTERVAL_SEC, DEFAULT_HISTORY_SIZE, true);

    private volatile CounterManager counterManager;
    private final SampledCounter cacheHitCount;
    private final SampledCounter cacheHitInMemoryCount;
    private final SampledCounter cacheHitOffHeapCount;
    private final SampledCounter cacheHitOnDiskCount;
    private final SampledCounter cacheMissCount;
    private final SampledCounter cacheMissInMemoryCount;
    private final SampledCounter cacheMissOffHeapCount;
    private final SampledCounter cacheMissOnDiskCount;
    private final SampledCounter cacheMissExpiredCount;
    private final SampledCounter cacheMissNotFoundCount;
    private final SampledCounter cacheElementEvictedCount;
    private final SampledCounter cacheElementRemoved;
    private final SampledCounter cacheElementExpired;
    private final SampledCounter cacheElementPut;
    private final SampledCounter cacheElementUpdated;
    private final SampledCounter cacheSearchCount;
    private final SampledCounter cacheXaCommitCount;
    private final SampledCounter cacheXaRollbackCount;
    private final SampledRateCounter averageGetTime;
    private final SampledRateCounter averageSearchTime;

    private final AtomicBoolean sampledStatisticsEnabled;
    private final AtomicInteger statisticsAccuracy;

    /**
     * The default constructor
     *
     * @param timer
     */
    public SampledCacheStatisticsImpl(FailSafeTimer timer) {
        this(timer, DEFAULT_SAMPLED_COUNTER_CONFIG, DEFAULT_AVG_GET_COUNTER_CONFIG, DEFAULT_AVG_SEARCH_COUNTER_CONFIG);
    }

    /**
     * @param timer
     * @param config
     */
    public SampledCacheStatisticsImpl(FailSafeTimer timer, SampledCounterConfig config) {
        this(timer, config, DEFAULT_AVG_GET_COUNTER_CONFIG, DEFAULT_AVG_SEARCH_COUNTER_CONFIG);
    }

    /**
     * Constructor that accepts a timer which will be used to schedule the
     * sampled counters
     */
    public SampledCacheStatisticsImpl(FailSafeTimer timer,
                                      SampledCounterConfig config,
                                      SampledRateCounterConfig rateGetConfig,
                                      SampledRateCounterConfig rateSearchConfig) {
        counterManager = new CounterManagerImpl(timer);
        cacheHitCount = createSampledCounter(config);
        cacheHitInMemoryCount = createSampledCounter(config);
        cacheHitOffHeapCount = createSampledCounter(config);
        cacheHitOnDiskCount = createSampledCounter(config);
        cacheMissCount = createSampledCounter(config);
        cacheMissInMemoryCount = createSampledCounter(config);
        cacheMissOffHeapCount = createSampledCounter(config);
        cacheMissOnDiskCount = createSampledCounter(config);
        cacheMissExpiredCount = createSampledCounter(config);
        cacheMissNotFoundCount = createSampledCounter(config);
        cacheElementEvictedCount = createSampledCounter(config);
        cacheElementRemoved = createSampledCounter(config);
        cacheElementExpired = createSampledCounter(config);
        cacheElementPut = createSampledCounter(config);
        cacheElementUpdated = createSampledCounter(config);
        cacheSearchCount = createSampledCounter(config);
        cacheXaCommitCount = createSampledCounter(config);
        cacheXaRollbackCount = createSampledCounter(config);

        averageGetTime = (SampledRateCounter)createSampledCounter(rateGetConfig);
        averageSearchTime = (SampledRateCounter)createSampledCounter(rateSearchConfig);

        this.sampledStatisticsEnabled = new AtomicBoolean(true);
        this.statisticsAccuracy = new AtomicInteger(Statistics.STATISTICS_ACCURACY_BEST_EFFORT);
    }

    private SampledCounter createSampledCounter(CounterConfig defaultCounterConfig) {
        return (SampledCounter)counterManager.createCounter(defaultCounterConfig);
    }

    private void incrementIfStatsEnabled(SampledCounter... counters) {
        if (!sampledStatisticsEnabled.get()) {
            return;
        }
        for (SampledCounter counter : counters) {
            counter.increment();
        }
    }

    /**
     * Clears the collected statistics. Resets all counters to zero
     */
    public void clearStatistics() {
        cacheHitCount.getAndReset();
        cacheHitInMemoryCount.getAndReset();
        cacheHitOffHeapCount.getAndReset();
        cacheHitOnDiskCount.getAndReset();
        cacheMissCount.getAndReset();
        cacheMissInMemoryCount.getAndReset();
        cacheMissOffHeapCount.getAndReset();
        cacheMissOnDiskCount.getAndReset();
        cacheMissExpiredCount.getAndReset();
        cacheMissNotFoundCount.getAndReset();
        cacheElementEvictedCount.getAndReset();
        cacheElementRemoved.getAndReset();
        cacheElementExpired.getAndReset();
        cacheElementPut.getAndReset();
        cacheElementUpdated.getAndReset();
        cacheSearchCount.getAndReset();
        cacheXaCommitCount.getAndReset();
        cacheXaRollbackCount.getAndReset();
        averageGetTime.getAndReset();
        averageSearchTime.getAndReset();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheElementEvicted() {
        incrementIfStatsEnabled(cacheElementEvictedCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheHitInMemory() {
        incrementIfStatsEnabled(cacheHitCount, cacheHitInMemoryCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheHitOffHeap() {
        incrementIfStatsEnabled(cacheHitCount, cacheHitOffHeapCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheHitOnDisk() {
        incrementIfStatsEnabled(cacheHitCount, cacheHitOnDiskCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheMissedWithExpired() {
        incrementIfStatsEnabled(cacheMissCount, cacheMissExpiredCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheMissedWithNotFound() {
        incrementIfStatsEnabled(cacheMissCount, cacheMissNotFoundCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheMissInMemory() {
        incrementIfStatsEnabled(cacheMissCount, cacheMissInMemoryCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheMissOffHeap() {
        incrementIfStatsEnabled(cacheMissCount, cacheMissOffHeapCount);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheMissOnDisk() {
        incrementIfStatsEnabled(cacheMissCount, cacheMissOnDiskCount);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        counterManager.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheElementExpired() {
        incrementIfStatsEnabled(cacheElementExpired);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheElementPut() throws CacheException {
        incrementIfStatsEnabled(cacheElementPut);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheElementRemoved() throws CacheException {
        incrementIfStatsEnabled(cacheElementRemoved);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheElementUpdated() throws CacheException {
        incrementIfStatsEnabled(cacheElementUpdated);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyTimeTakenForGet(long millis) {
        if (!sampledStatisticsEnabled.get()) {
            return;
        }
        averageGetTime.increment(millis, 1);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyStatisticsEnabledChanged(boolean enableStatistics) {
        if (!enableStatistics) {
            sampledStatisticsEnabled.set(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyStatisticsAccuracyChanged(int statisticsAccuracyValue) {
        if (Statistics.isValidStatisticsAccuracy(statisticsAccuracyValue)) {
            statisticsAccuracy.set(statisticsAccuracyValue);
            return;
        }
        throw new IllegalArgumentException("Invalid statistics accuracy value: " + statisticsAccuracyValue);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyStatisticsCleared() {
        cacheHitCount.getAndReset();
        cacheHitInMemoryCount.getAndReset();
        cacheHitOffHeapCount.getAndReset();
        cacheHitOnDiskCount.getAndReset();
        cacheMissCount.getAndReset();
        cacheMissInMemoryCount.getAndReset();
        cacheMissOffHeapCount.getAndReset();
        cacheMissOnDiskCount.getAndReset();
        cacheMissExpiredCount.getAndReset();
        cacheMissNotFoundCount.getAndReset();
        cacheElementEvictedCount.getAndReset();
        cacheElementRemoved.getAndReset();
        cacheElementExpired.getAndReset();
        cacheElementPut.getAndReset();
        cacheElementUpdated.getAndReset();
        cacheXaCommitCount.getAndReset();
        cacheXaRollbackCount.getAndReset();
        averageGetTime.getAndReset();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitMostRecentSample() {
        return cacheHitCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getAverageGetTimeMostRecentSample() {
        return averageGetTime.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementEvictedMostRecentSample() {
        return cacheElementEvictedCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitInMemoryMostRecentSample() {
        return cacheHitInMemoryCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitOffHeapMostRecentSample() {
        return cacheHitOffHeapCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitOnDiskMostRecentSample() {
        return cacheHitOnDiskCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissExpiredMostRecentSample() {
        return cacheMissExpiredCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissMostRecentSample() {
        return cacheMissCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissInMemoryMostRecentSample() {
        return cacheMissInMemoryCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissOffHeapMostRecentSample() {
        return cacheMissOffHeapCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissOnDiskMostRecentSample() {
        return cacheMissOnDiskCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissNotFoundMostRecentSample() {
        return cacheMissNotFoundCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementExpiredMostRecentSample() {
        return cacheElementExpired.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementPutMostRecentSample() {
        return cacheElementPut.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementRemovedMostRecentSample() {
        return cacheElementRemoved.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementUpdatedMostRecentSample() {
        return cacheElementUpdated.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatisticsAccuracy() {
        return statisticsAccuracy.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitSample() {
        return cacheHitCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitInMemorySample() {
        return cacheHitInMemoryCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitOffHeapSample() {
        return cacheHitOffHeapCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitOnDiskSample() {
        return cacheHitOnDiskCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissSample() {
        return cacheMissCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissInMemorySample() {
        return cacheMissInMemoryCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissOffHeapSample() {
        return cacheMissOffHeapCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissOnDiskSample() {
        return cacheMissOnDiskCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissExpiredSample() {
        return cacheMissExpiredCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissNotFoundSample() {
        return cacheMissNotFoundCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementEvictedSample() {
        return cacheElementEvictedCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementRemovedSample() {
        return cacheElementRemoved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementExpiredSample() {
        return cacheElementExpired;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementPutSample() {
        return cacheElementPut;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementUpdatedSample() {
        return cacheElementUpdated;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledRateCounter getAverageGetTimeSample() {
        return averageGetTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledRateCounter getAverageSearchTimeSample() {
        return averageSearchTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getSearchesPerSecondSample() {
        return cacheSearchCount;
    }

    /**
     * {@inheritDoc}
     */
    public String getStatisticsAccuracyDescription() {
        int value = statisticsAccuracy.get();
        if (value == 0) {
            return "None";
        } else if (value == 1) {
            return "Best Effort";
        } else {
            return "Guaranteed";
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSampledStatisticsEnabled() {
        return sampledStatisticsEnabled.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getAverageSearchTime() {
        return this.averageSearchTime.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public long getSearchesPerSecond() {
        return cacheSearchCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheXaCommitsSample() {
        return cacheXaCommitCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheXaRollbacksSample() {
        return cacheXaRollbackCount;
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheSearch(long executeTime) {
        this.cacheSearchCount.increment();
        this.averageSearchTime.increment(executeTime, 1);
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheXaCommitsMostRecentSample() {
        return cacheXaCommitCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyXaCommit() {
        incrementIfStatsEnabled(cacheXaCommitCount);
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheXaRollbacksMostRecentSample() {
        return cacheXaRollbackCount.getMostRecentSample().getCounterValue();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyXaRollback() {
        incrementIfStatsEnabled(cacheXaRollbackCount);
    }
}
