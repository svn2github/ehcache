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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

/**
 * An implementation of {@link SampledCacheStatistics} This also implements {@link CacheUsageListener} and depends on the notification
 * received from
 * these to update the stats
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCacheStatisticsImpl implements CacheUsageListener, SampledCacheStatistics {

    private static final int DEFAULT_HISTORY_SIZE = 30;
    private static final int DEFAULT_INTERVAL_SECS = 1;
    private final static SampledCounterConfig DEFAULT_SAMPLED_COUNTER_CONFIG = new SampledCounterConfig(DEFAULT_INTERVAL_SECS,
            DEFAULT_HISTORY_SIZE, true, 0L);
    private final static SampledRateCounterConfig DEFAULT_SAMPLED_RATE_COUNTER_CONFIG = new SampledRateCounterConfig(DEFAULT_INTERVAL_SECS,
            DEFAULT_HISTORY_SIZE, true);

    private volatile CounterManager counterManager;
    private final SampledCounter cacheHitCount;
    private final SampledCounter cacheHitInMemoryCount;
    private final SampledCounter cacheHitOffHeapCount;
    private final SampledCounter cacheHitOnDiskCount;
    private final SampledCounter cacheMissCount;
    private final SampledCounter cacheMissExpiredCount;
    private final SampledCounter cacheMissNotFoundCount;
    private final SampledCounter cacheElementEvictedCount;
    private final SampledCounter cacheElementRemoved;
    private final SampledCounter cacheElementExpired;
    private final SampledCounter cacheElementPut;
    private final SampledCounter cacheElementUpdated;
    private final SampledRateCounter averageGetTime;

    private final AtomicBoolean sampledStatisticsEnabled;
    private final AtomicInteger statisticsAccuracy;

    /**
     * Constructor that accepts a timer which will be used to schedule the
     * sampled counters
     */
    public SampledCacheStatisticsImpl(FailSafeTimer timer) {
        counterManager = new CounterManagerImpl(timer);
        cacheHitCount = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheHitInMemoryCount = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheHitOffHeapCount = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheHitOnDiskCount = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheMissCount = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheMissExpiredCount = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheMissNotFoundCount = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheElementEvictedCount = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheElementRemoved = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheElementExpired = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheElementPut = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);
        cacheElementUpdated = createSampledCounter(DEFAULT_SAMPLED_COUNTER_CONFIG);

        averageGetTime = (SampledRateCounter) createSampledCounter(DEFAULT_SAMPLED_RATE_COUNTER_CONFIG);

        this.sampledStatisticsEnabled = new AtomicBoolean(true);
        this.statisticsAccuracy = new AtomicInteger(Statistics.STATISTICS_ACCURACY_BEST_EFFORT);
    }

    private SampledCounter createSampledCounter(CounterConfig defaultCounterConfig) {
        return (SampledCounter) counterManager.createCounter(defaultCounterConfig);
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
        cacheMissExpiredCount.getAndReset();
        cacheMissNotFoundCount.getAndReset();
        cacheElementEvictedCount.getAndReset();
        cacheElementRemoved.getAndReset();
        cacheElementExpired.getAndReset();
        cacheElementPut.getAndReset();
        cacheElementUpdated.getAndReset();
        averageGetTime.setValue(0, 1);
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
        cacheMissExpiredCount.getAndReset();
        cacheMissNotFoundCount.getAndReset();
        cacheElementEvictedCount.getAndReset();
        cacheElementRemoved.getAndReset();
        cacheElementExpired.getAndReset();
        cacheElementPut.getAndReset();
        cacheElementUpdated.getAndReset();
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

}
