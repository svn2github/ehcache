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

package net.sf.ehcache.constructs.blocking;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;
import net.sf.ehcache.concurrent.StripedReadWriteLockSync;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;
import net.sf.ehcache.loader.CacheLoader;

/**
 * A blocking decorator for an Ehcache, backed by a {@link Ehcache}.
 * <p/>
 * It allows concurrent read access to elements already in the cache. If the element is null, other
 * reads will block until an element with the same key is put into the cache.
 * <p/>
 * This is useful for constructing read-through or self-populating caches.
 * <p/>
 * This implementation uses the {@link net.sf.ehcache.concurrent.ReadWriteLockSync} class. If you wish to use
 * this class, you will need the concurrent package in your class path.
 * <p/>
 * It features:
 * <ul>
 * <li>Excellent liveness.
 * <li>Fine-grained locking on each element, rather than the cache as a whole.
 * <li>Scalability to a large number of threads.
 * </ul>
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
public class BlockingCache extends EhcacheDecoratorAdapter {

    /**
     * The amount of time to block a thread before a LockTimeoutException is thrown
     */
    protected volatile int timeoutMillis;

    private final int stripes;
    private final AtomicReference<CacheLockProvider> cacheLockProviderReference;
    
    /**
     * Creates a BlockingCache which decorates the supplied cache.
     *
     * @param cache           a backing ehcache.
     * @param numberOfStripes how many stripes to has the keys against. Must be a non-zero even number. This is a trade-off between
     *                        memory use and concurrency
     * @throws CacheException shouldn't happen
     * @since 1.2
     */
    public BlockingCache(final Ehcache cache, int numberOfStripes) throws CacheException {
        super(cache);
        this.stripes = numberOfStripes;
        this.cacheLockProviderReference = new AtomicReference<CacheLockProvider>();
    }

    /**
     * Creates a BlockingCache which decorates the supplied cache.
     *
     * @param cache a backing ehcache.
     * @throws CacheException shouldn't happen
     * @since 1.6.1
     */
    public BlockingCache(final Ehcache cache) throws CacheException {
        this(cache, StripedReadWriteLockSync.DEFAULT_NUMBER_OF_MUTEXES);
    }

    private CacheLockProvider getCacheLockProvider() {
        CacheLockProvider provider = cacheLockProviderReference.get();
        while (provider == null) {
            cacheLockProviderReference.compareAndSet(null, createCacheLockProvider());
            provider = cacheLockProviderReference.get();
        }
        return provider;
    }

    private CacheLockProvider createCacheLockProvider() {
        Object context = underlyingCache.getInternalContext();
        if (underlyingCache.getCacheConfiguration().isTerracottaClustered() && context != null) {
            return (CacheLockProvider) context;
        } else {
            return new StripedReadWriteLockSync(stripes);
        }
    }

    /**
     * Retrieve the EHCache backing cache
     *
     * @return the backing cache
     */
    protected Ehcache getCache() {
        return underlyingCache;
    }

    /**
     * Looks up an entry.  Blocks if the entry is null until a call to {@link #put} is done
     * to put an Element in.
     * <p/>
     * If a put is not done, the lock is never released.
     * <p/>
     * If this method throws an exception, it is the responsibility of the caller to catch that exception and call
     * <code>put(new Element(key, null));</code> to release the lock acquired. See {@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}
     * for an example.
     * <p/>
     * Note. If a LockTimeoutException is thrown while doing a <code>get</code> it means the lock was never acquired,
     * therefore it is a threading error to call {@link #put}
     *
     * @throws LockTimeoutException if timeout millis is non zero and this method has been unable to
     *                              acquire a lock in that time
     * @throws RuntimeException     if thrown the lock will not released. Catch and call<code>put(new Element(key, null));</code>
     *                              to release the lock acquired.
     */
    @Override
    public Element get(final Object key) throws RuntimeException, LockTimeoutException {

        Sync lock = getLockForKey(key);
        acquiredLockForKey(key, lock, LockType.READ);
        Element element;
        try {
            element = underlyingCache.get(key);
        } finally {
            lock.unlock(LockType.READ);
        }
        if (element == null) {
            acquiredLockForKey(key, lock, LockType.WRITE);
            element = underlyingCache.getQuiet(key);
            if (element != null) {
                if (underlyingCache.isStatisticsEnabled()) {
                    element = underlyingCache.get(key);
                }
                lock.unlock(LockType.WRITE);
            }
        }
        return element;
    }

    private void acquiredLockForKey(final Object key, final Sync lock, final LockType lockType) {
        if (timeoutMillis > 0) {
            try {
                boolean acquired = lock.tryLock(lockType, timeoutMillis);
                if (!acquired) {
                    StringBuilder message = new StringBuilder("Lock timeout. Waited more than ")
                            .append(timeoutMillis)
                            .append("ms to acquire lock for key ")
                            .append(key).append(" on blocking cache ").append(underlyingCache.getName());
                    throw new LockTimeoutException(message.toString());
                }
            } catch (InterruptedException e) {
                throw new LockTimeoutException("Got interrupted while trying to acquire lock for key " + key, e);
            }
        } else {
            lock.lock(lockType);
        }
    }


    /**
     * Gets the Sync to use for a given key.
     *
     * @param key the key
     * @return one of a limited number of Sync's.
     */
    protected Sync getLockForKey(final Object key) {
        return getCacheLockProvider().getSyncForKey(key);
    }

    /**
     * Adds an entry and unlocks it
     */
    @Override
    public void put(final Element element) {

        doAndReleaseWriteLock(new PutAction<Void>(element) {
            @Override
            public Void put() {
                if (element.getObjectValue() != null) {
                    underlyingCache.put(element);
                } else {
                    underlyingCache.remove(element.getObjectKey());
                }
                return null;
            }
        });
    }

    @Override
    public void put(final Element element, final boolean doNotNotifyCacheReplicators)
        throws IllegalArgumentException, IllegalStateException, CacheException {
        doAndReleaseWriteLock(new PutAction<Void>(element) {
            @Override
            public Void put() {
                underlyingCache.put(element, doNotNotifyCacheReplicators);
                return null;
            }
        });
    }

    @Override
    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        doAndReleaseWriteLock(new PutAction<Void>(element) {
            @Override
            public Void put() {
                underlyingCache.putQuiet(element);
                return null;
            }
        });
    }

    @Override
    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        doAndReleaseWriteLock(new PutAction<Void>(element) {
            @Override
            public Void put() {
                underlyingCache.putWithWriter(element);
                return null;
            }
        });
    }

    @Override
    public Element putIfAbsent(final Element element) throws NullPointerException {
        return doAndReleaseWriteLock(new PutAction<Element>(element) {
            @Override
            public Element put() {
                return underlyingCache.putIfAbsent(element);
            }
        });
    }

    @Override
    public Element putIfAbsent(final Element element, final boolean doNotNotifyCacheReplicators) throws NullPointerException {
        return doAndReleaseWriteLock(new PutAction<Element>(element) {
            @Override
            public Element put() {
                return underlyingCache.putIfAbsent(element, doNotNotifyCacheReplicators);
            }
        });
    }

    private <V> V doAndReleaseWriteLock(PutAction<V> putAction) {

        if (putAction.element == null) {
            return null;
        }

        Object key = putAction.element.getObjectKey();

        Sync lock = getLockForKey(key);

        if (!lock.isHeldByCurrentThread(LockType.WRITE)) {
            lock.lock(LockType.WRITE);
        }
        try {
            return putAction.put();
        } finally {
            //Release the writelock here. This will have been acquired in the get, where the element was null
            lock.unlock(LockType.WRITE);
        }
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
    @Override
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return this.get((Object) key);
    }

    /**
     * Synchronized version of getName to test liveness of the object lock.
     * <p/>
     * The time taken for this method to return is a useful measure of runtime contention on the cache.
     *
     * @return the name of the cache.
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
     * Register a {@link CacheLoader} with the cache. It will then be tied into the cache lifecycle.
     * <p/>
     * If the CacheLoader is not initialised, initialise it.
     *
     * @param cacheLoader A Cache Loader to register
     */
    @Override
    public void registerCacheLoader(CacheLoader cacheLoader) {
        throw new CacheException("This method is not appropriate for a blocking cache.");
    }

    /**
     * Unregister a {@link CacheLoader} with the cache. It will then be detached from the cache lifecycle.
     *
     * @param cacheLoader A Cache Loader to unregister
     */
    @Override
    public void unregisterCacheLoader(CacheLoader cacheLoader) {
        throw new CacheException("This method is not appropriate for a blocking cache.");
    }

    /**
     * This method is not appropriate to use with BlockingCache.
     *
     * @throws CacheException if this method is called
     */
    @Override
    public Element getWithLoader(Object key, CacheLoader loader, Object loaderArgument) throws CacheException {
        throw new CacheException("This method is not appropriate for a Blocking Cache");
    }

    /**
     * This method is not appropriate to use with BlockingCache.
     *
     * @throws CacheException if this method is called
     */
    @Override
    public Map getAllWithLoader(Collection keys, Object loaderArgument) throws CacheException {
        throw new CacheException("This method is not appropriate for a Blocking Cache");
    }

    /**
     * This method is not appropriate to use with BlockingCache.
     *
     * @throws CacheException if this method is called
     */
    @Override
    public void load(Object key) throws CacheException {
        throw new CacheException("This method is not appropriate for a Blocking Cache");
    }

    /**
     * This method is not appropriate to use with BlockingCache.
     *
     * @throws CacheException if this method is called
     */
    @Override
    public void loadAll(Collection keys, Object argument) throws CacheException {
        throw new CacheException("This method is not appropriate for a Blocking Cache");
    }

    /**
     * Callable like class to actually execute one of the many Ehcache.put* methods in the context of a BlockingCache
     * @param <V>
     */
    private abstract static class PutAction<V> {

        private final Element element;

        private PutAction(Element element) {
            this.element = element;
        }

        /**
         * implement method with the put*
         * @return the return value of the put*
         */
        abstract V put();
    }
}



