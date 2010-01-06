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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

import java.util.Collection;
import java.util.Properties;

/**
 * A CacheStorer is an interface used for write-through and write-behind caching to a underlying resource.
 * <p/>
 * A CacheStorer extends CacheLoader, adding write methods to it. Therefore CacheStorer's can also be used as CacheLoaders for
 * read-through caching.
 * <p/>
 * If configured for a cache, CacheStorer's methods will be called on a cache operation. A cache put will cause a CacheStorer store
 * and a cache remove will cause a storer delete.
 * <p>
 * Implementers should create an implementation which handles storing and deleting to an underlying resource.
 * </p>
 * <h4>Write-Through</h4>
 * In write-through mode, the cache operation will occur and the storer operation will occur before CacheEventListeners are notified. If
 * the store operation fails an exception will be thrown. This can result in a cache which is inconsistent with the underlying resource.
 * To avoid this, the cache and the underlying resource should be configured to participate in a transaction. In the event of a failure
 * a rollback can return all components to a consistent state.
 *
 * <h4>Write-Behind</h4>
 * In write-behind mode, writes are written to a write-behind queue. They are written by a separate execution thread in a configurable
 * way. When used with Terracotta Server Array, the queue is highly available. In addition any node in the cluster may perform the
 * write-behind operations.
 *
 * <h4>Creation and Configuration</h4>
 * CacheStorers can be created using the CacheStorerFactory.
 *
 * CacheStorers are extensively configurable. See {@link CacheStorerFactory} for a list of well-known properties
 * and their defaults. These properties are used to wire in the CacheStore on creation of a cache. 
 *
 *
 *
 * See the CacheStorer chapter in the documentation for more information on how to use storers.
 *
 * @author Greg Luck
 *
 */
public interface CacheStorer extends CacheLoader {

    /**
     * Store the specified value under the specified key in the underlying store.
     * This method is intended to support both key/value creation and value update for a specific key.
     *
     * @param element the element to be stored
     */
    void store(Element element) throws CacheException;

    /**
     * Store the specified Elements in the underlying store. This method is intended to support both insert and update.
     * If this operation fails (by throwing an exception) after a partial success,
     * the convention is that entries which have been stored successfully are to be removed from the specified mapEntries,
     * indicating that the store operation for the entries left in the map has failed or has not been attempted.
     *
     * @param elements the Elements to be stored
     */
    void storeAll(Collection<Element> elements) throws CacheException;


    /**
     * Remove the key and associated data from the store
     *
     * @param key the key whose mapping will be removed
     */
    void delete(Object key) throws CacheException;


    /**
     * Remove data and keys from the underlying store for the given collection of keys, if present. If this operation fails
     * (by throwing an exception) after a partial success, the convention is that keys which have been erased successfully
     * are to be removed from the specified keys, indicating that the erase operation for the keys left in the collection 
     * has failed or has not been attempted.
     *
     * @param keys the keys whose mappings will be removed
     */
    void deleteAll(Collection<Object> keys) throws CacheException;


    /**
     * Gets the properties used to create this storer. Well-known properties will be used to configure
     * the storer into the cache infrastrucutre on initialisation.
     * @return
     */
    Properties getProperties();

}
