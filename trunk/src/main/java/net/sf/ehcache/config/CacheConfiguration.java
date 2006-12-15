/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.config;

import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to represent Cache configuration.
 * e.g.
 * <cache name="testCache1"
 * maxElementsInMemory="10000"
 * eternal="false"
 * timeToIdleSeconds="3600"
 * timeToLiveSeconds="10"
 * overflowToDisk="true"
 * diskPersistent="true"
 * diskExpiryThreadIntervalSeconds="120"
 * maxElementsOnDisk="10000"
 * />
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class CacheConfiguration {

    /**
     * the name of the cache.
     */
    protected String name;

    /**
     * the maximum objects to be held in the {@link net.sf.ehcache.store.MemoryStore}.
     */
    protected int maxElementsInMemory;

    /**
     * the maximum objects to be held in the {@link net.sf.ehcache.store.DiskStore}.
     */
    protected int maxElementsOnDisk;

    /**
     * The policy used to evict elements from the {@link net.sf.ehcache.store.MemoryStore}.
     * This can be one of:
     * <ol>
     * <li>LRU - least recently used
     * <li>LFU - Less frequently used
     * <li>FIFO - first in first out, the oldest element by creation time
     * </ol>
     * The default value is LRU
     *
     * @since 1.2
     */
    protected MemoryStoreEvictionPolicy memoryStoreEvictionPolicy;


    /**
     * Sets whether elements are eternal. If eternal,  timeouts are ignored and the element
     * is never expired.
     */
    protected boolean eternal;

    /**
     * the time to idle for an element before it expires. Is only used
     * if the element is not eternal.A value of 0 means do not check for idling.
     */
    protected long timeToIdleSeconds;

    /**
     * Sets the time to idle for an element before it expires. Is only used
     * if the element is not eternal. This attribute is optional in the configuration.
     * A value of 0 means do not check time to live.
     */
    protected long timeToLiveSeconds;

    /**
     * whether elements can overflow to disk when the in-memory cache
     * has reached the set limit.
     */
    protected boolean overflowToDisk;

    /**
     * For caches that overflow to disk, whether the disk cache persists between CacheManager instances.
     */
    protected boolean diskPersistent;

    /**
     * The interval in seconds between runs of the disk expiry thread.
     * <p/>
     * 2 minutes is the default.
     * This is not the same thing as time to live or time to idle. When the thread runs it checks
     * these things. So this value is how often we check for expiry.
     */
    protected long diskExpiryThreadIntervalSeconds;

    /**
     * The event listener factories added by BeanUtils.
     */
    protected final List cacheEventListenerConfigurations = new ArrayList();

    /**
     * The BootstrapCacheLoaderFactoryConfiguration.
     */
    protected BootstrapCacheLoaderFactoryConfiguration bootstrapCacheLoaderFactoryConfiguration;

    /**
     * Sets the name of the cache. This must be unique.
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the maximum objects to be held in memory.
     */
    public final void setMaxElementsInMemory(int maxElementsInMemory) {
        this.maxElementsInMemory = maxElementsInMemory;
    }

    /**
     * Sets the eviction policy. An invalid argument will set it to null.
     * @param memoryStoreEvictionPolicy a String representation of the policy. One of "LRU", "LFU" or "FIFO".
     */
    public final void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
        this.memoryStoreEvictionPolicy = MemoryStoreEvictionPolicy.fromString(memoryStoreEvictionPolicy);
    }

    /**
     * Sets the eviction policy. This method has a strange name to workaround a problem with XML parsing.
     *
     */
    public final void setMemoryStoreEvictionPolicyFromObject(MemoryStoreEvictionPolicy memoryStoreEvictionPolicy) {
        this.memoryStoreEvictionPolicy = memoryStoreEvictionPolicy;
    }

    /**
     * Sets whether elements are eternal. If eternal, timeouts are ignored and the element is never expired.
     */
    public final void setEternal(boolean eternal) {
        this.eternal = eternal;
    }

    /**
     * Sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     */
    public final void setTimeToIdleSeconds(long timeToIdleSeconds) {
        this.timeToIdleSeconds = timeToIdleSeconds;
    }

    /**
     * Sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     */
    public final void setTimeToLiveSeconds(long timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    /**
     * Sets whether elements can overflow to disk when the in-memory cache has reached the set limit.
     */
    public final void setOverflowToDisk(boolean overflowToDisk) {
        this.overflowToDisk = overflowToDisk;
    }

    /**
     * Sets whether, for caches that overflow to disk, the disk cache persist between CacheManager instances.
     */
    public final void setDiskPersistent(boolean diskPersistent) {
        this.diskPersistent = diskPersistent;
    }

    /**
     * Sets the maximum number elements on Disk. 0 means unlimited.
     */
    public void setMaxElementsOnDisk(int maxElementsOnDisk) {
        this.maxElementsOnDisk = maxElementsOnDisk;
    }

    /**
     * Sets the interval in seconds between runs of the disk expiry thread.
     * <p/>
     * 2 minutes is the default.
     * This is not the same thing as time to live or time to idle. When the thread runs it checks
     * these things. So this value is how often we check for expiry.
     */
    public final void setDiskExpiryThreadIntervalSeconds(long diskExpiryThreadIntervalSeconds) {
        this.diskExpiryThreadIntervalSeconds = diskExpiryThreadIntervalSeconds;
    }

    /**
     * Configuration for the CachePeerListenerFactoryConfiguration.
     */
    public final class CacheEventListenerFactoryConfiguration extends FactoryConfiguration {
    }

    /**
     * Used by BeanUtils to add cacheEventListenerFactory elements to the cache configuration.
     */
    public final void addCacheEventListenerFactory(CacheEventListenerFactoryConfiguration factory) {
        cacheEventListenerConfigurations.add(factory);
    }

    /**
     * Configuration for the BootstrapCacheLoaderFactoryConfiguration.
     */
    public final class BootstrapCacheLoaderFactoryConfiguration extends FactoryConfiguration {
    }

    /**
     * Allows {@link BeanHandler} to add the CacheManagerEventListener to the configuration.
     */
    public final void addBootstrapCacheLoaderFactory(BootstrapCacheLoaderFactoryConfiguration
            bootstrapCacheLoaderFactoryConfiguration) {
        this.bootstrapCacheLoaderFactoryConfiguration = bootstrapCacheLoaderFactoryConfiguration;

    }

    /**
     * Accessor
     */
    public String getName() {
        return name;
    }

    /**
     * Accessor
     */
    public int getMaxElementsInMemory() {
        return maxElementsInMemory;
    }

    /**
     * Accessor
     */
    public int getMaxElementsOnDisk() {
        return maxElementsOnDisk;
    }

    /**
     * Accessor
     */
    public MemoryStoreEvictionPolicy getMemoryStoreEvictionPolicy() {
        return memoryStoreEvictionPolicy;
    }

    /**
     * Accessor
     */
    public boolean isEternal() {
        return eternal;
    }

    /**
     * Accessor
     */
    public long getTimeToIdleSeconds() {
        return timeToIdleSeconds;
    }

    /**
     * Accessor
     */
    public long getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    /**
     * Accessor
     */
    public boolean isOverflowToDisk() {
        return overflowToDisk;
    }

    /**
     * Accessor
     */
    public boolean isDiskPersistent() {
        return diskPersistent;
    }

    /**
     * Accessor
     */
    public long getDiskExpiryThreadIntervalSeconds() {
        return diskExpiryThreadIntervalSeconds;
    }

    /**
     * Accessor
     */
    public List getCacheEventListenerConfigurations() {
        return cacheEventListenerConfigurations;
    }

    /**
     * Accessor
     */
    public BootstrapCacheLoaderFactoryConfiguration getBootstrapCacheLoaderFactoryConfiguration() {
        return bootstrapCacheLoaderFactoryConfiguration;
    }

}
