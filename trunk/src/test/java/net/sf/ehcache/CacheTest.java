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

package net.sf.ehcache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


/**
 * Tests for a Cache
 *
 * @author Greg Luck, Claus Ibsen
 * @version $Id$
 */
public class CacheTest extends AbstractCacheTest {
    private static final Log LOG = LogFactory.getLog(CacheTest.class.getName());


    /**
     * teardown
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Gets the sample cache 1
     */
    protected Ehcache getSampleCache1() {
        Cache cache = manager.getCache("sampleCache1");
        return cache;
    }

    /**
     * Creates a cache
     *
     * @return
     */
    protected Ehcache createTestCache() {
        Cache cache = new Cache("test4", 1000, true, true, 0, 0);
        manager.addCache(cache);
        return cache;
    }

    /**
     * Checks we cannot use a cache after shutdown
     */
    public void testUseCacheAfterManagerShutdown() throws CacheException {
        Ehcache cache = getSampleCache1();
        manager.shutdown();
        Element element = new Element("key", "value");
        try {
            cache.getSize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
        try {
            cache.put(element);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
        try {
            cache.get("key");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive.", e.getMessage());
        }
        if (cache instanceof Cache) {
            Cache castCache = (Cache) cache;
        //ok to get stats
            castCache.getHitCount();
            castCache.getMemoryStoreHitCount();
            castCache.getDiskStoreHitCount();
            castCache.getMissCountExpired();
            castCache.getMissCountNotFound();
        }

    }


    /**
     * Checks we cannot use a cache outside the manager
     */
    public void testUseCacheOutsideManager() throws CacheException {
        //Not put into manager.
        Cache cache = new Cache("testCache", 1, true, false, 5, 2);
        Element element = new Element("key", "value");
        try {
            cache.getSize();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
        try {
            cache.put(element);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
        try {
            cache.get("key");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive.", e.getMessage());
        }
        //ok to get stats
        cache.getHitCount();
        cache.getMemoryStoreHitCount();
        cache.getDiskStoreHitCount();
        cache.getMissCountExpired();
        cache.getMissCountNotFound();
    }

    /**
     * Checks when and how we can set the cache name.
     */
    public void testSetCacheName() throws CacheException {
        //Not put into manager.
        Ehcache cache = new Cache("testCache", 1, true, false, 5, 2);

        try {
            cache.setName(null);
            fail();
        } catch (IllegalArgumentException e) {
            //expected
        }

        try {
            cache.setName("illegal/name");
            fail();
        } catch (IllegalArgumentException e) {
            //expected
        }

        manager.addCache(cache);
        try {
            cache.setName("trying_to_change_name_after_initialised");
            fail();
        } catch (IllegalStateException e) {
            //expected
        }
    }


    /**
     * Test using a cache which has been removed and replaced.
     */
    public void testStaleCacheReference() throws CacheException {
        manager.addCache("test");
        Ehcache cache = manager.getCache("test");
        assertNotNull(cache);
        cache.put(new Element("key1", "value1"));

        assertEquals("value1", cache.get("key1").getObjectValue());
        manager.removeCache("test");
        manager.addCache("test");

        try {
            cache.get("key1");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The test Cache is not alive.", e.getMessage());
        }
    }

    /**
     * Tests getting the cache name
     *
     * @throws Exception
     */
    public void testCacheName() throws Exception {
        manager.addCache("test");
        Ehcache cache = manager.getCache("test");
        assertEquals("test", cache.getName());
        assertEquals(Status.STATUS_ALIVE, cache.getStatus());
    }


    /**
     * Tests getting the cache name
     *
     * @throws Exception
     */
    public void testCacheWithNoIdle() throws Exception {
        Ehcache cache = manager.getCache("sampleCacheNoIdle");
        assertEquals("sampleCacheNoIdle", cache.getName());
        assertEquals(Status.STATUS_ALIVE, cache.getStatus());
        assertEquals(0, cache.getTimeToIdleSeconds());
    }

    /**
     * Test expiry based on time to live
     * <cache name="sampleCacheNoIdle"
     * maxElementsInMemory="1000"
     * eternal="false"
     * timeToLiveSeconds="5"
     * overflowToDisk="false"
     * />
     */
    public void testExpiryBasedOnTimeToLiveWhenNoIdle() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache cache = manager.getCache("sampleCacheNoIdle");
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to idle. Should not idle out because not specified
        Thread.sleep(2000);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to live.
        Thread.sleep(5001);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }


    /**
     * Test expiry based on time to live for a cache with config
     * <cache name="sampleCacheNoIdle"
     * maxElementsInMemory="1000"
     * eternal="false"
     * timeToLiveSeconds="5"
     * overflowToDisk="false"
     * />
     * <p/>
     * where an Elment override is set on TTL
     */
    public void testExpiryBasedOnTimeToLiveWhenNoIdleElementOverride() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache cache = manager.getCache("sampleCacheNoIdle");
        Element element1 = new Element("key1", "value1");
        element1.setTimeToLive(3);
        cache.put(element1);

        Element element2 = new Element("key2", "value1");
        element2.setTimeToLive(3);
        cache.put(element2);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to idle. Should not idle out because not specified
        Thread.sleep(2000);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to live.
        Thread.sleep(3001);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    /**
     * Test expiry based on time to live for a cache with config
     * <cache name="sampleCacheNoIdle"
     * maxElementsInMemory="1000"
     * eternal="false"
     * timeToLiveSeconds="5"
     * overflowToDisk="false"
     * />
     * <p/>
     * where an Elment override is set on TTL
     */
    public void testExpiryBasedOnTimeToIdleElementOverride() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache cache = manager.getCache("sampleCacheNoIdle");
        Element element1 = new Element("key1", "value1");
        element1.setTimeToIdle(1);
        cache.put(element1);

        Element element2 = new Element("key2", "value1");
        element2.setTimeToIdle(1);
        cache.put(element2);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to idle. Should not idle out because not specified
        Thread.sleep(1001);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));

    }


