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

package net.sf.ehcache;

import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.DiskStore;
import net.sf.ehcache.store.MemoryStore;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
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


    private String name;

    private DiskStore diskStore;

    private String diskStorePath;

    private Status status;

    private final int maxElementsInMemory;

    private MemoryStoreEvictionPolicy memoryStoreEvictionPolicy;

    private int statisticsAccuracy = Statistics.STATISTICS_ACCURACY_BEST_EFFORT;

    /**
     * Whether cache elements in this cache overflowToDisk.
     */
    private final boolean overflowToDisk;

    /**
     * The interval in seconds between runs of the disk expiry thread. 2 minutes is the default.
     * This is not the same thing as time to live or time to idle. When the thread runs it checks
     * these things. So this value is how often we check for expiry.
     */
    private final long diskExpiryThreadIntervalSeconds;

    /**
     * For caches that overflow to disk, does the disk cache persist between CacheManager instances.
     */
    private final boolean diskPersistent;

    /**
     * The shutdown hook thread for {@link #diskPersistent} caches. This thread
     * must be unregistered as a shutdown hook, when the cache is disposed.
     * Otherwise the cache is not GC-able.
     */
    private Thread shutdownHook;

    /**
     * Whether elements are eternal, which is the same as non-expiring.
     */
    private final boolean eternal;

    /**
     * The maximum time between creation time and when an element expires.
     * Is only used if the element is not eternal.
     */
    private final long timeToLiveSeconds;

    /**
     * The maximum amount of time between {@link #get(Object)}s before an element expires.
     */
    private final long timeToIdleSeconds;


    /**
     * Cache hit count.
     */
    private int hitCount;

    /**
     * Memory cache hit count.
     */
    private int memoryStoreHitCount;

    /**
     * Auxiliary hit counts broken down by auxiliary.
     */
    private int diskStoreHitCount;

    /**
     * Count of misses where element was not found.
     */
    private int missCountNotFound;

    /**
     * Count of misses where element was expired.
     */
    private int missCountExpired;

    /**
     * The {@link MemoryStore} of this {@link Cache}. All caches have a memory store.
     */
    private MemoryStore memoryStore;

    private RegisteredEventListeners registeredEventListeners;

    private final String guid = new StringBuffer() .append(localhost) .append("-").append(new UID()).toString();

    private CacheManager cacheManager;

    private BootstrapCacheLoader bootstrapCacheLoader;

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
     * @param name                Cache name
     * @param maxElementsInMemory Max elements in memory
     * @param overflowToDisk      Overflow to disk (boolean)
     * @param eternal             Whether the elements expire
     * @param timeToLiveSeconds
     * @param timeToIdleSeconds
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
     * @param name
     * @param maxElementsInMemory
     * @param overflowToDisk
     * @param eternal
     * @param timeToLiveSeconds
     * @param timeToIdleSeconds
     * @param diskPersistent
     * @param diskExpiryThreadIntervalSeconds
     *
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
        LOG.warn("An API change between ehcache-1.1 results in the persistence path being set to java.io.tmp when the " +
                "ehcache-1.1 constructor is used. Please change to the 1.2 constructor");
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
     * @param name
     * @param maxElementsInMemory
     * @param memoryStoreEvictionPolicy one of LRU, LFU and FIFO. Optionally null, in which case it will be set to LRU.
     * @param overflowToDisk
     * @param diskStorePath             the directory to be used a disk store path. Uses java.io.tmpdir if the argument is null.
     * @param eternal
     * @param timeToLiveSeconds
     * @param timeToIdleSeconds
     * @param diskPersistent
     * @param diskExpiryThreadIntervalSeconds
     *
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
     * 1.2 Constructor
     * <p/>
     * The {@link net.sf.ehcache.config.ConfigurationFactory} and clients can create these.
     * <p/>
     * A client can specify their own settings here and pass the {@link Cache} object
     * into {@link CacheManager#addCache} to specify parameters other than the defaults.
     * <p/>
     * Only the CacheManager can initialise them.
     *
     * @param name
     * @param maxElementsInMemory
     * @param memoryStoreEvictionPolicy one of LRU, LFU and FIFO. Optionally null, in which case it will be set to LRU.
     * @param overflowToDisk
     * @param diskStorePath             the directory to be used a disk store path. Uses java.io.tmpdir if the argument is null.
     * @param eternal
     * @param timeToLiveSeconds
     * @param timeToIdleSeconds
     * @param diskPersistent
     * @param diskExpiryThreadIntervalSeconds
     *
     * @param bootstrapCacheLoader
     * @param registeredEventListeners  a notification service. Optionally null, in which case a new
     *                                  one with no registered listeners will be created.
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
        this.name = name;
        this.maxElementsInMemory = maxElementsInMemory;
        this.memoryStoreEvictionPolicy = memoryStoreEvictionPolicy;
        this.overflowToDisk = overflowToDisk;
        this.eternal = eternal;
        this.timeToLiveSeconds = timeToLiveSeconds;
        this.timeToIdleSeconds = timeToIdleSeconds;
        this.diskPersistent = diskPersistent;

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
            this.diskExpiryThreadIntervalSeconds = DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS;
        } else {
            this.diskExpiryThreadIntervalSeconds = diskExpiryThreadIntervalSeconds;
        }

        // For backward compatibility with 1.1 and earlier
        if (memoryStoreEvictionPolicy == null) {
            this.memoryStoreEvictionPolicy = DEFAULT_MEMORY_STORE_EVICTION_POLICY;
        }

        this.bootstrapCacheLoader = bootstrapCacheLoader;

        changeStatus(Status.STATUS_UNINITIALISED);
    }


    /**
     * Newly created caches do not have a {@link net.sf.ehcache.store.MemoryStore} or a {@link net.sf.ehcache.store.DiskStore}.
     * <p/>
     * This method creates those and makes the cache ready to accept elements
     */
    public void initialise() {
        synchronized (this) {
            if (!status.equals(Status.STATUS_UNINITIALISED)) {
                throw new IllegalStateException("Cannot initialise the " + name
                        + " cache because its status is not STATUS_UNINITIALISED");
            }

            if (maxElementsInMemory == 0) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Cache: " + name + " has a maxElementsInMemory of 0. It is strongly recommended to " +
                            "have a maximumSize of at least 1. Performance is halved by not using a MemoryStore.");
                }
            }

            if (overflowToDisk) {
                diskStore = new DiskStore(this, diskStorePath);
            }

            memoryStore = MemoryStore.create(this, diskStore);


            if (diskPersistent) {
                addShutdownHook();
            }

            changeStatus(Status.STATUS_ALIVE);


        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Initialised cache: " + name);
        }

        if (disabled) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Cache: " + name + " is disabled because the " + NET_SF_EHCACHE_DISABLED
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
     * Some caches might be persistent, so we want to add a shutdown hook if that is the
     * case, so that the data and index can be written to disk.
     */
    private void addShutdownHook() {
        Thread localShutdownHook = new Thread() {
            public void run() {
                synchronized (this) {
                    if (status.equals(Status.STATUS_ALIVE)) {
                        // clear shutdown hook reference to prevent
                        // removeShutdownHook to remove it during shutdown
                        Cache.this.shutdownHook = null;

                        if (LOG.isDebugEnabled()) {
                        LOG.debug("VM shutting down with the disk store for " + name
                                + " still active. The disk store is persistent. Calling dispose...");
                        }
                        dispose();
                    }
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(localShutdownHook);
        shutdownHook = localShutdownHook;
    }


    /**
     * Remove the shutdown hook to prevent leaving orphaned caches around. This
     * is called by {@link #dispose()} AFTER the status has been set to shutdown.
     */
    private void removeShutdownHook() {
        if (shutdownHook != null) {
            // remove shutdown hook
            Runtime.getRuntime().removeShutdownHook(shutdownHook);

            // run the shutdown thread to remove it from its thread group
            shutdownHook.start();

            shutdownHook = null;
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
     *
     * @param element An object. If Serializable it can fully participate in replication and the DiskStore.
     * @throws IllegalStateException    if the cache is not {@link Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public final synchronized void put(Element element) throws IllegalArgumentException, IllegalStateException,
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
     *
     * @param element                     An object. If Serializable it can fully participate in replication and the DiskStore.
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @throws IllegalStateException    if the cache is not {@link Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public final synchronized void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
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

        boolean elementExists = false;
        if (registeredEventListeners != null) {
            Object key = element.getObjectKey();
            elementExists = isElementInMemory(key) || isElementOnDisk(key);
        }

        if (elementExists) {
            element.updateUpdateStatistics();
        }

        applyDefaultsToElementWithoutLifespanSet(element);

        memoryStore.put(element);

        if (elementExists) {
            registeredEventListeners.notifyElementUpdated(element, doNotNotifyCacheReplicators);
        } else {
            registeredEventListeners.notifyElementPut(element, doNotNotifyCacheReplicators);
        }

    }

    private void applyDefaultsToElementWithoutLifespanSet(Element element) {
        if (!element.isLifespanSet()) {
            //Setting with Cache defaults
            element.setTimeToLive((int) timeToLiveSeconds);
            element.setTimeToIdle((int) timeToIdleSeconds);
            element.setEternal(eternal);
        }
    }


    /**
     * Put an element in the cache, without updating statistics, or updating listeners. This is meant to be used
     * in conjunction with {@link #getQuiet}
     *
     * @param element An object. If Serializable it can fully participate in replication and the DiskStore.
     * @throws IllegalStateException    if the cache is not {@link Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public final synchronized void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException,
            CacheException {
        checkStatus();

        if (disabled) {
            return;
        }

        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }

        applyDefaultsToElementWithoutLifespanSet(element);

        memoryStore.put(element);
    }

    /**
     * Gets an element from the cache. Updates Element Statistics
     * <p/>
     * Note that the Element's lastAccessTime is always the time of this get.
     * Use {@link #getQuiet(Object)} to peak into the Element to see its last access time with get
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see #isExpired
     */
    public final synchronized Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }


    /**
     * Gets an element from the cache. Updates Element Statistics
     * <p/>
     * Note that the Element's lastAccessTime is always the time of this get.
     * Use {@link #getQuiet(Object)} to peak into the Element to see its last access time with get
     *
     * @param key an Object value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see #isExpired
     * @since 1.2
     */
    public final synchronized Element get(Object key) throws IllegalStateException, CacheException {
        checkStatus();
        Element element;

        element = searchInMemoryStore(key, true);
        if (element == null && overflowToDisk) {
            element = searchInDiskStore(key, true);
        }

        if (element == null) {
            missCountNotFound++;
            if (LOG.isTraceEnabled()) {
                LOG.trace(name + " cache - Miss");
            }
            return null;
        } else {
            hitCount++;
            return element;
        }
    }

    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * still updated.
     * <p/>
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see #isExpired
     */
    public final synchronized Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * still updated.
     * <p/>
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @see #isExpired
     * @since 1.2
     */
    public final synchronized Element getQuiet(Object key) throws IllegalStateException, CacheException {
        checkStatus();
        Element element;

        element = searchInMemoryStore(key, false);
        if (element == null && overflowToDisk) {
            element = searchInDiskStore(key, false);
        }

        if (element == null) {
            missCountNotFound++;
            if (LOG.isTraceEnabled()) {
                LOG.trace(name + " cache - Miss");
            }
            return null;
        } else {
            hitCount++;
            return element;
        }
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
        if (overflowToDisk) {
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
        if (overflowToDisk) {
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
                    LOG.debug(name + " Memory cache hit, but element expired");
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
                    LOG.debug(name + " cache - Disk Store hit, but element expired");
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
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final synchronized boolean remove(Serializable key) throws IllegalStateException {
        return remove((Object) key);
    }

    /**
     * Removes an {@link Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element
     * with the key actually existed.
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @since 1.2
     */
    public final synchronized boolean remove(Object key) throws IllegalStateException {
        return remove(key, false);
    }


    /**
     * Removes an {@link Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element
     * with the key actually existed.
     *
     * @param key
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @noinspection SameParameterValue
     */
    public final synchronized boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove((Object) key, doNotNotifyCacheReplicators);
    }

    /**
     * Removes an {@link Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element
     * with the key actually existed.
     *
     * @param key
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final synchronized boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove(key, false, true, doNotNotifyCacheReplicators);
    }

    /**
     * Removes an {@link Element} from the Cache, without notifying listeners. This also removes it from any
     * stores it may be in.
     * <p/>
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final synchronized boolean removeQuiet(Serializable key) throws IllegalStateException {
        return remove(key, false, false, false);
    }

    /**
     * Removes an {@link Element} from the Cache, without notifying listeners. This also removes it from any
     * stores it may be in.
     * <p/>
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @since 1.2
     */
    public final synchronized boolean removeQuiet(Object key) throws IllegalStateException {
        return remove(key, false, false, false);
    }


    /**
     * Expires an {@link Element} from the Cache after an attempt to get it determined that it should be expired.
     * This also removes it from any stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element has expired, but only if an Element
     * with the key actually existed.
     *
     * @param key
     * @param expiry                      if the reason this method is being called is to expire the element
     * @param notifyListeners             whether to notify listeners
     * @param doNotNotifyCacheReplicators whether not to notify cache replicators
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    private synchronized boolean remove(Object key, boolean expiry, boolean notifyListeners,
                                        boolean doNotNotifyCacheReplicators)
            throws IllegalStateException, CacheException {
        checkStatus();
        boolean removed = false;
        Element elementFromMemoryStore;
        elementFromMemoryStore = memoryStore.remove(key);
        if (elementFromMemoryStore != null) {
            if (notifyListeners) {
                if (expiry) {
                    registeredEventListeners.notifyElementExpiry(elementFromMemoryStore, doNotNotifyCacheReplicators);
                } else {
                    registeredEventListeners.notifyElementRemoved(elementFromMemoryStore, doNotNotifyCacheReplicators);
                }
            }
            removed = true;
        }

        //could have been removed from both places, if there are two copies in the cache
        Element elementFromDiskStore = null;
        if (overflowToDisk) {
            if (!(key instanceof Serializable)) {
                return false;
            }
            Serializable serializableKey = (Serializable) key;
            elementFromDiskStore = diskStore.remove(serializableKey);
        }

        if (elementFromDiskStore != null) {
            if (expiry) {
                registeredEventListeners.notifyElementExpiry(elementFromDiskStore, doNotNotifyCacheReplicators);
            } else {
                registeredEventListeners.notifyElementRemoved(elementFromDiskStore, doNotNotifyCacheReplicators);
            }
            removed = true;
        }

        return removed;
    }


    /**
     * Removes all cached items.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public final synchronized void removeAll() throws IllegalStateException, CacheException {
        checkStatus();
        memoryStore.removeAll();
        if (overflowToDisk) {
            diskStore.removeAll();
        }
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
        if (overflowToDisk) {
            diskStore.dispose();
            diskStore = null;
        }

        registeredEventListeners.dispose();

        changeStatus(Status.STATUS_SHUTDOWN);

        if (diskPersistent) {
            removeShutdownHook();
        }
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
            if (overflowToDisk) {
                diskStore.flush();
            }
        } catch (IOException e) {
            throw new CacheException("Unable to flush cache: " + name + ". Initial cause was " + e.getMessage(), e);
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
     * Gets the size of the memory store for this cache
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
        if (overflowToDisk) {
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
            throw new IllegalStateException("The " + name + " Cache is not alive.");
        }
    }


    /**
     * The number of times a requested item was found in the cache.
     *
     * @return the number of times a requested item was found in the cache
     */
    public final int getHitCount() {
        return hitCount;
    }

    /**
     * Number of times a requested item was found in the Memory Store.
     *
     * @return Number of times a requested item was found in the Memory Store.
     */
    public final int getMemoryStoreHitCount() {
        return memoryStoreHitCount;
    }

    /**
     * Number of times a requested item was found in the Disk Store.
     */
    public final int getDiskStoreHitCount() {
        return diskStoreHitCount;
    }

    /**
     * Number of times a requested element was not found in the cache. This
     * may be because it expired, in which case this will also be recorded in {@link #getMissCountExpired},
     * or because it was simply not there.
     */
    public final int getMissCountNotFound() {
        return missCountNotFound;
    }

    /**
     * Number of times a requested element was found but was expired.
     */
    public final int getMissCountExpired() {
        return missCountExpired;
    }

    /**
     * Gets the cache name.
     */
    public final String getName() {
        return name;
    }

    /**
     * Sets the cache name which will name.
     *
     * @param name the name of the cache. Should not be null.
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * Gets timeToIdleSeconds.
     */
    public final long getTimeToIdleSeconds() {
        return timeToIdleSeconds;
    }

    /**
     * Gets timeToLiveSeconds.
     */
    public final long getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    /**
     * Are elements eternal.
     */
    public final boolean isEternal() {
        return eternal;
    }

    /**
     * Does the overflow go to disk.
     */
    public final boolean isOverflowToDisk() {
        return overflowToDisk;
    }

    /**
     * Gets the maximum number of elements to hold in memory.
     */
    public final int getMaxElementsInMemory() {
        return maxElementsInMemory;
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
     */
    public final MemoryStoreEvictionPolicy getMemoryStoreEvictionPolicy() {
        return memoryStoreEvictionPolicy;
    }

    /**
     * Returns a {@link String} representation of {@link Cache}.
     */
    public final String toString() {
        StringBuffer dump = new StringBuffer();

        dump.append("[ ")
                .append(" name = ").append(name)
                .append(" status = ").append(status)
                .append(" eternal = ").append(eternal)
                .append(" overflowToDisk = ").append(overflowToDisk)
                .append(" maxElementsInMemory = ").append(maxElementsInMemory)
                .append(" memoryStoreEvictionPolicy = ").append(memoryStoreEvictionPolicy)
                .append(" timeToLiveSeconds = ").append(timeToLiveSeconds)
                .append(" timeToIdleSeconds = ").append(timeToIdleSeconds)
                .append(" diskPersistent = ").append(diskPersistent)
                .append(" diskExpiryThreadIntervalSeconds = ").append(diskExpiryThreadIntervalSeconds)
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
        if (registeredEventListenersFromCopy == null || registeredEventListenersFromCopy.getCacheEventListeners().size() == 0)
        {
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
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    final DiskStore getDiskStore() throws IllegalStateException {
        checkStatus();
        return diskStore;
    }

    /**
     * Gets the internal MemoryStore.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    final MemoryStore getMemoryStore() throws IllegalStateException {
        checkStatus();
        return memoryStore;
    }

    /**
     * @return true if the cache overflows to disk and the disk is persistent between restarts
     */
    public final boolean isDiskPersistent() {
        return diskPersistent;
    }

    /**
     * @return the interval between runs
     *         of the expiry thread, where it checks the disk store for expired elements. It is not the
     *         the timeToLiveSeconds.
     */
    public final long getDiskExpiryThreadIntervalSeconds() {
        return diskExpiryThreadIntervalSeconds;
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
        return diskStore != null && diskStore.containsKey(serializableKey);
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
        diskStore.expireElements();
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
        if (isElementInMemory(key)) {
            return true;
        } else {
            return isElementOnDisk(key);
        }
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
     * Package local setter for use by CacheManager
     *
     * @param cacheManager
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
            throw new CacheException("A bootstrap cache loader can only be set before the cache is initialized. " + name);
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
            throw new CacheException("A DiskStore path can only be set before the cache is initialized. " + name);
        }
        this.diskStorePath = diskStorePath;
    }


    /**
     * An equals method which follows the contract of {@link Object#equals(Object)}
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
