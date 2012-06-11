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

package net.sf.ehcache;

import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.bootstrap.BootstrapCacheLoaderFactory;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.ClusterSchemeNotAvailableException;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.StripedReadWriteLockSync;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.ManagementRESTServiceConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.NonstopExecutorService;
import net.sf.ehcache.constructs.nonstop.concurrency.LockOperationTimedOutNonstopException;
import net.sf.ehcache.constructs.nonstop.store.NonstopStoreImpl;
import net.sf.ehcache.constructs.nonstop.store.RejoinAwareNonstopStore;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.extension.CacheExtensionFactory;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.loader.CacheLoaderFactory;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.BoundedPool;
import net.sf.ehcache.pool.impl.FromLargestCacheOnDiskPoolEvictor;
import net.sf.ehcache.pool.impl.FromLargestCacheOnHeapPoolEvictor;
import net.sf.ehcache.pool.impl.UnboundedPool;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.statistics.CacheUsageListener;
import net.sf.ehcache.statistics.LiveCacheStatistics;
import net.sf.ehcache.statistics.LiveCacheStatisticsWrapper;
import net.sf.ehcache.statistics.sampled.CacheStatisticsSampler;
import net.sf.ehcache.statistics.sampled.SampledCacheStatistics;
import net.sf.ehcache.statistics.sampled.SampledCacheStatisticsWrapper;
import net.sf.ehcache.store.DiskBackedMemoryStore;
import net.sf.ehcache.store.ElementIdAssigningStore;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.LegacyStoreWrapper;
import net.sf.ehcache.store.LruMemoryStore;
import net.sf.ehcache.store.MemoryOnlyStore;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.StoreQuery.Ordering;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.store.compound.ImmutableValueElementCopyStrategy;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.store.disk.DiskStore;
import net.sf.ehcache.store.disk.StoreUpdateException;
import net.sf.ehcache.terracotta.InternalEhcache;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.local.JtaLocalTransactionStore;
import net.sf.ehcache.transaction.local.LocalTransactionStore;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;
import net.sf.ehcache.transaction.xa.EhcacheXAResourceImpl;
import net.sf.ehcache.transaction.xa.XATransactionStore;
import net.sf.ehcache.util.ClassLoaderUtil;
import net.sf.ehcache.util.NamedThreadFactory;
import net.sf.ehcache.util.PropertyUtil;
import net.sf.ehcache.util.TimeUtil;
import net.sf.ehcache.util.VmUtils;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterFactory;
import net.sf.ehcache.writer.CacheWriterManager;
import net.sf.ehcache.writer.CacheWriterManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Cache is the central class in ehcache. Caches have {@link Element}s and are managed
 * by the {@link CacheManager}. The Cache performs logical actions. It delegates physical
 * implementations to its {@link net.sf.ehcache.store.Store}s.
 * <p/>
 * A reference to a Cache can be obtained through the {@link CacheManager}. A Cache thus obtained
 * is guaranteed to have status {@link Status#STATUS_ALIVE}. This status is checked for any method which
 * throws {@link IllegalStateException} and the same thrown if it is not alive. This would normally
 * happen if a call is made after {@link CacheManager#shutdown} is invoked.
 * <p/>
 * Cache is threadsafe.
 * <p/>
 * Statistics on cache usage are collected and made available through the {@link #getStatistics()} methods.
 * <p/>
 * Various decorators are available for Cache, such as BlockingCache, SelfPopulatingCache and the dynamic proxy
 * ExceptionHandlingDynamicCacheProxy. See each class for details.
 *
 * @author Greg Luck
 * @author Geert Bevin
 * @version $Id$
 */
public class Cache implements InternalEhcache, StoreListener {

    /**
     * A reserved word for cache names. It denotes a default configuration
     * which is applied to caches created without configuration.
     */
    public static final String DEFAULT_CACHE_NAME = "default";

    /**
     * System Property based method of disabling ehcache. If disabled no elements will be added to a cache.
     * <p/>
     * Set the property "net.sf.ehcache.disabled=true" to disable ehcache.
     * <p/>
     * This can easily be done using <code>java -Dnet.sf.ehcache.disabled=true</code> in the command line.
     */
    public static final String NET_SF_EHCACHE_DISABLED = "net.sf.ehcache.disabled";

    /**
     * System Property based method of selecting the LruMemoryStore in use up to ehcache 1.5. This is provided
     * for ease of migration.
     * <p/>
     * Set the property "net.sf.ehcache.use.classic.lru=true" to use the older LruMemoryStore implementation
     * when LRU is selected as the eviction policy.
     * <p/>
     * This can easily be done using <code>java -Dnet.sf.ehcache.use.classic.lru=true</code> in the command line.
     */
    public static final String NET_SF_EHCACHE_USE_CLASSIC_LRU = "net.sf.ehcache.use.classic.lru";

    /**
     * The default interval between runs of the expiry thread.
     * @see CacheConfiguration#DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS CacheConfiguration#DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS for a preferred way of setting
     */
    public static final long DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS = CacheConfiguration.DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS;

    private static final Logger LOG = LoggerFactory.getLogger(Cache.class.getName());

    private static InetAddress localhost;

    /**
     * The amount of time to wait if a store gets backed up
     */
    private static final int BACK_OFF_TIME_MILLIS = 50;

    private static final int EXECUTOR_KEEP_ALIVE_TIME = 60000;
    private static final int EXECUTOR_MAXIMUM_POOL_SIZE = Math.min(10, Runtime.getRuntime().availableProcessors());
    private static final int EXECUTOR_CORE_POOL_SIZE = 1;
    private static final String EHCACHE_CLUSTERREDSTORE_MAX_CONCURRENCY_PROP = "ehcache.clusteredStore.maxConcurrency";
    private static final int DEFAULT_EHCACHE_CLUSTERREDSTORE_MAX_CONCURRENCY = 4096;

    static {
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOG.error("Unable to set localhost. This prevents creation of a GUID. Cause was: " + e.getMessage(), e);
        } catch (java.lang.NoClassDefFoundError e) {
            LOG.debug("InetAddress is being blocked by your runtime environment. e.g. Google App Engine." +
                    " Ehcache will work as a local cache.");
        }
    }

    private volatile boolean disabled = Boolean.getBoolean(NET_SF_EHCACHE_DISABLED);

    private final boolean useClassicLru = Boolean.getBoolean(NET_SF_EHCACHE_USE_CLASSIC_LRU);

    private volatile CacheStatus cacheStatus = new CacheStatus();

    private volatile CacheConfiguration configuration;

    /**
     * The {@link import net.sf.ehcache.store.Store} of this {@link Cache}.
     */
    private volatile Store compoundStore;

    private volatile CacheLockProvider lockProvider;

    private volatile RegisteredEventListeners registeredEventListeners;

    private volatile List<CacheExtension> registeredCacheExtensions;

    private volatile String guid;

    private volatile CacheManager cacheManager;

    private volatile BootstrapCacheLoader bootstrapCacheLoader;

    private volatile CacheExceptionHandler cacheExceptionHandler;

    private volatile List<CacheLoader> registeredCacheLoaders;

    private volatile CacheWriterManager cacheWriterManager;

    private volatile AtomicBoolean cacheWriterManagerInitFlag = new AtomicBoolean(false);

    private volatile ReentrantLock cacheWriterManagerInitLock = new ReentrantLock();

    private volatile CacheWriter registeredCacheWriter;

    /**
     * A ThreadPoolExecutor which uses a thread pool to schedule loads in the order in which they are requested.
     * <p/>
     * Each cache has its own one of these, if required. Because the Core Thread Pool is zero, no threads
     * are used until actually needed. Threads are added to the pool up to a maximum of 10. The keep alive
     * time is 60 seconds, after which, if they are not required they will be stopped and collected.
     * <p/>
     * The executorService is only used for cache loading, and is created lazily on demand to avoid unnecessary resource
     * usage.
     * <p/>
     * Use {@link #getExecutorService()} to ensure that it is initialised.
     */
    private volatile ExecutorService executorService;

    private volatile LiveCacheStatisticsWrapper liveCacheStatisticsData;

    private volatile SampledCacheStatisticsWrapper sampledCacheStatistics;

    private volatile TransactionManagerLookup transactionManagerLookup;

    private volatile boolean allowDisable = true;

    private volatile PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    private volatile ElementValueComparator elementValueComparator;

    private volatile NonstopActiveDelegateHolderImpl nonstopActiveDelegateHolder;

    private volatile EhcacheXAResource xaResource;

    /**
     * 2.0 and higher Constructor
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache} object
     * into {@link CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param cacheConfiguration the configuration that should be used to create the cache with
     */
    public Cache(CacheConfiguration cacheConfiguration) {
        this(cacheConfiguration, null, null);
    }

    /**
     * 2.0 and higher Constructor
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory}
     * and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache}
     * object into {@link CacheManager#addCache} to specify parameters other
     * than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param cacheConfiguration the configuration that should be used to create the cache with
     * @param registeredEventListeners  a notification service. Optionally null, in which case a new one with no registered listeners will be created.
     * @param bootstrapCacheLoader      the BootstrapCacheLoader to use to populate the cache when it is first initialised. Null if none is required.
     */
    public Cache(CacheConfiguration cacheConfiguration,
                 RegisteredEventListeners registeredEventListeners,
                 BootstrapCacheLoader bootstrapCacheLoader) {
        cacheStatus.changeState(Status.STATUS_UNINITIALISED);

        this.configuration = cacheConfiguration.clone();
        configuration.validateCompleteConfiguration();

        guid = createGuid();

        if (registeredEventListeners == null) {
            this.registeredEventListeners = new RegisteredEventListeners(this);
        } else {
            this.registeredEventListeners = registeredEventListeners;
        }

        registeredCacheExtensions = new CopyOnWriteArrayList<CacheExtension>();
        registeredCacheLoaders = new CopyOnWriteArrayList<CacheLoader>();

        // initialize statistics
        liveCacheStatisticsData = new LiveCacheStatisticsWrapper(this);
        sampledCacheStatistics = new SampledCacheStatisticsWrapper();

        RegisteredEventListeners listeners = getCacheEventNotificationService();
        registerCacheListeners(configuration, listeners);
        registerCacheExtensions(configuration, this);

        if (null == bootstrapCacheLoader) {
            this.bootstrapCacheLoader = createBootstrapCacheLoader(configuration.getBootstrapCacheLoaderFactoryConfiguration());
        } else {
            this.bootstrapCacheLoader = bootstrapCacheLoader;
        }
        registerCacheLoaders(configuration, this);
        registerCacheWriter(configuration, this);

        this.nonstopActiveDelegateHolder = new NonstopActiveDelegateHolderImpl(this);
    }



    /**
     * 1.0 Constructor.
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache} object
     * into {@link CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     * <p/>
     * This constructor creates disk stores, if specified, that do not persist between restarts.
     * <p/>
     * The default expiry thread interval of 120 seconds is used. This is the interval between runs
     * of the expiry thread, where it checks the disk store for expired elements. It is not the
     * the timeToLiveSeconds.
     *
     * @param name                the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxElementsInMemory the maximum number of elements in memory, before they are evicted (0 == no limit)
     * @param overflowToDisk      whether to use the disk store
     * @param eternal             whether the elements in the cache are eternal, i.e. never expire
     * @param timeToLiveSeconds   the default amount of time to live for an element from its creation date
     * @param timeToIdleSeconds   the default amount of time to live for an element from its last accessed or modified date
     * @since 1.0
     * @see #Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader) Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader),
     * for full construction support of version 2.0 and higher features.
     */
    public Cache(String name, int maxElementsInMemory, boolean overflowToDisk,
                 boolean eternal, long timeToLiveSeconds, long timeToIdleSeconds) {

        this(new CacheConfiguration(name, maxElementsInMemory)
                    .overflowToDisk(overflowToDisk)
                    .eternal(eternal)
                    .timeToLiveSeconds(timeToLiveSeconds)
                    .timeToIdleSeconds(timeToIdleSeconds));
    }


    /**
     * 1.1 Constructor.
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache} object
     * into {@link CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param name                the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxElementsInMemory the maximum number of elements in memory, before they are evicted (0 == no limit)
     * @param overflowToDisk      whether to use the disk store
     * @param eternal             whether the elements in the cache are eternal, i.e. never expire
     * @param timeToLiveSeconds   the default amount of time to live for an element from its creation date
     * @param timeToIdleSeconds   the default amount of time to live for an element from its last accessed or modified date
     * @param diskPersistent      whether to persist the cache to disk between JVM restarts
     * @param diskExpiryThreadIntervalSeconds
     *                            how often to run the disk store expiry thread. A large number of 120 seconds plus is recommended
     * @since 1.1
     * @see #Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader) Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader),
     * for full construction support of version 2.0 and higher features.
     */
    public Cache(String name,
                 int maxElementsInMemory,
                 boolean overflowToDisk,
                 boolean eternal,
                 long timeToLiveSeconds,
                 long timeToIdleSeconds,
                 boolean diskPersistent,
                 long diskExpiryThreadIntervalSeconds) {

        this(new CacheConfiguration(name, maxElementsInMemory)
                    .overflowToDisk(overflowToDisk)
                    .eternal(eternal)
                    .timeToLiveSeconds(timeToLiveSeconds)
                    .timeToIdleSeconds(timeToIdleSeconds)
                    .diskPersistent(diskPersistent)
                    .diskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds));

        LOG.warn("An API change between ehcache-1.1 and ehcache-1.2 results in the persistence path being set to " +
                DiskStoreConfiguration.getDefaultPath() + " when the ehcache-1.1 constructor is used. " +
                "Please change to the 1.2 constructor.");
    }


    /**
     * 1.2 Constructor
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache} object
     * into {@link CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param name                      the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted (0 == no limit)
     * @param memoryStoreEvictionPolicy one of LRU, LFU and FIFO. Optionally null, in which case it will be set to LRU.
     * @param overflowToDisk            whether to use the disk store
     * @param diskStorePath             this parameter is ignored. CacheManager sets it using setter injection.
     * @param eternal                   whether the elements in the cache are eternal, i.e. never expire
     * @param timeToLiveSeconds         the default amount of time to live for an element from its creation date
     * @param timeToIdleSeconds         the default amount of time to live for an element from its last accessed or modified date
     * @param diskPersistent            whether to persist the cache to disk between JVM restarts
     * @param diskExpiryThreadIntervalSeconds
     *                                  how often to run the disk store expiry thread. A large number of 120 seconds plus is recommended
     * @param registeredEventListeners  a notification service. Optionally null, in which case a new
     *                                  one with no registered listeners will be created.
     * @since 1.2
     * @see #Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader) Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader),
     * for full construction support of version 2.0 and higher features.
     */
    public Cache(String name,
                 int maxElementsInMemory,
                 MemoryStoreEvictionPolicy memoryStoreEvictionPolicy,
                 boolean overflowToDisk,
                 String diskStorePath,
                 boolean eternal,
                 long timeToLiveSeconds,
                 long timeToIdleSeconds,
                 boolean diskPersistent,
                 long diskExpiryThreadIntervalSeconds,
                 RegisteredEventListeners registeredEventListeners) {

        this(new CacheConfiguration(name, maxElementsInMemory)
                    .memoryStoreEvictionPolicy(memoryStoreEvictionPolicy)
                    .overflowToDisk(overflowToDisk)
                    .eternal(eternal)
                    .timeToLiveSeconds(timeToLiveSeconds)
                    .timeToIdleSeconds(timeToIdleSeconds)
                    .diskPersistent(diskPersistent)
                    .diskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds),
                registeredEventListeners,
                null);

    }

    /**
     * 1.2.1 Constructor
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache} object
     * into {@link CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param name                      the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted (0 == no limit)
     * @param memoryStoreEvictionPolicy one of LRU, LFU and FIFO. Optionally null, in which case it will be set to LRU.
     * @param overflowToDisk            whether to use the disk store
     * @param diskStorePath             this parameter is ignored. CacheManager sets it using setter injection.
     * @param eternal                   whether the elements in the cache are eternal, i.e. never expire
     * @param timeToLiveSeconds         the default amount of time to live for an element from its creation date
     * @param timeToIdleSeconds         the default amount of time to live for an element from its last accessed or modified date
     * @param diskPersistent            whether to persist the cache to disk between JVM restarts
     * @param diskExpiryThreadIntervalSeconds
     *                                  how often to run the disk store expiry thread. A large number of 120 seconds plus is recommended
     * @param registeredEventListeners  a notification service. Optionally null, in which case a new one with no registered listeners will be created.
     * @param bootstrapCacheLoader      the BootstrapCacheLoader to use to populate the cache when it is first initialised. Null if none is required.
     * @since 1.2.1
     * @see #Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader) Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader),
     * for full construction support of version 2.0 and higher features.
     */
    public Cache(String name,
                 int maxElementsInMemory,
                 MemoryStoreEvictionPolicy memoryStoreEvictionPolicy,
                 boolean overflowToDisk,
                 String diskStorePath,
                 boolean eternal,
                 long timeToLiveSeconds,
                 long timeToIdleSeconds,
                 boolean diskPersistent,
                 long diskExpiryThreadIntervalSeconds,
                 RegisteredEventListeners registeredEventListeners,
                 BootstrapCacheLoader bootstrapCacheLoader) {

        this(new CacheConfiguration(name, maxElementsInMemory)
                    .memoryStoreEvictionPolicy(memoryStoreEvictionPolicy)
                    .overflowToDisk(overflowToDisk)
                    .eternal(eternal)
                    .timeToLiveSeconds(timeToLiveSeconds)
                    .timeToIdleSeconds(timeToIdleSeconds)
                    .diskPersistent(diskPersistent)
                    .diskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds),
                registeredEventListeners,
                bootstrapCacheLoader);
    }

    /**
     * 1.2.4 Constructor
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache} object
     * into {@link CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param name                      the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted (0 == no limit)
     * @param memoryStoreEvictionPolicy one of LRU, LFU and FIFO. Optionally null, in which case it will be set to LRU.
     * @param overflowToDisk            whether to use the disk store
     * @param diskStorePath             this parameter is ignored. CacheManager sets it using setter injection.
     * @param eternal                   whether the elements in the cache are eternal, i.e. never expire
     * @param timeToLiveSeconds         the default amount of time to live for an element from its creation date
     * @param timeToIdleSeconds         the default amount of time to live for an element from its last accessed or modified date
     * @param diskPersistent            whether to persist the cache to disk between JVM restarts
     * @param diskExpiryThreadIntervalSeconds
     *                                  how often to run the disk store expiry thread. A large number of 120 seconds plus is recommended
     * @param registeredEventListeners  a notification service. Optionally null, in which case a new one with no registered listeners will be created.
     * @param bootstrapCacheLoader      the BootstrapCacheLoader to use to populate the cache when it is first initialised. Null if none is required.
     * @param maxElementsOnDisk         the maximum number of Elements to allow on the disk. 0 means unlimited.
     * @since 1.2.4
     * @see #Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader) Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader),
     * for full construction support of version 2.0 and higher features.
     */
    public Cache(String name,
                 int maxElementsInMemory,
                 MemoryStoreEvictionPolicy memoryStoreEvictionPolicy,
                 boolean overflowToDisk,
                 String diskStorePath,
                 boolean eternal,
                 long timeToLiveSeconds,
                 long timeToIdleSeconds,
                 boolean diskPersistent,
                 long diskExpiryThreadIntervalSeconds,
                 RegisteredEventListeners registeredEventListeners,
                 BootstrapCacheLoader bootstrapCacheLoader,
                 int maxElementsOnDisk) {

        this(new CacheConfiguration(name, maxElementsInMemory)
                    .memoryStoreEvictionPolicy(memoryStoreEvictionPolicy)
                    .overflowToDisk(overflowToDisk)
                    .eternal(eternal)
                    .timeToLiveSeconds(timeToLiveSeconds)
                    .timeToIdleSeconds(timeToIdleSeconds)
                    .diskPersistent(diskPersistent)
                    .diskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds)
                    .maxElementsOnDisk(maxElementsOnDisk),
                registeredEventListeners,
                bootstrapCacheLoader);
    }

    /**
     * 1.3 Constructor
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache} object
     * into {@link CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param name                      the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted (0 == no limit)
     * @param memoryStoreEvictionPolicy one of LRU, LFU and FIFO. Optionally null, in which case it will be set to LRU.
     * @param overflowToDisk            whether to use the disk store
     * @param diskStorePath             this parameter is ignored. CacheManager sets it using setter injection.
     * @param eternal                   whether the elements in the cache are eternal, i.e. never expire
     * @param timeToLiveSeconds         the default amount of time to live for an element from its creation date
     * @param timeToIdleSeconds         the default amount of time to live for an element from its last accessed or modified date
     * @param diskPersistent            whether to persist the cache to disk between JVM restarts
     * @param diskExpiryThreadIntervalSeconds
     *                                  how often to run the disk store expiry thread. A large number of 120 seconds plus is recommended
     * @param registeredEventListeners  a notification service. Optionally null, in which case a new one with no registered listeners will be created.
     * @param bootstrapCacheLoader      the BootstrapCacheLoader to use to populate the cache when it is first initialised. Null if none is required.
     * @param maxElementsOnDisk         the maximum number of Elements to allow on the disk. 0 means unlimited.
     * @param diskSpoolBufferSizeMB     the amount of memory to allocate the write buffer for puts to the DiskStore.
     * @since 1.3
     * @see #Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader) Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader),
     * for full construction support of version 2.0 and higher features.
     */
    public Cache(String name,
                 int maxElementsInMemory,
                 MemoryStoreEvictionPolicy memoryStoreEvictionPolicy,
                 boolean overflowToDisk,
                 String diskStorePath,
                 boolean eternal,
                 long timeToLiveSeconds,
                 long timeToIdleSeconds,
                 boolean diskPersistent,
                 long diskExpiryThreadIntervalSeconds,
                 RegisteredEventListeners registeredEventListeners,
                 BootstrapCacheLoader bootstrapCacheLoader,
                 int maxElementsOnDisk,
                 int diskSpoolBufferSizeMB) {

        this(new CacheConfiguration(name, maxElementsInMemory)
                    .memoryStoreEvictionPolicy(memoryStoreEvictionPolicy)
                    .overflowToDisk(overflowToDisk)
                    .eternal(eternal)
                    .timeToLiveSeconds(timeToLiveSeconds)
                    .timeToIdleSeconds(timeToIdleSeconds)
                    .diskPersistent(diskPersistent)
                    .diskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds)
                    .maxElementsOnDisk(maxElementsOnDisk)
                    .diskSpoolBufferSizeMB(diskSpoolBufferSizeMB),
                registeredEventListeners,
                bootstrapCacheLoader);
    }

    /**
     * 1.6.0 Constructor
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache} object
     * into {@link CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param name                      the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted (0 == no limit)
     * @param memoryStoreEvictionPolicy one of LRU, LFU and FIFO. Optionally null, in which case it will be set to LRU.
     * @param overflowToDisk            whether to use the disk store
     * @param diskStorePath             this parameter is ignored. CacheManager sets it using setter injection.
     * @param eternal                   whether the elements in the cache are eternal, i.e. never expire
     * @param timeToLiveSeconds         the default amount of time to live for an element from its creation date
     * @param timeToIdleSeconds         the default amount of time to live for an element from its last accessed or modified date
     * @param diskPersistent            whether to persist the cache to disk between JVM restarts
     * @param diskExpiryThreadIntervalSeconds
     *                                  how often to run the disk store expiry thread. A large number of 120 seconds plus is recommended
     * @param registeredEventListeners  a notification service. Optionally null, in which case a new one with no registered listeners will be created.
     * @param bootstrapCacheLoader      the BootstrapCacheLoader to use to populate the cache when it is first initialised. Null if none is required.
     * @param maxElementsOnDisk         the maximum number of Elements to allow on the disk. 0 means unlimited.
     * @param diskSpoolBufferSizeMB     the amount of memory to allocate the write buffer for puts to the DiskStore.
     * @param clearOnFlush              whether the in-memory storage should be cleared when {@link #flush flush()} is called on the cache
     * @since 1.6.0
     * @see #Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader) Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader),
     * for full construction support of version 2.0 and higher features.
     */
    public Cache(String name,
                 int maxElementsInMemory,
                 MemoryStoreEvictionPolicy memoryStoreEvictionPolicy,
                 boolean overflowToDisk,
                 String diskStorePath,
                 boolean eternal,
                 long timeToLiveSeconds,
                 long timeToIdleSeconds,
                 boolean diskPersistent,
                 long diskExpiryThreadIntervalSeconds,
                 RegisteredEventListeners registeredEventListeners,
                 BootstrapCacheLoader bootstrapCacheLoader,
                 int maxElementsOnDisk,
                 int diskSpoolBufferSizeMB,
                 boolean clearOnFlush) {

        this(new CacheConfiguration(name, maxElementsInMemory)
                    .memoryStoreEvictionPolicy(memoryStoreEvictionPolicy)
                    .overflowToDisk(overflowToDisk)
                    .eternal(eternal)
                    .timeToLiveSeconds(timeToLiveSeconds)
                    .timeToIdleSeconds(timeToIdleSeconds)
                    .diskPersistent(diskPersistent)
                    .diskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds)
                    .maxElementsOnDisk(maxElementsOnDisk)
                    .diskSpoolBufferSizeMB(diskSpoolBufferSizeMB)
                    .clearOnFlush(clearOnFlush),
                registeredEventListeners,
                bootstrapCacheLoader);
    }

    /**
     * 1.7.0 Constructor
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache} object
     * into {@link CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param name                      the name of the cache. Note that "default" is a reserved name for the defaultCache.
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted (0 == no limit)
     * @param memoryStoreEvictionPolicy one of LRU, LFU and FIFO. Optionally null, in which case it will be set to LRU.
     * @param overflowToDisk            whether to use the disk store
     * @param diskStorePath             this parameter is ignored. CacheManager sets it using setter injection.
     * @param eternal                   whether the elements in the cache are eternal, i.e. never expire
     * @param timeToLiveSeconds         the default amount of time to live for an element from its creation date
     * @param timeToIdleSeconds         the default amount of time to live for an element from its last accessed or modified date
     * @param diskPersistent            whether to persist the cache to disk between JVM restarts
     * @param diskExpiryThreadIntervalSeconds
     *                                  how often to run the disk store expiry thread. A large number of 120 seconds plus is recommended
     * @param registeredEventListeners  a notification service. Optionally null, in which case a new one with no registered listeners will be created.
     * @param bootstrapCacheLoader      the BootstrapCacheLoader to use to populate the cache when it is first initialised. Null if none is required.
     * @param maxElementsOnDisk         the maximum number of Elements to allow on the disk. 0 means unlimited.
     * @param diskSpoolBufferSizeMB     the amount of memory to allocate the write buffer for puts to the DiskStore.
     * @param clearOnFlush              whether the in-memory storage should be cleared when {@link #flush flush()} is called on the cache
     * @param isTerracottaClustered     whether to cluster this cache with Terracotta
     * @param terracottaValueMode       either "SERIALIZATION" or "IDENTITY" mode, only used if isTerracottaClustered=true
     * @param terracottaCoherentReads   whether this cache should use coherent reads (usually should be true) unless optimizing for read-only
     * @since 1.7.0
     * @see #Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader) Cache(CacheConfiguration, RegisteredEventListeners, BootstrapCacheLoader),
     * for full construction support of version 2.0 and higher features.
     */
    public Cache(String name, int maxElementsInMemory, MemoryStoreEvictionPolicy memoryStoreEvictionPolicy, boolean overflowToDisk,
                 String diskStorePath, boolean eternal, long timeToLiveSeconds, long timeToIdleSeconds, boolean diskPersistent,
                 long diskExpiryThreadIntervalSeconds, RegisteredEventListeners registeredEventListeners,
                 BootstrapCacheLoader bootstrapCacheLoader, int maxElementsOnDisk, int diskSpoolBufferSizeMB, boolean clearOnFlush,
                 boolean isTerracottaClustered, String terracottaValueMode, boolean terracottaCoherentReads) {

        this(new CacheConfiguration(name, maxElementsInMemory)
                    .memoryStoreEvictionPolicy(memoryStoreEvictionPolicy)
                    .overflowToDisk(overflowToDisk)
                    .eternal(eternal)
                    .timeToLiveSeconds(timeToLiveSeconds)
                    .timeToIdleSeconds(timeToIdleSeconds)
                    .diskPersistent(diskPersistent)
                    .diskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds)
                    .maxElementsOnDisk(maxElementsOnDisk)
                    .diskSpoolBufferSizeMB(diskSpoolBufferSizeMB)
                    .clearOnFlush(clearOnFlush)
                    .terracotta(new TerracottaConfiguration()
                        .clustered(isTerracottaClustered)
                        .valueMode(terracottaValueMode)
                        .coherentReads(terracottaCoherentReads)),
                registeredEventListeners,
                bootstrapCacheLoader);
    }


    /**
     * A factory method to create a RegisteredEventListeners
     */
    private static void registerCacheListeners(CacheConfiguration cacheConfiguration,
                                                 RegisteredEventListeners registeredEventListeners) {
        List cacheEventListenerConfigurations = cacheConfiguration.getCacheEventListenerConfigurations();
        for (Object cacheEventListenerConfiguration : cacheEventListenerConfigurations) {
            CacheConfiguration.CacheEventListenerFactoryConfiguration factoryConfiguration =
                    (CacheConfiguration.CacheEventListenerFactoryConfiguration) cacheEventListenerConfiguration;
            CacheEventListener cacheEventListener = createCacheEventListener(factoryConfiguration);
            registeredEventListeners.registerListener(cacheEventListener, factoryConfiguration.getListenFor());
        }
    }

    /**
     * A factory method to register cache extensions
     *
     * @param cacheConfiguration the cache configuration
     * @param cache              the cache
     */
    private static void registerCacheExtensions(CacheConfiguration cacheConfiguration, Ehcache cache) {
        List cacheExtensionConfigurations = cacheConfiguration.getCacheExtensionConfigurations();
        for (Object cacheExtensionConfiguration : cacheExtensionConfigurations) {
            CacheConfiguration.CacheExtensionFactoryConfiguration factoryConfiguration =
                    (CacheConfiguration.CacheExtensionFactoryConfiguration) cacheExtensionConfiguration;
            CacheExtension cacheExtension = createCacheExtension(factoryConfiguration, cache);
            cache.registerCacheExtension(cacheExtension);
        }
    }

    /**
     * A factory method to register cache Loaders
     *
     * @param cacheConfiguration the cache configuration
     * @param cache              the cache
     */
    private static void registerCacheLoaders(CacheConfiguration cacheConfiguration, Ehcache cache) {
        List cacheLoaderConfigurations = cacheConfiguration.getCacheLoaderConfigurations();
        for (Object cacheLoaderConfiguration : cacheLoaderConfigurations) {
            CacheConfiguration.CacheLoaderFactoryConfiguration factoryConfiguration =
                    (CacheConfiguration.CacheLoaderFactoryConfiguration) cacheLoaderConfiguration;
            CacheLoader cacheLoader = createCacheLoader(factoryConfiguration, cache);
            cache.registerCacheLoader(cacheLoader);
        }
    }

    /**
     * A factory method to register cache writers
     *
     * @param cacheConfiguration the cache configuration
     * @param cache              the cache
     */
    private static void registerCacheWriter(CacheConfiguration cacheConfiguration, Ehcache cache) {
        CacheWriterConfiguration config = cacheConfiguration.getCacheWriterConfiguration();
        if (config != null) {
            CacheWriter cacheWriter = createCacheWriter(config, cache);
            cache.registerCacheWriter(cacheWriter);
        }
    }


    /**
     * Tries to load the class specified otherwise defaults to null.
     *
     * @param factoryConfiguration
     */
    private static CacheEventListener createCacheEventListener(
            CacheConfiguration.CacheEventListenerFactoryConfiguration factoryConfiguration) {
        String className = null;
        CacheEventListener cacheEventListener = null;
        if (factoryConfiguration != null) {
            className = factoryConfiguration.getFullyQualifiedClassPath();
        }
        if (className == null) {
            LOG.debug("CacheEventListener factory not configured. Skipping...");
        } else {
            CacheEventListenerFactory factory = (CacheEventListenerFactory)
                    ClassLoaderUtil.createNewInstance(className);
            Properties properties =

                    PropertyUtil.parseProperties(factoryConfiguration.getProperties(),
                            factoryConfiguration.getPropertySeparator());
            cacheEventListener =
                    factory.createCacheEventListener(properties);
        }
        return cacheEventListener;
    }

    /**
     * Tries to load the class specified otherwise defaults to null.
     *
     * @param factoryConfiguration
     */
    private static CacheExtension createCacheExtension(
            CacheConfiguration.CacheExtensionFactoryConfiguration factoryConfiguration, Ehcache cache) {
        String className = null;
        CacheExtension cacheExtension = null;
        if (factoryConfiguration != null) {
            className = factoryConfiguration.getFullyQualifiedClassPath();
        }
        if (className == null) {
            LOG.debug("CacheExtension factory not configured. Skipping...");
        } else {
            CacheExtensionFactory factory = (CacheExtensionFactory) ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(factoryConfiguration.getProperties(),
                    factoryConfiguration.getPropertySeparator());
            cacheExtension = factory.createCacheExtension(cache, properties);
        }
        return cacheExtension;
    }

    /**
     * Tries to load the class specified otherwise defaults to null.
     *
     * @param factoryConfiguration
     */
    private static CacheLoader createCacheLoader(
            CacheConfiguration.CacheLoaderFactoryConfiguration factoryConfiguration, Ehcache cache) {
        String className = null;
        CacheLoader cacheLoader = null;
        if (factoryConfiguration != null) {
            className = factoryConfiguration.getFullyQualifiedClassPath();
        }
        if (className == null) {
            LOG.debug("CacheLoader factory not configured. Skipping...");
        } else {
            CacheLoaderFactory factory = (CacheLoaderFactory) ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(factoryConfiguration.getProperties(),
                    factoryConfiguration.getPropertySeparator());
            cacheLoader = factory.createCacheLoader(cache, properties);
        }
        return cacheLoader;
    }

    /**
     * Tries to load the class specified otherwise defaults to null.
     *
     * @param config
     */
    private static CacheWriter createCacheWriter(CacheWriterConfiguration config, Ehcache cache) {
        String className = null;
        CacheWriter cacheWriter = null;
        CacheWriterConfiguration.CacheWriterFactoryConfiguration factoryConfiguration = config.getCacheWriterFactoryConfiguration();
        if (factoryConfiguration != null) {
            className = factoryConfiguration.getFullyQualifiedClassPath();
        }
        if (null == className) {
            LOG.debug("CacheWriter factory not configured. Skipping...");
        } else {
            CacheWriterFactory factory = (CacheWriterFactory) ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(factoryConfiguration.getProperties(),
                    factoryConfiguration.getPropertySeparator());
            if (null == properties) {
                properties = new Properties();
            }
            cacheWriter = factory.createCacheWriter(cache, properties);
        }
        return cacheWriter;
    }

    /**
     * Tries to load a BootstrapCacheLoader from the class specified.
     *
     * @return If there is none returns null.
     */
    private static final BootstrapCacheLoader createBootstrapCacheLoader(
            CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration factoryConfiguration) throws CacheException {
        String className = null;
        BootstrapCacheLoader bootstrapCacheLoader = null;
        if (factoryConfiguration != null) {
            className = factoryConfiguration.getFullyQualifiedClassPath();
        }
        if (className == null || className.length() == 0) {
            LOG.debug("No BootstrapCacheLoaderFactory class specified. Skipping...");
        } else {
            BootstrapCacheLoaderFactory factory = (BootstrapCacheLoaderFactory)
                    ClassLoaderUtil.createNewInstance(className);
            Properties properties = PropertyUtil.parseProperties(factoryConfiguration.getProperties(),
                    factoryConfiguration.getPropertySeparator());
            return factory.createBootstrapCacheLoader(properties);
        }
        return bootstrapCacheLoader;
    }

    /**
     * Get the TransactionManagerLookup implementation used to lookup the TransactionManager.
     * This is generally only set for XA transactional caches
     * @return The {@link net.sf.ehcache.transaction.manager.TransactionManagerLookup} instance
     */
    public TransactionManagerLookup getTransactionManagerLookup() {
       return transactionManagerLookup;
    }

    /**
     * Sets the TransactionManagerLookup that needs to be used for this cache to lookup the TransactionManager
     * This needs to be set before {@link Cache#initialise()} is called
     * @param lookup The {@link net.sf.ehcache.transaction.manager.TransactionManagerLookup} instance
     */
    public void setTransactionManagerLookup(TransactionManagerLookup lookup) {
        TransactionManagerLookup oldValue = getTransactionManagerLookup();
        this.transactionManagerLookup = lookup;
        firePropertyChange("TransactionManagerLookup", oldValue, lookup);
    }

    /**
     * Newly created caches do not have a {@link net.sf.ehcache.store.Store}.
     * <p/>
     * This method creates the store and makes the cache ready to accept elements
     */
    public void initialise() {
        synchronized (this) {
            if (!cacheStatus.canInitialize()) {
                throw new IllegalStateException("Cannot initialise the " + configuration.getName()
                        + " cache because its status is not STATUS_UNINITIALISED");
            }

            // on-heap pool configuration
            Pool onHeapPool;
            if (configuration.getMaxBytesLocalHeap() > 0) {
                PoolEvictor<PoolableStore> evictor = new FromLargestCacheOnHeapPoolEvictor();
                SizeOfEngine sizeOfEngine = cacheManager.createSizeOfEngine(this);
                onHeapPool = new BoundedPool(configuration.getMaxBytesLocalHeap(), evictor, sizeOfEngine);
            } else if (getCacheManager() != null && getCacheManager().getConfiguration().isMaxBytesLocalHeapSet()) {
                onHeapPool = getCacheManager().getOnHeapPool();
            } else {
                onHeapPool = new UnboundedPool();
            }

            // on-disk pool configuration
            Pool onDiskPool;
            if (configuration.getMaxBytesLocalDisk() > 0) {
                PoolEvictor<PoolableStore> evictor = new FromLargestCacheOnDiskPoolEvictor();
                onDiskPool = new BoundedPool(configuration.getMaxBytesLocalDisk(), evictor, null);
            } else if (getCacheManager() != null && getCacheManager().getConfiguration().isMaxBytesLocalDiskSet()) {
                onDiskPool = getCacheManager().getOnDiskPool();
            } else {
                onDiskPool = new UnboundedPool();
            }

            ReadWriteCopyStrategy<Element> copyStrategy = null;
            if (configuration.getTransactionalMode().isTransactional()) {
                configuration.getCopyStrategyConfiguration().setCopyStrategyInstance(null);
                copyStrategy = configuration.getCopyStrategyConfiguration().getCopyStrategyInstance();
                configuration.getCopyStrategyConfiguration().setCopyStrategyInstance(new ImmutableValueElementCopyStrategy());
            }
            elementValueComparator = configuration.getElementValueComparatorConfiguration().getElementComparatorInstance(configuration);

            if (configuration.getTransactionalMode().isTransactional()
                && configuration.isTerracottaClustered()
                && configuration.getTerracottaConfiguration().getValueMode() != TerracottaConfiguration.ValueMode.SERIALIZATION) {
                throw new CacheException("To be transactional, a Terracotta clustered cache needs to be in Serialization value mode");
            }

            Store store;
            if (isTerracottaClustered()) {
                checkClusteredConfig();
                int maxConcurrency = Integer.getInteger(EHCACHE_CLUSTERREDSTORE_MAX_CONCURRENCY_PROP,
                        DEFAULT_EHCACHE_CLUSTERREDSTORE_MAX_CONCURRENCY);
                if (getCacheConfiguration().getTerracottaConfiguration().getConcurrency() > maxConcurrency) {
                    throw new InvalidConfigurationException("Maximum supported concurrency for Terracotta clustered caches is "
                            + maxConcurrency + ". Please reconfigure cache '" + getName() + "' with concurrency value <= " + maxConcurrency
                            + " or use system property '" + EHCACHE_CLUSTERREDSTORE_MAX_CONCURRENCY_PROP + "' to override the default");
                }
                boolean cachePinned = getCacheConfiguration().getPinningConfiguration() != null
                        && getCacheConfiguration().getPinningConfiguration().getStore() == PinningConfiguration.Store.INCACHE;
                if (!cachePinned && getCacheConfiguration().getMaxElementsOnDisk() == 0) {
                    LOG.warn("Performance may degrade and server disks could run out of space!\nThe distributed cache {} does not have " +
                             "maxElementsOnDisk set. Failing to set maxElementsOnDisk could mean no eviction of its elements from the " +
                             "Terracotta Server Array disk store. To avoid this, set maxElementsOnDisk to a non-zero value.", getName());
                }
                Store tempStore = cacheManager.createTerracottaStore(this);
                if (!(tempStore instanceof TerracottaStore)) {
                    throw new CacheException(
                            "CacheManager should create instances of TerracottaStore for Terracotta Clustered caches instead of - "
                                    + (tempStore == null ? "null" : tempStore.getClass().getName()));
                }
                CacheConfiguration.TransactionalMode clusteredTransactionalMode = ((TerracottaStore)tempStore).getTransactionalMode();
                if (clusteredTransactionalMode != null && !clusteredTransactionalMode.equals(configuration.getTransactionalMode())) {
                    throw new InvalidConfigurationException("Transactional mode cannot be changed on clustered caches. "
                        + "Please reconfigure cache '" + getName() + "' with transactionalMode = " + clusteredTransactionalMode
                    );
                }
                TerracottaStore terracottaStore = (TerracottaStore) makeXaStrictTransactionalIfNeeded(tempStore, copyStrategy);

                NonstopConfiguration nonstopConfig = getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration();
                // freeze the config whether nonstop is enabled or not
                if (nonstopConfig != null) {
                    nonstopConfig.freezeConfig();
                }
                if (getCacheConfiguration().getTerracottaConfiguration().isNonstopEnabled()) {
                    nonstopActiveDelegateHolder.terracottaStoreInitialized(terracottaStore);
                    store = nonstopActiveDelegateHolder.getNonstopStore();
                } else {
                    store = terracottaStore;
                }
            } else {
                FeaturesManager featuresManager = cacheManager.getFeaturesManager();
                if (featuresManager == null) {
                    if (configuration.isOverflowToOffHeap()) {
                        throw new CacheException("Cache " + configuration.getName()
                                + " cannot be configured because the enterprise features manager could not be found. "
                                + "You must use an enterprise version of Ehcache to successfully enable overflowToOffHeap.");
                    }
                    PersistenceConfiguration persistence = configuration.getPersistenceConfiguration();
                    if (persistence != null && Strategy.LOCALRESTARTABLE.equals(persistence.getStrategy())) {
                        throw new CacheException("Cache " + configuration.getName()
                                + " cannot be configured because the enterprise features manager could not be found. "
                                + "You must use an enterprise version of Ehcache to successfully enable enterprise persistence.");
                    }

                    if (useClassicLru && configuration.getMemoryStoreEvictionPolicy().equals(MemoryStoreEvictionPolicy.LRU)) {
                        Store disk = createDiskStore();
                        store = new LegacyStoreWrapper(new LruMemoryStore(this, disk), disk, registeredEventListeners, configuration);
                    } else {
                        if (configuration.isOverflowToDisk()) {
                            store = DiskBackedMemoryStore.create(this, onHeapPool, onDiskPool);
                        } else {
                            store = MemoryOnlyStore.create(this, onHeapPool);
                        }
                    }
                } else {
                    store = featuresManager.createStore(this, onHeapPool, onDiskPool);
                }
                store = makeXaStrictTransactionalIfNeeded(store, copyStrategy);
            }

            /* note: this isn't part of makeXaStrictTransactionalIfNeeded() as only xa_strict supports NonStop, meaning that only
             * that transactional store can be wrapped by NonStopStore. Other TX modes have to wrap the NonStop store due to their
             * lack of NonStop support (ie: lack of transaction context suspension/resuming).
             */
            if (configuration.isXaTransactional()) {
                SoftLockFactory softLockFactory = cacheManager.createSoftLockFactory(this);
                LocalTransactionStore localTransactionStore = new LocalTransactionStore(getCacheManager().getTransactionController(),
                        softLockFactory, this, store, copyStrategy);
                this.compoundStore = new JtaLocalTransactionStore(localTransactionStore, transactionManagerLookup,
                        cacheManager.getTransactionController());
            } else if (configuration.isLocalTransactional()) {
                SoftLockFactory softLockFactory = cacheManager.createSoftLockFactory(this);
                this.compoundStore = new LocalTransactionStore(getCacheManager().getTransactionController(), softLockFactory, this, store,
                        copyStrategy);
            } else {
                this.compoundStore = store;
            }

            if (isSearchable()) {
                this.compoundStore = new ElementIdAssigningStore(compoundStore);
            }

            Map<String, AttributeExtractor> extractors = new HashMap<String, AttributeExtractor>();
            for (SearchAttribute sa : configuration.getSearchAttributes().values()) {
                extractors.put(sa.getName(), sa.constructExtractor());
            }
            compoundStore.setAttributeExtractors(extractors);
            this.cacheWriterManager = configuration.getCacheWriterConfiguration().getWriteMode().createWriterManager(this);
            initialiseCacheWriterManager(false);
            cacheStatus.changeState(Status.STATUS_ALIVE);
            initialiseRegisteredCacheExtensions();
            initialiseRegisteredCacheLoaders();
            initialiseRegisteredCacheWriter();
            // initialize live statistics
            // register to get notifications of
            // put/update/removeInternal/expiry/eviction
            getCacheEventNotificationService().registerListener(liveCacheStatisticsData);
            // set up default values
            liveCacheStatisticsData.setStatisticsAccuracy(Statistics.STATISTICS_ACCURACY_BEST_EFFORT);
            liveCacheStatisticsData.setStatisticsEnabled(configuration.getStatistics());

            // register the sampled cache statistics
            this.registerCacheUsageListener(sampledCacheStatistics);

            if (isTerracottaClustered()) {
                // create this to be sure that it's present on each node to receive clustered events,
                // even if this node is not sending out its events
                cacheManager.createTerracottaEventReplicator(this);
            }

            Object context = compoundStore.getInternalContext();
            if (context instanceof CacheLockProvider) {
                lockProvider = (CacheLockProvider) context;
            } else {
                this.lockProvider = new StripedReadWriteLockSync(StripedReadWriteLockSync.DEFAULT_NUMBER_OF_MUTEXES);
            }
        }

        compoundStore.addStoreListener(this);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Initialised cache: " + configuration.getName());
        }

        if (disabled) {
            LOG.warn("Cache: " + configuration.getName() + " is disabled because the " + NET_SF_EHCACHE_DISABLED
                    + " property was set to true. No elements will be added to the cache.");
        }
    }

    private void checkClusteredConfig() {
        final Consistency consistency = getCacheConfiguration().getTerracottaConfiguration().getConsistency();
        final boolean coherent = getCacheConfiguration().getTerracottaConfiguration().isCoherent();
        if (getCacheConfiguration().getTerracottaConfiguration().isSynchronousWrites() && consistency == Consistency.EVENTUAL) {
            throw new InvalidConfigurationException(
                    "Terracotta clustered caches with eventual consistency and synchronous writes are not supported yet."
                            + " You can fix this by either making the cache in 'strong' consistency mode "
                            + "(<terracotta consistency=\"strong\"/>) or turning off synchronous writes.");
        }
        if (getCacheConfiguration().getTransactionalMode().isTransactional() && consistency == Consistency.EVENTUAL) {
            throw new InvalidConfigurationException("Consistency should be " + Consistency.STRONG
                    + " when cache is configured with transactions enabled. "
                    + "You can fix this by either making the cache in 'strong' consistency mode "
                    + "(<terracotta consistency=\"strong\"/>) or turning off transactions.");
        }
        if (getCacheConfiguration().getTransactionalMode().isTransactional()
                && !getCacheConfiguration().getTransactionalMode().equals(CacheConfiguration.TransactionalMode.XA_STRICT)
                && getCacheConfiguration().getTerracottaConfiguration().isNonstopEnabled()) {
            LOG.warn("Cache: " + configuration.getName() + " configured both NonStop and transactional non xa_strict."
                    + " NonStop features won't work for this cache!");
        }
        if ((coherent && consistency == Consistency.EVENTUAL) || (!coherent && consistency == Consistency.STRONG)) {
            throw new InvalidConfigurationException("Coherent and consistency attribute values are conflicting. "
                    + "Please remove the coherent attribute as its deprecated.");
        }
    }

    /*
     * Note: this method could be used for xa and local tx modes as well if they supported NonStop
     */
    private Store makeXaStrictTransactionalIfNeeded(Store clusteredStore, ReadWriteCopyStrategy<Element> copyStrategy) {
        Store wrappedStore;

        if (configuration.isXaStrictTransactional()) {
            if (transactionManagerLookup.getTransactionManager() == null) {
                throw new CacheException("You've configured cache " + cacheManager.getName() + "."
                                         + configuration.getName() + " to be transactional, but no TransactionManager could be found!");
            }
            //set xa enabled
            if (configuration.isTerracottaClustered()) {
                configuration.getTerracottaConfiguration().setCacheXA(true);
            }
            SoftLockFactory softLockFactory = cacheManager.createSoftLockFactory(this);
            TransactionIDFactory transactionIDFactory = cacheManager.getOrCreateTransactionIDFactory();

            // this xaresource is for initial registration and recovery
            xaResource = new EhcacheXAResourceImpl(this, clusteredStore, transactionManagerLookup,
                    softLockFactory, transactionIDFactory, copyStrategy);
            transactionManagerLookup.register(xaResource);

            wrappedStore = new XATransactionStore(transactionManagerLookup, softLockFactory, transactionIDFactory, this, clusteredStore,
                    copyStrategy);
        } else {
            wrappedStore = clusteredStore;
        }

        return wrappedStore;
    }

    private CacheCluster getCacheCluster() {
        CacheCluster cacheCluster;
        try {
            cacheCluster = getCacheManager().getCluster(ClusterScheme.TERRACOTTA);
        } catch (ClusterSchemeNotAvailableException e) {
            LOG.info("Terracotta ClusterScheme is not available, using ClusterScheme.NONE");
            cacheCluster = getCacheManager().getCluster(ClusterScheme.NONE);
        }
        return cacheCluster;
    }

    /**
     * The CacheWriterManager's initialisation can be deferred until an actual CacheWriter has been registered.
     * <p/>
     * This allows users to register a cache through XML in the cache manager and still specify the CacheWriter manually through Java code, possibly referencing local resources.
     *
     * @param imperative indicates whether it's imperative for the cache writer manager to be initialised before operations can continue
     * @throws CacheException when the CacheWriterManager couldn't be initialised but it was imperative to do so
     */
    private void initialiseCacheWriterManager(boolean imperative) throws CacheException {
        if (!cacheWriterManagerInitFlag.get()) {
            cacheWriterManagerInitLock.lock();
            try {
                if (!cacheWriterManagerInitFlag.get()) {
                    if (cacheWriterManager != null && registeredCacheWriter != null) {
                        cacheWriterManager.init(this);
                        cacheWriterManagerInitFlag.set(true);
                    } else if (imperative) {
                        throw new CacheException("Cache: " + configuration.getName() + " was being used with cache writer " +
                                "features, but it wasn't properly registered beforehand.");
                    }
                }
            } finally {
                cacheWriterManagerInitLock.unlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public CacheWriterManager getWriterManager() {
        return cacheWriterManager;
    }

    /**
     * Creates a disk store when either:
     * <ol>
     * <li>overflowToDisk is enabled
     * <li>diskPersistent is enabled
     * </ol>
     *
     * @return the disk store
     */
    protected DiskStore createDiskStore() {
        if (isDiskStore()) {
            return DiskStore.create(this);
        } else {
            return null;
        }
    }

    /**
     * Whether this cache uses a disk store
     *
     * @return true if the cache either overflows to disk or uses a local-classic persistence strategy.
     */
    protected boolean isDiskStore() {
        return configuration.isOverflowToDisk();
    }

    /**
     * Indicates whether this cache is clustered by Terracotta
     *
     * @return {@code true} when the cache is clustered by Terracotta; or {@code false} otherwise
     */
    public boolean isTerracottaClustered() {
        return configuration.isTerracottaClustered();
    }

    /**
     * Bootstrap command. This must be called after the Cache is initialised, during
     * CacheManager initialisation. If loads are synchronous, they will complete before the CacheManager
     * initialise completes, otherwise they will happen in the background.
     */
    public void bootstrap() {
        if (!disabled && bootstrapCacheLoader != null) {
            bootstrapCacheLoader.load(this);
        }

    }

    /**
     * Put an element in the cache.
     * <p/>
     * Resets the access statistics on the element, which would be the case if it has previously been
     * gotten from a cache, and is now being put back.
     * <p/>
     * Also notifies the CacheEventListener that:
     * <ul>
     * <li>the element was put, but only if the Element was actually put.
     * <li>if the element exists in the cache, that an update has occurred, even if the element would be expired
     * if it was requested
     * </ul>
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those circumstances.
     *
     * @param element A cache Element. If Serializable it can fully participate in replication and the DiskStore. If it is
     *                <code>null</code> or the key is <code>null</code>, it is ignored as a NOOP.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @throws CacheException
     */
    public final void put(Element element) throws IllegalArgumentException, IllegalStateException,
            CacheException {
        put(element, false);
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Collection<Element> elements) throws IllegalArgumentException, IllegalStateException, CacheException {
        putAll(elements, false);
    }


    /**
     * Put an element in the cache.
     * <p/>
     * Resets the access statistics on the element, which would be the case if it has previously been
     * gotten from a cache, and is now being put back.
     * <p/>
     * Also notifies the CacheEventListener that:
     * <ul>
     * <li>the element was put, but only if the Element was actually put.
     * <li>if the element exists in the cache, that an update has occurred, even if the element would be expired
     * if it was requested
     * </ul>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those circumstances.
     *
     * @param element                     A cache Element. If Serializable it can fully participate in replication and the DiskStore. If it is
     *                                    <code>null</code> or the key is <code>null</code>, it is ignored as a NOOP.
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @throws IllegalStateException    if the cache is not {@link Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public final void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        putInternal(element, doNotNotifyCacheReplicators, false);
    }

    /**
     * {@inheritDoc}
     */
    private void putAll(Collection<Element> elements, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        putAllInternal(elements, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public void putWithWriter(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        putInternal(element, false, true);
    }

    private void putInternal(Element element, boolean doNotNotifyCacheReplicators, boolean useCacheWriter) {
        if (useCacheWriter) {
            initialiseCacheWriterManager(true);
        }

        checkStatus();

        if (disabled) {
            return;
        }

        if (element == null) {
            if (doNotNotifyCacheReplicators) {

                LOG.debug("Element from replicated put is null. This happens because the element is a SoftReference" +
                        " and it has been collected. Increase heap memory on the JVM or set -Xms to be the same as " +
                        "-Xmx to avoid this problem.");

            }
            //nulls are ignored
            return;
        }


        if (element.getObjectKey() == null) {
            //nulls are ignored
            return;
        }

        element.resetAccessStatistics();

        applyDefaultsToElementWithoutLifespanSet(element);

        backOffIfDiskSpoolFull();
        element.updateUpdateStatistics();
        if (useCacheWriter) {
            boolean elementExists = false;
            boolean notifyListeners = true;
            try {
                elementExists = !compoundStore.putWithWriter(element, cacheWriterManager);
            } catch (StoreUpdateException e) {
                elementExists = e.isUpdate();
                notifyListeners = configuration.getCacheWriterConfiguration().getNotifyListenersOnException();
                RuntimeException cause = e.getCause();
                if (cause instanceof CacheWriterManagerException) {
                    throw ((CacheWriterManagerException)cause).getCause();
                }
                throw cause;
            } finally {
                if (notifyListeners) {
                    notifyPutInternalListeners(element, doNotNotifyCacheReplicators, elementExists);
                }
            }
        } else {
            boolean elementExists = !compoundStore.put(element);
            notifyPutInternalListeners(element, doNotNotifyCacheReplicators, elementExists);
        }
    }

    private void putAllInternal(Collection<Element> elements, boolean doNotNotifyCacheReplicators) {
        checkStatus();

        if (disabled || elements.isEmpty()) {
            return;
        }

        backOffIfDiskSpoolFull();

        compoundStore.putAll(elements);
        for (Element element : elements) {
            element.resetAccessStatistics();
            applyDefaultsToElementWithoutLifespanSet(element);
            notifyPutInternalListeners(element, doNotNotifyCacheReplicators, false);
        }
    }

    private void notifyPutInternalListeners(Element element, boolean doNotNotifyCacheReplicators, boolean elementExists) {
        if (elementExists) {
            registeredEventListeners.notifyElementUpdated(element, doNotNotifyCacheReplicators);
        } else {
            registeredEventListeners.notifyElementPut(element, doNotNotifyCacheReplicators);
        }
    }

    /**
     * wait outside of synchronized block so as not to block readers
     * If the disk store spool is full wait a short time to give it a chance to
     * catch up.
     * todo maybe provide a warning if this is continually happening or monitor via JMX
     */
    private void backOffIfDiskSpoolFull() {

        if (compoundStore.bufferFull()) {
            // back off to avoid OutOfMemoryError
            try {
                Thread.sleep(BACK_OFF_TIME_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void applyDefaultsToElementWithoutLifespanSet(Element element) {
        if (!element.isLifespanSet()) {
            element.setLifespanDefaults(TimeUtil.convertTimeToInt(configuration.getTimeToIdleSeconds()),
                    TimeUtil.convertTimeToInt(configuration.getTimeToLiveSeconds()),
                    configuration.isEternal());
        }
    }

    /**
     * Put an element in the cache, without updating statistics, or updating listeners. This is meant to be used
     * in conjunction with {@link #getQuiet}.
     * Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those circumstances.
     * <p/>
     *
     * @param element A cache Element. If Serializable it can fully participate in replication and the DiskStore. If it is
     *                <code>null</code> or the key is <code>null</code>, it is ignored as a NOOP.
     * @throws IllegalStateException    if the cache is not {@link Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public final void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException,
            CacheException {
        checkStatus();

        if (disabled) {
            return;
        }

        if (element == null || element.getObjectKey() == null) {
            //nulls are ignored
            return;
        }

        applyDefaultsToElementWithoutLifespanSet(element);

        compoundStore.put(element);
    }

    /**
     * Gets an element from the cache. Updates Element Statistics
     * <p/>
     * Note that the Element's lastAccessTime is always the time of this get.
     * Use {@link #getQuiet(Object)} to peak into the Element to see its last access time with get
     * <p/>
     * Synchronization is handled within the method.
     *
     * @param key a serializable value. Null keys are not stored so get(null) always returns null
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see #isExpired
     */
    public final Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }


    /**
     * Gets an element from the cache. Updates Element Statistics
     * <p/>
     * Note that the Element's lastAccessTime is always the time of this get.
     * Use {@link #getQuiet(Object)} to peak into the Element to see its last access time with get
     * <p/>
     * Synchronization is handled within the method.
     *
     * @param key an Object value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see #isExpired
     * @since 1.2
     */
    public final Element get(Object key) throws IllegalStateException, CacheException {
        checkStatus();

        if (disabled) {
            return null;
        }

        if (isStatisticsEnabled()) {
            long start = System.currentTimeMillis();
            Element element = searchInStoreWithStats(key);
            //todo is this expensive. Maybe ditch.
            long end = System.currentTimeMillis();
            liveCacheStatisticsData.addGetTimeMillis(end - start);
            return element;
        } else {
            return searchInStoreWithoutStats(key, false, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Element> getAll(Collection<?> keys) throws IllegalStateException, CacheException {
        checkStatus();

        if (disabled) {
            return null;
        }

        if (keys.isEmpty()) {
            return Collections.EMPTY_MAP;
        }

        if (isStatisticsEnabled()) {
            long start = System.currentTimeMillis();
            Map<Object, Element> elements = searchAllInStoreWithStats(keys);
            liveCacheStatisticsData.addGetTimeMillis(System.currentTimeMillis() - start);
            return elements;
        } else {
            return searchAllInStoreWithoutStats(keys);
        }
    }

    /**
     * This method will return, from the cache, the Element associated with the argument "key".
     * <p/>
     * If the Element is not in the cache, the associated cache loader will be called. That is either the CacheLoader passed in, or if null,
     * the one associated with the cache. If both are null, no load is performed and null is returned.
     * <p/>
     * If the loader decides to assign a null value to the Element, an Element with a null value is created and stored in the cache.
     * <p/>
     * Because this method may take a long time to complete, it is not synchronized. The underlying cache operations
     * are synchronized.
     *
     * @param key            key whose associated value is to be returned.
     * @param loader         the override loader to use. If null, the cache's default loader will be used
     * @param loaderArgument an argument to pass to the CacheLoader.
     * @return an element if it existed or could be loaded, otherwise null
     * @throws CacheException
     */
    public Element getWithLoader(Object key, CacheLoader loader, Object loaderArgument) throws CacheException {

        Element element = get(key);
        if (element != null) {
            return element;
        }

        if (registeredCacheLoaders.size() == 0 && loader == null) {
            return null;
        }

        try {
            //check again in case the last thread loaded it
            element = getQuiet(key);
            if (element != null) {
                return element;
            }

            //wait for result
            long cacheLoaderTimeoutMillis = configuration.getCacheLoaderTimeoutMillis();
            final Object value;
            if (cacheLoaderTimeoutMillis > 0) {
                final Future<AtomicReference<Object>> future = asynchronousLoad(key, loader, loaderArgument);
                value = future.get(cacheLoaderTimeoutMillis, TimeUnit.MILLISECONDS).get();
            } else {
                value = loadValueUsingLoader(key, loader, loaderArgument);
            }
            if (value != null) {
                put(new Element(key, value), false);
            }
        } catch (TimeoutException e) {
            throw new LoaderTimeoutException("Timeout on load for key " + key, e);
        } catch (Exception e) {
            throw new CacheException("Exception on load for key " + key, e);
        }
        return getQuiet(key);
    }

    /**
     * The load method provides a means to "pre-load" the cache. This method will, asynchronously, load the specified
     * object into the cache using the associated CacheLoader. If the object already exists in the cache, no action is
     * taken. If no loader is associated with the object, no object will be loaded into the cache. If a problem is
     * encountered during the retrieving or loading of the object, an exception should be logged. If the "arg" argument
     * is set, the arg object will be passed to the CacheLoader.load method. The cache will not dereference the object.
     * If no "arg" value is provided a null will be passed to the load method. The storing of null values in the cache
     * is permitted, however, the get method will not distinguish returning a null stored in the cache and not finding
     * the object in the cache. In both cases a null is returned.
     * <p/>
     * The Ehcache native API provides similar functionality to loaders using the
     * decorator {@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}
     *
     * @param key key whose associated value to be loaded using the associated CacheLoader if this cache doesn't contain it.
     * @throws CacheException
     */
    public void load(final Object key) throws CacheException {
        if (registeredCacheLoaders.size() == 0) {

            LOG.debug("The CacheLoader is null. Returning.");
            return;
        }

        boolean existsOnCall = isKeyInCache(key);
        if (existsOnCall) {

            LOG.debug("The key {} exists in the cache. Returning.", key);
            return;
        }

        asynchronousPut(key, null, null);
    }

    /**
     * The getAll method will return, from the cache, a Map of the objects associated with the Collection of keys in argument "keys".
     * If the objects are not in the cache, the associated cache loader will be called. If no loader is associated with an object,
     * a null is returned. If a problem is encountered during the retrieving or loading of the objects, an exception will be thrown.
     * If the "arg" argument is set, the arg object will be passed to the CacheLoader.loadAll method. The cache will not dereference
     * the object. If no "arg" value is provided a null will be passed to the loadAll method. The storing of null values in the cache
     * is permitted, however, the get method will not distinguish returning a null stored in the cache and not finding the object in
     * the cache. In both cases a null is returned.
     * <p/>
     * <p/>
     * Note. If the getAll exceeds the maximum cache size, the returned map will necessarily be less than the number specified.
     * <p/>
     * Because this method may take a long time to complete, it is not synchronized. The underlying cache operations
     * are synchronized.
     * <p/>
     * The constructs package provides similar functionality using the
     * decorator {@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}
     *
     * @param keys           a collection of keys to be returned/loaded
     * @param loaderArgument an argument to pass to the CacheLoader.
     * @return a Map populated from the Cache. If there are no elements, an empty Map is returned.
     * @throws CacheException
     */
    public Map getAllWithLoader(Collection keys, Object loaderArgument) throws CacheException {
        if (keys == null) {
            return new HashMap(0);
        }
        Map<Object, Object> map = new HashMap<Object, Object>(keys.size());

        List<Object> missingKeys = new ArrayList<Object>(keys.size());

        if (registeredCacheLoaders.size() > 0) {
            Object key = null;
            try {
                map = new HashMap<Object, Object>(keys.size());

                for (Object key1 : keys) {
                    key = key1;
                    Element element = get(key);

                    if (element != null) {
                        map.put(key, element.getObjectValue());
                    } else if (isKeyInCache(key)) {
                        map.put(key, null);
                    } else {
                        missingKeys.add(key);
                    }
                }

                //now load everything that's missing.
                Future future = asynchronousLoadAll(missingKeys, loaderArgument);

                //wait for result
                long cacheLoaderTimeoutMillis = configuration.getCacheLoaderTimeoutMillis();
                if (cacheLoaderTimeoutMillis > 0) {
                    try {
                        future.get(cacheLoaderTimeoutMillis, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        throw new LoaderTimeoutException("Timeout on load for key " + key, e);
                    }
                } else {
                    future.get();
                }


                for (Object missingKey : missingKeys) {
                    key = missingKey;
                    Element element = get(key);
                    if (element != null) {
                        map.put(key, element.getObjectValue());
                    } else {
                        map.put(key, null);
                    }
                }

            } catch (InterruptedException e) {
                throw new CacheException(e.getMessage() + " for key " + key, e);
            } catch (ExecutionException e) {
                throw new CacheException(e.getMessage() + " for key " + key, e);
            }
        } else {
            for (Object key : keys) {
                Element element = get(key);
                if (element != null) {
                    map.put(key, element.getObjectValue());
                } else {
                    map.put(key, null);
                }
            }
        }
        return map;
    }


    /**
     * The loadAll method provides a means to "pre load" objects into the cache. This method will, asynchronously, load
     * the specified objects into the cache using the associated cache loader(s). If the an object already exists in the
     * cache, no action is taken. If no loader is associated with the object, no object will be loaded into the cache.
     * If a problem is encountered during the retrieving or loading of the objects, an exception (to be defined)
     * should be logged. The getAll method will return, from the cache, a Map of the objects associated with the
     * Collection of keys in argument "keys". If the objects are not in the cache, the associated cache loader will be
     * called. If no loader is associated with an object, a null is returned. If a problem is encountered during the
     * retrieving or loading of the objects, an exception (to be defined) will be thrown. If the "arg" argument is set,
     * the arg object will be passed to the CacheLoader.loadAll method. The cache will not dereference the object.
     * If no "arg" value is provided a null will be passed to the loadAll method.
     * <p/>
     * keys - collection of the keys whose associated values to be loaded into this cache by using the associated
     * CacheLoader if this cache doesn't contain them.
     * <p/>
     * The Ehcache native API provides similar functionality to loaders using the
     * decorator {@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}
     */
    public void loadAll(final Collection keys, final Object argument) throws CacheException {

        if (registeredCacheLoaders.size() == 0) {

            LOG.debug("The CacheLoader is null. Returning.");
            return;
        }
        if (keys == null) {
            return;
        }
        asynchronousLoadAll(keys, argument);
    }

    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * still updated. Listeners are not called.
     * <p/>
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see #isExpired
     */
    public final Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * not updated.
     * <p/>
     * Listeners are not called.
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see #isExpired
     * @since 1.2
     */
    public final Element getQuiet(Object key) throws IllegalStateException, CacheException {
        checkStatus();
        return searchInStoreWithoutStats(key, true, false);
    }

    /**
     * Returns a list of all element keys in the cache, whether or not they are expired.
     * <p/>
     * The returned keys are unique and can almost be considered a set. See {@link net.sf.ehcache.store.CacheKeySet CacheKeySet} for
     * more details.
     * <p/>
     * The List returned is not live. It is a copy.
     * <p/>
     * The time taken is O(n). On a single CPU 1.8Ghz P4, approximately 8ms is required
     * for each 1000 entries.
     *
     * @return a list of {@link Object} keys
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see net.sf.ehcache.store.CacheKeySet
     */
    public final List getKeys() throws IllegalStateException, CacheException {
        checkStatus();
        return compoundStore.getKeys();
    }

    /**
     * Returns a list of all element keys in the cache. Only keys of non-expired
     * elements are returned.
     * <p/>
     * The returned keys are unique and can be considered a set.
     * <p/>
     * The List returned is not live. It is a copy.
     * <p/>
     * The time taken is O(n), where n is the number of elements in the cache. On
     * a 1.8Ghz P4, the time taken is approximately 200ms per 1000 entries. This method
     * is not synchronized, because it relies on a non-live list returned from {@link #getKeys()}
     * , which is synchronised, and which takes 8ms per 1000 entries. This way
     * cache liveness is preserved, even if this method is very slow to return.
     * <p/>
     * Consider whether your usage requires checking for expired keys. Because
     * this method takes so long, depending on cache settings, the list could be
     * quite out of date by the time you get it.
     *
     * @return a list of {@link Object} keys
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
         List allKeyList = getKeys();
         //removeInternal keys of expired elements
         ArrayList < Object > nonExpiredKeys = new ArrayList(allKeyList.size());
         for (Iterator iter = allKeyList.iterator(); iter.hasNext();) {
             Object key = iter.next();
             Element element = getQuiet(key);
             if (element != null) {
                 nonExpiredKeys.add(key);
             }
         }
         nonExpiredKeys.trimToSize();
         return nonExpiredKeys;
    }


    /**
     * Returns a list of all elements in the cache, whether or not they are expired.
     * <p/>
     * The returned keys are not unique and may contain duplicates. If the cache is only
     * using the memory store, the list will be unique. If the disk store is being used
     * as well, it will likely contain duplicates, because of the internal store design.
     * <p/>
     * The List returned is not live. It is a copy.
     * <p/>
     * The time taken is O(log n). On a single CPU 1.8Ghz P4, approximately 6ms is required
     * for 1000 entries and 36 for 50000.
     * <p/>
     * This is the fastest getKeys method
     *
     * @return a list of {@link Object} keys
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final List getKeysNoDuplicateCheck() throws IllegalStateException {
        checkStatus();
        return getKeys();
    }

    private Element searchInStoreWithStats(Object key) {
        boolean wasInMemory = compoundStore.containsKeyInMemory(key);
        boolean wasOffHeap = false;
        boolean hasOffHeap = getCacheConfiguration().isOverflowToOffHeap();
        boolean isTCClustered = getCacheConfiguration().isTerracottaClustered();
        boolean hasOnDisk = isTCClustered || getCacheConfiguration().isOverflowToDisk();
        Element element;

        if (!wasInMemory) {
            liveCacheStatisticsData.cacheMissInMemory();
            if (hasOffHeap) {
                wasOffHeap = compoundStore.containsKeyOffHeap(key);
            }
        }

        element = compoundStore.get(key);

        if (element != null) {
            if (isExpired(element)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(configuration.getName() + " cache hit, but element expired");
                }
                liveCacheStatisticsData.cacheMissExpired();
                tryRemoveImmediately(key, true);
                element = null;
            } else {
                element.updateAccessStatistics();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cache: " + getName() + " store hit for " + key);
                }

                if (wasInMemory) {
                    liveCacheStatisticsData.cacheHitInMemory();
                } else if (wasOffHeap) {
                    liveCacheStatisticsData.cacheHitOffHeap();
                } else if (hasOffHeap) {
                    liveCacheStatisticsData.cacheMissOffHeap();
                    liveCacheStatisticsData.cacheHitOnDisk();
                } else {
                    liveCacheStatisticsData.cacheHitOnDisk();
                }
            }
        } else {
            liveCacheStatisticsData.cacheMissNotFound();
            if (hasOffHeap) {
                liveCacheStatisticsData.cacheMissOffHeap();
            }
            if (hasOnDisk) {
                liveCacheStatisticsData.cacheMissOnDisk();
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug(configuration.getName() + " cache - Miss");
            }
        }
        return element;
    }

    private Map<Object, Element> searchAllInStoreWithStats(Collection<?> keys) {
        boolean wasOnDisk = false;
        boolean wasOffHeap = false;
        boolean hasOffHeap = getCacheConfiguration().isOverflowToOffHeap();
        boolean isTCClustered = getCacheConfiguration().isTerracottaClustered();
        boolean hasOnDisk = isTCClustered || getCacheConfiguration().isOverflowToDisk();
        Map<Object, Element> elements;
        Map<Object, Boolean> wasOffHeapMap = new HashMap<Object, Boolean>();
        Map<Object, Boolean> wasOnDiskMap = new HashMap<Object, Boolean>();

        for (Object key : keys) {
            if (!compoundStore.containsKeyInMemory(key)) {
                liveCacheStatisticsData.cacheMissInMemory();
                if (hasOffHeap) {
                    wasOffHeap = compoundStore.containsKeyOffHeap(key);
                    wasOffHeapMap.put(key, wasOffHeap);
                }
              if (!wasOffHeap) {
                  if (hasOffHeap) {
                      liveCacheStatisticsData.cacheMissOffHeap();
                  }
                  wasOnDisk = compoundStore.containsKeyOnDisk(key);
                  wasOnDiskMap.put(key, wasOnDisk);
                  if (hasOnDisk && !wasOnDisk) {
                      liveCacheStatisticsData.cacheMissOnDisk();
                  }
              }
            }
        }
        elements = compoundStore.getAll(keys);

        for (Entry<Object, Element> entry : elements.entrySet()) {
            Object key = entry.getKey();
            Element element = entry.getValue();
            if (element != null) {
                if (isExpired(element)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(configuration.getName() + " cache hit, but element expired");
                    }
                    liveCacheStatisticsData.cacheMissExpired();
                    tryRemoveImmediately(key, true);
                    element = null;
                } else {
                    element.updateAccessStatistics();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Cache: " + getName() + " store hit for " + key);
                    }

                    if (wasOffHeapMap.containsKey(key) && wasOffHeapMap.get(key)) {
                        liveCacheStatisticsData.cacheHitOffHeap();
                    } else if (wasOnDiskMap.containsKey(key) && wasOnDiskMap.get(key)) {
                        liveCacheStatisticsData.cacheHitOnDisk();
                    } else {
                        liveCacheStatisticsData.cacheHitInMemory();
                    }
                }
            } else {
                liveCacheStatisticsData.cacheMissNotFound();
                if (LOG.isDebugEnabled()) {
                    LOG.debug(configuration.getName() + " cache - Miss");
                }
            }
            elements.put(key, element);
        }
        return elements;
    }

    private Element searchInStoreWithoutStats(Object key, boolean quiet, boolean notifyListeners) {
        Element element = compoundStore.getQuiet(key);

        return elementStatsHelper(key, quiet, notifyListeners, element);
    }

    private Element elementStatsHelper(Object key, boolean quiet, boolean notifyListeners, Element element) {
        if (element != null) {
            if (isExpired(element)) {
                tryRemoveImmediately(key, notifyListeners);
                element = null;
            } else if (!(quiet || skipUpdateAccessStatistics(element))) {
                element.updateAccessStatistics();
            }
        }
        return element;
    }


    private Map<Object, Element> searchAllInStoreWithoutStats(Collection<?> keys) {
        Map<Object, Element> elements = compoundStore.getAllQuiet(keys);

        for (Entry<Object, Element> entry : elements.entrySet()) {
            Element element = entry.getValue();
            Object key = entry.getKey();
            elements.put(key, elementStatsHelper(key, false, true, element));
        }
        return elements;
    }

    private void tryRemoveImmediately(final Object key, final boolean notifyListeners) {
        Sync syncForKey = ((CacheLockProvider)getInternalContext()).getSyncForKey(key);
        boolean writeLocked = false;
        try {
            writeLocked = syncForKey.tryLock(LockType.WRITE, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (LockOperationTimedOutNonstopException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Try lock attempt failed, inline expiry will not happen. Exception: " + e);
            }
        } catch (Error e) {
            if (!(e.getClass().getName().equals("com.tc.exception.TCLockUpgradeNotSupportedError"))) {
               throw e;
            }
        }
        if (writeLocked) {
            try {
                removeInternal(key, true, notifyListeners, false, false);
            } finally {
                syncForKey.unlock(LockType.WRITE);
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(configuration.getName() + " cache: element " + key + " expired, but couldn't be inline evicted");
            }
        }
    }

    private boolean skipUpdateAccessStatistics(Element element) {
      return configuration.isFrozen() && element.isEternal()
              && (configuration.getMaxElementsInMemory() == 0)
              && (!configuration.isOverflowToDisk() || configuration.getMaxElementsOnDisk() == 0);
    }

    /**
     * Removes an {@link Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed.
     * <p/>
     * Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those circumstances.
     *
     * @param key the element key to operate on
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final boolean remove(Serializable key) throws IllegalStateException {
        return remove((Object) key);
    }

    /**
     * Removes an {@link Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element
     * with the key actually existed.
     * <p/>
     * Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those circumstances.
     * <p/>
     *
     * @param key the element key to operate on
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @since 1.2
     */
    public final boolean remove(Object key) throws IllegalStateException {
        return remove(key, false);
    }

    /**
     * Removes an {@link Element} from the Cache and returns it. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element with the key actually existed.
     * <p/>
     * Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails. This exception
     * should be caught in those circumstances.
     * <p/>
     *
     * @param key the element key to operate on
     * @return element the removed element associated with the key, null if no mapping exists
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final Element removeAndReturnElement(Object key) throws IllegalStateException {
        return removeInternal(key, false, true, false, false);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(final Collection<?> keys) throws IllegalStateException {
        removeAll(keys, false);
    }

    /**
     * {@inheritDoc}
    */
    public final void removeAll(final Collection<?> keys, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        removeAllInternal(keys, false, true, doNotNotifyCacheReplicators);
    }

    /**
     * Removes an {@link Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element
     * with the key actually existed.
     * <p/>
     * Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those circumstances.
     *
     * @param key                         the element key to operate on
     * @param doNotNotifyCacheReplicators whether the remove is coming from a doNotNotifyCacheReplicators cache peer, in which case this remove should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove((Object) key, doNotNotifyCacheReplicators);
    }

    /**
     * Removes an {@link Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element
     * with the key actually existed.
     * <p/>
     * Synchronization is handled within the method.
     *
     * @param key                         the element key to operate on
     * @param doNotNotifyCacheReplicators whether the remove is coming from a doNotNotifyCacheReplicators cache peer, in which case this remove should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return (removeInternal(key, false, true, doNotNotifyCacheReplicators, false) != null);
    }

    /**
     * Removes an {@link Element} from the Cache, without notifying listeners. This also removes it from any
     * stores it may be in.
     * <p/>
     * Listeners are not called.
     *
     * @param key the element key to operate on
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final boolean removeQuiet(Serializable key) throws IllegalStateException {
       return (removeInternal(key, false, false, false, false) != null);
    }

    /**
     * Removes an {@link Element} from the Cache, without notifying listeners. This also removes it from any
     * stores it may be in.
     * <p/>
     * Listeners are not called.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails. This exception
     * should be caught in those circumstances.
     *
     * @param key the element key to operate on
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @since 1.2
     */
    public final boolean removeQuiet(Object key) throws IllegalStateException {
        return (removeInternal(key, false, false, false, false) != null);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeWithWriter(Object key) throws IllegalStateException {
        return (removeInternal(key, false, true, false, true) != null);
    }

    /**
     * Removes or expires an {@link Element} from the Cache after an attempt to get it determined that it should be expired.
     * This also removes it from any stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element has expired.
     * <p/>
     * Synchronization is handled within the method.
     * <p/>
     * If a remove was called, listeners are notified, regardless of whether the element existed or not.
     * This allows distributed cache listeners to remove elements from a cluster regardless of whether they
     * existed locally.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those circumstances.
     *
     * @param key                         the element key to operate on
     * @param expiry                      if the reason this method is being called is to expire the element
     * @param notifyListeners             whether to notify listeners
     * @param doNotNotifyCacheReplicators whether not to notify cache replicators
     * @param useCacheWriter              if the element should else be removed from the cache writer
     * @return element if the element was removed, null if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    private Element removeInternal(Object key, boolean expiry, boolean notifyListeners,
                           boolean doNotNotifyCacheReplicators, boolean useCacheWriter)
            throws IllegalStateException {

        if (useCacheWriter) {
            initialiseCacheWriterManager(true);
        }

        checkStatus();
        Element elementFromStore = null;

        if (useCacheWriter) {
            try {
                elementFromStore = compoundStore.removeWithWriter(key, cacheWriterManager);
            } catch (CacheWriterManagerException e) {
                if (configuration.getCacheWriterConfiguration().getNotifyListenersOnException()) {
                    notifyRemoveInternalListeners(key, expiry, notifyListeners, doNotNotifyCacheReplicators,
                            elementFromStore);
                }
                throw e.getCause();
            }
        } else {
            elementFromStore = compoundStore.remove(key);
        }

        notifyRemoveInternalListeners(key, expiry, notifyListeners, doNotNotifyCacheReplicators,
            elementFromStore);

        return elementFromStore;
    }

    private boolean notifyRemoveInternalListeners(Object key, boolean expiry, boolean notifyListeners, boolean doNotNotifyCacheReplicators,
                                                  Element elementFromStore) {
        boolean removed = false;
        boolean removeNotified = false;

        if (elementFromStore != null) {
            if (expiry) {
                //always notify expire which is lazy regardless of the removeQuiet
                registeredEventListeners.notifyElementExpiry(elementFromStore, doNotNotifyCacheReplicators);
            } else if (notifyListeners) {
                removeNotified = true;
                registeredEventListeners.notifyElementRemoved(elementFromStore, doNotNotifyCacheReplicators);
            }
            removed = true;
        }

        //If we are trying to remove an element which does not exist locally, we should still notify so that
        //cluster invalidations work.
        if (notifyListeners && !expiry && !removeNotified) {
            Element syntheticElement = new Element(key, null);
            registeredEventListeners.notifyElementRemoved(syntheticElement, doNotNotifyCacheReplicators);
        }

        return removed;
    }

    /**
     * Removes or expires a collection of {@link Element}s from the Cache after an attempt to get it determined that it should be expired.
     * This also removes it from any stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element has expired.
     * <p/>
     * Synchronization is handled within the method.
     * <p/>
     * If a removeAll was called, listeners are notified, regardless of whether the element existed or not. This allows distributed cache
     * listeners to remove elements from a cluster regardless of whether they existed locally.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails. This exception
     * should be caught in those circumstances.
     *
     * @param keys a collection of keys to operate on
     * @param expiry if the reason this method is being called is to expire the element
     * @param notifyListeners whether to notify listeners
     * @param doNotNotifyCacheReplicators whether not to notify cache replicators
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    private void removeAllInternal(final Collection<?> keys, boolean expiry, boolean notifyListeners,
            boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        checkStatus();

        if (disabled || keys.isEmpty()) {
            return;
        }

        compoundStore.removeAll(keys);
        for (Object key : keys) {
            Element syntheticElement = new Element(key, null);
            notifyRemoveInternalListeners(key, false, notifyListeners, doNotNotifyCacheReplicators, syntheticElement);
        }
    }

    /**
     * Removes all cached items.
     * Terracotta clustered caches may require more time to execute this operation because cached items must also be removed from the Terracotta Server Array. Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those circumstances.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        removeAll(false);
    }


    /**
     * Removes all cached items.
     * Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those circumstances.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        checkStatus();
        compoundStore.removeAll();
        registeredEventListeners.notifyRemoveAll(doNotNotifyCacheReplicators);
    }

    /**
     * Starts an orderly shutdown of the Cache. Steps are:
     * <ol>
     * <li>Completes any outstanding CacheLoader loads.
     * <li>Completes any outstanding CacheWriter operations.
     * <li>Disposes any cache extensions.
     * <li>Disposes any cache event listeners. The listeners normally complete, so for example distributed caching operations will complete.
     * <li>Flushes all cache items from memory to the disk store, if any
     * <li>changes status to shutdown, so that any cache operations after this point throw IllegalStateException
     * </ol>
     * This method should be invoked only by CacheManager, as a cache's lifecycle is bound into that of it's cache manager.
     *
     * @throws IllegalStateException if the cache is already {@link Status#STATUS_SHUTDOWN}
     */
    public synchronized void dispose() throws IllegalStateException {
        if (checkStatusAlreadyDisposed()) {
            return;
        }

        if (bootstrapCacheLoader != null && bootstrapCacheLoader instanceof Disposable) {
            ((Disposable)bootstrapCacheLoader).dispose();
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        disposeRegisteredCacheExtensions();
        disposeRegisteredCacheLoaders();
        disposeRegisteredCacheWriter();
        registeredEventListeners.dispose();

        if (cacheWriterManager != null) {
            cacheWriterManager.dispose();
        }

        if (compoundStore != null) {
            compoundStore.removeStoreListener(this);
            compoundStore.dispose();
            // null compoundStore explicitly to help gc (particularly for offheap)
            compoundStore = null;
        }

        if (xaResource != null) {
            transactionManagerLookup.unregister(xaResource);
            xaResource = null;
        }

        // null the lockProvider too explicitly to help gc
        lockProvider = null;

        if (cacheManager != null) {
            cacheManager.getCacheRejoinAction().unregister(this);
        }
        cacheStatus.changeState(Status.STATUS_SHUTDOWN);
    }

    private void initialiseRegisteredCacheExtensions() {
        for (CacheExtension cacheExtension : registeredCacheExtensions) {
            cacheExtension.init();
        }
    }

    private void disposeRegisteredCacheExtensions() {
        for (CacheExtension cacheExtension : registeredCacheExtensions) {
            cacheExtension.dispose();
        }
    }

    private void initialiseRegisteredCacheLoaders() {
        for (CacheLoader cacheLoader : registeredCacheLoaders) {
            cacheLoader.init();
        }
    }

    private void disposeRegisteredCacheLoaders() {
        for (CacheLoader cacheLoader : registeredCacheLoaders) {
            cacheLoader.dispose();
        }
    }

    private void initialiseRegisteredCacheWriter() {
        CacheWriter writer = registeredCacheWriter;
        if (writer != null) {
            writer.init();
        }
    }

    private void disposeRegisteredCacheWriter() {
        CacheWriter writer = registeredCacheWriter;
        if (writer != null) {
            writer.dispose();
        }
    }

    /**
     * Gets the cache configuration this cache was created with.
     * <p/>
     * Things like listeners that are added dynamically are excluded.
     */
    public CacheConfiguration getCacheConfiguration() {
        return configuration;
    }


    /**
     * Flushes all cache items from memory to the disk store, and from the DiskStore to disk.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final synchronized void flush() throws IllegalStateException, CacheException {
        checkStatus();
        try {
            compoundStore.flush();
        } catch (IOException e) {
            throw new CacheException("Unable to flush cache: " + configuration.getName()
                    + ". Initial cause was " + e.getMessage(), e);
        }
    }

    /**
     * Gets the size of the cache. This is a subtle concept. See below.
     * <p/>
     * This number is the actual number of elements, including expired elements
     * that have not been removed.
     * <p/>
     * Expired elements are removed from the the memory store when getting an
     * expired element, or when attempting to spool an expired element to disk.
     * <p/>
     * Expired elements are removed from the disk store when getting an expired
     * element, or when the expiry thread runs, which is once every five
     * minutes.
     * <p/>
     * To get an exact size, which would exclude expired elements, use
     * {@link #getKeysWithExpiryCheck()}.size(), although see that method for
     * the approximate time that would take.
     * <p/>
     * To get a very fast result, use {@link #getKeysNoDuplicateCheck()}.size().
     * If the disk store is being used, there will be some duplicates.
     * <p/>
     * Note:getSize() is a very expensive operation in off-heap, disk and Terracotta implementations.
     *
     * @return The size value
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final int getSize() throws IllegalStateException, CacheException {
        checkStatus();

        if (isTerracottaClustered()) {
            return compoundStore.getTerracottaClusteredSize();
        } else {
            return compoundStore.getSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getSizeBasedOnAccuracy(int statisticsAccuracy)
            throws IllegalStateException, CacheException {
        if (statisticsAccuracy == Statistics.STATISTICS_ACCURACY_BEST_EFFORT) {
            return getSize();
        } else if (statisticsAccuracy == Statistics.STATISTICS_ACCURACY_GUARANTEED) {
            return getKeysWithExpiryCheck().size();
        } else if (statisticsAccuracy == Statistics.STATISTICS_ACCURACY_NONE) {
            return getKeysNoDuplicateCheck().size();
        }
        throw new IllegalArgumentException("Unknown statistics accuracy: "
                + statisticsAccuracy);
    }

    /**
     * Gets the size of the memory store for this cache. This method relies on calculating
     * Serialized sizes. If the Element values are not Serializable they will show as zero.
     * <p/>
     * Warning: This method can be very expensive to run. Allow approximately 1 second
     * per 1MB of entries. Running this method could create liveness problems
     * because the object lock is held for a long period
     * <p/>
     *
     * @return the approximate size of the memory store in bytes
     * @throws IllegalStateException
     */
    public final long calculateInMemorySize() throws IllegalStateException, CacheException {
        checkStatus();
        return compoundStore.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAbortedSizeOf() {
        checkStatus();
        return compoundStore.hasAbortedSizeOf();
    }

    /**
     * Gets the size of the off-heap store for this cache.
     *
     * @return the size of the off-heap store in bytes
     * @throws IllegalStateException
     */
    public final long calculateOffHeapSize() throws IllegalStateException, CacheException {
        checkStatus();
        return compoundStore.getOffHeapSizeInBytes();
    }

    /**
     * Gets the size of the on-disk store for this cache
     *
     * @return the size of the on-disk store in bytes
     * @throws IllegalStateException
     */
    public final long calculateOnDiskSize() throws IllegalStateException, CacheException {
        checkStatus();
        return compoundStore.getOnDiskSizeInBytes();
    }

    /**
     * Returns the number of elements in the memory store.
     *
     * @return the number of elements in the memory store
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final long getMemoryStoreSize() throws IllegalStateException {
        checkStatus();
        return compoundStore.getInMemorySize();
    }

    /**
     * Returns the number of elements in the off-heap store.
     *
     * @return the number of elements in the off-heap store
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public long getOffHeapStoreSize() throws IllegalStateException {
        checkStatus();
        return compoundStore.getOffHeapSize();
    }

    /**
     * Returns the number of elements in the disk store.
     *
     * @return the number of elements in the disk store.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final int getDiskStoreSize() throws IllegalStateException {
        checkStatus();
        if (isTerracottaClustered()) {
            return compoundStore.getTerracottaClusteredSize();
        } else {
            return compoundStore.getOnDiskSize();
        }
    }

    /**
     * Gets the status attribute of the Cache.
     *
     * @return The status value from the Status enum class
     */
    public final Status getStatus() {
        return cacheStatus.getStatus();
    }


    private void checkStatus() throws IllegalStateException {
        cacheStatus.checkAlive(configuration);
    }

    private boolean checkStatusAlreadyDisposed() throws IllegalStateException {
        return cacheStatus.isShutdown();
    }


    /**
     * Gets the cache name.
     */
    public final String getName() {
        return configuration.getName();
    }

    /**
     * Sets the cache name which will name.
     *
     * @param name the name of the cache. Should not be null. Should also not contain any '/' characters, as these interfere
     *             with distribution
     * @throws IllegalArgumentException if an illegal name is used.
     */
    public final void setName(String name) throws IllegalArgumentException {
        if (!cacheStatus.isUninitialized()) {
            throw new IllegalStateException("Only uninitialised caches can have their names set.");
        }
        configuration.setName(name);
    }

    /**
     * Returns a {@link String} representation of {@link Cache}.
     */
    @Override
    public String toString() {
        StringBuilder dump = new StringBuilder();

        dump.append("[")
                .append(" name = ").append(configuration.getName())
                .append(" status = ").append(cacheStatus.getStatus())
                .append(" eternal = ").append(configuration.isEternal())
                .append(" overflowToDisk = ").append(configuration.isOverflowToDisk())
                .append(" maxEntriesLocalHeap = ").append(configuration.getMaxEntriesLocalHeap())
                .append(" maxEntriesLocalDisk = ").append(configuration.getMaxEntriesLocalDisk())
                .append(" memoryStoreEvictionPolicy = ").append(configuration.getMemoryStoreEvictionPolicy())
                .append(" timeToLiveSeconds = ").append(configuration.getTimeToLiveSeconds())
                .append(" timeToIdleSeconds = ").append(configuration.getTimeToIdleSeconds())
                .append(" persistence = ").append(configuration.getPersistenceConfiguration() == null ?
                    "none" : configuration.getPersistenceConfiguration().getStrategy())
                .append(" diskExpiryThreadIntervalSeconds = ").append(configuration.getDiskExpiryThreadIntervalSeconds())
                .append(registeredEventListeners)
                .append(" hitCount = ").append(getLiveCacheStatisticsNoCheck().getCacheHitCount())
                .append(" memoryStoreHitCount = ").append(getLiveCacheStatisticsNoCheck().getInMemoryHitCount())
                .append(" diskStoreHitCount = ").append(getLiveCacheStatisticsNoCheck().getOnDiskHitCount())
                .append(" missCountNotFound = ").append(getLiveCacheStatisticsNoCheck().getCacheMissCount())
                .append(" missCountExpired = ").append(getLiveCacheStatisticsNoCheck().getCacheMissCountExpired())
                .append(" maxBytesLocalHeap = ").append(configuration.getMaxBytesLocalHeap())
                .append(" overflowToOffHeap = ").append(configuration.isOverflowToOffHeap())
                .append(" maxBytesLocalOffHeap = ").append(configuration.getMaxBytesLocalOffHeap())
                .append(" maxBytesLocalDisk = ").append(configuration.getMaxBytesLocalDisk())
                .append(" pinned = ")
                .append(configuration.getPinningConfiguration() != null ? configuration.getPinningConfiguration().getStore().name() : "false")
                .append(" ]");

        return dump.toString();
    }


    /**
     * Checks whether this cache element has expired.
     * <p/>
     * The element is expired if:
     * <ol>
     * <li> the idle time is non-zero and has elapsed, unless the cache is eternal; or
     * <li> the time to live is non-zero and has elapsed, unless the cache is eternal; or
     * <li> the value of the element is null.
     * </ol>
     *
     * @return true if it has expired
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @throws NullPointerException  if the element is null
     *                               todo this does not need to be synchronized
     */
    public final boolean isExpired(Element element) throws IllegalStateException, NullPointerException {
        checkStatus();
        return element.isExpired(configuration);
    }


    /**
     * Clones a cache. This is only legal if the cache has not been
     * initialized. At that point only primitives have been set and no
     * stores have been created.
     * <p/>
     * A new, empty, RegisteredEventListeners is created on clone.
     * <p/>
     *
     * @return an object of type {@link Cache}
     * @throws CloneNotSupportedException
     */
    @Override
    public final Cache clone() throws CloneNotSupportedException {
        if (compoundStore != null) {
            throw new CloneNotSupportedException("Cannot clone an initialized cache.");
        }
        Cache copy = (Cache) super.clone();
        // create new copies of the statistics
        copy.liveCacheStatisticsData = new LiveCacheStatisticsWrapper(copy);
        copy.sampledCacheStatistics = new SampledCacheStatisticsWrapper();

        copy.configuration = configuration.clone();
        copy.guid = createGuid();
        copy.cacheStatus = new CacheStatus();
        copy.cacheStatus.changeState(Status.STATUS_UNINITIALISED);
        copy.configuration.getCopyStrategyConfiguration().setCopyStrategyInstance(null);
        copy.elementValueComparator = copy.configuration.getElementValueComparatorConfiguration()
            .getElementComparatorInstance(copy.configuration);
        copy.propertyChangeSupport = new PropertyChangeSupport(copy);
        copy.nonstopActiveDelegateHolder = new NonstopActiveDelegateHolderImpl(copy);
        copy.cacheWriterManagerInitFlag = new AtomicBoolean(false);
        copy.cacheWriterManagerInitLock = new ReentrantLock();
        for (PropertyChangeListener propertyChangeListener : propertyChangeSupport.getPropertyChangeListeners()) {
            copy.addPropertyChangeListener(propertyChangeListener);
        }

        RegisteredEventListeners registeredEventListenersFromCopy = copy.getCacheEventNotificationService();
        if (registeredEventListenersFromCopy == null || registeredEventListenersFromCopy.getCacheEventListeners().size() == 0) {
            copy.registeredEventListeners = new RegisteredEventListeners(copy);
        } else {
            copy.registeredEventListeners = new RegisteredEventListeners(copy);
            Set cacheEventListeners = registeredEventListeners.getCacheEventListeners();
            for (Object cacheEventListener1 : cacheEventListeners) {
                CacheEventListener cacheEventListener = (CacheEventListener) cacheEventListener1;
                CacheEventListener cacheEventListenerClone = (CacheEventListener) cacheEventListener.clone();
                copy.registeredEventListeners.registerListener(cacheEventListenerClone);
            }
        }


        copy.registeredCacheExtensions = new CopyOnWriteArrayList<CacheExtension>();
        for (CacheExtension registeredCacheExtension : registeredCacheExtensions) {
            copy.registerCacheExtension(registeredCacheExtension.clone(copy));
        }

        copy.registeredCacheLoaders = new CopyOnWriteArrayList<CacheLoader>();
        for (CacheLoader registeredCacheLoader : registeredCacheLoaders) {
            copy.registerCacheLoader(registeredCacheLoader.clone(copy));
        }

        if (registeredCacheWriter != null) {
            copy.registerCacheWriter(registeredCacheWriter.clone(copy));
        }

        if (bootstrapCacheLoader != null) {
            BootstrapCacheLoader bootstrapCacheLoaderClone = (BootstrapCacheLoader) bootstrapCacheLoader.clone();
            copy.setBootstrapCacheLoader(bootstrapCacheLoaderClone);
        }

        return copy;
    }

    /**
     * Gets the internal Store.
     *
     * @return the Store referenced by this cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    final Store getStore() throws IllegalStateException {
        checkStatus();
        return compoundStore;
    }

    /**
     * Get the optional store management bean for this cache.
     */
    public final Object getStoreMBean() {
      return getStore().getMBean();
    }

    /**
     * Use this to access the service in order to register and unregister listeners
     *
     * @return the RegisteredEventListeners instance for this cache.
     */
    public final RegisteredEventListeners getCacheEventNotificationService() {
        return registeredEventListeners;
    }


    /**
     * Whether an Element is stored in the cache in Memory, indicating a very low cost of retrieval.
     * <p>
     * Since no assertions are made about the state of the Element it is possible that the
     * Element is expired, but this method still returns true.
     *
     * @return true if an element matching the key is found in memory
     */
    public final boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    /**
     * Whether an Element is stored in the cache in Memory, indicating a very low cost of retrieval.
     * <p>
     * Since no assertions are made about the state of the Element it is possible that the
     * Element is expired, but this method still returns true.
     *
     * @return true if an element matching the key is found in memory
     * @since 1.2
     */
    public final boolean isElementInMemory(Object key) {
        checkStatus();
        return compoundStore.containsKeyInMemory(key);
    }

    /**
     * Whether an Element is stored in the cache in off-heap memory, indicating an intermediate cost of retrieval.
     * <p>
     * Since no assertions are made about the state of the Element it is possible that the
     * Element is expired, but this method still returns true.
     *
     * @return true if an element matching the key is found in off-heap
     * @since 2.3
     */
    public final boolean isElementOffHeap(Object key) {
        checkStatus();
        return compoundStore.containsKeyOffHeap(key);
    }

    /**
     * Whether an Element is stored in the cache on Disk, indicating a higher cost of retrieval.
     * <p>
     * Since no assertions are made about the state of the Element it is possible that the
     * Element is expired, but this method still returns true.
     *
     * @return true if an element matching the key is found in the diskStore
     */
    public final boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    /**
     * Whether an Element is stored in the cache on Disk, indicating a higher cost of retrieval.
     * <p>
     * Since no assertions are made about the state of the Element it is possible that the
     * Element is expired, but this method still returns true.
     *
     * @return true if an element matching the key is found in the diskStore
     * @since 1.2
     */
    public final boolean isElementOnDisk(Object key) {
        checkStatus();
        return compoundStore.containsKeyOnDisk(key);
    }

    /**
     * The GUID for this cache instance can be used to determine whether two cache instance references
     * are pointing to the same cache.
     *
     * @return the globally unique identifier for this cache instance. This is guaranteed to be unique.
     * @since 1.2
     */
    public final String getGuid() {
        return guid;
    }

    /**
     * Gets the CacheManager managing this cache. For a newly created cache this will be null until
     * it has been added to a CacheManager.
     *
     * @return the manager or null if there is none
     */
    public final CacheManager getCacheManager() {
        return cacheManager;
    }


    /**
     * Resets statistics counters back to 0.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public void clearStatistics() throws IllegalStateException {
        checkStatus();
        liveCacheStatisticsData.clearStatistics();
        sampledCacheStatistics.clearStatistics();
        registeredEventListeners.clearCounters();
    }

    /**
     * Accurately measuring statistics can be expensive. Returns the current accuracy setting.
     *
     * @return one of {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT}, {@link Statistics#STATISTICS_ACCURACY_GUARANTEED}, {@link Statistics#STATISTICS_ACCURACY_NONE}
     */
    public int getStatisticsAccuracy() {
        return getLiveCacheStatistics().getStatisticsAccuracy();
    }

    /**
     * Sets the statistics accuracy.
     *
     * @param statisticsAccuracy one of {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT}, {@link Statistics#STATISTICS_ACCURACY_GUARANTEED}, {@link Statistics#STATISTICS_ACCURACY_NONE}
     */
    public void setStatisticsAccuracy(int statisticsAccuracy) {
        int oldValue = getStatisticsAccuracy();
        if (statisticsAccuracy != oldValue) {
            liveCacheStatisticsData.setStatisticsAccuracy(statisticsAccuracy);
            firePropertyChange("StatisticsAccuracy", oldValue, statisticsAccuracy);
        }
    }

    /**
     * Causes all elements stored in the Cache to be synchronously checked for expiry, and if expired, evicted.
     */
    public void evictExpiredElements() {
        checkStatus();
        compoundStore.expireElements();
    }

    /**
     * An inexpensive check to see if the key exists in the cache.
     * <p/>
     * This method is not synchronized. It is possible that an element may exist in the cache and be removed
     * before the check gets to it, or vice versa.  Since no assertions are made about the state of the Element
     * it is possible that the Element is expired, but this method still returns true.

     *
     * @param key the key to check.
     * @return true if an Element matching the key is found in the cache. No assertions are made about the state of the Element.
     */
    public boolean isKeyInCache(Object key) {
        if (key == null) {
            return false;
        }
        return compoundStore.containsKey(key);
    }

    /**
     * An extremely expensive check to see if the value exists in the cache. This implementation is O(n). Ehcache
     * is not designed for efficient access in this manner.
     * <p/>
     * This method is not synchronized. It is possible that an element may exist in the cache and be removed
     * before the check gets to it, or vice versa. Because it is slow to execute the probability of that this will
     * have happened.
     *
     * @param value to check for
     * @return true if an Element matching the key is found in the cache. No assertions are made about the state of the Element.
     */
    public boolean isValueInCache(Object value) {
        for (Object key : getKeys()) {
            Element element = get(key);
            if (element != null) {
                Object elementValue = element.getValue();
                if (elementValue == null) {
                    if (value == null) {
                        return true;
                    }
                } else {
                    if (elementValue.equals(value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Note, the {@link #getSize} method will have the same value as the size
     * reported by Statistics for the statistics accuracy of
     * {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT}.
     */
    public Statistics getStatistics() throws IllegalStateException {
        int size = getSizeBasedOnAccuracy(getLiveCacheStatistics()
                .getStatisticsAccuracy());
        return new Statistics(this, getLiveCacheStatistics()
                .getStatisticsAccuracy(), getLiveCacheStatistics()
                .getCacheHitCount(), getLiveCacheStatistics()
                .getOnDiskHitCount(), getLiveCacheStatistics()
                .getOffHeapHitCount(), getLiveCacheStatistics()
                .getInMemoryHitCount(), getLiveCacheStatistics()
                .getCacheMissCount(), getLiveCacheStatistics()
                .getOnDiskMissCount(), getLiveCacheStatistics()
                .getOffHeapMissCount(), getLiveCacheStatistics()
                .getInMemoryMissCount(), size, getAverageGetTime(),
                getLiveCacheStatistics().getEvictedCount(),
                getMemoryStoreSize(), getOffHeapStoreSize(), getDiskStoreSize(),
                getSearchesPerSecond(), getAverageSearchTime(), getLiveCacheStatistics().getWriterQueueLength());
    }

    /**
     * {@inheritDoc}
     */
    public long getAverageSearchTime() {
        return sampledCacheStatistics.getAverageSearchTime();
    }

    /**
     * {@inheritDoc}
     */
    public long getSearchesPerSecond() {
        return sampledCacheStatistics.getSearchesPerSecond();
    }

    /**
     * For use by CacheManager.
     *
     * @param cacheManager the CacheManager for this cache to use.
     */
    public void setCacheManager(CacheManager cacheManager) {
        CacheManager oldValue = getCacheManager();
        if (oldValue != null) {
            oldValue.getCacheRejoinAction().unregister(this);
        }
        this.cacheManager = cacheManager;
        cacheManager.getCacheRejoinAction().register(this);
        firePropertyChange("CacheManager", oldValue, cacheManager);
    }

    /**
     * Accessor for the BootstrapCacheLoader associated with this cache. For testing purposes.
     */
    public BootstrapCacheLoader getBootstrapCacheLoader() {
        return bootstrapCacheLoader;
    }

    /**
     * Sets the bootstrap cache loader.
     *
     * @param bootstrapCacheLoader the loader to be used
     * @throws CacheException if this method is called after the cache is initialized
     */
    public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) throws CacheException {
        if (!cacheStatus.isUninitialized()) {
            throw new CacheException("A bootstrap cache loader can only be set before the cache is initialized. "
                    + configuration.getName());
        }
        BootstrapCacheLoader oldValue = getBootstrapCacheLoader();
        this.bootstrapCacheLoader = bootstrapCacheLoader;
        firePropertyChange("BootstrapCacheLoader", oldValue, bootstrapCacheLoader);
    }

    /**
     * An equals method which follows the contract of {@link Object#equals(Object)}
     * <p/>
     * An Cache is equal to another one if it implements Ehcache and has the same GUID.
     *
     * @param object the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     * @see #hashCode()
     * @see java.util.Hashtable
     */
    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (!(object instanceof Ehcache)) {
            return false;
        }
        Ehcache other = (Ehcache) object;
        return guid.equals(other.getGuid());
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <code>java.util.Hashtable</code>.
     * <p/>
     * The general contract of <code>hashCode</code> is:
     * <ul>
     * <li>Whenever it is invoked on the same object more than once during
     * an execution of a Java application, the <tt>hashCode</tt> method
     * must consistently return the same integer, provided no information
     * used in <tt>equals</tt> comparisons on the object is modified.
     * This integer need not remain consistent from one execution of an
     * application to another execution of the same application.
     * <li>If two objects are equal according to the <tt>equals(Object)</tt>
     * method, then calling the <code>hashCode</code> method on each of
     * the two objects must produce the same integer result.
     * <li>It is <em>not</em> required that if two objects are unequal
     * according to the {@link Object#equals(Object)}
     * method, then calling the <tt>hashCode</tt> method on each of the
     * two objects must produce distinct integer results.  However, the
     * programmer should be aware that producing distinct integer results
     * for unequal objects may improve the performance of hashtables.
     * </ul>
     * <p/>
     * As much as is reasonably practical, the hashCode method defined by
     * class <tt>Object</tt> does return distinct integers for distinct
     * objects. (This is typically implemented by converting the internal
     * address of the object into an integer, but this implementation
     * technique is not required by the
     * Java(TM) programming language.)
     * <p/>
     * This implementation use the GUID of the cache.
     *
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see java.util.Hashtable
     */
    @Override
    public int hashCode() {
        return guid.hashCode();
    }


    /**
     * Create globally unique ID for this cache.
     */
    private String createGuid() {
        StringBuilder buffer = new StringBuilder().append(localhost).append("-").append(UUID.randomUUID());
        return buffer.toString();
    }

    /**
     * Register a {@link CacheExtension} with the cache. It will then be tied into the cache lifecycle.
     * <p/>
     * If the CacheExtension is not initialised, initialise it.
     */
    public void registerCacheExtension(CacheExtension cacheExtension) {
        registeredCacheExtensions.add(cacheExtension);
    }

    /**
     * @return the cache extensions as a live list
     */
    public List<CacheExtension> getRegisteredCacheExtensions() {
        return registeredCacheExtensions;
    }


    /**
     * Unregister a {@link CacheExtension} with the cache. It will then be detached from the cache lifecycle.
     */
    public void unregisterCacheExtension(CacheExtension cacheExtension) {
        cacheExtension.dispose();
        registeredCacheExtensions.remove(cacheExtension);
    }


    /**
     * The average get time in ms.
     */
    public float getAverageGetTime() {
        return getLiveCacheStatistics().getAverageGetTimeMillis();
    }

    /**
     * Sets an ExceptionHandler on the Cache. If one is already set, it is overwritten.
     * <p/>
     * The ExceptionHandler is only used if this Cache's methods are accessed using
     * {@link net.sf.ehcache.exceptionhandler.ExceptionHandlingDynamicCacheProxy}.
     *
     * @see net.sf.ehcache.exceptionhandler.ExceptionHandlingDynamicCacheProxy
     */
    public void setCacheExceptionHandler(CacheExceptionHandler cacheExceptionHandler) {
        CacheExceptionHandler oldValue = getCacheExceptionHandler();
        this.cacheExceptionHandler = cacheExceptionHandler;
        firePropertyChange("CacheExceptionHandler", oldValue, cacheExceptionHandler);
    }

    /**
     * Gets the ExceptionHandler on this Cache, or null if there isn't one.
     * <p/>
     * The ExceptionHandler is only used if this Cache's methods are accessed using
     * {@link net.sf.ehcache.exceptionhandler.ExceptionHandlingDynamicCacheProxy}.
     *
     * @see net.sf.ehcache.exceptionhandler.ExceptionHandlingDynamicCacheProxy
     */
    public CacheExceptionHandler getCacheExceptionHandler() {
        return cacheExceptionHandler;
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheLoader(CacheLoader cacheLoader) {
        registeredCacheLoaders.add(cacheLoader);
    }

    /**
     * Unregister a {@link CacheLoader} with the cache. It will then be detached from the cache lifecycle.
     *
     * @param cacheLoader A Cache Loader to unregister
     */
    public void unregisterCacheLoader(CacheLoader cacheLoader) {
        registeredCacheLoaders.remove(cacheLoader);
    }


    /**
     * @return the cache loaders as a live list
     */
    public List<CacheLoader> getRegisteredCacheLoaders() {
        return registeredCacheLoaders;
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheWriter(CacheWriter cacheWriter) {
        synchronized (this) {
            this.registeredCacheWriter = cacheWriter;
            if (cacheStatus.isAlive()) {
                initialiseRegisteredCacheWriter();
            }
        }
        initialiseCacheWriterManager(false);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterCacheWriter() {
        if (cacheWriterManagerInitFlag.get()) {
            throw new CacheException("Cache: " + configuration.getName() + " has its cache writer being unregistered " +
                    "after it was already initialised.");
        }
        this.registeredCacheWriter = null;
    }

    /**
     * {@inheritDoc}
     */
    public CacheWriter getRegisteredCacheWriter() {
        return this.registeredCacheWriter;
    }

    /**
     * Does the asynchronous put into the cache of the asynchronously loaded value.
     *
     * @param key the key to load
     * @param specificLoader a specific loader to use. If null the default loader is used.
     * @param argument the argument to pass to the writer
     * @return a Future which can be used to monitor execution
     */
    Future asynchronousPut(final Object key, final CacheLoader specificLoader, final Object argument) {
        return getExecutorService().submit(new Runnable() {

            /**
             * Calls the CacheLoader and puts the result in the Cache
             */
            public void run() throws CacheException {
                try {
                    //Test to see if it has turned up in the meantime
                    boolean existsOnRun = isKeyInCache(key);
                    if (!existsOnRun) {
                        Object value = loadValueUsingLoader(key, specificLoader, argument);
                        if (value != null) {
                            put(new Element(key, value), false);
                        }
                    }
                } catch (RuntimeException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Problem during load. Load will not be completed. Cause was " + e.getCause(), e);
                    }
                    throw new CacheException("Problem during load. Load will not be completed. Cause was " + e.getCause(), e);
                }
            }
        });
    }

    /**
     * Does the asynchronous loading. But doesn't put it into the cache
     *
     * @param key the key to load
     * @param specificLoader a specific loader to use. If null the default loader is used.
     * @param argument the argument to pass to the writer
     * @return a Future which can be used to monitor execution
     */
    Future<AtomicReference<Object>> asynchronousLoad(final Object key, final CacheLoader specificLoader, final Object argument) {
        final AtomicReference<Object> result = new AtomicReference<Object>();
        return getExecutorService().submit(new Runnable() {

            /**
             * Calls the CacheLoader and puts the result in the Cache
             */
            public void run() throws CacheException {
                try {
                    //Test to see if it has turned up in the meantime
                    boolean existsOnRun = isKeyInCache(key);
                    if (!existsOnRun) {
                        Object value = loadValueUsingLoader(key, specificLoader, argument);
                        if (value != null) {
                            result.set(value);
                        }
                    }
                } catch (RuntimeException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Problem during load. Load will not be completed. Cause was " + e.getCause(), e);
                    }
                    throw new CacheException("Problem during load. Load will not be completed. Cause was " + e.getCause(), e);
                }
            }
        }, result);
    }

    /**
     * Will attempt to load the value for a key, either using the passedin loader, or falling back to registered ones
     * @param key the key to load for
     * @param specificLoader the loader to use, can be null to fallback to Cache registered loaders
     * @param argument the argument to pass the loader
     * @return null if not present in the underlying SoR or if no loader available, otherwise the loaded object
     */
    private Object loadValueUsingLoader(final Object key, final CacheLoader specificLoader, final Object argument) {
        Object value = null;
        if (specificLoader != null) {
            if (argument == null) {
                value = specificLoader.load(key);
            } else {
                value = specificLoader.load(key, argument);
            }
        } else if (!registeredCacheLoaders.isEmpty()) {
            value = loadWithRegisteredLoaders(argument, key);
        }
        return value;
    }

    private Object loadWithRegisteredLoaders(Object argument, Object key) throws CacheException {

        Object value = null;

        if (argument == null) {
            for (CacheLoader registeredCacheLoader : registeredCacheLoaders) {
                value = registeredCacheLoader.load(key);
                if (value != null) {
                    break;
                }
            }
        } else {
            for (CacheLoader registeredCacheLoader : registeredCacheLoaders) {
                value = registeredCacheLoader.load(key, argument);
                if (value != null) {
                    break;
                }
            }
        }
        return value;
    }


    /**
     * Creates a future to perform the load
     *
     * @param keys
     * @param argument the loader argument
     * @return a Future which can be used to monitor execution
     */
    Future asynchronousLoadAll(final Collection keys, final Object argument) {
        return getExecutorService().submit(new Runnable() {
            /**
             * Calls the CacheLoader and puts the result in the Cache
             */
            public void run() {
                try {
                    Set<Object> nonLoadedKeys = new HashSet<Object>();
                    for (Object key : keys) {
                        if (!isKeyInCache(key)) {
                            nonLoadedKeys.add(key);
                        }
                    }
                    Map<?, ?> map = loadWithRegisteredLoaders(argument, nonLoadedKeys);
                    for (Entry<?, ?> e : map.entrySet()) {
                        put(new Element(e.getKey(), e.getValue()));
                    }
                } catch (Throwable e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Problem during load. Load will not be completed. Cause was " + e.getCause(), e);
                    }
                }
            }
        });
    }

    /**
     * Does the asynchronous loading.
     *
     * @param argument      the loader argument
     * @param nonLoadedKeys the Set of keys that are already in the Cache
     * @return A map of loaded elements
     */
    Map loadWithRegisteredLoaders(Object argument, Set<Object> nonLoadedKeys) {
        Map result = new HashMap();
        for (CacheLoader registeredCacheLoader : registeredCacheLoaders) {
            if (nonLoadedKeys.isEmpty()) {
                break;
            }

            Map resultForThisCacheLoader = null;
            if (argument == null) {
                resultForThisCacheLoader = registeredCacheLoader.loadAll(nonLoadedKeys);
            } else {
                resultForThisCacheLoader = registeredCacheLoader.loadAll(nonLoadedKeys, argument);
            }
            if (resultForThisCacheLoader != null) {
                nonLoadedKeys.removeAll(resultForThisCacheLoader.keySet());
                result.putAll(resultForThisCacheLoader);
            }
        }
        return result;
    }

    /**
     * @return Gets the executor service. This is not publically accessible.
     */
    ExecutorService getExecutorService() {
        if (executorService == null) {
            synchronized (this) {
                if (VmUtils.isInGoogleAppEngine()) {
                    // no Thread support. Run all tasks on the caller thread
                    executorService = new AbstractExecutorService() {
                        /** {@inheritDoc} */
                        public void execute(Runnable command) {
                            command.run();
                        }

                        /** {@inheritDoc} */
                        public List<Runnable> shutdownNow() {
                            return Collections.emptyList();
                        }

                        /** {@inheritDoc} */
                        public void shutdown() {
                        }

                        /** {@inheritDoc} */
                        public boolean isTerminated() {
                            return isShutdown();
                        }

                        /** {@inheritDoc} */
                        public boolean isShutdown() {
                            return false;
                        }

                        /** {@inheritDoc} */
                        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                            return true;
                        }
                    };
                } else {
                    // we can create Threads
                    executorService = new ThreadPoolExecutor(EXECUTOR_CORE_POOL_SIZE, EXECUTOR_MAXIMUM_POOL_SIZE, EXECUTOR_KEEP_ALIVE_TIME,
                            TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), new NamedThreadFactory("Cache Executor Service"));
                }
            }
        }
        return executorService;
    }


    /**
     * Whether this cache is disabled. "Disabled" means:
     * <ol>
     * <li>bootstrap is disabled</li>
     * <li>puts are discarded</li>
     * <li>putQuiets are discarded</li>
     * <li>gets return null</li>
     * </ol>
     * In all other respects the cache continues as it is.
     * <p/>
     * You can disable and enable a cache programmatically through the {@link #setDisabled(boolean)} method.
     * <p/>
     * By default caches are enabled on creation, unless the <code>net.sf.ehcache.disabled</code> system
     * property is set.
     *
     * @return true if the cache is disabled.
     * @see #NET_SF_EHCACHE_DISABLED ?
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Disables or enables this cache. This call overrides the previous value of disabled, even if the
     * <code>net.sf.ehcache.disabled</code> system property is set
     * <p/>
     *
     * @param disabled true if you wish to disable, false to enable
     * @see #isDisabled()
     */
    public void setDisabled(boolean disabled) {
        if (allowDisable) {
            boolean oldValue = isDisabled();
            if (oldValue != disabled) {
                synchronized (this) {
                    this.disabled = disabled;
                }
                firePropertyChange("Disabled", oldValue, disabled);
            }
        } else {
            throw new CacheException("Dynamic cache features are disabled");
        }
    }

    /**
     * @return the current in-memory eviction policy. This may not be the configured policy, if it has been
     *         dynamically set.
     */
    public Policy getMemoryStoreEvictionPolicy() {
        checkStatus();
        return compoundStore.getInMemoryEvictionPolicy();
    }

    /**
     * Sets the eviction policy strategy. The Cache will use a policy at startup. There
     * are three policies which can be configured: LRU, LFU and FIFO. However many other
     * policies are possible. That the policy has access to the whole element enables policies
     * based on the key, value, metadata, statistics, or a combination of any of the above.
     * It is safe to change the policy of a store at any time. The new policy takes effect
     * immediately.
     *
     * @param policy the new policy
     */
    public void setMemoryStoreEvictionPolicy(Policy policy) {
        checkStatus();
        Policy oldValue = getMemoryStoreEvictionPolicy();
        compoundStore.setInMemoryEvictionPolicy(policy);
        firePropertyChange("MemoryStoreEvictionPolicy", oldValue, policy);
    }

    /**
     * {@inheritDoc}
     */
    public LiveCacheStatistics getLiveCacheStatistics()
            throws IllegalStateException {
        checkStatus();
        return liveCacheStatisticsData;
    }

    private LiveCacheStatistics getLiveCacheStatisticsNoCheck() {
        return liveCacheStatisticsData;
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheUsageListener(CacheUsageListener cacheUsageListener)
            throws IllegalStateException {
        checkStatus();
        liveCacheStatisticsData.registerCacheUsageListener(cacheUsageListener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeCacheUsageListener(CacheUsageListener cacheUsageListener)
            throws IllegalStateException {
        checkStatus();
        liveCacheStatisticsData.removeCacheUsageListener(cacheUsageListener);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return getLiveCacheStatistics().isStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsEnabled(boolean enableStatistics) {
        boolean oldValue = isStatisticsEnabled();
        if (oldValue != enableStatistics) {
            liveCacheStatisticsData.setStatisticsEnabled(enableStatistics);
            if (!enableStatistics) {
                setSampledStatisticsEnabled(false);
            }
            firePropertyChange("StatisticsEnabled", oldValue, enableStatistics);
        }
    }

    /**
     * {@inheritDoc}
     */
    public SampledCacheStatistics getSampledCacheStatistics() {
        return sampledCacheStatistics;
    }

    /**
     * An access for the {@link CacheStatisticsSampler} associated to this {@code Cache}
     *
     * @return the {@code CacheStatisticsSampler}
     */
    public CacheStatisticsSampler getCacheStatisticsSampler() {
        return sampledCacheStatistics;
    }

    /**
     * {@inheritDoc}
     */
    public void setSampledStatisticsEnabled(final boolean enableStatistics) {
        if (cacheManager == null) {
            throw new IllegalStateException("You must add the cache to a CacheManager before enabling/disabling sampled statistics.");
        }
        boolean oldValue = isSampledStatisticsEnabled();
        if (oldValue != enableStatistics) {
            if (enableStatistics) {
                ManagementRESTServiceConfiguration mgmtRESTConfigSvc = cacheManager.getConfiguration().getManagementRESTService();
                if (mgmtRESTConfigSvc != null && mgmtRESTConfigSvc.isEnabled()) {
                    sampledCacheStatistics.enableSampledStatistics(cacheManager.getTimer(), mgmtRESTConfigSvc.makeSampledCounterConfig(),
                            mgmtRESTConfigSvc.makeSampledGetRateCounterConfig(), mgmtRESTConfigSvc.makeSampledSearchRateCounterConfig());
                } else {
                    sampledCacheStatistics.enableSampledStatistics(cacheManager.getTimer());
                }
                setStatisticsEnabled(true);
            } else {
                sampledCacheStatistics.disableSampledStatistics();
            }
            firePropertyChange("SampledStatisticsEnabled", oldValue, enableStatistics);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.Ehcache#isSampledStatisticsEnabled()
     */
    public boolean isSampledStatisticsEnabled() {
        return sampledCacheStatistics.isSampledStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        checkStatus();
        return compoundStore.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    public void disableDynamicFeatures() {
        configuration.freezeConfiguration();
        allowDisable = false;
    }

    /**
     * {@inheritDoc}
     * @deprecated use {@link #isClusterBulkLoadEnabled()} instead
     */
    @Deprecated
    public boolean isClusterCoherent() {
        return !this.isClusterBulkLoadEnabled();
    }

    /**
     * {@inheritDoc}
     * @deprecated use {@link #isNodeBulkLoadEnabled()} instead
     */
    @Deprecated
    public boolean isNodeCoherent() {
        return !this.isNodeBulkLoadEnabled();
    }

    /**
     * {@inheritDoc}
     * @deprecated use {@link #setNodeBulkLoadEnabled(boolean)} instead
     */
    @Deprecated
    public void setNodeCoherent(boolean coherent) {
        this.setNodeBulkLoadEnabled(!coherent);
    }

    /**
     * {@inheritDoc}
     * @deprecated use {@link #waitUntilClusterBulkLoadComplete()} instead
     */
    @Deprecated
    public void waitUntilClusterCoherent() {
        this.waitUntilClusterBulkLoadComplete();
    }

    // PropertyChangeSupport

    /**
     * @param listener
     */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
      if (listener != null && propertyChangeSupport != null) {
        propertyChangeSupport.removePropertyChangeListener(listener);
        propertyChangeSupport.addPropertyChangeListener(listener);
      }
    }

    /**
     * @param listener
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
      if (listener != null && propertyChangeSupport != null) {
        propertyChangeSupport.removePropertyChangeListener(listener);
      }
    }

    /**
     * @param propertyName
     * @param oldValue
     * @param newValue
     */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
      PropertyChangeSupport pcs;
      synchronized (this) {
        pcs = propertyChangeSupport;
      }
      if (pcs != null && (oldValue != null || newValue != null)) {
        pcs.firePropertyChange(propertyName, oldValue, newValue);
      }
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        return putIfAbsent(element, false);
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element, boolean doNotNotifyCacheReplicators) throws NullPointerException {
        checkStatus();

        checkCASOperationSupported();

        if (element.getObjectKey() == null) {
            throw new NullPointerException();
        }

        if (disabled) {
            return null;
        }

        //this guard currently ensures reasonable behavior on expiring elements
        getQuiet(element.getObjectKey());

        element.resetAccessStatistics();
        applyDefaultsToElementWithoutLifespanSet(element);
        backOffIfDiskSpoolFull();
        element.updateUpdateStatistics();
        Element result = compoundStore.putIfAbsent(element);
        if (result == null) {
            notifyPutInternalListeners(element, doNotNotifyCacheReplicators, false);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeElement(Element element) throws NullPointerException {
        checkStatus();

        checkCASOperationSupported();

        if (element.getObjectKey() == null) {
            throw new NullPointerException();
        }

        if (disabled) {
            return false;
        }

        // this guard currently ensures reasonable behavior on expiring elements
        getQuiet(element.getObjectKey());

        Element result = compoundStore.removeElement(element, elementValueComparator);

        // FIXME shouldn't this be done only if result != null
        notifyRemoveInternalListeners(element.getObjectKey(), false, true, false, result);
        return result != null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        checkStatus();

        checkCASOperationSupported();

        if (old.getObjectKey() == null || element.getObjectKey() == null) {
            throw new NullPointerException();
        }
        if (!old.getObjectKey().equals(element.getObjectKey())) {
            throw new IllegalArgumentException("The keys for the element arguments to replace must be equal");
        }

        if (disabled) {
            return false;
        }

        getQuiet(old.getObjectKey());

        element.resetAccessStatistics();
        applyDefaultsToElementWithoutLifespanSet(element);
        backOffIfDiskSpoolFull();

        boolean result = compoundStore.replace(old, element, elementValueComparator);

        if (result) {
            element.updateUpdateStatistics();
            notifyPutInternalListeners(element, false, true);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        checkStatus();

        checkCASOperationSupported();

        if (element.getObjectKey() == null) {
            throw new NullPointerException();
        }

        if (disabled) {
            return null;
        }

        getQuiet(element.getObjectKey());

        element.resetAccessStatistics();
        applyDefaultsToElementWithoutLifespanSet(element);
        backOffIfDiskSpoolFull();

        Element result = compoundStore.replace(element);
        if (result != null) {
            element.updateUpdateStatistics();
            notifyPutInternalListeners(element, false, true);
        }
        return result;
    }

    private void checkCASOperationSupported() {
        if (registeredEventListeners.hasCacheReplicators()) {
            throw new CacheException(
                    "You have configured the cache with a replication scheme that cannot properly support CAS operation guarantees.");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.StoreListener#clusterCoherent(boolean)
     */
    public void clusterCoherent(boolean clusterCoherent) {
        firePropertyChange("ClusterCoherent", !clusterCoherent, clusterCoherent);
    }


    /**
     * {@inheritDoc}
     */
    public <T> Attribute<T> getSearchAttribute(String attributeName) throws CacheException {
        // We don't trust config here since the store is the real authority
        checkStatus();
        Attribute<T> searchAttribute = compoundStore.getSearchAttribute(attributeName);

        if (searchAttribute == null) {
            final String msg;
            if (attributeName.equals(Query.KEY.getAttributeName())) {
                msg = "Key search attribute is disabled for cache [" + getName() + "]. It can be enabled with <searchable keys=\"true\"...";
            } else if (attributeName.equals(Query.VALUE.getAttributeName())) {
                msg = "Value search attribute is disabled for cache [" + getName() + "]. It can be enabled with <searchable values=\"true\"...";
            } else {
                msg = "No such search attribute [" + attributeName + "] defined for this cache [" + getName() + "]";
            }

            throw new CacheException(msg);
        }

        return searchAttribute;
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery() {
        if (!isSearchable()) {
            throw new CacheException("This cache is not configured for search");
        }
        return new CacheQuery(this);
    }

    /**
     * Execute the given query
     *
     * @param query query to execute
     * @return query results
     */
    Results executeQuery(StoreQuery query) throws SearchException {

        validateSearchQuery(query);

        if (isStatisticsEnabled()) {
            long start = System.currentTimeMillis();
            Results results = this.compoundStore.executeQuery(query);
            sampledCacheStatistics.notifyCacheSearch(System.currentTimeMillis() - start);
            return results;
        }

        return this.compoundStore.executeQuery(query);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSearchable() {
        return configuration.isSearchable();
    }

    /**
     * Start cluster rejoin
     */
    void clusterRejoinStarted() {
        try {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().dispose();
        } catch (Exception e) {
            LOG.debug("Ignoring exception while disposing old store on rejoin - " + e.getMessage(), e);
        }
        cacheStatus.clusterRejoinInProgress();
    }

    /**
     * Complete cluster rejoin
     */
    void clusterRejoinComplete() {
        // initialize again
        initialise();
        cacheStatus.clusterRejoinComplete();
        if (compoundStore instanceof RejoinAwareNonstopStore) {
            ((RejoinAwareNonstopStore) compoundStore).clusterRejoined();
        }
    }

    /**
     * Gets the lock for a given key
     *
     * @param key
     * @return the lock object for the passed in key
     */
    protected Sync getLockForKey(final Object key) {
        checkStatus();
        return lockProvider.getSyncForKey(key);
    }

    private void acquireLockOnKey(Object key, LockType lockType) {
        Sync s = getLockForKey(key);
        s.lock(lockType);
    }

    private void releaseLockOnKey(Object key, LockType lockType) {
        Sync s = getLockForKey(key);
        s.unlock(lockType);
    }

    /**
     * Acquires the proper read lock for a given cache key
     *
     * @param key - The key that retrieves a value that you want to protect via locking
     */
    public void acquireReadLockOnKey(Object key) {
        this.acquireLockOnKey(key, LockType.READ);
    }

    /**
     * Acquires the proper write lock for a given cache key
     *
     * @param key - The key that retrieves a value that you want to protect via locking
     */
    public void acquireWriteLockOnKey(Object key) {
        this.acquireLockOnKey(key, LockType.WRITE);
    }

    /**
     * Try to get a read lock on a given key. If can't get it in timeout millis then
     * return a boolean telling that it didn't get the lock
     *
     * @param key - The key that retrieves a value that you want to protect via locking
     * @param timeout - millis until giveup on getting the lock
     * @return whether the lock was awarded
     * @throws InterruptedException
     */
    public boolean tryReadLockOnKey(Object key, long timeout) throws InterruptedException {
        Sync s = getLockForKey(key);
        return s.tryLock(LockType.READ, timeout);
    }

    /**
     * Try to get a write lock on a given key. If can't get it in timeout millis then
     * return a boolean telling that it didn't get the lock
     *
     * @param key - The key that retrieves a value that you want to protect via locking
     * @param timeout - millis until giveup on getting the lock
     * @return whether the lock was awarded
     * @throws InterruptedException
     */
    public boolean tryWriteLockOnKey(Object key, long timeout) throws InterruptedException {
        Sync s = getLockForKey(key);
        return s.tryLock(LockType.WRITE, timeout);
    }

    /**
     * Release a held read lock for the passed in key
     *
     * @param key - The key that retrieves a value that you want to protect via locking
     */
    public void releaseReadLockOnKey(Object key) {
        releaseLockOnKey(key, LockType.READ);
    }

    /**
     * Release a held write lock for the passed in key
     *
     * @param key - The key that retrieves a value that you want to protect via locking
     */
    public void releaseWriteLockOnKey(Object key) {
        releaseLockOnKey(key, LockType.WRITE);
    }


    /**
     * {@inheritDoc}
     * <p>
     * Only Terracotta clustered cache instances currently support querying a thread's read lock hold status.
     */
    public boolean isReadLockedByCurrentThread(Object key) throws UnsupportedOperationException {
        return getLockForKey(key).isHeldByCurrentThread(LockType.READ);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isWriteLockedByCurrentThread(Object key) {
        return getLockForKey(key).isHeldByCurrentThread(LockType.WRITE);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterBulkLoadEnabled() throws UnsupportedOperationException, TerracottaNotRunningException {
        checkStatus();
        return !compoundStore.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeBulkLoadEnabled() throws UnsupportedOperationException, TerracottaNotRunningException {
        checkStatus();
        return !compoundStore.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeBulkLoadEnabled(boolean enabledBulkLoad) throws UnsupportedOperationException, TerracottaNotRunningException {
        final boolean oldValue = isNodeBulkLoadEnabled();
        if (oldValue != enabledBulkLoad) {
            compoundStore.setNodeCoherent(!enabledBulkLoad);
            nonstopActiveDelegateHolder.nodeBulkLoadChanged(enabledBulkLoad);
            firePropertyChange("NodeCoherent", oldValue, enabledBulkLoad);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilClusterBulkLoadComplete() throws UnsupportedOperationException, TerracottaNotRunningException {
        checkStatus();
        try {
            compoundStore.waitUntilClusterCoherent();
        } catch (InterruptedException e) {
            // re-throw as cacheException
            throw new CacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unpinAll() {
        checkStatus();
        if (disabled) {
            return;
        }
        compoundStore.unpinAll();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPinned(Object key) {
        checkStatus();
        if (disabled || key == null) {
            return false;
        }
        return compoundStore.isPinned(key);
    }

    /**
     * {@inheritDoc}
     */
    public void setPinned(Object key, boolean pinned) {
        checkStatus();
        if (disabled || key == null) {
            return;
        }
        compoundStore.setPinned(key, pinned);
    }

    /**
     * Returns the {@link NonstopActiveDelegateHolder}
     * @return the {@link NonstopActiveDelegateHolder}
     */
    protected NonstopActiveDelegateHolder getNonstopActiveDelegateHolder() {
        return nonstopActiveDelegateHolder;
    }

    /**
     * {@inheritDoc}
     */
    public void recalculateSize(Object key) {
        checkStatus();
        this.compoundStore.recalculateSize(key);
    }

    /**
     * Private class maintaining status of the cache
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class CacheStatus {
        private volatile Status status = Status.STATUS_UNINITIALISED;
        private final AtomicBoolean clusterRejoinInProgress = new AtomicBoolean(false);

        private void clusterRejoinComplete() {
            clusterRejoinInProgress.set(false);
        }

        public void checkAlive(CacheConfiguration configuration) {
            final Status readStatus = status;
            if (readStatus != Status.STATUS_ALIVE) {
                throw new IllegalStateException("The " + configuration.getName() + " Cache is not alive (" + readStatus + ")");
            }
        }

        private void clusterRejoinInProgress() {
            clusterRejoinInProgress.set(true);
        }

        /**
         * Returns true if cache can be initialized. Cache can be initialized if cache has not been shutdown yet.
         *
         * @return true if cache can be initialized
         */
        public boolean canInitialize() {
            return status == Status.STATUS_UNINITIALISED || clusterRejoinInProgress.get();
        }

        /**
         * Change state to the new state
         *
         * @param newState state
         */
        public void changeState(Status newState) {
            this.status = newState;
        }

        /**
         * Get the current state
         *
         * @return current state
         */
        public Status getStatus() {
            return status;
        }

        /**
         * Returns true if the cache is alive
         *
         * @return true if the cache is alive
         */
        public boolean isAlive() {
            return status == Status.STATUS_ALIVE;
        }

        /**
         * Returns true if the cache has been disposed
         *
         * @return true if the cache has been disposed
         */
        public boolean isShutdown() {
            return status == Status.STATUS_SHUTDOWN;
        }

        /**
         * Returns true if the cache is uninitialized
         *
         * @return true if the cache is uninitialized
         */
        public boolean isUninitialized() {
            return status == Status.STATUS_UNINITIALISED;
        }

    }

    /**
     * Private Static class
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class NonstopActiveDelegateHolderImpl implements NonstopActiveDelegateHolder {

        private final Cache cache;
        private volatile NonstopStoreImpl nonstopStore;
        private volatile TerracottaStore underlyingTerracottaStore;
        private volatile NonstopExecutorService nonstopExecutorService;
        private volatile CacheLockProvider underlyingCacheLockProvider;
        private volatile boolean nodeBulkLoadEnabled;
        private volatile CacheEventListener cacheEventReplicator;

        public NonstopActiveDelegateHolderImpl(Cache cache) {
            this.cache = cache;
        }

        public void nodeBulkLoadChanged(final boolean enabled) {
            this.nodeBulkLoadEnabled = enabled;
        }

        public RejoinAwareNonstopStore getNonstopStore() {
            if (nonstopStore != null) {
                return nonstopStore;
            }
            initializeNonstopStore();
            return nonstopStore;
        }

        private synchronized void initializeNonstopStore() {
            if (nonstopStore == null) {
                if (!cache.getCacheConfiguration().isTerracottaClustered()) {
                    throw new AssertionError("NonstopStore supported for Terracotta clustered caches only");
                }
                if (!cache.getCacheConfiguration().getTerracottaConfiguration().isNonstopEnabled()) {
                    throw new AssertionError("Nonstop is not enabled");
                }
                nonstopStore = new NonstopStoreImpl(this, cache.getCacheCluster(), cache.getCacheConfiguration()
                        .getTerracottaConfiguration().getNonstopConfiguration(), cache.getCacheConfiguration().getTransactionalMode(),
                        cache.getTransactionManagerLookup());
            }
        }

        public synchronized void terracottaStoreInitialized(TerracottaStore newTerracottaStore) {
            this.underlyingTerracottaStore = newTerracottaStore;

            if (nodeBulkLoadEnabled) {
                LOG.debug("Enabling bulk-load for " + cache.getName());
                underlyingTerracottaStore.setNodeCoherent(false);
            }

            // reset all other holders associated with the new store
            nonstopExecutorService = cache.getCacheManager().getNonstopExecutorService();
            Object context = underlyingTerracottaStore.getInternalContext();
            if (context instanceof CacheLockProvider) {
                underlyingCacheLockProvider = (CacheLockProvider) context;
            } else {
                throw new AssertionError("TerracottaStore.getInternalContext() is not correct - "
                        + (context == null ? "NULL" : context.getClass().getName()));
            }
            cacheEventReplicator = cache.getCacheManager().getClusteredInstanceFactory(cache).createEventReplicator(cache);
        }

        public TerracottaStore getUnderlyingTerracottaStore() {
            return underlyingTerracottaStore;
        }

        public NonstopExecutorService getNonstopExecutorService() {
            return nonstopExecutorService;
        }

        public CacheLockProvider getUnderlyingCacheLockProvider() {
            return underlyingCacheLockProvider;
        }

        public CacheEventListener getCacheEventReplicator() {
            if (cacheEventReplicator != null) {
                return cacheEventReplicator;
            } else {
                synchronized (this) {
                    if (cacheEventReplicator == null) {
                        // don't use getCacheManager().createTerracottaEventReplicator(cache) but create one using the clustered instance
                        // factory
                        cacheEventReplicator = cache.getCacheManager().getClusteredInstanceFactory(cache).createEventReplicator(cache);
                    }
                    return cacheEventReplicator;
                }
            }
        }

    }

    private void validateSearchQuery(StoreQuery query) throws SearchException {
        if (!query.requestsKeys() && !query.requestsValues() && query.requestedAttributes().isEmpty() && query.getAggregatorInstances().isEmpty()) {
            String msg = "No results specified. " +
            "Please specify one or more of includeKeys(), includeValues(), includeAggregator() or includeAttribute()";
            throw new SearchException(msg);
        }
        Set<Attribute<?>> groupBy = query.groupByAttributes();
        if (!groupBy.isEmpty()) {
            if (groupBy.contains(Query.KEY)) {
                throw new SearchException("Explicit grouping by element key not supported.");
            }
            if (groupBy.contains(Query.VALUE)) {
                throw new SearchException("Grouping by element value not supported.");
            }
            if (!groupBy.containsAll(query.requestedAttributes())) {
                throw new SearchException("Some of the requested attributes not used in group by clause.");
            }
            for (Ordering order : query.getOrdering()) {
                if (!groupBy.contains(order.getAttribute())) {
                    throw new SearchException("All ordering attributes must be present in group by clause.");
                }
            }
            if (query.requestsValues() || query.requestsKeys()) {
                throw new SearchException("It is not possible to include keys or values with group by queries.");
            }
        }
    }
}
