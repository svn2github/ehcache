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

package net.sf.ehcache.management;

import net.sf.ehcache.Status;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class CacheManager implements CacheManagerMBean {

    private net.sf.ehcache.CacheManager cacheManager;

    /**
     * Create a management CacheManager
     * @param cacheManager
     */
    public CacheManager(net.sf.ehcache.CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Gets the status attribute of the Ehcache
     *
     * @return The status value from the Status enum class
     */
    public Status getStatus() {
        return cacheManager.getStatus();
    }

    /**
     * Shuts down the CacheManager.
    * <p/>
    * If the shutdown occurs on the singleton, then the singleton is removed, so that if a singleton access method
    * is called, a new singleton will be created.
    */
   public void shutdown() {
        cacheManager.shutdown();
    }

    /**
     * Clears  the contents of all caches in the CacheManager, but without
     * removing any caches.
     * <p/>
     * This method is not synchronized. It only guarantees to clear those elements in a cache
     * at the time that the {@link net.sf.ehcache.Ehcache#removeAll()} mehod  on each cache is called.
     */
    public void clearAll() {
        cacheManager.clearAll();
    }

    /**
     * Returns a JMX Cache bean
     */
    public Cache getCache(String name) {
        return new Cache(cacheManager.getCache(name));
    }

    /**
     * Gets the cache names managed by the CacheManager
     */
    public String[] getCacheNames() throws IllegalStateException {
        return cacheManager.getCacheNames();
    }

    /**
     * Gets a list of caches in this CacheManager
     *
     * @return a list of JMX Cache objects
     */
    public List getCaches() {
        List cacheList = new ArrayList();
        String[] caches = getCacheNames();
        for (int i = 0; i < caches.length; i++) {
            String cacheName = caches[i];
            Cache cache = getCache(cacheName);
            cacheList.add(cache);
        }
        return cacheList;
    }
}
