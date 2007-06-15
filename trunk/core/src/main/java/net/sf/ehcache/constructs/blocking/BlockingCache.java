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

package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.Status;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.constructs.concurrent.ConcurrencyUtil;
import net.sf.ehcache.constructs.concurrent.Mutex;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.List;


/**
 * A blocking decorator for an Ehcache, backed by a {@link Ehcache}.
 * <p/>
 * It allows concurrent read access to elements already in the cache. If the element is null, other
 * reads will block until an element with the same key is put into the cache.
 * <p/>
 * This is useful for constructing read-through or self-populating caches.
 * <p/>
 * This implementation uses the {@link Mutex} class from Doug Lea's concurrency package. If you wish to use
 * this class, you will need the concurrent package in your class path.
 * <p/>
 * It features:
 * <ul>
 * <li>Excellent liveness.
 * <li>Fine-grained locking on each element, rather than the cache as a whole.
 * <li>Scalability to a large number of threads.
 * </ul>
 * <p/>
 * This class has been updated to use Lock striping. The Mutex implementation gives scalability but creates
 * many Mutex objects of 24 bytes each. Lock striping limits their number to 100.
 * <p/>
 * A version of this class is planned which will dynamically use JDK5's concurrency package, which is
 * based on Doug Lea's, so as to avoid a dependency on his package for JDK5 systems. This will not
 * be implemented until JDK5 is released on MacOSX and Linux, as JDK5 will be required to compile
 * it, though any version from JDK1.2 up will be able to run the code, falling back to Doug
 * Lea's concurrency package, if the JDK5 package is not found in the classpath.
 * <p/>
 * The <code>Mutex</code> class does not appear in the JDK5 concurrency package. Doug Lea has
 * generously offered the following advice:
 * <p/>
 * "You should just be able to use ReentrantLock here.  We supply
 * ReentrantLock, but not Mutex because the number of cases where a
 * non-reentrant mutex is preferable is small, and most people are more
 * familiar with reentrant seamantics. If you really need a non-reentrant
 * one, the javadocs for class AbstractQueuedSynchronizer include sample
 * code for them."
 * <p/>
 * -Doug
 * <p/>
 * "Hashtable / synchronizedMap uses the "one big fat lock" approach to guard the mutable state of the map.
 * That works, but is a big concurrency bottleneck, as you've observed.  You went to the opposite extreme, one lock per key.
 * That works (as long as you've got sufficient synchronization in the cache itself to protect its own data structures.)
 * <p/>
 * Lock striping is a middle ground, partitioning keys into a fixed number of subsets, like the trick used at large
 * theaters for will-call ticket pickup -- there are separate lines for "A-F, G-M, N-R, and S-Z".
 * This way, there are a fixed number of locks, each guarding (hopefully) 1/Nth of the keys."
 * - Brian Goetz
 * <p/>
 * Further improvements to hashing suggested by Joe Bowbeer.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class BlockingCache implements Ehcache {

    /**
     * The default number of locks to use. Must be a power of 2
     */
    public static final int LOCK_NUMBER = 2048;

    private static final Log LOG = LogFactory.getLog(BlockingCache.class.getName());

    /**
     * Based on the lock striping concept from Brian Goetz. See Java Concurrency in Practice 11.4.3
     */
    protected final Mutex[] locks = new Mutex[LOCK_NUMBER];

    {
        for (int i = 0; i < LOCK_NUMBER; i++) {
            locks[i] = new Mutex();
        }
    }


    /**
     * The backing Cache
     */
    protected final Ehcache cache;

    /**
     * The amount of time to block a thread before a LockTimeoutException is thrown
     */
    protected int timeoutMillis;

    /**
     * Creates a BlockingCache which decorates the supplied cache.
     *
     * @param cache a backing ehcache.
     * @throws CacheException
     * @since 1.2
     */
    public BlockingCache(final Ehcache cache) throws CacheException {
        this.cache = cache;
    }

    /**
     * Retrieve the EHCache backing cache
     */
    protected Ehcache getCache() {
        return cache;
    }

    /**
     * Returns this cache's name
     */
    public String getName() {
        return cache.getName();
    }

    /**
     * Sets the cache name which will name.
     *
     * @param name the name of the cache. Should not be null.
     */
    public void setName(String name) {
        cache.setName(name);
    }

    /**
     * Gets timeToIdleSeconds.
     */
    public long getTimeToIdleSeconds() {
        return cache.getTimeToIdleSeconds();
    }

    /**
     * Gets timeToLiveSeconds.
     */
    public long getTimeToLiveSeconds() {
        return cache.getTimeToLiveSeconds();
    }

    /**
     * Are elements eternal.
     */
    public boolean isEternal() {
        return cache.isEternal();
    }

    /**
     * Does the overflow go to disk.
     */
    public boolean isOverflowToDisk() {
        return cache.isOverflowToDisk();
    }

    /**
     * Gets the maximum number of elements to hold in memory.
     */
    public int getMaxElementsInMemory() {
        return cache.getMaxElementsInMemory();
    }

    /**
     * @return the maximum number of elements on Disk, or 0 if unlimited
     * @see net.sf.ehcache.Cache#getMaxElementsOnDisk
     */
    public int getMaxElementsOnDisk() {
        return cache.getMaxElementsOnDisk();
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
    public MemoryStoreEvictionPolicy getMemoryStoreEvictionPolicy() {
        return cache.getMemoryStoreEvictionPolicy();
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
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws NullPointerException  if the element is null
     */
    public boolean isExpired(Element element) throws IllegalStateException, NullPointerException {
        return cache.isExpired(element);
    }

    /**
     * Clones a cache. This is only legal if the cache has not been
     * initialized. At that point only primitives have been set and no
     * {@link net.sf.ehcache.store.LruMemoryStore} or {@link net.sf.ehcache.store.DiskStore} has been created.
     * <p/>
     * A new, empty, RegisteredEventListeners is created on clone.
     * <p/>
     *
     * @return an object of type {@link net.sf.ehcache.Cache}
     * @throws CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * @return true if the cache overflows to disk and the disk is persistent between restarts
     */
    public boolean isDiskPersistent() {
        return cache.isDiskPersistent();
    }

    /**
     * @return the interval between runs
     *         of the expiry thread, where it checks the disk store for expired elements. It is not the
     *         the timeToLiveSeconds.
     */
    public long getDiskExpiryThreadIntervalSeconds() {
        return cache.getDiskExpiryThreadIntervalSeconds();
    }

    /**
     * Use this to access the service in order to register and unregister listeners
     *
     * @return the RegisteredEventListeners instance for this cache.
     */
    public RegisteredEventListeners getCacheEventNotificationService() {
        return cache.getCacheEventNotificationService();
    }

    /**
     * Whether an Element is stored in the cache in Memory, indicating a very low cost of retrieval.
     *
     * @return true if an element matching the key is found in memory
     */
    public boolean isElementInMemory(Serializable key) {
        return cache.isElementInMemory(key);
    }

    /**
     * Whether an Element is stored in the cache in Memory, indicating a very low cost of retrieval.
     *
     * @return true if an element matching the key is found in memory
     * @since 1.2
     */
    public boolean isElementInMemory(Object key) {
        return cache.isElementInMemory(key);
    }

    /**
     * Whether an Element is stored in the cache on Disk, indicating a higher cost of retrieval.
     *
     * @return true if an element matching the key is found in the diskStore
     */
    public boolean isElementOnDisk(Serializable key) {
        return cache.isElementOnDisk(key);
    }

    /**
     * Whether an Element is stored in the cache on Disk, indicating a higher cost of retrieval.
     *
     * @return true if an element matching the key is found in the diskStore
     * @since 1.2
     */
    public boolean isElementOnDisk(Object key) {
        return cache.isElementOnDisk(key);
    }

    /**
     * The GUID for this cache instance can be used to determine whether two cache instance references
     * are pointing to the same cache.
     *
     * @return the globally unique identifier for this cache instance. This is guaranteed to be unique.
     * @since 1.2
     */
    public String getGuid() {
        return cache.getGuid();
    }

    /**
     * Gets the CacheManager managing this cache. For a newly created cache this will be null until
     * it has been added to a CacheManager.
     *
     * @return the manager or null if there is none
     */
    public CacheManager getCacheManager() {
        return cache.getCacheManager();
    }

    /**
     * Resets statistics counters back to 0.
     */
    public void clearStatistics() {
        cache.clearStatistics();
    }

    /**
     * Accurately measuring statistics can be expensive. Returns the current accuracy setting.
     *
     * @return one of {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_BEST_EFFORT}, {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_GUARANTEED}, {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_NONE}
     */
    public int getStatisticsAccuracy() {
        return cache.getStatisticsAccuracy();
    }

    /**
     * Sets the statistics accuracy.
     *
     * @param statisticsAccuracy one of {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_BEST_EFFORT}, {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_GUARANTEED}, {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_NONE}
     */
    public void setStatisticsAccuracy(int statisticsAccuracy) {
        cache.setStatisticsAccuracy(statisticsAccuracy);
    }

    /**
     * Causes all elements stored in the Cache to be synchronously checked for expiry, and if expired, evicted.
     */
    public void evictExpiredElements() {
        cache.evictExpiredElements();
    }

    /**
     * An inexpensive check to see if the key exists in the cache.
     *
     * @param key the key to check for
     * @return true if an Element matching the key is found in the cache. No assertions are made about the state of the Element.
     */
    public boolean isKeyInCache(Object key) {
        return cache.isKeyInCache(key);
    }

    /**
     * An extremely expensive check to see if the value exists in the cache.
     *
     * @param value to check for
     * @return true if an Element matching the key is found in the cache. No assertions are made about the state of the Element.
     */
    public boolean isValueInCache(Object value) {
        return cache.isValueInCache(value);
    }

    /**
     * Gets an immutable Statistics object representing the Cache statistics at the time. How the statistics are calculated
     * depends on the statistics accuracy setting. The only aspect of statistics sensitive to the accuracy setting is
     * object size. How that is calculated is discussed below.
     * <h3>Best Effort Size</h3>
     * This result is returned when the statistics accuracy setting is {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_BEST_EFFORT}.
     * <p/>
     * The size is the number of {@link net.sf.ehcache.Element}s in the {@link net.sf.ehcache.store.MemoryStore} plus
     * the number of {@link net.sf.ehcache.Element}s in the {@link net.sf.ehcache.store.DiskStore}.
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
     * This result is returned when the statistics accuracy setting is {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_GUARANTEED}.
     * <p/>
     * This method accounts for elements which might be expired or duplicated between stores. It take approximately
     * 200ms per 1000 elements to execute.
     * <h3>Fast but non-accurate Size</h3>
     * This result is returned when the statistics accuracy setting is {@link net.sf.ehcache.Statistics#STATISTICS_ACCURACY_NONE}.
     * <p/>
     * The number given may contain expired elements. In addition if the DiskStore is used it may contain some double
     * counting of elements. It takes 6ms for 1000 elements to execute. Time to execute is O(log n). 50,000 elements take
     * 36ms.
     *
     * @return the number of elements in the ehcache, with a varying degree of accuracy, depending on accuracy setting.
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public Statistics getStatistics() throws IllegalStateException {
        return cache.getStatistics();
    }

    /**
     * Sets the CacheManager
     *
     * @param cacheManager
     */
    public void setCacheManager(CacheManager cacheManager) {
        cache.setCacheManager(cacheManager);
    }

    /**
     * Accessor for the BootstrapCacheLoader associated with this cache. For testing purposes.
     */
    public BootstrapCacheLoader getBootstrapCacheLoader() {
        return cache.getBootstrapCacheLoader();
    }

    /**
     * Sets the bootstrap cache loader.
     *
     * @param bootstrapCacheLoader the loader to be used
     * @throws net.sf.ehcache.CacheException if this method is called after the cache is initialized
     */
    public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) throws CacheException {
        cache.setBootstrapCacheLoader(bootstrapCacheLoader);
    }

    /**
     * DiskStore paths can conflict between CacheManager instances. This method allows the path to be changed.
     *
     * @param diskStorePath the new path to be used.
     * @throws net.sf.ehcache.CacheException if this method is called after the cache is initialized
     */
    public void setDiskStorePath(String diskStorePath) throws CacheException {
        cache.setDiskStorePath(diskStorePath);
    }

    /**
     * Newly created caches do not have a {@link net.sf.ehcache.store.MemoryStore} or a {@link net.sf.ehcache.store.DiskStore}.
     * <p/>
     * This method creates those and makes the cache ready to accept elements
     */
    public void initialise() {
        cache.initialise();
    }

    /**
     * Bootstrap command. This must be called after the Cache is intialised, during
     * CacheManager initialisation. If loads are synchronous, they will complete before the CacheManager
     * initialise completes, otherwise they will happen in the background.
     */
    public void bootstrap() {
        cache.bootstrap();
    }

    /**
     * Flushes all cache items from memory to auxilliary caches and close the auxilliary caches.
     * <p/>
     * Should be invoked only by CacheManager.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public void dispose() throws IllegalStateException {
        cache.dispose();
    }

    /**
     * Gets the cache configuration this cache was created with.
     * <p/>
     * Things like listeners that are added dynamically are excluded.
     */
    public CacheConfiguration getCacheConfiguration() {
        return cache.getCacheConfiguration();
    }

    /**
     * Looks up an entry.  Blocks if the entry is null until a call to {@link #put} is done
     * to put an Element in.
     * <p/>
     * If a put is not done, the lock is never released
     * <p/>
     * Note. If a LockTimeoutException is thrown while doing a {@link #get} it means the lock was never acquired,
     * therefore it is a threading error to call {@link #put}
     *
     * @throws LockTimeoutException if timeout millis is non zero and this method has been unable to
     *                              acquire a lock in that time
     */
    public Element get(final Object key) throws LockTimeoutException {
        Mutex lock = getLockForKey(key);
        try {
            if (timeoutMillis == 0) {
                lock.acquire();
            } else {
                boolean acquired = lock.attempt(timeoutMillis);
                if (!acquired) {
                    StringBuffer message = new StringBuffer("Lock timeout. Waited more than ")
                            .append(timeoutMillis)
                            .append("ms to acquire lock for key ")
                            .append(key).append(" on blocking cache ").append(cache.getName());
                    throw new LockTimeoutException(message.toString());
                }
            }
            final Element element = cache.get(key);
            if (element != null) {
                //ok let the other threads in
                lock.release();
                return element;
            } else {
                //don't release the read lock until we put
                return null;
            }
        } catch (InterruptedException e) {
            throw new CacheException("Interrupted. Message was: " + e.getMessage());
        }
    }


    /**
     * Gets the Mutex to use for a given key.
     *
     * @param key the key
     * @return one of a limited number of Mutexes.
     */
    protected Mutex getLockForKey(final Object key) {
        int lockNumber = ConcurrencyUtil.selectLock(key, LOCK_NUMBER);
        return locks[lockNumber];
    }

    /**
     * Adds an entry and unlocks it
     */
    public void put(Element element) {

        if (element == null) {
            return;
        }
        Object key = element.getObjectKey();
        Object value = element.getObjectValue();

        Mutex lock = getLockForKey(key);
        try {
            if (value != null) {
                cache.put(element);
            } else {
                cache.remove(key);
            }
        } finally {
            //Release the readlock here. This will have been acquired in the get, where the element was null
            lock.release();
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
     * @param element                     An object. If Serializable it can fully participate in replication and the DiskStore.
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer, in which case this put should not initiate a
     *                                    further notification to doNotNotifyCacheReplicators cache peers
     * @throws IllegalStateException    if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        cache.put(element, doNotNotifyCacheReplicators);
    }

    /**
     * Put an element in the cache, without updating statistics, or updating listeners. This is meant to be used
     * in conjunction with {@link #getQuiet}
     *
     * @param element An object. If Serializable it can fully participate in replication and the DiskStore.
     * @throws IllegalStateException    if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @throws IllegalArgumentException if the element is null
     */
    public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        cache.putQuiet(element);
    }

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
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return this.get((Object) key);
    }


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
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return cache.getQuiet(key);
    }

    /**
     * Gets an element from the cache, without updating Element statistics. Cache statistics are
     * still updated.
     * <p/>
     *
     * @param key a serializable value
     * @return the element, or null, if it does not exist.
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     * @see #isExpired
     * @since 1.2
     */
    public Element getQuiet(Object key) throws IllegalStateException, CacheException {
        return cache.getQuiet(key);
    }

    /**
     * Returns the keys for this cache.
     *
     * @return The keys of this cache.  This is not a live set, so it will not track changes to the key set.
     */
    public List getKeys() throws CacheException {
        return cache.getKeys();
    }

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
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        return cache.getKeysWithExpiryCheck();
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
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        return cache.getKeysNoDuplicateCheck();
    }

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
     */
    public boolean remove(Serializable key) throws IllegalStateException {
        return cache.remove(key);
    }

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
    public boolean remove(Object key) throws IllegalStateException {
        return cache.remove(key);
    }

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
     * @noinspection SameParameterValue
     */
    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return cache.remove(key, doNotNotifyCacheReplicators);
    }

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
    public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return cache.remove(key, doNotNotifyCacheReplicators);
    }

    /**
     * Removes an {@link net.sf.ehcache.Element} from the Cache, without notifying listeners. This also removes it from any
     * stores it may be in.
     * <p/>
     *
     * @param key
     * @return true if the element was removed, false if it was not found in the cache
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return cache.removeQuiet(key);
    }

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
    public boolean removeQuiet(Object key) throws IllegalStateException {
        return cache.removeQuiet(key);
    }

    /**
     * Removes all cached items.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        cache.removeAll();
    }

    /**
     * Removes all cached items.
     *
     * @param doNotNotifyCacheReplicators whether the put is coming from a doNotNotifyCacheReplicators cache peer,
     *                                    in which case this put should not initiate a further notification to doNotNotifyCacheReplicators cache peers
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        cache.removeAll(doNotNotifyCacheReplicators);
    }

    /**
     * Flushes all cache items from memory to the disk store, and from the DiskStore to disk.
     *
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public void flush() throws IllegalStateException, CacheException {
        cache.flush();
    }

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
    public int getSize() throws IllegalStateException, CacheException {
        return cache.getSize();
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
    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        return cache.calculateInMemorySize();
    }

    /**
     * Returns the number of elements in the memory store.
     *
     * @return the number of elements in the memory store
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public long getMemoryStoreSize() throws IllegalStateException {
        return cache.getMemoryStoreSize();
    }

    /**
     * Returns the number of elements in the disk store.
     *
     * @return the number of elements in the disk store.
     * @throws IllegalStateException if the cache is not {@link net.sf.ehcache.Status#STATUS_ALIVE}
     */
    public int getDiskStoreSize() throws IllegalStateException {
        return cache.getDiskStoreSize();
    }

    /**
     * Gets the status attribute of the Cache.
     *
     * @return The status value from the Status enum class
     */
    public Status getStatus() {
        return cache.getStatus();
    }

    /**
     * Synchronized version of getName to test liveness of the object lock.
     * <p/>
     * The time taken for this method to return is a useful measure of runtime contention on the cache.
     */
    public synchronized String liveness() {
        return getName();
    }

    /**
     * Sets the time to wait to acquire a lock. This may be modified at any time.
     * <p/>
     * The consequences of setting a timeout are:
     * <ol>
     * <li>if a lock cannot be acquired in the given time a LockTimeoutException is thrown.
     * <li>if there is a queue of threads waiting for the first thread to complete, but it does not complete within
     * the time out period, the successive threads may find that they have exceeded their lock timeouts and fail. This
     * is usually a good thing because it stops a build up of threads from overwhelming a busy resource, but it does
     * need to be considered in the design of user interfaces. The timeout should be set no greater than the time a user
     * would be expected to wait before considering the action will never return
     * <li>it will be common to see a number of threads timeout trying to get the same lock. This is a normal and desired
     * consequence.
     * </ol>
     * The consequences of not setting a timeout (or setting it to 0) are:
     * <ol>
     * <li>There are no partial failures in the system. But there is a greater possibility that a temporary overload
     * in one part of the system can cause a back up that may take a long time to recover from.
     * <li>A failing method that perhaps fails because a resource is overloaded will be hit by each thread in turn, no matter whether there is a still a user who
     * cares about getting a response.
     * </ol>
     *
     * @param timeoutMillis the time in ms. Must be a positive number. 0 means wait forever.
     */
    public void setTimeoutMillis(int timeoutMillis) {
        if (timeoutMillis < 0) {
            throw new CacheException("The lock timeout must be a positive number of ms. Value was " + timeoutMillis);
        }
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Gets the time to wait to acquire a lock.
     *
     * @return the time in ms.
     */
    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Register a {@link net.sf.ehcache.extension.CacheExtension} with the cache. It will then be tied into the cache lifecycle.
     */
    public void registerCacheExtension(CacheExtension cacheExtension) {
        cache.registerCacheExtension(cacheExtension);
    }

    /**
     * Unregister a {@link net.sf.ehcache.extension.CacheExtension} with the cache. It will then be detached from the cache lifecycle.
     */
    public void unregisterCacheExtension(CacheExtension cacheExtension) {
        cache.unregisterCacheExtension(cacheExtension);
    }


}



