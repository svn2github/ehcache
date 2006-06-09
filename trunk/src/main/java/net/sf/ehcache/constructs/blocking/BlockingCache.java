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

package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.concurrent.Mutex;
import net.sf.ehcache.constructs.valueobject.KeyValuePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * A blocking cache, backed by {@link Ehcache}.
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
public class BlockingCache {

    /**
     * The default number of locks to use. Must be a power of 2
     */
    protected static final int LOCK_NUMBER = 2048;

    static {
        int number = LOCK_NUMBER & (LOCK_NUMBER - 1);
        if (number != 0) {
            throw new IllegalStateException("Lock number must be a power of two: " + LOCK_NUMBER);
        }
    }

    private static final Log LOG = LogFactory.getLog(BlockingCache.class.getName());

    private static final int DOUG_LEA_BLACK_MAGIC_OPERAND_1 = 20;
    private static final int DOUG_LEA_BLACK_MAGIC_OPERAND_2 = 12;
    private static final int DOUG_LEA_BLACK_MAGIC_OPERAND_3 = 7;
    private static final int DOUG_LEA_BLACK_MAGIC_OPERAND_4 = 4;



    /**
     * Based on the lock striping concept from Brian Goetz. See Java Concurrency in Practice 11.4.3
     */
    private final Mutex[] locks = new Mutex[LOCK_NUMBER];

    {

        for (int i = 0; i < LOCK_NUMBER; i++) {
            locks[i] = new Mutex();
        }
    }

    /**
     * The backing Cache
     */
    private final Ehcache cache;


    private final int timeoutMillis;

    /**
     * A map of cache entry locks, one per key, if present
     */
    //private final Map locks = new HashMap();

    /**
     * Creates a BlockingCache with the given name.
     *
     * @param name the name to give the cache
     * @throws CacheException
     */
    public BlockingCache(final String name) throws CacheException {
        this(name, 0);
    }

