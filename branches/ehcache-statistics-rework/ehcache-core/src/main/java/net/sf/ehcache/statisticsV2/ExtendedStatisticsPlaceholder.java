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

public class ExtendedStatisticsPlaceholder implements ExtendedStatistics {
    /**
     * The default interval in seconds for the {@link SampledRateCounter} for recording the average search rate counter
     */
    public static int DEFAULT_SEARCH_INTERVAL_SEC = 10;

    /**
     * The default history size for {@link SampledCounter} objects.
     */
    public static int DEFAULT_HISTORY_SIZE = 30;

    /**
     * The default interval for sampling events for {@link SampledCounter} objects.
     */
    public static int DEFAULT_INTERVAL_SECS = 1;

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getAverageGetTime()
     */
    @Override
    public float getAverageGetTime() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getCacheHitMostRecentSample()
     */
    @Override
    public long getCacheHitMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getCacheMissMostRecentSample()
     */
    @Override
    public long getCacheMissMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getPutCount()
     */
    @Override
    public long getPutCount() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getCacheElementPutMostRecentSample()
     */
    @Override
    public long getCacheElementPutMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getMaxGetTimeMillis()
     */
    @Override
    public long getMaxGetTimeMillis() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.ehcache.statisticsV2.ExtendedStatistics#getMinGetTimeMillis()
     */
    @Override
    public long getMinGetTimeMillis() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheHitInMemoryMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheHitOffHeapMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheHitOnDiskMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheMissInMemoryMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheMissOffHeapMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheMissOnDiskMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheElementUpdatedMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheElementRemovedMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheElementEvictedMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheElementExpiredMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getSearchesPerSecond() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAverageSearchTime() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheXaCommitsMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheXaRollbacksMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAverageGetTimeMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getAverageGetTimeNanosMostRecentSample() {
        throw new UnsupportedOperationException();
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
    public long getCacheMissNotFoundMostRecentSample() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getCacheMissExpiredMostRecentSample() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getCacheHitRatioMostRecentSample() {
      throw new UnsupportedOperationException();
    }

}
