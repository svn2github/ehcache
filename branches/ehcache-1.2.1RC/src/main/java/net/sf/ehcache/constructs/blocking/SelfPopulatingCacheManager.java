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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A cache manager for self populating caches
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public abstract class SelfPopulatingCacheManager extends BlockingCacheManager {

    /**
     * Constructor. Caches are set up here.
     */
    public SelfPopulatingCacheManager() throws CacheException {
        super();
        setupCaches();
    }

    /**
     * Constructor. Caches are set up here.
     */
    public SelfPopulatingCacheManager(CacheManager mgr) throws CacheException {
        super(mgr);
        setupCaches();
    }

    /**
     * Gets a self-populating cache.
     *
     * @param name the name of the cache
     * @throws CacheException If the cache does not exist.
     */
    public SelfPopulatingCache getSelfPopulatingCache(final String name) throws CacheException {
        // Create the cache
        final SelfPopulatingCache cache = (SelfPopulatingCache) caches.get(name);
        if (cache == null) {
            throw new CacheException("Cache " + name + " cannot be retrieved. Please check ehcache.xml");
        }
        return cache;
    }

    /**
     * Refreshes all caches.
     */
    public void refreshAll() throws Exception {
        final List caches = getCaches();
        for (int i = 0; i < caches.size(); i++) {
            final SelfPopulatingCache cache = (SelfPopulatingCache) caches.get(i);
            cache.refresh();
        }
    }

    /**
     * Refreshes a SelfPopulatingCache. The cache will repopulate itself.
     *
     * @param name the name of the cace
     * @throws CacheException
     */
    public void refresh(final String name) throws CacheException {
        final SelfPopulatingCache cache = (SelfPopulatingCache) getSelfPopulatingCache(name);
        cache.refresh();
    }

    /**
     * Refreshes a single entry in a SelfPopulatingCache.
     * The old entry is discarded and then requested, causing it to be populated.
     * Note: Used by tests only, do not use in production.
     */
    public void refreshEntry(final String cacheName, final Serializable key) throws Exception {
        final SelfPopulatingCache cache = (SelfPopulatingCache) getSelfPopulatingCache(cacheName);
        cache.put(key, null);
        cache.get(key);
    }

    /**
     * Creates a self-populating cache.
     */
    protected synchronized SelfPopulatingCache createSelfPopulatingCache(final String name,
                                                                         final CacheEntryFactory factory) throws CacheException {
        if (caches.containsKey(name)) {
            throw new CacheException("A cache with name \"" + name + "\" already exists.");
        }

        // Create the cache
        SelfPopulatingCache cache = null;
        if (getCacheManager() == null) {
            cache = new SelfPopulatingCache(name, factory);
        } else {
            cache = new SelfPopulatingCache(name, super.getCacheManager(), factory);
        }
        caches.put(name, cache);
        return cache;
    }

    /**
     * Creates a self-populating cache with an UpdatingCacheEntryFactory
     */
    protected synchronized SelfPopulatingCache createUpdatingSelfPopulatingCache(final String name,
            final UpdatingCacheEntryFactory factory) throws CacheException {
        if (caches.containsKey(name)) {
            throw new CacheException("A cache with name \"" + name + "\" already exists.");
        }

        // Create the cache
        SelfPopulatingCache cache = null;
        if (getCacheManager() == null) {
            cache = new SelfPopulatingCache(name, factory);
        } else {
            cache = new SelfPopulatingCache(name, super.getCacheManager(), factory);
        }

        caches.put(name, cache);
        return cache;
    }

    /**
     * Builds the set of caches. Returns a copy so that the monitor can be released.
     */
    protected synchronized List getCaches() {
        final ArrayList caches = new ArrayList();
        caches.addAll(this.caches.values());
        return caches;
    }

    /**
     * Sets up the caches used by the manager.
     */
    protected synchronized void setupCaches() throws CacheException {
        doSetupCaches();
    }

    /**
     * A template method to set up caches. It is wrapped by {@link #setupCaches}
     * to ensure that caches are created within a synchronized method.
     * <p/>
     * Implementations of this method should typically call {@link #createSelfPopulatingCache(java.lang.String, net.sf.ehcache.constructs.blocking.CacheEntryFactory)},
     * or {@link #createUpdatingSelfPopulatingCache(java.lang.String, net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory)}
     * for each required cache.
     */
    protected abstract void doSetupCaches() throws CacheException;
}
