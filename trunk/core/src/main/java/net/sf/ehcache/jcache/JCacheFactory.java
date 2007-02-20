/**
 *  Copyright 2003-2007 Greg Luck
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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.util.ClassLoaderUtil;
import net.sf.ehcache.util.PropertyUtil;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * A CacheFactory implementation for JCache.
 *
 * This factory uses ehcache in singleton CacheManager mode i.e. one per classloader.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class JCacheFactory implements CacheFactory {


    private static final Log LOG = LogFactory.getLog(JCacheFactory.class.getName());

    /**
     * Creates a new implementation specific Cache object using the environment parameters.
     *
     * The created cache is not accessible from the JCache CacheManager until it has been registered with the manager.
     *
     * Create caches are registered with a singleton ehcache CacheManager.
     *
     * @param environment String values for the following properties:
     *            String name,
     *            int maxElementsInMemory,
     *            MemoryStoreEvictionPolicy memoryStoreEvictionPolicy (one of LFU, LRU or FIFO)
     *            boolean overflowToDisk,
     *            boolean eternal,
     *            long timeToLiveSeconds,
     *            long timeToIdleSeconds,
     *            boolean diskPersistent,
     *            long diskExpiryThreadIntervalSeconds,
     *            int maxElementsOnDisk,
     *            String cacheLoaderFactoryClassName
     *
     *
     * Note that the following cannot be set:
     * <ol>
     * <li>diskStorePath - this is set on the CacheManager and ignored here
     * <li>RegisteredEventListeners - register any of these after cache creation
     * <li>BootstrapCacheLoader - not supported here
     * </ol>
     * @return a newly created JCache registered in the singleton CacheManager
     * @throws CacheException
     */
    public Cache createCache(Map environment) throws CacheException {


        CacheLoader cacheLoader = null;
        Ehcache cache = null;
        try {
            String name = PropertyUtil.extractAndLogProperty("name", environment);

            String maxElementsInMemoryString = PropertyUtil.extractAndLogProperty("maxElementsInMemory", environment);
            int maxElementsInMemory = Integer.parseInt(maxElementsInMemoryString);

            String memoryStoreEvictionPolicyString = PropertyUtil.extractAndLogProperty("memoryStoreEvictionPolicy", environment);
            MemoryStoreEvictionPolicy memoryStoreEvictionPolicy =
                    MemoryStoreEvictionPolicy.fromString(memoryStoreEvictionPolicyString);

            String overflowToDiskString = PropertyUtil.extractAndLogProperty("overflowToDisk", environment);
            boolean overflowToDisk = PropertyUtil.parseBoolean(overflowToDiskString);

            String eternalString = PropertyUtil.extractAndLogProperty("eternal", environment);
            boolean eternal = PropertyUtil.parseBoolean(eternalString);

            String timeToLiveSecondsString = PropertyUtil.extractAndLogProperty("timeToLiveSeconds", environment);
            long timeToLiveSeconds = Long.parseLong(timeToLiveSecondsString);

            String timeToIdleSecondsString = PropertyUtil.extractAndLogProperty("timeToIdleSeconds", environment);
            long timeToIdleSeconds = Long.parseLong(timeToIdleSecondsString);

            String diskPersistentString = PropertyUtil.extractAndLogProperty("diskPersistentSeconds", environment);
            boolean diskPersistent = PropertyUtil.parseBoolean(diskPersistentString);

            long diskExpiryThreadIntervalSeconds = 0;
            String diskExpiryThreadIntervalSecondsString =
                    PropertyUtil.extractAndLogProperty("diskExpiryThreadIntervalSeconds", environment);
            if (diskExpiryThreadIntervalSecondsString != null) {
                diskExpiryThreadIntervalSeconds = Long.parseLong(diskExpiryThreadIntervalSecondsString);
            }

            int maxElementsOnDisk = 0;
            String maxElementsOnDiskString =
                            PropertyUtil.extractAndLogProperty("maxElementsOnDisk", environment);
            if (maxElementsOnDiskString != null) {
                maxElementsOnDisk = Integer.parseInt(maxElementsOnDiskString);
            }

            cacheLoader = null;
            String cacheLoaderFactoryClassName =
                            PropertyUtil.extractAndLogProperty("cacheLoaderFactoryClassName", environment);
            if (cacheLoaderFactoryClassName == null) {
                LOG.debug("cacheLoaderFactoryClassName not configured. Skipping...");
            } else {
                CacheLoaderFactory factory = (CacheLoaderFactory)
                        ClassLoaderUtil.createNewInstance(cacheLoaderFactoryClassName);
                cacheLoader = factory.createCacheLoader(environment);
            }


            cache = new net.sf.ehcache.Cache(name, maxElementsInMemory, memoryStoreEvictionPolicy,
                    overflowToDisk, null, eternal,
                    timeToLiveSeconds, timeToIdleSeconds, diskPersistent, diskExpiryThreadIntervalSeconds,
                    null, null, maxElementsOnDisk);

            net.sf.ehcache.CacheManager.getInstance().addCache(cache);
        } catch (net.sf.ehcache.CacheException e) {
            throw new CacheException(e.getMessage(), e);
        }


        return new JCache(cache, cacheLoader);

    }
}
