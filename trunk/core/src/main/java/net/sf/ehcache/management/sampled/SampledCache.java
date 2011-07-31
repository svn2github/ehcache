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

package net.sf.ehcache.management.sampled;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfigurationListener;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.hibernate.management.impl.BaseEmitterBean;
import net.sf.ehcache.util.CacheTransactionHelper;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link SampledCacheMBean}
 *
 * <p />
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public class SampledCache extends BaseEmitterBean implements SampledCacheMBean, CacheConfigurationListener, PropertyChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(SampledCache.class.getName());

    private static final MBeanNotificationInfo[] NOTIFICATION_INFO;

    private final Ehcache cache;
    private final String immutableCacheName;

    static {
        final String[] notifTypes = new String[] {CACHE_ENABLED, CACHE_CHANGED, CACHE_FLUSHED, CACHE_STATISTICS_ENABLED,
                CACHE_STATISTICS_RESET, };
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
        cache.getCacheConfiguration().addConfigurationListener(this);
        cache.addPropertyChangeListener(this);
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
        try {
            cache.setDisabled(!enabled);
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     * @deprecated use {@link #isClusterBulkLoadEnabled()} instead
     */
    @Deprecated
    public boolean isClusterCoherent() {
        try {
            return cache.isClusterCoherent();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterBulkLoadEnabled() {
        try {
            return cache.isClusterBulkLoadEnabled();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     * @deprecated use {@link #isNodeBulkLoadEnabled()} instead
     */
    @Deprecated
    public boolean isNodeCoherent() {
        try {
            return cache.isNodeCoherent();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeBulkLoadEnabled() {
        return !isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     * @deprecated use {@link #setNodeBulkLoadEnabled(boolean)} instead
     */
    @Deprecated
    public void setNodeCoherent(boolean coherent) {
        boolean isNodeCoherent = isNodeCoherent();
        if (coherent != isNodeCoherent) {
            if (!coherent && getTransactional()) {
                LOG.warn("a transactional cache cannot be incoherent");
                return;
            }
            try {
                cache.setNodeCoherent(coherent);
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeBulkLoadEnabled(boolean bulkLoadEnabled) {
        if (bulkLoadEnabled && getTransactional()) {
            LOG.warn("a transactional cache cannot be put into bulk-load mode");
            return;
        }
        setNodeCoherent(!bulkLoadEnabled);
    }

    private RuntimeException newPlainException(RuntimeException e) {
        String type = e.getClass().getName();
        if (type.startsWith("java.") || type.startsWith("javax.")) {
            return e;
        } else {
            RuntimeException result = new RuntimeException(e.getMessage());
            result.setStackTrace(e.getStackTrace());
            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flush() {
        try {
            cache.flush();
            sendNotification(CACHE_FLUSHED, getCacheAttributes(), getImmutableCacheName());
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
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
        CacheTransactionHelper.beginTransactionIfNeeded(cache);
        try {
            cache.removeAll();
            sendNotification(CACHE_CLEARED, getCacheAttributes(), getImmutableCacheName());
        } catch (RuntimeException e) {
            throw newPlainException(e);
        } finally {
            try {
                CacheTransactionHelper.commitTransactionIfNeeded(cache);
            } catch (RuntimeException e2) {
                throw newPlainException(e2);
            }
        }
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
    public long getCacheEvictionRate() {
        return getCacheElementEvictedMostRecentSample();
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
    public long getCacheExpirationRate() {
        return getCacheElementExpiredMostRecentSample();
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
    public long getCachePutRate() {
        return getCacheElementPutMostRecentSample();
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
    public long getCacheRemoveRate() {
        return getCacheElementRemovedMostRecentSample();
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
    public long getCacheUpdateRate() {
        return getCacheElementUpdatedMostRecentSample();
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
    public long getCacheInMemoryHitRate() {
        return getCacheHitInMemoryMostRecentSample();
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
    public long getCacheOffHeapHitRate() {
        return getCacheHitOffHeapMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitOffHeapMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheHitOffHeapMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitRate() {
        return getCacheHitMostRecentSample();
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
    public long getCacheOnDiskHitRate() {
        return getCacheHitOnDiskMostRecentSample();
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
    public long getCacheMissRate() {
        return getCacheMissMostRecentSample();
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
    public long getCacheInMemoryMissRate() {
        return getCacheMissInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissInMemoryMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheMissInMemoryMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheOffHeapMissRate() {
        return getCacheMissOffHeapMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissOffHeapMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheMissOffHeapMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheOnDiskMissRate() {
        return getCacheMissOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissOnDiskMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheMissOnDiskMostRecentSample();
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
        try {
            cache.clearStatistics();
            sendNotification(CACHE_STATISTICS_RESET, getCacheAttributes(), getImmutableCacheName());
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
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
     */
    public String getTerracottaConsistency() {
        Consistency consistency = this.cache.getCacheConfiguration().getTerracottaConsistency();
        return consistency != null ? consistency.name() : "na";
    }

    /**
     * {@inheritDoc}
     */
    public String getTerracottaStorageStrategy() {
        StorageStrategy storageStrategy = this.cache.getCacheConfiguration().getTerracottaStorageStrategy();
        return storageStrategy != null ? storageStrategy.name() : "na";
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#enableStatistics()
     */
    public void enableStatistics() {
        if (!cache.isStatisticsEnabled()) {
            try {
                cache.setSampledStatisticsEnabled(true);
                cache.setStatisticsEnabled(true);
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#disableStatistics()
     */
    public void disableStatistics() {
        if (cache.isStatisticsEnabled()) {
            try {
                cache.setSampledStatisticsEnabled(false);
                cache.setStatisticsEnabled(false);
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setStatisticsEnabled(boolean)
     */
    public void setStatisticsEnabled(boolean statsEnabled) {
        boolean oldValue = isStatisticsEnabled();
        if (oldValue != statsEnabled) {
            if (statsEnabled) {
                enableStatistics();
            } else {
                disableStatistics();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#enableSampledStatistics()
     */
    public void enableSampledStatistics() {
        if (!cache.isSampledStatisticsEnabled()) {
            try {
                cache.setSampledStatisticsEnabled(true);
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#disableSampledStatistics ()
     */
    public void disableSampledStatistics() {
        if (cache.isSampledStatisticsEnabled()) {
            try {
                cache.setSampledStatisticsEnabled(false);
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getCacheAverageGetTime() {
        return getAverageGetTimeMillis();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getAverageGetTimeMillis()
     */
    public float getAverageGetTimeMillis() {
        try {
            return cache.getAverageGetTime();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getMaxGetTimeMillis()
     */
    public long getMaxGetTimeMillis() {
        try {
            return cache.getLiveCacheStatistics().getMaxGetTimeMillis();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getXaCommitCount()
     */
    public long getXaCommitCount() {
        try {
            return cache.getLiveCacheStatistics().getXaCommitCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getXaRollbackCount()
     */
    public long getXaRollbackCount() {
        try {
            return cache.getLiveCacheStatistics().getXaRollbackCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getHasWriteBehindWriter() {
        return cache.getWriterManager() instanceof WriteBehindManager &&
            cache.getRegisteredCacheWriter() != null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getWriterQueueLength()
     */
    public long getWriterQueueLength() {
        try {
            return cache.getLiveCacheStatistics().getWriterQueueLength();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getWriterMaxQueueSize() {
        return cache.getCacheConfiguration().getCacheWriterConfiguration().getWriteBehindMaxQueueSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getWriterConcurrency() {
        return cache.getCacheConfiguration().getCacheWriterConfiguration().getWriteBehindConcurrency();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.statistics.LiveCacheStatistics#getMinGetTimeMillis()
     */
    public long getMinGetTimeMillis() {
        try {
            return cache.getLiveCacheStatistics().getMinGetTimeMillis();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheHitCount()
     */
    public long getCacheHitCount() {
        try {
            return cache.getLiveCacheStatistics().getCacheHitCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheMissCount()
     */
    public long getCacheMissCount() {
        try {
            return cache.getLiveCacheStatistics().getCacheMissCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getInMemoryMissCount()
     */
    public long getInMemoryMissCount() {
        try {
            return cache.getLiveCacheStatistics().getInMemoryMissCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOffHeapMissCount()
     */
    public long getOffHeapMissCount() {
        try {
            return cache.getLiveCacheStatistics().getOffHeapMissCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOnDiskMissCount()
     */
    public long getOnDiskMissCount() {
        try {
            return cache.getLiveCacheStatistics().getOnDiskMissCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getCacheMissCountExpired()
     */
    public long getCacheMissCountExpired() {
        try {
            return cache.getLiveCacheStatistics().getCacheMissCountExpired();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
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
        if (getConfigDiskExpiryThreadIntervalSeconds() != seconds) {
            try {
                cache.getCacheConfiguration().setDiskExpiryThreadIntervalSeconds(seconds);
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigMaxEntriesLocalHeap()
     */
    public long getConfigMaxEntriesLocalHeap() {
        return cache.getCacheConfiguration().getMaxEntriesLocalHeap();
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
        if (getConfigMaxElementsInMemory() != maxElements) {
            try {
                cache.getCacheConfiguration().setMaxElementsInMemory(maxElements);
                sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getConfigMaxEntriesLocalDisk()
     */
    public long getConfigMaxEntriesLocalDisk() {
        return cache.getCacheConfiguration().getMaxEntriesLocalDisk();
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
        if (getConfigMaxElementsOnDisk() != maxElements) {
            try {
                cache.getCacheConfiguration().setMaxElementsOnDisk(maxElements);
                sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
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
        if (!getConfigMemoryStoreEvictionPolicy().equals(evictionPolicy)) {
            try {
                cache.getCacheConfiguration().setMemoryStoreEvictionPolicy(evictionPolicy);
                sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
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
        if (getConfigTimeToIdleSeconds() != tti) {
            try {
                cache.getCacheConfiguration().setTimeToIdleSeconds(tti);
                sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
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
        if (getConfigTimeToLiveSeconds() != ttl) {
            try {
                cache.getCacheConfiguration().setTimeToLiveSeconds(ttl);
                sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
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
        if (isConfigDiskPersistent() != diskPersistent) {
            try {
                cache.getCacheConfiguration().setDiskPersistent(diskPersistent);
                sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
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
        if (isConfigEternal() != eternal) {
            try {
                cache.getCacheConfiguration().setEternal(eternal);
                sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
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
        if (isConfigOverflowToDisk() != overflowToDisk) {
            try {
                cache.getCacheConfiguration().setOverflowToDisk(overflowToDisk);
                sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#isConfigLoggingEnabled()
     */
    public boolean isConfigLoggingEnabled() {
        return cache.getCacheConfiguration().getLogging();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#setConfigLoggingEnabled(boolean)
     */
    public void setConfigLoggingEnabled(boolean enabled) {
        if (isConfigLoggingEnabled() != enabled) {
            try {
                cache.getCacheConfiguration().setLogging(enabled);
                sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
            } catch (RuntimeException e) {
                throw newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getEvictedCount()
     */
    public long getEvictedCount() {
        try {
            return cache.getLiveCacheStatistics().getEvictedCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getExpiredCount()
     */
    public long getExpiredCount() {
        try {
            return cache.getLiveCacheStatistics().getExpiredCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getInMemoryHitCount()
     */
    public long getInMemoryHitCount() {
        try {
            return cache.getLiveCacheStatistics().getInMemoryHitCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getInMemorySize()
     * @deprecated use {@link #getLocalHeapSize()}
     */
    @Deprecated
    public long getInMemorySize() {
        return getLocalHeapSize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOffHeapHitCount()
     */
    public long getOffHeapHitCount() {
        try {
            return cache.getLiveCacheStatistics().getOffHeapHitCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOffHeapSize()
     * @deprecated use {@link #getLocalOffHeapSize()}
     */
    @Deprecated
    public long getOffHeapSize() {
        return getLocalOffHeapSize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOnDiskHitCount()
     */
    public long getOnDiskHitCount() {
        try {
            return cache.getLiveCacheStatistics().getOnDiskHitCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getOnDiskSize()
     * @deprecated use {@link #getLocalDiskSize()}
     */
    @Deprecated
    public long getOnDiskSize() {
        return getLocalDiskSize();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalDiskSize()
     */
    public long getLocalDiskSize() {
        try {
            return cache.getLiveCacheStatistics().getLocalDiskSize();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalHeapSize()
     */
    public long getLocalHeapSize() {
        try {
            return cache.getLiveCacheStatistics().getLocalHeapSize();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalOffHeapSize()
     */
    public long getLocalOffHeapSize() {
        try {
            return cache.getLiveCacheStatistics().getLocalOffHeapSize();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalDiskSizeInBytes()
     */
    public long getLocalDiskSizeInBytes() {
        try {
            return cache.getLiveCacheStatistics().getLocalDiskSizeInBytes();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalHeapSizeInBytes()
     */
    public long getLocalHeapSizeInBytes() {
        try {
            return cache.getLiveCacheStatistics().getLocalHeapSizeInBytes();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getLocalOffHeapSizeInBytes()
     */
    public long getLocalOffHeapSizeInBytes() {
        try {
            return cache.getLiveCacheStatistics().getLocalOffHeapSizeInBytes();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getPutCount()
     */
    public long getPutCount() {
        try {
            return cache.getLiveCacheStatistics().getPutCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getRemovedCount()
     */
    public long getRemovedCount() {
        try {
            return cache.getLiveCacheStatistics().getRemovedCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getSize()
     */
    public long getSize() {
        try {
            return cache.getLiveCacheStatistics().getSize();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.management.sampled.SampledCacheMBean#getUpdateCount()
     */
    public long getUpdateCount() {
        try {
            return cache.getLiveCacheStatistics().getUpdateCount();
        } catch (RuntimeException e) {
            throw newPlainException(e);
        }
    }

    /**
     * getCacheAttributes
     *
     * @return map of attribute name -> value
     */
    public Map<String, Object> getCacheAttributes() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("Enabled", isEnabled());
        result.put("TerracottaClustered", isTerracottaClustered());
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
        result.put("TerracottaConsistency", getTerracottaConsistency());
        if (isTerracottaClustered()) {
            result.put("NodeBulkLoadEnabled", isNodeBulkLoadEnabled());
            result.put("NodeCoherent", isNodeCoherent());
            result.put("ClusterBulkLoadEnabled", isClusterBulkLoadEnabled());
            result.put("ClusterCoherent", isClusterCoherent());
        }
        result.put("StatisticsEnabled", isStatisticsEnabled());
        result.put("WriterConcurrency", getWriterConcurrency());
        result.put("Transactional", getTransactional());
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
     */
    public void deregistered(CacheConfiguration config) {
        /**/
    }

    /**
     * {@inheritDoc}
     */
    public void maxBytesLocalHeapChanged(final long oldValue, final long newValue) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void maxBytesLocalDiskChanged(final long oldValue, final long newValue) {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        if (oldCapacity != newCapacity) {
            setConfigMaxElementsOnDisk(newCapacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void loggingChanged(boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            setConfigLoggingEnabled(newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
        if (oldCapacity != newCapacity) {
            setConfigMaxElementsInMemory(newCapacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registered(CacheConfiguration config) {
        /**/
    }

    /**
     * {@inheritDoc}
     */
    public void timeToIdleChanged(long oldTimeToIdle, long newTimeToIdle) {
        if (oldTimeToIdle != newTimeToIdle) {
            setConfigTimeToIdleSeconds(newTimeToIdle);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void timeToLiveChanged(long oldTimeToLive, long newTimeToLive) {
        if (oldTimeToLive != newTimeToLive) {
            setConfigTimeToLiveSeconds(newTimeToLive);
        }
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
        sendNotification(CACHE_CHANGED, getCacheAttributes(), getImmutableCacheName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDispose() {
        cache.getCacheConfiguration().removeConfigurationListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public long getAverageSearchTime() {
        return cache.getAverageSearchTime();
    }

    /**
     * {@inheritDoc}
     */
    public long getSearchesPerSecond() {
        return cache.getSearchesPerSecond();
    }

    /**
     * {@inheritDoc}
     */
    public boolean getTransactional() {
        return cache.getCacheConfiguration().getTransactionalMode().isTransactional();
    }

    /**
     * {@inheritDoc}
     */
    public boolean getSearchable() {
        return cache.getCacheConfiguration().getSearchable() != null;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheSearchRate() {
        return cache.getSampledCacheStatistics().getSearchesPerSecond();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheAverageSearchTime() {
        return cache.getSampledCacheStatistics().getAverageSearchTime();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionCommitRate() {
        return getCacheXaCommitsMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheXaCommitsMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheXaCommitsMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionRollbackRate() {
        return getCacheXaRollbacksMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheXaRollbacksMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheXaRollbacksMostRecentSample();
    }
}
