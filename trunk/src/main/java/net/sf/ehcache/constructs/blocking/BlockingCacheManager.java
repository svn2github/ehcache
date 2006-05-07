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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * There can be multiple CacheManagers.
 *
 * @author Greg Luck
 * @author Adam Murdoch
 * @version $Id$
 */
public class BlockingCacheManager {

    private static final Log LOG = LogFactory.getLog(BlockingCacheManager.class.getName());

    /**
     * A custom cache manager, in case the user does not
     * want to use the default cache manager when
     * creating custom caches.
     */
    private static CacheManager manager;

    /**
     * A map of BlockingCaches, keyed by cache name
     */
    protected final Map caches;


    /**
     * Empty Constructor
     */
    public BlockingCacheManager() {
        caches = new HashMap();
    }

    /**
     * Constructor that assigns the cache manager to use when
     * creating caches.
     */
    public BlockingCacheManager(CacheManager mgr) {
        manager = mgr;
        caches = new HashMap();
    }

    /**
     * Creates a cache.
     */
    public BlockingCache getCache(final String name) throws CacheException {
        // Lookup the cache
        BlockingCache blockingCache = (BlockingCache) caches.get(name);
        if (blockingCache != null) {
            return blockingCache;
        }

        // Create the cache
        synchronized (this) {
            if (manager == null) {
                blockingCache = new BlockingCache(name);
            } else {
                blockingCache = new BlockingCache(name, manager);
            }

            caches.put(name, blockingCache);
            return blockingCache;
        }
    }

    /**
     * Drops the contents of all caches.
     */
    public void clearAll() throws CacheException {
        final List cacheList = getCaches();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Removing all blocking caches");
        }
        for (int i = 0; i < cacheList.size(); i++) {
            final BlockingCache cache = (BlockingCache) cacheList.get(i);
            cache.clear();
        }
    }

    /**
     * Drops the contents of a named cache.
     */
    public void clear(final String name) throws CacheException {
        final BlockingCache blockingCache = (BlockingCache) getCache(name);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Clearing " + name);
        }
        blockingCache.clear();
    }

    /**
     * Returns the EHCache Cache Manager used in creating Blocking Caches.
     *
     * @return The cache manager
     */
    protected CacheManager getCacheManager() {
        return manager;
    }

    /**
     * Sets the EHCache Cache Manager used in creating blocking caches.
     *
     * @param mgr The new manager to use
     */
    protected void setCacheManager(CacheManager mgr) {
        manager = mgr;
    }

    /**
     * Builds the set of caches.
     * Returns a copy so that the monitor can be released.
     */
    private synchronized List getCaches() {
        final ArrayList blockingCaches = new ArrayList();
        blockingCaches.addAll(this.caches.values());
        return blockingCaches;
    }

}