    /**
     * Creates a BlockingCache with the given name.
     *
     * @param name          the name to give the cache
     * @param timeoutMillis the amount of time, in milliseconds, to block for
     * @throws CacheException
     * @since 1.2
     */
    public BlockingCache(final String name, int timeoutMillis) throws CacheException {
        CacheManager manager = null;
        try {
            manager = CacheManager.create();
        } catch (net.sf.ehcache.CacheException e) {
            LOG.fatal("CacheManager cannot be created. Cause was: " + e.getMessage() + e);
            throw new CacheException("CacheManager cannot be created", e);
        }
        cache = manager.getCache(name);
        if (cache == null || !cache.getName().equals(name)) {
            throw new CacheException("Cache " + name + " cannot be retrieved. Please check ehcache.xml");
        }
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Creates a BlockingCache with the given name and
     * uses the given cache manager to create the cache
     *
     * @param name    the name to give the cache
     * @param manager the EHCache CacheManager used to create the backing cache
     * @throws CacheException
     */
    public BlockingCache(final String name, final CacheManager manager) throws CacheException {
        this(name, manager, 0);
    }

    /**
     * Creates a BlockingCache with the given name and
     * uses the given cache manager to create the cache
     *
     * @param name          the name to give the cache
     * @param manager       the EHCache CacheManager used to create the backing cache
     * @param timeoutMillis the amount of time, in milliseconds, to block for
     * @throws CacheException
     * @since 1.2
     */
    public BlockingCache(final String name, final CacheManager manager, int timeoutMillis) throws CacheException {
        if (manager == null) {
            throw new CacheException("CacheManager cannot be null");
        }
        cache = manager.getCache(name);
        if (cache == null || !cache.getName().equals(name)) {
            throw new CacheException("Cache " + name + " cannot be retrieved. Please check ehcache.xml");
        }
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Retrieve the EHCache backing cache
     */
    protected net.sf.ehcache.Ehcache getCache() {
        return cache;
    }

    /**
     * Returns this cache's name
     */
    public String getName() {
        return cache.getName();
    }

    /**
     * Looks up an entry.  Blocks if the entry is null.
     * Relies on the first thread putting an entry in, which releases the lock
     * If a put is not done, the lock is never released
     */
    public Serializable get(final Serializable key) throws BlockingCacheException {
        Mutex lock = getLockForKey(key);
        try {
            if (timeoutMillis == 0) {
                lock.acquire();
            } else {
                boolean acquired = lock.attempt(timeoutMillis);
                if (!acquired) {
                    StringBuffer message = new StringBuffer("lock timeout attempting to acquire lock for key ")
                            .append(key).append(" on cache ").append(cache.getName());
                    throw new BlockingCacheException(message.toString());
                }
            }
            final Element element = cache.get(key);
            if (element != null) {
                //ok let the other threads in
                lock.release();
                return element.getValue();
            } else {
                //don't release the read lock until we write
                return null;
            }
        } catch (InterruptedException e) {
            throw new CacheException("Interrupted. Message was: " + e.getMessage());
        }
    }


    private Mutex getLockForKey(final Object key) {
        int lockNumber = selectLock(key);
        return locks[lockNumber];
    }

    /**
     * Selects a lock for a key. The same lock is always used for a given key.
     *
     * @param key
     * @return the selected lock index
     */
    int selectLock(final Object key) {
        if (key == null) {
            return 0;
        } else {
            int hash = hash(key) & (LOCK_NUMBER - 1);
            return hash;
        }

    }


    /**
     * Returns a hash code for non-null Object x.
     * <p/>
     * This function ensures that hashCodes that differ only by
     * constant multiples at each bit position have a bounded
     * number of collisions. (Doug Lea)
     *
     * @param object the object serving as a key
     * @return the hash code
     */
    private static int hash(Object object) {

        int h = object.hashCode();
        h ^= (h >>> DOUG_LEA_BLACK_MAGIC_OPERAND_1) ^ (h >>> DOUG_LEA_BLACK_MAGIC_OPERAND_2);
        return h ^ (h >>> DOUG_LEA_BLACK_MAGIC_OPERAND_3) ^ (h >>> DOUG_LEA_BLACK_MAGIC_OPERAND_4);
    }

    /**
     * Adds an entry and unlocks it
     */
    public void put(final Serializable key, final Serializable value) {
        Mutex lock = getLockForKey(key);
        try {
            if (value != null) {
                final Element element = new Element(key, value);
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
     * Returns the keys for this cache.
     *
     * @return The keys of this cache.  This is not a live set, so it will not track changes to the key set.
     */
    public Collection getKeys() throws CacheException {
        return cache.getKeys();
    }

    /**
     * Drops the contents of this cache.
     */
    public void clear() throws CacheException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Cache " + cache.getName() + ": removing all entries");
        }
        cache.removeAll();
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
     * Gets all entries from a blocking cache. Cache is not serializable. This
     * method provides a way of accessing the keys and values of a Cache in a Serializable way e.g.
     * to return from a Remote call.
     * <p/>
     * This method may take a long time to return. It does not lock the cache. The list of entries is based
     * on a copy. The actual cache may have changed in the time between getting the list and gathering the
     * KeyValuePairs.
     * <p/>
     * This method can potentially return an extremely large object, roughly matching the memory taken by the cache
     * itself. Care should be taken before using this method.
     * <p/>
     * By getting all of the entries at once this method can transfer a whole cache with a single method call, which
     * is important for Remote calls across a network.
     *
     * @return a Serializable {@link java.util.List} of {@link KeyValuePair}s, which implement the Map.Entry interface
     * @throws CacheException where there is an error in the underlying cache
     */
    public List getEntries() throws CacheException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting entries for the " + cache.getName() + " cache");
        }
        Collection keys = cache.getKeys();
        List keyValuePairs = new ArrayList(keys.size());
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            Serializable key = (Serializable) iterator.next();
            Element element = cache.get(key);
            keyValuePairs.add(new KeyValuePair(key, element.getValue()));
        }
        return keyValuePairs;
    }
}



