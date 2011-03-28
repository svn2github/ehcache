/**
 *  Copyright 2003-2010 Terracotta, Inc.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * A cache loader that introduces a configurable delay when used
 *
 * @author Ludovic Orban
 * @version $Id$
 */
public class DelayingLoader extends CountingCacheLoader {

    private static final Logger LOG = LoggerFactory.getLogger(DelayingLoader.class.getName());

    private int loadCounter;
    private int loadAllCounter;
    private Random random = new Random();
    private String name = "ExceptionThrowingLoader";
    private long delayMillis;

    public DelayingLoader(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    /**
     * loads an object. Application writers should implement this
     * method to customize the loading of cache object. This method is called
     * by the caching service when the requested object is not in the cache.
     * <p/>
     *
     * @param key the key identifying the object being loaded
     * @return The object that is to be stored in the cache.
     */
    public Object load(Object key) throws CacheException {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            LOG.error("Interrupted");
        }
        throw new CacheException("Some exception with key " + key);
    }

    /**
     * loads multiple object. Application writers should implement this
     * method to customize the loading of cache object. This method is called
     * by the caching service when the requested object is not in the cache.
     * <p/>
     *
     * @param keys a Collection of keys identifying the objects to be loaded
     * @return A Map of objects that are to be stored in the cache.
     */

    public Map loadAll(Collection keys) throws CacheException {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            LOG.error("Interrupted");
        }
        throw new CacheException("Some exception with keys " + keys);
    }


    /**
     * Load using both a key and an argument.
     * <p/>
     * JCache will use the load(key) method where the argument is null.
     */
    public Object load(Object key, Object argument) throws CacheException {
        try {
            Thread.sleep(random.nextInt(3) + 1);
        } catch (InterruptedException e) {
            LOG.error("Interrupted");
        }
        throw new CacheException("Some exception with key " + key);
    }

    /**
     * Load using both a key and an argument.
     * <p/>
     * JCache will use the loadAll(key) method where the argument is null.
     */
    public Map loadAll(Collection keys, Object argument) throws CacheException {
        throw new CacheException("Some exception with key " + keys.toArray()[0]);
    }

}
