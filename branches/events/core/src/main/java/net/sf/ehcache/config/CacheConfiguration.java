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

package net.sf.ehcache.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.event.NotificationScope;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

/**
 * A value object to represent Cache configuration that can be set by the BeanHandler.
 * e.g.
 * <pre>{@code
 * <cache name="testCache1"
 *   maxElementsInMemory="10000"
 *   eternal="false"
 *   timeToIdleSeconds="3600"
 *   timeToLiveSeconds="10"
 *   overflowToDisk="true"
 *   diskPersistent="true"
 *   diskExpiryThreadIntervalSeconds="120"
 *   maxElementsOnDisk="10000"
 * />
 * }</pre>
 * CacheConfiguration instances retrieved from Cache instances allow the dynamic
 * modification of certain configuration properties.  Currently the dynamic
 * properties are:
 * <ul>
 * <li>Time To Idle</li>
 * <li>Time To Live</li>
 * <li>Max Elements in Memory</li>
 * <li>Max Elements on Disk</li>
 * </ul>
 * Dynamic changes are however not persistent across cache restarts.  On restart
 * the cache configuration will be reloaded from its original source, erasing any
 * changes made previously at runtime.
 * 
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @author <a href="mailto:cdennis@terracottatech.com>Chris Dennis</a>
 * @version $Id$
 */
