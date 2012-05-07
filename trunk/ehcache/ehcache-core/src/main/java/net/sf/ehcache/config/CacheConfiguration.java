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

package net.sf.ehcache.config;

import static net.sf.ehcache.config.Configuration.getAllActiveCaches;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.event.NotificationScope;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A value object used to represent cache configuration.
 * <h4>Construction Patterns</h4>
 * The recommended way of creating a <code>Cache</code> in Ehcache 2.0 and above is to create a <code>CacheConfiguration</code> object
 * and pass it to the <code>Cache</code> constructor. See {@link net.sf.ehcache.Cache#Cache(CacheConfiguration)}.
 * <p/>
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
 * @author Greg Luck
 * @author Chris Dennis
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

    /**
     * Default maxBytesOnHeap value
     */
    public static final long DEFAULT_MAX_BYTES_ON_HEAP  = 0;

    /**
     * Default maxBytesOffHeap value
     */
    public static final long DEFAULT_MAX_BYTES_OFF_HEAP = 0;

    /**
     * Default maxBytesOnDisk value
     */
    public static final long DEFAULT_MAX_BYTES_ON_DISK  = 0;

    /**
     * Default eternal value
     */
    public static final boolean DEFAULT_ETERNAL_VALUE = false;


    private static final Logger LOG = LoggerFactory.getLogger(CacheConfiguration.class.getName());
    private static final int HUNDRED_PERCENT = 100;
    private static final int MINIMUM_RECOMMENDED_IN_MEMORY = 100;

    /**
     * the name of the cache.
     */
    protected volatile String name;

    /**
     * Timeout in milliseconds for CacheLoader related calls
     */
    protected volatile long cacheLoaderTimeoutMillis;

    /**
     * the maximum objects to be held in the {@link net.sf.ehcache.store.MemoryStore}.
     * <p/>
     * <code>0</code> translates to no-limit.
     */
    protected volatile Integer maxEntriesLocalHeap;

    /**
     * the maximum objects to be held in the {@link net.sf.ehcache.store.disk.DiskStore}.
     * <p/>
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
     * Sets whether elements are eternal. If eternal, timeouts are ignored and the element
     * is never expired.
     */
    protected volatile boolean eternal = DEFAULT_ETERNAL_VALUE;

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
    protected volatile Boolean overflowToDisk;

    /**
     * For caches that overflow to disk, whether the disk cache persists between CacheManager instances.
     */
    protected volatile boolean diskPersistent = DEFAULT_DISK_PERSISTENT;

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
     * whether elements can overflow to off heap memory when the in-memory cache
     * has reached the set limit.
     */
    protected volatile Boolean overflowToOffHeap;

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
     * The PinningConfiguration.
     */
    protected volatile PinningConfiguration pinningConfiguration;

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
    private volatile TransactionalMode transactionalMode;
    private volatile boolean statistics = DEFAULT_STATISTICS;
    private volatile CopyStrategyConfiguration copyStrategyConfiguration = DEFAULT_COPY_STRATEGY_CONFIGURATION.copy();
    private volatile SizeOfPolicyConfiguration sizeOfPolicyConfiguration;
    private volatile PersistenceConfiguration persistenceConfiguration;
    private volatile ElementValueComparatorConfiguration elementValueComparatorConfiguration =
        new ElementValueComparatorConfiguration();
    private volatile Boolean copyOnRead;
    private volatile Boolean copyOnWrite;
    private volatile boolean conflictingEternalValuesWarningLogged;
    private volatile Searchable searchable;
    private String maxBytesLocalHeapInput;
    private String maxBytesLocalOffHeapInput;
    private String maxBytesLocalDiskInput;
    private Long maxBytesLocalHeap;
    private Long maxBytesLocalOffHeap;
    private Long maxBytesLocalDisk;
    private Integer maxBytesLocalHeapPercentage;
    private Integer maxBytesLocalOffHeapPercentage;
    private Integer maxBytesLocalDiskPercentage;
    private PoolUsage onHeapPoolUsage;
    private PoolUsage offHeapPoolUsage;
    private PoolUsage onDiskPoolUsage;
    private volatile boolean maxEntriesLocalDiskExplicitlySet;
    private volatile boolean maxBytesLocalDiskExplicitlySet;
    private volatile boolean maxBytesLocalOffHeapExplicitlySet;

    /**
     * Default constructor.
     * <p/>
     * Note that an empty Cache is not valid and must have extra configuration added which can be done
     * through the fluent methods in this class. Call <code>validateConfiguration()</code> to check your configuration.
     *
     * @see #validateCompleteConfiguration
     */
    public CacheConfiguration() {
        // empty constructor
    }

    /**
     * Create a new cache configuration.
     * <p/>
     * Extra configuration can added after construction via the fluent methods in this class.
     * Call <code>validateConfiguration()</code> to check your configuration.
     *
     * @param name                the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxEntriesLocalHeap the maximum number of elements in memory, before they are evicted (0 == no limit)
     * @see #validateCompleteConfiguration()
     */
    public CacheConfiguration(String name, int maxEntriesLocalHeap) {
        this.name = name;
        verifyGreaterThanOrEqualToZero((long) maxEntriesLocalHeap, "maxEntriesLocalHeap");
        this.maxEntriesLocalHeap = maxEntriesLocalHeap;
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

    private void assertArgumentNotNull(String name, Object object) {
        if (object == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
    }

    /**
     * Sets the name of the cache.
     *
     * @param name the cache name. This must be unique. The / character is illegal. The # character does not work with RMI replication.
     */
    public final void setName(String name) {
        checkDynamicChange();
        assertArgumentNotNull("Cache name", name);
        this.name = name;
    }

    /**
     * Builder to set the name of the cache.
     *
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
     *
     * @param enable If true, enables logging otherwise disables logging
     */
    public final void setLogging(boolean enable) {
        checkDynamicChange();
        boolean oldLoggingEnabled = this.logging;
        this.logging = enable;
        fireLoggingChanged(oldLoggingEnabled, enable);
    }

    /**
     * Enables or disables offheap store for the cache.
     *
     * @param overflowToOffHeap If true, enables offheap store otherwise disables it.
     */
    public final void setOverflowToOffHeap(boolean overflowToOffHeap) {
        checkDynamicChange();
        this.overflowToOffHeap = overflowToOffHeap;
    }

    /**
     * Builder to enable or disable offheap store for the cache.
     *
     * @param overflowToOffHeap If true, enables offheap store otherwise disables it.
     * @return this configuration instance
     * @see #setOverflowToOffHeap(boolean)
     */
    public CacheConfiguration overflowToOffHeap(boolean overflowToOffHeap) {
        setOverflowToOffHeap(overflowToOffHeap);
        return this;
    }

    /**
     * Sets the SizeOfPolicyConfiguration for this cache.
     *
     * @param sizeOfPolicyConfiguration the SizeOfPolicy Configuration
     */
    public void addSizeOfPolicy(SizeOfPolicyConfiguration sizeOfPolicyConfiguration) {
        this.sizeOfPolicyConfiguration = sizeOfPolicyConfiguration;
    }

    /**
     * Builder to set the SizeOfPolicyConfiguration for this cache.
     *
     * @param sizeOfPolicyConfiguration the SizeOfPolicy Configuration
     * @return this configuration instance
     * @see #addSizeOfPolicy(SizeOfPolicyConfiguration)
     */
    public CacheConfiguration sizeOfPolicy(SizeOfPolicyConfiguration sizeOfPolicyConfiguration) {
        addSizeOfPolicy(sizeOfPolicyConfiguration);
        return this;
    }

    /**
     * Sets the PersistenceConfiguration for this cache.
     *
     * @param persistenceConfiguration the Persistence Configuration
     */
    public void addPersistence(PersistenceConfiguration persistenceConfiguration) {
        this.persistenceConfiguration = persistenceConfiguration;
    }

    /**
     * Builder to set the PersistenceConfiguration for this cache.
     *
     * @param persistenceConfiguration the Persistence Configuration
     * @return this configuration instance
     * @see #addPersistence(PersistenceConfiguration)
     */
    public CacheConfiguration persistence(PersistenceConfiguration persistenceConfiguration) {
        addPersistence(persistenceConfiguration);
        return this;
    }

    /**
     * Sets the max off heap memory size allocated for this cache.
     *
     * @param maxMemoryOffHeap the max off heap memory size allocated for this cache.
     */
    public final void setMaxMemoryOffHeap(String maxMemoryOffHeap) {
        checkDynamicChange();
        assertArgumentNotNull("Cache maxMemoryOffHeap", maxMemoryOffHeap);
        setMaxBytesLocalOffHeap(maxMemoryOffHeap);
    }

    /**
     * Builder to set the max off heap memory size allocated for this cache.
     *
     * @param maxMemoryOffHeap the max off heap memory size allocated for this cache.
     * @return this configuration instance
     * @see #setMaxMemoryOffHeap(String)
     */
    public CacheConfiguration maxMemoryOffHeap(String maxMemoryOffHeap) {
        setMaxMemoryOffHeap(maxMemoryOffHeap);
        return this;
    }

    /**
     * Builder to enable or disable logging for the cache
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     * Only used when cache is clustered with Terracotta
     *
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
     *
     * @param maxElementsInMemory The maximum number of elements in memory, before they are evicted (0 == no limit)
     * @deprecated use {@link #setMaxEntriesLocalHeap(long)}
     */
    @Deprecated
    public final void setMaxElementsInMemory(int maxElementsInMemory) {
        setMaxEntriesLocalHeap(maxElementsInMemory);
    }

    /**
     * Sets the maximum objects to be held in local heap memory (0 = no limit).
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     *
     * @param maxEntriesLocalHeap The maximum number of elements in memory, before they are evicted (0 == no limit)
     */
    public final void setMaxEntriesLocalHeap(long maxEntriesLocalHeap) {
        verifyGreaterThanOrEqualToZero(maxEntriesLocalHeap, "maxEntriesLocalHeap");
        if (maxEntriesLocalHeap > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Values larger than Integer.MAX_VALUE are not currently supported.");
        }

        checkDynamicChange();
        if (onHeapPoolUsage != null && onHeapPoolUsage != PoolUsage.None) {
            throw new InvalidConfigurationException("MaxEntriesLocalHeap is not compatible with " +
                                                    "MaxBytesLocalHeap set on cache");
        }
        int oldCapacity = this.maxEntriesLocalHeap == null ? 0 : this.maxEntriesLocalHeap;
        int newCapacity = (int) maxEntriesLocalHeap;
        this.maxEntriesLocalHeap = (int) maxEntriesLocalHeap;
        fireMemoryCapacityChanged(oldCapacity, newCapacity);
    }

    /**
     * Builder that sets the maximum objects to be held in memory (0 = no limit).
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     *
     * @param maxElementsInMemory The maximum number of elements in memory, before they are evicted (0 == no limit)
     * @return this configuration instance
     * @deprecated use {@link #maxEntriesLocalHeap(int)}
     */
    @Deprecated
    public final CacheConfiguration maxElementsInMemory(int maxElementsInMemory) {
        setMaxElementsInMemory(maxElementsInMemory);
        return this;
    }

    /**
     * Builder that sets the maximum objects to be held in memory (0 = no limit).
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     *
     * @param maxElementsInMemory The maximum number of elements in memory, before they are evicted (0 == no limit)
     * @return this configuration instance
     */
    public final CacheConfiguration maxEntriesLocalHeap(int maxElementsInMemory) {
        setMaxEntriesLocalHeap(maxElementsInMemory);
        return this;
    }

    /**
     * Sets the timeout for CacheLoader execution (0 = no timeout).
     *
     * @param cacheLoaderTimeoutMillis the timeout in milliseconds.
     */
    public final void setCacheLoaderTimeoutMillis(long cacheLoaderTimeoutMillis) {
        checkDynamicChange();
        this.cacheLoaderTimeoutMillis = cacheLoaderTimeoutMillis;
    }

    /**
     * Builder that sets the timeout for CacheLoader execution (0 = no timeout).

     * @param timeoutMillis the timeout in milliseconds.
     * @return this configuration instance
     */
    public CacheConfiguration timeoutMillis(long timeoutMillis) {
        setCacheLoaderTimeoutMillis(timeoutMillis);
        return this;
    }

    /**
     * Sets the eviction policy. An invalid argument will set it to LRU.
     *
     * @param memoryStoreEvictionPolicy a String representation of the policy. One of "LRU", "LFU" or "FIFO".
     */
    public final void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
        assertArgumentNotNull("Cache memoryStoreEvictionPolicy", memoryStoreEvictionPolicy);
        setMemoryStoreEvictionPolicyFromObject(MemoryStoreEvictionPolicy.fromString(memoryStoreEvictionPolicy));
    }

    /**
     * Builder that sets the eviction policy. An invalid argument will set it to null.
     *
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
     *
     * @param clearOnFlush true to clear on flush
     */
    public final void setClearOnFlush(boolean clearOnFlush) {
        checkDynamicChange();
        this.clearOnFlush = clearOnFlush;
    }

    /**
     * Builder which sets whether the MemoryStore should be cleared when
     * {@link net.sf.ehcache.Ehcache#flush flush()} is called on the cache - true by default.
     *
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
     *
     * @param eternal true for eternal
     */
    public final void setEternal(boolean eternal) {
        checkDynamicChange();
        isEternalValueConflictingWithTTIOrTTL(eternal, getTimeToLiveSeconds(), getTimeToIdleSeconds());
        this.eternal = eternal;
        if (eternal) {
            setTimeToIdleSeconds(0);
            setTimeToLiveSeconds(0);
        }
    }

    private boolean isEternalValueConflictingWithTTIOrTTL(boolean newEternalValue, long newTTLValue, long newTTIValue) {
        boolean conflicting = false;

        if (newEternalValue && (newTTLValue != 0 || newTTIValue != 0)) {
            conflicting = true;
        }

        if (conflicting && !conflictingEternalValuesWarningLogged) {
            conflictingEternalValuesWarningLogged = true;
            LOG.warn("Cache '" + getName() + "' is set to eternal but also has TTI/TTL set. "
                    + " To avoid this warning, clean up the config " + "removing conflicting values of eternal,"
                    + " TTI and TTL. Effective configuration for Cache '" + getName() + "' will be eternal='" + newEternalValue
                    + "', timeToIdleSeconds='0', timeToLiveSeconds='0'.");
        }
        return conflicting;
    }

    /**
     * Builder which sets whether elements are eternal. If eternal, timeouts are ignored and the element is never expired. False by default.
     *
     * @param eternal true for eternal
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
     *
     * @param timeToIdleSeconds the default amount of time to live for an element from its last accessed or modified date
     */
    public final void setTimeToIdleSeconds(long timeToIdleSeconds) {
        checkDynamicChange();
        verifyGreaterThanOrEqualToZero(timeToIdleSeconds, "timeToIdleSeconds");
        if (!isEternalValueConflictingWithTTIOrTTL(eternal, 0, timeToIdleSeconds)) {
            long oldTti = this.timeToIdleSeconds;
            long newTti = timeToIdleSeconds;
            this.timeToIdleSeconds = timeToIdleSeconds;
            fireTtiChanged(oldTti, newTti);
        }
    }

    /**
     * Builder which sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     * This default can be overridden in {@link net.sf.ehcache.Element}
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     *
     * @param timeToIdleSeconds the default amount of time to live for an element from its last accessed or modified date
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
     *
     * @param timeToLiveSeconds the default amount of time to live for an element from its creation date
     */
    public final void setTimeToLiveSeconds(long timeToLiveSeconds) {
        checkDynamicChange();
        verifyGreaterThanOrEqualToZero(timeToLiveSeconds, "timeToLiveSeconds");
        if (!isEternalValueConflictingWithTTIOrTTL(eternal, timeToLiveSeconds, 0)) {
            long oldTtl = this.timeToLiveSeconds;
            long newTtl = timeToLiveSeconds;
            this.timeToLiveSeconds = timeToLiveSeconds;
            fireTtlChanged(oldTtl, newTtl);
        }
    }

    /**
     * Builder which sets the time to idle for an element before it expires. Is only used if the element is not eternal.
     * This default can be overridden in {@link net.sf.ehcache.Element}
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     *
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
     *
     * @param overflowToDisk whether to use the disk store
     */
    public final void setOverflowToDisk(boolean overflowToDisk) {
        checkDynamicChange();
        this.overflowToDisk = overflowToDisk;
        validateConfiguration();
    }

    /**
     * Builder which sets whether elements can overflow to disk when the in-memory cache has reached the set limit.
     *
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
     *
     * @param diskPersistent whether to persist the cache to disk between JVM restarts
     */
    public final void setDiskPersistent(boolean diskPersistent) {
        checkDynamicChange();
        this.diskPersistent = diskPersistent;
        validateConfiguration();
    }

    /**
     * Builder which sets whether the disk store persists between CacheManager instances. Note that this operates independently of {@link #overflowToDisk}.
     *
     * @param diskPersistent whether to persist the cache to disk between JVM restarts.
     * @return this configuration instance
     * @see #setDiskPersistent(boolean)
     */
    public final CacheConfiguration diskPersistent(boolean diskPersistent) {
        setDiskPersistent(diskPersistent);
        return this;
    }

    /**
     * Sets the disk spool size, which is used to buffer writes to the DiskStore.
     * If not set it defaults to {@link #DEFAULT_SPOOL_BUFFER_SIZE}
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
     * Builder which sets the disk spool size, which is used to buffer writes to the DiskStore.
     * If not set it defaults to {@link #DEFAULT_SPOOL_BUFFER_SIZE}
     *
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
     *
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
     *
     * @param maxElementsOnDisk the maximum number of Elements to allow on the disk. 0 means unlimited.
     */
    public void setMaxElementsOnDisk(int maxElementsOnDisk) {
        if (onDiskPoolUsage != null && onDiskPoolUsage != PoolUsage.None) {
            throw new InvalidConfigurationException("MaxEntriesLocalDisk is not compatible with " +
                                                    "MaxBytesLocalDisk set on cache");
        }
        verifyGreaterThanOrEqualToZero((long) maxElementsOnDisk, "maxElementsOnDisk");
        checkDynamicChange();
        int oldCapacity = this.maxElementsOnDisk;
        this.maxElementsOnDisk = maxElementsOnDisk;
        fireDiskCapacityChanged(oldCapacity, this.maxElementsOnDisk);
    }

    /**
     * Sets the maximum number elements on Disk. 0 means unlimited.
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     *
     * @param maxEntriesLocalDisk the maximum number of Elements to allow on the disk. 0 means unlimited.
     */
    public void setMaxEntriesLocalDisk(long maxEntriesLocalDisk) {
        verifyGreaterThanOrEqualToZero(maxEntriesLocalDisk, "maxEntriesLocalDisk");
        if (maxEntriesLocalDisk > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Values greater than Integer.MAX_VALUE are not currently supported.");
        }
        // This check against pool usage is only there to see if this configuration backs up a running cache
        if (onDiskPoolUsage != null && isTerracottaClustered()) {
            throw new IllegalStateException("Can't use local disks with Terracotta clustered caches!");
        }
        maxEntriesLocalDiskExplicitlySet = true;
        setMaxElementsOnDisk((int)maxEntriesLocalDisk);
    }

    /**
     * Builder which sets the maximum number elements on Disk. 0 means unlimited.
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     *
     * @param maxElementsOnDisk the maximum number of Elements to allow on the disk. 0 means unlimited.
     * @return this configuration instance
     * @see #setMaxElementsOnDisk(int)
     */
    public final CacheConfiguration maxElementsOnDisk(int maxElementsOnDisk) {
        setMaxElementsOnDisk(maxElementsOnDisk);
        return this;
    }

    /**
     * Builder which sets the maximum number elements on Disk. 0 means unlimited.
     * <p/>
     * This property can be modified dynamically while the cache is operating.
     *
     * @param maxElementsOnDisk the maximum number of Elements to allow on the disk. 0 means unlimited.
     * @return this configuration instance
     * @see #setMaxElementsOnDisk(int)
     */
    public final CacheConfiguration maxEntriesLocalDisk(int maxElementsOnDisk) {
        setMaxEntriesLocalDisk(maxElementsOnDisk);
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
     *
     * @return this configuration instance
     * @see #setDiskExpiryThreadIntervalSeconds(long)
     */
    public final CacheConfiguration diskExpiryThreadIntervalSeconds(long diskExpiryThreadIntervalSeconds) {
        setDiskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds);
        return this;
    }

    /**
     * Freeze this configuration. Any subsequent changes will throw a CacheException
     */
    public void freezeConfiguration() {
        frozen = true;
        if (searchable != null) {
            searchable.freezeConfiguration();
        }
    }

    /**
     * @return true is this configuration is frozen - it cannot be changed dynamically.
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Getter to the configured ReadWriteCopyStrategy.
     * This will always return the same unique instance per cache
     *
     * @return the {@link ReadWriteCopyStrategy} for instance for this cache
     */
    public ReadWriteCopyStrategy<Element> getCopyStrategy() {
        // todo really make this pluggable through config!
        return copyStrategyConfiguration.getCopyStrategyInstance();
    }

    /**
     * Whether the Cache should copy elements it returns
     *
     * @param copyOnRead true, if copyOnRead
     */
    public CacheConfiguration copyOnRead(boolean copyOnRead) {
        this.setCopyOnRead(copyOnRead);
        return this;
    }

    /**
     * Whether the Cache should copy elements it returns
     *
     * @return true, is copyOnRead
     */
    public boolean isCopyOnRead() {
        validateTransactionalSettings();
        return copyOnRead;
    }

    /**
     * Whether the Cache should copy elements it returns
     *
     * @param copyOnRead true, if copyOnRead
     */
    public void setCopyOnRead(final boolean copyOnRead) {
        this.copyOnRead = copyOnRead;
    }

    /**
     * Whether the Cache should copy elements it gets
     *
     * @param copyOnWrite true, if copyOnWrite
     */
    public CacheConfiguration copyOnWrite(boolean copyOnWrite) {
        this.copyOnWrite = copyOnWrite;
        return this;
    }

    /**
     * Whether the Cache should copy elements it gets
     *
     * @return true, if copyOnWrite
     */
    public boolean isCopyOnWrite() {
        validateTransactionalSettings();
        return copyOnWrite;
    }

    /**
     * Whether the Cache should copy elements it gets
     *
     * @param copyOnWrite true, if copyOnWrite
     */
    public void setCopyOnWrite(final boolean copyOnWrite) {
        this.copyOnWrite = copyOnWrite;
    }

    /**
     * Sets the CopyStrategyConfiguration for this cache
     *
     * @param copyStrategyConfiguration the CopyStrategy Configuration
     */
    public void addCopyStrategy(CopyStrategyConfiguration copyStrategyConfiguration) {
        this.copyStrategyConfiguration = copyStrategyConfiguration;
    }

    /**
     * Sets the ElementValueComparatorConfiguration for this cache
     *
     * @param elementValueComparatorConfiguration the ElementComparator Configuration
     */
    public void addElementValueComparator(ElementValueComparatorConfiguration elementValueComparatorConfiguration) {
        this.elementValueComparatorConfiguration = elementValueComparatorConfiguration;
    }

    /**
     * Add configuration to make this cache searchable
     *
     * @param searchable search config to add
     */
    public final void addSearchable(Searchable searchable) {
        checkDynamicChange();
        this.searchable = searchable;
    }

    /**
     * The maximum amount of bytes the cache should occupy on heap
     * @return value in bytes, 0 if none set
     */
    public long getMaxBytesLocalHeap() {
        return maxBytesLocalHeap == null ? DEFAULT_MAX_BYTES_ON_HEAP : maxBytesLocalHeap;
    }

    /**
     * Setter for maxBytesLocalHeap as a String. Value can have a one char unit suffix or be a percentage (ending in %)
     * @param maxBytesHeap String representation of the size, can be relative (in %)
     */
    public void setMaxBytesLocalHeap(final String maxBytesHeap) {
        assertArgumentNotNull("Cache maxBytesLocalHeap", maxBytesHeap);
        if (isPercentage(maxBytesHeap)) {
            maxBytesLocalHeapPercentage = parsePercentage(maxBytesHeap);
        } else {
            setMaxBytesLocalHeap(MemoryUnit.parseSizeInBytes(maxBytesHeap));
        }
        maxBytesLocalHeapInput = maxBytesHeap;
    }

    /**
     * Setter for maxBytesLocalHeap in bytes
     * @param maxBytesHeap max bytes in heap in bytes
     */
    public void setMaxBytesLocalHeap(final Long maxBytesHeap) {
        if (onHeapPoolUsage != null && getMaxEntriesLocalHeap() > 0) {
            throw new InvalidConfigurationException("MaxEntriesLocalHeap is not compatible with " +
                                                    "MaxBytesLocalHeap set on cache");
        }
        if (onHeapPoolUsage != null && onHeapPoolUsage != PoolUsage.Cache) {
            throw new IllegalStateException("A Cache can't switch memory pool!");
        }
        verifyGreaterThanZero(maxBytesHeap, "maxBytesLocalHeap");
        Long oldValue = this.maxBytesLocalHeap;
        this.maxBytesLocalHeap = maxBytesHeap;
        fireMaxBytesOnLocalHeapChanged(oldValue, maxBytesHeap);
    }

    private void fireMaxBytesOnLocalHeapChanged(final Long oldValue, final Long newValue) {
        if ((oldValue != null && !oldValue.equals(newValue)) || (newValue != null && !newValue.equals(oldValue))) {
            for (CacheConfigurationListener listener : listeners) {
                listener.maxBytesLocalHeapChanged(oldValue != null ? oldValue : 0, newValue);
            }
        }
    }

    private void fireMaxBytesOnLocalDiskChanged(final Long oldValue, final Long newValue) {
        if ((oldValue != null && !oldValue.equals(newValue)) || (newValue != null && !newValue.equals(oldValue))) {
            for (CacheConfigurationListener listener : listeners) {
                listener.maxBytesLocalDiskChanged(oldValue != null ? oldValue : 0, newValue);
            }
        }
    }

    /**
     * Sets the maxOnHeap size
     * @param amount the amount of unit
     * @param memoryUnit the actual unit
     * @return this
     */

    public CacheConfiguration maxBytesLocalHeap(final long amount, final MemoryUnit memoryUnit) {
        setMaxBytesLocalHeap(memoryUnit.toBytes(amount));
        return this;
    }

    /**
     * The maximum amount of bytes the cache should occupy off heap
     * @return value in bytes, 0 if none set
     */
    public long getMaxBytesLocalOffHeap() {
        return maxBytesLocalOffHeap == null ? DEFAULT_MAX_BYTES_OFF_HEAP : maxBytesLocalOffHeap;
    }

    /**
     * The string form of the maximum amount of bytes the cache should occupy off heap
     * @return value as string in bytes
     */
    public String getMaxBytesLocalOffHeapAsString() {
        return maxBytesLocalOffHeapInput != null ? maxBytesLocalOffHeapInput : NumberFormat.getNumberInstance().format(getMaxBytesLocalOffHeap());
    }

    /**
     * Setter for maximum bytes off heap as a String. Value can have a one char unit suffix or be a percentage (ending in %)
     * @param maxBytesOffHeap String representation of the size, can be relative (in %)
     */
    public void setMaxBytesLocalOffHeap(final String maxBytesOffHeap) {
        assertArgumentNotNull("Cache maxBytesLocalOffHeap", maxBytesOffHeap);
        if (isPercentage(maxBytesOffHeap)) {
            maxBytesLocalOffHeapPercentage = parsePercentage(maxBytesOffHeap);
        } else {
            setMaxBytesLocalOffHeap(MemoryUnit.parseSizeInBytes(maxBytesOffHeap));
        }
        maxBytesLocalOffHeapInput = maxBytesOffHeap;
        maxBytesLocalOffHeapExplicitlySet = true;
    }

    /**
     * Getter for maximum bytes off heap expressed as a percentage
     * @return percentage (between 0 and 100)
     */
    public Integer getMaxBytesLocalOffHeapPercentage() {
        return maxBytesLocalOffHeapPercentage;
    }

    /**
     * Getter for maximum bytes on heap expressed as a percentage
     * @return percentage (between 0 and 100)
     */
    public Integer getMaxBytesLocalHeapPercentage() {
        return maxBytesLocalHeapPercentage;
    }

    /**
     * The string form of the maximum amount of bytes the cache should occupy on heap
     * @return value as string in bytes
     */
    public String getMaxBytesLocalHeapAsString() {
        return maxBytesLocalHeapInput != null ? maxBytesLocalHeapInput : NumberFormat.getNumberInstance().format(getMaxBytesLocalHeap());
    }

    /**
     * Getter for maximum bytes on disk expressed as a percentage
     * @return percentage (between 0 and 100)
     */
    public Integer getMaxBytesLocalDiskPercentage() {
        return maxBytesLocalDiskPercentage;
    }

    private int parsePercentage(final String stringValue) {
        String trimmed = stringValue.trim();
        int percentage = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
        if (percentage > HUNDRED_PERCENT || percentage < 0) {
            throw new IllegalArgumentException("Percentage need values need to be between 0 and 100 inclusive, but got : " + percentage);
        }
        return percentage;
    }

    private boolean isPercentage(final String stringValue) {
        String trimmed = stringValue.trim();
        return trimmed.charAt(trimmed.length() - 1) == '%';
    }

    /**
     * Sets the maximum amount of bytes the cache being configured will use on the OffHeap tier
     * @param maxBytesOffHeap max bytes on disk in bytes
     */
    public void setMaxBytesLocalOffHeap(final Long maxBytesOffHeap) {
        if (offHeapPoolUsage != null) {
            throw new IllegalStateException("OffHeap can't be set dynamically!");
        }
        verifyGreaterThanZero(maxBytesOffHeap, "maxBytesLocalOffHeap");
        this.maxBytesLocalOffHeapExplicitlySet = true;
        this.maxBytesLocalOffHeap = maxBytesOffHeap;
    }

    /**
     * Sets the maxOffHeap tier size
     * @param amount the amount of unit
     * @param memoryUnit the actual unit
     * @return this
     */
    public CacheConfiguration maxBytesLocalOffHeap(final long amount, final MemoryUnit memoryUnit) {
        setMaxBytesLocalOffHeap(memoryUnit.toBytes(amount));
        return this;
    }

    /**
     * The maximum amount of bytes the cache should occupy on disk
     * @return value in bytes, 0 if none set
     */
    public long getMaxBytesLocalDisk() {
        return maxBytesLocalDisk == null ? DEFAULT_MAX_BYTES_ON_DISK : maxBytesLocalDisk;
    }

    /**
     * The string form of the maximum amount of bytes the cache should occupy on disk
     * @return value as string in bytes
     */
    public String getMaxBytesLocalDiskAsString() {
        return maxBytesLocalDiskInput != null ? maxBytesLocalDiskInput : NumberFormat.getNumberInstance().format(getMaxBytesLocalDisk());
    }

    /**
     * Setter for maxBytesOnDisk as a String. Value can have a one char unit suffix or be a percentage (ending in %)
     * @param maxBytesDisk String representation of the size, can be relative (in %)
     */
    public void setMaxBytesLocalDisk(final String maxBytesDisk) {
        assertArgumentNotNull("Cache maxBytesLocalDisk", maxBytesDisk);
        if (isPercentage(maxBytesDisk)) {
            maxBytesLocalDiskPercentage = parsePercentage(maxBytesDisk);
        } else {
            setMaxBytesLocalDisk(MemoryUnit.parseSizeInBytes(maxBytesDisk));
        }
        maxBytesLocalDiskExplicitlySet = true;
        maxBytesLocalDiskInput = maxBytesDisk;
    }

    /**
     * Sets the maximum amount of bytes the cache being configured will use on the OnDisk tier
     * @param maxBytesDisk max bytes on disk in bytes
     */
    public void setMaxBytesLocalDisk(final Long maxBytesDisk) {
        if (onDiskPoolUsage != null && getMaxEntriesLocalDisk() > 0) {
            throw new InvalidConfigurationException("MaxEntriesLocalDisk is not compatible with " +
                                                    "MaxBytesLocalDisk set on cache");
        }
        if (onDiskPoolUsage != null && onDiskPoolUsage != PoolUsage.Cache) {
            throw new IllegalStateException("A Cache can't switch disk pool!");
        }
        verifyGreaterThanZero(maxBytesDisk, "maxBytesLocalDisk");
        maxBytesLocalDiskExplicitlySet = true;
        Long oldValue = this.maxBytesLocalDisk;
        this.maxBytesLocalDisk = maxBytesDisk;
        fireMaxBytesOnLocalDiskChanged(oldValue, maxBytesDisk);
    }

    /**
     * Sets the maxOnDisk size
     * @param amount the amount of unit
     * @param memoryUnit the actual unit
     * @return this
     */
    public CacheConfiguration maxBytesLocalDisk(final long amount, final MemoryUnit memoryUnit) {
        setMaxBytesLocalDisk(memoryUnit.toBytes(amount));
        return this;
    }

    private void verifyGreaterThanZero(final Long fieldVal, final String fieldName) {
        if (fieldVal != null && fieldVal < 1) {
            throw new IllegalArgumentException("Illegal value " + fieldVal + " for " + fieldName + ": has to be larger than 0");
        }
    }

    private void verifyGreaterThanOrEqualToZero(final Long fieldVal, final String fieldName) {
        if (fieldVal != null && fieldVal < 0) {
            throw new IllegalArgumentException("Illegal value " + fieldVal + " for " + fieldName + ": has to be larger than or equal to 0");
        }
    }

    /**
     * Returns the copyStrategyConfiguration
     *
     * @return the copyStrategyConfiguration
     */
    public CopyStrategyConfiguration getCopyStrategyConfiguration() {
        return this.copyStrategyConfiguration;
    }

    /**
     * Returns the elementComparatorConfiguration
     *
     * @return the elementComparatorConfiguration
     */
    public ElementValueComparatorConfiguration getElementValueComparatorConfiguration() {
        return elementValueComparatorConfiguration;
    }

    /**
     * Checks whether the user explicitly set the maxBytesOnHeapPercentage
     * @return true if set by user, false otherwise
     * @see #setMaxBytesLocalHeap(String)
     */
    public boolean isMaxBytesLocalHeapPercentageSet() {
        return maxBytesLocalHeapPercentage != null;
    }

    /**
     * Checks whether the user explicitly set the maxBytesOffHeapPercentage
     * @return true if set by user, false otherwise
     * @see #setMaxBytesLocalOffHeap(String)
     */
    public boolean isMaxBytesLocalOffHeapPercentageSet() {
        return maxBytesLocalOffHeapPercentage != null;
    }

    /**
     * Checks whether the user explicitly set the maxBytesOnDiskPercentage
     * @return true if set by user, false otherwise
     * @see #setMaxBytesLocalDisk(String)
     */
    public boolean isMaxBytesLocalDiskPercentageSet() {
        return maxBytesLocalDiskPercentage != null;
    }

    /**
     * Sets up the CacheConfiguration for runtime consumption, also registers this cache configuration with the cache manager's configuration
     * @param cacheManager The CacheManager as part of which the cache is being setup
     */
    public void setupFor(final CacheManager cacheManager) {
        setupFor(cacheManager, true);
    }

    /**
     * Sets up the CacheConfiguration for runtime consumption
     * @param cacheManager The CacheManager as part of which the cache is being setup
     * @param register true to register this cache configuration with the cache manager.
     */
    public void setupFor(final CacheManager cacheManager, final boolean register) {
        final Collection<ConfigError> errors = validate(cacheManager.getConfiguration());
        configCachePools(cacheManager.getConfiguration());
        errors.addAll(verifyPoolAllocationsBeforeAddingTo(cacheManager,
            cacheManager.getConfiguration().getMaxBytesLocalHeap(),
            cacheManager.getConfiguration().getMaxBytesLocalOffHeap(),
            cacheManager.getConfiguration().getMaxBytesLocalDisk()));
        if (!errors.isEmpty()) {
            throw new InvalidConfigurationException(errors);
        }


        if (!isTerracottaClustered()) {
            updateCacheManagerPoolSizes(cacheManager);
        }

        if (register) {
            registerCacheConfiguration(cacheManager);
        }
        if (cacheManager.getConfiguration().isMaxBytesLocalHeapSet() || cacheManager.getConfiguration().isMaxBytesLocalDiskSet()) {
            addConfigurationListener(new AbstractCacheConfigurationListener() {
                @Override
                public void maxBytesLocalHeapChanged(final long oldValue, final long newValue) {
                    if (getMaxBytesLocalHeap() > 0
                       && cacheManager.getConfiguration().getCacheConfigurations().keySet().contains(getName())
                       && cacheManager.getConfiguration().isMaxBytesLocalHeapSet()) {
                        long previous = cacheManager.getOnHeapPool().getMaxSize();
                        cacheManager.getOnHeapPool().setMaxSize(previous + oldValue - newValue);
                    }
                }

                @Override
                public void maxBytesLocalDiskChanged(final long oldValue, final long newValue) {
                    if (getMaxBytesLocalDisk() > 0
                       && cacheManager.getConfiguration().getCacheConfigurations().keySet().contains(getName())
                       && cacheManager.getConfiguration().isMaxBytesLocalDiskSet()) {
                        long previous = cacheManager.getOnDiskPool().getMaxSize();
                        cacheManager.getOnDiskPool().setMaxSize(previous + oldValue - newValue);
                    }
                }
            });
        }

        if (overflowToOffHeap == null && (cacheManager.getConfiguration().isMaxBytesLocalOffHeapSet() || getMaxBytesLocalOffHeap() > 0)) {
            overflowToOffHeap = true;
        }
        if (overflowToDisk == null && cacheManager.getConfiguration().isMaxBytesLocalDiskSet() || getMaxBytesLocalDisk() > 0) {
            overflowToDisk = true;
        }
        warnMaxEntriesLocalHeap(register, cacheManager);
        warnMaxEntriesForOverflowToOffHeap(register);
        warnSizeOfPolicyConfiguration();
        freezePoolUsages(cacheManager);
    }

    private void warnMaxEntriesForOverflowToOffHeap(final boolean register) {
        if (overflowToOffHeap != null && overflowToOffHeap && register) {
            if (getMaxEntriesLocalHeap() > 0 && getMaxEntriesLocalHeap() < MINIMUM_RECOMMENDED_IN_MEMORY) {
                LOG.warn("The " + getName() + " cache is configured for off-heap and has a maxEntriesLocalHeap/maxElementsInMemory of "
                        + getMaxEntriesLocalHeap() + ".  It is recommended to set maxEntriesLocalHeap/maxElementsInMemory to at least "
                        + MINIMUM_RECOMMENDED_IN_MEMORY + " elements when using an off-heap store, otherwise performance "
                        + "will be seriously degraded.");
              }
        }
    }

    private void warnMaxEntriesLocalHeap(final boolean register, CacheManager cacheManager) {
        if (getMaxEntriesLocalHeap() == 0 && register) {
            if (getMaxBytesLocalHeap() == 0 && (!cacheManager.getConfiguration().isMaxBytesLocalHeapSet())) {
            LOG.warn("Cache: " + getName() +
                    " has a maxElementsInMemory of 0. This might lead to performance degradation or OutOfMemoryError at Terracotta client." +
                    "From Ehcache 2.0 onwards this has been changed to mean a store" +
                    " with no capacity limit. Set it to 1 if you want" +
                    " no elements cached in memory");
            }
        }
    }

    private void warnSizeOfPolicyConfiguration() {
        if (isTerracottaClustered() && getSizeOfPolicyConfiguration() != null) {
            LOG.warn("Terracotta clustered cache: " + getName() + " has a sizeOf policy configuration specificed. " +
                    "SizeOfPolicyConfiguration is unsupported for Terracotta clustered caches.");
        }
    }

    private void freezePoolUsages(final CacheManager cacheManager) {
        if (getMaxBytesLocalHeap() > 0) {
            onHeapPoolUsage = CacheConfiguration.PoolUsage.Cache;
        } else if (cacheManager.getConfiguration().isMaxBytesLocalHeapSet()) {
            onHeapPoolUsage = CacheConfiguration.PoolUsage.CacheManager;
        } else {
            onHeapPoolUsage = CacheConfiguration.PoolUsage.None;
        }

        if (getMaxBytesLocalOffHeap() > 0) {
            offHeapPoolUsage = CacheConfiguration.PoolUsage.Cache;
        } else if (cacheManager.getConfiguration().isMaxBytesLocalOffHeapSet()) {
            offHeapPoolUsage = CacheConfiguration.PoolUsage.CacheManager;
        } else {
            offHeapPoolUsage = CacheConfiguration.PoolUsage.None;
        }

        if (isTerracottaClustered()) {
            onDiskPoolUsage = CacheConfiguration.PoolUsage.None;
        } else {
            if (getMaxBytesLocalDisk() > 0) {
                onDiskPoolUsage = CacheConfiguration.PoolUsage.Cache;
            } else if (cacheManager.getConfiguration().isMaxBytesLocalDiskSet()) {
                onDiskPoolUsage = CacheConfiguration.PoolUsage.CacheManager;
            } else {
                onDiskPoolUsage = CacheConfiguration.PoolUsage.None;
            }
        }
    }

    private void registerCacheConfiguration(final CacheManager cacheManager) {
        Map<String, CacheConfiguration> configMap = cacheManager.getConfiguration().getCacheConfigurations();
        if (!configMap.containsKey(getName())) {
            cacheManager.getConfiguration().addCache(this, false);
        }
    }

    private void updateCacheManagerPoolSizes(final CacheManager cacheManager) {
        if (cacheManager.getOnHeapPool() != null) {
            cacheManager.getOnHeapPool().setMaxSize(cacheManager.getOnHeapPool().getMaxSize() - getMaxBytesLocalHeap());
        }
        if (cacheManager.getOnDiskPool() != null) {
            cacheManager.getOnDiskPool().setMaxSize(cacheManager.getOnDiskPool().getMaxSize() - getMaxBytesLocalDisk());
        }
    }

    /**
     * Will verify that we don't overallocate pools
     * @param cacheManager The cacheManager that will manage the cache
     * @param managerMaxBytesLocalHeap bytes for local heap
     * @param managerMaxBytesLocalOffHeap bytes for local offheap
     * @param managerMaxBytesLocalDisk bytes for local disk
     * @return a list with potential errors
     */
    List<ConfigError> verifyPoolAllocationsBeforeAddingTo(CacheManager cacheManager,
                                                     long managerMaxBytesLocalHeap,
                                                     long managerMaxBytesLocalOffHeap,
                                                     long managerMaxBytesLocalDisk) {
        final List<ConfigError> configErrors = new ArrayList<ConfigError>();

        long totalOnHeapAssignedMemory  = 0;
        long totalOffHeapAssignedMemory = 0;
        long totalOnDiskAssignedMemory  = 0;

        boolean isUpdate = false;
        for (Cache cache : getAllActiveCaches(cacheManager)) {
            isUpdate = cache.getName().equals(getName()) || isUpdate;
            final CacheConfiguration config = cache.getCacheConfiguration();
            totalOnHeapAssignedMemory += config.getMaxBytesLocalHeap();
            totalOffHeapAssignedMemory += config.getMaxBytesLocalOffHeap();
            totalOnDiskAssignedMemory += config.getMaxBytesLocalDisk();
        }

        if (!isUpdate) {
            totalOnHeapAssignedMemory += getMaxBytesLocalHeap();
            totalOffHeapAssignedMemory += getMaxBytesLocalOffHeap();
            totalOnDiskAssignedMemory += getMaxBytesLocalDisk();
        }

        verifyLocalHeap(managerMaxBytesLocalHeap, configErrors, totalOnHeapAssignedMemory);
        verifyLocalOffHeap(managerMaxBytesLocalOffHeap, configErrors, totalOffHeapAssignedMemory);
        verifyLocalDisk(managerMaxBytesLocalDisk, configErrors, totalOnDiskAssignedMemory);

        if (managerMaxBytesLocalHeap > 0 && managerMaxBytesLocalHeap - totalOnHeapAssignedMemory == 0) {
            LOG.warn("All the onHeap memory has been assigned, there is none left for dynamically added caches");
        }

        if (Runtime.getRuntime().maxMemory() - totalOnHeapAssignedMemory < 0) {
            // todo this could be a nicer message (with actual values)
            configErrors.add(new ConfigError("You've assigned more memory to the on-heap than the VM can sustain, " +
                                            "please adjust your -Xmx setting accordingly"));
        }

        if (totalOnHeapAssignedMemory / (float) Runtime.getRuntime().maxMemory() > CacheManager.ON_HEAP_THRESHOLD) {
            LOG.warn("You've assigned over 80% of your VM's heap to be used by the cache!");
        }

        return configErrors;
    }

    private void verifyLocalDisk(final long managerMaxBytesLocalDisk,
                                 final List<ConfigError> configErrors, final long totalOnDiskAssignedMemory) {
        if ((isMaxBytesLocalDiskPercentageSet() || getMaxBytesLocalDisk() > 0)
            && managerMaxBytesLocalDisk > 0 && managerMaxBytesLocalDisk - totalOnDiskAssignedMemory < 0) {
            configErrors.add(new ConfigError("Cache '" + getName()
                                                    + "' over-allocates CacheManager's localOnDisk limit!"));
        }
    }

    private void verifyLocalOffHeap(final long managerMaxBytesLocalOffHeap,
                                    final List<ConfigError> configErrors, final long totalOffHeapAssignedMemory) {
        if ((isMaxBytesLocalOffHeapPercentageSet() || getMaxBytesLocalOffHeap() > 0)
            && managerMaxBytesLocalOffHeap > 0 && managerMaxBytesLocalOffHeap - totalOffHeapAssignedMemory < 0) {
            configErrors.add(new ConfigError("Cache '" + getName()
                                            + "' over-allocates CacheManager's localOffHeap limit!"));
        }
    }

    private void verifyLocalHeap(final long managerMaxBytesLocalHeap,
                                 final List<ConfigError> configErrors, final long totalOnHeapAssignedMemory) {
        if ((isMaxBytesLocalHeapPercentageSet() || getMaxBytesLocalHeap() > 0)
            && managerMaxBytesLocalHeap > 0 && managerMaxBytesLocalHeap - totalOnHeapAssignedMemory < 0) {
            configErrors.add(new ConfigError("Cache '" + getName()
                                                    + "' over-allocates CacheManager's localOnHeap limit!"));
        }
    }

    /**
     * Configures the cache pools
     * @param configuration the Configuration of the CacheManager managing this cache
     */
    void configCachePools(Configuration configuration) {

        long cacheAssignedMem;
        if (getMaxBytesLocalHeapPercentage() != null) {
            cacheAssignedMem = configuration.getMaxBytesLocalHeap() * getMaxBytesLocalHeapPercentage() / HUNDRED_PERCENT;
            setMaxBytesLocalHeap(cacheAssignedMem);
        }

        if (offHeapPoolUsage == null && getMaxBytesLocalOffHeapPercentage() != null) {
            cacheAssignedMem = configuration.getMaxBytesLocalOffHeap() * getMaxBytesLocalOffHeapPercentage() / HUNDRED_PERCENT;
            setMaxBytesLocalOffHeap(cacheAssignedMem);
        }

        if (getMaxBytesLocalDiskPercentage() != null) {
            cacheAssignedMem = configuration.getMaxBytesLocalDisk() * getMaxBytesLocalDiskPercentage() / HUNDRED_PERCENT;
            setMaxBytesLocalDisk(cacheAssignedMem);
        }

    }

    /**
     * Validates the configuration
     * @param configuration the CacheManager configuration this is going to be used with
     * @return the errors in the config
     */
    public Collection<ConfigError> validate(final Configuration configuration) {

        final Collection<ConfigError> errors = new ArrayList<ConfigError>();

        verifyClusteredCacheConfiguration(configuration, errors);

        if (maxEntriesLocalHeap == null && !configuration.isMaxBytesLocalHeapSet() && maxBytesLocalHeap == null) {
            errors.add(new CacheConfigError("If your CacheManager has no maxBytesLocalHeap set, you need to either set " +
                    "maxEntriesLocalHeap or maxBytesLocalHeap at the Cache level", getName()));
        }

        if (configuration.isMaxBytesLocalHeapSet() && Runtime.getRuntime().maxMemory() - configuration.getMaxBytesLocalHeap() < 0) {
            errors.add(new ConfigError("You've assigned more memory to the on-heap than the VM can sustain, " +
                                                    "please adjust your -Xmx setting accordingly"));
        }

        //commenting this check until fixed for cachemanger is fixed
        //if (isOverflowToOffHeapSet() && !maxBytesLocalOffHeapExplicitlySet) {
        //    errors.add(new CacheConfigError("\"overFlowToOffHeap\" is set, but \"maxBytesLocalOffHeap\" is not set.", getName()));
        //}

        errors.addAll(validateCachePools(configuration));

        return errors;
    }

    private void verifyClusteredCacheConfiguration(final Configuration configuration, final Collection<ConfigError> errors) {
        if (!isTerracottaClustered()) { return; }

        if (getPinningConfiguration() != null && getPinningConfiguration().getStore() == PinningConfiguration.Store.INCACHE
                && getMaxElementsOnDisk() != 0) {
            errors.add(new CacheConfigError("maxElementsOnDisk may not be used on a pinned cache.", getName()));
        }

        if (maxEntriesLocalDiskExplicitlySet) {
            errors.add(new CacheConfigError("You can't set maxEntriesLocalDisk when clustering your cache with Terracotta, " +
                    "local disks won't be used! To control elements going in the cache cluster wide, " +
                    "use maxElementsOnDisk instead", getName()));
        }


        if (maxBytesLocalDiskExplicitlySet) {
            errors.add(new CacheConfigError("You can't set maxBytesLocalDisk when clustering your cache with Terracotta",
                    getName()));
        }

        validateTerracottaConfig(configuration, errors);
    }

    /**
     * Validates the CacheConfiguration against the CacheManager's Configuration
     * @param configuration The CacheManager Configuration
     * @return the list of errors encountered, or an empty list
     */
    List<CacheConfigError> validateCachePools(final Configuration configuration) {
        List<CacheConfigError> errors = new ArrayList<CacheConfigError>();

        if (configuration.isMaxBytesLocalHeapSet()
            && getMaxEntriesLocalHeap() > 0) {
            errors.add(new CacheConfigError("MaxElementsInMemory is not compatible with " +
                                            "MaxBytesLocalHeap set on cache manager", getName()));
        }
        if (getMaxBytesLocalHeap() > 0 && getMaxEntriesLocalHeap() > 0) {
            errors.add(new CacheConfigError("MaxElementsInMemory is not compatible with " +
                                            "MaxBytesLocalHeap set on cache", getName()));
        }
        if (isMaxBytesLocalHeapPercentageSet() && !configuration.isMaxBytesLocalHeapSet()) {
            errors.add(new CacheConfigError("Defines a percentage maxBytesOnHeap value but no CacheManager " +
                                            "wide value was configured", getName()));
        }
        if (isMaxBytesLocalOffHeapPercentageSet() && !configuration.isMaxBytesLocalOffHeapSet()) {
            errors.add(new CacheConfigError("Defines a percentage maxBytesOffHeap value but no CacheManager " +
                                            "wide value was configured", getName()));
        }
        if (isMaxBytesLocalDiskPercentageSet() && !configuration.isMaxBytesLocalDiskSet()) {
            errors.add(new CacheConfigError("Defines a percentage maxBytesOnDisk value but no CacheManager " +
                                            "wide value was configured", getName()));
        }
        return errors;
    }

    private void validateTerracottaConfig(final Configuration configuration, final Collection<ConfigError> errors) {
        final TerracottaClientConfiguration clientConfiguration = configuration.getTerracottaConfiguration();
        if (getTerracottaConfiguration().getStorageStrategy().equals(StorageStrategy.CLASSIC)) {
            if (getTerracottaConfiguration().isNonstopEnabled()) {
                errors.add(new CacheConfigError("NONSTOP can't be enabled with " + StorageStrategy.CLASSIC
                    .name() + " strategy.", getName()));
            }

            if (clientConfiguration != null && clientConfiguration.isRejoin()) {
                errors.add(new CacheConfigError("REJOIN can't be enabled with " + StorageStrategy.CLASSIC
                    .name() + " strategy.", getName()));
            }

            if (getTerracottaConsistency().equals(Consistency.EVENTUAL)) {
                errors.add(new CacheConfigError(Consistency.EVENTUAL
                                                    .name() + " consistency can't be enabled with " + StorageStrategy.CLASSIC.name()
                                                + " strategy.", getName()));
            }
        }

        if (clientConfiguration != null && clientConfiguration.isRejoin() && !getTerracottaConfiguration().isNonstopEnabled()) {
            errors.add(new CacheConfigError("Terracotta clustered caches must be nonstop when rejoin is enabled.", getName()));
        }
    }

    /**
     * Whether this cache is Count based
     * @return true if maxEntries set, false otherwise
     */
    public boolean isCountBasedTuned() {
        return (maxEntriesLocalHeap != null && maxEntriesLocalHeap > 0) || maxElementsOnDisk > 0;
    }

    /**
     * Checks whether the overflowing to off heap behavior was explicitly set
     * @return true if explicitly set, false otherwise
     */
    public boolean isOverflowToOffHeapSet() {
        return overflowToOffHeap != null;
    }


    /**
     * Configuration for the CacheEventListenerFactory.
     */
    public static final class CacheEventListenerFactoryConfiguration extends FactoryConfiguration<CacheEventListenerFactoryConfiguration> {
        private NotificationScope notificationScope = NotificationScope.ALL;

        /**
         * Used by BeanHandler to set the mode during parsing. Convert listenFor string to uppercase and
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
     * Allows BeanHandler to add the PinningConfiguration to the configuration.
     */
    public final void addPinning(PinningConfiguration pinningConfiguration) {
        this.pinningConfiguration = pinningConfiguration;
        validateConfiguration();
    }

    /**
     * @return this configuration instance
     * @see #addPinning(PinningConfiguration)
     */
    public final CacheConfiguration pinning(PinningConfiguration pinningConfiguration) {
        addPinning(pinningConfiguration);
        return this;
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
     * @see #addSearchable(Searchable)
     * @param searchable
     * @return this
     */
    public final CacheConfiguration searchable(Searchable searchable) {
        addSearchable(searchable);
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
     * @param transactionalMode one of OFF, LOCAL, XA, XA_STRICT
     */
    public final void setTransactionalMode(final String transactionalMode) {
        assertArgumentNotNull("Cache transactionalMode", transactionalMode);
        transactionalMode(TransactionalMode.valueOf(transactionalMode.toUpperCase()));
    }

    /**
     * Builder which sets the transactionalMode
     *
     * @param transactionalMode one of OFF, LOCAL, XA, XA_STRICT
     * @return this configuration instance
     * @see #setTransactionalMode(String)
     */
    public final CacheConfiguration transactionalMode(String transactionalMode) {
        setTransactionalMode(transactionalMode);
        return this;
    }

    /**
     * Builder which sets the transactionalMode
     *
     * @param transactionalMode one of OFF, LOCAL, XA, XA_STRICT
     * @return this configuration instance
     * @see #setTransactionalMode(String)
     */
    public final CacheConfiguration transactionalMode(TransactionalMode transactionalMode) {
        if (transactionalMode == null) {
            throw new IllegalArgumentException("TransactionalMode value must be non-null");
        }
        if (this.transactionalMode != null) {
            throw new InvalidConfigurationException("transactionalMode cannot be changed once set");
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

    /**
     * Used to validate what should be a complete Cache Configuration.
     */
    public void validateCompleteConfiguration() {

        validateConfiguration();

        // Extra checks that a completed cache config should have

        if (name == null) {
            throw new InvalidConfigurationException("Caches must be named.");
        }
    }

    /**
     * Used to validate a Cache Configuration.
     */
    public void validateConfiguration() {
        if (terracottaConfiguration != null && terracottaConfiguration.isClustered()) {
            if (overflowToDisk != null && overflowToDisk) {
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
        boolean transactional = getTransactionalMode().isTransactional();
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
     *
     * @deprecated use {@link #getMaxEntriesLocalHeap()}
     */
    @Deprecated
    public int getMaxElementsInMemory() {
        return (int)getMaxEntriesLocalHeap();
    }

    /**
     * Accessor
     */
    public long getCacheLoaderTimeoutMillis() {
        return cacheLoaderTimeoutMillis;
    }

    /**
     * Accessor
     *
     */
    public int getMaxElementsOnDisk() {
        return maxElementsOnDisk;
    }

    /**
     * Configured maximum number of entries for the local disk store.
     */
    public long getMaxEntriesLocalDisk() {
        return maxElementsOnDisk;
    }

    /**
     * Configured maximum number of entries for the local memory heap.
     */
    public long getMaxEntriesLocalHeap() {
        return maxEntriesLocalHeap == null ? 0 : maxEntriesLocalHeap;
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
        return overflowToDisk == null ? false : overflowToDisk;
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
    public boolean isSearchable() {
        return searchable != null;
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
     *
     * @return true if offheap store is enabled, otherwise false.
     */
    public boolean isOverflowToOffHeap() {
        return overflowToOffHeap == null ? false : overflowToOffHeap;
    }

    /**
     * Accessor
     *
     * @return the SizeOfPolicy Configuration for this cache.
     */
    public SizeOfPolicyConfiguration getSizeOfPolicyConfiguration() {
        return sizeOfPolicyConfiguration;
    }

    /**
     * Accessor
     *
     * @return the persistence configuration for this cache.
     */
    public PersistenceConfiguration getPersistenceConfiguration() {
        return persistenceConfiguration;
    }

    /**
     * Accessor
     *
     * @return the max memory of the offheap store for this cache.
     */
    public String getMaxMemoryOffHeap() {
        return maxBytesLocalOffHeapInput;
    }

    /**
     * Accessor
     *
     * @return the max memory of the offheap store for this cache, in bytes.
     * @see #getMaxMemoryOffHeap()
     */
    public long getMaxMemoryOffHeapInBytes() {
        return getMaxBytesLocalOffHeap();
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
     * @return the pinning configuration
     */
    public PinningConfiguration getPinningConfiguration() {
        return pinningConfiguration;
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
     * Accessor
     *
     * @return the CoherenceMode if Terracotta-clustered or null
     */
    public Consistency getTerracottaConsistency() {
        return terracottaConfiguration != null ? terracottaConfiguration.getConsistency() : null;
    }

    /**
     * Accessor
     *
     * @return the StorageStrategy if Terracotta-clustered or null
     */
    public StorageStrategy getTerracottaStorageStrategy() {
        return terracottaConfiguration != null ? terracottaConfiguration.getStorageStrategy() : null;
    }

    /**
     * To what transactionalMode was the Cache set
     *
     * @return transactionaMode
     */
    public final TransactionalMode getTransactionalMode() {
        if (transactionalMode == null) {
            return DEFAULT_TRANSACTIONAL_MODE;
        }
        return transactionalMode;
    }

    /**
     * Helper method to compute whether the cache is XA transactional or not
     *
     * @return true if transactionalMode="xa_strict"
     */
    public boolean isXaStrictTransactional() {
        validateTransactionalSettings();
        return getTransactionalMode().equals(TransactionalMode.XA_STRICT);
    }

    /**
     * Helper method to compute whether the cache is local transactional or not
     *
     * @return true if transactionalMode="local"
     */
    public boolean isLocalTransactional() {
        validateTransactionalSettings();
        return getTransactionalMode().equals(TransactionalMode.LOCAL);
    }

    /**
     * Helper method to compute whether the cache is local_jta transactional or not
     *
     * @return true if transactionalMode="xa"
     */
    public boolean isXaTransactional() {
        validateTransactionalSettings();
        return getTransactionalMode().equals(TransactionalMode.XA);
    }

    /**
     * An enum to identify what pool a resource uses
     */
    private static enum PoolUsage {

        CacheManager(true), Cache(true), None(false);

        private final boolean usingPool;
        private PoolUsage(final boolean poolUser) {
            this.usingPool = poolUser;
        }

        public boolean isUsingPool() {
            return usingPool;
        }
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
         * Local Transactions
         */
        LOCAL(true),

        /**
         * XA Transactions
         */
        XA(true),

        /**
         * XA Strict (true 2PC) Transactions
         */
        XA_STRICT(true);

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
        this.maxEntriesLocalHeap = capacity;
    }

    /**
     * Intended for internal use only, and subject to change.
     */
    public void internalSetMemCapacityInBytes(long capacity) {
        this.maxBytesLocalHeap = capacity;
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
     * Get the defined search attributes indexed by attribute name
     *
     * @return search attributes
     */
    public Map<String, SearchAttribute> getSearchAttributes() {
        if (searchable == null) {
            return Collections.emptyMap();
        }
        return searchable.getSearchAttributes();
    }

    /**
     * Get the search configuration for this cache (if any)
     *
     * @return search config (may be null)
     */
    public Searchable getSearchable() {
        return searchable;
    }

}
