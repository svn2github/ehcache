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

import net.sf.ehcache.statistics.LiveCacheStatistics;
import net.sf.ehcache.statistics.sampled.SampledCacheStatistics;

/**
 * An MBean for Cache exposing cache statistics.
 * Extends from both {@link LiveCacheStatistics} and {@link SampledCacheStatistics}
 *
 * <p />
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface SampledCacheMBean extends LiveCacheStatistics, SampledCacheStatistics {
    /**
     * CACHE_ENABLED
     */
    final String CACHE_ENABLED = "CacheEnabled";

    /**
     * CACHE_CHANGED
     */
    final String CACHE_CHANGED = "CacheChanged";

    /**
     * CACHE_FLUSHED
     */
    final String CACHE_FLUSHED = "CacheFlushed";

    /**
     * CACHE_CLEARED
     */
    final String CACHE_CLEARED = "CacheCleared";

    /**
     * CACHE_STATISTICS_ENABLED
     */
    final String CACHE_STATISTICS_ENABLED = "CacheStatisticsEnabled";

    /**
     * CACHE_STATISTICS_RESET
     */
    final String CACHE_STATISTICS_RESET = "CacheStatisticsReset";

    /**
     * Is the cache enabled?
     */
    boolean isEnabled();

    /**
     * Enabled/disable cache coherence mode for this node.
     * @deprecated use {@link #setNodeBulkLoadEnabled(boolean)} instead
     */
    @Deprecated
    void setNodeCoherent(boolean coherent);

    /**
     * Enabled/disable bulk-load mode for this node.
     */
    void setNodeBulkLoadEnabled(boolean bulkLoadEnabled);

    /**
     * Is the cache coherent cluster-wide?
     * @deprecated use {@link #isClusterBulkLoadEnabled()} instead
     */
    @Deprecated
    boolean isClusterCoherent();

    /**
     * Is the cache in bulk-load mode cluster-wide?
     */
    boolean isClusterBulkLoadEnabled();

    /**
     * Is the cache coherent locally?
     * @deprecated use {@link #isNodeBulkLoadEnabled()} instead
     */
    @Deprecated
    boolean isNodeCoherent();

    /**
     * Is the cache in bulk-load mode locally?
     */
    boolean isNodeBulkLoadEnabled();

    /**
     * Enabled/disable the cache.
     */
    void setEnabled(boolean enabled);

    /**
     * Removes all cached items.
     */
    void removeAll();

    /**
     * Flushes all cache items from memory to the disk store, and from the
     * DiskStore to disk.
     */
    void flush();

    /**
     * Gets the status attribute of the Cache.
     *
     * @return The status value from the Status enum class
     */
    String getStatus();

    /**
     * Is the cache configured with Terracotta clustering?
     *
     * @return true if clustered with terracotta
     */
    boolean isTerracottaClustered();

    /**
     * Returns a textual description of a Terracotta-clustered cache's consistency mode.
     * @return "STRONG", "EVENTUAL", or "na" if the cache is not Terracotta-clustered
     */
    String getTerracottaConsistency();

    /**
     * Returns a textual description of a Terracotta-clustered cache's storage-strategy.
     * @return "CDV2", "CLASSIC", or "na" if the cache is not Terracotta-clustered
     */
    String getTerracottaStorageStrategy();

    /**
     * Clear both sampled and cumulative statistics
     */
    void clearStatistics();

    /**
     * Enables statistics collection
     */
    void enableStatistics();

    /**
     * Disables statistics collection. Also disables sampled statistics if it is
     * enabled.
     */
    void disableStatistics();

    /**
     * Controls the statistics. Also controls sampled statistics if it is
     * enabled.
     */
    void setStatisticsEnabled(boolean statsEnabled);

    /**
     * Enables statistics collection. As it requires that normal statistics
     * collection to be enabled, it enables it if its not already
     */
    void enableSampledStatistics();

    /**
     * Disables statistics collection
     */
    void disableSampledStatistics();

    /**
     * Configuration property accessor
     *
     * @return Max entries local heap config setting value
     */
    long getConfigMaxEntriesLocalHeap();

    /**
     * Configuration property accessor
     *
     * @return Max elements in memory config setting value
     * @deprecated use {@link #getConfigMaxEntriesLocalHeap()} instead
     */
    @Deprecated
    int getConfigMaxElementsInMemory();

    /**
     * setConfigMaxElementsInMemory
     * @param maxElements
     */
    void setConfigMaxElementsInMemory(int maxElements);

    /**
     * Configuration property accessor
     *
     * @return Max bytes local heap config setting value
     */
    long getConfigMaxBytesLocalHeap();

    /**
     * Configuration property accessor
     *
     * @return Max entries local offheap config setting value
     */
    long getConfigMaxEntriesLocalOffHeap();

    /**
     * Configuration property accessor
     *
     * @return Max bytes local offheap config setting value
     */
    long getConfigMaxBytesLocalOffHeap();

    /**
     * Configuration property accessor
     *
     * @return Max entries local disk config setting value
     */
    long getConfigMaxEntriesLocalDisk();

    /**
     * Configuration property accessor
     *
     * @return Max elements on disk config setting value
     * @deprecated use {@link #getConfigMaxEntriesLocalDisk()} instead
     */
    @Deprecated
    int getConfigMaxElementsOnDisk();

    /**
     * setConfigMaxElementsOnDisk
     * @param maxElements
     */
    void setConfigMaxElementsOnDisk(int maxElements);

    /**
     * Configuration property accessor
     *
     * @return Max bytes local disk config setting value
     */
    long getConfigMaxBytesLocalDisk();

    /**
     * Configuration property accessor
     *
     * @return a String representation of the policy
     */
    String getConfigMemoryStoreEvictionPolicy();

    /**
     * setConfigMemoryStoreEvictionPolicy
     * @param evictionPolicy
     */
    void setConfigMemoryStoreEvictionPolicy(String evictionPolicy);

    /**
     * Configuration property accessor
     *
     * @return true if set to eternal in config
     */
    boolean isConfigEternal();

    /**
     * setConfigEternal
     * @param eternal
     */
    void setConfigEternal(boolean eternal);

    /**
     * Configuration property accessor
     *
     * @return TTI in config
     */
    long getConfigTimeToIdleSeconds();

    /**
     * setConfigTimeToIdleSeconds
     * @param tti
     */
    void setConfigTimeToIdleSeconds(long tti);

    /**
     * Configuration property accessor
     *
     * @return TTL in config
     */
    long getConfigTimeToLiveSeconds();

    /**
     * setConfigTimeToLiveSeconds
     * @param ttl
     */
    void setConfigTimeToLiveSeconds(long ttl);

    /**
     * Configuration property accessor
     *
     * @return true if overflow to disk specified in config
     */
    boolean isConfigOverflowToDisk();

    /**
     * setConfigOverflowToDisk
     * @param overflowToDisk
     */
    void setConfigOverflowToDisk(boolean overflowToDisk);

    /**
     * Configuration property accessor
     *
     * @return true if configured with disk persistence
     */
    boolean isConfigDiskPersistent();

    /**
     * setConfigDiskPersistent
     * @param diskPersistent
     */
    void setConfigDiskPersistent(boolean diskPersistent);

    /**
     * Configuration property accessor
     *
     * @return Value for disk expiry thread interval in seconds specified in config
     */
    long getConfigDiskExpiryThreadIntervalSeconds();

    /**
     * setConfigDiskExpiryThreadIntervalSeconds
     * @param seconds
     */
    void setConfigDiskExpiryThreadIntervalSeconds(long seconds);

    /**
     * Configuration property accessor
     *
     * @return true if logging is enabled on the cache
     */
    boolean isConfigLoggingEnabled();

    /**
     * setConfigLoggingEnabled
     * @param enabled
     */
    void setConfigLoggingEnabled(boolean enabled);

    /**
     * Configuration property accessor
     *
     * @return true if the cache is pinned
     * @see net.sf.ehcache.config.PinnedConfiguration
     */
    boolean isConfigPinned();

    /**
     * Configuration property accessor
     *
     * @return the store to which this cache is pinned
     * @see net.sf.ehcache.config.PinnedConfiguration
     */
    String getConfigPinnedToStore();

    /**
     * Is there a registered Write-behind CacheWriter
     */
    boolean getHasWriteBehindWriter();

    /**
     * Returns the total length of all write-behind queues for this cache
     * @return writer-behind queue length
     */
    long getWriterQueueLength();

    /**
     * Returns the maximum size of any write-behind queues.
     * @return Maximum elements that can be queued for processing by the write-behind writer
     * @see net.sf.ehcache.config.CacheWriterConfiguration#getWriteBehindMaxQueueSize()
     */
    int getWriterMaxQueueSize();

    /**
     * Returns the number of configured write-behind queues/threads.
     * @return Number of configured processing queues/threads for use by the write-behind writer
     * @see net.sf.ehcache.config.CacheWriterConfiguration#getWriteBehindConcurrency()
     */
    int getWriterConcurrency();

    /**
     * Is the cache a transactional one
     * @see net.sf.ehcache.config.CacheConfiguration.TransactionalMode
     */
    boolean getTransactional();

    /**
     * @return Xa commit rate
     */
    long getTransactionCommitRate();

    /**
     * @return Xa rollback rate
     */
    long getTransactionRollbackRate();

    /**
     * Is the cache configured for search
     * @see net.sf.ehcache.config.Searchable
     */
    boolean getSearchable();

    /**
     * @return search rate
     */
    long getCacheSearchRate();

    /**
     * @return search time
     */
    long getCacheAverageSearchTime();

    /**
     * @return hit rate
     */
    long getCacheHitRate();

    /**
     * @return in-memory hit rate
     */
    long getCacheInMemoryHitRate();

    /**
     * @return off-heap hit rate
     */
    long getCacheOffHeapHitRate();

    /**
     * @return on-disk hit rate
     */
    long getCacheOnDiskHitRate();

    /**
     * @return miss rate
     */
    long getCacheMissRate();

    /**
     * @return in-memory miss rate
     */
    long getCacheInMemoryMissRate();

    /**
     * @return off-heap miss rate
     */
    long getCacheOffHeapMissRate();

    /**
     * @return on-disk miss rate
     */
    long getCacheOnDiskMissRate();

    /**
     * @return put rate
     */
    long getCachePutRate();

    /**
     * @return update rate
     */
    long getCacheUpdateRate();

    /**
     * @return remove rate
     */
    long getCacheRemoveRate();

    /**
     * @return eviction rate
     */
    long getCacheEvictionRate();

    /**
     * @return expiration rate
     */
    long getCacheExpirationRate();

    /**
     * @return average get time (ms.)
     */
    float getCacheAverageGetTime();
}
