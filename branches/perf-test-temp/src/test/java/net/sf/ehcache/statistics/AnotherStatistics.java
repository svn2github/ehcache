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

package net.sf.ehcache.statistics;

/**
 * Just another statistics class extending from {@link LiveCacheStatisticsImpl}
 * Only for testing as a {@link CacheUsageListener}. The following methods will
 * not work for this class (as it depends on an internal cache) and will throw
 * an NPE :
 * - getCacheName()
 * - getInMemorySize()
 * - getOnDiskSize()
 * - getSize()
 *
 * @author Abhishek Sanoujam
 *
 */
public class AnotherStatistics extends LiveCacheStatisticsImpl implements
        CacheUsageListener {

    /**
     * Default constructor. Passes null in the super(Ehcache) constructor.
     */
    public AnotherStatistics() {
        super(null);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementEvicted()
     */
    public void notifyCacheElementEvicted() {
        super.notifyElementEvicted(null, null);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementExpired()
     */
    public void notifyCacheElementExpired() {
        super.notifyElementExpired(null, null);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementPut()
     */
    public void notifyCacheElementPut() {
        super.notifyElementPut(null, null);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementRemoved()
     */
    public void notifyCacheElementRemoved() {
        super.notifyElementRemoved(null, null);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheElementUpdated()
     */
    public void notifyCacheElementUpdated() {
        super.notifyElementUpdated(null, null);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheHitInMemory()
     */
    public void notifyCacheHitInMemory() {
        super.cacheHitInMemory();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheHitOffHeap()
     */
    public void notifyCacheHitOffHeap() {
        super.cacheHitOffHeap();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheHitOnDisk()
     */
    public void notifyCacheHitOnDisk() {
        super.cacheHitOnDisk();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissedWithExpired()
     */
    public void notifyCacheMissedWithExpired() {
        super.cacheMissExpired();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissedWithNotFound()
     */
    public void notifyCacheMissedWithNotFound() {
        super.cacheMissNotFound();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissInMemory()
     */
    public void notifyCacheMissInMemory() {
        super.cacheMissInMemory();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissOffHeap()
     */
    public void notifyCacheMissOffHeap() {
        super.cacheMissOffHeap();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyCacheMissOnDisk()
     */
    public void notifyCacheMissOnDisk() {
        super.cacheMissOnDisk();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyRemoveAll()
     */
    public void notifyRemoveAll() {
        super.notifyRemoveAll(null);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyStatisticsAccuracyChanged(int)
     */
    public void notifyStatisticsAccuracyChanged(int statisticsAccuracy) {
        super.setStatisticsAccuracy(statisticsAccuracy);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyStatisticsCleared()
     */
    public void notifyStatisticsCleared() {
        super.clearStatistics();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyStatisticsEnabledChanged(boolean)
     */
    public void notifyStatisticsEnabledChanged(boolean enableStatistics) {
        super.setStatisticsEnabled(enableStatistics);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.CacheUsageListener#notifyTimeTakenForGet(long)
     */
    public void notifyTimeTakenForGet(long millis) {
        super.addGetTimeMillis(millis);
    }

    /**
     * {@inheritDoc}
     */
    public void notifyCacheSearch(long executeTime) {
        throw new AssertionError();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyXaCommit() {
        super.xaCommit();
    }

    /**
     * {@inheritDoc}
     */
    public void notifyXaRollback() {
        super.xaRollback();
    }
}
