/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.store.DiskStore;
import net.sf.ehcache.store.MemoryStore;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.LruMemoryStore;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * @version $Id$
 */
public class Cache implements Ehcache {

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

    {
        String value = System.getProperty(NET_SF_EHCACHE_DISABLED);
        if (value != null) {
            disabled = value.equalsIgnoreCase("true");
        }
    }

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

    {
        String value = System.getProperty(NET_SF_EHCACHE_USE_CLASSIC_LRU);
        if (value != null) {
            useClassicLru = value.equalsIgnoreCase("true");
        }
    }

    /**
     * The default interval between runs of the expiry thread.
     */
    public static final long DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS = 120;

    /**
     * Set a buffer size for the spool of approx 30MB
     */
    private static final int DEFAULT_SPOOL_BUFFER_SIZE = 30;

    private static final Logger LOG = Logger.getLogger(Cache.class.getName());

    private static final MemoryStoreEvictionPolicy DEFAULT_MEMORY_STORE_EVICTION_POLICY = MemoryStoreEvictionPolicy.LRU;

    private static InetAddress localhost;

    /**
     * The amount of time to wait if a store gets backed up
     */
    private static final int BACK_OFF_TIME_MILLIS = 50;

    private static final int EXECUTOR_KEEP_ALIVE_TIME = 60000;
    private static final int EXECUTOR_MAXIMUM_POOL_SIZE = 10;
    private static final int EXECUTOR_CORE_POOL_SIZE = 1;

