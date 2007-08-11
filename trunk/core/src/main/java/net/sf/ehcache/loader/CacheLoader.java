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

import java.util.Map;
import java.util.Collection;

/**
 * Extends JCache CacheLoader with load methods that take an argument in addition to a key
 * @author Greg Luck
 * @version $Id$
 */
public interface CacheLoader extends net.sf.jsr107cache.CacheLoader {


    /**
     * Load using both a key and an argument.
     *
     * JCache will call through to the load(key) method, rather than this method, where the argument is null.
     * @param key the key to load the object for
     * @param argument can be anything that makes sense to the loader
     * @return the Object loaded
     *
     *
     * @throws CacheException
     */
    Object load(Object key, Object argument) throws CacheException;

    /**
     * Load using both a key and an argument.
     *
     * JCache will use the loadAll(key) method where the argument is null.
     * @param keys the keys to load objects for
     * @param argument can be anything that makes sense to the loader
     * @return a map of Objects keyed by the collection of keys passed in.
     * @throws CacheException
     */
    Map loadAll(Collection keys, Object argument) throws CacheException;

    /**
     * Gets the name of a CacheLoader
     * @return the name of this CacheLoader
     */
    String getName();
}
