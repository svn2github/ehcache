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

package net.sf.ehcache.statistics.sampled;

import net.sf.ehcache.statistics.CacheUsageListener;
import net.sf.ehcache.util.FailSafeTimer;

/**
 * An implementation of {@link SampledCacheStatistics} and also implements
 * {@link CacheUsageListener} and depends on the notification received from
 * these to update the stats. Uses separate delegates depending on whether
 * sampled statistics is enabled or not.
 * <p />
 * To collect statistics data, instances of this class should be registered as a
 * {@link CacheUsageListener} to a Cache
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCacheStatisticsWrapper implements CacheUsageListener,
        SampledCacheStatistics {

    private static final NullSampledCacheStatistics NULL_SAMPLED_CACHE_STATISTICS = new NullSampledCacheStatistics();

    private volatile SampledCacheStatistics delegate;

    /**
     * Default constructor.
     */
    public SampledCacheStatisticsWrapper() {
        delegate = new NullSampledCacheStatistics();
    }

    /**
     * Enable sampled statistics collection
     * 
     * @param timer
     */
    public void enableSampledStatistics(FailSafeTimer timer) {
        delegate.dispose();
        delegate = new SampledCacheStatisticsImpl(timer);
    }

    /**
     * Disable sampled statistics collection
     */
    public void disableSampledStatistics() {
        delegate.dispose();
        delegate = NULL_SAMPLED_CACHE_STATISTICS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.sampled.SampledCacheStatistics#isSampledStatisticsEnabled
     *      ()
     */
    public boolean isSampledStatisticsEnabled() {
        return delegate instanceof SampledCacheStatisticsImpl;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.sampled.SampledCacheStatistics#dispose()
     */
    public void dispose() {
        delegate.dispose();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getAverageGetTimeMostRecentSample
     *                                                       ()
     */
    public long getAverageGetTimeMostRecentSample() {
        return delegate.getAverageGetTimeMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getCacheElementEvictedMostRecentSample
     *                                                       ()
     */
    public long getCacheElementEvictedMostRecentSample() {
        return delegate.getCacheElementEvictedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getCacheElementExpiredMostRecentSample
     *                                                       ()
     */
    public long getCacheElementExpiredMostRecentSample() {
        return delegate.getCacheElementExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getCacheElementPutMostRecentSample
     *                                                       ()
     */
    public long getCacheElementPutMostRecentSample() {
        return delegate.getCacheElementPutMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getCacheElementRemovedMostRecentSample
     *                                                       ()
     */
    public long getCacheElementRemovedMostRecentSample() {
        return delegate.getCacheElementRemovedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getCacheElementUpdatedMostRecentSample
     *                                                       ()
     */
    public long getCacheElementUpdatedMostRecentSample() {
        return delegate.getCacheElementUpdatedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getCacheHitInMemoryMostRecentSample
     *                                                       ()
     */
    public long getCacheHitInMemoryMostRecentSample() {
        return delegate.getCacheHitInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.sampled.SampledCacheStatistics#getCacheHitMostRecentSample
     *      ()
     */
    public long getCacheHitMostRecentSample() {
        return delegate.getCacheHitMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getCacheHitOnDiskMostRecentSample
     *                                                       ()
     */
    public long getCacheHitOnDiskMostRecentSample() {
        return delegate.getCacheHitOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getCacheMissExpiredMostRecentSample
     *                                                       ()
     */
    public long getCacheMissExpiredMostRecentSample() {
        return delegate.getCacheMissExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.sampled.SampledCacheStatistics#getCacheMissMostRecentSample
     *      ()
     */
    public long getCacheMissMostRecentSample() {
        return delegate.getCacheMissMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getCacheMissNotFoundMostRecentSample
     *                                                       ()
     */
    public long getCacheMissNotFoundMostRecentSample() {
        return delegate.getCacheMissNotFoundMostRecentSample();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.sampled.SampledCacheStatistics#getStatisticsAccuracy()
     */
    public int getStatisticsAccuracy() {
        return delegate.getStatisticsAccuracy();
    }

    /**
     * {@inheritDoc}
     * 
     * @seenet.sf.ehcache.statistics.SampledCacheStatistics#
     *                                                       getStatisticsAccuracyDescription
     *                                                       ()
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
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementEvicted()
     */
    public void notifyCacheElementEvicted() {
        getDelegateAsListener().notifyCacheElementEvicted();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementExpired()
     */
    public void notifyCacheElementExpired() {
        getDelegateAsListener().notifyCacheElementExpired();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementPut()
     */
    public void notifyCacheElementPut() {
        getDelegateAsListener().notifyCacheElementPut();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementRemoved()
     */
    public void notifyCacheElementRemoved() {
        getDelegateAsListener().notifyCacheElementRemoved();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementUpdated()
     */
    public void notifyCacheElementUpdated() {
        getDelegateAsListener().notifyCacheElementUpdated();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheHitInMemory()
     */
    public void notifyCacheHitInMemory() {
        getDelegateAsListener().notifyCacheHitInMemory();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheHitOnDisk()
     */
    public void notifyCacheHitOnDisk() {
        getDelegateAsListener().notifyCacheHitOnDisk();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissedWithExpired()
     */
    public void notifyCacheMissedWithExpired() {
        getDelegateAsListener().notifyCacheMissedWithExpired();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissedWithNotFound()
     */
    public void notifyCacheMissedWithNotFound() {
        getDelegateAsListener().notifyCacheMissedWithNotFound();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyRemoveAll()
     */
    public void notifyRemoveAll() {
        getDelegateAsListener().notifyRemoveAll();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyStatisticsAccuracyChanged(int)
     */
    public void notifyStatisticsAccuracyChanged(int statisticsAccuracy) {
        getDelegateAsListener().notifyStatisticsAccuracyChanged(
                statisticsAccuracy);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyStatisticsCleared()
     */
    public void notifyStatisticsCleared() {
        getDelegateAsListener().notifyStatisticsCleared();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyStatisticsEnabledChanged(boolean)
     */
    public void notifyStatisticsEnabledChanged(boolean enableStatistics) {
        getDelegateAsListener()
                .notifyStatisticsEnabledChanged(enableStatistics);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyTimeTakenForGet(long)
     */
    public void notifyTimeTakenForGet(long millis) {
        getDelegateAsListener().notifyTimeTakenForGet(millis);
    }

}