    static {
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOG.log(Level.SEVERE, "Unable to set localhost. This prevents creation of a GUID. Cause was: " + e.getMessage(), e);
        }
    }

    private boolean disabled;

    private boolean useClassicLru;

    private Store diskStore;

    private String diskStorePath;

    private Status status;

    private CacheConfiguration configuration;

    /**
     * Cache hit count.
     */
    private long hitCount;

    /**
     * Memory cache hit count.
     */
    private long memoryStoreHitCount;

    /**
     * DiskStore hit count.
     */
    private long diskStoreHitCount;

    /**
     * Count of misses where element was not found.
     */
    private long missCountNotFound;

    /**
     * Count of misses where element was expired.
     */
    private long missCountExpired;

    /**
     * The {@link MemoryStore} of this {@link Cache}. All caches have a memory store.
     */
    private Store memoryStore;

    private RegisteredEventListeners registeredEventListeners;

    private List<CacheExtension> registeredCacheExtensions;

    private String guid;

    private CacheManager cacheManager;

    private BootstrapCacheLoader bootstrapCacheLoader;

    private int statisticsAccuracy;

    private long totalGetTime;

    private CacheExceptionHandler cacheExceptionHandler;

    private List<CacheLoader> registeredCacheLoaders;

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
    private ThreadPoolExecutor executorService;


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
     * @param maxElementsInMemory the maximum number of elements in memory, before they are evicted
     * @param overflowToDisk      whether to use the disk store
     * @param eternal             whether the elements in the cache are eternal, i.e. never expire
     * @param timeToLiveSeconds   the default amount of time to live for an element from its creation date
     * @param timeToIdleSeconds   the default amount of time to live for an element from its last accessed or modified date
     * @since 1.0
     */
    public Cache(String name, int maxElementsInMemory, boolean overflowToDisk,
                 boolean eternal, long timeToLiveSeconds, long timeToIdleSeconds) {
        this(name, maxElementsInMemory, DEFAULT_MEMORY_STORE_EVICTION_POLICY, overflowToDisk,
                null, eternal, timeToLiveSeconds, timeToIdleSeconds, false, DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS, null, null);
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
     * @param maxElementsInMemory the maximum number of elements in memory, before they are evicted
     * @param overflowToDisk      whether to use the disk store
     * @param eternal             whether the elements in the cache are eternal, i.e. never expire
     * @param timeToLiveSeconds   the default amount of time to live for an element from its creation date
     * @param timeToIdleSeconds   the default amount of time to live for an element from its last accessed or modified date
     * @param diskPersistent      whether to persist the cache to disk between JVM restarts
     * @param diskExpiryThreadIntervalSeconds
     *                            how often to run the disk store expiry thread. A large number of 120 seconds plus is recommended
     * @since 1.1
     */
    public Cache(String name,
                 int maxElementsInMemory,
                 boolean overflowToDisk,
                 boolean eternal,
                 long timeToLiveSeconds,
                 long timeToIdleSeconds,
                 boolean diskPersistent,
                 long diskExpiryThreadIntervalSeconds) {
        this(name, maxElementsInMemory, DEFAULT_MEMORY_STORE_EVICTION_POLICY, overflowToDisk, null,
                eternal, timeToLiveSeconds, timeToIdleSeconds, diskPersistent, diskExpiryThreadIntervalSeconds, null, null);
        LOG.warning("An API change between ehcache-1.1 and ehcache-1.2 results in the persistence path being set to " +
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
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted
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
        this(name,
                maxElementsInMemory,
                memoryStoreEvictionPolicy,
                overflowToDisk,
                diskStorePath,
                eternal,
                timeToLiveSeconds,
                timeToIdleSeconds,
                diskPersistent,
                diskExpiryThreadIntervalSeconds,
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
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted
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

        this(name,
                maxElementsInMemory,
                memoryStoreEvictionPolicy,
                overflowToDisk,
                diskStorePath,
                eternal,
                timeToLiveSeconds,
                timeToIdleSeconds,
                diskPersistent,
                diskExpiryThreadIntervalSeconds,
                registeredEventListeners,
                bootstrapCacheLoader,
                0);
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
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted
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


        this(name,
                maxElementsInMemory,
                memoryStoreEvictionPolicy,
                overflowToDisk,
                diskStorePath,
                eternal,
                timeToLiveSeconds,
                timeToIdleSeconds,
                diskPersistent,
                diskExpiryThreadIntervalSeconds,
                registeredEventListeners,
                bootstrapCacheLoader,
                maxElementsOnDisk,
                0,
                true);

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
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted
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
        this(name,
                maxElementsInMemory,
                memoryStoreEvictionPolicy,
                overflowToDisk,
                diskStorePath,
                eternal,
                timeToLiveSeconds,
                timeToIdleSeconds,
                diskPersistent,
                diskExpiryThreadIntervalSeconds,
                registeredEventListeners,
                bootstrapCacheLoader,
                maxElementsOnDisk,
                diskSpoolBufferSizeMB,
                true);

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
     * @param maxElementsInMemory       the maximum number of elements in memory, before they are evicted
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
     * @param clearOnFlush              whether the MemoryStore should be cleared when {@link #flush flush()} is called on the cache
     * @since 1.6.0
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

        changeStatus(Status.STATUS_UNINITIALISED);

        guid = createGuid();

        configuration = new CacheConfiguration();
        configuration.setName(name);
        configuration.setMaxElementsInMemory(maxElementsInMemory);
        configuration.setMemoryStoreEvictionPolicyFromObject(memoryStoreEvictionPolicy);
        configuration.setOverflowToDisk(overflowToDisk);
        configuration.setEternal(eternal);
        configuration.setTimeToLiveSeconds(timeToLiveSeconds);
        configuration.setTimeToIdleSeconds(timeToIdleSeconds);
        configuration.setDiskPersistent(diskPersistent);
        configuration.setMaxElementsOnDisk(maxElementsOnDisk);
        configuration.setClearOnFlush(clearOnFlush);


        if (diskStorePath == null) {
            this.diskStorePath = DiskStoreConfiguration.getDefaultPath();
        } else {
            this.diskStorePath = diskStorePath;
        }

        if (registeredEventListeners == null) {
            this.registeredEventListeners = new RegisteredEventListeners(this);
        } else {
            this.registeredEventListeners = registeredEventListeners;
        }

        registeredCacheExtensions = new CopyOnWriteArrayList<CacheExtension>();
        registeredCacheLoaders = new CopyOnWriteArrayList<CacheLoader>();

        //Set this to a safe value.
        if (diskExpiryThreadIntervalSeconds == 0) {
            configuration.setDiskExpiryThreadIntervalSeconds(DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS);
        } else {
            configuration.setDiskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds);
        }

        if (diskSpoolBufferSizeMB == 0) {
            configuration.setDiskSpoolBufferSizeMB(DEFAULT_SPOOL_BUFFER_SIZE);
        } else {
            configuration.setDiskSpoolBufferSizeMB(diskSpoolBufferSizeMB);
        }

        // For backward compatibility with 1.1 and earlier
        if (memoryStoreEvictionPolicy == null) {
            configuration.setMemoryStoreEvictionPolicyFromObject(DEFAULT_MEMORY_STORE_EVICTION_POLICY);
        }

        this.bootstrapCacheLoader = bootstrapCacheLoader;

        statisticsAccuracy = Statistics.STATISTICS_ACCURACY_BEST_EFFORT;

    }

    /**
     * Newly created caches do not have a {@link net.sf.ehcache.store.MemoryStore} or a {@link net.sf.ehcache.store.DiskStore}.
     * <p/>
     * This method creates those and makes the cache ready to accept elements
     */
    public void initialise() {
        synchronized (this) {
            if (!status.equals(Status.STATUS_UNINITIALISED)) {
                throw new IllegalStateException("Cannot initialise the " + configuration.getName()
                        + " cache because its status is not STATUS_UNINITIALISED");
            }

            if (configuration.getMaxElementsInMemory() == 0) {
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("Cache: " + configuration.getName()
                            + " has a maxElementsInMemory of 0. It is strongly recommended to " +
                            "have a maximumSize of at least 1. Performance is halved by not using a MemoryStore.");
                }
            }

            this.diskStore = createDiskStore();

            if (useClassicLru && configuration.getMemoryStoreEvictionPolicy().equals(MemoryStoreEvictionPolicy.LRU)) {
                memoryStore = new LruMemoryStore(this, diskStore);
            } else {
                memoryStore = MemoryStore.create(this, diskStore);
            }
            changeStatus(Status.STATUS_ALIVE);
            initialiseRegisteredCacheExtensions();
            initialiseRegisteredCacheLoaders();
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Initialised cache: " + configuration.getName());
        }

        if (disabled) {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.warning("Cache: " + configuration.getName() + " is disabled because the " + NET_SF_EHCACHE_DISABLED
                        + " property was set to true. No elements will be added to the cache.");
            }
        }
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
    protected Store createDiskStore() {
        if (isDiskStore()) {
            return new DiskStore(this, diskStorePath);
        } else {
            return null;
        }
    }

    /**
     * Whether this cache uses a disk store
     *
     * @return true if the cache either overflows to disk or is disk persistent
     */
    protected boolean isDiskStore() {
        return configuration.isOverflowToDisk() || configuration.isDiskPersistent();
    }

    /**
     * Bootstrap command. This must be called after the Cache is intialised, during
     * CacheManager initialisation. If loads are synchronous, they will complete before the CacheManager
     * initialise completes, otherwise they will happen in the background.
     */
    public void bootstrap() {
        if (!disabled && bootstrapCacheLoader != null) {
            bootstrapCacheLoader.load(this);
        }

    }

    private void changeStatus(Status status) {
        this.status = status;
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
     * This exception should be caught in those cirucmstances.
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
     * This exception should be caught in those cirucmstances.
     *
     * @param element                     A cache Element. If Serializable it can fully participate in replication and the DiskStore. If it is
     *                                    <code>null</code> or the key is <code>null</code>, it is ignored as a NOOP.
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @throws IllegalStateException    if the cache is not {@link Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public final void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException,
            CacheException {
        checkStatus();

        if (disabled) {
            return;
        }

        if (element == null) {
            if (doNotNotifyCacheReplicators) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Element from replicated put is null. This happens because the element is a SoftReference" +
                            " and it has been collected.Increase heap memory on the JVM or set -Xms to be the same as " +
                            "-Xmx to avoid this problem.");
                }
            }
            //nulls are ignored
            return;
        }


        if (element.getObjectKey() == null) {
            //nulls are ignored
            return;
        }

        element.resetAccessStatistics();
        boolean elementExists;
        Object key = element.getObjectKey();
        elementExists = isElementInMemory(key) || isElementOnDisk(key);
        if (elementExists) {
            element.updateUpdateStatistics();
        }
        applyDefaultsToElementWithoutLifespanSet(element);

        backOffIfDiskSpoolFull();


        memoryStore.put(element);

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

        if (diskStore != null && diskStore.bufferFull()) {
            //back off to avoid OutOfMemoryError
            try {
                Thread.sleep(BACK_OFF_TIME_MILLIS);
            } catch (InterruptedException e) {
                //do not care if this happens
            }
        }
    }

    private void applyDefaultsToElementWithoutLifespanSet(Element element) {
        if (!element.isLifespanSet()) {
            //Setting with Cache defaults
            element.setTimeToLive((int) configuration.getTimeToLiveSeconds());
            element.setTimeToIdle((int) configuration.getTimeToIdleSeconds());
            element.setEternal(configuration.isEternal());
        }
    }


    /**
     * Put an element in the cache, without updating statistics, or updating listeners. This is meant to be used
     * in conjunction with {@link #getQuiet}.
     * Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those cirucmstances.
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

        memoryStore.put(element);
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
        Element element;
        long start = System.currentTimeMillis();

        element = searchInMemoryStore(key, true);
        if (element == null && isDiskStore()) {
            element = searchInDiskStore(key, true);
        }
        if (element == null) {
            missCountNotFound++;
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest(configuration.getName() + " cache - Miss");
            }
        } else {
            hitCount++;
        }
        long end = System.currentTimeMillis();
        totalGetTime += (end - start);
        return element;
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
            Future future = asynchronousLoad(key, loader, loaderArgument);
            //wait for result
            future.get();
        } catch (Exception e) {
            throw new CacheException("Exception on load for key " + key, e);
        }
        return getQuiet(key);
    }

    /**
     * The load method provides a means to "pre load" the cache. This method will, asynchronously, load the specified
     * object into the cache using the associated cacheloader. If the object already exists in the cache, no action is
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
     * @param key key whose associated value to be loaded using the associated cacheloader if this cache doesn't contain it.
     * @throws CacheException
     */
    public void load(final Object key) throws CacheException {
        if (registeredCacheLoaders.size() == 0) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("The CacheLoader is null. Returning.");
            }
            return;
        }

        boolean existsOnCall = isKeyInCache(key);
        if (existsOnCall) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("The key " + key + " exists in the cache. Returning.");
            }
            return;
        }

        asynchronousLoad(key, null, null);
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

                    if (isKeyInCache(key)) {
                        Element element = get(key);
                        if (element != null) {
                            map.put(key, element.getObjectValue());
                        } else {
                            map.put(key, null);
                        }
                    } else {
                        missingKeys.add(key);
                    }
                }

                //now load everything that's missing.
                Future future = asynchronousLoadAll(missingKeys, loaderArgument);
                future.get();


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
     * the specified objects into the cache using the associated cache loader. If the an object already exists in the
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
     * cacheloader if this cache doesn't contain them.
     * <p/>
     * The Ehcache native API provides similar functionality to loaders using the
     * decorator {@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}
     */
    public void loadAll(final Collection keys, final Object argument) throws CacheException {

        if (registeredCacheLoaders.size() == 0) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("The CacheLoader is null. Returning.");
            }
            return;
        }
        if (keys == null) {
            return;
        }
        asynchronousLoadAll(keys, argument);
    }

    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * still updated.
     * <p/>
     * Synchronization is handled within the method.
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
     * Synchronization is handled within the method.
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see #isExpired
     * @since 1.2
     */
    public final Element getQuiet(Object key) throws IllegalStateException, CacheException {
        checkStatus();
        Element element;

        element = searchInMemoryStore(key, false);
        if (element == null && isDiskStore()) {
            element = searchInDiskStore(key, false);
        }
        return element;
    }

    /**
     * Returns a list of all element keys in the cache, whether or not they are expired.
     * <p/>
     * The returned keys are unique and can be considered a set.
     * <p/>
     * The List returned is not live. It is a copy.
     * <p/>
     * The time taken is O(n). On a single cpu 1.8Ghz P4, approximately 8ms is required
     * for each 1000 entries.
     *
     * @return a list of {@link Object} keys
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final List getKeys() throws IllegalStateException, CacheException {
        checkStatus();
        /* An element with the same key can exist in both the memory store and the
            disk store at the same time. Because the memory store is always searched first
            these duplicates do not cause problems when getting elements/

            This method removes these duplicates before returning the list of keys*/
        List<Object> allKeyList = new ArrayList<Object>();
        List<Object> keyList = Arrays.asList(memoryStore.getKeyArray());
        allKeyList.addAll(keyList);
        if (isDiskStore()) {
            Set<Object> allKeys = new HashSet<Object>();
            //within the store keys will be unique
            allKeys.addAll(keyList);
            Object[] diskKeys = diskStore.getKeyArray();
            for (Object diskKey : diskKeys) {
                if (allKeys.add(diskKey)) {
                    //Unique, so add it to the list
                    allKeyList.add(diskKey);
                }
            }
        }
        return allKeyList;
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
     * is not syncrhonized, because it relies on a non-live list returned from {@link #getKeys()}
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
        //remove keys of expired elements
        ArrayList<Object> nonExpiredKeys = new ArrayList<Object>(allKeyList.size());
        int allKeyListSize = allKeyList.size();
        for (int i = 0; i < allKeyListSize; i++) {
            Object key = allKeyList.get(i);
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
     * The time taken is O(log n). On a single cpu 1.8Ghz P4, approximately 6ms is required
     * for 1000 entries and 36 for 50000.
     * <p/>
     * This is the fastest getKeys method
     *
     * @return a list of {@link Object} keys
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final List getKeysNoDuplicateCheck() throws IllegalStateException {
        checkStatus();
        ArrayList<Object> allKeys = new ArrayList<Object>();
        List<Object> memoryKeySet = Arrays.asList(memoryStore.getKeyArray());
        allKeys.addAll(memoryKeySet);
        if (isDiskStore()) {
            List<Object> diskKeySet = Arrays.asList(diskStore.getKeyArray());
            allKeys.addAll(diskKeySet);
        }
        return allKeys;
    }

    private Element searchInMemoryStore(Object key, boolean updateStatistics) {
        Element element;
        if (updateStatistics) {
            element = memoryStore.get(key);
        } else {
            element = memoryStore.getQuiet(key);
        }
        if (element != null) {
            if (isExpired(element)) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(configuration.getName() + " Memory cache hit, but element expired");
                }
                if (updateStatistics) {
                    missCountExpired++;
                }
                remove(key, true, true, false);
                element = null;
            } else {
                if (updateStatistics) {
                    memoryStoreHitCount++;
                }
            }
        }
        return element;
    }

    private Element searchInDiskStore(Object key, boolean updateStatistics) {
        if (!(key instanceof Serializable)) {
            return null;
        }
        Serializable serializableKey = (Serializable) key;
        Element element;
        if (updateStatistics) {
            element = diskStore.get(serializableKey);
        } else {
            element = diskStore.getQuiet(serializableKey);
        }
        if (element != null) {
            if (isExpired(element)) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(configuration.getName() + " cache - Disk Store hit, but element expired");
                }
                missCountExpired++;
                remove(key, true, true, false);
                element = null;
            } else {
                diskStoreHitCount++;
                //Put the item back into memory to preserve policies in the memory store and to save updated statistics
                //todo - maybe make the DiskStore a one-way evict. i.e. Do not replace See testGetSpeedMostlyDisk for speed comp.
                memoryStore.put(element);
            }
        }
        return element;
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
     * This exception should be caught in those cirucmstances.
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
     * This exception should be caught in those cirucmstances.
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
     * Removes an {@link Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element
     * with the key actually existed.
     * <p/>
     * Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those cirucmstances.
     *
     * @param key                         the element key to operate on
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
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
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove(key, false, true, doNotNotifyCacheReplicators);
    }

    /**
     * Removes an {@link Element} from the Cache, without notifying listeners. This also removes it from any
     * stores it may be in.
     * <p/>
     * Synchronization is handled within the method.
     *
     * @param key the element key to operate on
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final boolean removeQuiet(Serializable key) throws IllegalStateException {
        return remove(key, false, false, false);
    }

    /**
     * Removes an {@link Element} from the Cache, without notifying listeners. This also removes it from any
     * stores it may be in.
     * <p/>
     * Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those cirucmstances.
     *
     * @param key the element key to operate on
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @since 1.2
     */
    public final boolean removeQuiet(Object key) throws IllegalStateException {
        return remove(key, false, false, false);
    }


    /**
     * Removes or expires an {@link Element} from the Cache after an attempt to get it determined that it should be expired.
     * This also removes it from any stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element has expired, but only if an Element
     * with the key actually existed.
     * <p/>
     * Synchronization is handled within the method.
     * <p/>
     * If a remove was called, listeners are notified, regardless of whether the element existed or not.
     * This allows distributed cache listeners to remove elements from a cluster regardless of whether they
     * existed locally.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those cirucmstances.
     *
     * @param key                         the element key to operate on
     * @param expiry                      if the reason this method is being called is to expire the element
     * @param notifyListeners             whether to notify listeners
     * @param doNotNotifyCacheReplicators whether not to notify cache replicators
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    private boolean remove(Object key, boolean expiry, boolean notifyListeners,
                           boolean doNotNotifyCacheReplicators)
            throws IllegalStateException {
        checkStatus();
        boolean removed = false;
        Element elementFromMemoryStore;
        Element elementFromDiskStore;
        elementFromMemoryStore = memoryStore.remove(key);

        //could have been removed from both places, if there are two copies in the cache
        elementFromDiskStore = null;
        if (isDiskStore()) {
            if ((key instanceof Serializable)) {
                Serializable serializableKey = (Serializable) key;
                elementFromDiskStore = diskStore.remove(serializableKey);
            }

        }

        boolean removeNotified = false;

        if (elementFromMemoryStore != null) {
            if (expiry) {
                //always notify expire which is lazy regardless of the removeQuiet
                registeredEventListeners.notifyElementExpiry(elementFromMemoryStore, doNotNotifyCacheReplicators);
            } else if (notifyListeners) {
                removeNotified = true;
                registeredEventListeners.notifyElementRemoved(elementFromMemoryStore, doNotNotifyCacheReplicators);
            }
            removed = true;
        }
        if (elementFromDiskStore != null) {
            if (expiry) {
                registeredEventListeners.notifyElementExpiry(elementFromDiskStore, doNotNotifyCacheReplicators);
            } else if (notifyListeners) {
                removeNotified = true;
                registeredEventListeners.notifyElementRemoved(elementFromDiskStore, doNotNotifyCacheReplicators);
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
     * Removes all cached items.
     * Synchronization is handled within the method.
     * <p/>
     * Caches which use synchronous replication can throw RemoteCacheException here if the replication to the cluster fails.
     * This exception should be caught in those cirucmstances.
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
     * This exception should be caught in those cirucmstances.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        checkStatus();
        memoryStore.removeAll();
        if (isDiskStore()) {
            diskStore.removeAll();
        }
        registeredEventListeners.notifyRemoveAll(doNotNotifyCacheReplicators);
    }

    /**
     * Starts an orderly shutdown of the Cache. Steps are:
     * <ol>
     * <li>Completes any outstanding CacheLoader loads.
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
        checkStatusNotDisposed();

        if (executorService != null) {
            executorService.shutdown();
        }
        disposeRegisteredCacheExtensions();
        disposeRegisteredCacheLoaders();
        registeredEventListeners.dispose();

        if (memoryStore != null) {
            memoryStore.dispose();
        }
        if (diskStore != null) {
            diskStore.dispose();
        }
        changeStatus(Status.STATUS_SHUTDOWN);
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
            memoryStore.flush();
            if (isDiskStore()) {
                diskStore.flush();
            }
        } catch (IOException e) {
            throw new CacheException("Unable to flush cache: " + configuration.getName()
                    + ". Initial cause was " + e.getMessage(), e);
        }
    }


    /**
     * Gets the size of the cache. This is a subtle concept. See below.
     * <p/>
     * The size is the number of {@link Element}s in the {@link MemoryStore} plus
     * the number of {@link Element}s in the {@link DiskStore}.
     * <p/>
     * This number is the actual number of elements, including expired elements that have
     * not been removed.
     * <p/>
     * Expired elements are removed from the the memory store when
     * getting an expired element, or when attempting to spool an expired element to
     * disk.
     * <p/>
     * Expired elements are removed from the disk store when getting an expired element,
     * or when the expiry thread runs, which is once every five minutes.
     * <p/>
     * To get an exact size, which would exclude expired elements, use {@link #getKeysWithExpiryCheck()}.size(),
     * although see that method for the approximate time that would take.
     * <p/>
     * To get a very fast result, use {@link #getKeysNoDuplicateCheck()}.size(). If the disk store
     * is being used, there will be some duplicates.
     *
     * @return The size value
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final int getSize() throws IllegalStateException, CacheException {
        checkStatus();
        /* The memory store and the disk store can simultaneously contain elements with the same key
            Cache size is the size of the union of the two key sets.*/
        return getKeys().size();
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
        return memoryStore.getSizeInBytes();
    }


    /**
     * Returns the number of elements in the memory store.
     *
     * @return the number of elements in the memory store
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final long getMemoryStoreSize() throws IllegalStateException {
        checkStatus();
        return memoryStore.getSize();
    }

    /**
     * Returns the number of elements in the disk store.
     *
     * @return the number of elements in the disk store.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final int getDiskStoreSize() throws IllegalStateException {
        checkStatus();
        if (isDiskStore()) {
            return diskStore.getSize();
        } else {
            return 0;
        }
    }

    /**
     * Gets the status attribute of the Cache.
     *
     * @return The status value from the Status enum class
     */
    public final Status getStatus() {
        return status;
    }


    private void checkStatus() throws IllegalStateException {
        if (!status.equals(Status.STATUS_ALIVE)) {
            throw new IllegalStateException("The " + configuration.getName() + " Cache is not alive.");
        }
    }

    private void checkStatusNotDisposed() throws IllegalStateException {
        if (status.equals(Status.STATUS_SHUTDOWN)) {
            throw new IllegalStateException("The " + configuration.getName() + " Cache is disposed.");
        }
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
        if (!status.equals(Status.STATUS_UNINITIALISED)) {
            throw new IllegalStateException("Only unitialised caches can have their names set.");
        }
        configuration.setName(name);
    }

    /**
     * Returns a {@link String} representation of {@link Cache}.
     */
    public final String toString() {
        StringBuffer dump = new StringBuffer();

        dump.append("[")
                .append(" name = ").append(configuration.getName())
                .append(" status = ").append(status)
                .append(" eternal = ").append(configuration.isEternal())
                .append(" overflowToDisk = ").append(configuration.isOverflowToDisk())
                .append(" maxElementsInMemory = ").append(configuration.getMaxElementsInMemory())
                .append(" maxElementsOnDisk = ").append(configuration.getMaxElementsOnDisk())
                .append(" memoryStoreEvictionPolicy = ").append(configuration.getMemoryStoreEvictionPolicy())
                .append(" timeToLiveSeconds = ").append(configuration.getTimeToLiveSeconds())
                .append(" timeToIdleSeconds = ").append(configuration.getTimeToIdleSeconds())
                .append(" diskPersistent = ").append(configuration.isDiskPersistent())
                .append(" diskExpiryThreadIntervalSeconds = ").append(configuration.getDiskExpiryThreadIntervalSeconds())
                .append(registeredEventListeners)
                .append(" hitCount = ").append(hitCount)
                .append(" memoryStoreHitCount = ").append(memoryStoreHitCount)
                .append(" diskStoreHitCount = ").append(diskStoreHitCount)
                .append(" missCountNotFound = ").append(missCountNotFound)
                .append(" missCountExpired = ").append(missCountExpired)
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
     */
    public final boolean isExpired(Element element) throws IllegalStateException, NullPointerException {
        checkStatus();
        synchronized (element) {
            return element.isExpired();
        }
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
    public final Object clone() throws CloneNotSupportedException {
        if (!(memoryStore == null && diskStore == null)) {
            throw new CloneNotSupportedException("Cannot clone an initialized cache.");
        }
        Cache copy = (Cache) super.clone();
        copy.configuration = (CacheConfiguration) configuration.clone();
        copy.guid = createGuid();

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

        if (bootstrapCacheLoader != null) {
            BootstrapCacheLoader bootstrapCacheLoaderClone = (BootstrapCacheLoader) bootstrapCacheLoader.clone();
            copy.setBootstrapCacheLoader(bootstrapCacheLoaderClone);
        }

        return copy;
    }

    /**
     * Gets the internal DiskStore.
     *
     * @return the DiskStore referenced by this cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    final Store getDiskStore() throws IllegalStateException {
        checkStatus();
        return diskStore;
    }

    /**
     * Gets the internal MemoryStore.
     *
     * @return the MemoryStore referenced by this cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    final Store getMemoryStore() throws IllegalStateException {
        checkStatus();
        return memoryStore;
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
     *
     * @return true if an element matching the key is found in memory
     */
    public final boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    /**
     * Whether an Element is stored in the cache in Memory, indicating a very low cost of retrieval.
     *
     * @return true if an element matching the key is found in memory
     * @since 1.2
     */
    public final boolean isElementInMemory(Object key) {
        return memoryStore.containsKey(key);
    }

    /**
     * Whether an Element is stored in the cache on Disk, indicating a higher cost of retrieval.
     *
     * @return true if an element matching the key is found in the diskStore
     */
    public final boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    /**
     * Whether an Element is stored in the cache on Disk, indicating a higher cost of retrieval.
     *
     * @return true if an element matching the key is found in the diskStore
     * @since 1.2
     */
    public final boolean isElementOnDisk(Object key) {
        if (!(key instanceof Serializable)) {
            return false;
        }
        Serializable serializableKey = (Serializable) key;
        if (isDiskStore()) {
            return diskStore != null && diskStore.containsKey(serializableKey);
        } else {
            return false;
        }
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
        hitCount = 0;
        memoryStoreHitCount = 0;
        diskStoreHitCount = 0;
        missCountExpired = 0;
        missCountNotFound = 0;
        totalGetTime = 0;
        registeredEventListeners.clearCounters();
    }

    /**
     * Accurately measuring statistics can be expensive. Returns the current accuracy setting.
     *
     * @return one of {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT}, {@link Statistics#STATISTICS_ACCURACY_GUARANTEED}, {@link Statistics#STATISTICS_ACCURACY_NONE}
     */
    public int getStatisticsAccuracy() {
        return statisticsAccuracy;
    }

    /**
     * Sets the statistics accuracy.
     *
     * @param statisticsAccuracy one of {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT}, {@link Statistics#STATISTICS_ACCURACY_GUARANTEED}, {@link Statistics#STATISTICS_ACCURACY_NONE}
     */
    public void setStatisticsAccuracy(int statisticsAccuracy) {
        this.statisticsAccuracy = statisticsAccuracy;
    }

    /**
     * Causes all elements stored in the Cache to be synchronously checked for expiry, and if expired, evicted.
     */
    public void evictExpiredElements() {
        Object[] keys = memoryStore.getKeyArray();

        for (Object key : keys) {
            searchInMemoryStore(key, false);
        }

        //This is called regularly by the expiry thread, but call it here synchronously
        if (isDiskStore()) {
            diskStore.expireElements();
        }
    }

    /**
     * An inexpensive check to see if the key exists in the cache.
     * <p/>
     * This method is not synchronized. It is possible that an element may exist in the cache aned be removed
     * before the check gets to it, or vice versa.
     *
     * @param key the key to check.
     * @return true if an Element matching the key is found in the cache. No assertions are made about the state of the Element.
     */
    public boolean isKeyInCache(Object key) {
        return isElementInMemory(key) || isElementOnDisk(key);
    }

    /**
     * An extremely expensive check to see if the value exists in the cache. This implementation is O(n). Ehcache
     * is not designed for efficient access in this manner.
     * <p/>
     * This method is not synchronized. It is possible that an element may exist in the cache aned be removed
     * before the check gets to it, or vice versa. Because it is slow to execute the probability of that this will
     * have happened.
     *
     * @param value to check for
     * @return true if an Element matching the key is found in the cache. No assertions are made about the state of the Element.
     */
    public boolean isValueInCache(Object value) {
        boolean isSerializable = value instanceof Serializable;
        List keys;
        if (isSerializable) {
            keys = getKeys();
        } else {
            keys = Arrays.asList(memoryStore.getKeyArray());
        }

        for (Object key : keys) {
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
     * Note, the {@link #getSize} method will have the same value as the size reported by Statistics
     * for the statistics accuracy of {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT}.
     */
    public Statistics getStatistics() throws IllegalStateException {
        int size = 0;
        if (statisticsAccuracy == Statistics.STATISTICS_ACCURACY_BEST_EFFORT) {
            size = getSize();
        } else if (statisticsAccuracy == Statistics.STATISTICS_ACCURACY_GUARANTEED) {
            size = getKeysWithExpiryCheck().size();
        } else if (statisticsAccuracy == Statistics.STATISTICS_ACCURACY_NONE) {
            size = getKeysNoDuplicateCheck().size();
        }
        return new Statistics(this, statisticsAccuracy, hitCount, diskStoreHitCount, memoryStoreHitCount,
                missCountExpired + missCountNotFound, size, getAverageGetTime(),
                registeredEventListeners.getElementsEvictedCounter(),
                getMemoryStoreSize(), getDiskStoreSize());
    }

    /**
     * For use by CacheManager.
     *
     * @param cacheManager the CacheManager for this cache to use.
     */
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
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
        if (!status.equals(Status.STATUS_UNINITIALISED)) {
            throw new CacheException("A bootstrap cache loader can only be set before the cache is initialized. "
                    + configuration.getName());
        }
        this.bootstrapCacheLoader = bootstrapCacheLoader;
    }

    /**
     * DiskStore paths can conflict between CacheManager instances. This method allows the path to be changed.
     *
     * @param diskStorePath the new path to be used.
     * @throws CacheException if this method is called after the cache is initialized
     */
    public void setDiskStorePath(String diskStorePath) throws CacheException {
        if (!status.equals(Status.STATUS_UNINITIALISED)) {
            throw new CacheException("A DiskStore path can only be set before the cache is initialized. "
                    + configuration.getName());
        }
        this.diskStorePath = diskStorePath;
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
     * Java<font size="-2"><sup>TM</sup></font> programming language.)
     * <p/>
     * This implementation use the GUID of the cache.
     *
     * @return a hash code value for this object.
     * @see Object#equals(Object)
     * @see java.util.Hashtable
     */
    public int hashCode() {
        return guid.hashCode();
    }


    /**
     * Create globally unique ID for this cache.
     */
    private String createGuid() {
        return new StringBuffer().append(localhost).append("-").append(new UID()).toString();
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
        if (hitCount == 0) {
            return 0;
        } else {
            return (float) totalGetTime / hitCount;
        }
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
        this.cacheExceptionHandler = cacheExceptionHandler;
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
     * Register a {@link CacheLoader} with the cache. It will then be tied into the cache lifecycle.
     * <p/>
     * If the CacheLoader is not initialised, initialise it.
     *
     * @param cacheLoader A Cache Loader to register
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
     * Does the asynchronous loading.
     *
     * @param key
     * @param specificLoader a specific loader to use. If null the default loader is used.
     * @param argument
     * @return a Future which can be used to monitor execution
     */
    Future asynchronousLoad(final Object key, final CacheLoader specificLoader, final Object argument) {
        return getExecutorService().submit(new Runnable() {

            /**
             * Calls the CacheLoader and puts the result in the Cache
             */
            public void run() throws CacheException {
                try {
                    //Test to see if it has turned up in the meantime
                    boolean existsOnRun = isKeyInCache(key);
                    if (!existsOnRun) {
                        Object value;
                        if (specificLoader == null) {
                            if (registeredCacheLoaders.size() == 0) {
                                return;
                            }
                            value = loadWithRegisteredLoaders(argument, key);
                        } else {
                            if (argument == null) {
                                value = specificLoader.load(key);
                            } else {
                                value = specificLoader.load(key, argument);
                            }
                        }
                        if (value != null) {
                            put(new Element(key, value), false);
                        }
                    }
                } catch (Throwable e) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Problem during load. Load will not be completed. Cause was " + e.getCause(), e);
                    }
                    throw new CacheException("Problem during load. Load will not be completed. Cause was " + e.getCause(), e);
                }
            }
        });
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
     * Does the asynchronous loading.
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
                    List<Object> nonLoadedKeys = new ArrayList<Object>(keys.size());
                    for (Object key : keys) {
                        if (!isKeyInCache(key)) {
                            nonLoadedKeys.add(key);
                        }
                    }
                    Map map = null;
                    if (argument == null) {
                        for (CacheLoader registeredCacheLoader : registeredCacheLoaders) {
                            map = registeredCacheLoader.loadAll(nonLoadedKeys);
                            if (map != null) {
                                break;
                            }
                        }
                    } else {
                        for (CacheLoader registeredCacheLoader : registeredCacheLoaders) {
                            map = registeredCacheLoader.loadAll(nonLoadedKeys, argument);
                            if (map != null) {
                                break;
                            }
                        }
                    }
                    if (map != null) {
                        for (Object key : map.keySet()) {
                            put(new Element(key, map.get(key)));
                        }
                    }
                } catch (Throwable e) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Problem during load. Load will not be completed. Cause was " + e.getCause(), e);
                    }
                }
            }
        });
    }

    /**
     * @return Gets the executor service. This is not publically accessible.
     */
    ThreadPoolExecutor getExecutorService() {
        if (executorService == null) {
            synchronized (this) {
                executorService = new ThreadPoolExecutor(EXECUTOR_CORE_POOL_SIZE, EXECUTOR_MAXIMUM_POOL_SIZE,
                        EXECUTOR_KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());
            }
        }
        return executorService;
    }


    /**
     * Whether this cache is disabled. "Disabled" means:
     * <ol>
     * <li>bootstrap is disabled
     * <li>puts are discarded
     * <li>putQuites are discarded
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
        this.disabled = disabled;
    }

    /**
     * @return the current MemoryStore policy. This may not be the configured policy, if it has been
     *         dynamically set.
     */
    public Policy getMemoryStoreEvictionPolicy() {
        return memoryStore.getEvictionPolicy();
    }

    /**
     * Sets the eviction policy strategy. The Cache will use a policy at startup. There are three policies
     * which can be configured: LRU, LFU and FIFO. However many other policies are possible. That the policy
     * has access to the whole element enables policies based on the key, value, metadata, statistics, or a combination of
     * any of the above. It is safe to change the policy of a store at any time. The new policy takes effect
     * immediately.
     *
     * @param policy the new policy
     */
    public void setMemoryStoreEvictionPolicy(Policy policy) {
        memoryStore.setEvictionPolicy(policy);
    }


}
