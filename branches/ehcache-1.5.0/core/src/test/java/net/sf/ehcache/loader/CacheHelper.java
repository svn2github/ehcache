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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.AbstractCacheTest;

/**
 * Written for Dead-lock poc
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public final class CacheHelper {

    private static Map managers = new HashMap();


    /**
     * Utility class
     */
    private CacheHelper() {
        //noop
    }


    /**
     * Initialises the CacheHelper
     */
    public static void init() {
        managers = new HashMap();
    }

    /**
     *
     * @param cacheManagerUrl
     * @param cacheName
     * @return
     */
    public static Ehcache getCache(String cacheManagerUrl, String cacheName) {
        CacheManager mgr = (CacheManager) managers.get(cacheManagerUrl);

        if (mgr == null) {
            mgr = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + cacheManagerUrl);
            //Requires the config file to be in the classpath, which is not how we test specific configs in these tests
            //mgr = new CacheManager(Thread.currentThread().getContextClassLoader().getResourceAsStream(cacheManagerUrl));
            managers.put(cacheManagerUrl, mgr);
        }

        return mgr.getEhcache(cacheName);
    }

    /**
     *
     * @param cacheManagerUrl
     * @param cacheName
     * @param key
     * @return
     */
    public static Object get(String cacheManagerUrl, String cacheName, String key) {
        return get(cacheManagerUrl, cacheName, key, null);
    }

    /**
     *
     * @param cacheManagerUrl
     * @param cacheName
     * @param key
     * @param arguments
     * @return
     */
    public static Object get(String cacheManagerUrl, String cacheName, String key, Object arguments) {
        Ehcache cache = getCache(cacheManagerUrl, cacheName);
        Element elem = cache.getWithLoader(key, null, arguments);
        return elem.getObjectValue();
    }

    /**
     * 
     */
    public static void shutdown() {
        for (Iterator iter = managers.values().iterator(); iter.hasNext();) {
            ((CacheManager) iter.next()).shutdown();
        }
    }

}
