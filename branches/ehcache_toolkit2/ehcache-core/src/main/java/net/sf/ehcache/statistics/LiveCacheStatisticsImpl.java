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
package net.sf.ehcache.statistics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.writer.CacheWriterManager;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

/**
 * Implementation which can be used both as a {@link LiveCacheStatistics} and {@link LiveCacheStatisticsData}
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class LiveCacheStatisticsImpl implements LiveCacheStatistics, LiveCacheStatisticsData {

    private static final Logger LOG = LoggerFactory.getLogger(LiveCacheStatisticsImpl.class.getName());

    private static final int MIN_MAX_DEFAULT_VALUE = -1;

    private final AtomicBoolean statisticsEnabled = new AtomicBoolean(true);
    private final AtomicLong cacheHitInMemoryCount = new AtomicLong();
    private final AtomicLong cacheHitOffHeapCount = new AtomicLong();
    private final AtomicLong cacheHitOnDiskCount = new AtomicLong();
    private final AtomicLong cacheMissNotFound = new AtomicLong();
    private final AtomicLong cacheMissInMemoryCount = new AtomicLong();
    private final AtomicLong cacheMissOffHeapCount = new AtomicLong();
    private final AtomicLong cacheMissOnDiskCount = new AtomicLong();
    private final AtomicLong cacheMissExpired = new AtomicLong();
    private final AtomicLong cacheElementEvictedCount = new AtomicLong();
    private final AtomicLong totalGetTimeTakenMillis = new AtomicLong();
    private final AtomicLong cacheElementRemoved = new AtomicLong();
    private final AtomicLong cacheElementExpired = new AtomicLong();
    private final AtomicLong cacheElementPut = new AtomicLong();
    private final AtomicLong cacheElementUpdated = new AtomicLong();
    private final AtomicInteger statisticsAccuracy = new AtomicInteger();
    private final AtomicLong minGetTimeMillis = new AtomicLong(MIN_MAX_DEFAULT_VALUE);
    private final AtomicLong maxGetTimeMillis = new AtomicLong(MIN_MAX_DEFAULT_VALUE);
    private final AtomicLong xaCommitCount = new AtomicLong();
    private final AtomicLong xaRollbackCount = new AtomicLong();

    private final List<CacheUsageListener> listeners = new CopyOnWriteArrayList<CacheUsageListener>();

    private final Ehcache cache;

    /**
     * Constructor that accepts the backing {@link Ehcache}, needed for {@link #getSize()}
     *
     * @param cache
     */
    public LiveCacheStatisticsImpl(Ehcache cache) {
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    public void clearStatistics() {
        cacheHitInMemoryCount.set(0);
        cacheHitOffHeapCount.set(0);
        cacheHitOnDiskCount.set(0);
        cacheMissExpired.set(0);
        cacheMissNotFound.set(0);
        cacheMissInMemoryCount.set(0);
        cacheMissOffHeapCount.set(0);
        cacheMissOnDiskCount.set(0);
        cacheElementEvictedCount.set(0);
        totalGetTimeTakenMillis.set(0);
        cacheElementRemoved.set(0);
        cacheElementExpired.set(0);
        cacheElementPut.set(0);
        cacheElementUpdated.set(0);
        minGetTimeMillis.set(MIN_MAX_DEFAULT_VALUE);
        maxGetTimeMillis.set(MIN_MAX_DEFAULT_VALUE);
        xaCommitCount.set(0);
        xaRollbackCount.set(0);
        for (CacheUsageListener l : listeners) {
            l.notifyStatisticsCleared();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void xaCommit() {
        if (!statisticsEnabled.get()) {
            return;
        }
        xaCommitCount.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyXaCommit();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void xaRollback() {
        if (!statisticsEnabled.get()) {
            return;
        }
        xaRollbackCount.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyXaRollback();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return statisticsEnabled.get();
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsEnabled(boolean enableStatistics) {
        if (enableStatistics) {
            clearStatistics();
        }
        statisticsEnabled.set(enableStatistics);
        for (CacheUsageListener l : listeners) {
            l.notifyStatisticsEnabledChanged(enableStatistics);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addGetTimeMillis(long millis) {
        if (!statisticsEnabled.get()) {
            return;
        }
        totalGetTimeTakenMillis.addAndGet(millis);
        for (CacheUsageListener l : listeners) {
            l.notifyTimeTakenForGet(millis);
        }
        if (minGetTimeMillis.get() == MIN_MAX_DEFAULT_VALUE || (millis < minGetTimeMillis.get() /*&& millis > 0*/)) {
            minGetTimeMillis.set(millis);
        }
        if (maxGetTimeMillis.get() == MIN_MAX_DEFAULT_VALUE || (millis > maxGetTimeMillis.get() && millis > 0)) {
            maxGetTimeMillis.set(millis);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cacheHitInMemory() {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheHitInMemoryCount.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheHitInMemory();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cacheHitOffHeap() {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheHitOffHeapCount.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheHitOffHeap();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cacheHitOnDisk() {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheHitOnDiskCount.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheHitOnDisk();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cacheMissExpired() {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheMissExpired.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheMissedWithExpired();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cacheMissNotFound() {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheMissNotFound.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheMissedWithNotFound();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cacheMissInMemory() {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheMissInMemoryCount.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheMissInMemory();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cacheMissOffHeap() {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheMissOffHeapCount.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheMissOffHeap();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cacheMissOnDisk() {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheMissOnDiskCount.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheMissOnDisk();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsAccuracy(int statisticsAccuracy) {
        if (!Statistics.isValidStatisticsAccuracy(statisticsAccuracy)) {
            throw new IllegalArgumentException("Invalid statistics accuracy value: " + statisticsAccuracy);
        }
        this.statisticsAccuracy.set(statisticsAccuracy);
        for (CacheUsageListener l : listeners) {
            l.notifyStatisticsAccuracyChanged(statisticsAccuracy);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        for (CacheUsageListener l : listeners) {
            l.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheElementEvictedCount.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheElementEvicted();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementExpired(Ehcache cache, Element element) {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheElementExpired.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheElementExpired();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheElementPut.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheElementPut();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheElementRemoved.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheElementRemoved();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        if (!statisticsEnabled.get()) {
            return;
        }
        cacheElementUpdated.incrementAndGet();
        for (CacheUsageListener l : listeners) {
            l.notifyCacheElementUpdated();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void notifyRemoveAll(Ehcache cache) {
        if (!statisticsEnabled.get()) {
            return;
        }
        for (CacheUsageListener l : listeners) {
            l.notifyRemoveAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        // to shut up check-style, why do we need this ?
        super.clone();
        throw new CloneNotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageGetTimeMillis() {
        long accessCount = getCacheHitCount() + getCacheMissCount();
        if (accessCount == 0) {
            return 0f;
        }
        return (float) totalGetTimeTakenMillis.get() / accessCount;
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException {
        if (!isStatisticsEnabled()) {
            LOG.warn("Registering a CacheUsageListener on {} whose statistics are currently disabled.  "
                    + "No events will be fired until statistics are enabled.", cache.getName());
        }
        listeners.add(cacheUsageListener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException {
        listeners.remove(cacheUsageListener);
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitCount() {
        return cacheHitInMemoryCount.get() + cacheHitOffHeapCount.get() + cacheHitOnDiskCount.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissCount() {
        return cacheMissNotFound.get() + cacheMissExpired.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemoryMissCount() {
        return cacheMissInMemoryCount.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapMissCount() {
        return cacheMissOffHeapCount.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskMissCount() {
        return cacheMissOnDiskCount.get();
    }


    /**
     * {@inheritDoc}
     */
    public long getCacheMissCountExpired() {
        return cacheMissExpired.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getEvictedCount() {
        return cacheElementEvictedCount.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemoryHitCount() {
        return cacheHitInMemoryCount.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapHitCount() {
        return cacheHitOffHeapCount.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskHitCount() {
        return cacheHitOnDiskCount.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        if (!statisticsEnabled.get()) {
            return 0;
        }
        return cache.getSizeBasedOnAccuracy(statisticsAccuracy.get());
    }

    /**
     * {@inheritDoc}
     * @deprecated see {@link #getLocalHeapSize()}
     */
    @Deprecated
    public long getInMemorySize() {
        return getLocalHeapSize();
    }

    /**
     * {@inheritDoc}
     * @deprecated see {@link #getLocalOffHeapSize()}
     */
    @Deprecated
    public long getOffHeapSize() {
        return getLocalOffHeapSize();
    }

    /**
     * {@inheritDoc}
     * @deprecated see {@link #getLocalDiskSize()}
     */
    @Deprecated
    public long getOnDiskSize() {
        return getLocalDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalHeapSize() {
        if (!statisticsEnabled.get()) {
            return 0;
        }
        return cache.getMemoryStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalOffHeapSize() {
        if (!statisticsEnabled.get()) {
            return 0;
        }
        return cache.getOffHeapStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalDiskSize() {
        if (!statisticsEnabled.get()) {
            return 0;
        }
        return cache.getDiskStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalDiskSizeInBytes() {
        if (!statisticsEnabled.get()) {
            return 0;
        }
        return cache.calculateOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalHeapSizeInBytes() {
        if (!statisticsEnabled.get()) {
            return 0;
        }
        return cache.calculateInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalOffHeapSizeInBytes() {
        if (!statisticsEnabled.get()) {
            return 0;
        }
        return cache.calculateOffHeapSize();
    }


    /**
     * {@inheritDoc}
     */
    public String getCacheName() {
        return cache.getName();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatisticsAccuracy() {
        return statisticsAccuracy.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getExpiredCount() {
        return cacheElementExpired.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getPutCount() {
        return cacheElementPut.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getRemovedCount() {
        return cacheElementRemoved.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getUpdateCount() {
        return cacheElementUpdated.get();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatisticsAccuracyDescription() {
        int value = statisticsAccuracy.get();
        if (value == 0) {
            return "None";
        } else if (value == 1) {
            return "Best Effort";
        } else {
            return "Guaranteed";
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getMaxGetTimeMillis()
     */
    public long getMaxGetTimeMillis() {
        return maxGetTimeMillis.get();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getXaCommitCount()
     */
    public long getXaCommitCount() {
        return xaCommitCount.get();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getXaRollbackCount()
     */
    public long getXaRollbackCount() {
        return xaRollbackCount.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getWriterQueueLength() {
        CacheWriterManager writerManager = cache.getWriterManager();
        if (writerManager instanceof WriteBehindManager) {
            return ((WriteBehindManager)writerManager).getQueueSize();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getMinGetTimeMillis()
     */
    public long getMinGetTimeMillis() {
        return minGetTimeMillis.get();
    }
}
