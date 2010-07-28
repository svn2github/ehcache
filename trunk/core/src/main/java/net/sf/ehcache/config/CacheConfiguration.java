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

package net.sf.ehcache.config;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.event.NotificationScope;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.compound.CopyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A value object used to represent cache configuration.
 * <h4>Construction Patterns</h4>
 * The recommended way of creating a <code>Cache</code> in Ehcache 2.0 and above is to create a <code>CacheConfiguration</code> object
 * and pass it to the <code>Cache</code> constructor. See {@link net.sf.ehcache.Cache#Cache(CacheConfiguration)}.
 * <p>
 * This class supports setter injection and also the fluent builder pattern.
 * e.g.
 * <code>Cache cache = new Cache(new CacheConfiguration("test2", 1000).eternal(true).memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.FIFO));</code>
 * <p/>
 * Rather than proliferation of new constructors as new versions of Ehcache come out, it intended to add the new configuration to this
 * class.
 * <p/>
 * Another way to set configuration is declaratively in the <code>ehcache.xml</code> configuration file.
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
 * <p/>
 * <h4>Dynamic Configuration</h4>
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
     * Default value for clearOnFlush
     */
    public static final boolean DEFAULT_CLEAR_ON_FLUSH = true;

    /**
     * The default interval between runs of the expiry thread.
     */
    public static final long DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS = 120;

    /**
     * Set a buffer size for the spool of approx 30MB.
     */
    public static final int DEFAULT_SPOOL_BUFFER_SIZE = 30;

    /**
     * Default number of diskAccessStripes.
     */
    public static final int DEFAULT_DISK_ACCESS_STRIPES = 1;
    
    /**
     * Logging is off by default.
     */
    public static final boolean DEFAULT_LOGGING = false;
    
    /**
     * The default memory store eviction policy is LRU.
     */
    public static final MemoryStoreEvictionPolicy DEFAULT_MEMORY_STORE_EVICTION_POLICY = MemoryStoreEvictionPolicy.LRU;


    /**
     * The default cacheWriterConfiguration
     */
    public static final CacheWriterConfiguration DEFAULT_CACHE_WRITER_CONFIGURATION = new CacheWriterConfiguration();
    
    /**
     * Default value for copyOnRead
     */
    public static final boolean DEFAULT_COPY_ON_READ = false;
    
    /**
     * Default value for copyOnRead
     */
    public static final boolean DEFAULT_COPY_ON_WRITE = false;
    
    /**
     * Default value for ttl
     */
    public static final long DEFAULT_TTL = 0;
    
    /**
     * Default value for tti
     */
    public static final long DEFAULT_TTI = 0;

    /**
     * Default value for maxElementsOnDisk
     */
    public static final int DEFAULT_MAX_ELEMENTS_ON_DISK = 0;

    /**
     * Default value for transactionalMode
     */
    public static final TransactionalMode DEFAULT_TRANSACTIONAL_MODE = TransactionalMode.OFF;

    /**
     * Default value for statistics
     */
    public static final boolean DEFAULT_STATISTICS = false;

    /**
     * Default value for diskPersistent
     */
    public static final boolean DEFAULT_DISK_PERSISTENT = false;

    /**
     * Default copyStrategyConfiguration
     */
    public static final CopyStrategyConfiguration DEFAULT_COPY_STRATEGY_CONFIGURATION = new CopyStrategyConfiguration();
    
    private static final Logger LOG = LoggerFactory.getLogger(CacheConfiguration.class.getName());
    
    /**
     * the name of the cache.
     */
    protected volatile String name;

    /**
     * the maximum objects to be held in the {@link net.sf.ehcache.store.MemoryStore}.
     * <p>
     * <code>0</code> translates to no-limit.
     */
    protected volatile int maxElementsInMemory;

    /**
     * the maximum objects to be held in the {@link net.sf.ehcache.store.DiskStore}.
     * <p>
     * <code>0</code> translates to no-limit.
     */
    protected volatile int maxElementsOnDisk = DEFAULT_MAX_ELEMENTS_ON_DISK;

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
    protected volatile MemoryStoreEvictionPolicy memoryStoreEvictionPolicy = DEFAULT_MEMORY_STORE_EVICTION_POLICY;

    /**
     * Sets whether the MemoryStore should be cleared when
     * {@link net.sf.ehcache.Ehcache#flush flush()} is called on the cache - true by default.
     */
    protected volatile boolean clearOnFlush = DEFAULT_CLEAR_ON_FLUSH;


    /**
     * Sets whether elements are eternal. If eternal,  timeouts are ignored and the element
     * is never expired.
     */
    protected volatile boolean eternal;

    /**
     * the time to idle for an element before it expires. Is only used
     * if the element is not eternal.A value of 0 means do not check for idling.
     */
    protected volatile long timeToIdleSeconds = DEFAULT_TTI;

    /**
     * Sets the time to idle for an element before it expires. Is only used
     * if the element is not eternal. This attribute is optional in the configuration.
     * A value of 0 means do not check time to live.
     */
    protected volatile long timeToLiveSeconds = DEFAULT_TTL;

    /**
     * whether elements can overflow to disk when the in-memory cache
     * has reached the set limit.
     */
    protected volatile boolean overflowToDisk;

    /**
     * For caches that overflow to disk, whether the disk cache persists between CacheManager instances.
     */
    protected volatile boolean diskPersistent = DEFAULT_DISK_PERSISTENT;

    /**
     * The path where the disk store is located
     */
    protected volatile String diskStorePath = DiskStoreConfiguration.getDefaultPath();

    /**
     * The size of the disk spool used to buffer writes
     */
    protected volatile int diskSpoolBufferSizeMB = DEFAULT_SPOOL_BUFFER_SIZE;

    /**
     * The number of concurrent disk access stripes.
     */
    protected volatile int diskAccessStripes = DEFAULT_DISK_ACCESS_STRIPES;
    
    /**
     * The interval in seconds between runs of the disk expiry thread.
     * <p/>
     * 2 minutes is the default.
     * This is not the same thing as time to live or time to idle. When the thread runs it checks
     * these things. So this value is how often we check for expiry.
     */
    protected volatile long diskExpiryThreadIntervalSeconds = DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS;

    /**
     * Indicates whether logging is enabled or not. False by default.
     * Only used when cache is clustered with Terracotta.
     */
    protected volatile boolean logging = DEFAULT_LOGGING;

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
    protected CacheWriterConfiguration cacheWriterConfiguration = DEFAULT_CACHE_WRITER_CONFIGURATION;

    /**
     * The cache loader factories added by BeanUtils.
     */
    protected volatile List<CacheLoaderFactoryConfiguration> cacheLoaderConfigurations = new ArrayList<CacheLoaderFactoryConfiguration>();
    
    /**
     * The cache decorator factories added by BeanUtils.
     */
    protected volatile List<CacheDecoratorFactoryConfiguration> cacheDecoratorConfigurations = 
        new ArrayList<CacheDecoratorFactoryConfiguration>();

    /**
     * The listeners for this configuration.
     */
    protected volatile Set<CacheConfigurationListener> listeners = new CopyOnWriteArraySet<CacheConfigurationListener>();

    private volatile boolean frozen;
    private TransactionalMode transactionalMode = DEFAULT_TRANSACTIONAL_MODE;
    private volatile boolean statistics = DEFAULT_STATISTICS;
    private volatile CopyStrategyConfiguration copyStrategyConfiguration = DEFAULT_COPY_STRATEGY_CONFIGURATION;
    private volatile Boolean copyOnRead;
    private volatile Boolean copyOnWrite;
    private Object defaultTransactionManager;

    /**
     * Default constructor that can only be used by classes in this package.
     */
    public CacheConfiguration() {
        // default constructor is only accessible in this package
    }

    /**
     * Create a new cache configuration.
     *
     * @param name                the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxElementsInMemory the maximum number of elements in memory, before they are evicted (0 == no limit)
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
        
        cloneCacheDecoratorConfigurations(config);

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
    
    private void cloneCacheDecoratorConfigurations(CacheConfiguration config) {
        if (cacheDecoratorConfigurations.size() > 0) {
            List<CacheDecoratorFactoryConfiguration> copy = new ArrayList<CacheDecoratorFactoryConfiguration>();
            for (CacheDecoratorFactoryConfiguration item : cacheDecoratorConfigurations) {
                copy.add(item.clone());
            }
            config.cacheDecoratorConfigurations = copy;
        }
    }

    /**
     * Sets the name of the cache.
     * @param name the cache name. This must be unique. The / character is illegal. The # character does not work with RMI replication.
     */
    public final void setName(String name) {
        checkDynamicChange();
        if (name == null) {
            throw new IllegalArgumentException("Cache name cannot be null.");
        }
        this.name = name;
    }

    /**
     * Builder to set the name of the cache.
     * @param name the cache name. This must be unique. The / character is illegal. The # character does not work with RMI replication.
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
     * Only used when cache is clustered with Terracotta
     * @param enable If true, enables logging otherwise disables logging
     */
    public final void setLogging(boolean enable) {
        checkDynamicChange();
        boolean oldLoggingEnabled = this.logging;
        this.logging = enable;
        fireLoggingChanged(oldLoggingEnabled, enable);
    }

    /**
     * Builder to enable or disable logging for the cache
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     * Only used when cache is clustered with Terracotta
     * @param enable If true, enables logging otherwise disables logging
     * @return this configuration instance
     * @see #setLogging(boolean)
     */
    public final CacheConfiguration logging(boolean enable) {
        setLogging(enable);
        return this;
    }

    /**
     * Sets the maximum objects to be held in memory (0 = no limit).
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     * @param maxElementsInMemory The maximum number of elements in memory, before they are evicted (0 == no limit)
     */
    public final void setMaxElementsInMemory(int maxElementsInMemory) {
        checkDynamicChange();
        int oldCapacity = this.maxElementsInMemory;
        int newCapacity = maxElementsInMemory;
        this.maxElementsInMemory = maxElementsInMemory;
        fireMemoryCapacityChanged(oldCapacity, newCapacity);
    }

    /**
     * Builder that sets the maximum objects to be held in memory (0 = no limit).
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     * @param maxElementsInMemory The maximum number of elements in memory, before they are evicted (0 == no limit)
     * @return this configuration instance
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
     * Builder that sets the eviction policy. An invalid argument will set it to null.
     * @param memoryStoreEvictionPolicy a String representation of the policy. One of "LRU", "LFU" or "FIFO".
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
     * Builder which Sets the eviction policy. An invalid argument will set it to null.
     *
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
     * @param clearOnFlush true to clear on flush
     */
    public final void setClearOnFlush(boolean clearOnFlush) {
        checkDynamicChange();
        this.clearOnFlush = clearOnFlush;
    }

    /**
     * Builder which sets whether the MemoryStore should be cleared when
     * {@link net.sf.ehcache.Ehcache#flush flush()} is called on the cache - true by default.
     * @param clearOnFlush true to clear on flush
     * @return this configuration instance
     * @see #setClearOnFlush(boolean)
     */
    public final CacheConfiguration clearOnFlush(boolean clearOnFlush) {
        setClearOnFlush(clearOnFlush);
        return this;
    }

    /**
     * Sets whether elements are eternal. If eternal, timeouts are ignored and the element is never expired. False by default.
     * @param  eternal true for eternal
     */
    public final void setEternal(boolean eternal) {
        checkDynamicChange();
        checkConflictingEternalValues(eternal, getTimeToLiveSeconds(), getTimeToIdleSeconds());
        this.eternal = eternal;
    }

    private void checkConflictingEternalValues(boolean newEternalValue, long newTTLValue, long newTTIValue) {
        if (newEternalValue && (newTTLValue != 0 || newTTIValue != 0)) {
            throw new CacheException(
                    "Conflicting values detected. When eternal is true, timeToLiveSeconds and timeToIdleSeconds should be zero. Trying to set eternal="
                            + newEternalValue + ", timeToLiveSeconds=" + newTTLValue + ", timeToIdleSeconds=" + newTTIValue + " )");
        }
    }

    /**
     * Builder which sets whether elements are eternal. If eternal, timeouts are ignored and the element is never expired. False by default.
     * @param  eternal true for eternal
     * @return this configuration instance
     * @see #setEternal(boolean)
     */
    public final CacheConfiguration eternal(boolean eternal) {
        setEternal(eternal);
        return this;
    }

    /**
     * Sets the time to idle for an element before it expires. Is only used if the element is not eternal. This can be overidden in
     * {@link net.sf.ehcache.Element}
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     * @param timeToIdleSeconds   the default amount of time to live for an element from its last accessed or modified date
     */
    public final void setTimeToIdleSeconds(long timeToIdleSeconds) {
        checkDynamicChange();
        checkConflictingEternalValues(eternal, getTimeToLiveSeconds(), timeToIdleSeconds);
        long oldTti = this.timeToIdleSeconds;
        long newTti = timeToIdleSeconds;
        this.timeToIdleSeconds = timeToIdleSeconds;
        fireTtiChanged(oldTti, newTti);
    }

    /**
     * Builder which sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     * This default can be overridden in {@link net.sf.ehcache.Element}
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     * @param timeToIdleSeconds   the default amount of time to live for an element from its last accessed or modified date
     * @return this configuration instance
     * @see #setTimeToIdleSeconds(long)
     */
    public final CacheConfiguration timeToIdleSeconds(long timeToIdleSeconds) {
        setTimeToIdleSeconds(timeToIdleSeconds);
        return this;
    }

    /**
     * Sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     * This default can be overridden in {@link net.sf.ehcache.Element}
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     * @param timeToLiveSeconds the default amount of time to live for an element from its creation date
     */
    public final void setTimeToLiveSeconds(long timeToLiveSeconds) {
        checkDynamicChange();
        checkConflictingEternalValues(eternal, timeToLiveSeconds, getTimeToIdleSeconds());
        long oldTtl = this.timeToLiveSeconds;
        long newTtl = timeToLiveSeconds;
        this.timeToLiveSeconds = timeToLiveSeconds;
        fireTtlChanged(oldTtl, newTtl);
    }

    /**
     * Builder which sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     * This default can be overridden in {@link net.sf.ehcache.Element}
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     * @param timeToLiveSeconds the default amount of time to live for an element from its creation date
     * @return this configuration instance
     * @see #setTimeToLiveSeconds(long)
     */
    public final CacheConfiguration timeToLiveSeconds(long timeToLiveSeconds) {
        setTimeToLiveSeconds(timeToLiveSeconds);
        return this;
    }

    /**
     * Sets whether elements can overflow to disk when the in-memory cache has reached the set limit.
     * @param overflowToDisk whether to use the disk store
     */
    public final void setOverflowToDisk(boolean overflowToDisk) {
        checkDynamicChange();
        this.overflowToDisk = overflowToDisk;
        validateConfiguration();
    }

    /**
     * Builder which sets whether elements can overflow to disk when the in-memory cache has reached the set limit.
     * @param overflowToDisk whether to use the disk store
     * @return this configuration instance
     * @see #setOverflowToDisk(boolean)
     */
    public final CacheConfiguration overflowToDisk(boolean overflowToDisk) {
        setOverflowToDisk(overflowToDisk);
        return this;
    }

    /**
     * Sets whether the disk store persists between CacheManager instances. Note that this operates independently of {@link #overflowToDisk}.
     * @param diskPersistent  whether to persist the cache to disk between JVM restarts
     */
    public final void setDiskPersistent(boolean diskPersistent) {
        checkDynamicChange();
        this.diskPersistent = diskPersistent;
        validateConfiguration();
    }

    /**
     * Builder which sets whether the disk store persists between CacheManager instances. Note that this operates independently of {@link #overflowToDisk}.
     * @param diskPersistent  whether to persist the cache to disk between JVM restarts.
     * @return this configuration instance
     * @see #setDiskPersistent(boolean)
     */
    public final CacheConfiguration diskPersistent(boolean diskPersistent) {
        setDiskPersistent(diskPersistent);
        return this;
    }

    /**
     * Sets the path that will be used for the disk store.
     * @param diskStorePath this parameter is ignored. CacheManager sets it using setter injection.
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
     * Builder which sets the path that will be used for the disk store.
     * @param diskStorePath this parameter is ignored. CacheManager sets it using setter injection.
     * @return this configuration instance
     * @see #setDiskStorePath(String)
     */
    public final CacheConfiguration diskStorePath(String diskStorePath) {
        setDiskStorePath(diskStorePath);
        return this;
    }

    /**
     * Sets the disk spool size, which is used to buffer writes to the DiskStore.
     * If not set it defaults to {@link #DEFAULT_SPOOL_BUFFER_SIZE}
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
     * Builder which sets the disk spool size, which is used to buffer writes to the DiskStore.
     * If not set it defaults to {@link #DEFAULT_SPOOL_BUFFER_SIZE}
     * @param diskSpoolBufferSizeMB a positive number
     * @return this configuration instance
     * @see #setDiskSpoolBufferSizeMB(int)
     */
    public final CacheConfiguration diskSpoolBufferSizeMB(int diskSpoolBufferSizeMB) {
        setDiskSpoolBufferSizeMB(diskSpoolBufferSizeMB);
        return this;
    }

    /**
     * Sets the number of disk stripes. RandomAccessFiles used to access the data file. By default there
     * is one stripe.
     *
     * @param stripes number of stripes (rounded up to a power-of-2)
     */
    public void setDiskAccessStripes(int stripes) {
        checkDynamicChange();
        if (stripes <= 0) {
            this.diskAccessStripes = DEFAULT_DISK_ACCESS_STRIPES;
        } else {
            this.diskAccessStripes = stripes;
        }
    }
    
    /**
     * Builder which sets the number of disk stripes. RandomAccessFiles used to access the data file. By default there
     * is one stripe.
     * @return this configuration instance
     * @see #setDiskAccessStripes(int)
     */
    public final CacheConfiguration diskAccessStripes(int stripes) {
        setDiskAccessStripes(stripes);
        return this;
    }
    
    /**
     * Sets the maximum number elements on Disk. 0 means unlimited.
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     * @param maxElementsOnDisk the maximum number of Elements to allow on the disk. 0 means unlimited.
     */
    public void setMaxElementsOnDisk(int maxElementsOnDisk) {
        checkDynamicChange();
        int oldCapacity = this.maxElementsOnDisk;
        int newCapacity = maxElementsOnDisk;
        this.maxElementsOnDisk = maxElementsOnDisk;
        fireDiskCapacityChanged(oldCapacity, newCapacity);
    }

    /**
     * Builder which sets the maximum number elements on Disk. 0 means unlimited.
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     * @param maxElementsOnDisk the maximum number of Elements to allow on the disk. 0 means unlimited.
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
     * Builder which sets the interval in seconds between runs of the disk expiry thread.
     * <p/>
     * 2 minutes is the default.
     * This is not the same thing as time to live or time to idle. When the thread runs it checks
     * these things. So this value is how often we check for expiry.
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
     * @return true is this configuration is frozen - it cannot be changed dynamically.
     */
    public boolean isFrozen() {
      return frozen;
    }

    /**
     * Getter to the CopyStrategy set in the config (really? how?).
     * This will always return the same unique instance per cache
     * @return the {@link CopyStrategy} for instance for this cache
     */
    public CopyStrategy getCopyStrategy() {
        // todo really make this pluggable through config!
        return copyStrategyConfiguration.getCopyStrategyInstance();
    }

    /**
     * Whether the Cache should copy elements it returns
     * @param copyOnRead true, if copyOnRead
     */
    public CacheConfiguration copyOnRead(boolean copyOnRead) {
        this.setCopyOnRead(copyOnRead);
        return this;
    }

    /**
     * Whether the Cache should copy elements it returns
     * @return true, is copyOnRead
     */
    public boolean isCopyOnRead() {
        validateTransactionalSettings();
        return copyOnRead;
    }

    /**
     * Whether the Cache should copy elements it returns
     * @param copyOnRead true, if copyOnRead
     */
    public void setCopyOnRead(final boolean copyOnRead) {
        this.copyOnRead = copyOnRead;
    }

    /**
     * Whether the Cache should copy elements it gets
     * @param copyOnWrite true, if copyOnWrite
     */
    public CacheConfiguration copyOnWrite(boolean copyOnWrite) {
        this.copyOnWrite = copyOnWrite;
        return this;
    }

    /**
     * Whether the Cache should copy elements it gets
     * @return true, if copyOnWrite
     */
    public boolean isCopyOnWrite() {
        validateTransactionalSettings();
        return copyOnWrite;
    }

    /**
     * Whether the Cache should copy elements it gets
     * @param copyOnWrite true, if copyOnWrite
     */
    public void setCopyOnWrite(final boolean copyOnWrite) {
        this.copyOnWrite = copyOnWrite;
    }

    /**
     * Sets the CopyStrategyConfiguration for this cache
     * @param copyStrategyConfiguration the CopyStrategy Configuration
     */
    public void addCopyStrategy(CopyStrategyConfiguration copyStrategyConfiguration) {
        this.copyStrategyConfiguration = copyStrategyConfiguration;
    }
    
    /**
     * Returns the copyStrategyConfiguration
     * @return the copyStrategyConfiguration
     */
    public CopyStrategyConfiguration getCopyStrategyConfiguration() {
        return this.copyStrategyConfiguration;
    }

    /**
     * Getter to the default TM to use
     * @return the default one if set, or null
     */
    public Object getDefaultTransactionManager() {
        return defaultTransactionManager;
    }

    /**
     * Setter to the default TM
     * @param defaultTransactionManager the default TM, can be null to fall back to TMLookup
     */
    public void setDefaultTransactionManager(final Object defaultTransactionManager) {
        this.defaultTransactionManager = defaultTransactionManager;
    }

    /**
     * Configuration for the CachePeerListenerFactoryConfiguration.
     */
    public static final class CacheEventListenerFactoryConfiguration extends FactoryConfiguration<CacheEventListenerFactoryConfiguration> {
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
         * @return this factory configuration instance
         * @see #setListenFor(String)
         */
        public final CacheEventListenerFactoryConfiguration listenFor(String listenFor) {
            setListenFor(listenFor);
            return this;
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
     * <p/>
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
     * Configuration for the CacheDecoratorFactoryConfiguration.
     */
    public static final class CacheDecoratorFactoryConfiguration extends FactoryConfiguration<CacheDecoratorFactoryConfiguration> {
    }

    /**
     * Used by BeanUtils to add each cacheDecoratorFactory to the cache configuration.
     *
     * @param factory
     */
    public final void addCacheDecoratorFactory(CacheDecoratorFactoryConfiguration factory) {
        checkDynamicChange();
        cacheDecoratorConfigurations.add(factory);
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

    /**
     * Sets the transactionalMode
     *
     * @param transactionalMode OFF or XA
     */
    public final void setTransactionalMode(final String transactionalMode) {
        if (null == transactionalMode) {
            throw new IllegalArgumentException("TransactionalMode value must be non-null");
        }
        this.transactionalMode = TransactionalMode.valueOf(transactionalMode.toUpperCase());
    }

    /**
     * Builder which sets the transactionalMode
     * @param transactionalMode one of OFF or XA
     * @return this configuration instance
     * @see #setTransactionalMode(String)
     */
    public final CacheConfiguration transactionalMode(String transactionalMode) {
        setTransactionalMode(transactionalMode);
        return this;
    }

    /**
     * Builder which sets the transactionalMode
     * @param transactionalMode one of OFF or XA enum values
     * @return this configuration instance
     * @see #setTransactionalMode(String)
     */
    public final CacheConfiguration transactionalMode(TransactionalMode transactionalMode) {
        if (null == transactionalMode) {
            throw new IllegalArgumentException("TransactionalMode value must be non-null");
        }
        this.transactionalMode = transactionalMode;
        return this;
    }

    /**
     * Sets whether the cache's statistics are enabled. at startup
     */
    public final void setStatistics(boolean enabled) {
        this.statistics = enabled;
    }

    /**
     * Builder which sets whether the cache's statistics are enabled.
     *
     * @return this configuration instance
     * @see #setStatistics(boolean)
     */
    public final CacheConfiguration statistics(boolean statistics) {
        setStatistics(statistics);
        return this;
    }


    /**
     * Gets whether the cache's statistics will be enabled at startup
     */
    public final boolean getStatistics() {
        return statistics;
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
                    if (!listenerConfig.getFullyQualifiedClassPath().startsWith("net.sf.ehcache.") &&
                            LOG.isWarnEnabled()) {
                        LOG.warn("The non-standard CacheEventListenerFactory '" + listenerConfig.getFullyQualifiedClassPath() +
                                "' is used with a clustered Terracotta cache, " +
                                "if the purpose of this listener is replication it is not supported in a clustered context");
                    }
                }
            }
        }
    }

    private void validateTransactionalSettings() {
        boolean transactional = transactionalMode.isTransactional();
        if (copyOnRead == null) {
            if (terracottaConfiguration != null && terracottaConfiguration.isCopyOnReadSet()) {
                copyOnRead = terracottaConfiguration.isCopyOnRead();
            } else {
                copyOnRead = transactional;
            }
        }
        if (copyOnWrite == null) {
            copyOnWrite = transactional;
        }

        if (transactional) {
            if (!copyOnRead || !copyOnWrite) {
                throw new InvalidConfigurationException("A transactional cache has to be copyOnRead and copyOnWrite!");
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
     */
    public int getDiskAccessStripes() {
        return diskAccessStripes;
    }
    
    /**
     * Only used when cache is clustered with Terracotta
     *
     * @return true if logging is enabled otherwise false
     */
    public boolean getLogging() {
        return logging;
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
    public List<CacheDecoratorFactoryConfiguration> getCacheDecoratorConfigurations() {
        return cacheDecoratorConfigurations;
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
     *
     * @return transactionaMode
     */
    public final TransactionalMode getTransactionalMode() {
        return transactionalMode;
    }

    /**
     * Helper method to compute whether the cache is transactional or not
     *
     * @return true if transactionalMode="XA"
     */
    public boolean isTransactional() {
        validateTransactionalSettings();
        return transactionalMode.isTransactional();
    }

    /**
     * Represents whether the Cache is transactional or not.
     *
     * @author alexsnaps
     */
    public static enum TransactionalMode {

        /**
         * No Transactions
         */
        OFF(false),

        /**
         * XA Transactions
         */
        XA(true);

        private final boolean transactional;

        /**
         * @param transactional
         */
        TransactionalMode(final boolean transactional) {
            this.transactional = transactional;
        }

        /**
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

    private void fireLoggingChanged(boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            for (CacheConfigurationListener l : listeners) {
                l.loggingChanged(oldValue, newValue);
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
     * Intended for internal use only, and subject to change.
     * This is required so that changes in store implementation's config
     * (probably from other nodes) can propagate up to here
     */
    public void internalSetTimeToIdle(long timeToIdle) {
        this.timeToIdleSeconds = timeToIdle;
    }

    /**
     * Intended for internal use only, and subject to change.
     */
    public void internalSetTimeToLive(long timeToLive) {
        this.timeToLiveSeconds = timeToLive;
    }

    /**
     * Intended for internal use only, and subject to change.
     */
    public void internalSetMemCapacity(int capacity) {
        this.maxElementsInMemory = capacity;
    }

    /**
     * Intended for internal use only, and subject to change.
     */
    public void internalSetDiskCapacity(int capacity) {
        this.maxElementsOnDisk = capacity;
    }

    /**
     * Intended for internal use only, and subject to change.
     */
    public void internalSetLogging(boolean logging) {
        this.logging = logging;
    }
    
    /**
     * Intended for internal use only, and subject to change.
     * This is called from the store implementations to reflect the new coherent value
     *
     * @param coherent true for coherent
     */
    public void internalSetCoherent(boolean coherent) {
        if (isTerracottaClustered()) {
            this.getTerracottaConfiguration().setCoherent(coherent);
        }
    }
}
