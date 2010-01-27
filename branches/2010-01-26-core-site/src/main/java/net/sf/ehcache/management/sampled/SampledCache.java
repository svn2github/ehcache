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

package net.sf.ehcache.management.sampled;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.hibernate.management.impl.BaseEmitterBean;

/**
 * An implementation of {@link SampledCacheMBean}
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCache extends BaseEmitterBean implements SampledCacheMBean {
    private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

    private final Ehcache cache;
    private final String immutableCacheName;
    
    static {
        final String[] notifTypes = new String[] {CACHE_ENABLED, CACHE_CHANGED, CACHE_FLUSHED,
                CACHE_STATISTICS_ENABLED, CACHE_STATISTICS_RESET, };
        final String name = Notification.class.getName();
        final String description = "Ehcache SampledCache Event";
        NOTIFICATION_INFO = new MBeanNotificationInfo[] {new MBeanNotificationInfo(notifTypes, name, description), };
    }

    /**
     * Constructor accepting the backing {@link Ehcache}
     * 
     * @param cache
     */
    public SampledCache(Ehcache cache) throws NotCompliantMBeanException {
        super(SampledCacheMBean.class);
        this.cache = cache;
        immutableCacheName = cache.getName();
    }

    /**
     * Method which returns the name of the cache at construction time.
     * Package protected method.
     * 
     * @return The name of the cache
     */
    String getImmutableCacheName() {
        return immutableCacheName;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnabled() {
        return !cache.isDisabled();
    }
    
    /**
     * {@inheritDoc}
     */
    public void setEnabled(boolean enabled) {
        cache.setDisabled(!enabled);
        sendNotification(CACHE_ENABLED, getCacheAttributes(), getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCoherent() {
        return cache.isCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public void setCoherent(boolean coherent) {
        cache.setCoherent(coherent);
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     */
    public void flush() {
        cache.flush();
        sendNotification(CACHE_FLUSHED, getCacheAttributes(), getImmutableCacheName());
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
    public String getStatus() {
        return cache.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() {
        cache.removeAll();
        sendNotification(CACHE_CLEARED, getCacheAttributes(), getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     */
    public long getAverageGetTimeMostRecentSample() {
        return cache.getSampledCacheStatistics().getAverageGetTimeMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementEvictedMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheElementEvictedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementExpiredMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheElementExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementPutMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheElementPutMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementRemovedMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheElementRemovedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheElementUpdatedMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheElementUpdatedMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitInMemoryMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheHitInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheHitMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitOnDiskMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheHitOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissExpiredMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheMissExpiredMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheMissMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissNotFoundMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheMissNotFoundMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatisticsAccuracy() {
        return cache.getSampledCacheStatistics().getStatisticsAccuracy();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatisticsAccuracyDescription() {
        return cache.getSampledCacheStatistics().getStatisticsAccuracyDescription();
    }

    /**
     * {@inheritDoc}
     */
    public void clearStatistics() {
        cache.clearStatistics();
        sendNotification(CACHE_STATISTICS_RESET, getCacheAttributes(), getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return cache.isStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSampledStatisticsEnabled() {
        return cache.getSampledCacheStatistics().isSampledStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTerracottaClustered() {
        return this.cache.getCacheConfiguration().isTerracottaClustered();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#enableStatistics()
     */
    public void enableStatistics() {
        cache.setStatisticsEnabled(true);
        sendNotification(CACHE_STATISTICS_ENABLED, getCacheAttributes(), getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#disableStatistics()
     */
    public void disableStatistics() {
        cache.setStatisticsEnabled(false);
        sendNotification(CACHE_STATISTICS_ENABLED, getCacheAttributes(), getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#enableSampledStatistics()
     */
    public void enableSampledStatistics() {
        cache.setSampledStatisticsEnabled(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#disableSampledStatistics ()
     */
    public void disableSampledStatistics() {
        cache.setSampledStatisticsEnabled(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getAverageGetTimeMillis()
     */
    public float getAverageGetTimeMillis() {
        return cache.getLiveCacheStatistics().getAverageGetTimeMillis();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getMaxGetTimeMillis()
     */
    public long getMaxGetTimeMillis() {
        return cache.getLiveCacheStatistics().getMaxGetTimeMillis();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getMinGetTimeMillis()
     */
    public long getMinGetTimeMillis() {
        return cache.getLiveCacheStatistics().getMinGetTimeMillis();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheHitCount()
     */
    public long getCacheHitCount() {
        return cache.getLiveCacheStatistics().getCacheHitCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheMissCount()
     */
    public long getCacheMissCount() {
        return cache.getLiveCacheStatistics().getCacheMissCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheMissCountExpired()
     */
    public long getCacheMissCountExpired() {
        return cache.getLiveCacheStatistics().getCacheMissCountExpired();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigDiskExpiryThreadIntervalSeconds()
     */
    public long getConfigDiskExpiryThreadIntervalSeconds() {
        return cache.getCacheConfiguration().getDiskExpiryThreadIntervalSeconds();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigDiskExpiryThreadIntervalSeconds(long)
     */
    public void setConfigDiskExpiryThreadIntervalSeconds(long seconds) {
        cache.getCacheConfiguration().setDiskExpiryThreadIntervalSeconds(seconds);
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigMaxElementsInMemory()
     */
    public int getConfigMaxElementsInMemory() {
        return cache.getCacheConfiguration().getMaxElementsInMemory();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigMaxElementsInMemory(int)
     */
    public void setConfigMaxElementsInMemory(int maxElements) {
        cache.getCacheConfiguration().setMaxElementsInMemory(maxElements);
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigMaxElementsOnDisk()
     */
    public int getConfigMaxElementsOnDisk() {
        return cache.getCacheConfiguration().getMaxElementsOnDisk();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigMaxElementsOnDisk(int)
     */
    public void setConfigMaxElementsOnDisk(int maxElements) {
        cache.getCacheConfiguration().setMaxElementsOnDisk(maxElements);
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigMemoryStoreEvictionPolicy()
     */
    public String getConfigMemoryStoreEvictionPolicy() {
        return cache.getCacheConfiguration().getMemoryStoreEvictionPolicy().toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigMemoryStoreEvictionPolicy(String)
     */
    public void setConfigMemoryStoreEvictionPolicy(String evictionPolicy) {
        cache.getCacheConfiguration().setMemoryStoreEvictionPolicy(evictionPolicy);
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigTimeToIdleSeconds()
     */
    public long getConfigTimeToIdleSeconds() {
        return cache.getCacheConfiguration().getTimeToIdleSeconds();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigTimeToIdleSeconds(long)
     */
    public void setConfigTimeToIdleSeconds(long tti) {
        cache.getCacheConfiguration().setTimeToIdleSeconds(tti);
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigTimeToLiveSeconds()
     */
    public long getConfigTimeToLiveSeconds() {
        return cache.getCacheConfiguration().getTimeToLiveSeconds();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigTimeToLiveSeconds(long)
     */
    public void setConfigTimeToLiveSeconds(long ttl) {
        cache.getCacheConfiguration().setTimeToLiveSeconds(ttl);
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }


    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isConfigDiskPersistent()
     */
    public boolean isConfigDiskPersistent() {
        return cache.getCacheConfiguration().isDiskPersistent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigDiskPersistent(boolean)
     */
    public void setConfigDiskPersistent(boolean diskPersistent) {
        cache.getCacheConfiguration().setDiskPersistent(diskPersistent);
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isConfigEternal()
     */
    public boolean isConfigEternal() {
        return cache.getCacheConfiguration().isEternal();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigEternal(boolean)
     */
    public void setConfigEternal(boolean eternal) {
        cache.getCacheConfiguration().setEternal(eternal);
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isConfigOverflowToDisk()
     */
    public boolean isConfigOverflowToDisk() {
        return cache.getCacheConfiguration().isOverflowToDisk();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigOverflowToDisk(boolean)
     */
    public void setConfigOverflowToDisk(boolean overflowToDisk) {
        cache.getCacheConfiguration().setOverflowToDisk(overflowToDisk);
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isConfigLoggingEnabled()
     */
    public boolean isConfigLoggingEnabled() {
        return cache.getCacheConfiguration().isLoggingEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigLoggingEnabled(boolean)
     */
    public void setConfigLoggingEnabled(boolean enabled) {
        cache.getCacheConfiguration().setLoggingEnabled(enabled);
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getEvictedCount()
     */
    public long getEvictedCount() {
        return cache.getLiveCacheStatistics().getEvictedCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getExpiredCount()
     */
    public long getExpiredCount() {
        return cache.getLiveCacheStatistics().getExpiredCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getInMemoryHitCount()
     */
    public long getInMemoryHitCount() {
        return cache.getLiveCacheStatistics().getInMemoryHitCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getInMemorySize()
     */
    public long getInMemorySize() {
        return cache.getLiveCacheStatistics().getInMemorySize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOnDiskHitCount()
     */
    public long getOnDiskHitCount() {
        return cache.getLiveCacheStatistics().getOnDiskHitCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOnDiskSize()
     */
    public long getOnDiskSize() {
        return cache.getLiveCacheStatistics().getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getPutCount()
     */
    public long getPutCount() {
        return cache.getLiveCacheStatistics().getPutCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getRemovedCount()
     */
    public long getRemovedCount() {
        return cache.getLiveCacheStatistics().getRemovedCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getSize()
     */
    public long getSize() {
        return cache.getLiveCacheStatistics().getSize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getUpdateCount()
     */
    public long getUpdateCount() {
        return cache.getLiveCacheStatistics().getUpdateCount();
    }

    /**
     * getCacheAttributes
     * @return map of attribute name -> value
     */
    public Map<String, Object> getCacheAttributes() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("Enabled", isEnabled());
        result.put("LoggingEnabled", isConfigLoggingEnabled());
        result.put("TimeToIdleSeconds", getConfigTimeToIdleSeconds());
        result.put("TimeToLiveSeconds", getConfigTimeToLiveSeconds());
        result.put("MaxElementsInMemory", getConfigMaxElementsInMemory());
        result.put("MaxElementsOnDisk", getConfigMaxElementsOnDisk());
        result.put("DiskPersistent", isConfigDiskPersistent());
        result.put("Eternal", isConfigEternal());
        result.put("OverflowToDisk", isConfigOverflowToDisk());
        result.put("DiskExpiryThreadIntervalSeconds", getConfigDiskExpiryThreadIntervalSeconds());
        result.put("MemoryStoreEvictionPolicy", getConfigMemoryStoreEvictionPolicy());
        result.put("Coherent", isCoherent());
        return result;
    }
    
    /**
     * @see BaseEmitterBean#getNotificationInfo()
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return NOTIFICATION_INFO;
    }

    /**
     * {@inheritDoc}
     * 
     * @see net.sf.ehcache.statistics.sampled.SampledCacheStatistics#dispose()
     */
    public void dispose() {
        // no-op
    }
}
