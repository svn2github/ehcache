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
}
