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

public interface ExtendedStatistics {

    float getAverageGetTime();

    long getCacheHitMostRecentSample();

    long getCacheMissMostRecentSample();

    long getPutCount();

    long getCacheElementPutMostRecentSample();

    long getCacheMissNotFoundMostRecentSample();

    long getCacheMissExpiredMostRecentSample();

    long getMaxGetTimeMillis();

    long getMinGetTimeMillis();

    long getCacheHitInMemoryMostRecentSample();

    long getCacheHitOffHeapMostRecentSample();

    long getCacheHitOnDiskMostRecentSample();

    long getCacheMissInMemoryMostRecentSample();

    long getCacheMissOffHeapMostRecentSample();

    long getCacheMissOnDiskMostRecentSample();

    long getCacheElementUpdatedMostRecentSample();

    long getCacheElementRemovedMostRecentSample();

    long getCacheElementEvictedMostRecentSample();

    long getCacheElementExpiredMostRecentSample();

    long getSearchesPerSecond();

    long getAverageSearchTime();

    long getCacheXaCommitsMostRecentSample();

    long getCacheXaRollbacksMostRecentSample();

    long getAverageGetTimeMostRecentSample();

    long getAverageGetTimeNanosMostRecentSample();

    int getStatisticsAccuracy();

    String getStatisticsAccuracyDescription();

    boolean isSampledStatisticsEnabled();

    int getCacheHitRatioMostRecentSample();

}