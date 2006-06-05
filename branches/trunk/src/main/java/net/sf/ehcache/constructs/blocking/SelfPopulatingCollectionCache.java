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
import java.util.Collection;
import java.util.Collections;


/**
 * This cache is different to SelfPopulatingCache in that it is optimised for fast
 * refreshes of Collections. When it is refreshed, it updates the collection value in place, rather than creating a new one.
 * <p/>
 * It is suitable for values which:
 * <ul>
 * <li>are of type {@link Collection}, or a subclass
 * <li>are mutable
 * <li>do not need modify the collection value when it is returned from the cache
 * </ul>
 * To achieve improved thread safety values are made unmodifiable when they are gotten from this cache using
 * {@link java.util.Collections#unmodifiableCollection(java.util.Collection)}
 *
 * @author Greg Luck
 * @version $Id$
 */
public class SelfPopulatingCollectionCache extends SelfPopulatingCache {

    /**
     * Creates a SelfPopulatingCollectionCache.
     *
     * @param name    the name of the cache
     * @param factory must be an UpdatingCacheEntryFactory
     * @throws CacheException
     */
    public SelfPopulatingCollectionCache(final String name, final CacheEntryFactory factory) throws CacheException {
        super(name, factory);
        if (!(factory instanceof UpdatingCacheEntryFactory)) {
            throw new IllegalArgumentException("The factory must be an instance of UpdatingCacheEntryFactory");
        }
    }

    /**
     * Creates a SelfPopulatingCollectionCache.
     *
     * @param name    the name of the cache
     * @param mgr     the {@link net.sf.ehcache.CacheManager} to use to create the backing cache.
     * @param factory must be an UpdatingCacheEntryFactory
     * @throws CacheException
     */
    public SelfPopulatingCollectionCache(final String name, final CacheManager mgr, final CacheEntryFactory factory)
            throws CacheException {
        super(name, mgr, factory);
        if (!(factory instanceof UpdatingCacheEntryFactory)) {
            throw new IllegalArgumentException("The factory must be an instance of UpdatingCacheEntryFactory");
        }
    }

    /**
     * Adds an entry and unlocks it.
     * <p/>
     * Relies on a get always being called before a put
     *
     * @param key   the key for adding the value to the cache
     * @param value value must be both a {@link Collection} and {@link Serializable}
     * @throws IllegalArgumentException
     */
    public void put(Serializable key, Serializable value) throws IllegalArgumentException {
        if (!(value instanceof Collection)) {
            throw new IllegalArgumentException("value must be an instance of Collection");
        }
        super.put(key, (Serializable) value);
    }

    /**
     * Looks up an entry for a key, creating it if it is not found
     *
     * @param key
     * @return a {@link Serializable} {@link Collection}
     * @throws CacheException
     * @throws ClassCastException if the value is not a Collection
     */
    public Serializable get(Serializable key) throws BlockingCacheException, ClassCastException {
        Collection collection = (Collection) super.get(key);
        return (Serializable) Collections.unmodifiableCollection(collection);
    }
}
