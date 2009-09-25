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

package net.sf.ehcache.statistics;

import net.sf.ehcache.Statistics;

/**
 * A no-op implementation of {@link SampledCacheUsageStatistics}
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class NullSampledCacheUsageStatistics implements
        SampledCacheUsageStatistics {

    /**
     * {@inheritDoc}
     */
    public long getAverageGetTimeMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementEvictedMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementExpiredMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementPutMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementRemovedMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementUpdatedMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitInMemoryMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitOnDiskMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissExpiredMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissNotFoundMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getStatisticsAccuracy() {
        return Statistics.STATISTICS_ACCURACY_NONE;
    }

    /**
     * {@inheritDoc}
     */
    public String getStatisticsAccuracyDescription() {
        return "None";
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSampledStatisticsEnabled() {
        return false;
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
