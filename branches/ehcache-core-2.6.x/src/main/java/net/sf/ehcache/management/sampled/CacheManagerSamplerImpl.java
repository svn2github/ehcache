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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.statistics.LiveCacheStatistics;
import net.sf.ehcache.statistics.sampled.SampledCacheStatistics;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;

import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of {@link CacheManagerSampler}
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @author <a href="mailto:byoukste@terracottatech.com">byoukste</a>
 */
public class CacheManagerSamplerImpl implements CacheManagerSampler {

    private final CacheManager cacheManager;

    /**
     * Constructor taking the backing {@link CacheManager}
     *
     * @param cacheManager to wrap
     */
    public CacheManagerSamplerImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * {@inheritDoc}
     */
    public void clearAll() {
        cacheManager.clearAll();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getCacheNames() throws IllegalStateException {
        return cacheManager.getCacheNames();
    }

    /**
     * {@inheritDoc}
     */
    public String getStatus() {
        return cacheManager.getStatus().toString();
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, long[]> getCacheMetrics() {
        Map<String, long[]> result = new HashMap<String, long[]>();
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result.put(cacheName, new long[] {stats.getCacheHitMostRecentSample(),
                    stats.getCacheMissNotFoundMostRecentSample()
                    + stats.getCacheMissExpiredMostRecentSample(),
                    stats.getCacheElementPutMostRecentSample()});
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheHitRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheHitMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheInMemoryHitRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheHitInMemoryMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheOffHeapHitRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheHitOffHeapMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheOnDiskHitRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheHitOnDiskMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheMissRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += (stats.getCacheMissNotFoundMostRecentSample()
                           + stats.getCacheMissExpiredMostRecentSample());
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheInMemoryMissRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheMissInMemoryMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheOffHeapMissRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheMissOffHeapMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheOnDiskMissRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheMissOnDiskMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCachePutRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheElementPutMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheUpdateRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheElementUpdatedMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheRemoveRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheElementRemovedMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheEvictionRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheElementEvictedMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheExpirationRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheElementExpiredMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public float getCacheAverageGetTime() {
        float result = 0;
        int instances = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                result += cache.getAverageGetTime();
                instances++;
            }
        }
        return instances > 0 ? result / instances : 0;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheSearchRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getSearchesPerSecond();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getCacheAverageSearchTime() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getAverageSearchTime();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean getHasWriteBehindWriter() {
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                if (cache.getWriterManager() instanceof WriteBehindManager &&
                    cache.getRegisteredCacheWriter() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public long getWriterQueueLength() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                LiveCacheStatistics stats = cache.getLiveCacheStatistics();
                result += Math.max(stats.getWriterQueueLength(), 0);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int getWriterMaxQueueSize() {
        int result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                CacheWriterConfiguration writerConfig = cache.getCacheConfiguration().getCacheWriterConfiguration();
                result += (writerConfig.getWriteBehindMaxQueueSize() * writerConfig.getWriteBehindConcurrency());
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalDisk() {
        return cacheManager.getConfiguration().getMaxBytesLocalDisk();
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalDiskAsString() {
        return cacheManager.getConfiguration().getMaxBytesLocalDiskAsString();
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalDisk(long maxBytes) {
        try {
            cacheManager.getConfiguration().setMaxBytesLocalDisk(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalDiskAsString(String maxBytes) {
        try {
            cacheManager.getConfiguration().setMaxBytesLocalDisk(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalHeap() {
        return cacheManager.getConfiguration().getMaxBytesLocalHeap();
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalHeapAsString() {
        return cacheManager.getConfiguration().getMaxBytesLocalHeapAsString();
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalHeap(long maxBytes) {
        try {
            cacheManager.getConfiguration().setMaxBytesLocalHeap(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxBytesLocalHeapAsString(String maxBytes) {
        try {
            cacheManager.getConfiguration().setMaxBytesLocalHeap(maxBytes);
        } catch (RuntimeException e) {
            throw Utils.newPlainException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxBytesLocalOffHeap() {
        return cacheManager.getConfiguration().getMaxBytesLocalOffHeap();
    }

    /**
     * {@inheritDoc}
     */
    public String getMaxBytesLocalOffHeapAsString() {
        return cacheManager.getConfiguration().getMaxBytesLocalOffHeapAsString();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return cacheManager.getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getClusterUUID() {
        return cacheManager.getClusterUUID();
    }

    /**
     * {@inheritDoc}
     */
    public void clearStatistics() {
        for (String cacheName : getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clearStatistics();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void enableStatistics() {
        for (String cacheName : getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // enables regular statistics also
                cache.setSampledStatisticsEnabled(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void disableStatistics() {
        for (String cacheName : getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // disables regular statistics also
                cache.setStatisticsEnabled(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsEnabled(boolean enabled) {
        if (enabled) {
            enableStatistics();
        } else {
            disableStatistics();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        for (String cacheName : getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                if (!cache.isSampledStatisticsEnabled()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String generateActiveConfigDeclaration() {
        return this.cacheManager.getActiveConfigurationText();
    }

    /**
     * {@inheritDoc}
     */
    public String generateActiveConfigDeclaration(String cacheName) {
        return this.cacheManager.getActiveConfigurationText(cacheName);
    }

    /**
     * {@inheritDoc}
     */
    public boolean getTransactional() {
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null && cache.getCacheConfiguration().getTransactionalMode().isTransactional()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean getSearchable() {
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null && cache.getCacheConfiguration().getSearchable() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionCommittedCount() {
        return this.cacheManager.getTransactionController().getTransactionCommittedCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionCommitRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheXaCommitsMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionRolledBackCount() {
        return this.cacheManager.getTransactionController().getTransactionRolledBackCount();
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionRollbackRate() {
        long result = 0;
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                SampledCacheStatistics stats = cache.getSampledCacheStatistics();
                result += stats.getCacheXaRollbacksMostRecentSample();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getTransactionTimedOutCount() {
        return this.cacheManager.getTransactionController().getTransactionTimedOutCount();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEnabled() throws CacheException {
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null && cache.isDisabled()) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setEnabled(boolean enabled) {
        for (String cacheName : getCacheNames()) {
            Ehcache cache = cacheManager.getEhcache(cacheName);
            if (cache != null) {
                cache.setDisabled(!enabled);
            }
        }
    }
}