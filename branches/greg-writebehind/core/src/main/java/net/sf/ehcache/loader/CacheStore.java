/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.loader;

import net.sf.ehcache.Element;

import java.util.Collection;

/**
 * A CacheStore is an interface used for write-through and write-behind caching.
 * <p/>
 * A CacheStore extends CacheLoader, adding write methods to it. Therefore CacheStore's can also be used as CacheLoaders for
 * read-through caching.
 *
 * @author Greg Luck
 */
public interface CacheStore extends CacheLoader {

    /**
     * Store the specified value under the specified key in the underlying store.
     * This method is intended to support both key/value creation and value update for a specific key.
     *
     * @param element the element to be stored
     */
    void store(Element element);

    /**
     * Store the specified Elements in the underlying store. This method is intended to support both insert and update.
     * If this operation fails (by throwing an exception) after a partial success,
     * the convention is that entries which have been stored successfully are to be removed from the specified mapEntries,
     * indicating that the store operation for the entries left in the map has failed or has not been attempted.
     *
     * @param elements the Elements to be stored
     */
    void storeAll(Collection<Element> elements);


    /**
     * Remove the key and associated data from the store
     *
     * @param key the key whose mapping will be removed
     */
    void delete(Object key);


    /**
     * Remove data and keys from the underlying store for the given collection of keys, if present. If this operation fails
     * (by throwing an exception) after a partial success, the convention is that keys which have been erased successfully
     * are to be removed from the specified keys, indicating that the erase operation for the keys left in the collection 
     * has failed or has not been attempted.
     *
     * @param keys the keys whose mappings will be removed
     */
    void deleteAll(Collection<Object> keys);


}
