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

package net.sf.ehcache.statisticsV2;

import java.util.concurrent.TimeUnit;
import org.terracotta.statistics.derived.EventParameterSimpleMovingAverage;
import org.terracotta.statistics.derived.EventRateSimpleMovingAverage;

public class ExtendedStatisticsPlaceholder implements ExtendedStatistics {
    /**
     * The default interval in seconds for the {@link SampledRateCounter} for recording the average search rate counter
     */
    public static int DEFAULT_SEARCH_INTERVAL_SECS = 10;

    /**
     * The default history size for {@link SampledCounter} objects.
     */
    public static int DEFAULT_HISTORY_SIZE = 30;

    /**
     * The default interval for sampling events for {@link SampledCounter} objects.
     */
    public static int DEFAULT_INTERVAL_SECS = 1;

    private final EventParameterSimpleMovingAverage getLatencyAverage = new EventParameterSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventParameterSimpleMovingAverage searchLatencyAverage = new EventParameterSimpleMovingAverage(DEFAULT_SEARCH_INTERVAL_SECS, TimeUnit.SECONDS);
    
    private final EventRateSimpleMovingAverage hitRateAverage = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventRateSimpleMovingAverage missRateAverage = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventRateSimpleMovingAverage putRateAverage = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    
    private final EventRateSimpleMovingAverage missNotFoundRateAverage = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventRateSimpleMovingAverage missExpiredRateAverage = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    
    private final EventRateSimpleMovingAverage heapMissRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventRateSimpleMovingAverage offheapMissRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);;
    private final EventRateSimpleMovingAverage diskMissRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);;
    
    private final EventRateSimpleMovingAverage offheapHitRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventRateSimpleMovingAverage diskHitRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventRateSimpleMovingAverage expiredRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventRateSimpleMovingAverage evictedRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventRateSimpleMovingAverage removedRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventRateSimpleMovingAverage updateRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    
    private final EventRateSimpleMovingAverage xaRollbackRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    private final EventRateSimpleMovingAverage xaCommitRate = new EventRateSimpleMovingAverage(DEFAULT_INTERVAL_SECS, TimeUnit.SECONDS);
    
    private final EventRateSimpleMovingAverage searchRate = new EventRateSimpleMovingAverage(DEFAULT_SEARCH_INTERVAL_SECS, TimeUnit.SECONDS);
    
    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getAverageGetTime()
     */
    @Override
    public double getAverageGetTime() {
        return getLatencyAverage.average();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getCacheHitMostRecentSample()
     */
    @Override
    public double getCacheHitMostRecentSample() {
        return hitRateAverage.rate(TimeUnit.SECONDS);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getCacheMissMostRecentSample()
     */
    @Override
    public double getCacheMissMostRecentSample() {
        return missRateAverage.rate(TimeUnit.SECONDS);
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getCacheElementPutMostRecentSample()
     */
    @Override
    public double getCacheElementPutMostRecentSample() {
        return putRateAverage.rate(TimeUnit.SECONDS);
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getMaxGetTimeMillis()
     */
    @Override
    public long getMaxGetTimeMillis() {
        return getLatencyAverage.maximum();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getMinGetTimeMillis()
     */
    @Override
    public long getMinGetTimeMillis() {
        return getLatencyAverage.minimum();
    }

    @Override
    public double getCacheHitInMemoryMostRecentSample() {
        return hitRateAverage.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheHitOffHeapMostRecentSample() {
        return offheapHitRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheHitOnDiskMostRecentSample() {
        return diskHitRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheMissInMemoryMostRecentSample() {
        return heapMissRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheMissOffHeapMostRecentSample() {
        return offheapMissRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheMissOnDiskMostRecentSample() {
        return diskMissRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheElementUpdatedMostRecentSample() {
        return updateRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheElementRemovedMostRecentSample() {
        return removedRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheElementEvictedMostRecentSample() {
        return evictedRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheElementExpiredMostRecentSample() {
        return expiredRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getSearchesPerSecond() {
        return searchRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getAverageSearchTime() {
        return searchLatencyAverage.average();
    }

    @Override
    public double getCacheXaCommitsMostRecentSample() {
        return xaCommitRate.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheXaRollbacksMostRecentSample() {
        return xaRollbackRate.rate(TimeUnit.SECONDS);
    }
    
    @Override
    public double getAverageGetTimeMostRecentSample() {
        return getAverageGetTime();
    }

    @Override
    public double getAverageGetTimeNanosMostRecentSample() {
        return getAverageGetTime() * TimeUnit.SECONDS.toNanos(1);
    }

    @Override
    public int getStatisticsAccuracy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getStatisticsAccuracyDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSampledStatisticsEnabled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getCacheMissNotFoundMostRecentSample() {
        return missNotFoundRateAverage.rate(TimeUnit.SECONDS);
    }

    @Override
    public double getCacheMissExpiredMostRecentSample() {
        return missExpiredRateAverage.rate(TimeUnit.SECONDS);
    }

    @Override
    public int getCacheHitRatioMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMaxGetTimeNanos() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getMinGetTimeNanos() {
        throw new UnsupportedOperationException();
    }

}
