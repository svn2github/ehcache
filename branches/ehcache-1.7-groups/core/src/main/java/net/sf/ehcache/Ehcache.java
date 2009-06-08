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
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.loader.CacheLoader;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An interface for Ehcache.
 * <p/>
 * Ehcache is the central interface. Caches have {@link Element}s and are managed
 * by the {@link CacheManager}. The Cache performs logical actions. It delegates physical
 * implementations to its {@link net.sf.ehcache.store.Store}s.
 * <p/>
 * A reference to an EhCache can be obtained through the {@link CacheManager}. An Ehcache thus obtained
 * is guaranteed to have status {@link Status#STATUS_ALIVE}. This status is checked for any method which
 * throws {@link IllegalStateException} and the same thrown if it is not alive. This would normally
 * happen if a call is made after {@link CacheManager#shutdown} is invoked.
 * <p/>
 * Statistics on cache usage are collected and made available through public methods.
 *
 * @author Greg Luck
 * @version $Id$
 */
public interface Ehcache extends Cloneable {
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
     * @throws IllegalStateException    if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     * @throws CacheException
     */
    void put(Element element) throws IllegalArgumentException, IllegalStateException,
            CacheException;

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
     * @throws IllegalStateException    if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException,
            CacheException;

    /**
     * Put an element in the cache, without updating statistics, or updating listeners. This is meant to be used
     * in conjunction with {@link #getQuiet}
     *
     * @param element An object. If Serializable it can fully participate in replication and the DiskStore.
     * @throws IllegalStateException    if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException,
            CacheException;

    /**
     * Gets an element from the cache. Updates Element Statistics
     * <p/>
     * Note that the Element's lastAccessTime is always the time of this get.
     * Use {@link #getQuiet(Object)} to peak into the Element to see its last access time with get
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @see #isExpired
     */
    Element get(Serializable key) throws IllegalStateException, CacheException;

    /**
     * Gets an element from the cache. Updates Element Statistics
     * <p/>
     * Note that the Element's lastAccessTime is always the time of this get.
     * Use {@link #getQuiet(Object)} to peak into the Element to see its last access time with get
     *
     * @param key an Object value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @see #isExpired
     * @since 1.2
     */
    Element get(Object key) throws IllegalStateException, CacheException;

    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * still updated.
     * <p/>
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @see #isExpired
     */
    Element getQuiet(Serializable key) throws IllegalStateException, CacheException;

    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * also not updated.
     * <p/>
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @see #isExpired
     * @since 1.2
     */
    Element getQuiet(Object key) throws IllegalStateException, CacheException;

    /**
     * Returns a list of all elements in the cache, whether or not they are expired.
     * <p/>
     * The returned keys are unique and can be considered a set.
     * <p/>
     * The List returned is not live. It is a copy.
     * <p/>
     * The time taken is O(n). On a single cpu 1.8Ghz P4, approximately 8ms is required
     * for each 1000 entries.
     *
     * @return a list of {@link Object} keys
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    List getKeys() throws IllegalStateException, CacheException;

    /**
     * Returns a list of all elements in the cache. Only keys of non-expired
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
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    List getKeysWithExpiryCheck() throws IllegalStateException, CacheException;

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
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    List getKeysNoDuplicateCheck() throws IllegalStateException;

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed.
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    boolean remove(Serializable key) throws IllegalStateException;

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element
     * with the key actually existed.
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @since 1.2
     */
    boolean remove(Object key) throws IllegalStateException;

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element
     * with the key actually existed.
     *
     * @param key
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException;

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache. This also removes it from any
     * stores it may be in.
     * <p/>
     * Also notifies the CacheEventListener after the element was removed, but only if an Element
     * with the key actually existed.
     *
     * @param key
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException;

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache, without notifying listeners. This also removes it from any
     * stores it may be in.
     * <p/>
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    boolean removeQuiet(Serializable key) throws IllegalStateException;

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache, without notifying listeners. This also removes it from any
     * stores it may be in.
     * <p/>
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @since 1.2
     */
    boolean removeQuiet(Object key) throws IllegalStateException;

    /**
     * Removes all cached items.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    void removeAll() throws IllegalStateException, CacheException;

    /**
     * Removes all cached items.
     *
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer,
     *                                    in which case this put should not initiate a further notification to doNotNotifyCacheReplicators cache peers
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException;

    /**
     * Flushes all cache items from memory to the disk store, and from the DiskStore to disk.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    void flush() throws IllegalStateException, CacheException;

    /**
     * Gets the size of the cache. This is a subtle concept. See below.
     * <p/>
     * The size is the number of {@link net.sf.ehcache.Element}s in the {@link net.sf.ehcache.store.MemoryStore} plus
     * the number of {@link net.sf.ehcache.Element}s in the {@link net.sf.ehcache.store.DiskStore}.
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
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    int getSize() throws IllegalStateException, CacheException;

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
    long calculateInMemorySize() throws IllegalStateException, CacheException;

    /**
     * Returns the number of elements in the memory store.
     *
     * @return the number of elements in the memory store
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    long getMemoryStoreSize() throws IllegalStateException;

    /**
     * Returns the number of elements in the disk store.
     *
     * @return the number of elements in the disk store.
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    int getDiskStoreSize() throws IllegalStateException;

    /**
     * Gets the status attribute of the Cache.
     *
     * @return The status value from the Status enum class
     */
    Status getStatus();

    /**
     * Gets the cache name.
     */
    String getName();

    /**
     * Sets the cache name which will name.
     *
     * @param name the name of the cache. Should not be null.
     */
    void setName(String name);

    /**
     * Returns a {@link String} representation of {@link net.sf.ehcache.Cache}.
     */
    String toString();

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
     * @param element the element to check
     * @return true if it has expired
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws NullPointerException  if the element is null
     */
    boolean isExpired(Element element) throws IllegalStateException, NullPointerException;

    /**
     * Clones a cache. This is only legal if the cache has not been
     * initialized. At that point only primitives have been set and no
     * {@link net.sf.ehcache.store.MemoryStore} or {@link net.sf.ehcache.store.DiskStore} has been created.
     * <p/>
     * A new, empty, RegisteredEventListeners is created on clone.
     * <p/>
     *
     * @return an object of type {@link net.sf.ehcache.Cache}
     * @throws CloneNotSupportedException
     */
    Object clone() throws CloneNotSupportedException;


    /**
     * Use this to access the service in order to register and unregister listeners
     *
     * @return the RegisteredEventListeners instance for this cache.
     */
    RegisteredEventListeners getCacheEventNotificationService();

    /**
     * Whether an Element is stored in the cache in Memory, indicating a very low cost of retrieval.
     *
     * @return true if an element matching the key is found in memory
     */
    boolean isElementInMemory(Serializable key);

    /**
     * Whether an Element is stored in the cache in Memory, indicating a very low cost of retrieval.
     *
     * @return true if an element matching the key is found in memory
     * @since 1.2
     */
    boolean isElementInMemory(Object key);

    /**
     * Whether an Element is stored in the cache on Disk, indicating a higher cost of retrieval.
     *
     * @return true if an element matching the key is found in the diskStore
     */
    boolean isElementOnDisk(Serializable key);

    /**
     * Whether an Element is stored in the cache on Disk, indicating a higher cost of retrieval.
     *
     * @return true if an element matching the key is found in the diskStore
     * @since 1.2
     */
    boolean isElementOnDisk(Object key);

    /**
     * The GUID for this cache instance can be used to determine whether two cache instance references
     * are pointing to the same cache.
     *
     * @return the globally unique identifier for this cache instance. This is guaranteed to be unique.
     * @since 1.2
     */
    String getGuid();

    /**
     * Gets the CacheManager managing this cache. For a newly created cache this will be null until
     * it has been added to a CacheManager.
     *
     * @return the manager or null if there is none
     */
    CacheManager getCacheManager();

    /**
     * Resets statistics counters back to 0.
     */
    void clearStatistics();

    /**
     * Accurately measuring statistics can be expensive. Returns the current accuracy setting.
     *
     * @return one of {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT}, {@link Statistics#STATISTICS_ACCURACY_GUARANTEED}, {@link Statistics#STATISTICS_ACCURACY_NONE}
     */
    public int getStatisticsAccuracy();


    /**
     * Sets the statistics accuracy.
     *
     * @param statisticsAccuracy one of {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT}, {@link Statistics#STATISTICS_ACCURACY_GUARANTEED}, {@link Statistics#STATISTICS_ACCURACY_NONE}
     */
    public void setStatisticsAccuracy(int statisticsAccuracy);


    /**
     * Causes all elements stored in the Cache to be synchronously checked for expiry, and if expired, evicted.
     */
    void evictExpiredElements();

    /**
     * An inexpensive check to see if the key exists in the cache.
     *
     * @param key the key to check for
     * @return true if an Element matching the key is found in the cache. No assertions are made about the state of the Element.
     */
    boolean isKeyInCache(Object key);

    /**
     * An extremely expensive check to see if the value exists in the cache.
     *
     * @param value to check for
     * @return true if an Element matching the key is found in the cache. No assertions are made about the state of the Element.
     */
    boolean isValueInCache(Object value);

    /**
     * Gets an immutable Statistics object representing the Cache statistics at the time. How the statistics are calculated
     * depends on the statistics accuracy setting. The only aspect of statistics sensitive to the accuracy setting is
     * object size. How that is calculated is discussed below.
     * <h3>Best Effort Size</h3>
     * This result is returned when the statistics accuracy setting is {@link Statistics#STATISTICS_ACCURACY_BEST_EFFORT}.
     * <p/>
     * The size is the number of {@link Element}s in the {@link net.sf.ehcache.store.MemoryStore} plus
     * the number of {@link Element}s in the {@link net.sf.ehcache.store.DiskStore}.
     * <p/>
     * This number is the actual number of elements, including expired elements that have
     * not been removed. Any duplicates between stores are accounted for.
     * <p/>
     * Expired elements are removed from the the memory store when
     * getting an expired element, or when attempting to spool an expired element to
     * disk.
     * <p/>
     * Expired elements are removed from the disk store when getting an expired element,
     * or when the expiry thread runs, which is once every five minutes.
     * <p/>
     * <h3>Guaranteed Accuracy Size</h3>
     * This result is returned when the statistics accuracy setting is {@link Statistics#STATISTICS_ACCURACY_GUARANTEED}.
     * <p/>
     * This method accounts for elements which might be expired or duplicated between stores. It take approximately
     * 200ms per 1000 elements to execute.
     * <h3>Fast but non-accurate Size</h3>
     * This result is returned when the statistics accuracy setting is {@link Statistics#STATISTICS_ACCURACY_NONE}.
     * <p/>
     * The number given may contain expired elements. In addition if the DiskStore is used it may contain some double
     * counting of elements. It takes 6ms for 1000 elements to execute. Time to execute is O(log n). 50,000 elements take
     * 36ms.
     *
     * @return the number of elements in the ehcache, with a varying degree of accuracy, depending on accuracy setting.
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    Statistics getStatistics() throws IllegalStateException;


    /**
     * Sets the CacheManager
     *
     * @param cacheManager the CacheManager for this cache to use.
     */
    void setCacheManager(CacheManager cacheManager);


    /**
     * Accessor for the BootstrapCacheLoader associated with this cache. For testing purposes.
     *
     * @return the BootstrapCacheLoader to use
     */
    BootstrapCacheLoader getBootstrapCacheLoader();

    /**
     * Sets the bootstrap cache loader.
     *
     * @param bootstrapCacheLoader the loader to be used
     * @throws CacheException if this method is called after the cache is initialized
     */
    void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) throws CacheException;


    /**
     * DiskStore paths can conflict between CacheManager instances. This method allows the path to be changed.
     *
     * @param diskStorePath the new path to be used.
     * @throws CacheException if this method is called after the cache is initialized
     */
    void setDiskStorePath(String diskStorePath) throws CacheException;

    /**
     * Newly created caches do not have a {@link net.sf.ehcache.store.MemoryStore} or a {@link net.sf.ehcache.store.DiskStore}.
     * <p/>
     * This method creates those and makes the cache ready to accept elements
     */
    void initialise();

    /**
     * Bootstrap command. This must be called after the Cache is intialised, during
     * CacheManager initialisation. If loads are synchronous, they will complete before the CacheManager
     * initialise completes, otherwise they will happen in the background.
     */
    void bootstrap();

    /**
     * Flushes all cache items from memory to auxilliary caches and close the auxilliary caches.
     * <p/>
     * Should be invoked only by CacheManager.
     *
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     */
    public void dispose() throws IllegalStateException;

    /**
     * Gets the cache configuration this cache was created with.
     * <p/>
     * Things like listeners that are added dynamically are excluded.
     */
    CacheConfiguration getCacheConfiguration();

    /**
     * Register a {@link CacheExtension} with the cache. It will then be tied into the cache lifecycle.
     * <p/>
     * If the CacheExtension is not initialised, initialise it.
     */
    public void registerCacheExtension(CacheExtension cacheExtension);

    /**
     * Unregister a {@link CacheExtension} with the cache. It will then be detached from the cache lifecycle.
     */
    public void unregisterCacheExtension(CacheExtension cacheExtension);


    /**
     *
     * @return the cache extensions as a live list
     */
    public List<CacheExtension> getRegisteredCacheExtensions();

    /**
     * The average get time in ms.
     */
    public float getAverageGetTime();

    /**
     * Sets an ExceptionHandler on the Cache. If one is already set, it is overwritten.
     */
    public void setCacheExceptionHandler(CacheExceptionHandler cacheExceptionHandler);

    /**
     * Sets an ExceptionHandler on the Cache. If one is already set, it is overwritten.
     */
    public CacheExceptionHandler getCacheExceptionHandler();

    /**
     * Register a {@link CacheLoader} with the cache. It will then be tied into the cache lifecycle.
     * <p/>
     * If the CacheLoader is not initialised, initialise it.
     *
     * @param cacheLoader A Cache Loader to register
     */
    public void registerCacheLoader(CacheLoader cacheLoader);

    /**
     * Unregister a {@link CacheLoader} with the cache. It will then be detached from the cache lifecycle.
     *
     * @param cacheLoader A Cache Loader to unregister
     */
    public void unregisterCacheLoader(CacheLoader cacheLoader);


    /**
     *
     * @return the cache loaders as a live list
     */
    public List<CacheLoader> getRegisteredCacheLoaders();

    /**
     * This method will return, from the cache, the object associated with
     * the argument "key".
     * <p/>
     * If the object is not in the cache, the associated
     * cache loader will be called. That is either the CacheLoader passed in, or if null, the one associated with the cache.
     * If both are null, no load is performed and null is returned.
     * <p/>
     * Because this method may take a long time to complete, it is not synchronized. The underlying cache operations
     * are synchronized.
     *
     * @param key key whose associated value is to be returned.
     * @param loader the override loader to use. If null, the cache's default loader will be used
     * @param loaderArgument an argument to pass to the CacheLoader.
     * @return an element if it existed or could be loaded, otherwise null
     * @throws CacheException
     */
    public Element getWithLoader(Object key, CacheLoader loader, Object loaderArgument) throws CacheException;

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
     * @param keys a collection of keys to be returned/loaded
     * @param loaderArgument an argument to pass to the CacheLoader.
     * @return a Map populated from the Cache. If there are no elements, an empty Map is returned.
     * @throws CacheException
     */
    public Map getAllWithLoader(Collection keys, Object loaderArgument) throws CacheException;


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
    public void load(final Object key) throws CacheException;


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
    public void loadAll(final Collection keys, final Object argument) throws CacheException;



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
     * @return true if the cache is disabled.
     */
    public boolean isDisabled();

    /**
     * Disables or enables this cache. This call overrides the previous value of disabled.
     * <p/>
     * @param disabled true if you wish to disable, false to enable
     * @see #isDisabled()
     */
    public void setDisabled(boolean disabled);

	public String getMasterGroupKey();

	public void setMasterGroupKey(String masterGroupKey);
	
    /**
     * Returns a list of all element keys in the cache, whether or not they are expired,
     * whose elements are members are members of the specificed group
     * <p/>
     * The Set returned is not live. It is a copy, and may be out of date since 
     * an Element expiry does not necessarily cause a cleanup of the group membership 
     * <p/>
     * The group element's statistics are update by this access.
     * 
     * @param groupKey the group identifier
     * @return a set of {@link Object} keys, or null if the group is not present in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @throws CacheException if the key does not identify a Group but a regular Element
     */
    public Set getKeysForGroup(Object groupKey) throws IllegalStateException, CacheException;

    /**
     * Removes all elements which participate in the given group.
     * <p/>
     * The Set returned is the set of Elements that were removed, this may be a 
     * subset of the set returned by {@link #getKeysForGroups(Object)}; since this
     * method only includes in the set the keys that were actually removed.
     * <p/>
     * The group element's statistics are update by this access.
     * 
     * @param groupKey the group identifier
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @return a set of {@link Object} keys removed, or null if the group is not present in the cache
     * @throws IllegalStateException if the cache is not {@link Status#STATUS_ALIVE}
     * @throws CacheException if the key does not identify a Group but a regular Element
     */
    public Set removeByGroup(Object groupKey, boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException;
    
}
