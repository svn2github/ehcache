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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * A {@link net.sf.ehcache.Cache} backed cache that creates entries on demand.
 * <p/>
 * Clients of the cache simply call it without needing knowledge of whether
 * the entry exists in the cache, or whether it needs updating before use.
 * <p/>
 *
 * Thread safety depends on the factory being used. The UpdatingCacheEntryFactory should be made
 * thread safe. In addition users of returned values should not modify their contents.
 *
 * @author Greg Luck
 * @version $Id$
 * @see SelfPopulatingCollectionCache
 */
public class UpdatingSelfPopulatingCache extends SelfPopulatingCache {
    private static final Log LOG = LogFactory.getLog(UpdatingSelfPopulatingCache.class.getName());
    private final LockManager lockManager;

    /**
     * Creates a SelfPopulatingCache.
     */
    public UpdatingSelfPopulatingCache(final String name, final UpdatingCacheEntryFactory factory)
        throws CacheException {
        super(name, factory);
        lockManager = new LockManager();
    }


    /**
     * Creates a SelfPopulatingCache and use the given cache manager to create cache objects
     */
    public UpdatingSelfPopulatingCache(final String name, final CacheManager mgr, final CacheEntryFactory factory)
        throws CacheException {
        super(name, mgr, factory);
        lockManager = new LockManager();
    }

    /**
     * Looks up an object.
     * <p/>
     * If null, it creates it. If not null, it updates it. For performance this method should only be
     * used with {@link UpdatingCacheEntryFactory}'s
     * <p/>
     * It is expected that
     * gets, which update as part of the get, might take considerable time. Access to the cache cannot be blocked
     * while that is happening. This method is therefore not synchronized. The {@link LockManager} is used
     * to synchronise individual entries.
     * @param key
     * @return a value
     * @throws net.sf.ehcache.CacheException
     */
    public Serializable get(final Serializable key) throws BlockingCacheException {
        String oldThreadName = Thread.currentThread().getName();
        setThreadName("get", key);

        Serializable value = null;

        try {
            lockManager.acquireLock(key);

            Ehcache backingCache = getCache();
            Element element = backingCache.get(key);

            if (element == null) {
                value = super.get(key);
            } else {
                value = element.getValue();
                update(key);
            }

            return value;
        } catch (final Throwable throwable) {
            // Could not fetch - Ditch the entry from the cache and rethrow
            setThreadName("put", key);
            put(key, null);

            try {
                throw new BlockingCacheException("Could not fetch object for cache entry \"" + key + "\".", throwable);
            } catch (NoSuchMethodError e) {
                //Running 1.3 or lower
                throw new CacheException("Could not fetch object for cache entry \"" + key + "\".");
            }
        } finally {
            Thread.currentThread().setName(oldThreadName);
            lockManager.releaseLock(key);
        }
    }

    private void update(final Serializable key) {
        try {
            Ehcache backingCache = getCache();
            final Element element = backingCache.getQuiet(key);

            if (element == null) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(getName() + ": entry with key " + key + " has been removed - skipping it");
                }
            }

            refreshElement(element, backingCache);
        } catch (final Exception e) {
            // Collect the exception and keep going.
            // Throw the exception once all the entries have been refreshed
            // If the refresh fails, keep the old element. It will simply become staler.
            LOG.warn(getName() + "Could not refresh element " + key, e);
        }
    }

    /**
     * This method should not be used. Because elements are always updated before they are
     * returned, it makes no sense to refresh this cache.
     */
    public void refresh() throws CacheException {
        throw new CacheException("UpdatingSelfPopulatingCache objects should not be refreshed.");
    }

    /**
     * Provides locking at the level of a single cache entry using Doug Lea's concurrency library.
     * <p/>
     * This permits scalability of an order of magnitude higher than simple class instance locking
     * provided by the synchronized method.
     *
     */
    public static class LockManager {
        /**
         * A map of cache entry locks, one per key, if present
         */
        protected final Map locks;

        /**
         * Creates a new LockManager and initialises the locks Map
         */
        public LockManager() {
            locks = new HashMap();
        }

        /**
         * Acquires a lock with the given key.
         * <p/>
         * If the lock does not exist it is created.
         * @param key
         * @throws InterruptedException
         */
        public void acquireLock(Serializable key) throws InterruptedException {
            Mutex lock = checkLockExistsForKey(key);
            lock.acquire();
        }

        /**
         * Releases a lock with the given key
         * <p/>
         * If the lock does not exist it is created.
         * @param key
         */
        public void releaseLock(Serializable key) {
            Mutex lock = checkLockExistsForKey(key);
            lock.release();
        }

        private synchronized Mutex checkLockExistsForKey(final Serializable key) {
            Mutex lock;
            lock = (Mutex) locks.get(key);

            if (lock == null) {
                lock = new Mutex();
                locks.put(key, lock);
            }
            return lock;
        }
    }
}
