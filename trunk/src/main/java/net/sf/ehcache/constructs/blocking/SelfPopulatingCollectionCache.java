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
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

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
     * @param factory must be an UpdatingCacheEntryFactory
     * @throws CacheException
     */
    public SelfPopulatingCollectionCache(final Ehcache cache, final CacheEntryFactory factory) throws CacheException {
        super(cache, factory);
        if (!(factory instanceof UpdatingCacheEntryFactory)) {
            throw new IllegalArgumentException("The factory must be an instance of UpdatingCacheEntryFactory");
        }
    }

     /**
     * Adds an entry and unlocks it.
     * <p/>
     * Relies on a get always being called before a put
     *
     * @param element an element with a value which must a {@link Collection}
     * @throws IllegalArgumentException
     */
    public void put(Element element) throws IllegalArgumentException {
         if (element == null) {
             return;
         }
        if (!(element.getObjectValue() instanceof Collection)) {
            throw new IllegalArgumentException("value must be an instance of Collection");
        }
        super.put(element);
    }

    /**
     * Looks up an entry for a key, creating it if it is not found
     *
     * @param key
     * @return a new Element containg an unmodifiable {@link Collection}
     * @throws CacheException
     * @throws ClassCastException if the value is not a Collection
     */
    public Element get(Object key) throws LockTimeoutException, ClassCastException {
        Element element = super.get(key);
        if (element == null) {
            return null;
        }
        Collection collection = (Collection) element.getObjectValue();
        return new Element(element.getKey(), Collections.unmodifiableCollection(collection));
    }
}