    /**
     * Test expiry based on time to live for a cache with config
     * <cache name="sampleCacheNoIdle"
     * maxElementsInMemory="1000"
     * eternal="false"
     * timeToLiveSeconds="5"
     * overflowToDisk="false"
     * />
     * <p/>
     * where an Elment override is set on TTL
     */
    public void testExpiryBasedEternalElementOverride() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache cache = manager.getCache("sampleCacheNoIdle");
        Element element1 = new Element("key1", "value1");
        element1.setEternal(true);
        cache.put(element1);

        Element element2 = new Element("key2", "value1");
        element2.setEternal(true);
        cache.put(element2);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        Thread.sleep(5001);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

    }


    /**
     * Test expiry based on time to live. Even though eternal is false, because there are no
     * expiry or idle times, it is eternal.
     * <cache name="sampleCacheNotEternalButNoIdleOrExpiry"
     * maxElementsInMemory="1000"
     * eternal="false"
     * overflowToDisk="false"
     * />
     */
    public void testExpirySampleCacheNotEternalButNoIdleOrExpiry() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache cache = manager.getCache("sampleCacheNotEternalButNoIdleOrExpiry");
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to idle. Should not idle out because not specified
        Thread.sleep(2000);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to live.
        Thread.sleep(5001);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }


    /**
     * Test overflow to disk = false
     */
    public void testNoOverflowToDisk() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, false, true, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        assertNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }


    /**
     * Performance tests for a range of Memory Store - Disk Store combinations.
     * <p/>
     * This demonstrates that a memory only store is approximately an order of magnitude
     * faster than a disk only store.
     * <p/>
     * It also shows that double the performance of a Disk Only store can be obtained
     * with a maximum memory size of only 1. Accordingly a Cache created without a
     * maximum memory size of less than 1 will issue a warning.
     * <p/>
     * Threading changes were made in v1.41 of DiskStore. The before and after numbers are shown.
     */
    public void testProportionMemoryAndDiskPerformance() throws Exception {
        StopWatch stopWatch = new StopWatch();
        long time = 0;

        //Memory only Typical 192ms
        Cache memoryOnlyCache = new Cache("testMemoryOnly", 5000, false, false, 5, 2);
        manager.addCache(memoryOnlyCache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = new Integer(i);
            memoryOnlyCache.put(new Element(new Integer(i), "value"));
            memoryOnlyCache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for MemoryStore: " + time);
        assertTrue("Time to put and get 5000 entries into MemoryStore", time < 300);

        //Set size so that all elements overflow to disk.
        // 1245 ms v1.38 DiskStore
        // 273 ms v1.42 DiskStore
        Cache diskOnlyCache = new Cache("testDiskOnly", 0, true, false, 5, 2);
        manager.addCache(diskOnlyCache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = new Integer(i);
            diskOnlyCache.put(new Element(key, "value"));
            diskOnlyCache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for DiskStore: " + time);
        assertTrue("Time to put and get 5000 entries into DiskStore was less than 2 sec", time < 2000);

        // 1 Memory, 999 Disk
        // 591 ms v1.38 DiskStore
        // 56 ms v1.42 DiskStore
        Cache m1d999Cache = new Cache("m1d999Cache", 1, true, false, 5, 2);
        manager.addCache(m1d999Cache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = new Integer(i);
            m1d999Cache.put(new Element(key, "value"));
            m1d999Cache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for m1d999Cache: " + time);
        assertTrue("Time to put and get 5000 entries into m1d999Cache", time < 2000);

        // 500 Memory, 500 Disk
        // 669 ms v1.38 DiskStore
        // 47 ms v1.42 DiskStore
        Cache m500d500Cache = new Cache("m500d500Cache", 500, true, false, 5, 2);
        manager.addCache(m500d500Cache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = new Integer(i);
            m500d500Cache.put(new Element(key, "value"));
            m500d500Cache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for m500d500Cache: " + time);
        assertTrue("Time to put and get 5000 entries into m500d500Cache", time < 2000);

    }

    /**
     * Test Caches with persistent stores dispose properly. Tests:
     * <ol>
     * <li>No exceptions are thrown on dispose
     * <li>You cannot re add a cache after it has been disposed and removed
     * <li>You can create a new cache with the same name
     * </ol>
     */
    public void testCreateAddDisposeAdd() throws CacheException {
        Cache cache = new Cache("test2", 1, true, true, 0, 0, true, 120);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        int sizeFromGetSize = cache.getSize();
        int sizeFromKeys = cache.getKeys().size();
        assertEquals(sizeFromGetSize, sizeFromKeys);
        assertEquals(2, cache.getSize());
        //package protected method, only available to tests. Called by teardown
        cache.dispose();
        manager.removeCache("test2");


        try {
            manager.addCache(cache);
            fail();
        } catch (CacheException e) {
            //expected
        }

        //Add a new cache with the same name as the disposed one.
        Cache cache2 = new Cache("test2", 1, true, true, 0, 0, true, 120);
        manager.addCache(cache2);
        Ehcache cacheFromManager = manager.getCache("test2");
        assertTrue(cacheFromManager.getStatus().equals(Status.STATUS_ALIVE));

    }

    /**
     * Test expiry based on time to live
     */
    public void testExpiryBasedOnTimeToLive() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 3, 0);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        Thread.sleep(1001);
        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        Thread.sleep(1001);
        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        Thread.sleep(1001);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }


    /**
     * Tests that a cache created from defaults will expire as per
     * the default expiry policy.
     *
     * @throws Exception
     */
    public void testExpiryBasedOnTimeToLiveForDefault() throws Exception {
        String name = "ThisIsACacheWhichIsNotConfiguredAndWillThereforeUseDefaults";
        Ehcache cache = null;
        CacheManager manager = CacheManager.getInstance();
        cache = manager.getCache(name);
        if (cache == null) {
            LOG.warn("Could not find configuration for " + name
                    + ". Configuring using the defaultCache settings.");
            manager.addCache(name);
            cache = manager.getCache(name);
        }

        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        Thread.sleep(10010);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));


    }


    /**
     * Test expiry based on time to live.
     * <p/>
     * Elements are put quietly back into the cache after being cloned.
     * The elements should expire as if the putQuiet had not happened.
     */
    public void testExpiryBasedOnTimeToLiveAfterPutQuiet() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        Element element1 = cache.get("key1");
        Element element2 = cache.get("key2");
        assertNotNull(element1);
        assertNotNull(element2);

        //Test time to live
        Thread.sleep(2010);
        //Should not affect age
        cache.putQuiet((Element) element2.clone());
        cache.putQuiet((Element) element2.clone());
        Thread.sleep(3010);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    /**
     * Test expiry based on time to live
     */
    public void testNoIdleOrExpiryBasedOnTimeToLiveForEternal() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, true, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Check that we did not idle out
        Thread.sleep(2010);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Check that we did not expire out
        Thread.sleep(3010);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }

    /**
     * Test expiry based on time to idle.
     */
    public void testExpiryBasedOnTimeToIdle() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 6, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to idle
        Element element1 = cache.get("key1");
        Element element2 = cache.get("key2");
        assertNotNull(element1);
        assertNotNull(element2);
        Thread.sleep(2010);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));

        //Test effect of get
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        Thread.sleep(1010);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        Thread.sleep(2010);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }


    /**
     * Test expiry based on time to idle.
     */
    public void testExpiryBasedOnTimeToIdleAfterPutQuiet() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 3);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to idle
        Element element1 = cache.get("key1");
        Element element2 = cache.get("key2");
        assertNotNull(element1);
        assertNotNull(element2);

        //Now, getQuiet and check still times out 2 seconds after last get
        Thread.sleep(1010);
        element1 = cache.getQuiet("key1");
        element2 = cache.getQuiet("key2");
        Thread.sleep(2010);
        assertNull(cache.getQuiet("key1"));
        assertNull(cache.getQuiet("key2"));

        //Now put back in with putQuiet. Should be immediately expired
        cache.putQuiet((Element) element1.clone());
        cache.putQuiet((Element) element2.clone());
        assertNull(cache.get("key1"));
        element2 = cache.get("key2");
        assertNull(element2);
    }

    /**
     * Test element statistics, including get and getQuiet
     * eternal="false"
     * timeToIdleSeconds="5"
     * timeToLiveSeconds="10"
     * overflowToDisk="true"
     */
    public void testElementStatistics() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        Element element1 = cache.get("key1");
        assertEquals("Should be one", 1, element1.getHitCount());
        element1 = cache.getQuiet("key1");
        assertEquals("Should be one", 1, element1.getHitCount());
        element1 = cache.get("key1");
        assertEquals("Should be two", 2, element1.getHitCount());
    }

    /**
     * Test cache statistics, including get and getQuiet
     */
    public void testCacheStatistics() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        Element element1 = cache.get("key1");
        assertEquals("Should be one", 1, element1.getHitCount());
        assertEquals("Should be one", 1, cache.getHitCount());
        element1 = cache.getQuiet("key1");
        assertEquals("Should be one", 1, element1.getHitCount());
        assertEquals("Should be one", 1, cache.getHitCount());
        element1 = cache.get("key1");
        assertEquals("Should be two", 2, element1.getHitCount());
        assertEquals("Should be two", 2, cache.getHitCount());


        assertEquals("Should be 0", 0, cache.getMissCountNotFound());
        cache.get("doesnotexist");
        assertEquals("Should be 1", 1, cache.getMissCountNotFound());


    }

    /**
     * Checks that getQuiet works how we expect it to
     *
     * @throws Exception
     */
    public void testGetQuietAndPutQuiet() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        Element element1 = cache.get("key1");
        long lastAccessedElement1 = element1.getLastAccessTime();
        long hitCountElement1 = element1.getHitCount();
        assertEquals("Should be two", 1, element1.getHitCount());

        element1 = cache.getQuiet("key1");
        element1 = cache.getQuiet("key1");
        Element clonedElement1 = (Element) element1.clone();
        cache.putQuiet(clonedElement1);
        element1 = cache.getQuiet("key1");
        assertEquals("last access time should be unchanged",
                lastAccessedElement1, element1.getLastAccessTime());
        assertEquals("hit count should be unchanged",
                hitCountElement1, element1.getHitCount());
        element1 = cache.get("key1");
        assertEquals("Should be two", 2, element1.getHitCount());
    }

    /**
     * Test size with put and remove.
     * <p/>
     * It checks that size makes sense, and also that getKeys.size() matches getSize()
     */
    public void testSizeWithPutAndRemove() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test2", 1, true, true, 0, 0);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        int sizeFromGetSize = cache.getSize();
        int sizeFromKeys = cache.getKeys().size();
        assertEquals(sizeFromGetSize, sizeFromKeys);
        assertEquals(2, cache.getSize());
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key1", "value1"));

        //key1 should be in the Disk Store
        assertEquals(cache.getSize(), cache.getKeys().size());
        assertEquals(2, cache.getSize());
        //there were two of these, so size will now be one
        cache.remove("key1");
        assertEquals(cache.getSize(), cache.getKeys().size());
        assertEquals(1, cache.getSize());
        cache.remove("key2");
        assertEquals(cache.getSize(), cache.getKeys().size());
        assertEquals(0, cache.getSize());

        //try null values
        cache.put(new Element("nullValue1", null));
        cache.put(new Element("nullValue2", null));
        //Cannot overflow therefore just one
        assertEquals(1, cache.getSize());
        Element nullValueElement = cache.get("nullValue2");
        assertNull(nullValueElement.getValue());
        assertNull(nullValueElement.getObjectValue());

    }

    /**
     * Test getKeys after expiry
     * <p/>
     * Makes sure that if an element is expired, its key should also be expired
     */
    public void testGetKeysAfterExpiry() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test2", 1, true, false, 1, 0);
        manager.addCache(cache);
        String key1 = "key1";
        cache.put(new Element(key1, "value1"));
        cache.put(new Element("key2", "value1"));
        //getSize uses getKeys().size(), so these should be the same
        assertEquals(cache.getSize(), cache.getKeys().size());
        //getKeys does not do an expiry check, so the expired elements are counted
        assertEquals(2, cache.getSize());
        String keyFromDisk = (String) cache.get(key1).getObjectKey();
        assertTrue(key1 == keyFromDisk);
        Thread.sleep(1010);
        assertEquals(2, cache.getKeys().size());
        //getKeysWithExpiryCheck does check and gives the correct answer of 0
        assertEquals(0, cache.getKeysWithExpiryCheck().size());
    }


    /**
     * Answers the question of whether key references are preserved as elements are written to disk.
     * This is not a mandatory part of the API. If this test breaks in future it should be removed.
     */
    public void testKeysEqualsEquals() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test2", 0, true, false, 1, 0);
        manager.addCache(cache);
        String key1 = "key1";
        cache.put(new Element(key1, "value1"));
        cache.put(new Element("key2", "value1"));
        String keyFromDisk = (String) cache.get(key1).getObjectKey();
        assertTrue(key1 == keyFromDisk);
    }

    /**
     * Test size after multiple calls, with put and remove
     */
    public void testSizeMultipleCallsWithPutAndRemove() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test3", 1, true, true, 0, 0);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //key1 should be in the Disk Store
        assertEquals(2, cache.getSize());
        assertEquals(2, cache.getSize());
        assertEquals(2, cache.getSize());
        assertEquals(2, cache.getSize());
        assertEquals(2, cache.getSize());
        cache.remove("key1");
        assertEquals(1, cache.getSize());
        assertEquals(1, cache.getSize());
        assertEquals(1, cache.getSize());
        assertEquals(1, cache.getSize());
        assertEquals(1, cache.getSize());
        cache.remove("key2");
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getSize());
    }

    /**
     * Checks the expense of checking for duplicates
     * Typical Results Duplicate Check: 8ms versus 3ms for No Duplicate Check
     * <p/>
     * 66ms for 1000, 6ms for no duplicate/expiry
     * 187565 for 100000, where 500 is the in-memory size. 964ms without checking expiry. 134ms for getKeysNoDuplicateCheckTime
     * 18795 for 100000, where 50000 is in-memory size. 873ms without checking expiry. 158ms for getKeysNoDuplicateCheckTime
     */
    public void testGetKeysPerformance() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache cache = createTestCache();

        for (int i = 0; i < 2000; i++) {
            cache.put(new Element("key" + i, "value"));
        }
        //let the notifiers cool down
        Thread.sleep(1000);
        StopWatch stopWatch = new StopWatch();
        List keys = cache.getKeys();
        assertTrue("Should be 2000 keys. ", keys.size() == 2000);
        long getKeysTime = stopWatch.getElapsedTime();
        cache.getKeysNoDuplicateCheck();
        long getKeysNoDuplicateCheckTime = stopWatch.getElapsedTime();
        LOG.info("Time to get 1000 keys: With Duplicate Check: " + getKeysTime
                + " Without Duplicate Check: " + getKeysNoDuplicateCheckTime);
        assertTrue("Getting keys took more than 150ms", getKeysTime < 100);
    }


    /**
     * Checks the expense of checking in-memory size
     * 3467890 bytes in 1601ms for JDK1.4.2
     */
    public void testCalculateInMemorySizePerformanceAndReasonableness() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache cache = createTestCache();

        //Set up object graphs
        for (int i = 0; i < 1000; i++) {
            HashMap map = new HashMap(100);
            for (int j = 0; j < 100; j++) {
                map.put("key" + j, new String[]{"adfdafs", "asdfdsafa", "sdfasdf"});
            }
            cache.put(new Element("key" + i, map));
        }

        StopWatch stopWatch = new StopWatch();
        long size = cache.calculateInMemorySize();
        assertTrue("Size is " + size + ". Check it for reasonableness.", size > 100000 && size < 5000000);
        long elapsed = stopWatch.getElapsedTime();
        LOG.info("In-memory size in bytes: " + size
                + " time to calculate in ms: " + elapsed);
        assertTrue("Calculate memory size takes less than 3.5 seconds", elapsed < 3500);
    }


    /**
     * Expire elements and verify size is correct.
     */
    public void testGetSizeAfterExpiry() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Let the idle expire
        Thread.sleep(5010);
        assertEquals(null, cache.get("key1"));
        assertEquals(null, cache.get("key2"));

        assertEquals(0, cache.getSize());
    }

    /**
     * Test create and access times
     */
    public void testAccessTimes() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 5, true, false, 5, 2);
        assertEquals(Status.STATUS_UNINITIALISED, cache.getStatus());
        manager.addCache(cache);
        Element newElement = new Element("key1", "value1");
        long creationTime = newElement.getCreationTime();
        assertTrue(newElement.getCreationTime() > (System.currentTimeMillis() - 500));
        assertTrue(newElement.getHitCount() == 0);
        assertTrue(newElement.getLastAccessTime() == 0);

        cache.put(newElement);

        Element element = cache.get("key1");
        assertNotNull(element);
        assertEquals(creationTime, element.getCreationTime());
        assertTrue(element.getLastAccessTime() != 0);
        assertTrue(element.getHitCount() == 1);

        //Check that access statistics were reset but not creation time
        cache.put(element);
        element = cache.get("key1");
        assertEquals(creationTime, element.getCreationTime());
        assertTrue(element.getLastAccessTime() != 0);
        assertTrue(element.getHitCount() == 1);
    }

    /**
     * Tests initialisation failures
     */
    public void testInitialiseFailures() {
        try {
            Cache cache = new Cache("testInitialiseFailures2", 1, false, false, 5, 1);
            cache.initialise();

            cache.initialise();
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalStateException e) {
            //noop
        }
    }

    /**
     * Tests putting nulls throws correct exception
     *
     * @throws Exception
     */
    public void testPutFailures() throws Exception {
        Cache cache = new Cache("testPutFailures", 1, false, false, 5, 1);
        manager.addCache(cache);

        try {
            cache.put(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            //noop
        }

        try {
            cache.putQuiet(null);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            //noop
        }

        //Null Elements like this are OK
        cache.putQuiet(new Element(null, null));
    }

    /**
     * Tests cache, memory store and disk store sizes from config
     */
    public void testSizes() throws Exception {
        Ehcache cache = getSampleCache1();

        assertEquals(0, cache.getMemoryStoreSize());

        for (int i = 0; i < 10010; i++) {
            cache.put(new Element("key" + i, "value1"));
        }
        assertEquals(10010, cache.getSize());
        assertEquals(10000, cache.getMemoryStoreSize());
        assertEquals(10, cache.getDiskStoreSize());

        //NonSerializable
        cache.put(new Element(new Object(), Object.class));

        assertEquals(10011, cache.getSize());
        assertEquals(10000, cache.getMemoryStoreSize());
        assertEquals(11, cache.getDiskStoreSize());


        cache.remove("key4");
        cache.remove("key3");

        assertEquals(10009, cache.getSize());
        assertEquals(10000, cache.getMemoryStoreSize());
        assertEquals(9, cache.getDiskStoreSize());


        cache.removeAll();
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(0, cache.getDiskStoreSize());

    }

    /**
     * Tests flushing the cache
     *
     * @throws Exception
     */
    public void testFlushWhenOverflowToDisk() throws Exception {
        Cache cache = new Cache("testGetMemoryStoreSize", 50, true, false, 100, 200);
        manager.addCache(cache);

        assertEquals(0, cache.getMemoryStoreSize());

        for (int i = 0; i < 100; i++) {
            cache.put(new Element("" + i, new Date()));
        }
        //Not spoolable, should get ignored
        cache.put(new Element("key", new Object()));
        cache.put(new Element(new Object(), new Object()));
        cache.put(new Element(new Object(), "value"));

        //these "null" Elements are keyed the same way and only count as one
        cache.put(new Element(null, null));
        cache.put(new Element(null, null));

        cache.put(new Element("nullValue", null));

        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(55, cache.getDiskStoreSize());

        cache.flush();
        assertEquals(0, cache.getMemoryStoreSize());
        //Non Serializable Elements gets discarded
        assertEquals(100, cache.getDiskStoreSize());

    }


    /**
     * When flushing large MemoryStores, OutOfMemory issues can happen if we are
     * not careful to move each to Element to the DiskStore, rather than copy them all
     * and then delete them from the MemoryStore.
     * <p/>
     * This test manipulates a MemoryStore right on the edge of what can fit into the 64MB standard VM size.
     * An inefficient spool will cause an OutOfMemoryException.
     *
     * @throws Exception
     */
    public void testMemoryEfficiencyOfFlushWhenOverflowToDisk() throws Exception {
        Cache cache = new Cache("testGetMemoryStoreSize", 40000, true, false, 100, 200);
        manager.addCache(cache);

        assertEquals(0, cache.getMemoryStoreSize());

        for (int i = 0; i < 80000; i++) {
            cache.put(new Element("" + i, new byte[480]));
        }

        assertEquals(40000, cache.getMemoryStoreSize());
        assertEquals(40000, cache.getDiskStoreSize());

        long beforeMemory = measureMemoryUse();
        cache.flush();

        //It takes a while to write all the Elements to disk
        Thread.sleep(5000);

        long afterMemory = measureMemoryUse();
        long memoryIncrease = afterMemory - beforeMemory;
        assertTrue(memoryIncrease < 40000000);

        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(80000, cache.getDiskStoreSize());

    }


    /**
     * Tests using elements with null values. They should work as normal.
     *
     * @throws Exception
     */
    public void testElementWithNullValue() throws Exception {
        Cache cache = new Cache("testElementWithNullValue", 10, false, false, 100, 200);
        manager.addCache(cache);

        Object key1 = new Object();
        Element element = new Element(key1, null);
        cache.put(element);
        assertNotNull(cache.get(key1));
        assertNotNull(cache.getQuiet(key1));
        assertSame(element, cache.get(key1));
        assertSame(element, cache.getQuiet(key1));
        assertNull(cache.get(key1).getObjectValue());
        assertNull(cache.getQuiet(key1).getObjectValue());

        assertEquals(false, cache.isExpired(element));
    }


    /**
     * Tests put works correctly for Elements with overriden TTL
     *
     * @throws Exception
     */
    public void testPutWithOverriddenTTLAndTTI() throws Exception {
        Cache cache = new Cache("testElementWithNullValue", 10, false, false, 1, 1);
        manager.addCache(cache);

        Object key = new Object();
        Element element = new Element(key, "value");
        element.setTimeToLive(2);
        cache.put(element);
        Thread.sleep(1010);
        assertNotNull(cache.get(key));
        assertSame(element, cache.get(key));


        Element element2 = new Element(key, "value");
        cache.put(element2);
        Thread.sleep(1010);
        assertNull(cache.get(key));

        Element element3 = new Element(key, "value");
        element3.setTimeToLive(2);
        cache.put(element3);
        Thread.sleep(1500);
        assertSame(element3, cache.get(key));

    }


    /**
     * Tests putQuiet works correctly for Elements with overriden TTL
     *
     * @throws Exception
     */
    public void testPutQuietWithOverriddenTTLAndTTI() throws Exception {
        Cache cache = new Cache("testElementWithNullValue", 10, false, false, 1, 1);
        manager.addCache(cache);

        Object key = new Object();
        Element element = new Element(key, "value");
        element.setTimeToLive(2);
        cache.putQuiet(element);
        Thread.sleep(1010);
        assertNotNull(cache.get(key));
        assertSame(element, cache.get(key));


        Element element2 = new Element(key, "value");
        cache.putQuiet(element2);
        Thread.sleep(1010);
        assertNull(cache.get(key));

        Element element3 = new Element(key, "value");
        element3.setTimeToLive(2);
        cache.putQuiet(element3);
        Thread.sleep(1500);
        assertSame(element3, cache.get(key));

    }


    /**
     * Tests using elements with null values. They should work as normal.
     *
     * @throws Exception
     */
    public void testNonSerializableElement() throws Exception {
        Cache cache = new Cache("testElementWithNonSerializableValue", 1, true, false, 100, 200);
        manager.addCache(cache);

        Element element1 = new Element("key1", new Object());
        Element element2 = new Element("key2", new Object());
        cache.put(element1);
        cache.put(element2);

        //Removed because could not overflow
        assertNull(cache.get("key1"));

        //Second one should be in the MemoryStore and retrievable
        assertNotNull(cache.get("key2"));
    }


    /**
     * Tests what happens when an Element throws an Error on serialization. This mimics
     * what a nasty error like OutOfMemoryError could do.
     * <p/>
     * Before a change to the SpoolThread to handle this situation this test failed and generated the following log message.
     * Jun 28, 2006 7:17:16 PM net.sf.ehcache.store.DiskStore put
     * SEVERE: testThreadKillerCache: Elements cannot be written to disk store because the spool thread has died.
     *
     * @throws Exception
     */
    public void testSpoolThreadHandlesThreadKiller() throws Exception {
        Cache cache = new Cache("testThreadKiller", 1, true, false, 100, 200);
        manager.addCache(cache);

        Element elementThreadKiller = new Element("key", new ThreadKiller());
        cache.put(elementThreadKiller);
        Element element1 = new Element("key1", "one");
        Element element2 = new Element("key2", "two");
        cache.put(element1);
        cache.put(element2);

        Thread.sleep(2000);

        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }

    /**
     * Tests disk store and memory store size
     *
     * @throws Exception
     */
    public void testGetDiskStoreSize() throws Exception {
        Cache cache = new Cache("testGetDiskStoreSize", 1, true, false, 100, 200);
        manager.addCache(cache);
        assertEquals(0, cache.getDiskStoreSize());

        cache.put(new Element("key1", "value1"));
        assertEquals(0, cache.getDiskStoreSize());
        assertEquals(1, cache.getSize());

        cache.put(new Element("key2", "value2"));
        assertEquals(2, cache.getSize());
        assertEquals(1, cache.getDiskStoreSize());
        assertEquals(1, cache.getMemoryStoreSize());

        cache.put(new Element("key3", "value3"));
        cache.put(new Element("key4", "value4"));
        assertEquals(4, cache.getSize());
        assertEquals(3, cache.getDiskStoreSize());
        assertEquals(1, cache.getMemoryStoreSize());

        // remove last element inserted (is in memory store)
        assertNotNull(cache.getMemoryStore().get("key4"));
        cache.remove("key4");
        assertEquals(3, cache.getSize());
        assertEquals(3, cache.getDiskStoreSize());
        assertEquals(0, cache.getMemoryStoreSize());

        // remove key1 element
        assertNotNull(cache.getDiskStore().get("key1"));
        cache.remove("key1");
        assertEquals(2, cache.getSize());
        assertEquals(2, cache.getDiskStoreSize());
        assertEquals(0, cache.getMemoryStoreSize());

        // add another
        cache.put(new Element("key5", "value5"));
        assertEquals(3, cache.getSize());
        assertEquals(2, cache.getDiskStoreSize());
        assertEquals(1, cache.getMemoryStoreSize());

        // remove all
        cache.removeAll();
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getDiskStoreSize());
        assertEquals(0, cache.getMemoryStoreSize());

        //Check behaviour of NonSerializable objects
        cache.put(new Element(new Object(), new Object()));
        cache.put(new Element(new Object(), new Object()));
        cache.put(new Element(new Object(), new Object()));
        assertEquals(1, cache.getSize());
        assertEquals(0, cache.getDiskStoreSize());
        assertEquals(1, cache.getMemoryStoreSize());

    }

    /**
     * Tests that attempting to clone a cache fails with the right exception.
     *
     * @throws Exception
     */
    public void testCloneFailures() throws Exception {
        Cache cache = new Cache("testGetMemoryStore", 10, false, false, 100, 200);
        manager.addCache(cache);
        try {
            cache.clone();
            fail("Should have thrown CloneNotSupportedException");
        } catch (CloneNotSupportedException e) {
            //noop
        }
    }


    /**
     * Tests that the toString() method works.
     */
    public void testToString() {
        Ehcache cache = new Cache("testGetMemoryStore", 10, false, false, 100, 200);
        assertTrue(cache.toString().indexOf("testGetMemoryStore") > -1);
        assertEquals(411, cache.toString().length());
    }


    /**
     * When does equals mean the same thing as == ?
     *
     * @throws CacheException
     * @throws InterruptedException
     */
    public void testEquals() throws CacheException, InterruptedException {
        Cache cache = new Cache("cache", 1, true, false, 100, 200, false, 1);
        manager.addCache(cache);

        Element element1 = new Element("1", new Date());
        Element element2 = new Element("2", new Date());
        cache.put(element1);
        cache.put(element2);

        //Test equals and == from an Element retrieved from the MemoryStore
        Element elementFromStore = cache.get("2");
        assertEquals(element2, elementFromStore);
        assertTrue(element2 == elementFromStore);

        //Give the spool a chance to make sure it really got serialized to Disk
        Thread.sleep(300);

        //Test equals and == from an Element retrieved from the MemoryStore
        Element elementFromDiskStore = cache.get("1");
        assertEquals(element1, elementFromDiskStore);
        assertTrue(element1 != elementFromDiskStore);
    }

    /**
     * Tests the uniqueness of the GUID
     */
    public void testGuid() {
        Ehcache cache1 = new Cache("testGetMemoryStore", 10, false, false, 100, 200);
        Ehcache cache2 = new Cache("testGetMemoryStore", 10, false, false, 100, 200);
        String guid1 = cache1.getGuid();
        String guid2 = cache2.getGuid();
        assertEquals(cache1.getName(), cache2.getName());
        assertTrue(!guid1.equals(guid2));

    }


    /**
     * Does the Object API work?
     */
    public void testAPIObjectCompatibility() {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 5, true, false, 5, 2);
        manager.addCache(cache);

        Object objectKey = new Object();
        Object objectValue = new Object();
        Element objectElement = new Element(objectKey, objectValue);
        cache.put(objectElement);

        //Cannot get it back using get
        Element retrievedElement = cache.get(objectKey);
        assertNotNull(retrievedElement);
        try {
            retrievedElement.getObjectValue();
        } catch (CacheException e) {
            //expected
        }

        //Test that equals works
        retrievedElement = cache.get(objectKey);
        assertEquals(objectElement, retrievedElement);

        //Can with getObjectValue
        retrievedElement = cache.get(objectKey);
        assertEquals(objectValue, retrievedElement.getObjectValue());

    }


    /**
     * Does the Serializable API work?
     */
    public void testAPISerializableCompatibility() {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 5, true, false, 5, 2);
        manager.addCache(cache);

        //Try object compatibility
        Serializable key = new String();
        Element objectElement = new Element(key, new String());
        cache.put(objectElement);
        Object retrievedObject = cache.get(key);
        assertEquals(retrievedObject, objectElement);

        //Test that equals works
        assertEquals(objectElement, retrievedObject);
    }

    /**
     * Test issues reported.
     */
    public void testDiskStoreFlorian() {
        manager.shutdown();

        byte[] config = ("<ehcache> \n" +
                "<diskStore path=\"java.io.tmpdir\"/> \n" +
                "<defaultCache \n" +
                "            maxElementsInMemory=\"10000\" \n" +
                "            eternal=\"false\" \n" +
                "            timeToIdleSeconds=\"120\" \n" +
                "            timeToLiveSeconds=\"120\" \n" +
                "            overflowToDisk=\"true\" \n" +
                "            diskPersistent=\"false\" \n" +
                "            diskExpiryThreadIntervalSeconds=\"120\" \n" +
                "            memoryStoreEvictionPolicy=\"LRU\" \n" +
                "            /> " +
                "\n" +
                "<cache name=\"testCache\" \n" +
                "       maxElementsInMemory=\"20000\" \n" +
                "       eternal=\"false\" \n" +
                "       overflowToDisk=\"false\" \n" +
                "       timeToIdleSeconds=\"300\" \n" +
                "       timeToLiveSeconds=\"600\" \n" +
                "       diskPersistent=\"false\" \n" +
                "       diskExpiryThreadIntervalSeconds=\"1\" \n" +
                "       memoryStoreEvictionPolicy=\"LFU\" \n" +
                "/>           \n" +
                "<cache name=\"test2Cache\" \n" +
                "       maxElementsInMemory=\"20000\" \n" +
                "       eternal=\"false\" \n" +
                "       overflowToDisk=\"true\" \n" +
                "       timeToIdleSeconds=\"300\" \n" +
                "       timeToLiveSeconds=\"600\" \n" +
                "       diskPersistent=\"false\" \n" +
                "       diskExpiryThreadIntervalSeconds=\"1\" \n" +
                "       memoryStoreEvictionPolicy=\"LFU\" \n" +
                "/> \n" +
                "</ehcache> ").getBytes();


        CacheManager cacheManager = new CacheManager(new ByteArrayInputStream(config));
        Cache cache = new Cache("test3cache", 20000, true, false, 50, 30);
        assertTrue(cache.isOverflowToDisk());
        cacheManager.addCache(cache);

        for (int i = 0; i < 25000; i++) {
            cache.put(new Element(i + "", "value"));
        }

        assertEquals(5000, cache.getDiskStoreSize());
    }


    /**
     * Multi-thread read-write test with 20 threads
     * Just use MemoryStore to put max stress on cache
     * Values that work:
     * <pre>
     * size     threads     maxTime
     * 10000    50          200
     * 200000   50          500
     * 200000   500         800
     * </pre>
     */
    public void testReadWriteThreads() throws Exception {

        final int size = 10000;
        final int maxTime = 330;
        final Cache cache = new Cache("test3cache", size, false, true, 30, 30);
        manager.addCache(cache);

        long start = System.currentTimeMillis();
        final List executables = new ArrayList();
        final Random random = new Random();

        for (int i = 0; i < size; i++) {
            cache.put(new Element("" + i, "value"));
        }

        // 50% of the time get data
        for (int i = 0; i < 30; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    cache.get("key" + random.nextInt(size));
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    assertTrue("Get time outside of allowed range: " + elapsed, elapsed < maxTime);
                }
            };
            executables.add(executable);
        }

        //25% of the time add data
        for (int i = 0; i < 10; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    cache.put(new Element("key" + random.nextInt(size), "value"));
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    assertTrue("Put time outside of allowed range: " + elapsed, elapsed < maxTime);
                }
            };
            executables.add(executable);
        }

        //25% of the time remove the data
        for (int i = 0; i < 10; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    cache.remove("key" + random.nextInt(size));
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    assertTrue("Remove time outside of allowed range: " + elapsed, elapsed < maxTime);
                }
            };
            executables.add(executable);
        }

        //some of the time remove the data
        for (int i = 0; i < 10; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    int randomInteger = random.nextInt(20);
                    if (randomInteger == 3) {
                        cache.removeAll();
                    }
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    assertTrue("RemoveAll time outside of allowed range: " + elapsed, elapsed < maxTime);
                }
            };
            executables.add(executable);
        }

        runThreads(executables);
        long end = System.currentTimeMillis();
        LOG.info("Total time for the test: " + (end - start) + " ms");
    }


}
