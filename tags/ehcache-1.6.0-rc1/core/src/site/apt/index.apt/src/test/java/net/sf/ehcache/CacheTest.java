/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

import static junit.framework.Assert.assertSame;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.ExceptionHandlingDynamicCacheProxy;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.loader.CountingCacheLoader;
import net.sf.ehcache.loader.ExceptionThrowingLoader;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests for a Cache
 *
 * @author Greg Luck, Claus Ibsen
 * @version $Id$
 */
public class CacheTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheTest.class.getName());


    /**
     * teardown
     */
    @After
    public void tearDown() throws Exception {
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
    @Test
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

    }


    /**
     * Checks we cannot use a cache outside the manager
     */
    @Test
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
    }

    /**
     * Checks when and how we can set the cache name.
     */
    @Test
    public void testSetCacheName() throws CacheException {
        //Not put into manager.
        Ehcache cache = new Cache("testCache", 1, true, false, 5, 2);

        try {
            cache.setName(null);
            fail();
        } catch (IllegalArgumentException e) {
            //expected
        }

        cache.setName("name/with/slash");

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
    @Test
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
    @Test
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
    @Test
    public void testCacheWithNoIdle() throws Exception {
        Ehcache cache = manager.getCache("sampleCacheNoIdle");
        assertEquals("sampleCacheNoIdle", cache.getName());
        assertEquals(Status.STATUS_ALIVE, cache.getStatus());
        assertEquals(0, cache.getCacheConfiguration().getTimeToIdleSeconds());
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
    @Test
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
        Thread.sleep(5020);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }


    /**
     * Tests that the version and lastUpdate get upped for each put.
     * <cache name="sampleCacheNoIdle"
     * maxElementsInMemory="1000"
     * eternal="false"
     * timeToLiveSeconds="5"
     * overflowToDisk="false"
     * />
     */
    @Test
    public void testLastUpdate() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache cache = manager.getCache("sampleCache1");
        long beforeElementCreation = System.currentTimeMillis();
        //put in delay because time resolution is not exact on Windows
        Thread.sleep(10);
        cache.put(new Element("key1", "value1"));
        Element element = cache.get("key1");
        assertTrue(element.getCreationTime() >= beforeElementCreation);
        LOG.info("version: " + element.getVersion());
        LOG.info("creationTime: " + element.getCreationTime());
        LOG.info("lastUpdateTime: " + element.getLastUpdateTime());
        assertEquals(0, element.getLastUpdateTime());

        cache.put(new Element("key1", "value1"));
        element = cache.get("key1");
        LOG.info("version: " + element.getVersion());
        LOG.info("creationTime: " + element.getCreationTime());
        LOG.info("lastUpdateTime: " + element.getLastUpdateTime());

        cache.put(new Element("key1", "value1"));
        element = cache.get("key1");
        LOG.info("version: " + element.getVersion());
        LOG.info("creationTime: " + element.getCreationTime());
        LOG.info("lastUpdateTime: " + element.getLastUpdateTime());
    }


    /**
     * When to search the disk store
     */
    @Test
    public void testOverflowToDiskAndDiskPersistent() throws Exception {
        Ehcache cache = manager.getCache("sampleIdlingExpiringCache");

        for (int i = 0; i < 1001; i++) {
            cache.put(new Element("key" + i, "value1"));
        }

        assertNotNull(cache.get("key0"));

        for (int i = 0; i < 1001; i++) {
            cache.put(new Element("key" + i, "value1"));
            assertNotNull(cache.get("key" + i));
        }
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
    @Test
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
        Thread.sleep(1000);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to live.
        Thread.sleep(4020);
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
    @Test
    public void testExpiryBasedOnTimeToIdleElementOverride() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache cache = manager.getCache("sampleCacheNoIdle");
        assertEquals(30, cache.getCacheConfiguration().getDiskSpoolBufferSizeMB());
        Element element1 = new Element("key1", "value1");
        element1.setTimeToIdle(1);
        cache.put(element1);

        Element element2 = new Element("key2", "value1");
        element2.setTimeToIdle(1);
        cache.put(element2);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to idle. Should not idle out because not specified
        Thread.sleep(1050);
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
    @Test
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

        Thread.sleep(5050);
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
    @Test
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
        Thread.sleep(5020);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }


    /**
     * Test overflow to disk = false
     */
    @Test
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
     * <p/>
     * This test also has a cache with a CacheExceptionHandler registered. The performance effect is not detectable.
     */
    @Test
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

        //Memory only Typical 192ms
        for (int j = 0; j < 10; j++) {
            time = stopWatch.getElapsedTime();
            for (int i = 0; i < 5000; i++) {
                Integer key = new Integer(i);
                memoryOnlyCache.put(new Element(new Integer(i), "value"));
                memoryOnlyCache.get(key);
            }
            time = stopWatch.getElapsedTime();
            LOG.info("Time for MemoryStore: " + time);
            assertTrue("Time to put and get 5000 entries into MemoryStore", time < 300);
            Thread.sleep(500);
        }

        //Memory only with ExceptionHandlingTypical 192ms
        manager.replaceCacheWithDecoratedCache(memoryOnlyCache, ExceptionHandlingDynamicCacheProxy.createProxy(memoryOnlyCache));
        Ehcache exceptionHandlingMemoryOnlyCache = manager.getEhcache("testMemoryOnly");
        for (int j = 0; j < 10; j++) {
            time = stopWatch.getElapsedTime();
            for (int i = 0; i < 5000; i++) {
                Integer key = new Integer(i);
                exceptionHandlingMemoryOnlyCache.put(new Element(new Integer(i), "value"));
                exceptionHandlingMemoryOnlyCache.get(key);
            }
            time = stopWatch.getElapsedTime();
            LOG.info("Time for exception handling MemoryStore: " + time);
            assertTrue("Time to put and get 5000 entries into exception handling MemoryStore", time < 300);
            Thread.sleep(500);
        }

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
    @Test
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
    @Test
    public void testExpiryBasedOnTimeToLive() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 3, 0);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        Thread.sleep(1020);
        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        Thread.sleep(1020);
        //Test time to live
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
        Thread.sleep(1020);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }


    /**
     * Tests that a cache created from defaults will expire as per
     * the default expiry policy.
     *
     * @throws Exception
     */
    @Test
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
        Thread.sleep(10020);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));


    }


    /**
     * Test expiry based on time to live.
     * <p/>
     * Elements are put quietly back into the cache after being cloned.
     * The elements should expire as if the putQuiet had not happened.
     */
    @Test
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
        Thread.sleep(2020);
        //Should not affect age
        cache.putQuiet((Element) element2.clone());
        cache.putQuiet((Element) element2.clone());
        Thread.sleep(3020);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    /**
     * Test expiry based on time to live
     */
    @Test
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
        Thread.sleep(2020);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Check that we did not expire out
        Thread.sleep(3020);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }

    /**
     * Test expiry based on time to idle.
     */
    @Test
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
        Thread.sleep(2050);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));

        //Test effect of get
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        Thread.sleep(1050);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        Thread.sleep(2050);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }


    /**
     * Test expiry based on time to idle.
     */
    @Test
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
        Thread.sleep(1050);
        element1 = cache.getQuiet("key1");
        element2 = cache.getQuiet("key2");
        Thread.sleep(2050);
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
    @Test
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
    @Test
    public void testCacheStatistics() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        Element element1 = cache.get("key1");
        assertEquals("Should be one", 1, element1.getHitCount());
        assertEquals("Should be one", 1, cache.getStatistics().getCacheHits());
        element1 = cache.getQuiet("key1");
        assertEquals("Should be one", 1, element1.getHitCount());
        assertEquals("Should be one", 1, cache.getStatistics().getCacheHits());
        element1 = cache.get("key1");
        assertEquals("Should be two", 2, element1.getHitCount());
        assertEquals("Should be two", 2, cache.getStatistics().getCacheHits());


        assertEquals("Should be 0", 0, cache.getStatistics().getCacheMisses());
        cache.get("doesnotexist");
        assertEquals("Should be 1", 1, cache.getStatistics().getCacheMisses());


    }

    /**
     * Checks that getQuiet works how we expect it to
     *
     * @throws Exception
     */
    @Test
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
        assertEquals(1L, cache.getStatistics().getCacheHits());

        element1 = cache.getQuiet("key1");
        element1 = cache.getQuiet("key1");
        assertEquals(1L, cache.getStatistics().getCacheHits());
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
    @Test
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
        Object object = new Object();
        cache.put(new Element(object, null));
        cache.put(new Element(object, null));
        //Cannot overflow therefore just one
        assertEquals(1, cache.getSize());
        Element nullValueElement = cache.get(object);
        assertNull(nullValueElement.getValue());
        assertNull(nullValueElement.getObjectValue());

    }

    /**
     * Test getKeys after expiry
     * <p/>
     * Makes sure that if an element is expired, its key should also be expired
     */
    @Test
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
        Thread.sleep(1050);
        assertEquals(2, cache.getKeys().size());
        //getKeysWithExpiryCheck does check and gives the correct answer of 0
        assertEquals(0, cache.getKeysWithExpiryCheck().size());
    }


    /**
     * Answers the question of whether key references are preserved as elements are written to disk.
     * This is not a mandatory part of the API. If this test breaks in future it should be removed.
     */
    @Test
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
    @Test
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
    @Test
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
    @Test
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
    @Test
    public void testGetSizeAfterExpiry() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 1, 0);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Let the idle expire
        Thread.sleep(1020);
        assertEquals(null, cache.get("key1"));
        assertEquals(null, cache.get("key2"));

        assertEquals(0, cache.getSize());
    }

    /**
     * Test create and access times
     */
    @Test
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
    @Test
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
     * Nulls should be ignored
     *
     * @throws Exception
     */
    @Test
    public void testNullPuts() throws Exception {
        Cache cache = new Cache("testPutFailures", 1, false, false, 5, 1);
        manager.addCache(cache);

        cache.put(null);
        cache.put(null, false);
        cache.putQuiet(null);
        cache.putQuiet(new Element(null, null));

        //Null Elements like this are ignored
        cache.put(new Element(null, "dog"));
        cache.put(new Element(null, null));

        //Null Elements like this are ignored
        cache.putQuiet(new Element(null, "dog"));
        cache.putQuiet(new Element(null, null));
    }


    /**
     * Tests cache, memory store and disk store sizes from config
     */
    @Test
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
        Thread.sleep(15);
        cache.put(new Element(new Object(), Object.class));

        assertEquals(10011, cache.getSize());
        assertEquals(11, cache.getDiskStoreSize());
        assertEquals(10000, cache.getMemoryStoreSize());
        assertEquals(10000, cache.getMemoryStoreSize());
        assertEquals(10000, cache.getMemoryStoreSize());
        assertEquals(10000, cache.getMemoryStoreSize());


        cache.remove("key4");
        cache.remove("key3");

        assertEquals(10009, cache.getSize());
        //cannot make any guarantees as no elements have been getted, and all are equally likely to be evicted.
        //assertEquals(10000, cache.getMemoryStoreSize());
        //assertEquals(9, cache.getDiskStoreSize());


        cache.removeAll();
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(0, cache.getDiskStoreSize());

    }


    //@Test
    public void testSizesContinuous() throws Exception {
        while (true) {
            testFlushWhenOverflowToDisk();
        }
    }


    /**
     * Tests flushing the cache, with the default, which is to clear
     *
     * @throws Exception
     */
    @Test
    public void testFlushWhenOverflowToDisk() throws Exception {
        if (manager.getCache("testFlushWhenOverflowToDisk") == null) {
            manager.addCache(new Cache("testFlushWhenOverflowToDisk", 50, true, false, 100, 200, true, 120));
        }
        Cache cache = manager.getCache("testFlushWhenOverflowToDisk");
        cache.removeAll();

        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(0, cache.getDiskStoreSize());


        for (int i = 0; i < 100; i++) {
            cache.put(new Element("" + i, new Date()));
            //hit
            cache.get("" + i);
        }
        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(50, cache.getDiskStoreSize());


        cache.put(new Element("key", new Object()));
        cache.put(new Element("key2", new Object()));
        Object key = new Object();
        cache.put(new Element(key, "value"));

        //get it and make sure it is mru
        Thread.sleep(15);
        cache.get(key);

        assertEquals(101, cache.getSize());
        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(51, cache.getDiskStoreSize());


        //these "null" Elements are ignored and do not get put in
        cache.put(new Element(null, null));
        cache.put(new Element(null, null));

        assertEquals(101, cache.getSize());
        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(51, cache.getDiskStoreSize());

        //this one does
        cache.put(new Element("nullValue", null));

        LOG.info("Size: " + cache.getDiskStoreSize());

        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(52, cache.getDiskStoreSize());

        cache.flush();
        assertEquals(0, cache.getMemoryStoreSize());
        //Non Serializable Elements get discarded
        assertEquals(101, cache.getDiskStoreSize());

        cache.removeAll();

    }

    @Test
    public void testFlushWithoutClear() throws InterruptedException {

        CacheManager cacheManager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache.xml");
        Cache cache = cacheManager.getCache("SimplePageCachingFilter");
        cache.removeAll();
        for (int i = 0; i < 100; i++) {
            cache.put(new Element("" + i, new Date()));
            //hit
            cache.get("" + i);
        }
        assertEquals(10, cache.getMemoryStoreSize());
        assertEquals(90, cache.getDiskStoreSize());

        cache.flush();
        Thread.sleep(1000);

        assertEquals(10, cache.getMemoryStoreSize());
        assertEquals(100, cache.getDiskStoreSize());
        cacheManager.shutdown();

    }

    @Test
    public void testFlushWithClear() throws InterruptedException {

        CacheManager cacheManager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache.xml");
        Cache cache = cacheManager.getCache("SimplePageFragmentCachingFilter");
        cache.removeAll();
        for (int i = 0; i < 100; i++) {
            cache.put(new Element("" + i, new Date()));
            //hit
            cache.get("" + i);
        }
        assertEquals(10, cache.getMemoryStoreSize());
        assertEquals(90, cache.getDiskStoreSize());

        cache.flush();
        Thread.sleep(1000);

        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(100, cache.getDiskStoreSize());
        cacheManager.shutdown();

    }


    /**
     * When flushing large MemoryStores, OutOfMemory issues can happen if we are
     * not careful to move each Element to the DiskStore, rather than copy them all
     * and then delete them from the MemoryStore.
     * <p/>
     * This test manipulates a MemoryStore right on the edge of what can fit into the 64MB standard VM size.
     * An inefficient spool will cause an OutOfMemoryException.
     *
     * @throws Exception
     */
    @Test
    public void testMemoryEfficiencyOfFlushWhenOverflowToDisk() throws Exception {
        Cache cache = new Cache("testGetMemoryStoreSize", 40000, true, false, 100, 200, false, 120);

        manager.addCache(cache);
        StopWatch stopWatch = new StopWatch();

        assertEquals(0, cache.getMemoryStoreSize());

        for (int i = 0; i < 80000; i++) {
            cache.put(new Element("" + i, new byte[480]));
        }
        LOG.info("Put time: " + stopWatch.getElapsedTime());

        assertEquals(40000, cache.getMemoryStoreSize());
        assertEquals(40000, cache.getDiskStoreSize());

        long beforeMemory = measureMemoryUse();
        stopWatch.getElapsedTime();
        cache.flush();
        LOG.info("Flush time: " + stopWatch.getElapsedTime());

        //It takes a while to write all the Elements to disk
        Thread.sleep(1000);

        long afterMemory = measureMemoryUse();
        long memoryIncrease = afterMemory - beforeMemory;
        assertTrue(memoryIncrease < 40000000);

        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(40000, cache.getDiskStoreSize());

    }

    /**
     * Shows the effect of jamming large amounts of puts into a cache that overflows to disk.
     * The DiskStore should cause puts to back off and avoid an out of memory error.
     */
    @Test
    public void testBehaviourOnDiskStoreBackUp() throws Exception {
        Cache cache = new Cache("testGetMemoryStoreSize", 10, true, false, 100, 200, false, 0);
        manager.addCache(cache);

        assertEquals(0, cache.getMemoryStoreSize());

        Element a = null;
        int i = 0;
        try {
            for (; i < 200000; i++) {
                String key = i + "";
                String value = key;
                a = new Element(key, value + "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
                cache.put(a);
            }
        } catch (OutOfMemoryError e) {
            LOG.info("OutOfMemoryError: " + e.getMessage() + " " + i);
            fail();
        }
    }


    /**
     * Tests using elements with null values. They should work as normal.
     *
     * @throws Exception
     */
    @Test
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
    @Test
    public void testPutWithOverriddenTTLAndTTI() throws Exception {
        Cache cache = new Cache("testElementWithNullValue", 10, false, false, 1, 1);
        manager.addCache(cache);

        Object key = new Object();
        Element element = new Element(key, "value");
        element.setTimeToLive(2);
        cache.put(element);
        Thread.sleep(1050);
        assertNotNull(cache.get(key));
        assertSame(element, cache.get(key));


        Element element2 = new Element(key, "value");
        cache.put(element2);
        Thread.sleep(1050);
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
    @Test
    public void testPutQuietWithOverriddenTTLAndTTI() throws Exception {
        Cache cache = new Cache("testElementWithNullValue", 10, false, false, 1, 1);
        manager.addCache(cache);

        Object key = new Object();
        Element element = new Element(key, "value");
        element.setTimeToLive(2);
        cache.putQuiet(element);
        Thread.sleep(1050);
        assertNotNull(cache.get(key));
        assertSame(element, cache.get(key));


        Element element2 = new Element(key, "value");
        cache.putQuiet(element2);
        Thread.sleep(1050);
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
    @Test
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
     * Tests serialization of Serializable classes with null values.
     *
     * @throws Exception
     */
    @Test
    public void testNullCollectionsAreSerializable() throws Exception {
        Cache cache = new Cache("testElementWithNonSerializableValue", 1, true, false, 100, 200);
        manager.addCache(cache);
        ArrayList arrayList = null;

        Element element1 = new Element("key1", arrayList);
        Element element2 = new Element("key2", arrayList);
        cache.put(element1);
        cache.put(element2);

        //Still retrievable because null Serializable classes are still Serializable
        Element element = cache.get("key1");
        assertNotNull(element);
        assertNull(element.getValue());

        //Still retrievable because null Serializable classes are still Serializable
        assertNotNull(cache.get("key2"));
    }


    /**
     * Tests what happens when an Element throws an Error on serialization. This mimics
     * what a nasty error like OutOfMemoryError could do.
     * <p/>
     * Before a change to the SpoolAndExpiryThread to handle this situation this test failed and generated the following log message.
     * Jun 28, 2006 7:17:16 PM net.sf.ehcache.store.DiskStore put
     * SEVERE: testThreadKillerCache: Elements cannot be written to disk store because the spool thread has died.
     *
     * @throws Exception
     */
    @Test
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
    @Test
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
    @Test
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
    @Test
    public void testToString() {
        Ehcache cache = new Cache("testGetMemoryStore", 10, false, false, 100, 200);
        assertTrue(cache.toString().indexOf("testGetMemoryStore") > -1);
        assertEquals(410, cache.toString().length());
    }


    /**
     * When does equals mean the same thing as == ?
     *
     * @throws CacheException
     * @throws InterruptedException
     */
    @Test
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
    @Test
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
    @Test
    public void testAPIObjectCompatibility() {
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
    @Test
    public void testAPISerializableCompatibility() {
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
    @Test
    public void testDiskStoreFlorian() throws InterruptedException {
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
        Cache cache = new Cache("test3cache", 20000, false, true, 50, 30);
        //assertTrue(cache.getCacheConfiguration().isOverflowToDisk());
        cacheManager.addCache(cache);

        //todo size is slow
        for (int i = 0; i < 25000; i++) {
            cache.put(new Element(i + "", "value"));
//            assertEquals(i + 1, cache.getSize());
        }
        assertEquals(20000, cache.getSize());
//        assertEquals(5000, cache.getDiskStoreSize());
    }


    /**
     * Orig.
     * INFO: Average Get Time: 0.37618342 ms
     * INFO: Average Put Time: 0.61346555 ms
     * INFO: Average Remove Time: 0.43651128 ms
     * INFO: Average Remove All Time: 0.20818481 ms
     * INFO: Average keySet Time: 0.11898771 ms
     * <p/>
     * CLHM
     * INFO: Average Get Time for 3611277 observations: 0.0043137097 ms
     * INFO: Average Put Time for 554433 obervations: 0.011824693 ms
     * INFO: Average Remove Time for 802361 obervations: 0.008200797 ms
     * INFO: Average Remove All Time for 2887862 observations: 4.685127E-4 ms
     * INFO: Average keySet Time for 2659524 observations: 0.003155828 ms
     * <p/>
     * CHM with sampling
     * INFO: Average Get Time for 5424446 observations: 0.0046010227 ms
     * INFO: Average Put Time for 358907 obervations: 0.027190888 ms
     * INFO: Average Remove Time for 971741 obervations: 0.00924732 ms
     * INFO: Average keySet Time for 466812 observations: 0.15059596 ms
     *
     * @throws Exception
     */
    @Test
    public void testConcurrentReadWriteRemoveLRU() throws Exception {
        testConcurrentReadWriteRemove(MemoryStoreEvictionPolicy.LRU);
    }

    /**
     * <pre>
     * Orig.
     * INFO: Average Get Time: 1.2396777 ms
     * INFO: Average Put Time: 1.4968935 ms
     * INFO: Average Remove Time: 1.3399061 ms
     * INFO: Average Remove All Time: 0.22590445 ms
     * INFO: Average keySet Time: 0.20492058 ms
     * <p/>
     * INFO: Average Get Time: 1.081209 ms
     * INFO: Average Put Time: 1.2307026 ms
     * INFO: Average Remove Time: 1.1294961 ms
     * INFO: Average Remove All Time: 0.16385451 ms
     * INFO: Average keySet Time: 0.1549516 ms
     * <p/>
     * CHM version with no sync on get.
     * INFO: Average Get Time for 2582432 observations: 0.019930825 ms
     * INFO: Average Put Time for 297 obervations: 41.40404 ms
     * INFO: Average Remove Time for 1491 obervations: 13.892018 ms
     * INFO: Average Remove All Time for 135893 observations: 0.54172766 ms
     * INFO: Average keySet Time for 112686 observations: 0.7157411 ms
     * <p/>
     * 1.6
     * INFO: Average Get Time for 4984448 observations: 0.006596317 ms
     * INFO: Average Put Time for 7266 obervations: 0.42361686 ms
     * INFO: Average Remove Time for 2024066 obervations: 0.012883473 ms
     * INFO: Average Remove All Time for 3572412 observations: 8.817572E-5 ms
     * INFO: Average keySet Time for 2653539 observations: 0.002160511 ms
     * INFO: Total loads: 38
     * </pre>
     * With iterator
     * 1.6 with 100,000 store size: puts take 45ms. keySet 7ms
     * 1.6 with 1000,000 store size: puts take 381ms. keySet 7ms
     * 1,000,000 - using FastRandom (j.u.Random was dog slow)
     * INFO: Average Get Time for 2065131 observations: 0.013553619 ms
     * INFO: Average Put Time for 46404 obervations: 0.1605034 ms
     * INFO: Average Remove Time for 20515 obervations: 0.1515964 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 198 observations: 0.0 ms
     * <p/>
     * 9999 - using iterator
     * INFO: Average Get Time for 4305030 observations: 0.006000423 ms
     * INFO: Average Put Time for 3216 obervations: 0.92008704 ms
     * INFO: Average Remove Time for 5294 obervations: 0.048545524 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 147342 observations: 0.5606073 ms
     * 10001 - using FastRandom
     * INFO: Average Get Time for 4815249 observations: 0.005541354 ms
     * INFO: Average Put Time for 5186 obervations: 0.49826455 ms
     * INFO: Average Remove Time for 129163 obervations: 0.015120429 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 177342 observations: 0.500733 ms
     * 4999 - using iterator
     * INFO: Average Get Time for 4317409 observations: 0.0061599445 ms
     * INFO: Average Put Time for 2708 obervations: 1.0768094 ms
     * INFO: Average Remove Time for 17664 obervations: 0.11713089 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 321180 observations: 0.26723954 ms
     * 5001 - using FastRandom
     * INFO: Average Get Time for 3203904 observations: 0.0053447294 ms
     * INFO: Average Put Time for 152905 obervations: 0.056616854 ms
     * INFO: Average Remove Time for 737289 obervations: 0.008854059 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 272898 observations: 0.3118601 ms
     *
     * @throws Exception
     */
    @Test
    public void testConcurrentReadWriteRemoveLFU() throws Exception {
        testConcurrentReadWriteRemove(MemoryStoreEvictionPolicy.LFU);
    }

    /**
     * INFO: Average Get Time: 0.28684255 ms
     * INFO: Average Put Time: 0.34759903 ms
     * INFO: Average Remove Time: 0.31298608 ms
     * INFO: Average Remove All Time: 0.21396147 ms
     * INFO: Average keySet Time: 0.11740683 ms
     * <p/>
     * CLHM
     * INFO: Average Get Time for 4567959 observations: 0.005231658 ms
     * INFO: Average Put Time for 437078 obervations: 0.01527645 ms
     * INFO: Average Remove Time for 178915 obervations: 0.013335941 ms
     * INFO: Average Remove All Time for 3500724 observations: 0.0070434003 ms
     * INFO: Average keySet Time for 3207776 observations: 0.011053764 ms
     */
    @Test
    public void testConcurrentReadWriteRemoveFIFO() throws Exception {
        testConcurrentReadWriteRemove(MemoryStoreEvictionPolicy.FIFO);
    }

    public void testConcurrentReadWriteRemove(MemoryStoreEvictionPolicy policy) throws Exception {

        final int size = 10000;
        //set it higher for normal continuous integration so occasional higher numbes do not break tests
        final int maxTime = (int) (500 * StopWatch.getSpeedAdjustmentFactor());
        manager.addCache(new Cache("test3cache", size, policy, false, null, true, 30, 30, false, 120, null));
        final Ehcache cache = manager.getEhcache("test3cache");

        System.gc();
        Thread.sleep(500);
        System.gc();
        Thread.sleep(500);

        final AtomicLong getTimeSum = new AtomicLong();
        final AtomicLong getTimeCount = new AtomicLong();
        final AtomicLong putTimeSum = new AtomicLong();
        final AtomicLong putTimeCount = new AtomicLong();
        final AtomicLong removeTimeSum = new AtomicLong();
        final AtomicLong removeTimeCount = new AtomicLong();
        final AtomicLong removeAllTimeSum = new AtomicLong();
        final AtomicLong removeAllTimeCount = new AtomicLong();
        final AtomicLong keySetTimeSum = new AtomicLong();
        final AtomicLong keySetTimeCount = new AtomicLong();

        CountingCacheLoader countingCacheLoader = new CountingCacheLoader();
        cache.registerCacheLoader(countingCacheLoader);

        final List executables = new ArrayList();
        final Random random = new Random();

        for (int i = 0; i < size; i++) {
            cache.put(new Element("" + i, "value"));
        }

        //some of the time get data
        for (int i = 0; i < 26; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    cache.get("key" + random.nextInt(size));
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    assertTrue("Get time outside of allowed range: " + elapsed, elapsed < maxTime);
                    getTimeSum.getAndAdd(elapsed);
                    getTimeCount.getAndIncrement();
                }
            };
            executables.add(executable);
        }

        //some of the time add data
        for (int i = 0; i < 10; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    cache.put(new Element("key" + random.nextInt(size), "value"));
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    assertTrue("Put time outside of allowed range: " + elapsed, elapsed < maxTime);
                    putTimeSum.getAndAdd(elapsed);
                    putTimeCount.getAndIncrement();
                }
            };
            executables.add(executable);
        }

        //some of the time remove the data
        for (int i = 0; i < 7; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    cache.remove("key" + random.nextInt(size));
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    assertTrue("Remove time outside of allowed range: " + elapsed, elapsed < maxTime);
                    removeTimeSum.getAndAdd(elapsed);
                    removeTimeCount.getAndIncrement();
                }
            };
            executables.add(executable);
        }


        //some of the time removeAll the data
