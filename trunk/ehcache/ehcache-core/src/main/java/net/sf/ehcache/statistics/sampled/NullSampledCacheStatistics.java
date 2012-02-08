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

import net.sf.ehcache.Statistics;
import net.sf.ehcache.statistics.CacheUsageListener;

/**
 * A no-op implementation of {@link SampledCacheStatistics}
 * <p />
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class NullSampledCacheStatistics implements CacheUsageListener, SampledCacheStatistics {

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
    public long getCacheHitOffHeapMostRecentSample() {
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
    public long getCacheMissInMemoryMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissOffHeapMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissOnDiskMostRecentSample() {
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
     * @see net.sf.ehcache.statistics.sampled.SampledCacheStatistics#dispose()
     */
    public void dispose() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementEvicted()
     */
    public void notifyCacheElementEvicted() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementExpired()
     */
    public void notifyCacheElementExpired() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementPut()
     */
    public void notifyCacheElementPut() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementRemoved()
     */
    public void notifyCacheElementRemoved() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementUpdated()
     */
    public void notifyCacheElementUpdated() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheHitInMemory()
     */
    public void notifyCacheHitInMemory() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheHitOffHeap()
     */
    public void notifyCacheHitOffHeap() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheHitOnDisk()
     */
    public void notifyCacheHitOnDisk() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissedWithExpired()
     */
    public void notifyCacheMissedWithExpired() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissedWithNotFound()
     */
    public void notifyCacheMissedWithNotFound() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissInMemory()
     */
    public void notifyCacheMissInMemory() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissOffHeap()
     */
    public void notifyCacheMissOffHeap() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissOnDisk()
     */
    public void notifyCacheMissOnDisk() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyRemoveAll()
     */
    public void notifyRemoveAll() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyStatisticsAccuracyChanged(int)
     */
    public void notifyStatisticsAccuracyChanged(int statisticsAccuracy) {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyStatisticsCleared()
     */
    public void notifyStatisticsCleared() {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyStatisticsEnabledChanged(boolean)
     */
    public void notifyStatisticsEnabledChanged(boolean enableStatistics) {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyTimeTakenForGet(long)
     */
    public void notifyTimeTakenForGet(long millis) {
        // no-op
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.sampled.SampledCacheStatistics#clearStatistics()
     */
    public void clearStatistics() {
        // no-op

    }

    /**
     * {@inheritDoc}
     */
    public long getAverageSearchTime() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getSearchesPerSecond() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheSearch(long executeTime) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void notifyXaCommit() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void notifyXaRollback() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheXaCommitsMostRecentSample() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheXaRollbacksMostRecentSample() {
        return 0;
    }
}
