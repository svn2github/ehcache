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

package net.sf.ehcache.constructs.blocking;

import static junit.framework.Assert.assertSame;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheTest;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;


/**
 * Test cases for the {@link SelfPopulatingCache}.
 *
 * @author Adam Murdoch
 * @author Greg Luck
 * @version $Id$
 */
public class SelfPopulatingCacheTest extends CacheTest {

    private static final Logger LOG = Logger.getLogger(SelfPopulatingCache.class.getName());

    /**
     * Shared with subclass
     */
    protected CacheManager manager;
    /**
     * Shared with subclass
     */
    protected SelfPopulatingCache selfPopulatingCache;
    /**
     * Shared with subclass
     */
    protected Ehcache cache;

    /**
     * Number of factory requests
     */
    protected volatile int cacheEntryFactoryRequests;

    /**
     * Load up the test cache
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        manager = new CacheManager();
        cache = manager.getCache("sampleIdlingExpiringCache");
        selfPopulatingCache = new SelfPopulatingCache(cache, new CountingCacheEntryFactory("value"));
        cacheEntryFactoryRequests = 0;
    }

    /**
     * teardown
     */
    @After
    public void tearDown() throws Exception {
        selfPopulatingCache.removeAll();
        manager.shutdown();
        super.tearDown();
    }

    /**
     * Tests fetching an entry.
     */
    @Test
    public void testFetch() throws Exception {

        // Lookup
        final Element element = selfPopulatingCache.get("key");
        assertEquals("value", element.getValue());
    }

    /**
     * Tests fetching an unknown entry.
     */
    @Test
    public void testFetchUnknown() throws Exception {
        final CacheEntryFactory factory = new CountingCacheEntryFactory(null);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        // Lookup
        assertNull(cache.get("key"));
    }

    /**
     * Tests when fetch fails.
     */
    @Test
    public void testFetchFail() throws Exception {
        final Exception exception = new Exception("Failed.");
        final CacheEntryFactory factory = new CacheEntryFactory() {
            public Object createEntry(final Object key) throws Exception {
                throw exception;
            }
        };
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        // Lookup
        try {
            selfPopulatingCache.get("key");
            fail();
        } catch (final Exception e) {
            Thread.sleep(20);
            // Check the error
            assertEquals("Could not fetch object for cache entry with key \"key\".", e.getMessage());
        }
    }

