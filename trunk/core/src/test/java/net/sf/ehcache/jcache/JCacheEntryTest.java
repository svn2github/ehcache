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
import java.util.Set;

/**
 * Tests for CacheEntry
 *
 * @author Greg Luck
 * @version $Id:JCacheEntryTest.java 318 2007-01-25 01:48:35Z gregluck $
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

    /**
     * This implementation does not have a notion of cost.
     */
    public void testCost() throws Exception {
        Cache cache = getTestCache();

        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        assertEquals(0, entry.getCost());
    }


    /**
     * Check the expiry time is correct.
     */
    public void testExpirationTime() throws Exception {

        Cache cache = getTestCache();
        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        cache.put(entry.getKey(), entry.getValue());
        CacheEntry retrievedEntry = cache.getCacheEntry(entry.getKey());

        //test expiry time s 1000ms after create time.
        assertTrue(retrievedEntry.getExpirationTime() > (System.currentTimeMillis() + 995));
        assertTrue(retrievedEntry.getExpirationTime() < (System.currentTimeMillis() + 1005));
    }

    /**
     * Check the expiry time is correct.
     */
    public void testCannotSet() throws Exception {

        Cache cache = getTestCache();
        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        cache.put(entry.getKey(), entry.getValue());
        CacheEntry retrievedEntry = cache.getCacheEntry(entry.getKey());

        try {
            retrievedEntry.setValue("test value");
        } catch (UnsupportedOperationException e) {
            assertEquals("Ehcache does not support modification of Elements. They are immutable.", e.getMessage());
        }

    }

    /**
     * Each get should cause a hit.
     */
    public void testHits() throws Exception {

        Cache cache = getTestCache();
        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        assertEquals(0, entry.getHits());

        cache.put(entry.getKey(), entry.getValue());
        CacheEntry retrievedEntry = cache.getCacheEntry(entry.getKey());

        assertEquals(1, retrievedEntry.getHits());

        cache.get(entry.getKey());
        retrievedEntry = cache.getCacheEntry(entry.getKey());

        assertEquals(3, retrievedEntry.getHits());

    }

    /**
     * Last access time should be 0 if not accessed and then the last get time.
     */
    public void testLastAccessTime() throws Exception {

        Cache cache = getTestCache();
        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        assertEquals(0, entry.getLastAccessTime());

        cache.put(entry.getKey(), entry.getValue());
        CacheEntry retrievedEntry = cache.getCacheEntry(entry.getKey());

        //test access is in the last 5ms
        assertTrue(retrievedEntry.getLastAccessTime() <= System.currentTimeMillis());

    }

    /**
     * 0 when first created. 0 when first put. 1 when replaced.
     */
    public void testLastUpdateTime() throws Exception {

        Cache cache = getTestCache();
        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        assertEquals(0, entry.getLastUpdateTime());

        cache.put(entry.getKey(), entry.getValue());
        //update it
        cache.put(entry.getKey(), entry.getValue());
        CacheEntry retrievedEntry = cache.getCacheEntry(entry.getKey());

        //test update is in the last 5ms
        assertTrue(retrievedEntry.getLastUpdateTime() > System.currentTimeMillis() - 10);
        assertTrue(retrievedEntry.getLastUpdateTime() <= System.currentTimeMillis());

    }

    /**
     * 0 when first created. 1 when first put. > 2 when replaced.
     */
    public void testVersion() throws Exception {

        Cache cache = getTestCache();
        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        assertEquals(1, entry.getVersion());


        cache.put(entry.getKey(), entry.getValue());
        //update it
        cache.put(entry.getKey(), entry.getValue());
        CacheEntry retrievedEntry = cache.getCacheEntry(entry.getKey());

        assertTrue(retrievedEntry.getVersion() >= 2);
    }


    /**
     * valid when first created. valid if not expired, invalid if expired.
     */
    public void testIsValid() throws Exception {

        Cache cache = getTestCache();
        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        assertEquals(true, entry.isValid());


        cache.put(entry.getKey(), entry.getValue());
        CacheEntry retrievedEntry = cache.getCacheEntry(entry.getKey());
        assertEquals(true, retrievedEntry.isValid());


        Thread.sleep(1020);
        assertEquals(false, retrievedEntry.isValid());

    }



    /**
     * Test getting the entry set
     */
    public void testEntrySet() throws Exception {

        JCache jcache = (JCache) getTestCache();
        CacheEntry entry = new JCacheEntry(new Element("key1", "value1"));
        assertEquals(true, entry.isValid());

        jcache.put(entry.getKey(), entry.getValue());

        //Entry Set works
        Set entries = jcache.entrySet();
        assertEquals(1, entries.size());

        //Entry Set is not live
        jcache.remove("key1");
        assertEquals(1, entries.size());


    }

}