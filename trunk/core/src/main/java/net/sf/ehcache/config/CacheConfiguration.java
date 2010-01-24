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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

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

    /**
     * The default interval between runs of the expiry thread.
     */
    public static final long DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS = 120;

    /**
     * Set a buffer size for the spool of approx 30MB.
     */
    public static final int DEFAULT_SPOOL_BUFFER_SIZE = 30;

    /**
     * The default memory store eviction policy is LRU.
     */
    public static final MemoryStoreEvictionPolicy DEFAULT_MEMORY_STORE_EVICTION_POLICY = MemoryStoreEvictionPolicy.LRU;


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
    protected MemoryStoreEvictionPolicy memoryStoreEvictionPolicy = DEFAULT_MEMORY_STORE_EVICTION_POLICY;

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
     * The path where the disk store is located
     */
    protected String diskStorePath = DiskStoreConfiguration.getDefaultPath();

    /**
     * The size of the disk spool used to buffer writes
     */
    protected int diskSpoolBufferSizeMB = DEFAULT_SPOOL_BUFFER_SIZE;

    /**
     * The interval in seconds between runs of the disk expiry thread.
     * <p/>
     * 2 minutes is the default.
     * This is not the same thing as time to live or time to idle. When the thread runs it checks
     * these things. So this value is how often we check for expiry.
     */
    protected long diskExpiryThreadIntervalSeconds = DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS;

    /**
     * Indicates whether logging is enabled or not. False by default
     */
    protected boolean loggingEnabled;

    /**
     * The event listener factories added by BeanUtils.
     */
    protected volatile List<CacheEventListenerFactoryConfiguration> cacheEventListenerConfigurations =
            new ArrayList<CacheEventListenerFactoryConfiguration>();

    /**
     * The cache extension factories added by BeanUtils.
     */
    protected volatile List<CacheExtensionFactoryConfiguration> cacheExtensionConfigurations =
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
     * The CacheWriterConfiguration.
     */
    protected CacheWriterConfiguration cacheWriterConfiguration = new CacheWriterConfiguration();

    /**
     * The cache loader factories added by BeanUtils.
     */
    protected volatile List<CacheLoaderFactoryConfiguration> cacheLoaderConfigurations = new ArrayList<CacheLoaderFactoryConfiguration>();

    /**
     * The listeners for this configuration.
     */
    protected volatile Set<CacheConfigurationListener> listeners = new CopyOnWriteArraySet<CacheConfigurationListener>();

    private volatile boolean frozen;
    private TransactionalMode transactionalMode = TransactionalMode.OFF;

    /**
     * Default constructor that can only be used by classes in this package.
     */
    CacheConfiguration() {
        // default constructor is only accessible in this package
    }

    /**
     * Create a new cache configuration.
     * 
     * @param name                the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxElementsInMemory the maximum number of elements in memory, before they are evicted
     */
    public CacheConfiguration(String name, int maxElementsInMemory) {
        this.name = name;
        this.maxElementsInMemory = maxElementsInMemory;
    }

    /**
     * Clones this object, following the usual contract.
     *
     * @return a copy, which independent other than configurations than cannot change.
     */
    @Override
    public CacheConfiguration clone() {
        CacheConfiguration config;
        try {
            config = (CacheConfiguration) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        cloneCacheEventListenerConfigurations(config);

        cloneCacheExtensionConfigurations(config);

        if (bootstrapCacheLoaderFactoryConfiguration != null) {
            config.bootstrapCacheLoaderFactoryConfiguration = bootstrapCacheLoaderFactoryConfiguration.clone();
        }

        if (cacheExceptionHandlerFactoryConfiguration != null) {
            config.cacheExceptionHandlerFactoryConfiguration = cacheExceptionHandlerFactoryConfiguration.clone();
        }

        if (terracottaConfiguration != null) {
            config.terracottaConfiguration = terracottaConfiguration.clone();
        }

        if (cacheWriterConfiguration != null) {
            config.cacheWriterConfiguration = cacheWriterConfiguration.clone();
        }

        cloneCacheLoaderConfigurations(config);

        config.listeners = new CopyOnWriteArraySet<CacheConfigurationListener>();

        return config;
    }

    private void cloneCacheEventListenerConfigurations(CacheConfiguration config) {
        if (cacheEventListenerConfigurations.size() > 0) {
            List<CacheEventListenerFactoryConfiguration> copy = new ArrayList<CacheEventListenerFactoryConfiguration>();
            for (CacheEventListenerFactoryConfiguration item : cacheEventListenerConfigurations) {
                copy.add(item.clone());
            }
            config.cacheEventListenerConfigurations = copy;
        }
    }

    private void cloneCacheExtensionConfigurations(CacheConfiguration config) {
        if (cacheExtensionConfigurations.size() > 0) {
            List<CacheExtensionFactoryConfiguration> copy = new ArrayList<CacheExtensionFactoryConfiguration>();
            for (CacheConfiguration.CacheExtensionFactoryConfiguration item : cacheExtensionConfigurations) {
                copy.add(item.clone());
            }
            config.cacheExtensionConfigurations = copy;
        }
    }

    private void cloneCacheLoaderConfigurations(CacheConfiguration config) {
        if (cacheLoaderConfigurations.size() > 0) {
            List<CacheLoaderFactoryConfiguration> copy = new ArrayList<CacheLoaderFactoryConfiguration>();
            for (CacheConfiguration.CacheLoaderFactoryConfiguration item : cacheLoaderConfigurations) {
                copy.add(item.clone());
            }
            config.cacheLoaderConfigurations = copy;
        }
    }

    /**
     * Sets the name of the cache. This must be unique.
     * The / character is illegal. The # character does not work
     * with RMI replication.
     *
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
     * @return this configuration instance
     * @see #setName(String)
     */
    public final CacheConfiguration name(String name) {
        setName(name);
        return this;
    }

    /**
     * Enables or disables logging for the cache
     * <p/>
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
     * @return this configuration instance
     * @see #setLoggingEnabled(boolean)
     */
    public final CacheConfiguration loggingEnabled(boolean enable) {
        setLoggingEnabled(enable);
        return this;
    }

    /**
     * Sets the maximum objects to be held in memory.
     * <p/>
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
     * @return this configuration instance
     * @see #setMaxElementsInMemory(int)
     */
    public final CacheConfiguration maxElementsInMemory(int maxElementsInMemory) {
        setMaxElementsInMemory(maxElementsInMemory);
        return this;
    }

    /**
     * Sets the eviction policy. An invalid argument will set it to null.
     *
     * @param memoryStoreEvictionPolicy a String representation of the policy. One of "LRU", "LFU" or "FIFO".
     */
    public final void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
        setMemoryStoreEvictionPolicyFromObject(MemoryStoreEvictionPolicy.fromString(memoryStoreEvictionPolicy));
    }

    /**
     * @return this configuration instance
     * @see #setMemoryStoreEvictionPolicy(String)
     */
    public final CacheConfiguration memoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
        setMemoryStoreEvictionPolicy(memoryStoreEvictionPolicy);
        return this;
    }

    /**
     * Sets the eviction policy. This method has a strange name to workaround a problem with XML parsing.
     */
    public final void setMemoryStoreEvictionPolicyFromObject(MemoryStoreEvictionPolicy memoryStoreEvictionPolicy) {
        checkDynamicChange();
        if (null == memoryStoreEvictionPolicy) {
            this.memoryStoreEvictionPolicy = DEFAULT_MEMORY_STORE_EVICTION_POLICY;
        } else {
            this.memoryStoreEvictionPolicy = memoryStoreEvictionPolicy;
        }
    }

    /**
     * @return this configuration instance
     * @see #setMemoryStoreEvictionPolicyFromObject(MemoryStoreEvictionPolicy)
     */
    public final CacheConfiguration memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy memoryStoreEvictionPolicy) {
        setMemoryStoreEvictionPolicyFromObject(memoryStoreEvictionPolicy);
        return this;
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
     * @return this configuration instance
     * @see #setClearOnFlush(boolean)
     */
    public final CacheConfiguration clearOnFlush(boolean clearOnFlush) {
        setClearOnFlush(clearOnFlush);
        return this;
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
    }

    /**
     * @return this configuration instance
     * @see #setEternal(boolean)
     */
    public final CacheConfiguration eternal(boolean eternal) {
        setEternal(eternal);
        return this;
    }

    /**
     * Sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     * <p/>
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
     * @return this configuration instance
     * @see #setTimeToIdleSeconds(long)
     */
    public final CacheConfiguration timeToIdleSeconds(long timeToIdleSeconds) {
        setTimeToIdleSeconds(timeToIdleSeconds);
        return this;
    }

    /**
     * Sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     * <p/>
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
     * @return this configuration instance
     * @see #setTimeToLiveSeconds(long)
     */
    public final CacheConfiguration timeToLiveSeconds(long timeToLiveSeconds) {
        setTimeToLiveSeconds(timeToLiveSeconds);
        return this;
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
     * @return this configuration instance
     * @see #setOverflowToDisk(boolean)
     */
    public final CacheConfiguration overflowToDisk(boolean overflowToDisk) {
        setOverflowToDisk(overflowToDisk);
        return this;
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
     * @return this configuration instance
     * @see #setDiskPersistent(boolean)
     */
    public final CacheConfiguration diskPersistent(boolean diskPersistent) {
        setDiskPersistent(diskPersistent);
        return this;
    }

    /**
     * Sets the path that will be used for the disk store.
     */
    public final void setDiskStorePath(String diskStorePath) {
        checkDynamicChange();
        if (null == diskStorePath) {
            this.diskStorePath = DiskStoreConfiguration.getDefaultPath();
        }
        this.diskStorePath = diskStorePath;
        validateConfiguration();
    }

    /**
     * @return this configuration instance
     * @see #setDiskStorePath(String)
     */
    public final CacheConfiguration diskStorePath(String diskStorePath) {
        setDiskStorePath(diskStorePath);
        return this;
    }

    /**
     * Sets the disk spool size
     *
     * @param diskSpoolBufferSizeMB a positive number
     */
    public void setDiskSpoolBufferSizeMB(int diskSpoolBufferSizeMB) {
        checkDynamicChange();
        if (diskSpoolBufferSizeMB <= 0) {
            this.diskSpoolBufferSizeMB = DEFAULT_SPOOL_BUFFER_SIZE;
        } else {
            this.diskSpoolBufferSizeMB = diskSpoolBufferSizeMB;
        }
    }

    /**
     * @return this configuration instance
     * @see #setDiskSpoolBufferSizeMB(int)
     */
    public final CacheConfiguration diskSpoolBufferSizeMB(int diskSpoolBufferSizeMB) {
        setDiskSpoolBufferSizeMB(diskSpoolBufferSizeMB);
        return this;
    }

    /**
     * Sets the maximum number elements on Disk. 0 means unlimited.
     * <p/>
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
     * @return this configuration instance
     * @see #setMaxElementsOnDisk(int)
     */
    public final CacheConfiguration maxElementsOnDisk(int maxElementsOnDisk) {
        setMaxElementsOnDisk(maxElementsOnDisk);
        return this;
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
        if (diskExpiryThreadIntervalSeconds <= 0) {
            this.diskExpiryThreadIntervalSeconds = DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS;
        } else {
            this.diskExpiryThreadIntervalSeconds = diskExpiryThreadIntervalSeconds;
        }
    }

    /**
     * @return this configuration instance
     * @see #setDiskExpiryThreadIntervalSeconds(long)
     */
    public final CacheConfiguration diskExpiryThreadIntervalSeconds(long diskExpiryThreadIntervalSeconds) {
        setDiskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds);
        return this;
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
    public static final class CacheEventListenerFactoryConfiguration extends FactoryConfiguration<CacheEventListenerFactoryConfiguration> {
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
     * @return this configuration instance
     * @see #addCacheEventListenerFactory(CacheEventListenerFactoryConfiguration)
     */
    public final CacheConfiguration cacheEventListenerFactory(CacheEventListenerFactoryConfiguration factory) {
        addCacheEventListenerFactory(factory);
        return this;
    }

    /**
     * Configuration for the CacheExtensionFactoryConfiguration.
     */
    public static final class CacheExtensionFactoryConfiguration extends FactoryConfiguration<CacheExtensionFactoryConfiguration> {
    }

    /**
     * Used by BeanUtils to add cacheExtensionFactory elements to the cache configuration.
     */
    public final void addCacheExtensionFactory(CacheExtensionFactoryConfiguration factory) {
        checkDynamicChange();
        cacheExtensionConfigurations.add(factory);
    }

    /**
     * @return this configuration instance
     * @see #addCacheExtensionFactory(CacheExtensionFactoryConfiguration)
     */
    public final CacheConfiguration cacheExtensionFactory(CacheExtensionFactoryConfiguration factory) {
        /**
         * {@inheritDoc}
         */
        addCacheExtensionFactory(factory);
        return this;
    }

    /**
     * Configuration for the BootstrapCacheLoaderFactoryConfiguration.
     */
    public static final class BootstrapCacheLoaderFactoryConfiguration extends
            FactoryConfiguration<BootstrapCacheLoaderFactoryConfiguration> {
    }

    /**
     * Allows BeanHandler to add the CacheManagerEventListener to the configuration.
     */
    public final void addBootstrapCacheLoaderFactory(BootstrapCacheLoaderFactoryConfiguration factory) {
        checkDynamicChange();
        this.bootstrapCacheLoaderFactoryConfiguration = factory;
    }

    /**
     * @return this configuration instance
     * @see #addBootstrapCacheLoaderFactory(BootstrapCacheLoaderFactoryConfiguration)
     */
    public final CacheConfiguration bootstrapCacheLoaderFactory(BootstrapCacheLoaderFactoryConfiguration factory) {
        addBootstrapCacheLoaderFactory(factory);
        return this;
    }

    /**
     * Configuration for the BootstrapCacheLoaderFactoryConfiguration.
     */
    public static final class CacheExceptionHandlerFactoryConfiguration extends
            FactoryConfiguration<CacheExceptionHandlerFactoryConfiguration> {
    }

    /**
     * Add the CacheExceptionHandlerFactory to the configuration.
     * <p>
     * Note that this will not have any effect when creating a cache solely through its constructed. The exception
     * handler will only be taken into account when {@link ConfigurationHelper} is used, for example through
     * {@link net.sf.ehcache.CacheManager}.
     */
    public final void addCacheExceptionHandlerFactory(CacheExceptionHandlerFactoryConfiguration factory) {
        checkDynamicChange();
        this.cacheExceptionHandlerFactoryConfiguration = factory;
    }

    /**
     * @return this configuration instance
     * @see #addCacheExceptionHandlerFactory(CacheExceptionHandlerFactoryConfiguration)
     */
    public final CacheConfiguration cacheExceptionHandlerFactory(CacheExceptionHandlerFactoryConfiguration factory) {
        addCacheExceptionHandlerFactory(factory);
        return this;
    }

    /**
     * Configuration for the CacheLoaderFactoryConfiguration.
     */
    public static final class CacheLoaderFactoryConfiguration extends FactoryConfiguration<CacheLoaderFactoryConfiguration> {
    }

    /**
     * Used by BeanUtils to add each cacheLoaderFactory to the cache configuration.
     *
     * @param factory
     */
    public final void addCacheLoaderFactory(CacheLoaderFactoryConfiguration factory) {
        checkDynamicChange();
        cacheLoaderConfigurations.add(factory);
    }

    /**
     * @return this configuration instance
     * @see #addCacheLoaderFactory(CacheLoaderFactoryConfiguration)
     */
    public final CacheConfiguration cacheLoaderFactory(CacheLoaderFactoryConfiguration factory) {
        addCacheLoaderFactory(factory);
        return this;
    }

    /**
     * Allows BeanHandler to add the TerracottaConfiguration to the configuration.
     */
    public final void addTerracotta(TerracottaConfiguration terracottaConfiguration) {
        this.terracottaConfiguration = terracottaConfiguration;
        validateConfiguration();
    }

    /**
     * @return this configuration instance
     * @see #addTerracotta(TerracottaConfiguration)
     */
    public final CacheConfiguration terracotta(TerracottaConfiguration terracottaConfiguration) {
        addTerracotta(terracottaConfiguration);
        return this;
    }

    /**
     * Allows BeanHandler to add the CacheWriterConfiguration to the configuration.
     */
    public final void addCacheWriter(CacheWriterConfiguration cacheWriterConfiguration) {
        if (null == cacheWriterConfiguration) {
            this.cacheWriterConfiguration = new CacheWriterConfiguration();
        } else {
            this.cacheWriterConfiguration = cacheWriterConfiguration;
        }
    }

    /**
     * @return this configuration instance
     * @see #addCacheWriter(CacheWriterConfiguration)
     */
    public final CacheConfiguration cacheWriter(CacheWriterConfiguration cacheWriterConfiguration) {
        addCacheWriter(cacheWriterConfiguration);
        return this;
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
    public String getDiskStorePath() {
        return diskStorePath;
    }

    /**
     * Accessor
     */
    public int getDiskSpoolBufferSizeMB() {
        return diskSpoolBufferSizeMB;
    }

    /**
     * Accessor
     */
    public long getDiskExpiryThreadIntervalSeconds() {
        return diskExpiryThreadIntervalSeconds;
    }

    /**
     * Accessor
     *
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
     *
     * @return the configuration
     */
    public List getCacheExtensionConfigurations() {
        return cacheExtensionConfigurations;
    }


    /**
     * Accessor
     *
     * @return the configuration
     */
    public List getCacheLoaderConfigurations() {
        return cacheLoaderConfigurations;
    }

    /**
     * Accessor
     *
     * @return the configuration
     */
    public BootstrapCacheLoaderFactoryConfiguration getBootstrapCacheLoaderFactoryConfiguration() {
        return bootstrapCacheLoaderFactoryConfiguration;
    }

    /**
     * Accessor
     *
     * @return the configuration
     */
    public CacheExceptionHandlerFactoryConfiguration getCacheExceptionHandlerFactoryConfiguration() {
        return cacheExceptionHandlerFactoryConfiguration;
    }

    /**
     * Accessor
     *
     * @return the terracotta configuration
     */
    public TerracottaConfiguration getTerracottaConfiguration() {
        return terracottaConfiguration;
    }

    /**
     * Accessor
     *
     * @return the writer configuration
     */
    public CacheWriterConfiguration getCacheWriterConfiguration() {
        return cacheWriterConfiguration;
    }

    /**
     * Helper method to compute whether the cache is clustered or not
     *
     * @return True if the &lt;terracotta/&gt; element exists with {@code clustered="true"}
     */
    public boolean isTerracottaClustered() {
        return terracottaConfiguration != null && terracottaConfiguration.isClustered();
    }

    /**
     * To what transactionalMode was the Cache set
     * @return transactionaMode
     */
    public final TransactionalMode getTransactionalMode() {
        return transactionalMode;
    }

    /**
     * Sets the transactionalMode
     * @param transactionalMode OFF or XA
     */
    public final void setTransactionalMode(final String transactionalMode) {
        if (transactionalMode == null) {
            throw new IllegalArgumentException("TransactionMode value must be non-null");
        }
        this.transactionalMode = TransactionalMode.valueOf(transactionalMode.toUpperCase());
    }

    /**
     * Helper method to compute whether the cache is transactional or not
     * @return True if transactionalMode="xa"
     */
    public boolean isTransactional() {
        return transactionalMode.isTransactional();
    }


    /**
      * Represents whether the Cache is transactional or not.
      * @author alexsnaps
      */
    public static enum TransactionalMode {

        /** No Transactions */
        OFF(false),

        /** XA Transactions */
        XA(true);

        private final boolean transactional;

        /**
         *
         * @param transactional
         */
        TransactionalMode(final boolean transactional) {
            this.transactional = transactional;
        }

        /**
         *
         * @return transactional
         */
        public boolean isTransactional() {
            return transactional;
        }
    }

    /**
     * Add a listener to this cache configuration
     *
     * @param listener listener instance to add
     * @return true if a listener was added
     */
    public boolean addConfigurationListener(CacheConfigurationListener listener) {
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
    public boolean removeConfigurationListener(CacheConfigurationListener listener) {
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