//        for (int i = 0; i < 10; i++) {
//            final Executable executable = new Executable() {
//                public void execute() throws Exception {
//                    final StopWatch stopWatch = new StopWatch();
//                    long start = stopWatch.getElapsedTime();
//                    int randomInteger = random.nextInt(20);
//                    if (randomInteger == 3) {
//                        cache.removeAll();
//                    }
//                    long end = stopWatch.getElapsedTime();
//                    long elapsed = end - start;
//                    //remove all is slower
//                    assertTrue("RemoveAll time outside of allowed range: " + elapsed, elapsed < (maxTime * 3));
//                    removeAllTimeSum.getAndAdd(elapsed);
//                    removeAllTimeCount.getAndIncrement();
//                }
//            };
//            executables.add(executable);
//        }


        //some of the time iterate
        for (int i = 0; i < 10; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    int randomInteger = random.nextInt(20);
                    if (randomInteger == 3) {
                        cache.getKeys();
                    }
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    //remove all is slower
                    assertTrue("cache.getKeys() time outside of allowed range: " + elapsed, elapsed < (maxTime * 3));
                    keySetTimeSum.getAndAdd(elapsed);
                    keySetTimeCount.getAndIncrement();
                }
            };
            executables.add(executable);
        }

        //some of the time exercise the loaders through their various methods. Loader methods themselves make no performance
        //guarantees. They should only lock the cache when doing puts and gets, which the time limits on the other threads
        //will check for.
        for (int i = 0; i < 4; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    int randomInteger = random.nextInt(20);
                    List keys = new ArrayList();
                    for (int i = 0; i < 2; i++) {
                        keys.add("key" + random.nextInt(size));
                    }
                    if (randomInteger == 1) {
                        cache.load("key" + random.nextInt(size));
                    } else if (randomInteger == 2) {
                        cache.loadAll(keys, null);
                    } else if (randomInteger == 3) {
                        cache.getWithLoader("key" + random.nextInt(size), null, null);
                    } else if (randomInteger == 4) {
                        cache.getAllWithLoader(keys, null);
                    }
                }
            };
            executables.add(executable);
        }


        try {
            int failures = runThreadsNoCheck(executables);
            LOG.info(failures + " failures");
            //CHM does have the occasional very slow time.
            assertTrue("Failures = " + failures, failures <= 50);
        } finally {
            LOG.info("Average Get Time for " + getTimeCount.get() + " observations: " + getTimeSum.floatValue() / getTimeCount.get() + " ms");
            LOG.info("Average Put Time for " + putTimeCount.get() + " obervations: " + putTimeSum.floatValue() / putTimeCount.get() + " ms");
            LOG.info("Average Remove Time for " + removeTimeCount.get() + " obervations: " + removeTimeSum.floatValue() / removeTimeCount.get() + " ms");
            LOG.info("Average Remove All Time for " + removeAllTimeCount.get() + " observations: " + removeAllTimeSum.floatValue() / removeAllTimeCount.get() + " ms");
            LOG.info("Average keySet Time for " + keySetTimeCount.get() + " observations: " + keySetTimeSum.floatValue() / keySetTimeCount.get() + " ms");
            LOG.info("Total loads: " + countingCacheLoader.getLoadCounter());
            LOG.info("Total loadAlls: " + countingCacheLoader.getLoadAllCounter());
        }
    }


    /**
     * Multi-thread read-write test with 20 threads
     * Just use MemoryStore to put max stress on cache
     * Values that work:
     * <pre>
     * Results 3/2/09
     * Feb 3, 2009 5:57:35 PM net.sf.ehcache.CacheTest testConcurrentReadPerformanceMemoryOnly
     * INFO: 400 threads. Average Get time: 0.033715356 ms
     * INFO: 800 threads. Average Get time: 18.419634 ms
     * INFO: 1200 threads. Average Get time: 56.21161 ms
     * INFO: 1600 threads. Average Get time: 85.19998 ms
     * INFO: 2000 threads. Average Get time: 85.83994 ms
     * </pre>
     * With ConcurrentHashMap
     * <pre>
     * INFO: 1 threads. Average Get time: 0.082987554 ms
     * INFO: 401 threads. Average Get time: 0.0070842816 ms
     * INFO: 801 threads. Average Get time: 0.0066290447 ms
     * INFO: 1201 threads. Average Get time: 0.0063261427 ms
     * INFO: 1601 threads. Average Get time: 0.005570657 ms
     * INFO: 2001 threads. Average Get time: 0.015918251 ms
     * <p/>
     * v207
     * INFO: 1 threads. Average Get time: 0.051759835 ms
     * INFO: 401 threads. Average Get time: 0.0118925795 ms
     * INFO: 801 threads. Average Get time: 0.021494854 ms
     * INFO: 1201 threads. Average Get time: 0.07880102 ms
     * INFO: 1601 threads. Average Get time: 0.067811936 ms
     * INFO: 2001 threads. Average Get time: 0.12559706 ms
     * </pre>
     */
    @Test
    public void testConcurrentReadPerformanceMemoryOnly() throws Exception {

        final int size = 10000;

        manager.addCache(new Cache("test3cache", size, false, true, 1000, 1000));
        final Ehcache cache = manager.getEhcache("test3cache");
        final Vector<Long> readTimes = new Vector<Long>();


        for (int threads = 1; threads <= 2100; threads += 400) {

            readTimes.clear();

            final List executables = new ArrayList();
            final Random random = new Random();

            for (int i = 0; i < size; i++) {
                cache.put(new Element("" + i, "value"));
            }

            //some of the time get data
            for (int i = 0; i < threads; i++) {
                final Executable executable = new Executable() {
                    public void execute() throws Exception {
                        final StopWatch stopWatch = new StopWatch();
                        long start = stopWatch.getElapsedTime();
                        cache.get("key" + random.nextInt(size));
                        long end = stopWatch.getElapsedTime();
                        long elapsed = end - start;
                        readTimes.add(elapsed);
                        Thread.sleep(10);
                    }
                };
                executables.add(executable);
            }


            int failures = runThreadsNoCheck(executables);
            LOG.info(failures + " failures");
            assertTrue(failures == 0);
            long totalReadTime = 0;
            for (Long readTime : readTimes) {
                totalReadTime += readTime;
            }
            LOG.info(threads + " threads. Average Get time: " + totalReadTime / (float) readTimes.size() + " ms");

        }

    }


    /**
     * Tests added from 1606323 Elements not stored in memory or on disk. This was supposedly
     * a bug but works.
     * This test passes.
     *
     * @throws Exception
     */
    @Test
    public void testTimeToLive15552000() throws Exception {
        long timeToLiveSeconds = 15552000;
        doRunTest(timeToLiveSeconds);
    }

    /**
     * This test passes.
     *
     * @throws Exception
     */
    @Test
    public void testTimeToLive604800() throws Exception {
        long timeToLiveSeconds = 604800;
        doRunTest(timeToLiveSeconds);
    }

    private void doRunTest(long timeToLiveSeconds) {
        String name = "memoryAndDiskCache";
        int maxElementsInMemory = 1000;
        MemoryStoreEvictionPolicy memoryStoreEvictionPolicy = MemoryStoreEvictionPolicy.LRU;
        boolean overflowToDisk = true;
        String diskStorePath = "java.io.tmp.dir/cache";
        boolean eternal = false;
        long timeToIdleSeconds = 0;
        boolean diskPersistent = true;
        long diskExpiryThreadIntervalSeconds = 3600;
        RegisteredEventListeners registeredEventListeners = null;
        BootstrapCacheLoader bootstrapCacheLoader = null;

        Cache memoryAndDisk = new Cache(
                name,
                maxElementsInMemory,
                memoryStoreEvictionPolicy,
                overflowToDisk,
                diskStorePath,
                eternal,
                timeToLiveSeconds,
                timeToIdleSeconds,
                diskPersistent,
                diskExpiryThreadIntervalSeconds,
                registeredEventListeners,
                bootstrapCacheLoader);

        this.manager.addCache(memoryAndDisk);

        String key = "test";
        Object value = new Object();

        memoryAndDisk.put(new Element(key, value));

        assertTrue(memoryAndDisk.isElementInMemory(key));
    }

    /**
     * Tests get from a finalize method, following a mailing list post from Felix Satyaputr
     *
     * @throws InterruptedException
     */
    @Test
    public void testGetQuietFromFinalize() throws InterruptedException {


        final Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);

        cache.put(new Element("key", "value"));
        cache.put(new Element("key2", "value"));
        cache.put(new Element("key3", "value"));
        cache.put(new Element("key4", "value"));
        cache.put(new Element("key5", "value"));

        //wait for overflow to kick in
        Thread.sleep(200);

        createTestObject();

        //try to get object finalized
        System.gc();
        Thread.sleep(200);
        System.gc();


    }

    private void createTestObject() {
        new TestObject();
    }


    /**
     * A class with a finalize implementation.
     */
    class TestObject {

        /**
         * Override the Object finalize method
         */
        protected void finalize() throws Throwable {
            manager.getCache("test").getQuiet("key");
            LOG.info("finalize run from thread " + Thread.currentThread().getName());
            super.finalize();
        }
    }


    @Test
    public void testGetWithLoader() {


        Cache cache = manager.getCache("sampleCache1");
        cache.registerCacheLoader(new CacheLoader() {
            public Object load(Object key, Object argument) throws CacheException {
                LOG.info("load1 " + key);
                return key;
            }

            /**
             * Load using both a key and an argument.
             * <p/>
             * JCache will use the loadAll(key) method where the argument is null.
             *
             * @param keys     the keys to load objects for
             * @param argument can be anything that makes sense to the loader
             * @return a map of Objects keyed by the collection of keys passed in.
             * @throws CacheException
             *
             */
            public Map loadAll(Collection keys, Object argument) throws CacheException {
                return null;
            }


            public String getName() {
                return null;
            }

            /**
             * Creates a clone of this extension. This method will only be called by ehcache before a
             * cache is initialized.
             * <p/>
             * Implementations should throw CloneNotSupportedException if they do not support clone
             * but that will stop them from being used with defaultCache.
             *
             * @return a clone
             * @throws CloneNotSupportedException if the extension could not be cloned.
             */
            public CacheLoader clone(Ehcache cache) throws CloneNotSupportedException {
                return null;
            }

            /**
             * Notifies providers to initialise themselves.
             * <p/>
             * This method is called during the Cache's initialise method after it has changed it's
             * status to alive. Cache operations are legal in this method.
             *
             * @throws CacheException
             */
            public void init() {
                //noop
            }

            /**
             * Providers may be doing all sorts of exotic things and need to be able to clean up on
             * dispose.
             * <p/>
             * Cache operations are illegal when this method is called. The cache itself is partly
             * disposed when this method is called.
             *
             * @throws CacheException
             */
            public void dispose() throws CacheException {
                //noop
            }

            /**
             * @return the status of the extension
             */
            public Status getStatus() {
                return null;
            }

            public Object load(Object o) throws CacheException {
                LOG.info("load2 " + o + " " + o.getClass());
                if (o.equals("c")) {
                    return null;
                }
                return o;
            }

            public Map loadAll(Collection collection) throws CacheException {
                return null;
            }
        });


        Element element = cache.get("a");
        assertNull(element);

        element = cache.getWithLoader("b", null, null);
        assertNotNull(element);

        //should be null
        element = cache.getWithLoader("c", null, null);
        assertNull(element);
    }

    /**
     * Tests the async load with a single item
     */
    @Test
    public void testAsynchronousLoad() throws InterruptedException, ExecutionException {

        CountingCacheLoader countingCacheLoader = new CountingCacheLoader();
        Cache cache = manager.getCache("sampleCache1");
        cache.registerCacheLoader(countingCacheLoader);
        ExecutorService executorService = cache.getExecutorService();

        Future future = cache.asynchronousLoad("key1", null, null);

        Object object = future.get();
        assertTrue(future.isDone());
        assertNull(object);

        assertFalse(executorService.isShutdown());

        assertEquals(1, cache.getSize());
        assertEquals(1, countingCacheLoader.getLoadCounter());
    }

    /**
     * Tests the async load with a single item
     */
    @Test
    public void testGetWithLoaderException() {
        Cache cache = manager.getCache("sampleCache1");
        cache.registerCacheLoader(new ExceptionThrowingLoader());
        try {
            cache.getWithLoader("key1", null, null);
            fail();
        } catch (CacheException e) {
            //expected
        }
    }


    /**
     * Tests the loadAll async method
     */
    @Test
    public void testAsynchronousLoadAll() throws InterruptedException, ExecutionException {

        CountingCacheLoader countingCacheLoader = new CountingCacheLoader();
        Cache cache = manager.getCache("sampleCache1");
        cache.registerCacheLoader(countingCacheLoader);
        ExecutorService executorService = cache.getExecutorService();

        List keys = new ArrayList();
        for (int i = 0; i < 1000; i++) {
            keys.add(new Integer(i));
        }

        Future future = cache.asynchronousLoadAll(keys, null);
        assertFalse(future.isDone());

        Object object = future.get();
        assertTrue(future.isDone());
        assertNull(object);

        assertFalse(executorService.isShutdown());

        assertEquals(1000, cache.getSize());
        assertEquals(1000, countingCacheLoader.getLoadAllCounter());
    }

    /**
     * Tests programmatically disabling and enabling a cache
     */
    @Test
    public void testEnableAndDisable() throws Exception {
        Ehcache cache = manager.getCache("sampleCacheNoIdle");
        cache.put(new Element("key1put", "value1"));
        cache.put(new Element("key1putQuiet", "value1"));
        assertFalse(cache.isDisabled());
        assertNotNull(cache.get("key1put"));
        assertNotNull(cache.get("key1putQuiet"));

        //now disable
        cache.setDisabled(true);

        assertTrue(cache.isDisabled());
        assertNotNull(cache.get("key1put"));
        assertNotNull(cache.get("key1putQuiet"));

        cache.put(new Element("key2put", "value1"));
        cache.put(new Element("key2putQuiet", "value1"));
        assertNull(cache.get("key2put"));
        assertNull(cache.get("key2putQuiet"));
    }

}
