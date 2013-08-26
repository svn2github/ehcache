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

import java.util.ArrayList;

import net.sf.ehcache.CacheOperationOutcomes.ClusterEventOutcomes;
import net.sf.ehcache.CacheOperationOutcomes.NonStopOperationOutcomes;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfigurationListener;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.util.CacheTransactionHelper;
import net.sf.ehcache.util.counter.sampled.SampledCounter;
import net.sf.ehcache.util.counter.sampled.SampledRateCounter;
import net.sf.ehcache.util.counter.sampled.TimeStampedCounterValue;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.statistics.archive.Timestamped;

/**
 * An implementation of {@link CacheSampler}
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 * 
 *         There is stupid here -- *Sample() is the same as *Rate()
 */
public class CacheSamplerImpl implements CacheSampler, CacheConfigurationListener {
    private static final double ONE_HUNDRED = 100.0d;

    private static final int PERCENTAGE_DIVISOR = 100;

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
    @Override
    public boolean isEnabled() {
        return !cache.isDisabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
    public boolean isNodeBulkLoadEnabled() {
        return cache.getCacheConfiguration().isTerracottaClustered() && cache.isNodeBulkLoadEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
    public String getCacheName() {
        return cache.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatus() {
        return cache.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public long getAverageGetTimeNanosMostRecentSample() {
        return getAverageGetTimeNanos();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheEvictionRate() {
        return cache.getStatistics().cacheEvictionOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheElementEvictedMostRecentSample() {
        return getCacheEvictionRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheExpirationRate() {
        return cache.getStatistics().cacheExpiredOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheElementExpiredMostRecentSample() {
        return getCacheExpirationRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCachePutRate() {
        return cache.getStatistics().cachePutOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheElementPutMostRecentSample() {
        return getCachePutRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheRemoveRate() {
        return cache.getStatistics().cacheRemoveOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheElementRemovedMostRecentSample() {
        return getCacheRemoveRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheUpdateRate() {
        return cache.getStatistics().cachePutReplacedOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheElementUpdatedMostRecentSample() {
        return getCacheUpdateRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheInMemoryHitRate() {
        return cache.getStatistics().localHeapHitOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheHitInMemoryMostRecentSample() {
        return getCacheInMemoryHitRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheOffHeapHitRate() {
        return cache.getStatistics().localOffHeapHitOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheHitOffHeapMostRecentSample() {
        return getCacheOffHeapHitRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheHitRate() {
        return cache.getStatistics().cacheHitOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheHitMostRecentSample() {
        return getCacheHitRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheOnDiskHitRate() {
        return cache.getStatistics().localDiskHitOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheHitOnDiskMostRecentSample() {
        return getCacheOnDiskHitRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissExpiredMostRecentSample() {
        return cache.getStatistics().cacheMissExpiredOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissRate() {
        return cache.getStatistics().cacheMissOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissMostRecentSample() {
        return getCacheMissRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheInMemoryMissRate() {
        return cache.getStatistics().localHeapMissOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissInMemoryMostRecentSample() {
        return getCacheInMemoryMissRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheOffHeapMissRate() {
        return cache.getStatistics().localOffHeapMissOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissOffHeapMostRecentSample() {
        return getCacheOffHeapMissRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheOnDiskMissRate() {
        return cache.getStatistics().localDiskMissOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissOnDiskMostRecentSample() {
        return getCacheOnDiskMissRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissNotFoundMostRecentSample() {
        return cache.getStatistics().cacheMissNotFoundOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        cache.getCacheConfiguration().removeConfigurationListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTerracottaClustered() {
        return this.cache.getCacheConfiguration().isTerracottaClustered();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTerracottaConsistency() {
        Consistency consistency = this.cache.getCacheConfiguration().getTerracottaConsistency();
        return consistency != null ? consistency.name() : "na";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAverageGetTime() {
        try {
            return cache.getStatistics().cacheGetOperation().latency().average().value().longValue();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getMaxGetTimeNanos() {
        try {
            return (Long)cache.getStatistics().cacheGetOperation().latency().maximum().value();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getMinGetTimeNanos() {
        try {
            return (Long)cache.getStatistics().cacheGetOperation().latency().minimum().value();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getXaCommitCount() {
        try {
            return cache.getStatistics().xaCommitCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getXaRollbackCount() {
        try {
            return cache.getStatistics().xaRollbackCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getXaRecoveredCount() {
        try {
            return cache.getStatistics().xaRecoveryCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getHasWriteBehindWriter() {
        return cache.getWriterManager() instanceof WriteBehindManager && cache.getRegisteredCacheWriter() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getWriterQueueLength() {
        try {
            return cache.getStatistics().getWriterQueueLength();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWriterMaxQueueSize() {
        return cache.getCacheConfiguration().getCacheWriterConfiguration().getWriteBehindMaxQueueSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWriterConcurrency() {
        return cache.getCacheConfiguration().getCacheWriterConfiguration().getWriteBehindConcurrency();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheHitCount() {
        try {
            return cache.getStatistics().cacheHitCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissCount() {
        try {
            return cache.getStatistics().cacheMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInMemoryMissCount() {
        try {
            return cache.getStatistics().localHeapMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getOffHeapMissCount() {
        try {
            return cache.getStatistics().localOffHeapMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getOnDiskMissCount() {
        try {
            return cache.getStatistics().localDiskMissCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheMissCountExpired() {
        try {
            return cache.getStatistics().cacheMissExpiredCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDiskExpiryThreadIntervalSeconds() {
        return cache.getCacheConfiguration().getDiskExpiryThreadIntervalSeconds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public long getMaxEntriesLocalHeap() {
        return cache.getCacheConfiguration().getMaxEntriesLocalHeap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public long getMaxBytesLocalHeap() {
        return cache.getCacheConfiguration().getMaxBytesLocalHeap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaxBytesLocalHeap(long maxBytes) {
        try {
            if (cache.getCacheManager().getConfiguration().isMaxBytesLocalHeapSet()) {
                long heapPoolSize = cache.getCacheManager().getConfiguration().getMaxBytesLocalHeap();
                if (maxBytes > heapPoolSize) {
                    throw new IllegalArgumentException("Requested maxBytesLocalHeap (" + maxBytes
                            + ") greater than available CacheManager heap pool size (" + heapPoolSize + ")");
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
    @Override
    public void setMaxBytesLocalHeapAsString(String maxBytes) {
        try {
            cache.getCacheConfiguration().setMaxBytesLocalHeap(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }

        if (cache.getCacheConfiguration().isMaxBytesLocalHeapPercentageSet()) {
            long cacheAssignedMem = cache.getCacheManager().getConfiguration().getMaxBytesLocalHeap()
                    * cache.getCacheConfiguration().getMaxBytesLocalHeapPercentage() / PERCENTAGE_DIVISOR;
            setMaxBytesLocalHeap(cacheAssignedMem);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMaxBytesLocalHeapAsString() {
        return cache.getCacheConfiguration().getMaxBytesLocalHeapAsString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMaxEntriesLocalDisk() {
        return cache.getCacheConfiguration().getMaxEntriesLocalDisk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public void setMaxBytesLocalDisk(long maxBytes) {
        try {
            if (cache.getCacheManager().getConfiguration().isMaxBytesLocalDiskSet()) {
                long diskPoolSize = cache.getCacheManager().getConfiguration().getMaxBytesLocalDisk();
                if (maxBytes > diskPoolSize) {
                    throw new IllegalArgumentException("Requested maxBytesLocalDisk (" + maxBytes
                            + ") greater than available CacheManager disk pool size (" + diskPoolSize + ")");
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
    @Override
    public void setMaxBytesLocalDiskAsString(String maxBytes) {
        try {
            cache.getCacheConfiguration().setMaxBytesLocalDisk(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }

        if (cache.getCacheConfiguration().isMaxBytesLocalDiskPercentageSet()) {
            long cacheAssignedMem = cache.getCacheManager().getConfiguration().getMaxBytesLocalDisk()
                    * cache.getCacheConfiguration().getMaxBytesLocalDiskPercentage() / PERCENTAGE_DIVISOR;
            setMaxBytesLocalDisk(cacheAssignedMem);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMaxBytesLocalDiskAsString() {
        return cache.getCacheConfiguration().getMaxBytesLocalDiskAsString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxElementsOnDisk() {
        return cache.getCacheConfiguration().getMaxElementsOnDisk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMaxEntriesInCache() {
        return cache.getCacheConfiguration().getMaxEntriesInCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public void setMaxEntriesInCache(long maxEntries) {
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
    @Override
    public long getMaxBytesLocalDisk() {
        return cache.getCacheConfiguration().getMaxBytesLocalDisk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMaxBytesLocalOffHeap() {
        return cache.getCacheConfiguration().getMaxBytesLocalOffHeap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMaxBytesLocalOffHeapAsString() {
        return cache.getCacheConfiguration().getMaxBytesLocalOffHeapAsString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMemoryStoreEvictionPolicy() {
        return cache.getCacheConfiguration().getMemoryStoreEvictionPolicy().toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public long getTimeToIdleSeconds() {
        return cache.getCacheConfiguration().getTimeToIdleSeconds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public long getTimeToLiveSeconds() {
        return cache.getCacheConfiguration().getTimeToLiveSeconds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public boolean isDiskPersistent() {
        return cache.getCacheConfiguration().isDiskPersistent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPersistenceStrategy() {
        PersistenceConfiguration pc = cache.getCacheConfiguration().getPersistenceConfiguration();
        return pc != null ? pc.getStrategy().name() : "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public boolean isOverflowToOffHeap() {
        return cache.getCacheConfiguration().isOverflowToOffHeap();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEternal() {
        return cache.getCacheConfiguration().isEternal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public boolean isOverflowToDisk() {
        return cache.getCacheConfiguration().isOverflowToDisk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public boolean isLoggingEnabled() {
        return cache.getCacheConfiguration().getLogging();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public boolean isPinned() {
        return cache.getCacheConfiguration().getPinningConfiguration() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPinnedToStore() {
        PinningConfiguration pinningConfig = cache.getCacheConfiguration().getPinningConfiguration();
        return pinningConfig != null ? pinningConfig.getStore().name() : "na";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getEvictedCount() {
        try {
            return cache.getStatistics().cacheEvictedCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getExpiredCount() {
        try {
            return cache.getStatistics().cacheExpiredCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInMemoryHitCount() {
        try {
            return cache.getStatistics().localHeapHitCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getOffHeapHitCount() {
        try {
            return cache.getStatistics().localOffHeapHitCount();
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
    @Override
    public long getOffHeapSize() {
        return getLocalOffHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getOnDiskHitCount() {
        try {
            return cache.getStatistics().localDiskHitCount();
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
    @Override
    public long getOnDiskSize() {
        return getLocalDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLocalDiskSize() {
        try {
            return cache.getStatistics().getLocalDiskSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLocalHeapSize() {
        try {
            return cache.getStatistics().getLocalHeapSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLocalOffHeapSize() {
        try {
            return cache.getStatistics().getLocalOffHeapSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLocalDiskSizeInBytes() {
        try {
            return cache.getStatistics().getLocalDiskSizeInBytes();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLocalHeapSizeInBytes() {
        try {
            return cache.getStatistics().getLocalHeapSizeInBytes();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLocalOffHeapSizeInBytes() {
        try {
            return cache.getStatistics().getLocalOffHeapSizeInBytes();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPutCount() {
        try {
            return cache.getStatistics().cachePutCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRemovedCount() {
        try {
            return cache.getStatistics().cacheRemoveCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSize() {
        try {
            return cache.getStatistics().getSize();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInMemorySize() {
        return getLocalHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUpdateCount() {
        try {
            return cache.getStatistics().cachePutUpdatedCount();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deregistered(CacheConfiguration config) {
        /**/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void maxBytesLocalHeapChanged(final long oldValue, final long newValue) {
        if (oldValue != newValue) {
            setMaxBytesLocalHeap(newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void maxBytesLocalDiskChanged(final long oldValue, final long newValue) {
        if (oldValue != newValue) {
            setMaxBytesLocalDisk(newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void diskCapacityChanged(int oldCapacity, int newCapacity) {
        if (oldCapacity != newCapacity) {
            setMaxElementsOnDisk(newCapacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void maxEntriesInCacheChanged(long oldCapacity, long newCapacity) {
        if (oldCapacity != newCapacity) {
            setMaxEntriesInCache(newCapacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loggingChanged(boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            setLoggingEnabled(newValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void memoryCapacityChanged(int oldCapacity, int newCapacity) {
        if (oldCapacity != newCapacity) {
            setMaxEntriesLocalHeap(newCapacity);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registered(CacheConfiguration config) {
        /**/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void timeToIdleChanged(long oldTimeToIdle, long newTimeToIdle) {
        if (oldTimeToIdle != newTimeToIdle) {
            setTimeToIdleSeconds(newTimeToIdle);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void timeToLiveChanged(long oldTimeToLive, long newTimeToLive) {
        if (oldTimeToLive != newTimeToLive) {
            setTimeToLiveSeconds(newTimeToLive);
        }
    }
 
    /**
     * {@inheritDoc}
     */
    @Override
    public long getAverageSearchTime() {
        return cache.getStatistics().cacheSearchOperation().latency().average().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getSearchesPerSecond() {
        // TODO I can't math.
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getTransactional() {
        return cache.getCacheConfiguration().getTransactionalMode().isTransactional();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getSearchable() {
        return cache.getCacheConfiguration().getSearchable() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheSearchRate() {
        return cache.getStatistics().cacheSearchOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTransactionCommitRate() {
        return cache.getStatistics().xaRecoveryOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheXaCommitsMostRecentSample() {
        return getTransactionCommitRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTransactionRollbackRate() {
        return cache.getStatistics().xaRollbackOperation().rate().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCacheXaRollbacksMostRecentSample() {
        return getTransactionRollbackRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocalHeapCountBased() {
      return cache.getCacheConfiguration()
                 .getMaxBytesLocalHeap() <= 0 && !(cache.getCacheManager() != null && cache.getCacheManager()
          .getConfiguration()
          .isMaxBytesLocalHeapSet());
    }

    /**
     * A package friendly method to allow dependants access to the underlying {@link Ehcache} object. This is available
     * in order to allow {@link SampledCache} objects to continue to provide deprecated methods on the {@link SampledCacheMBean} interface,
     * rather than burdening {@link CacheSampler} with these now irrelevant methods. This helper method
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
    @Override
    public int getCacheHitRatio() {
        return (int) (cache.getStatistics().getExtended().cacheHitRatio().value().doubleValue() * ONE_HUNDRED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCacheHitRatioMostRecentSample() {
        return getCacheHitRatio();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitRatioSample() {
        return new SampledCounterProxy<Double>(cache.getStatistics().getExtended().cacheHitRatio()) {

            @Override
            public TimeStampedCounterValue getMostRecentSample() {
                return new TimeStampedCounterValue(System.currentTimeMillis(), getValue());
            }

            @Override
            public TimeStampedCounterValue[] getAllSampleValues() {
                ArrayList<TimeStampedCounterValue> arr = new ArrayList<TimeStampedCounterValue>();
                for (Timestamped<Double> ts : rate.history()) {
                    arr.add(new TimeStampedCounterValue(ts.getTimestamp(), (int) (ts.getSample().doubleValue() * ONE_HUNDRED)));
                }
                return sortAndPresent(arr);
            }

            @Override
            public long getValue() {
                return (long) (rate.value().doubleValue() * ONE_HUNDRED);
            }

        };
    }

    @Override
    public long getAverageGetTimeNanos() {
        return cache.getStatistics().cacheGetOperation().latency().average().value().longValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheHitOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitInMemorySample() {
        return new SampledCounterProxy(cache.getStatistics().localHeapHitOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitOffHeapSample() {
        return new SampledCounterProxy(cache.getStatistics().localOffHeapHitOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheHitOnDiskSample() {
        return new SampledCounterProxy(cache.getStatistics().localDiskHitOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheMissOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissInMemorySample() {
        return new SampledCounterProxy(cache.getStatistics().localHeapMissOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissOffHeapSample() {
        return new SampledCounterProxy(cache.getStatistics().localOffHeapMissOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissOnDiskSample() {
        return new SampledCounterProxy(cache.getStatistics().localDiskMissOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissExpiredSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheMissExpiredOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheMissNotFoundSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheMissNotFoundOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementEvictedSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheEvictionOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementRemovedSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheRemoveOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementExpiredSample() {
        return new SampledCounterProxy(cache.getStatistics().cacheExpiredOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementPutSample() {
        return new SampledCounterProxy(cache.getStatistics().cachePutOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheElementUpdatedSample() {
        return new SampledCounterProxy(cache.getStatistics().cachePutReplacedOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledRateCounter getAverageGetTimeSample() {
        return new SampledRateCounterProxy(cache.getStatistics().cacheGetOperation().latency().average());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledRateCounter getAverageSearchTimeSample() {
        return new SampledRateCounterProxy(cache.getStatistics().cacheSearchOperation().latency().average());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getSearchesPerSecondSample() {
        return new SampledRateCounterProxy(cache.getStatistics().cacheSearchOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheXaCommitsSample() {
        return new SampledRateCounterProxy(cache.getStatistics().xaCommitSuccessOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getCacheXaRollbacksSample() {
        return new SampledRateCounterProxy(cache.getStatistics().xaRollbackOperation().rate());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getSizeSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getLocalHeapSizeSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().localHeapSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getLocalHeapSizeInBytesSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().localHeapSizeInBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getLocalOffHeapSizeSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().localOffHeapSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getLocalOffHeapSizeInBytesSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().localOffHeapSizeInBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getLocalDiskSizeSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().localDiskSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getLocalDiskSizeInBytesSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().localDiskSizeInBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getRemoteSizeSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().remoteSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SampledCounter getWriterQueueLengthSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().writerQueueLength());
    }

    @Override
    public long getAverageSearchTimeNanos() {
        return getAverageSearchTime();
    }

    @Override
    public long getCacheClusterOfflineCount() {
        try {
            return cache.getStatistics().getExtended().clusterEvent().component(ClusterEventOutcomes.OFFLINE).count().value();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }    
    }

    @Override
    public long getCacheClusterRejoinCount() {
        try {
            return cache.getStatistics().getExtended().clusterEvent().component(ClusterEventOutcomes.REJOINED).count().value();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }    
    }

    @Override
    public long getCacheClusterOnlineCount() {
        try {
            return cache.getStatistics().getExtended().clusterEvent().component(ClusterEventOutcomes.ONLINE).count().value();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }    
    }

    @Override
    public long getCacheClusterOfflineMostRecentSample() {
        return cache.getStatistics().getExtended().clusterEvent().component(ClusterEventOutcomes.OFFLINE).rate().value().longValue();
    }

    @Override
    public long getCacheClusterRejoinMostRecentSample() {
        return cache.getStatistics().getExtended().clusterEvent().component(ClusterEventOutcomes.REJOINED).rate().value().longValue();
    }

    @Override
    public long getCacheClusterOnlineMostRecentSample() {
        return cache.getStatistics().getExtended().clusterEvent().component(ClusterEventOutcomes.ONLINE).rate().value().longValue();
    }

    @Override
    public SampledCounter getCacheClusterOfflineSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().clusterEvent().component(ClusterEventOutcomes.OFFLINE).rate());
    }

    @Override
    public SampledCounter getCacheClusterOnlineSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().clusterEvent().component(ClusterEventOutcomes.ONLINE).rate());
    }

    @Override
    public SampledCounter getCacheClusterRejoinSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().clusterEvent().component(ClusterEventOutcomes.REJOINED).rate());
    }

    @Override
    public long getMostRecentRejoinTimeStampMillis() {
        try {
            return cache.getStatistics().getExtended().mostRecentRejoinTimeStampMillis().value().longValue();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    @Override
    public SampledCounter getMostRecentRejoinTimestampMillisSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().mostRecentRejoinTimeStampMillis());
    }

    @Override
    public long getNonStopSuccessCount() {
        try {
            return cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.SUCCESS).count().value();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }    
    }

    @Override
    public long getNonStopFailureCount() {
        try {
            return cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.FAILURE).count().value();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }    
    }

    @Override
    public long getNonStopRejoinTimeoutCount() {
        try {
            return cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.REJOIN_TIMEOUT).count().value();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }    
    }

    @Override
    public long getNonStopTimeoutCount() {
        try {
            return cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.TIMEOUT).count().value();
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }    
    }

    @Override
    public long getNonStopSuccessMostRecentSample() {
        return cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.SUCCESS).rate().value().longValue();
    }

    @Override
    public long getNonStopFailureMostRecentSample() {
        return cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.FAILURE).rate().value().longValue();
    }

    @Override
    public long getNonStopRejoinTimeoutMostRecentSample() {
        return cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.REJOIN_TIMEOUT).rate().value().longValue();
    }

    @Override
    public long getNonStopTimeoutMostRecentSample() {
        return cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.TIMEOUT).rate().value().longValue();
    }

    @Override
    public SampledCounter getNonStopSuccessSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.SUCCESS).rate());
    }

    @Override
    public SampledCounter getNonStopFailureSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.FAILURE).rate());
    }

    @Override
    public SampledCounter getNonStopRejoinTimeoutSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.REJOIN_TIMEOUT).rate());
    }

    @Override
    public SampledCounter getNonStopTimeoutSample() {
        return new SampledRateCounterProxy(cache.getStatistics().getExtended().nonstop().component(NonStopOperationOutcomes.TIMEOUT).rate());
    }

    @Override
    public long getNonStopSuccessRate() {
        return getNonStopSuccessMostRecentSample();
    }

    @Override
    public long getNonStopFailureRate() {
        return getNonStopFailureMostRecentSample();
    }

    @Override
    public long getNonStopRejoinTimeoutRate() {
        return getNonStopRejoinTimeoutMostRecentSample();
    }

    @Override
    public long getNonStopTimeoutRate() {
        return getNonStopTimeoutMostRecentSample();
    }

    @Override
    public int getNonstopTimeoutRatio() {
        return (int) (cache.getStatistics().getExtended().nonstopTimeoutRatio().value().doubleValue() * ONE_HUNDRED);
    }
}
