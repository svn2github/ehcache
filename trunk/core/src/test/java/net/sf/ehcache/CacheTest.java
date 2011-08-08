/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.Assert.assertSame;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.loader.CountingCacheLoader;
import net.sf.ehcache.loader.DelayingLoader;
import net.sf.ehcache.loader.ExceptionThrowingLoader;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.impl.AbstractPoolAccessor;
import net.sf.ehcache.store.FrontEndCacheTier;
import net.sf.ehcache.store.MemoryStore;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for a Cache
 * <p/>
 * Since expiration is rounded on seconds, we need to at least go up to the last
 * millisecond before the next second in many of the tests
 *
 * @author Greg Luck, Claus Ibsen
 * @version $Id$
 */
public class CacheTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheTest.class.getName());


    /**
     * teardown
     */
    @Override
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
            assertEquals("The sampleCache1 Cache is not alive (STATUS_SHUTDOWN)", e.getMessage());
        }
        try {
            cache.put(element);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive (STATUS_SHUTDOWN)", e.getMessage());
        }
        try {
            cache.get("key");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The sampleCache1 Cache is not alive (STATUS_SHUTDOWN)", e.getMessage());
        }

    }

    @Test
    public void testCantSwitchPool() throws Exception {
        Configuration configuration = new Configuration();
        CacheManager cacheManager = new CacheManager(configuration.maxBytesLocalHeap(10, MemoryUnit.MEGABYTES));

        CacheConfiguration cacheConfiguration = new CacheConfiguration("one", 0);
        cacheManager.addCache(new Cache(cacheConfiguration));
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(10), cacheManager.getCache("one"));
        try {
            cacheManager.getCache("one").getCacheConfiguration().maxBytesLocalHeap(5, MemoryUnit.MEGABYTES);
            fail();
        } catch (IllegalStateException e) {
            assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(10), cacheManager.getCache("one"));
        }
    }

    @Test
    public void testAdjustsPoolSizeDynamically() throws Exception {
        Configuration configuration = new Configuration();
        CacheManager cacheManager = new CacheManager(configuration.maxBytesLocalHeap(10, MemoryUnit.MEGABYTES));

        // Three and Four share the CM Pool
        CacheConfiguration cacheConfiguration = new CacheConfiguration("three", 0);
        cacheManager.addCache(new Cache(cacheConfiguration));
        cacheConfiguration = new CacheConfiguration("four", 0);
        cacheManager.addCache(new Cache(cacheConfiguration));

        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(10), cacheManager.getCache("three"));
        cacheManager.addCache(new Cache(new CacheConfiguration("one", 0).maxBytesLocalHeap(2, MemoryUnit.MEGABYTES)));
        Cache one = cacheManager.getCache("one");
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(2), one);
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(8), cacheManager.getCache("three"));
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(8), cacheManager.getCache("four"));

        one.getCacheConfiguration().maxBytesLocalHeap(5, MemoryUnit.MEGABYTES);
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(5), one);
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(5), cacheManager.getCache("three"));
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(5), cacheManager.getCache("four"));

        cacheConfiguration = new CacheConfiguration("two", 0);
        cacheConfiguration.setMaxBytesLocalHeap("20%");
        cacheManager.addCache(new Cache(cacheConfiguration));
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(2), cacheManager.getCache("two"));
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(3), cacheManager.getCache("three"));
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(3), cacheManager.getCache("four"));

        cacheManager.getConfiguration().maxBytesLocalHeap(20, MemoryUnit.MEGABYTES);
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(5), one);
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(4), cacheManager.getCache("two"));

        // 20M - 5M (one) - 4M (two has 20% of the 20M), leaves 11M for cache three and four sharing the CM Pool
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(11), cacheManager.getCache("three"));
        assertCachePoolSize(MemoryUnit.MEGABYTES.toBytes(11), cacheManager.getCache("four"));
    }

    private static void assertCachePoolSize(final long value, final Cache one) throws Exception {
        Store store = one.getStore();
        if (store instanceof FrontEndCacheTier) {
            Store authority = getAuthority(store);
            Field poolAccessor = MemoryStore.class.getDeclaredField("poolAccessor");
            poolAccessor.setAccessible(true);
            PoolAccessor accessor = (PoolAccessor)poolAccessor.get(authority);
            Field pool = AbstractPoolAccessor.class.getDeclaredField("pool");
            pool.setAccessible(true);
            Pool ourPool = (Pool)pool.get(accessor);
            assertThat(ourPool.getMaxSize(), is(value));
        }
    }

    private static Store getAuthority(final Store store) throws Exception {
        Field poolAccessor = FrontEndCacheTier.class.getDeclaredField("authority");
        poolAccessor.setAccessible(true);
        return (Store) poolAccessor.get(store);
    }

    /**
     * Test multiple calls to dispose is not a problem
     */
    @Test
    public void testMultipleDispose() {
        Ehcache cache = new Cache("testCache", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.dispose();
        // call dispose multiple times, shouldn't throw IllegalStateException anymore
        for (int i = 0; i < 10; i++) {
            cache.dispose();
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
            assertEquals("The testCache Cache is not alive (STATUS_UNINITIALISED)", e.getMessage());
        }
        try {
            cache.put(element);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive (STATUS_UNINITIALISED)", e.getMessage());
        }
        try {
            cache.get("key");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The testCache Cache is not alive (STATUS_UNINITIALISED)", e.getMessage());
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
            assertEquals("The test Cache is not alive (STATUS_SHUTDOWN)", e.getMessage());
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
        Thread.sleep(2999);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to live.
        Thread.sleep(5999);
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
        Thread.sleep(1999);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to live.
        Thread.sleep(4999);
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
     * where an Element override is set on TTL
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

        //Test time to idle
        Thread.sleep(1999);
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

        Thread.sleep(5999);
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
        Thread.sleep(2999);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Test time to live.
        Thread.sleep(5999);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
    }


    /**
     * Test overflow to disk = false
     */
    @Test
    public void testNoOverflowToDisk() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, false, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        assertNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));
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
        Thread.sleep(1959);
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
        Thread.sleep(10999);
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
        Thread.sleep(2999);
        //Should not affect age
        cache.putQuiet((Element) element2.clone());
        cache.putQuiet((Element) element2.clone());
        Thread.sleep(3000);
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
        Thread.sleep(2999);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        //Check that we did not expire out
        Thread.sleep(3999);
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
        Thread.sleep(4000);
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));

        //Test effect of get
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        Thread.sleep(1000);
        assertNotNull(cache.get("key1"));
        assertNotNull(cache.get("key2"));

        Thread.sleep(4000);
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
        Thread.sleep(2949);
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

        cache.setStatisticsEnabled(true);

        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        //allow disk write thread to catch up - MNK-2057
        Thread.sleep(100);

        Element element1 = cache.get("key1");
        assertEquals("Element hit count", 1, element1.getHitCount());
        element1 = cache.getQuiet("key1");
        assertEquals("Element hit count", 1, element1.getHitCount());
        element1 = cache.get("key1");
        assertEquals("Element hit count", 2, element1.getHitCount());
    }

    /**
     * Test cache statistics, including get and getQuiet
     */
    @Test
    public void testCacheStatistics() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);

        cache.setStatisticsEnabled(true);

        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //Allow disk write thread to do it's thing...
        Thread.sleep(100);

        Element element1 = cache.get("key1");
        assertEquals("Cache hit count", 1, cache.getStatistics().getCacheHits());
        assertEquals("Element hit count", 1, element1.getHitCount());
        element1 = cache.getQuiet("key1");
        assertEquals("Cache hit count", 1, cache.getStatistics().getCacheHits());
        assertEquals("Element hit count", 1, element1.getHitCount());
        element1 = cache.get("key1");
        assertEquals("Cache hit count", 2, cache.getStatistics().getCacheHits());
        assertEquals("Element hit count", 2, element1.getHitCount());


        assertEquals("Cache miss count", 0, cache.getStatistics().getCacheMisses());
        cache.get("doesnotexist");
        assertEquals("Cache miss count", 1, cache.getStatistics().getCacheMisses());


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

        cache.setStatisticsEnabled(true);

        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //allow the writer thread to complete
        Thread.sleep(200);

        Element element1 = cache.get("key1");
        long lastAccessedElement1 = element1.getLastAccessTime();
        long hitCountElement1 = element1.getHitCount();
        assertEquals("Element-1 Hit Count", 1, hitCountElement1);
        assertEquals("Cache Hit Count", 1L, cache.getStatistics().getCacheHits());

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
        final Cache cache = new Cache("test2", 1, true, true, 0, 0);
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
        cache.removeAll();
        Object object1 = new Object();
        Object object2 = new Object();
        cache.put(new Element(object1, null));
        cache.put(new Element(object2, null));

        //Cannot overflow therefore just one
        try {
            RetryAssert.assertBy(3, SECONDS, new Callable<Integer>() {
                public Integer call() throws Exception {
                    return cache.getSize();
                }
            }, Is.is(1));
        } catch (AssertionError e) {
            //eviction failure
            System.err.println(e + " - likely eviction failure: checking memory store");
            assertEquals(2, cache.getMemoryStoreSize());
        }
        Element nullValueElement = cache.get(object1);
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
        assertEquals(key1, keyFromDisk);
        Thread.sleep(1999);
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
     * Expire elements and verify size is correct.
     */
    @Test
    public void testGetSizeAfterExpiry() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 1, 0, false, Long.MAX_VALUE);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        // Let the idle expire
        Thread.sleep(1999);
        assertEquals(null, cache.get("key2"));
        assertEquals(null, cache.get("key1"));

        try {
            assertEquals(0, cache.getSize());
        } catch (AssertionError e) {
            LOG.warn("Inline Expiry Removal Failed (May Happen Occasionally) - trying explicit removal of expired elements.");
            cache.evictExpiredElements();
            assertEquals(0, cache.getSize());
        }
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
        final String name = "testInitialiseFailures2";
        Cache cache = new Cache(name, 1, false, false, 5, 1);
        cache.initialise();

        try {
            cache.initialise();
            fail("Calling cache.initialise() multiple times should fail with IllegalStateException");
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("Cannot initialise the " + name)) {
                // expected exception
            } else {
                throw e;
            }
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

        Thread.sleep(1000);

        assertEquals(10000, cache.getSize());
        assertEquals(10000, cache.getMemoryStoreSize());
        assertTrue(1010 > cache.getDiskStoreSize());

        //NonSerializable
        Thread.sleep(15);
        cache.put(new Element(new Object(), Object.class));

        Thread.sleep(1000);

        assertEquals(10000, cache.getSize());
        assertTrue(1010 > cache.getDiskStoreSize());
        assertEquals(10000, cache.getMemoryStoreSize());
        assertEquals(10000, cache.getMemoryStoreSize());
        assertEquals(10000, cache.getMemoryStoreSize());
        assertEquals(10000, cache.getMemoryStoreSize());


        cache.remove("key4");
        cache.remove("key3");

        assertEquals(9998, cache.getSize());
        //cannot make any guarantees as no elements have been getted, and all are equally likely to be evicted.
        //assertEquals(10000, cache.getMemoryStoreSize());
        //assertEquals(9, cache.getDiskStoreSize());


        Thread.sleep(1000);

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
     * <p/>
     * Note: Which element gets evicted is probabilistic. 1.5 and earlier were deterministic. Thus
     * the variation in what gets into the DiskStore.
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
            cache.put(new Element(Integer.valueOf(i), new Date()));
        }

        RetryAssert.assertBy(10, TimeUnit.SECONDS, new GetCacheDiskSize(cache), is(100));

        for (int i = 0; i < 100; i++) {
            cache.get(Integer.valueOf(i));
        }

        RetryAssert.assertBy(10, TimeUnit.SECONDS, new GetCacheMemorySize(cache), lessThanOrEqualTo(50L));
        assertEquals(100, cache.getSize());
        assertEquals(100, cache.getDiskStoreSize());


        //Should get selected. But this is probabilistic
        cache.put(new Element("key", new String("sdf")));
        cache.put(new Element("key2", new String("fdgdf")));
        cache.put(new Element("key1", "value"));

        RetryAssert.assertBy(10, TimeUnit.SECONDS, new GetCacheDiskSize(cache), is(103));

        cache.get("key1");

        RetryAssert.assertBy(10, TimeUnit.SECONDS, new GetCacheMemorySize(cache), lessThanOrEqualTo(50L));
        assertEquals(103, cache.getSize());
        assertEquals(103, cache.getDiskStoreSize());


        //these "null" Elements are ignored and do not get put in
        cache.put(new Element(null, null));
        cache.put(new Element(null, null));

        assertEquals(103, cache.getSize());
        assertEquals(103, cache.getDiskStoreSize());
        assertThat(cache.getMemoryStoreSize(), lessThanOrEqualTo(50L));

        //this one does
        cache.put(new Element("nullValue", null));

        RetryAssert.assertBy(10, TimeUnit.SECONDS, new GetCacheDiskSize(cache), is(104));
        assertThat(cache.getMemoryStoreSize(), lessThanOrEqualTo(50L));

        cache.flush();

        RetryAssert.assertBy(10, TimeUnit.SECONDS, new GetCacheMemorySize(cache), is(0L));
        //Non Serializable Elements get discarded
        assertEquals(104, cache.getDiskStoreSize());

        cache.removeAll();
    }

    @Test
    public void testFlushWithoutClear() throws InterruptedException {

        CacheManager cacheManager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache.xml");
        Cache cache = cacheManager.getCache("SimplePageCachingFilter");
        cache.removeAll();
        for (int i = 0; i < 100; i++) {
            cache.put(new Element("" + i, new Date()));
        }

        Thread.sleep(200);

        for (int i = 0; i < 100; i++) {
            cache.get("" + i);
        }

        assertEquals(10, cache.getMemoryStoreSize());
        assertEquals(100, cache.getDiskStoreSize());

        cache.flush();
        Thread.sleep(200);

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
            cache.put(new Element(Integer.toString(i), new Date()));
        }

        for (long start = System.nanoTime(); (cache.getDiskStoreSize() != 100) && (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(30));) {
            Thread.sleep(10);
        }

        for (int i = 0; i < 100; i++) {
            cache.get(Integer.toString(i));
        }

        assertEquals(10, cache.getMemoryStoreSize());
        assertEquals(100, cache.getDiskStoreSize());

        cache.flush();
        for (long start = System.nanoTime(); (cache.getMemoryStoreSize() > 0) && (System.nanoTime() - start < TimeUnit.SECONDS.toNanos(30));) {
            Thread.sleep(10);
        }

        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(100, cache.getDiskStoreSize());
        cacheManager.shutdown();
    }


    /**
     * Shows the effect of jamming large amounts of puts into a cache that overflows to disk.
     * The DiskStore should cause puts to back off and avoid an out of memory error.
     */
    @Test
    public void testBehaviourOnDiskStoreBackUp() throws Exception {
        Cache cache = new Cache(new CacheConfiguration().name("testBehaviourOnDiskStoreBackUp")
                .maxElementsInMemory(1000)
                .overflowToDisk(true)
                .eternal(false)
                .timeToLiveSeconds(100)
                .timeToIdleSeconds(200)
                .diskPersistent(false)
                .diskExpiryThreadIntervalSeconds(0)
                .diskSpoolBufferSizeMB(10));
        manager.addCache(cache);

        assertEquals(0, cache.getMemoryStoreSize());

        Element a = null;
        int i = 0;
        try {
            for (; i < 150000; i++) {
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
        element.setTimeToLive(3);
        cache.put(element);
        Thread.sleep(1050);
        assertNotNull(cache.get(key));
        assertSame(element, cache.get(key));

        Element element2 = new Element(key, "value");
        cache.put(element2);
        Thread.sleep(1999);
        assertNull(cache.get(key));

        Element element3 = new Element(key, "value");
        element3.setTimeToLive(5);
        cache.put(element3);
        Thread.sleep(1999);
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
        element.setTimeToLive(3);
        cache.putQuiet(element);
        Thread.sleep(1050);
        assertNotNull(cache.get(key));
        assertSame(element, cache.get(key));

        Element element2 = new Element(key, "value");
        cache.putQuiet(element2);
        Thread.sleep(1999);
        assertNull(cache.get(key));

        Element element3 = new Element(key, "value");
        element3.setTimeToLive(5);
        cache.putQuiet(element3);
        Thread.sleep(1999);
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

        Thread.sleep(1000);

        //Removed because could not overflow
        if (cache.get("key1") == null) {
            assertNotNull(cache.get("key2"));
        } else {
            assertNull(cache.get("key2"));
        }
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
        Cache cache = new Cache("testThreadKiller", 0, true, false, 100, 200);
        manager.addCache(cache);

        Element elementThreadKiller = new Element("key", new ThreadKiller());
        cache.put(elementThreadKiller);
        Thread.sleep(2999);
        Element element1 = new Element("key1", "one");
        Element element2 = new Element("key2", "two");
        cache.put(element1);
        cache.put(element2);

        Thread.sleep(2999);

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
        RetryAssert.assertBy(1, TimeUnit.SECONDS, new GetCacheDiskSize(cache), is(1));
        assertEquals(1, cache.getSize());

        cache.put(new Element("key2", "value2"));
        RetryAssert.assertBy(1, TimeUnit.SECONDS, new GetCacheDiskSize(cache), is(2));
        assertEquals(2, cache.getSize());
        assertEquals(1, cache.getMemoryStoreSize());

        cache.put(new Element("key3", "value3"));
        cache.put(new Element("key4", "value4"));
        RetryAssert.assertBy(1, TimeUnit.SECONDS, new GetCacheDiskSize(cache), is(4));
        assertEquals(4, cache.getSize());
        assertEquals(1, cache.getMemoryStoreSize());

        // remove last element inserted (is in memory store)
        cache.remove("key4");
        RetryAssert.assertBy(1, TimeUnit.SECONDS, new GetCacheDiskSize(cache), is(3));
        assertEquals(3, cache.getSize());
        assertEquals(1, cache.getMemoryStoreSize());

        // remove key1 element
        cache.remove("key1");
        RetryAssert.assertBy(1, TimeUnit.SECONDS, new GetCacheDiskSize(cache), is(2));
        assertEquals(2, cache.getSize());
        assertEquals(0, cache.getMemoryStoreSize());

        // add another
        cache.put(new Element("key5", "value5"));
        RetryAssert.assertBy(1, TimeUnit.SECONDS, new GetCacheDiskSize(cache), is(3));
        assertEquals(3, cache.getSize());
        assertEquals(1, cache.getMemoryStoreSize());

        // remove all
        cache.removeAll();
        RetryAssert.assertBy(1, TimeUnit.SECONDS, new GetCacheDiskSize(cache), is(0));
        assertEquals(0, cache.getSize());
        assertEquals(0, cache.getMemoryStoreSize());

        //Check behaviour of NonSerializable objects
        cache.put(new Element(new Object(), new Object()));
        cache.put(new Element(new Object(), new Object()));
        cache.put(new Element(new Object(), new Object()));

        Thread.sleep(200);

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
     * When does equals mean the same thing as == ?
     *
     * @throws CacheException
     * @throws InterruptedException
     */
    @Test
    public void testIsKeyInCache() throws CacheException, InterruptedException {
        Cache cache = new Cache("cache", 1, true, false, 100, 200, false, 1);
        manager.addCache(cache);

        Element element1 = new Element("1", new Date());
        Element element2 = new Element("2", new Date());
        cache.put(element1);
        cache.put(element2);

        assertTrue(cache.isKeyInCache("1"));
        assertTrue(cache.isKeyInCache("2"));
        assertFalse(cache.isKeyInCache(null));
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
        Cache cache = new Cache("test3cache", 20000, false, false, 50, 30);
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
        String diskStorePath = "java.io.tmpdir/cache";
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
        Object value = "value";

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
        @Override
        protected void finalize() throws Throwable {
            manager.getCache("test").getQuiet("key");
            LOG.info("finalize run from thread " + Thread.currentThread().getName());
            super.finalize();
        }
    }


    @Test
    public void testGetWithLoader() {

        /**
         *
         */
        class TestCacheLoader implements CacheLoader {


            public Object load(Object key, Object argument) throws CacheException {
                LOG.info("load1 " + key);
                return key;
            }

            public Map loadAll(Collection keys, Object argument) throws CacheException {
                return null;
            }


            public String getName() {
                return null;
            }

            public CacheLoader clone(Ehcache cache) throws CloneNotSupportedException {
                return null;
            }

            public void init() {
                //noop
            }

            public void dispose() throws CacheException {
                //noop
            }

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


        }

        Cache cache = manager.getCache("sampleCache1");
        cache.registerCacheLoader(new TestCacheLoader());


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

        Future future = cache.asynchronousPut("key1", null, null);

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
     * Tests the async load with a timeout
     */
    @Test
    public void testGetWithLoaderTimeout() {
        Cache cache = manager.getCache("sampleCacheTimeout");
        cache.registerCacheLoader(new DelayingLoader(2000));
        try {
            cache.getWithLoader("key1", null, null);
            fail();
        } catch (CacheException e) {
            //expected
        }
    }

    /**
     * Tests the async load with a timeout
     */
    @Test
    public void testGetAllWithLoaderTimeout() {
        Cache cache = manager.getCache("sampleCacheTimeout");
        cache.registerCacheLoader(new DelayingLoader(2000));
        try {
            cache.getAllWithLoader(Arrays.asList("key1"), null);
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
            keys.add(Integer.valueOf(i));
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
        assertNull(cache.get("key1put"));
        assertNull(cache.get("key1putQuiet"));

        cache.put(new Element("key2put", "value1"));
        cache.put(new Element("key2putQuiet", "value1"));
        assertNull(cache.get("key2put"));
        assertNull(cache.get("key2putQuiet"));
    }


    /**
     * Run testConcurrentPutsAreConsistent() repeatedly for 50 times to shake out issues that happen rarely.
     */
    @Test
    public void testConcurrentPutsAreConsistentRepeatedly() throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            manager.removalAll();
            testConcurrentPutsAreConsistent();
        }
    }

    /**
     * Shows a consistency problem as reported against 1.6.0.
     * <p/>
     * Does not happen when not using DiskStore
     * Putting synchronized on put/get on cache fixes it
     * Only happens when the Element is retrieved from the DiskStore. Debugging shows
     * that the problem is caused by puts not getting through or coming in the wrong order
     * Putting synchronized on MemoryStore.put() fixes the issue. That is the applied fix.
     * <p/>
     * The exact cause is unknown but the behaviour of ConcurrentHashMap is suspected.
     */
    @Test
    public void testConcurrentPutsAreConsistent() throws InterruptedException {
        Cache cache = new Cache("someName", 100, true, true, 0, 0);
        manager.addCache(cache);

        cache.setStatisticsEnabled(true);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 5000; i++) {
            executor.execute(new CacheTestRunnable(cache, String.valueOf(i)));
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals("Failures: ", 0, CacheTestRunnable.FAILURES.size());
        assertEquals(5000, cache.getStatistics().getCacheHits());

    }

    /**
     * A runnable that sets 5 times in a row then calls get and checks it is the last value set
     */
    private static final class CacheTestRunnable implements Runnable {
        static final List FAILURES = new ArrayList();

        private final Ehcache cache;
        private final String key;

        private CacheTestRunnable(Ehcache cache, String key) {
            this.cache = cache;
            this.key = key;
        }

        public void run() {
            setValue("new value");
            setValue("new value2");
            setValue("new value3");
            setValue("new value4");
            setValue("new value5");

            Element element = cache.get(key);
            String value = element.getValue().toString();
            boolean result = value.equals("new value5");
            if (!result) {
                LOG.info("key is: " + key + " value: " + value + " version: " + element.getVersion());
                FAILURES.add("key is: " + key + " value: " + value);
            }
        }

        private void setValue(String valueToSet) {
            cache.put(new Element(key, valueToSet));
        }

    }

    /**
     * test cache clones do not have same statistics
     *
     * @throws Exception
     */
    @Test
    public void testCloneCompleteness() throws Exception {
        final AtomicBoolean lastValue = new AtomicBoolean();
        Cache cache = new Cache("testGetMemoryStore", 10, false, false, 100,
                200);
        PropertyChangeListener changeListener = new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("Disabled")) {
                    LOG.info("" + evt.getSource());
                    lastValue.set((Boolean) evt.getNewValue());
                }
            }
        };
        cache.addPropertyChangeListener(changeListener);
        Cache clone = cache.clone();
        clone.setName("testGetMemoryStoreClone");
        manager.addCache(cache);
        manager.addCache(clone);

        cache.setStatisticsEnabled(true);
        clone.setStatisticsEnabled(true);

        assertFalse(cache.getGuid().equals(clone.getGuid()));

        // validate updating the statistics of one cache does NOT affect a
        // cloned one
        cache.get("notFoundKey");
        assertEquals(1, cache.getStatistics().getCacheMisses());
        assertEquals(0, clone.getStatistics().getCacheMisses());

        cache.setDisabled(true);
        clone.setDisabled(true);
        clone.setDisabled(false);

        assertFalse(cache.getGuid().equals(clone.getGuid()));
        assertThat(getPropertyChangeSupport(cache), not(sameInstance(getPropertyChangeSupport(clone))));
        assertThat(lastValue.get(), is(false));
        clone.removePropertyChangeListener(changeListener);
        cache.setDisabled(false);
        cache.setDisabled(true);
        assertThat(lastValue.get(), equalTo(true));
        clone.setDisabled(true);
        clone.setDisabled(false);
        assertThat(lastValue.get(), equalTo(true));
    }

    private PropertyChangeSupport getPropertyChangeSupport(final Cache cache) throws Exception {
        PropertyChangeSupport propertyChangeSupport = null;
        Field field = Cache.class.getDeclaredField("propertyChangeSupport");
        field.setAccessible(true);
        propertyChangeSupport = (PropertyChangeSupport) field.get(cache);

        return propertyChangeSupport;
    }


    /**
     * Checks that notification only happens once when clearOnFlush is false i.e.
     * The impact of this is that there will be one copy in each store.
     */
    @Test
    public void testRemoveListenersCalledOnce() {
        Cache cache = manager.getCache("sampleCache1");
        RemoveCountingListener l = new RemoveCountingListener();
        cache.getCacheEventNotificationService().registerListener(l);

        cache.getCacheConfiguration().setDiskPersistent(true);
        cache.getCacheConfiguration().setClearOnFlush(false);

        Element element = new Element("foo", "bar", 1L);

        cache.put(element);

        cache.flush();

        cache.remove("foo");

        assertEquals(1, l.count);
        assertEquals(element, l.element);
    }

    /**
     * test listener
     */
    private static class RemoveCountingListener implements CacheEventListener {

        private int count;
        private Element element;

        public void notifyElementRemoved(Ehcache cache, Element element)
                throws CacheException {
            count++;
            this.element = element;
        }

        public void dispose() {

        }

        public void notifyElementEvicted(Ehcache cache, Element element) {

        }

        public void notifyElementExpired(Ehcache cache, Element element) {

        }

        public void notifyElementPut(Ehcache cache, Element element)
                throws CacheException {

        }

        public void notifyElementUpdated(Ehcache cache, Element element)
                throws CacheException {
        }

        public void notifyRemoveAll(Ehcache cache) {

        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    /**
     * Checks that TTL of Long.MAX_VALUE means value never expires.
     * See EHC-432.
     */
    @Test
    public void testMaxLongTTLIsEternal() {
        long maxLiveTime = Long.MAX_VALUE;

        final Cache cache = new Cache("bla", 5000, false, false, maxLiveTime, 0);
        final CacheManager cacheManager = CacheManager.create();

        cacheManager.addCache(cache);

        Element e = new Element("key", "bla");
        cache.put(e);

        // theoretically we should wait a long time here but the error from EHC-432
        // has already shown up in the put.  And we don't have time to wait forever
        // to verify this.

        Element e2 = cache.get("key");
        assertNotNull(e2);
    }

    /**
     * Checks that TTL of Integer.MAX_VALUE means value never expires.
     * See EHC-432.
     */
    @Test
    public void testMaxIntegerTTLIsEternal() {
        long maxLiveTime = Integer.MAX_VALUE;

        final Cache cache = new Cache("bla", 5000, false, false, maxLiveTime, 0);
        final CacheManager cacheManager = CacheManager.create();

        cacheManager.addCache(cache);

        Element e = new Element("key", "bla");
        cache.put(e);

        // theoretically we should wait a long time here but the error from EHC-432
        // has already shown up in the put.  And we don't have time to wait forever
        // to verify this.

        Element e2 = cache.get("key");
        assertNotNull(e2);
    }

    /**
     * Versioning is broken when updates are done. If an Element constructor specifying a version is used, it should
     * be preserved.
     * <p/>
     * See EHC-666
     */
    @Test
    public void testVersioningShouldBePreserved() {

        CacheManager cacheManager = CacheManager.getInstance();
        cacheManager.addCache(new Cache("mltest", 50, MemoryStoreEvictionPolicy.LRU, true, null, true, 0, 0, false, 120, null, null, 0, 2, false));
        Cache cache = cacheManager.getCache("mltest");

        Element a = new Element("a key", "a value", 1L);
        cache.put(a);
        Element aAfter = cache.get("a key");
        assertEquals(1L, aAfter.getVersion());

        LOG.info("Element after first put with specific version." + aAfter);

        //A put where the version is not explicitly mentioned, gets a default version of 1.
        Element b = new Element("a key", "a value");
        cache.put(b);
        Element bAfter = cache.get("a key");
        assertEquals(1L, bAfter.getVersion());
        LOG.info("Element after second put. No version." + bAfter);

        //Explicit Version should be preserved
        Element c = new Element("a key", "a value", 3L);
        cache.put(c);
        LOG.info("Element after third put with specific version." + cache.get("a key"));
        Element cAfter = cache.get("a key");
        assertEquals(3L, cAfter.getVersion());

    }

    /**
     * When bulkOperations are working fine
     *
     * @throws CacheException
     * @throws InterruptedException
     */
    @Test
    public void testBulkOperations() throws CacheException, InterruptedException {
        Cache cache = new Cache("cache", 1000, true, false, 100000, 200000, false, 1);
        manager.addCache(cache);

        int numOfElements = 100;
        Set<Element> elements = new HashSet<Element>();
        for(int i = 0; i < numOfElements; i++){
            elements.add(new Element("key" + i, "value" + i));
        }
        cache.putAll(elements);
        assertEquals(numOfElements, cache.getSize());

        Set keySet1 = new HashSet<String>();
        for(int i = 0; i < numOfElements; i++){
            keySet1.add("key"+i);
        }

        Map<Object, Element> rv = cache.getAll(keySet1);
        assertEquals(numOfElements, rv.size());

        for(Element element : rv.values()){
            assertTrue(elements.contains(element));
        }

        Collection<Element> values = rv.values();
        for(Element element : elements){
            assertTrue(values.contains(element));
        }

        Random rand = new Random();
        Set keySet2 = new HashSet<String>();
        for(int i = 0; i < numOfElements/2; i++){
            keySet2.add("key" + rand.nextInt(numOfElements));
        }

        rv = cache.getAll(keySet2);
        assertEquals(keySet2.size(), rv.size());

        for(Element element : rv.values()){
            assertTrue(elements.contains(element));
        }

        assertEquals(keySet2, rv.keySet());

        cache.removeAll(keySet2);
        assertEquals(numOfElements - keySet2.size(), cache.getSize());

        for(Object key : keySet2){
            assertNull(cache.get(key));
        }

        cache.removeAll();
        assertEquals(0, cache.getSize());

        cache.putAll(elements);
        assertEquals(elements.size(), cache.getSize());

        Set keySet3 = new HashSet<String>();
        for(int i = 0; i < numOfElements; i++){
            keySet3.add("key" + 2 * i);
        }
        cache.removeAll(keySet3);
        assertEquals(numOfElements/2, cache.getSize());

        Set keySet4 = new HashSet<String>();
        for(int i = 0; i < 2 * numOfElements; i++){
            keySet4.add("key" + i);
        }

        Map<Object, Element> actual = cache.getAll(keySet4);
        Map<Object, Element> expected = new HashMap<Object, Element>();

        for(int i = 0; i < numOfElements; i++) {
            if(i % 2 == 0) {
                expected.put("key" + i, null);
            } else {
                Element val = actual.get("key" + i);
                assertNotNull("val for key" + i + " is " + val, val);
                expected.put("key" + i, val);
            }
        }

        for(int i = numOfElements; i < 2 * numOfElements; i++) {
            expected.put("key" + i, null);
        }

        assertEquals(expected, actual);
    }

    static class GetCacheMemorySize implements Callable<Long> {

        private final Ehcache cache;

        public GetCacheMemorySize(Ehcache cache) {
            this.cache = cache;
        }

        public Long call() throws Exception {
            return cache.getMemoryStoreSize();
        }
    }

    static class GetCacheDiskSize implements Callable<Integer> {

        private final Ehcache cache;

        public GetCacheDiskSize(Ehcache cache) {
            this.cache = cache;
        }

        public Integer call() throws Exception {
            return cache.getDiskStoreSize();
        }
    }
}

