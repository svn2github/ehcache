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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jsr107cache.CacheManager;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheEntry;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Element;

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


    /**
     * teardown
     * limits to what we can do here under jsr107
     */
    protected void tearDown() throws Exception {
        getTestCache().clear();
    }

    private Cache getTestCache() throws CacheException {
        Cache cache = CacheManager.getInstance().getCache("testCacheEntry");
        if (cache == null) {
            Map env = new HashMap();
            env.put("name", "testCacheEntry");
            env.put("maxElementsInMemory", "1");
            env.put("overflowToDisk", "true");
            env.put("eternal", "false");
            env.put("timeToLiveSeconds", "1");
            env.put("timeToIdleSeconds", "0");
            cache = CacheManager.getInstance().getCacheFactory().createCache(env);
            CacheManager.getInstance().registerCache("testCacheEntry", cache);
        }
        return CacheManager.getInstance().getCache("testCacheEntry");
    }



    /**
     * Test create and access times
     * jsr107 does not allow a CacheEntry to be put into a cache. So testing
     * recycling of elements is pointless.
     */
    public void testAccessTimes() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = getTestCache();

        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        long creationTime = entry.getCreationTime();
        assertTrue(entry.getCreationTime() > (System.currentTimeMillis() - 500));
        assertTrue(entry.getHits() == 0);
        assertTrue(entry.getLastAccessTime() == 0);

        cache.put(entry.getKey(), entry.getValue());

        entry = cache.getCacheEntry("key1");
        long entryCreationTime = entry.getCreationTime();
        assertNotNull(entry);
        cache.get("key1");
        cache.get("key1");
        cache.get("key1");
        //check creation time does not change
        assertEquals(entryCreationTime, entry.getCreationTime());
        assertTrue(entry.getLastAccessTime() != 0);
        assertTrue(entry.getHits() == 4);

    }


}