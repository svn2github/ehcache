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
        return cache.getStatistics().getExtended().getAverageGetTimeMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getAverageGetTimeNanosMostRecentSample() {
        return cache.getStatistics().getExtended().getAverageGetTimeNanosMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheElementEvictedMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheElementExpiredMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheElementPutMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheElementRemovedMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheElementUpdatedMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheHitInMemoryMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheHitOffHeapMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheHitMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheHitOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissExpiredMostRecentSample() {
        return cache.getStatistics().getExtended().getCacheMissExpiredMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheMissMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheMissInMemoryMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheMissOffHeapMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheMissOnDiskMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissNotFoundMostRecentSample() {
        return cache.getStatistics().getExtended().getCacheMissNotFoundMostRecentSample();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatisticsAccuracy() {
        return cache.getStatistics().getExtended().getStatisticsAccuracy();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatisticsAccuracyDescription() {
        return cache.getStatistics().getExtended().getStatisticsAccuracyDescription();
    }

    /**
     * {@inheritDoc}
     */
    public void clearStatistics() {
        try {
            cache.getStatistics().clearStatistics();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return cache.getStatistics().isStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSampledStatisticsEnabled() {
        return cache.getStatistics().getExtended().isSampledStatisticsEnabled();
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
    public void enableStatistics() {
        if (!cache.getStatistics().isStatisticsEnabled()) {
            try {
                cache.getStatistics().setSampledStatisticsEnabled(true);
                cache.getStatistics().setStatisticsEnabled(true);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void disableStatistics() {
        if (cache.getStatistics().isStatisticsEnabled()) {
            try {
                cache.getStatistics().setSampledStatisticsEnabled(false);
                cache.getStatistics().setStatisticsEnabled(false);
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
        if (!cache.getStatistics().isSampledStatisticsEnabled()) {
            try {
                cache.getStatistics().setSampledStatisticsEnabled(true);
            } catch (RuntimeException e) {
                throw Utils.newPlainException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void disableSampledStatistics() {
        if (cache.getStatistics().isSampledStatisticsEnabled()) {
            try {
                cache.getStatistics().setSampledStatisticsEnabled(false);
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
            return getAverageGetTimeNanosMostRecentSample();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxGetTimeMillis() {
        try {
            return cache.getStatistics().getCore().getMaxGetTimeMillis();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMinGetTimeMillis() {
        try {
            return cache.getStatistics().getCore().getMinGetTimeMillis();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxGetTimeNanos() {
        try {
            return cache.getStatistics().getExtended().getMaxGetTimeNanos();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMinGetTimeNanos() {
        try {
            return cache.getStatistics().getExtended().getMinGetTimeNanos();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getXaCommitCount() {
        try {
            return cache.getStatistics().getCore().getXaCommitCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getXaRollbackCount() {
        try {
            return cache.getStatistics().getCore().getXaRollbackCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getXaRecoveredCount() {
        try {
            return cache.getStatistics().getCore().getXaRecoveredCount();
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
            return cache.getStatistics().getCore().getWriterQueueLength();
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
    public long getInMemoryMissCount() {
        try {
            return cache.getStatistics().getCore().getInMemoryMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapMissCount() {
        try {
            return cache.getStatistics().getCore().getOffHeapMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskMissCount() {
        try {
            return cache.getStatistics().getCore().getOnDiskMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissCountExpired() {
        try {
            return cache.getStatistics().getCore().getCacheMissCountExpired();
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
    public int getMaxEntriesInCache() {
        return cache.getCacheConfiguration().getMaxEntriesInCache();
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
    public void setMaxEntriesInCache(int maxEntries) {
        if (getMaxEntriesInCache() != maxEntries) {
            try {
                cache.getCacheConfiguration().setMaxEntriesInCache(maxEntries);
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
            return cache.getStatistics().getCore().getEvictedCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getExpiredCount() {
        try {
            return cache.getStatistics().getCore().getExpiredCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemoryHitCount() {
        try {
            return cache.getStatistics().getCore().getInMemoryHitCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapHitCount() {
        try {
            return cache.getStatistics().getCore().getOffHeapHitCount();
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
            return cache.getStatistics().getCore().getOnDiskHitCount();
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
            return cache.getStatistics().getCore().getLocalDiskSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalHeapSize() {
        try {
            return cache.getStatistics().getCore().getLocalHeapSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalOffHeapSize() {
        try {
            return cache.getStatistics().getCore().getLocalOffHeapSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalDiskSizeInBytes() {
        try {
            return cache.getStatistics().getCore().getLocalDiskSizeInBytes();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalHeapSizeInBytes() {
        try {
            return cache.getStatistics().getCore().getLocalHeapSizeInBytes();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalOffHeapSizeInBytes() {
        try {
            return cache.getStatistics().getCore().getLocalOffHeapSizeInBytes();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getPutCount() {
        try {
            return cache.getStatistics().getCore().getPutCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getRemovedCount() {
        try {
            return cache.getStatistics().getCore().getRemovedCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        try {
            return cache.getStatistics().getCore().getSize();
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
            return cache.getStatistics().getCore().getUpdateCount();
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
    public void maxEntriesInCacheChanged(int oldCapacity, int newCapacity) {
        if (oldCapacity != newCapacity) {
            setMaxEntriesInCache(newCapacity);
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
        return cache.getStatistics().getExtended().getAverageSearchTime();
    }

    /**
     * {@inheritDoc}
     */
    public long getSearchesPerSecond() {
        return cache.getStatistics().getExtended().getSearchesPerSecond();
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
        return cache.getStatistics().getExtended().getSearchesPerSecond();
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheAverageSearchTime() {
        return cache.getStatistics().getExtended().getAverageSearchTime();
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
        return cache.getStatistics().getExtended().getCacheXaCommitsMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheXaRollbacksMostRecentSample();
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
        return cache.getStatistics().getExtended().getCacheHitRatioMostRecentSample();
    }

    @Override
    public long calculateInMemorySize() {
        return cache.getStatistics().getCore().calculateInMemorySize();
    }

    @Override
    public long getMemoryStoreSize() {
        return cache.getStatistics().getCore().getMemoryStoreSize();
    }

    @Override
    public int getDiskStoreSize() {
        return cache.getStatistics().getCore().getDiskStoreSize();
    }

    @Override
    public long calculateOffHeapSize() {
        return cache.getStatistics().getCore().calculateOffHeapSize();
    }

    @Override
    public long getOffHeapStoreSize() {
        return cache.getStatistics().getCore().getOffHeapStoreSize();
    }

    @Override
    public long getInMemoryHits() {
        return cache.getStatistics().getCore().getInMemoryHits();
    }

    @Override
    public long getInMemoryMisses() {
        return cache.getStatistics().getCore().getInMemoryMisses();
    }

    @Override
    public long getOnDiskHits() {
        return cache.getStatistics().getCore().getOnDiskHits();
    }

    @Override
    public long getOnDiskMisses() {
        return cache.getStatistics().getCore().getOnDiskMisses();
    }

    @Override
    public long getEvictionCount() {
        return cache.getStatistics().getCore().getEvictedCount();
    }

    @Override
    public long getCacheHits() {
        return cache.getStatistics().getCore().getCacheHits();
    }

    @Override
    public long getCacheMisses() {
        return cache.getStatistics().getCore().getCacheMisses();
    }

    @Override
    public long getObjectCount() {
        return cache.getStatistics().getCore().getObjectCount();
    }

    @Override
    public long getMemoryStoreObjectCount() {
        return cache.getStatistics().getCore().getMemoryStoreObjectCount();
    }

    @Override
    public long getDiskStoreObjectCount() {
        return cache.getStatistics().getCore().getDiskStoreObjectCount();
    }

    @Override
    public String getAssociatedCacheName() {
        return cache.getStatistics().getCore().getAssociatedCacheName();
    }

    @Override
    public long getOffHeapHits() {
        return cache.getStatistics().getCore().getOffHeapHits();
    }

    @Override
    public long getOffHeapMisses() {
        return cache.getStatistics().getCore().getOffHeapMisses();
    }

    @Override
    public long getWriterQueueSize() {
        return cache.getStatistics().getCore().getWriterQueueLength();
    }

    @Override
    public long getOffHeapStoreObjectCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getLocalHeapSizeString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public float getAverageGetTime() {
        // TODO Auto-generated method stub
        return 0;
    }
}


