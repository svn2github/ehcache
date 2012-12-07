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

import java.util.List;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.CacheOperationOutcomes;

public interface ExtendedStatistics {
    
    void setStatisticsTimeToDisable(long time, TimeUnit unit);
    
    CompoundOperation<CacheOperationOutcomes.GetOutcome> get();
    CompoundOperation<CacheOperationOutcomes.PutOutcome> put();
    
    interface CompoundOperation<T> {
        Operation component(T result);
    }

    public interface Operation {
        Statistic<Double> rate();
        Latency latency() throws UnsupportedOperationException;
    }
    
    public interface Latency {
        Statistic<Long> minimum();
        Statistic<Long> maximum();
        Statistic<Double> average();
    }
    
    public interface Statistic<T> {
        T value();
        List<Timestamped<T>> history() throws UnsupportedOperationException;
    }
    
    public interface Timestamped<T> {
        T value();
        long timestamp();
    }
    
    double getAverageGetTime();

    double getCacheHitMostRecentSample();

    double getCacheMissMostRecentSample();

    double getCacheElementPutMostRecentSample();

    double getCacheMissNotFoundMostRecentSample();

    double getCacheMissExpiredMostRecentSample();

    long getMaxGetTimeMillis();

    long getMinGetTimeMillis();

    double getCacheHitInMemoryMostRecentSample();

    double getCacheHitOffHeapMostRecentSample();

    double getCacheHitOnDiskMostRecentSample();

    double getCacheMissInMemoryMostRecentSample();

    double getCacheMissOffHeapMostRecentSample();

    double getCacheMissOnDiskMostRecentSample();

    double getCacheElementUpdatedMostRecentSample();

    double getCacheElementRemovedMostRecentSample();

    double getCacheElementEvictedMostRecentSample();

    double getCacheElementExpiredMostRecentSample();

    double getSearchesPerSecond();

    double getAverageSearchTime();

    double getCacheXaCommitsMostRecentSample();

    double getCacheXaRollbacksMostRecentSample();

    double getAverageGetTimeMostRecentSample();

    double getAverageGetTimeNanosMostRecentSample();

    int getStatisticsAccuracy();

    String getStatisticsAccuracyDescription();

    boolean isSampledStatisticsEnabled();

    int getCacheHitRatioMostRecentSample();

    long getMaxGetTimeNanos();

    long getMinGetTimeNanos();

}