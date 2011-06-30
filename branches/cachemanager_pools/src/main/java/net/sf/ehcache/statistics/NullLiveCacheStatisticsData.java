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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;

/**
 * A no-op implementation which can be used both as a {@link LiveCacheStatistics} and {@link LiveCacheStatisticsData}
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class NullLiveCacheStatisticsData implements LiveCacheStatistics, LiveCacheStatisticsData {

    /**
     * {@inheritDoc}
     */
    public String getCacheName() {
        return "_unknown_";
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageGetTimeMillis() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemoryMissCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapMissCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskMissCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissCountExpired() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getEvictedCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getExpiredCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemoryHitCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * @deprecated see {@link #getLocalHeapSize()}
     */
    @Deprecated
    public long getInMemorySize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapHitCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * @deprecated see {@link #getLocalOffHeapSize()}
     */
    @Deprecated
    public long getOffHeapSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskHitCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * @deprecated see {@link #getLocalDiskSize()}
     */
    @Deprecated
    public long getOnDiskSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalHeapSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalOffHeapSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalDiskSize() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalHeapSizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalOffHeapSizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalDiskSizeInBytes() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getPutCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getRemovedCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
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
    public long getUpdateCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void addGetTimeMillis(long millis) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void cacheHitInMemory() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void cacheHitOffHeap() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void cacheHitOnDisk() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void cacheMissExpired() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void xaCommit() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void xaRollback() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void cacheMissNotFound() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void cacheMissInMemory() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void cacheMissOffHeap() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void cacheMissOnDisk() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void clearStatistics() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */

    public void removeCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsAccuracy(int statisticsAccuracy) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsEnabled(boolean enableStatistics) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(Ehcache cache, Element element) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */

    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll(Ehcache cache) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxGetTimeMillis() {
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * but returns 0, always!
     */
    public long getWriterQueueLength() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getXaCommitCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getXaRollbackCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getMinGetTimeMillis() {
        return 0;
    }
}