    /**
     * Tests that an entry is created once only.
     */
    @Test
    public void testCreateOnce() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        // Fetch the value several times
        for (int i = 0; i < 5; i++) {
            assertSame(value, selfPopulatingCache.get("key").getObjectValue());
            assertEquals(1, factory.getCount());
        }
    }

    /**
     * Tests refreshing the entries.
     */
    @Test
    public void testRefresh() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        // Check the value
        assertSame(value, selfPopulatingCache.get("key").getObjectValue());
        assertEquals(1, factory.getCount());

        // Refresh
        selfPopulatingCache.refresh();
        assertEquals(2, factory.getCount());

        // Check the value
        assertSame(value, selfPopulatingCache.get("key").getObjectValue());
        assertEquals(2, factory.getCount());

    }

    /**
     * Tests that the current thread, which gets renamed when it enters a SelfPopulatingCache, comes out with
     * its old name.
     */
    @Test
    public void testThreadNaming() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        String originalThreadName = Thread.currentThread().getName();

        // Check the value
        selfPopulatingCache.get("key");
        assertEquals(originalThreadName, Thread.currentThread().getName());

        // Refresh
        selfPopulatingCache.refresh();
        assertEquals(originalThreadName, Thread.currentThread().getName());

        // Check the value with null key
        selfPopulatingCache.get(null);
        assertEquals(originalThreadName, Thread.currentThread().getName());


    }

    /**
     * Tests discarding little used entries.
     * <cache name="sampleIdlingExpiringCache"
     * maxElementsInMemory="1"
     * eternal="false"
     * timeToIdleSeconds="2"
     * timeToLiveSeconds="5"
     * overflowToDisk="true"
     * />
     */
    @Test
    public void testDiscardLittleUsed() throws Exception {
        final CacheEntryFactory factory = new CountingCacheEntryFactory("value");
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);


        selfPopulatingCache.get("key1");
        selfPopulatingCache.get("key2");
        assertEquals(2, selfPopulatingCache.getSize());
        selfPopulatingCache.refresh();
        assertEquals(2, selfPopulatingCache.getSize());
        Thread.sleep(2020);

        //Will be two, because counting expired elements
        assertEquals(2, selfPopulatingCache.getSize());

        // Check the cache
        selfPopulatingCache.removeAll();
        assertEquals(0, selfPopulatingCache.getSize());
    }

    /**
     * Tests discarding little used entries, where refreshing is slow.
     * <cache name="sampleIdlingExpiringCache"
     * maxElementsInMemory="1"
     * eternal="false"
     * timeToIdleSeconds="2"
     * timeToLiveSeconds="5"
     * overflowToDisk="true"
     * />
     */
    @Test
    public void testDiscardLittleUsedSlow() throws Exception {
        final CacheEntryFactory factory = new CacheEntryFactory() {
            public Object createEntry(final Object key) throws Exception {
                Thread.sleep(200);
                return key;
            }
        };
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);
    }


    /**
     * Expected: If multiple threads try to retrieve the same key from a
     * SelfPopulatingCache at the same time, and that key is not yet in the cache,
     * one thread obtains the lock for that key and uses the CacheEntryFactory to
     * generate the cache entry and all other threads wait on the lock.
     * Any and all threads which timeout while waiting for this lock should fail
     * to acquire the lock for that key and throw an exception.
     * <p/>
     * This thread tests for this by having several threads to a cache "get" for
     * the same key, allowing one to acquire the lock and the others to wait.  The
     * one that acquires the lock and attempts to generate the cache entry for the
     * key waits for a period of time long enough to allow all other threads to
     * timeout waiting for the lock.  Any thread that succeeds in acquiring the lock,
     * including the first to do so, increment a counter when they begin creating
     * the cache entry using the CacheEntryFactory.  It is expected that this
     * counter will only be "1" after all threads complete since all but the
     * first to acquire it should timeout and throw exceptions.
     * <p/>
     * We then test that a thread that comes along later increments the counter.
     */
    @Test
    public void testSelfPopulatingBlocksWithTimeoutSetNull() throws InterruptedException {
        selfPopulatingCache = new SelfPopulatingCache(new Cache("TestCache", 50, false, false, 0, 0), new NullCachePopulator());
        selfPopulatingCache.setTimeoutMillis(200);
        manager.addCache(selfPopulatingCache);

        CacheAccessorThread[] cacheAccessorThreads = new CacheAccessorThread[10];

        for (int i = 0; i < cacheAccessorThreads.length; i++) {
            cacheAccessorThreads[i] = new CacheAccessorThread(selfPopulatingCache, "key1");
            cacheAccessorThreads[i].start();
            // Do a slight delay here so that all the timeouts
            // don't happen simultaneously - this is key
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
                //
            }
        }

        //All of the others should have timed out. The first thread will have returned null.
        // This thread should be able to have a go, thus setting the count to 2
        Thread.sleep(1000);
        Thread lateThread = new CacheAccessorThread(selfPopulatingCache, "key1");
        lateThread.start();
        lateThread.join();

        assertEquals("Too many cacheAccessorThreads tried to create selfPopulatingCache entry for key1",
                2, cacheEntryFactoryRequests);
    }


    /**
     * Creating 11 Threads which attempt to get a null entry will result, eventually, in 11
     * calls to the CacheEntryFactory
     *
     * @throws InterruptedException
     */
    @Test
    public void testSelfPopulatingBlocksWithoutTimeoutSetNull() throws InterruptedException {
        selfPopulatingCache = new SelfPopulatingCache(new Cache("TestCache", 50, false, false, 0, 0), new NullCachePopulator());
        //selfPopulatingCache.setTimeoutMillis(200);
        manager.addCache(selfPopulatingCache);

        CacheAccessorThread[] cacheAccessorThreads = new CacheAccessorThread[10];

        for (int i = 0; i < cacheAccessorThreads.length; i++) {
            cacheAccessorThreads[i] = new CacheAccessorThread(selfPopulatingCache, "key1");
            cacheAccessorThreads[i].start();
            // Do a slight delay here so that all the timeouts
            // don't happen simultaneously - this is key
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
                //
            }
        }

        //All of the others should have timed out. The first thread will have returned null.
        // This thread should be able to have a go, thus setting the count to 2
        Thread.sleep(12000);
        Thread lateThread = new CacheAccessorThread(selfPopulatingCache, "key1");
        lateThread.start();
        lateThread.join();

        assertEquals("The wrong number of cacheAccessorThreads tried to create selfPopulatingCache entry for key1",
                11, cacheEntryFactoryRequests);
    }

    /**
     * Creating 11 Threads which attempt to get a non-null entry will result in 1
     * call to the CacheEntryFactory
     *
     * @throws InterruptedException
     */
    @Test
    public void testSelfPopulatingBlocksWithoutTimeoutSetNonNull() throws InterruptedException {
        selfPopulatingCache = new SelfPopulatingCache(new Cache("TestCache", 50, false, false, 0, 0),
                new NonNullCachePopulator());
        //selfPopulatingCache.setTimeoutMillis(200);
        manager.addCache(selfPopulatingCache);

        CacheAccessorThread[] cacheAccessorThreads = new CacheAccessorThread[10];

        for (int i = 0; i < cacheAccessorThreads.length; i++) {
            cacheAccessorThreads[i] = new CacheAccessorThread(selfPopulatingCache, "key1");
            cacheAccessorThreads[i].start();
            // Do a slight delay here so that all the timeouts
            // don't happen simultaneously - this is key
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {
                //
            }
        }

        //All of the others should have timed out. The first thread will have returned null.
        // This thread should be able to have a go, thus setting the count to 2
        Thread.sleep(2000);
        Thread lateThread = new CacheAccessorThread(selfPopulatingCache, "key1");
        lateThread.start();
        lateThread.join();

        assertEquals("The wrong number of cacheAccessorThreads tried to create selfPopulatingCache entry for key1",
                1, cacheEntryFactoryRequests);
    }


    /**
     * A thread that accesses a selfpopulating cache
     */
    private final class CacheAccessorThread extends Thread {
        private Ehcache cache;
        private String key;

        private CacheAccessorThread(Ehcache cache, String key) {
            this.cache = cache;
            this.key = key;
        }

        /**
         * Thread run method
         */
        public void run() {
            try {
                cache.get(key);
            } catch (Exception e) {
                LOG.info("Exception: " + e.getMessage());
            }
        }
    }

    /**
     * A cache entry factory that sleeps beyond the lock timeout
     */
    private class NullCachePopulator implements CacheEntryFactory {

        public Object createEntry(Object key) throws Exception {
            cacheEntryFactoryRequests++;
            Thread.sleep(1000);
            return null;
        }
    }


    /**
     * A cache entry factory that sleeps beyond the lock timeout
     */
    private class NonNullCachePopulator implements CacheEntryFactory {

        public Object createEntry(Object key) throws Exception {
            cacheEntryFactoryRequests++;
            Thread.sleep(1000);
            return "value";
        }
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
            //the disk store backs up on the laptop. 
            LOG.info("OutOfMemoryError: " + e.getMessage() + " " + i);
            fail();
        }
    }
}


