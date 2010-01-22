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
package net.sf.ehcache.writer;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.util.Collection;

/**
 * A CacheWriter is an interface used for write-through and write-behind caching to a underlying resource.
 * <p/>
 * If configured for a cache, CacheWriter's methods will be called on a cache operation. A cache put will cause a CacheWriter write
 * and a cache remove will cause a writer delete.
 * <p>
 * Implementers should create an implementation which handles storing and deleting to an underlying resource.
 * </p>
 * <h4>Write-Through</h4>
 * In write-through mode, the cache operation will occur and the writer operation will occur before CacheEventListeners are notified. If
 * the write operation fails an exception will be thrown. This can result in a cache which is inconsistent with the underlying resource.
 * To avoid this, the cache and the underlying resource should be configured to participate in a transaction. In the event of a failure
 * a rollback can return all components to a consistent state.
 * <p/>
 * <h4>Write-Behind</h4>
 * In write-behind mode, writes are written to a write-behind queue. They are written by a separate execution thread in a configurable
 * way. When used with Terracotta Server Array, the queue is highly available. In addition any node in the cluster may perform the
 * write-behind operations.
 * <p/>
 * <h4>Creation and Configuration</h4>
 * CacheWriters can be created using the CacheWriterFactory.
 * <p/>
 * The manner upon which a CacheWriter is actually called is determined by the {@link net.sf.ehcache.config.CacheWriterConfiguration} that is set up for cache
 * that is using the CacheWriter.
 * <p/>
 * See the CacheWriter chapter in the documentation for more information on how to use writers.
 *
 * @author Greg Luck
 * @author Geert Bevin
 * @version $Id$
 */
public interface CacheWriter {

    /**
     * Creates a clone of this writer. This method will only be called by ehcache before a
     * cache is initialized.
     * <p/>
     * Implementations should throw CloneNotSupportedException if they do not support clone
     * but that will stop them from being used with defaultCache.
     *
     * @return a clone
     * @throws CloneNotSupportedException if the extension could not be cloned.
     */
    public CacheWriter clone(Ehcache cache) throws CloneNotSupportedException;


    /**
     * Notifies writer to initialise themselves.
     * <p/>
     * This method is called during the Cache's initialise method after it has changed it's
     * status to alive. Cache operations are legal in this method.
     *
     * @throws net.sf.ehcache.CacheException
     */
    void init();

    /**
     * Providers may be doing all sorts of exotic things and need to be able to clean up on
     * dispose.
     * <p/>
     * Cache operations are illegal when this method is called. The cache itself is partly
     * disposed when this method is called.
     */
    void dispose() throws net.sf.ehcache.CacheException;

    /**
     * Write the specified value under the specified key to the underlying store.
     * This method is intended to support both key/value creation and value update for a specific key.
     *
     * @param element the element to be written
     */
    void write(Element element) throws CacheException;

    /**
     * Write the specified Elements to the underlying store. This method is intended to support both insert and update.
     * If this operation fails (by throwing an exception) after a partial success,
     * the convention is that entries which have been written successfully are to be removed from the specified mapEntries,
     * indicating that the write operation for the entries left in the map has failed or has not been attempted.
     *
     * @param elements the Elements to be written
     */
    void writeAll(Collection<Element> elements) throws CacheException;


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

}
