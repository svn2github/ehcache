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

package net.sf.jcache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.cache.CacheManager;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheEntry;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Element;
import net.sf.ehcache.jcache.JCacheEntry;

import java.util.Map;
import java.util.HashMap;

/**
 * Tests for CacheEntry
 *
 * @author Greg Luck
 * @version $Id$
 */
public class JCacheEntryTest extends AbstractCacheTest {
    private static final Log LOG = LogFactory.getLog(JCacheTest.class.getName());

    private CacheManager singletonManager;


    /**
     * setup test
     */
    protected void setUp() throws Exception {
        singletonManager = javax.cache.CacheManager.getInstance();
    }


    /**
     * teardown
     * limits to what we can do here under jsr107
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        getTest1Cache().clear();
        getTest2Cache().clear();
        getTest4Cache().clear();
    }


    /**
     * Gets the sample cache 1
     * <cache name="sampleCache1"
     * maxElementsInMemory="10000"
     * maxElementsOnDisk="1000"
     * eternal="false"
     * timeToIdleSeconds="360"
     * timeToLiveSeconds="1000"
     * overflowToDisk="true"
     * memoryStoreEvictionPolicy="LRU">
     * <cacheEventListenerFactory class="net.sf.ehcache.event.NullCacheEventListenerFactory"/>
     * </cache>
     */
    protected Cache getTest1Cache() throws CacheException {
        Cache cache = singletonManager.getCache("test1");
        if (cache == null) {
            //sampleCache1
            Map env = new HashMap();
            env.put("name", "test1");
            env.put("maxElementsInMemory", "10000");
            env.put("maxElementsOnDisk", "1000");
            env.put("memoryStoreEvictionPolicy", "LRU");
            env.put("overflowToDisk", "true");
            env.put("eternal", "false");
            env.put("timeToLiveSeconds", "1000");
            env.put("timeToIdleSeconds", "1000");
            env.put("diskPersistent", "false");
            env.put("diskExpiryThreadIntervalSeconds", "120");
            cache = singletonManager.getCacheFactory().createCache(env);
            singletonManager.registerCache("test1", cache);
        }
        return singletonManager.getCache("test1");
    }

    private Cache getTest2Cache() throws CacheException {
        Cache cache = singletonManager.getCache("test2");
        if (cache == null) {
            Map env = new HashMap();
            env.put("name", "test2");
            env.put("maxElementsInMemory", "1");
            env.put("overflowToDisk", "true");
            env.put("eternal", "false");
            env.put("timeToLiveSeconds", "1");
            env.put("timeToIdleSeconds", "0");
            cache = singletonManager.getCacheFactory().createCache(env);
            singletonManager.registerCache("test2", cache);
        }
        return singletonManager.getCache("test2");
    }

    private Cache getTest4Cache() throws CacheException {
        Cache cache = singletonManager.getCache("test4");
        if (cache == null) {
            Map env = new HashMap();
            env.put("name", "test4");
            env.put("maxElementsInMemory", "1000");
            env.put("overflowToDisk", "true");
            env.put("eternal", "true");
            env.put("timeToLiveSeconds", "0");
            env.put("timeToIdleSeconds", "0");
            cache = singletonManager.getCacheFactory().createCache(env);
            singletonManager.registerCache("test4", cache);
        }
        return singletonManager.getCache("test4");
    }


    /**
     * Test create and access times
     * jsr107 does not allow a CacheEntry to be put into a cache. So testing
     * recycling of elements is pointless.
     */
    public void testAccessTimes() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = getTest2Cache();

        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        long creationTime = entry.getCreationTime();
        assertTrue(entry.getCreationTime() > (System.currentTimeMillis() - 500));
        assertTrue(entry.getHits() == 0);
        assertTrue(entry.getLastAccessTime() == 0);

        cache.put(entry.getKey(), entry.getValue());

        entry = cache.getCacheEntry("key1");
        assertNotNull(entry);
        assertEquals(creationTime, entry.getCreationTime());
        assertTrue(entry.getLastAccessTime() != 0);
        assertTrue(entry.getHits() == 1);

    }


}