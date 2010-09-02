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

/**
 * An implementation of {@link LiveCacheStatistics} and also implements {@link LiveCacheStatisticsData}. Uses separate delegates depending
 * on whether
 * statistics is enabled or not.
 * <p />
 * To collect statistics element put/update/remove/expired data, instances of this class must be registered as a CacheEventListener to a
 * Cache
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class LiveCacheStatisticsWrapper implements LiveCacheStatistics, LiveCacheStatisticsData {

    private static final LiveCacheStatistics NULL_LIVE_CACHE_STATISTICS = new NullLiveCacheStatisticsData();

    private final LiveCacheStatisticsImpl liveDelegate;
    private volatile LiveCacheStatistics delegate;

    /**
     * Constructor accepting the backing cache.
     * 
     * @param cache
     */
    public LiveCacheStatisticsWrapper(Ehcache cache) {
        liveDelegate = new LiveCacheStatisticsImpl(cache);
        // enable statistics by default
        setStatisticsEnabled(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#setStatisticsEnabled(boolean)
     */
    public void setStatisticsEnabled(boolean enableStatistics) {
        if (enableStatistics) {
            delegate = liveDelegate;
        } else {
            delegate = NULL_LIVE_CACHE_STATISTICS;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#isStatisticsEnabled()
     */
    public boolean isStatisticsEnabled() {
        return delegate instanceof LiveCacheStatisticsImpl;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#registerCacheUsageListener(net.sf.ehcache.statistics.CacheUsageListener)
     */
    public void registerCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException {
        // always register on the live delegate
        liveDelegate.registerCacheUsageListener(cacheUsageListener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#removeCacheUsageListener(net.sf.ehcache.statistics.CacheUsageListener)
     */
    public void removeCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException {
        // always use the live delegate for this
        liveDelegate.removeCacheUsageListener(cacheUsageListener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#setStatisticsAccuracy(int)
     */
    public void setStatisticsAccuracy(int statisticsAccuracy) {
        // always use the live delegate for this
        liveDelegate.setStatisticsAccuracy(statisticsAccuracy);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getStatisticsAccuracy()
     */
    public int getStatisticsAccuracy() {
        // always use live delegate for this
        return liveDelegate.getStatisticsAccuracy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getStatisticsAccuracyDescription()
     */
    public String getStatisticsAccuracyDescription() {
        // always use live delegate for this
        return liveDelegate.getStatisticsAccuracyDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getCacheName()
     */
    public String getCacheName() {
        // always use the live delegate for this
        return liveDelegate.getCacheName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getSize()
     */
    public long getSize() {
        return delegate.getSize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getInMemorySize()
     */
    public long getInMemorySize() {
        return delegate.getInMemorySize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getOffHeapSize()
     */
    public long getOffHeapSize() {
        return delegate.getOffHeapSize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getOnDiskSize()
     */
    public long getOnDiskSize() {
        return delegate.getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getAverageGetTimeMillis()
     */
    public float getAverageGetTimeMillis() {
        return delegate.getAverageGetTimeMillis();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getCacheHitCount()
     */
    public long getCacheHitCount() {
        return delegate.getCacheHitCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getCacheMissCount()
     */
    public long getCacheMissCount() {
        return delegate.getCacheMissCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getCacheMissCountExpired()
     */
    public long getCacheMissCountExpired() {
        return delegate.getCacheMissCountExpired();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getEvictedCount()
     */
    public long getEvictedCount() {
        return delegate.getEvictedCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getExpiredCount()
     */
    public long getExpiredCount() {
        return delegate.getExpiredCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getInMemoryHitCount()
     */
    public long getInMemoryHitCount() {
        return delegate.getInMemoryHitCount();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getOffHeapHitCount()
     */
    public long getOffHeapHitCount() {
        return delegate.getOffHeapHitCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getOnDiskHitCount()
     */
    public long getOnDiskHitCount() {
        return delegate.getOnDiskHitCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getPutCount()
     */
    public long getPutCount() {
        return delegate.getPutCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getRemovedCount()
     */
    public long getRemovedCount() {
        return delegate.getRemovedCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getUpdateCount()
     */
    public long getUpdateCount() {
        return delegate.getUpdateCount();
    }

    private LiveCacheStatisticsData getDelegateAsLiveStatisticsData() {
        return (LiveCacheStatisticsData) delegate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#addGetTimeMillis(long)
     */
    public void addGetTimeMillis(long millis) {
        getDelegateAsLiveStatisticsData().addGetTimeMillis(millis);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getMaxGetTimeMillis()
     */
    public long getMaxGetTimeMillis() {
        return delegate.getMaxGetTimeMillis();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getMinGetTimeMillis()
     */
    public long getMinGetTimeMillis() {
        return delegate.getMinGetTimeMillis();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#cacheHitInMemory()
     */
    public void cacheHitInMemory() {
        getDelegateAsLiveStatisticsData().cacheHitInMemory();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#cacheHitOffHeap()
     */
    public void cacheHitOffHeap() {
        getDelegateAsLiveStatisticsData().cacheHitOffHeap();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#cacheHitOnDisk()
     */
    public void cacheHitOnDisk() {
        getDelegateAsLiveStatisticsData().cacheHitOnDisk();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#cacheMissExpired()
     */
    public void cacheMissExpired() {
        getDelegateAsLiveStatisticsData().cacheMissExpired();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#cacheMissNotFound()
     */
    public void cacheMissNotFound() {
        getDelegateAsLiveStatisticsData().cacheMissNotFound();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatisticsData#clearStatistics()
     */
    public void clearStatistics() {
        getDelegateAsLiveStatisticsData().clearStatistics();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.event.CacheEventListener#dispose()
     */
    public void dispose() {
        getDelegateAsLiveStatisticsData().dispose();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.event.CacheEventListener#notifyElementEvicted(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)
     */
    public void notifyElementEvicted(Ehcache cache, Element element) {
        getDelegateAsLiveStatisticsData().notifyElementEvicted(cache, element);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.event.CacheEventListener#notifyElementExpired(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)
     */
    public void notifyElementExpired(Ehcache cache, Element element) {
        getDelegateAsLiveStatisticsData().notifyElementExpired(cache, element);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.event.CacheEventListener#notifyElementPut(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)
     */
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        getDelegateAsLiveStatisticsData().notifyElementPut(cache, element);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.event.CacheEventListener#notifyElementRemoved(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)
     */
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        getDelegateAsLiveStatisticsData().notifyElementRemoved(cache, element);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.event.CacheEventListener#notifyElementUpdated(net.sf.ehcache.Ehcache, net.sf.ehcache.Element)
     */
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        getDelegateAsLiveStatisticsData().notifyElementUpdated(cache, element);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.event.CacheEventListener#notifyRemoveAll(net.sf.ehcache.Ehcache)
     */
    public void notifyRemoveAll(Ehcache cache) {
        getDelegateAsLiveStatisticsData().notifyRemoveAll(cache);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException();
    }

}