public class CacheConfiguration implements Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(CacheConfiguration.class.getName());

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
     * Sets whether the MemoryStore should be cleared when
     * {@link net.sf.ehcache.Ehcache#flush flush()} is called on the cache - true by default.
     */
    protected boolean clearOnFlush = true;


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
     * The size of the disk spool used to buffer writes
     */
    protected int diskSpoolBufferSizeMB;

    /**
     * The interval in seconds between runs of the disk expiry thread.
     * <p/>
     * 2 minutes is the default.
     * This is not the same thing as time to live or time to idle. When the thread runs it checks
     * these things. So this value is how often we check for expiry.
     */
    protected long diskExpiryThreadIntervalSeconds;
    
    /**
     * Indicates whether logging is enabled or not. False by default
     */
    protected boolean loggingEnabled;

    /**
     * The event listener factories added by BeanUtils.
     */
    protected final List<CacheEventListenerFactoryConfiguration> cacheEventListenerConfigurations =
        new ArrayList<CacheEventListenerFactoryConfiguration>();

    /**
     * The cache extension factories added by BeanUtils.
     */
    protected final List<CacheExtensionFactoryConfiguration> cacheExtensionConfigurations =
        new ArrayList<CacheExtensionFactoryConfiguration>();

    /**
     * The BootstrapCacheLoaderFactoryConfiguration.
     */
    protected BootstrapCacheLoaderFactoryConfiguration bootstrapCacheLoaderFactoryConfiguration;

    /**
     * The CacheExceptionHandlerFactoryConfiguration.
     */
    protected CacheExceptionHandlerFactoryConfiguration cacheExceptionHandlerFactoryConfiguration;

    /**
     * The TerracottaConfiguration.
     */
    protected TerracottaConfiguration terracottaConfiguration;
    
    /**
     * The cache loader factories added by BeanUtils.
     */
    //protected CacheLoaderFactoryConfiguration cacheLoaderFactoryConfiguration;
    protected List cacheLoaderConfigurations = new ArrayList();

    /**
     * The listeners for this configuration.
     */
    private volatile Set<CacheConfigurationListener> listeners = new CopyOnWriteArraySet<CacheConfigurationListener>();

    private volatile boolean frozen;

    /**
     * Clones this object, following the usual contract.
     *
     * @return a copy, which independent other than configurations than cannot change.
     * @throws CloneNotSupportedException
     */
    @Override
    public CacheConfiguration clone() throws CloneNotSupportedException {
        CacheConfiguration config = (CacheConfiguration) super.clone();
        config.listeners = new CopyOnWriteArraySet<CacheConfigurationListener>();
        return config;
    }

    /**
     * Sets the name of the cache. This must be unique.
     * The / character is illegal. The # character does not work
     * with RMI replication.
     * @param name the cache name
     */
    public final void setName(String name) {
        checkDynamicChange();
        if (name == null) {
            throw new IllegalArgumentException("Cache name cannot be null.");
        }
        this.name = name;
    }
    
    /**
     * Enables or disables logging for the cache
     * <p>
     * This property can be modified dynamically while the cache is operating.
     * 
     * @param enable if true, enables logging otherwise disables logging
     */
    public final void setLoggingEnabled(boolean enable) {
        checkDynamicChange();
        boolean oldLoggingEnabled = this.loggingEnabled;
        this.loggingEnabled = enable;
        fireLoggingEnabledChanged(oldLoggingEnabled, enable);
    }

    /**
     * Sets the maximum objects to be held in memory.
     * <p>
     * This property can be modified dynamically while the cache is operating.
     * 
     * @param maxElementsInMemory param
     */
    public final void setMaxElementsInMemory(int maxElementsInMemory) {
        checkDynamicChange();
        int oldCapacity = this.maxElementsInMemory;
        int newCapacity = maxElementsInMemory;
        this.maxElementsInMemory = maxElementsInMemory;
        fireMemoryCapacityChanged(oldCapacity, newCapacity);
    }

    /**
     * Sets the eviction policy. An invalid argument will set it to null.
     *
     * @param memoryStoreEvictionPolicy a String representation of the policy. One of "LRU", "LFU" or "FIFO".
     */
    public final void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
        checkDynamicChange();
        this.memoryStoreEvictionPolicy = MemoryStoreEvictionPolicy.fromString(memoryStoreEvictionPolicy);
    }

    /**
     * Sets the eviction policy. This method has a strange name to workaround a problem with XML parsing.
     */
    public final void setMemoryStoreEvictionPolicyFromObject(MemoryStoreEvictionPolicy memoryStoreEvictionPolicy) {
        checkDynamicChange();
        this.memoryStoreEvictionPolicy = memoryStoreEvictionPolicy;
    }
    
    /**
     * Sets whether the MemoryStore should be cleared when
     * {@link net.sf.ehcache.Ehcache#flush flush()} is called on the cache - true by default.
     */
    public final void setClearOnFlush(boolean clearOnFlush) {
        checkDynamicChange();
        this.clearOnFlush = clearOnFlush;
    }

    /**
     * Sets whether elements are eternal. If eternal, timeouts are ignored and the element is never expired.
     */
    public final void setEternal(boolean eternal) {
        checkDynamicChange();
        this.eternal = eternal;
        if (eternal) {
            setTimeToIdleSeconds(0);
            setTimeToLiveSeconds(0);
        }
//        else {
//            // XXX: Should this set the TTI/TTL to some default?
//        }
    }

    /**
     * Sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     * <p>
     * This property can be modified dynamically while the cache is operating.
     */
    public final void setTimeToIdleSeconds(long timeToIdleSeconds) {
        checkDynamicChange();
        long oldTti = this.timeToIdleSeconds;
        long newTti = timeToIdleSeconds;
        this.timeToIdleSeconds = timeToIdleSeconds;
        fireTtiChanged(oldTti, newTti);
    }

    /**
     * Sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     * <p>
     * This property can be modified dynamically while the cache is operating.
     */
    public final void setTimeToLiveSeconds(long timeToLiveSeconds) {
        checkDynamicChange();
        long oldTtl = this.timeToLiveSeconds;
        long newTtl = timeToLiveSeconds;
        this.timeToLiveSeconds = timeToLiveSeconds;
        fireTtlChanged(oldTtl, newTtl);
    }

    /**
     * Sets whether elements can overflow to disk when the in-memory cache has reached the set limit.
     */
    public final void setOverflowToDisk(boolean overflowToDisk) {
        checkDynamicChange();
        this.overflowToDisk = overflowToDisk;
        validateConfiguration();
    }

    /**
     * Sets whether, for caches that overflow to disk, the disk cache persist between CacheManager instances.
     */
    public final void setDiskPersistent(boolean diskPersistent) {
        checkDynamicChange();
        this.diskPersistent = diskPersistent;
        validateConfiguration();
    }

    /**
     * Getter
     */
    public int getDiskSpoolBufferSizeMB() {
        checkDynamicChange();
        return diskSpoolBufferSizeMB;
    }

    /**
     * Sets the disk spool size
     *
     * @param diskSpoolBufferSizeMB a postive number
     */
    public void setDiskSpoolBufferSizeMB(int diskSpoolBufferSizeMB) {
        checkDynamicChange();
        this.diskSpoolBufferSizeMB = diskSpoolBufferSizeMB;
    }

    /**
     * Sets the maximum number elements on Disk. 0 means unlimited.
     * <p>
     * This property can be modified dynamically while the cache is operating.
     */
    public void setMaxElementsOnDisk(int maxElementsOnDisk) {
        checkDynamicChange();
        int oldCapacity = this.maxElementsOnDisk;
        int newCapacity = maxElementsOnDisk;
        this.maxElementsOnDisk = maxElementsOnDisk;
        fireDiskCapacityChanged(oldCapacity, newCapacity);
    }

    /**
     * Sets the interval in seconds between runs of the disk expiry thread.
     * <p/>
     * 2 minutes is the default.
     * This is not the same thing as time to live or time to idle. When the thread runs it checks
     * these things. So this value is how often we check for expiry.
     */
    public final void setDiskExpiryThreadIntervalSeconds(long diskExpiryThreadIntervalSeconds) {
        checkDynamicChange();
        this.diskExpiryThreadIntervalSeconds = diskExpiryThreadIntervalSeconds;
    }

    /**
     * Freeze this configuration.  Any subsequent changes will throw a CacheException
     */
    public void freezeConfiguration() {
        frozen = true;
    }

    /**
     * Configuration for the CachePeerListenerFactoryConfiguration.
     */
    public static final class CacheEventListenerFactoryConfiguration extends FactoryConfiguration {
        private NotificationScope notificationScope = NotificationScope.ALL;
        
        /**
         * Used by BeanHandler to set the mode during parsing.  Convert listenFor string to uppercase and 
         * look up enum constant in NotificationScope.
         */
        public void setListenFor(String listenFor) {
            if (listenFor == null) {
                throw new IllegalArgumentException("listenFor must be non-null");
            }
            this.notificationScope = NotificationScope.valueOf(NotificationScope.class, listenFor.toUpperCase());
        }

        /**
         * Get the value mode in terms of the mode enum
         */
        public NotificationScope getListenFor() {
            return this.notificationScope;
        }
    }

    /**
     * Used by BeanUtils to add cacheEventListenerFactory elements to the cache configuration.
     */
    public final void addCacheEventListenerFactory(CacheEventListenerFactoryConfiguration factory) {
        checkDynamicChange();
        cacheEventListenerConfigurations.add(factory);
        validateConfiguration();
    }

    /**
     * Configuration for the CacheExtensionFactoryConfiguration.
     */
    public static final class CacheExtensionFactoryConfiguration extends FactoryConfiguration {
    }

    /**
     * Used by BeanUtils to add cacheExtensionFactory elements to the cache configuration.
     */
    public final void addCacheExtensionFactory(CacheExtensionFactoryConfiguration factory) {
        checkDynamicChange();
        cacheExtensionConfigurations.add(factory);
    }

    /**
     * Configuration for the BootstrapCacheLoaderFactoryConfiguration.
     */
    public static final class BootstrapCacheLoaderFactoryConfiguration extends FactoryConfiguration {
    }

    /**
     * Allows BeanHandler to add the CacheManagerEventListener to the configuration.
     */
    public final void addBootstrapCacheLoaderFactory(BootstrapCacheLoaderFactoryConfiguration
            bootstrapCacheLoaderFactoryConfiguration) {
        checkDynamicChange();
        this.bootstrapCacheLoaderFactoryConfiguration = bootstrapCacheLoaderFactoryConfiguration;
    }

    /**
     * Configuration for the BootstrapCacheLoaderFactoryConfiguration.
     */
    public static final class CacheExceptionHandlerFactoryConfiguration extends FactoryConfiguration {
    }


    /**
     * Allows BeanHandler to add the CacheExceptionHandlerFactory to the configuration.
     */
    public final void addCacheExceptionHandlerFactory(CacheExceptionHandlerFactoryConfiguration
            cacheExceptionHandlerFactoryConfiguration) {
        checkDynamicChange();
        this.cacheExceptionHandlerFactoryConfiguration = cacheExceptionHandlerFactoryConfiguration;
    }

    /**
     * Configuration for the CacheLoaderFactoryConfiguration.
     */
    public static final class CacheLoaderFactoryConfiguration extends FactoryConfiguration {
    }

    /**
     * Used by BeanUtils to add each cacheLoaderFactory to the cache configuration.
     * @param factory
     */
    public final void addCacheLoaderFactory(CacheLoaderFactoryConfiguration factory) {
        checkDynamicChange();
        cacheLoaderConfigurations.add(factory);
    }

    /**
     * Allows BeanHandler to add the TerracottaConfiguration to the configuration.
     * @param terracottaConfiguration
     */
    public final void addTerracotta(TerracottaConfiguration terracottaConfiguration) {
        this.terracottaConfiguration = terracottaConfiguration;
        validateConfiguration();
    }

    private void validateConfiguration() {
        if (terracottaConfiguration != null && terracottaConfiguration.isClustered()) {
            if (overflowToDisk) {
                throw new InvalidConfigurationException("overflowToDisk isn't supported for a clustered Terracotta cache");
            }
            if (diskPersistent) {
                throw new InvalidConfigurationException("diskPersistent isn't supported for a clustered Terracotta cache");
            }
            if (cacheEventListenerConfigurations != null) {
                for (CacheEventListenerFactoryConfiguration listenerConfig : cacheEventListenerConfigurations) {
                    if (null == listenerConfig.getFullyQualifiedClassPath()) {
                        continue;
                    }
                    if (listenerConfig.getFullyQualifiedClassPath().startsWith("net.sf.ehcache.distribution.")) {
                        throw new InvalidConfigurationException("cache replication isn't supported" + 
                                " for a clustered Terracotta cache");                        
                    } else if (listenerConfig.getFullyQualifiedClassPath().startsWith("net.sf.ehcache.") &&
                        LOG.isWarnEnabled()) {
                        LOG.warn("A non-standard CacheEventListenerFactory is used with a clustered Terracotta cache, " +
                                "if the purpose of this listener is replication it is not supported in a clustered context");
                    }
                }
            }
        }
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
    public boolean isClearOnFlush() {
      return clearOnFlush;
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
     * @return true if logging is enabled otherwise false
     */
    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * Accessor
     */
    public List getCacheEventListenerConfigurations() {
        return cacheEventListenerConfigurations;
    }

    /**
     * Accessor
     * @return the configuration
     */
    public List getCacheExtensionConfigurations() {
        return cacheExtensionConfigurations;
    }


    /**
     * Accessor
     * @return the configuration
     */
    public List getCacheLoaderConfigurations() {
        return cacheLoaderConfigurations;
    }

    /**
     * Accessor
     * @return the configuration
     */
    public BootstrapCacheLoaderFactoryConfiguration getBootstrapCacheLoaderFactoryConfiguration() {
        return bootstrapCacheLoaderFactoryConfiguration;
    }

    /**
     * Accessor
     * @return the configuration
     */
    public CacheExceptionHandlerFactoryConfiguration getCacheExceptionHandlerFactoryConfiguration() {
        return cacheExceptionHandlerFactoryConfiguration;
    }
    
    /**
     * Accessor
     * @return the terracotta configuration
     */
    public TerracottaConfiguration getTerracottaConfiguration() {
        return terracottaConfiguration;
    }

    /**
     * Helper method to compute whether the cache is clustered or not
     * @return True if the <terracotta/> element exists with clustered="true"
     */
    public boolean isTerracottaClustered() {
        return terracottaConfiguration != null && terracottaConfiguration.isClustered();
    }

    /**
     * Add a listener to this cache configuration
     *
     * @param listener listener instance to add
     * @return true if a listener was added
     */
    public boolean addListener(CacheConfigurationListener listener) {
        boolean added = listeners.add(listener);
        if (added) {
            listener.registered(this);
        }
        return added;
    }

    /**
     * Remove the supplied cache configuration listener.
     * 
     * @param listener listener to remove
     * @return true if a listener was removed
     */
    public boolean removeListener(CacheConfigurationListener listener) {
        boolean removed = listeners.remove(listener);
        if (removed) {
            listener.deregistered(this);
        }
        return removed;
    }

    private void fireTtiChanged(long oldTti, long newTti) {
        if (oldTti != newTti) {
            for (CacheConfigurationListener l : listeners) {
                l.timeToIdleChanged(oldTti, newTti);
            }
        }
    }

    private void fireTtlChanged(long oldTtl, long newTtl) {
        if (oldTtl != newTtl) {
            for (CacheConfigurationListener l : listeners) {
                l.timeToLiveChanged(oldTtl, newTtl);
            }
        }
    }
    
    private void fireLoggingEnabledChanged(boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            for (CacheConfigurationListener l : listeners) {
                l.loggingEnabledChanged(oldValue, newValue);
            }
        }
    }

    private void fireDiskCapacityChanged(int oldCapacity, int newCapacity) {
        if (oldCapacity != newCapacity) {
            for (CacheConfigurationListener l : listeners) {
                l.diskCapacityChanged(oldCapacity, newCapacity);
            }
        }
    }

    private void fireMemoryCapacityChanged(int oldCapacity, int newCapacity) {
        if (oldCapacity != newCapacity) {
            for (CacheConfigurationListener l : listeners) {
                l.memoryCapacityChanged(oldCapacity, newCapacity);
            }
        }
    }

    private void checkDynamicChange() {
        if (frozen) {
            throw new CacheException("Dynamic configuration changes are disabled for this cache");
        }
    }
    
    /**
     * internal use only
     */
    public void internalSetTimeToIdle(long timeToIdle) {
        this.timeToIdleSeconds = timeToIdle;
    }

    /**
     * internal use only
     */
    public void internalSetTimeToLive(long timeToLive) {
        this.timeToLiveSeconds = timeToLive;
    }

    /**
     * internal use only
     */
    public void internalSetMemCapacity(int capacity) {
        this.maxElementsInMemory = capacity;
    }

    /**
     * internal use only
     */
    public void internalSetDiskCapacity(int capacity) {
        this.maxElementsOnDisk = capacity;
    }
}
