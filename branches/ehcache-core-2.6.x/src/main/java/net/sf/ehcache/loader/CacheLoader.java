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

package net.sf.ehcache.loader;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;

import java.util.Collection;
import java.util.Map;

/**
 * Extends JCache CacheLoader with load methods that take an argument in addition to a key
 *
 * This interface has exactly the same interface as in the JCache module.
 *
 * @author Greg Luck
 * @version $Id$
 */
public interface CacheLoader {


     /**
     * loads an object. Application writers should implement this
     * method to customize the loading of cache object. This method is called
     * by the caching service when the requested object is not in the cache.
     * <P>
     *
     * @param key the key identifying the object being loaded
     *
     * @return The object that is to be stored in the cache.
     * @throws CacheException
     *
     */
    public Object load(Object key) throws CacheException;

    /**
     * loads multiple object. Application writers should implement this
     * method to customize the loading of cache object. This method is called
     * by the caching service when the requested object is not in the cache.
     * <P>
     *
     * @param keys a Collection of keys identifying the objects to be loaded
     *
     * @return A Map of objects that are to be stored in the cache.
     * @throws CacheException
     *
     */
    public Map loadAll(Collection keys);


    /**
     * Load using both a key and an argument.
     * <p/>
     * JCache will call through to the load(key) method, rather than this method, where the argument is null.
     *
     * @param key      the key to load the object for
     * @param argument can be anything that makes sense to the loader
     * @return the Object loaded
     * @throws CacheException
     */
    Object load(Object key, Object argument);

    /**
     * Load using both a key and an argument.
     * <p/>
     * JCache will use the loadAll(key) method where the argument is null.
     *
     * @param keys     the keys to load objects for
     * @param argument can be anything that makes sense to the loader
     * @return a map of Objects keyed by the collection of keys passed in.
     * @throws CacheException
     */
    Map loadAll(Collection keys, Object argument);

    /**
     * Gets the name of a CacheLoader
     *
     * @return the name of this CacheLoader
     */
    String getName();

    /**
     * Creates a clone of this extension. This method will only be called by ehcache before a
     * cache is initialized.
     * <p/>
     * Implementations should throw CloneNotSupportedException if they do not support clone
     * but that will stop them from being used with defaultCache.
     *
     * @return a clone
     * @throws CloneNotSupportedException if the extension could not be cloned.
     */
    public CacheLoader clone(Ehcache cache) throws CloneNotSupportedException;


    /**
     * Notifies providers to initialise themselves.
     * <p/>
     * This method is called during the Cache's initialise method after it has changed it's
     * status to alive. Cache operations are legal in this method.
     *
     * @throws net.sf.ehcache.CacheException
     * @see net.sf.ehcache.Ehcache#registerCacheLoader(net.sf.ehcache.loader.CacheLoader)
     */
    void init();

    /**
     * CacheLoader instances may be doing all sorts of exotic things and need to be able to clean up
     * on dispose. This method will be invoked when {@link net.sf.ehcache.Cache#dispose() Cache.dispose()} is invoked
     * if this CacheLoader is registered with the cache at disposal time, allowing for any necessary cleanup.
     * <p/>
     * No operations may be performed on the cache this CacheLoader is registered with. The
     * cache itself is partly disposed when this method is called, and should not be accessed.
     * <p/>
     *
     * @throws net.sf.ehcache.CacheException
     */
    void dispose() throws net.sf.ehcache.CacheException;


    /**
     * @return the status of the extension
     */
    public Status getStatus();
}
