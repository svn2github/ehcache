/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A cache loader that counts the number of things it has loaded, useful for testing.
 *
 *
 * Each load has a random delay to introduce some nice threading entropy.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CountingCacheLoader implements CacheLoader {

    private static final Log LOG = LogFactory.getLog(CountingCacheLoader.class.getName());

    private int loadCounter;
    private int loadAllCounter;
    private Random random = new Random();
    private String name = "CountingCacheLoader";

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
            LOG.error("Interrupted");
        }
        return new Integer(loadCounter++);
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
                LOG.error("Interrupted");
            }
            map.put(key, new Integer(loadAllCounter++));
        }
        return map;
    }


    /**
     * @return
     */
    public int getLoadCounter() {
        return loadCounter;
    }

    /**
     * @return
     */
    public int getLoadAllCounter() {
        return loadAllCounter;
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
            LOG.error("Interrupted");
        }
        return name + ":" + argument;
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
        return loadAll(keys);
    }

    /**
     * Gets the name of a CacheLoader
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     */
    public void setName(String name) {
        this.name = name;
    }


}
