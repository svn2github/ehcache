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

package net.sf.ehcache.management.sampled;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfigurationListener;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.util.CacheTransactionHelper;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link CacheSampler}
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 */
public class CacheSamplerImpl implements CacheSampler, CacheConfigurationListener {
    private static final int PERCENTAGE_DIVISOR = 100;
    private static final int MILLIS_PER_SECOND = 1000;
    private static final int NANOS_PER_MILLI = MILLIS_PER_SECOND * MILLIS_PER_SECOND;

    private static final Logger LOG = LoggerFactory.getLogger(CacheSamplerImpl.class);

    private final Ehcache cache;

    /**
     * Constructor accepting the backing {@link Ehcache}
     *
     * @param cache the cache object to use in initializing this sampled representation
     */
    public CacheSamplerImpl(Ehcache cache) {
        this.cache = cache;
        cache.getCacheConfiguration().addConfigurationListener(this);
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
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterBulkLoadEnabled() {
        try {
            return cache.isClusterBulkLoadEnabled();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeBulkLoadEnabled() {
        return cache.isNodeBulkLoadEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeBulkLoadEnabled(boolean bulkLoadEnabled) {
        if (bulkLoadEnabled && getTransactional()) {
            LOG.warn("a transactional cache cannot be put into bulk-load mode");
            return;
        }
        cache.setNodeBulkLoadEnabled(!bulkLoadEnabled);
    }

    /**
     * {@inheritDoc}
     */
    public void flush() {
        try {
            cache.flush();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
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
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        } finally {
            try {
                CacheTransactionHelper.commitTransactionIfNeeded(cache);
            } catch (RuntimeException e2) {
                throw Utils.newPlainException(e2);
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
    public long getAverageGetTimeNanosMostRecentSample() {
        return cache.getSampledCacheStatistics().getAverageGetTimeNanosMostRecentSample();
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
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
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
    public void dispose() {
        cache.getCacheConfiguration().removeConfigurationListener(this);
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
     */
    public void enableStatistics() {
        if (!cache.isStatisticsEnabled()) {
            try {
                cache.setSampledStatisticsEnabled(true);
                cache.setStatisticsEnabled(true);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void disableStatistics() {
        if (cache.isStatisticsEnabled()) {
            try {
                cache.setSampledStatisticsEnabled(false);
                cache.setStatisticsEnabled(false);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
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
     */
    public void enableSampledStatistics() {
        if (!cache.isSampledStatisticsEnabled()) {
            try {
                cache.setSampledStatisticsEnabled(true);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void disableSampledStatistics() {
        if (cache.isSampledStatisticsEnabled()) {
            try {
                cache.setSampledStatisticsEnabled(false);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheAverageGetTime() {
        return getAverageGetTimeNanos();
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageGetTimeMillis() {
        try {
            return getAverageGetTimeNanos() / (float) NANOS_PER_MILLI;
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getAverageGetTimeNanos() {
        try {
            return cache.getLiveCacheStatistics().getAverageGetTimeNanos();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxGetTimeMillis() {
        try {
            return cache.getLiveCacheStatistics().getMaxGetTimeMillis();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMinGetTimeMillis() {
        try {
            return cache.getLiveCacheStatistics().getMinGetTimeMillis();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxGetTimeNanos() {
        try {
            return cache.getLiveCacheStatistics().getMaxGetTimeNanos();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMinGetTimeNanos() {
        try {
            return cache.getLiveCacheStatistics().getMaxGetTimeNanos();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getXaCommitCount() {
        try {
            return cache.getLiveCacheStatistics().getXaCommitCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getXaRollbackCount() {
        try {
            return cache.getLiveCacheStatistics().getXaRollbackCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getXaRecoveredCount() {
        try {
            return cache.getLiveCacheStatistics().getXaRecoveredCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
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
     */
    public long getWriterQueueLength() {
        try {
            return cache.getLiveCacheStatistics().getWriterQueueLength();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
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
     */
    public long getCacheHitCount() {
        try {
            return cache.getLiveCacheStatistics().getCacheHitCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissCount() {
        try {
            return cache.getLiveCacheStatistics().getCacheMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemoryMissCount() {
        try {
            return cache.getLiveCacheStatistics().getInMemoryMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapMissCount() {
        try {
            return cache.getLiveCacheStatistics().getOffHeapMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskMissCount() {
        try {
            return cache.getLiveCacheStatistics().getOnDiskMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissCountExpired() {
        try {
            return cache.getLiveCacheStatistics().getCacheMissCountExpired();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getDiskExpiryThreadIntervalSeconds() {
        return cache.getCacheConfiguration().getDiskExpiryThreadIntervalSeconds();
    }

    /**
     * {@inheritDoc}
     */
    public void setDiskExpiryThreadIntervalSeconds(long seconds) {
        if (getDiskExpiryThreadIntervalSeconds() != seconds) {
            try {
                cache.getCacheConfiguration().setDiskExpiryThreadIntervalSeconds(seconds);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxEntriesLocalHeap() {
        return cache.getCacheConfiguration().getMaxEntriesLocalHeap();
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxEntriesLocalHeap(long maxEntries) {
        if (getMaxEntriesLocalHeap() != maxEntries) {
            try {
                cache.getCacheConfiguration().setMaxEntriesLocalHeap(maxEntries);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalHeap() {
        return cache.getCacheConfiguration().getMaxBytesLocalHeap();
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalHeap(long maxBytes) {
        try {
            if (cache.getCacheManager().getConfiguration().isMaxBytesLocalHeapSet()) {
                long heapPoolSize = cache.getCacheManager().getConfiguration().getMaxBytesLocalHeap();
                if (maxBytes > heapPoolSize) {
                    throw new IllegalArgumentException("Requested maxBytesLocalHeap ("
                                                       + maxBytes + ") greater than available CacheManager heap pool size ("
                                                       + heapPoolSize + ")");
                }
            }
            cache.getCacheConfiguration().setMaxBytesLocalHeap(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalHeapAsString(String maxBytes) {
        try {
            cache.getCacheConfiguration().setMaxBytesLocalHeap(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }

        if (cache.getCacheConfiguration().isMaxBytesLocalHeapPercentageSet()) {
            long cacheAssignedMem = cache.getCacheManager().getConfiguration().getMaxBytesLocalHeap() *
                                    cache.getCacheConfiguration().getMaxBytesLocalHeapPercentage() / PERCENTAGE_DIVISOR;
            setMaxBytesLocalHeap(cacheAssignedMem);
        }

    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalHeapAsString() {
        return cache.getCacheConfiguration().getMaxBytesLocalHeapAsString();
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxEntriesLocalDisk() {
        return cache.getCacheConfiguration().getMaxEntriesLocalDisk();
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxEntriesLocalDisk(long maxEntries) {
        if (getMaxEntriesLocalDisk() != maxEntries) {
            try {
                cache.getCacheConfiguration().setMaxEntriesLocalDisk(maxEntries);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalDisk(long maxBytes) {
        try {
            if (cache.getCacheManager().getConfiguration().isMaxBytesLocalDiskSet()) {
                long diskPoolSize = cache.getCacheManager().getConfiguration().getMaxBytesLocalDisk();
                if (maxBytes > diskPoolSize) {
                    throw new IllegalArgumentException("Requested maxBytesLocalDisk ("
                                                       + maxBytes + ") greater than available CacheManager disk pool size ("
                                                       + diskPoolSize + ")");
                }
            }
            cache.getCacheConfiguration().setMaxBytesLocalDisk(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalDiskAsString(String maxBytes) {
        try {
            cache.getCacheConfiguration().setMaxBytesLocalDisk(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }

        if (cache.getCacheConfiguration().isMaxBytesLocalDiskPercentageSet()) {
            long cacheAssignedMem = cache.getCacheManager().getConfiguration().getMaxBytesLocalDisk() *
                                    cache.getCacheConfiguration().getMaxBytesLocalDiskPercentage() / PERCENTAGE_DIVISOR;
            setMaxBytesLocalDisk(cacheAssignedMem);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalDiskAsString() {
        return cache.getCacheConfiguration().getMaxBytesLocalDiskAsString();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxElementsOnDisk() {
        return cache.getCacheConfiguration().getMaxElementsOnDisk();
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxElementsOnDisk(int maxElements) {
        if (getMaxElementsOnDisk() != maxElements) {
            try {
                cache.getCacheConfiguration().setMaxElementsOnDisk(maxElements);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalDisk() {
        return cache.getCacheConfiguration().getMaxBytesLocalDisk();
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalOffHeap() {
        return cache.getCacheConfiguration().getMaxBytesLocalOffHeap();
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalOffHeapAsString() {
        return cache.getCacheConfiguration().getMaxBytesLocalOffHeapAsString();
    }

    /**
     * {@inheritDoc}
     */
    public String getMemoryStoreEvictionPolicy() {
        return cache.getCacheConfiguration().getMemoryStoreEvictionPolicy().toString();
    }

    /**
     * {@inheritDoc}
     */
    public void setMemoryStoreEvictionPolicy(String evictionPolicy) {
        if (!getMemoryStoreEvictionPolicy().equals(evictionPolicy)) {
            try {
                cache.getCacheConfiguration().setMemoryStoreEvictionPolicy(evictionPolicy);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getTimeToIdleSeconds() {
        return cache.getCacheConfiguration().getTimeToIdleSeconds();
    }

    /**
     * {@inheritDoc}
     */
    public void setTimeToIdleSeconds(long tti) {
        if (getTimeToIdleSeconds() != tti) {
            try {
                cache.getCacheConfiguration().setTimeToIdleSeconds(tti);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getTimeToLiveSeconds() {
        return cache.getCacheConfiguration().getTimeToLiveSeconds();
    }

    /**
     * {@inheritDoc}
     */
    public void setTimeToLiveSeconds(long ttl) {
        if (getTimeToLiveSeconds() != ttl) {
            try {
                cache.getCacheConfiguration().setTimeToLiveSeconds(ttl);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDiskPersistent() {
        return cache.getCacheConfiguration().isDiskPersistent();
    }

    /**
     * {@inheritDoc}
     */
    public void setDiskPersistent(boolean diskPersistent) {
        if (isDiskPersistent() != diskPersistent) {
            try {
                cache.getCacheConfiguration().setDiskPersistent(diskPersistent);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEternal() {
        return cache.getCacheConfiguration().isEternal();
    }

    /**
     * {@inheritDoc}
     */
    public void setEternal(boolean eternal) {
        if (isEternal() != eternal) {
            try {
                cache.getCacheConfiguration().setEternal(eternal);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOverflowToDisk() {
        return cache.getCacheConfiguration().isOverflowToDisk();
    }

    /**
     * {@inheritDoc}
     */
    public void setOverflowToDisk(boolean overflowToDisk) {
        if (isOverflowToDisk() != overflowToDisk) {
            try {
                cache.getCacheConfiguration().setOverflowToDisk(overflowToDisk);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLoggingEnabled() {
        return cache.getCacheConfiguration().getLogging();
    }

    /**
     * {@inheritDoc}
     */
    public void setLoggingEnabled(boolean enabled) {
        if (isLoggingEnabled() != enabled) {
            try {
                cache.getCacheConfiguration().setLogging(enabled);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPinned() {
        return cache.getCacheConfiguration().getPinningConfiguration() != null;
    }

    /**
     * {@inheritDoc}
     */
    public String getPinnedToStore() {
        PinningConfiguration pinningConfig = cache.getCacheConfiguration().getPinningConfiguration();
        return pinningConfig != null ? pinningConfig.getStore().name() : "na";
    }

    /**
     * {@inheritDoc}
     */
    public long getEvictedCount() {
        try {
            return cache.getLiveCacheStatistics().getEvictedCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getExpiredCount() {
        try {
            return cache.getLiveCacheStatistics().getExpiredCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemoryHitCount() {
        try {
            return cache.getLiveCacheStatistics().getInMemoryHitCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapHitCount() {
        try {
            return cache.getLiveCacheStatistics().getOffHeapHitCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #getLocalOffHeapSize()}
     */
    @Deprecated
    public long getOffHeapSize() {
        return getLocalOffHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskHitCount() {
        try {
            return cache.getLiveCacheStatistics().getOnDiskHitCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated use {@link #getLocalDiskSize()}
     */
    @Deprecated
    public long getOnDiskSize() {
        return getLocalDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalDiskSize() {
        try {
            return cache.getLiveCacheStatistics().getLocalDiskSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalHeapSize() {
        try {
            return cache.getLiveCacheStatistics().getLocalHeapSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalOffHeapSize() {
        try {
            return cache.getLiveCacheStatistics().getLocalOffHeapSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalDiskSizeInBytes() {
        try {
            return cache.getLiveCacheStatistics().getLocalDiskSizeInBytes();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalHeapSizeInBytes() {
        try {
            return cache.getLiveCacheStatistics().getLocalHeapSizeInBytes();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalOffHeapSizeInBytes() {
        try {
            return cache.getLiveCacheStatistics().getLocalOffHeapSizeInBytes();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getPutCount() {
        try {
            return cache.getLiveCacheStatistics().getPutCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getRemovedCount() {
        try {
            return cache.getLiveCacheStatistics().getRemovedCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        try {
            return cache.getLiveCacheStatistics().getSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySize() {
        return getLocalHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getUpdateCount() {
        try {
            return cache.getLiveCacheStatistics().getUpdateCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
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
        if (oldValue != newValue) {
            setMaxBytesLocalHeap(newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void maxBytesLocalDiskChanged(final long oldValue, final long newValue) {
        if (oldValue != newValue) {
            setMaxBytesLocalDisk(newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        if (oldCapacity != newCapacity) {
            setMaxElementsOnDisk(newCapacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void loggingChanged(boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            setLoggingEnabled(newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
        if (oldCapacity != newCapacity) {
            setMaxEntriesLocalHeap(newCapacity);
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
            setTimeToIdleSeconds(newTimeToIdle);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void timeToLiveChanged(long oldTimeToLive, long newTimeToLive) {
        if (oldTimeToLive != newTimeToLive) {
            setTimeToLiveSeconds(newTimeToLive);
        }
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

    /**
     * A package friendly method to allow dependants access to the underlying {@link Ehcache} object. This is available
     * in order to allow {@link SampledCache} objects to continue to provide deprecated methods on the {@link SampledCacheMBean}
     * interface, rather than burdening {@link CacheSampler} with these now irrelevant methods. This helper method
     * should be removed if we are ever able to discontinue support for the deprecated methods on dependant interfaces.
     *
     * @return the underlying {@code Ehcache} object
     */
    Ehcache getCache() {
        return cache;
    }

    /**
     * {@inheritDoc}
     */
    public int getCacheHitRatio() {
        return getCacheHitRatioMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public int getCacheHitRatioMostRecentSample() {
        return cache.getSampledCacheStatistics().getCacheHitRatioMostRecentSample();
    }
}


