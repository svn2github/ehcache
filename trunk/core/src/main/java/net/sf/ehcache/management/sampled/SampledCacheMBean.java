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
    public static final String CACHE_ENABLED = "CacheEnabled";

    /**
     * CACHE_CHANGED
     */
    public static final String CACHE_CHANGED = "CacheChanged";

    /**
     * CACHE_FLUSHED
     */
    public static final String CACHE_FLUSHED = "CacheFlushed";

    /**
     * CACHE_CLEARED
     */
    public static final String CACHE_CLEARED = "CacheCleared";

    /**
     * CACHE_STATISTICS_ENABLED
     */
    public static final String CACHE_STATISTICS_ENABLED = "CacheStatisticsEnabled";

    /**
     * CACHE_STATISTICS_RESET
     */
    public static final String CACHE_STATISTICS_RESET = "CacheStatisticsReset";

    /**
     * Is the cache enabled?
     */
    boolean isEnabled();

    /**
     * Enabled/disable cache coherence mode for this node.
     */
    void setNodeCoherent(boolean coherent);

    /**
     * Is the cache coherent cluster-wide?
     */
    boolean isClusterCoherent();

    /**
     * Is the cache coherent locally?
     */
    boolean isNodeCoherent();

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
    public boolean isTerracottaClustered();

    /**
     * Returns a textual description of a Terracotta-clustered cache's coherence-mode.
     * @return "STRONG" or "EVENTUAL"
     */
    public String getTerracottaConsistency();

    /**
     * Clear both sampled and cumulative statistics
     */
    public void clearStatistics();

    /**
     * Enables statistics collection
     */
    public void enableStatistics();

    /**
     * Disables statistics collection. Also disables sampled statistics if it is
     * enabled.
     */
    public void disableStatistics();

    /**
     * Controls the statistics. Also controls sampled statistics if it is
     * enabled.
     */
    public void setStatisticsEnabled(boolean statsEnabled);

    /**
     * Enables statistics collection. As it requires that normal statistics
     * collection to be enabled, it enables it if its not already
     */
    public void enableSampledStatistics();

    /**
     * Disables statistics collection
     */
    public void disableSampledStatistics();

    /**
     * Configuration property accessor
     *
     * @return Max elements in memory config setting value
     */
    public int getConfigMaxElementsInMemory();

    /**
     * setConfigMaxElementsInMemory
     * @param maxElements
     */
    public void setConfigMaxElementsInMemory(int maxElements);

    /**
     * Configuration property accessor
     *
     * @return Max elements on disk config setting value
     */
    public int getConfigMaxElementsOnDisk();

    /**
     * setConfigMaxElementsOnDisk
     * @param maxElements
     */
    public void setConfigMaxElementsOnDisk(int maxElements);

    /**
     * Configuration property accessor
     *
     * @return a String representation of the policy
     */
    public String getConfigMemoryStoreEvictionPolicy();

    /**
     * setConfigMemoryStoreEvictionPolicy
     * @param evictionPolicy
     */
    public void setConfigMemoryStoreEvictionPolicy(String evictionPolicy);

    /**
     * Configuration property accessor
     *
     * @return true if set to eternal in config
     */
    public boolean isConfigEternal();

    /**
     * setConfigEternal
     * @param eternal
     */
    public void setConfigEternal(boolean eternal);

    /**
     * Configuration property accessor
     *
     * @return TTI in config
     */
    public long getConfigTimeToIdleSeconds();

    /**
     * setConfigTimeToIdleSeconds
     * @param tti
     */
    public void setConfigTimeToIdleSeconds(long tti);

    /**
     * Configuration property accessor
     *
     * @return TTL in config
     */
    public long getConfigTimeToLiveSeconds();

    /**
     * setConfigTimeToLiveSeconds
     * @param ttl
     */
    public void setConfigTimeToLiveSeconds(long ttl);

    /**
     * Configuration property accessor
     *
     * @return true if overflow to disk specified in config
     */
    public boolean isConfigOverflowToDisk();

    /**
     * setConfigOverflowToDisk
     * @param overflowToDisk
     */
    public void setConfigOverflowToDisk(boolean overflowToDisk);

    /**
     * Configuration property accessor
     *
     * @return true if configured with disk persistence
     */
    public boolean isConfigDiskPersistent();

    /**
     * setConfigDiskPersistent
     * @param diskPersistent
     */
    public void setConfigDiskPersistent(boolean diskPersistent);

    /**
     * Configuration property accessor
     *
     * @return Value for disk expiry thread interval in seconds specified in config
     */
    public long getConfigDiskExpiryThreadIntervalSeconds();

    /**
     * setConfigDiskExpiryThreadIntervalSeconds
     * @param seconds
     */
    public void setConfigDiskExpiryThreadIntervalSeconds(long seconds);

    /**
     * Configuration property accessor
     *
     * @return true if logging is enabled on the cache
     */
    public boolean isConfigLoggingEnabled();

    /**
     * setConfigLoggingEnabled
     * @param enabled
     */
    public void setConfigLoggingEnabled(boolean enabled);

    /**
     * Is there a registered Write-behind CacheWriter
     */
    boolean getHasWriteBehindWriter();

    /**
     * Returns the maximum size of any write-behind queues.
     * @return Maximum elements that can be queued for processing by the write-behind writer
     * @see net.sf.ehcache.config.CacheWriterConfiguration#getWriteBehindMaxQueueSize()
     */
    public int getWriterMaxQueueSize();

    /**
     * Returns the number of configured write-behind queues/threads.
     * @return Number of configured processing queues/threads for use by the write-behind writer
     * @see net.sf.ehcache.config.CacheWriterConfiguration#getWriteBehindConcurrency()
     */
    public int getWriterConcurrency();

    /**
     * Is the cache a transactional one
     * @see net.sf.ehcache.config.CacheConfiguration.TransactionalMode
     */
    public boolean getTransactional();
}
