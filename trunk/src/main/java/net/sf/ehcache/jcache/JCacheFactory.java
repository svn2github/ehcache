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
package net.sf.ehcache.jcache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.util.PropertyUtil;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import java.util.Map;

/**
 * A CacheFactory implementation fo JCache
 * @author Greg Luck
 * @version $Id$
 */
public class JCacheFactory implements CacheFactory {


    private CacheManager cacheManager;


    /**
     * Constructor which references the creating CacheManager
     */
    public JCacheFactory(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * creates a new implementation specific Cache object using the env parameters.
     *
     * @param env properties for the following
     *            String name,
     *            int maxElementsInMemory,
     *            MemoryStoreEvictionPolicy memoryStoreEvictionPolicy,
     *            boolean overflowToDisk,
     *            String diskStorePath,
     *            boolean eternal,
     *            long timeToLiveSeconds,
     *            long timeToIdleSeconds,
     *            boolean diskPersistent,
     *            long diskExpiryThreadIntervalSeconds,
     * @return a newly created Cache
     * @throws CacheException
     */
    public Cache createCache(Map env) throws CacheException {


        String name = PropertyUtil.extractAndLogProperty("name", env);

        String maxElementsInMemoryString = PropertyUtil.extractAndLogProperty("maxElementsInMemory", env);
        int maxElementsInMemory = new Integer(maxElementsInMemoryString).intValue();

        String memoryStoreEvictionPolicyString = PropertyUtil.extractAndLogProperty("memoryStoreEvictionPolicy", env);
        MemoryStoreEvictionPolicy memoryStoreEvictionPolicy =
                MemoryStoreEvictionPolicy.fromString(memoryStoreEvictionPolicyString);

//
//        boolean overflowToDisk = PropertyUtil.extractAndLogProperty(name, env);
//        String diskStorePath = PropertyUtil.extractAndLogProperty(name, env);
//        boolean eternal = PropertyUtil.extractAndLogProperty(name, env);
//        long timeToLiveSeconds PropertyUtil.extractAndLogProperty(name, env);
//        long timeToIdleSeconds PropertyUtil.extractAndLogProperty(name, env);
//        boolean diskPersistent = PropertyUtil.extractAndLogProperty(name, env);
//        long diskExpiryThreadIntervalSeconds = PropertyUtil.extractAndLogProperty(name, env);
//        RegisteredEventListeners registeredEventListeners = null;
//
//
//        Ehcache cache = new net.sf.ehcache.Cache();

        return null;
    }
}
