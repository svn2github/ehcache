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

package net.sf.ehcache.management;

/**
 * A JMX MBean interface for the configuration of a cache
 * @author Greg Luck
 * @version $Id$
 * @since 1.3
 */
public interface CacheConfigurationMBean {

    /**
     * Accessor
     */
    String getName();

    /**
     * Accessor
     */
    boolean isLoggingEnabled();

    /**
     * setLoggingEnabled
     *
     * @param loggingEnabled
     */
    void setLoggingEnabled(boolean loggingEnabled);

    /**
     * Accessor
     *
     * @deprecated use {@link #getMaxEntriesLocalHeap()}
     */
    @Deprecated
    int getMaxElementsInMemory();

    /**
     * setMaxElementsInMemory
     *
     * @param maxElements
     * @deprecated use {@link #setMaxEntriesLocalHeap(long)}
     */
    @Deprecated
    void setMaxElementsInMemory(int maxElements);

    /**
     * Accessor
     *
     * @deprecated use {@link #getMaxEntriesLocalDisk()}
     */
    @Deprecated
    int getMaxElementsOnDisk();

    /**
     * setMaxElementsOnDisk
     *
     * @param maxElements
     * @deprecated use {@link #setMaxEntriesLocalDisk(long)}
     */
    @Deprecated
    void setMaxElementsOnDisk(int maxElements);

    /**
     * Configured maximum number of entries for the local disk store.
     */
    long getMaxEntriesLocalDisk();

    /**
     * Configured maximum number of entries for the local memory heap.
     */
    long getMaxEntriesLocalHeap();

    /**
     * Configured maximum number of entries for the local disk store.
     */
    void setMaxEntriesLocalDisk(long maxEntries);

    /**
     * Configured maximum number of entries for the local memory heap.
     */
    void setMaxEntriesLocalHeap(long maxEntries);

    /**
     * Configured maximum number of bytes for the local disk store.
     */
    long getMaxBytesLocalDisk();

    /**
     * Configured maximum number of bytes for the local memory heap.
     */
    long getMaxBytesLocalHeap();

    /**
     * Configured maximum number of bytes for the local off-heap memory.
     */
    long getMaxBytesLocalOffHeap();

    /**
     * Accessor
     * @return a String representation of the policy
     */
    String getMemoryStoreEvictionPolicy();

    /**
     * setMemoryStoreEvictionPolicy
     *
     * @param policy
     */
    void setMemoryStoreEvictionPolicy(String policy);

    /**
     * Accessor
     */
    boolean isEternal();

    /**
     * setEternal
     *
     * @param eternal
     */
    void setEternal(boolean eternal);

    /**
     * Accessor
     */
    long getTimeToIdleSeconds();

    /**
     * setTimeToIdleSeconds
     *
     * @param tti
     */
    void setTimeToIdleSeconds(long tti);

    /**
     * Accessor
     */
    long getTimeToLiveSeconds();

    /**
     * setTimeToLiveSeconds
     *
     * @param ttl
     */
    void setTimeToLiveSeconds(long ttl);

    /**
     * Accessor
     */
    boolean isOverflowToDisk();

    /**
     * setOverflowToDisk
     *
     * @param overflow
     */
    void setOverflowToDisk(boolean overflow);

    /**
     * Accessor
     */
    boolean isDiskPersistent();

    /**
     * setDiskPersistent
     *
     * @param diskPersistent
     */
    void setDiskPersistent(boolean diskPersistent);

    /**
     * Accessor
     */
    long getDiskExpiryThreadIntervalSeconds();

    /**
     * setDiskExpiryThreadIntervalSeconds
     *
     * @param seconds
     */
    void setDiskExpiryThreadIntervalSeconds(long seconds);

    /**
     * Accessor
     */
    int getDiskSpoolBufferSizeMB();

    /**
     * setDiskSpoolBufferSizeMB
     *
     * @param diskSpoolBufferSizeMB
     */
    void setDiskSpoolBufferSizeMB(int diskSpoolBufferSizeMB);

    /**
     * Accessor
     */
    boolean isTerracottaClustered();

    /**
     * Accessor
     */
    String getTerracottaConsistency();

    /**
     * Accessor
     */
    boolean isOverflowToOffHeap();

    /**
     * Accessor
     *
     * @deprecated use {@link #getMaxBytesLocalOffHeap()}
     */
    @Deprecated
    long getMaxMemoryOffHeapInBytes();
}
