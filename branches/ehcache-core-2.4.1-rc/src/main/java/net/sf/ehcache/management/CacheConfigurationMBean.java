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
    public String getName();

    /**
     * Accessor
     */
    public boolean isLoggingEnabled();

    /**
     * setLoggingEnabled
     *
     * @param loggingEnabled
     */
    public void setLoggingEnabled(boolean loggingEnabled);

    /**
     * Accessor
     */
    public int getMaxElementsInMemory();

    /**
     * setMaxElementsInMemory
     *
     * @param maxElements
     */
    public void setMaxElementsInMemory(int maxElements);

    /**
     * Accessor
     */
    public int getMaxElementsOnDisk();

    /**
     * setMaxElementsOnDisk
     *
     * @param maxElements
     */
    public void setMaxElementsOnDisk(int maxElements);

    /**
     * Accessor
     * @return a String representation of the policy
     */
    public String getMemoryStoreEvictionPolicy();

    /**
     * setMemoryStoreEvictionPolicy
     *
     * @param policy
     */
    public void setMemoryStoreEvictionPolicy(String policy);

    /**
     * Accessor
     */
    public boolean isEternal();

    /**
     * setEternal
     *
     * @param eternal
     */
    public void setEternal(boolean eternal);

    /**
     * Accessor
     */
    public long getTimeToIdleSeconds();

    /**
     * setTimeToIdleSeconds
     *
     * @param tti
     */
    public void setTimeToIdleSeconds(long tti);

    /**
     * Accessor
     */
    public long getTimeToLiveSeconds();

    /**
     * setTimeToLiveSeconds
     *
     * @param ttl
     */
    public void setTimeToLiveSeconds(long ttl);

    /**
     * Accessor
     */
    public boolean isOverflowToDisk();

    /**
     * setOverflowToDisk
     *
     * @param overflow
     */
    public void setOverflowToDisk(boolean overflow);

    /**
     * Accessor
     */
    public boolean isDiskPersistent();

    /**
     * setDiskPersistent
     *
     * @param diskPersistent
     */
    public void setDiskPersistent(boolean diskPersistent);

    /**
     * Accessor
     */
    public long getDiskExpiryThreadIntervalSeconds();

    /**
     * setDiskExpiryThreadIntervalSeconds
     *
     * @param seconds
     */
    public void setDiskExpiryThreadIntervalSeconds(long seconds);

    /**
     * Accessor
     */
    public int getDiskSpoolBufferSizeMB();

    /**
     * setDiskSpoolBufferSizeMB
     *
     * @param diskSpoolBufferSizeMB
     */
    public void setDiskSpoolBufferSizeMB(int diskSpoolBufferSizeMB);

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
    public boolean isOverflowToOffHeap();

    /**
     * Accessor
     */
    public long getMaxMemoryOffHeapInBytes();
}
