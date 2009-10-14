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

import net.sf.ehcache.statistics.LiveCacheStatistics;
import net.sf.ehcache.statistics.sampled.SampledCacheStatistics;

/**
 * An MBean for Cache exposing cache statistics.
 * Extends from both {@link LiveCacheStatistics} and
 * {@link SampledCacheStatistics}
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * @since 1.7
 */
public interface SampledCacheMBean extends LiveCacheStatistics,
        SampledCacheStatistics {

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
     * Configuration property accessor
     * 
     * @return Max elements on disk config setting value
     */
    public int getConfigMaxElementsOnDisk();

    /**
     * Configuration property accessor
     * 
     * @return a String representation of the policy
     */
    public String getConfigMemoryStoreEvictionPolicy();

    /**
     * Configuration property accessor
     * 
     * @return true if set to eternal in config
     */
    public boolean isConfigEternal();

    /**
     * Configuration property accessor
     * 
     * @return TTI in config
     */
    public long getConfigTimeToIdleSeconds();

    /**
     * Configuration property accessor
     * 
     * @return TTL in config
     */
    public long getConfigTimeToLiveSeconds();

    /**
     * Configuration property accessor
     * 
     * @return true if overflow to disk specified in config
     */
    public boolean isConfigOverflowToDisk();

    /**
     * Configuration property accessor
     * 
     * @return true if configured with disk persistence
     */
    public boolean isConfigDiskPersistent();

    /**
     * Configuration property accessor
     * 
     * @return Value for disk expiry thread interval in seconds specified in config
     */
    public long getConfigDiskExpiryThreadIntervalSeconds();

}
