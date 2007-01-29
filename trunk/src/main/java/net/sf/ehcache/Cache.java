/**
 *  Copyright 2003-2007 Greg Luck
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
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.DiskStore;
import net.sf.ehcache.store.MemoryStore;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.config.CacheConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
 * Statistics on cache usage are collected and made available through public methods.
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
     * The default interval between runs of the expiry thread.
     */
    public static final long DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS = 120;

    private static final Log LOG = LogFactory.getLog(Cache.class.getName());

    private static final MemoryStoreEvictionPolicy DEFAULT_MEMORY_STORE_EVICTION_POLICY = MemoryStoreEvictionPolicy.LRU;


    private static InetAddress localhost;

    static {
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOG.error("Unable to set localhost. This prevents creation of a GUID. Cause was: " + e.getMessage(), e);
        }
    }

    private boolean disabled;

    private DiskStore diskStore;

    private String diskStorePath;

    private Status status;

    private final CacheConfiguration configuration;

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
    private MemoryStore memoryStore;

    private RegisteredEventListeners registeredEventListeners;

    private final String guid = new StringBuffer().append(localhost).append("-").append(new UID()).toString();

    private CacheManager cacheManager;

    private BootstrapCacheLoader bootstrapCacheLoader;

    private int statisticsAccuracy;

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
     * @param name                the name of the cache
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
     * @param name                the name of the cache
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
        LOG.warn("An API change between ehcache-1.1 and ehcache-1.2 results in the persistence path being set to java.io.tmp" +
                " when the ehcache-1.1 constructor is used. Please change to the 1.2 constructor");
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
     * @param name                      the name of the cache
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
     * @param name                      the name of the cache
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
     * @param name                      the name of the cache
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

        changeStatus(Status.STATUS_UNINITIALISED);

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

        if (diskStorePath == null) {
            this.diskStorePath = System.getProperty("java.io.tmpdir");
        } else {
            this.diskStorePath = diskStorePath;
        }

        if (registeredEventListeners == null) {
            this.registeredEventListeners = new RegisteredEventListeners(this);
        } else {
            this.registeredEventListeners = registeredEventListeners;
        }

        //Set this to a safe value.
        if (diskExpiryThreadIntervalSeconds == 0) {
            configuration.setDiskExpiryThreadIntervalSeconds(DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS);
        } else {
            configuration.setDiskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds);
        }

        // For backward compatibility with 1.1 and earlier
        if (memoryStoreEvictionPolicy == null) {
            configuration.setMemoryStoreEvictionPolicyFromObject(DEFAULT_MEMORY_STORE_EVICTION_POLICY);
        }

        this.bootstrapCacheLoader = bootstrapCacheLoader;

        statisticsAccuracy = Statistics.STATISTICS_ACCURACY_BEST_EFFORT;

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
     * @param configuration an object representing the configuration of the cache
     * todo hook this up properly
     */
    public Cache(CacheConfiguration configuration) {
        this.configuration = configuration;
        throw new CacheException("not fully implemented yet");
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
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Cache: " + configuration.getName()
                            + " has a maxElementsInMemory of 0. It is strongly recommended to " +
                            "have a maximumSize of at least 1. Performance is halved by not using a MemoryStore.");
                }
            }

            if (configuration.isOverflowToDisk()) {
                diskStore = new DiskStore(this, diskStorePath);
            }

            memoryStore = MemoryStore.create(this, diskStore);
            changeStatus(Status.STATUS_ALIVE);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Initialised cache: " + configuration.getName());
        }

        if (disabled) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Cache: " + configuration.getName() + " is disabled because the " + NET_SF_EHCACHE_DISABLED
                        + " property was set to true. No elements will be added to the cache.");
            }
        }
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
     * Synchronization is handled within the method.
     *
     * @param element An object. If Serializable it can fully participate in replication and the DiskStore.
     * @throws IllegalStateException    if the cache is not {@link Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
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
     * Synchronization is handled within the method.
     *
     * @param element                     An object. If Serializable it can fully participate in replication and the DiskStore.
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
            throw new IllegalArgumentException("Element cannot be null");
        }

        element.resetAccessStatistics();
        boolean elementExists;
        Object key = element.getObjectKey();
        elementExists = isElementInMemory(key) || isElementOnDisk(key);
        if (elementExists) {
            element.updateUpdateStatistics();
        }
        applyDefaultsToElementWithoutLifespanSet(element);

        synchronized (this) {
            memoryStore.put(element);
        }
        if (elementExists) {
            registeredEventListeners.notifyElementUpdated(element, doNotNotifyCacheReplicators);
        } else {
            registeredEventListeners.notifyElementPut(element, doNotNotifyCacheReplicators);
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
     *
     * @param element An object. If Serializable it can fully participate in replication and the DiskStore.
     * @throws IllegalStateException    if the cache is not {@link Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public final void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException,
            CacheException {
        checkStatus();

        if (disabled) {
            return;
        }

        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }

        applyDefaultsToElementWithoutLifespanSet(element);

        synchronized (this) {
            memoryStore.put(element);
        }
    }

    /**
     * Gets an element from the cache. Updates Element Statistics
     * <p/>
     * Note that the Element's lastAccessTime is always the time of this get.
     * Use {@link #getQuiet(Object)} to peak into the Element to see its last access time with get
     * <p/>
     * Synchronization is handled within the method.
     *
     * @param key a serializable value
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

        synchronized (this) {
            element = searchInMemoryStore(key, true);
            if (element == null && configuration.isOverflowToDisk()) {
                element = searchInDiskStore(key, true);
            }
            if (element == null) {
                missCountNotFound++;
                if (LOG.isTraceEnabled()) {
                    LOG.trace(configuration.getName() + " cache - Miss");
                }
            } else {
                hitCount++;
            }
        }
        return element;
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

        synchronized (this) {
            element = searchInMemoryStore(key, false);
            if (element == null && configuration.isOverflowToDisk()) {
                element = searchInDiskStore(key, false);
            }
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
    public final synchronized List getKeys() throws IllegalStateException, CacheException {
        checkStatus();
        /* An element with the same key can exist in both the memory store and the
            disk store at the same time. Because the memory store is always searched first
            these duplicates do not cause problems when getting elements/

            This method removes these duplicates before returning the list of keys*/
        List allKeyList = new ArrayList();
        List keyList = Arrays.asList(memoryStore.getKeyArray());
        allKeyList.addAll(keyList);
        if (configuration.isOverflowToDisk()) {
            Set allKeys = new HashSet();
            //within the store keys will be unique
            allKeys.addAll(keyList);
            Object[] diskKeys = diskStore.getKeyArray();
            for (int i = 0; i < diskKeys.length; i++) {
                Object diskKey = diskKeys[i];
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
        ArrayList nonExpiredKeys = new ArrayList(allKeyList.size());
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
    public final synchronized List getKeysNoDuplicateCheck() throws IllegalStateException {
        checkStatus();
        ArrayList allKeys = new ArrayList();
        List memoryKeySet = Arrays.asList(memoryStore.getKeyArray());
        allKeys.addAll(memoryKeySet);
        if (configuration.isOverflowToDisk()) {
            List diskKeySet = Arrays.asList(diskStore.getKeyArray());
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
                if (LOG.isDebugEnabled()) {
                    LOG.debug(configuration.getName() + " Memory cache hit, but element expired");
                }
                missCountExpired++;
                remove(key, true, true, false);
                element = null;
            } else {
                memoryStoreHitCount++;
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
                if (LOG.isDebugEnabled()) {
                    LOG.debug(configuration.getName() + " cache - Disk Store hit, but element expired");
                }
                missCountExpired++;
                remove(key, true, true, false);
                element = null;
            } else {
                diskStoreHitCount++;
                //Put the item back into memory to preserve policies in the memory store and to save updated statistics
                memoryStore.put(element);
            }
        }
        return element;
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
     *
     * @param key                         the element key to operate on
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @noinspection SameParameterValue
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
        synchronized (this) {
            elementFromMemoryStore = memoryStore.remove(key);

            //could have been removed from both places, if there are two copies in the cache
            elementFromDiskStore = null;
            if (configuration.isOverflowToDisk()) {
                if ((key instanceof Serializable)) {
                    Serializable serializableKey = (Serializable) key;
                    elementFromDiskStore = diskStore.remove(serializableKey);
                }

            }
        }

        boolean removeNotified = false;

        if (elementFromMemoryStore != null) {
            if (notifyListeners) {
                if (expiry) {
                    registeredEventListeners.notifyElementExpiry(elementFromMemoryStore, doNotNotifyCacheReplicators);
                } else {
                    removeNotified = true;
                    registeredEventListeners.notifyElementRemoved(elementFromMemoryStore, doNotNotifyCacheReplicators);
                }
            }
            removed = true;
        }
        if (elementFromDiskStore != null) {
            if (expiry) {
                registeredEventListeners.notifyElementExpiry(elementFromDiskStore, doNotNotifyCacheReplicators);
            } else {
                removeNotified = true;
                registeredEventListeners.notifyElementRemoved(elementFromDiskStore, doNotNotifyCacheReplicators);
            }
            removed = true;
        }
        //If we are trying to remove an element which does not exist locally, we should still notify so that
        //cluster invalidations work.
        if (!expiry && !removeNotified) {
            Element syntheticElement = new Element(key, null);
            registeredEventListeners.notifyElementRemoved(syntheticElement, doNotNotifyCacheReplicators);
        }

        return removed;
    }

    /**
     * Removes all cached items.
     * Synchronization is handled within the method.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        removeAll(false);
    }


    /**
     * Removes all cached items.
     * Synchronization is handled within the method.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        checkStatus();
        synchronized (this) {
            memoryStore.removeAll();
            if (configuration.isOverflowToDisk()) {
                diskStore.removeAll();
            }
        }
        registeredEventListeners.notifyRemoveAll(doNotNotifyCacheReplicators);
    }

    /**
     * Flushes all cache items from memory to auxilliary caches and close the auxilliary caches.
     * <p/>
     * Should be invoked only by CacheManager.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public synchronized void dispose() throws IllegalStateException {
        checkStatus();
        memoryStore.dispose();
        memoryStore = null;
        if (configuration.isOverflowToDisk()) {
            diskStore.dispose();
            diskStore = null;
        }

        registeredEventListeners.dispose();

        changeStatus(Status.STATUS_SHUTDOWN);
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
            if (configuration.isOverflowToDisk()) {
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
    public final synchronized int getSize() throws IllegalStateException, CacheException {
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
    public final synchronized long calculateInMemorySize() throws IllegalStateException, CacheException {
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
        if (configuration.isOverflowToDisk()) {
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


    /**
     * The number of times a requested item was found in the cache.
     * <p/>
     * The internal representation of this statistic has been changed to a long
     * to cater for real world situations when an int is not sufficient to hold it.
     * If the statistic is larger than <code>Integer.MAX_VALUE</code> a nonsensical value
     * will be returned. Use {@link net.sf.ehcache.Statistics} which contains this statistic.
     * <p/>
     *
     * @return the number of times a requested item was found in the cache
     * @deprecated Use {@link net.sf.ehcache.Statistics}
     */
    public final int getHitCount() {
        return (int) hitCount;
    }

    /**
     * Number of times a requested item was found in the Memory Store.
     * <p/>
     * The internal representation of this statistic has been changed to a long
     * to cater for real world situations when an int is not sufficient to hold it.
     * If the statistic is larger than <code>Integer.MAX_VALUE</code> a nonsensical value
     * will be returned. Use {@link net.sf.ehcache.Statistics} which contains this statistic.
     * <p/>
     *
     * @return Number of times a requested item was found in the Memory Store.
     * @deprecated Use {@link net.sf.ehcache.Statistics}
     */
    public final int getMemoryStoreHitCount() {
        return (int) memoryStoreHitCount;
    }

    /**
     * Number of times a requested item was found in the Disk Store.
     * <p/>
     * The internal representation of this statistic has been changed to a long
     * to cater for real world situations when an int is not sufficient to hold it.
     * If the statistic is larger than <code>Integer.MAX_VALUE</code> a nonsensical value
     * will be returned. Use {@link net.sf.ehcache.Statistics} which contains this statistic.
     * <p/>
     *
     * @deprecated Use {@link net.sf.ehcache.Statistics}
     */
    public final int getDiskStoreHitCount() {
        return (int) diskStoreHitCount;
    }

    /**
     * Number of times a requested element was not found in the cache. This
     * may be because it expired, in which case this will also be recorded in {@link #getMissCountExpired},
     * or because it was simply not there.
     * <p/>
     * The internal representation of this statistic has been changed to a long
     * to cater for real world situations when an int is not sufficient to hold it.
     * If the statistic is larger than <code>Integer.MAX_VALUE</code> a nonsensical value
     * will be returned. Use {@link net.sf.ehcache.Statistics} which contains this statistic.
     * <p/>
     *
     * @deprecated Use {@link net.sf.ehcache.Statistics}
     */
    public final int getMissCountNotFound() {
        return (int) missCountNotFound;
    }

    /**
     * Number of times a requested element was found but was expired.
     * <p/>
     * The internal representation of this statistic has been changed to a long
     * to cater for real world situations when an int is not sufficient to hold it.
     * If the statistic is larger than <code>Integer.MAX_VALUE</code> a nonsensical value
     * will be returned. Use {@link net.sf.ehcache.Statistics} which contains this statistic.
     * <p/>
     *
     * @deprecated Use {@link net.sf.ehcache.Statistics}
     */
    public final int getMissCountExpired() {
        return (int) missCountExpired;
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
     * Gets timeToIdleSeconds.
     *
     * @deprecated Get this from the configuration
     */
    public final long getTimeToIdleSeconds() {
        return configuration.getTimeToIdleSeconds();
    }

    /**
     * Gets timeToLiveSeconds.
     *
     * @deprecated Get this from the configuration
     */
    public final long getTimeToLiveSeconds() {
        return configuration.getTimeToLiveSeconds();
    }

    /**
     * Are elements eternal.
     *
     * @deprecated Get this from the configuration
     */
    public final boolean isEternal() {
        return configuration.isEternal();
    }

    /**
     * Does the overflow go to disk.
     *
     * @deprecated Get this from the configuration
     */
    public final boolean isOverflowToDisk() {
        return configuration.isOverflowToDisk();
    }

    /**
     * Gets the maximum number of elements to hold in memory.
     *
     * @deprecated Get this from the configuration
     */
    public final int getMaxElementsInMemory() {
        return configuration.getMaxElementsInMemory();
    }

    /**
     * Gets the maximum number of elements to hold on Disk
     *
     * @deprecated Get this from the configuration
     */
    public int getMaxElementsOnDisk() {
        return configuration.getMaxElementsOnDisk();
    }

    /**
     * The policy used to evict elements from the {@link net.sf.ehcache.store.MemoryStore}.
     * This can be one of:
     * <ol>
     * <li>LRU - least recently used
     * <li>LFU - least frequently used
     * <li>FIFO - first in first out, the oldest element by creation time
     * </ol>
     * The default value is LRU
     *
     * @since 1.2
     * @deprecated Get this from the configuration
     */
    public final MemoryStoreEvictionPolicy getMemoryStoreEvictionPolicy() {
        return configuration.getMemoryStoreEvictionPolicy();
    }

    /**
     * @return true if the cache overflows to disk and the disk is persistent between restarts
     * @deprecated Get this from the configuration     *
     */
    public final boolean isDiskPersistent() {
        return configuration.isDiskPersistent();
    }

    /**
     * @return the interval between runs
     *         of the expiry thread, where it checks the disk store for expired elements. It is not the
     *         the timeToLiveSeconds.
     * @deprecated Get this from the configuration
     */
    public final long getDiskExpiryThreadIntervalSeconds() {
        return configuration.getDiskExpiryThreadIntervalSeconds();
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
     * {@link net.sf.ehcache.store.LruMemoryStore} or {@link net.sf.ehcache.store.DiskStore} has been created.
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
        RegisteredEventListeners registeredEventListenersFromCopy = copy.getCacheEventNotificationService();
        if (registeredEventListenersFromCopy == null || registeredEventListenersFromCopy.getCacheEventListeners().size() == 0) {
            copy.registeredEventListeners = new RegisteredEventListeners(copy);
        } else {
            copy.registeredEventListeners = new RegisteredEventListeners(copy);
            Set cacheEventListeners = registeredEventListeners.getCacheEventListeners();
            for (Iterator iterator = cacheEventListeners.iterator(); iterator.hasNext();) {
                CacheEventListener cacheEventListener = (CacheEventListener) iterator.next();
                CacheEventListener cacheEventListenerClone = (CacheEventListener) cacheEventListener.clone();
                copy.registeredEventListeners.registerListener(cacheEventListenerClone);
            }
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
    final DiskStore getDiskStore() throws IllegalStateException {
        checkStatus();
        return diskStore;
    }

    /**
     * Gets the internal MemoryStore.
     *
     * @return the MemoryStore referenced by this cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    final MemoryStore getMemoryStore() throws IllegalStateException {
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
        if (!configuration.isOverflowToDisk()) {
            return false;
        } else {
            return diskStore != null && diskStore.containsKey(serializableKey);
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
    public synchronized void clearStatistics() throws IllegalStateException {
        checkStatus();
        hitCount = 0;
        memoryStoreHitCount = 0;
        diskStoreHitCount = 0;
        missCountExpired = 0;
        missCountNotFound = 0;
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
        synchronized (this) {
            for (int i = 0; i < keys.length; i++) {
                Object key = keys[i];
                searchInMemoryStore(key, false);
            }
        }
        //This is called regularly by the expiry thread, but call it here synchronously
        if (configuration.isOverflowToDisk()) {
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
        List keys = getKeys();
        for (int i = 0; i < keys.size(); i++) {
            Element element = (Element) keys.get(i);
            if (element != null && element.getObjectValue().equals(value)) {
                return true;
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
                missCountExpired + missCountNotFound, size);
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
     *
     * @param object the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise. Same for a Cache means, the same GUID
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


}
