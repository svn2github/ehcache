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

package net.sf.ehcache.loader;

import net.sf.jsr107cache.CacheException;

import java.util.Random;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;


/**
 * A cache loader that throws exceptions when used
 *
 * Each load has a random delay to introduce some nice threading entropy.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class ExceptionThrowingLoader extends CountingCacheLoader {

    private static final Logger LOG = Logger.getLogger(CountingCacheLoader.class.getName());

    private int loadCounter;
    private int loadAllCounter;
    private Random random = new Random();
    private String name = "ExceptionThrowingLoader";

    /**
     * loads an object. Application writers should implement this
     * method to customize the loading of cache object. This method is called
     * by the caching service when the requested object is not in the cache.
     * <p/>
     *
     * @param key the key identifying the object being loaded
     * @return The object that is to be stored in the cache.
     * @throws net.sf.jsr107cache.CacheException
     *
     */
    public Object load(Object key) throws CacheException {
        try {
            Thread.sleep(random.nextInt(3) + 1);
        } catch (InterruptedException e) {
            LOG.severe("Interrupted");
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
     * @throws net.sf.jsr107cache.CacheException
     *
     */

    public Map loadAll(Collection keys) throws CacheException {
        Map map = new HashMap(keys.size());
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            Object key = iterator.next();
            try {
                Thread.sleep(random.nextInt(4));
            } catch (InterruptedException e) {
                LOG.severe("Interrupted");
            }
            map.put(key, new Integer(loadAllCounter++));
            throw new CacheException("Some exception with key " + key);
        }

        return map;
    }



    /**
     * Load using both a key and an argument.
     * <p/>
     * JCache will use the load(key) method where the argument is null.
     *
     * @param key
     * @param argument
     * @return
     * @throws net.sf.jsr107cache.CacheException
     *
     */
    public Object load(Object key, Object argument) throws CacheException {
        try {
            Thread.sleep(random.nextInt(3) + 1);
        } catch (InterruptedException e) {
            LOG.severe("Interrupted");
        }
        throw new CacheException("Some exception with key " + key);
    }

    /**
     * Load using both a key and an argument.
     * <p/>
     * JCache will use the loadAll(key) method where the argument is null.
     *
     * @param keys
     * @param argument
     * @return
     * @throws net.sf.jsr107cache.CacheException
     *
     */
    public Map loadAll(Collection keys, Object argument) throws CacheException {
        throw new CacheException("Some exception with key " + keys.toArray()[0]);
    }

}