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

package net.sf.ehcache.constructs.blocking;


import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.Mutex;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A {@link net.sf.ehcache.Cache} backed cache that creates entries on demand.
 * <p/>
 * Clients of the cache simply call it without needing knowledge of whether
 * the entry exists in the cache, or whether it needs updating before use.
 * <p/>
 * <p/>
 * Thread safety depends on the factory being used. The UpdatingCacheEntryFactory should be made
 * thread safe. In addition users of returned values should not modify their contents.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class UpdatingSelfPopulatingCache extends SelfPopulatingCache {

    private static final Logger LOG = Logger.getLogger(UpdatingSelfPopulatingCache.class.getName());

    /**
     * Creates a SelfPopulatingCache.
     */
    public UpdatingSelfPopulatingCache(Ehcache cache, final UpdatingCacheEntryFactory factory)
            throws CacheException {
        super(cache, factory);
    }

    /**
     * Looks up an object.
     * <p/>
     * If null, it creates it. If not null, it updates it. For performance this method should only be
     * used with {@link UpdatingCacheEntryFactory}'s
     * <p/>
     * It is expected that
     * gets, which update as part of the get, might take considerable time. Access to the cache cannot be blocked
     * while that is happening. This method is therefore not synchronized. Mutexes are used for thread safety based on key
     *
     * @param key
     * @return a value
     * @throws net.sf.ehcache.CacheException
     */
    public Element get(final Object key) throws LockTimeoutException {

        try {

            Ehcache backingCache = getCache();
            Element element = backingCache.get(key);

            if (element == null) {
                element = super.get(key);
            } else {
                Mutex lock = stripedMutex.getMutexForKey(key);
                try {
                    lock.acquire();
                    update(key);
                } finally {
                    lock.release();
                }
            }
            return element;
        } catch (final Throwable throwable) {
            // Could not fetch - Ditch the entry from the cache and rethrow
            put(new Element(key, null));
            throw new LockTimeoutException("Could not update object for cache entry with key \"" + key + "\".", throwable);
        }
    }

    /**
     * Element can never be null. Add a null guard just in case.
     * @param key
     */
    protected void update(final Object key) {
        try {
            Ehcache backingCache = getCache();
            final Element element = backingCache.getQuiet(key);

            if (element == null) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(getName() + ": entry with key " + key + " has been removed - skipping it");
                }
                return;
            }

            refreshElement(element, backingCache);
        } catch (final Exception e) {
            // Collect the exception and keep going.
            // Throw the exception once all the entries have been refreshed
            // If the refresh fails, keep the old element. It will simply become staler.
            LOG.log(Level.WARNING, getName() + "Could not refresh element " + key, e);
        }
    }

    /**
     * This method should not be used. Because elements are always updated before they are
     * returned, it makes no sense to refresh this cache.
     */
    public void refresh() throws CacheException {
        throw new CacheException("UpdatingSelfPopulatingCache objects should not be refreshed.");
    }

}
