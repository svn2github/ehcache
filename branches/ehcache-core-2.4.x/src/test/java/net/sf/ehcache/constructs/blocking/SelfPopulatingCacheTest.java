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

package net.sf.ehcache.constructs.blocking;

import static junit.framework.Assert.assertSame;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheTest;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CountingCacheEventListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test cases for the {@link SelfPopulatingCache}.
 *
 * @author Adam Murdoch
 * @author Greg Luck
 * @version $Id$
 */
public class SelfPopulatingCacheTest extends CacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(SelfPopulatingCacheTest.class.getName());

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
    @Override
    @Before
    public void setUp() throws Exception {
        //Skip update checks. Causing an OutOfMemoryError
        System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
        super.setUp();
        manager = new CacheManager();
        cache = manager.getCache("sampleIdlingExpiringCache");
        selfPopulatingCache = new SelfPopulatingCache(cache, new CountingCacheEntryFactory("value"));
        cacheEntryFactoryRequests = 0;
    }

    /**
     * teardown
     */
    @Override
    @After
    public void tearDown() throws Exception {
        if (selfPopulatingCache != null) {
            selfPopulatingCache.removeAll();
        }
        if (manager != null) {
            manager.shutdown();
        }
        super.tearDown();
    }

    /**
     * Tests fetching an entry.
     */
    @Test
    public void testFetch() throws Exception {
        LOG.error(".");

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
        final Object value = new Object();
        final Exception exception = new Exception("Failed.");
        final AtomicBoolean throwException = new AtomicBoolean(true);
        final CacheEntryFactory factory = new CacheEntryFactory() {
            public Object createEntry(final Object key) throws Exception {
                if (throwException.get()) {
                    throw exception;
                } else {
                    return value;
                }
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

        throwException.set(false);
        selfPopulatingCache.setTimeoutMillis(1);
        Element element = null;
        try {
            element = selfPopulatingCache.get("key");
        } catch (LockTimeoutException e) {
            fail("Key should not be locked anymore!");
        }
        assertThat(element, is(notNullValue()));
        assertThat(element.getObjectValue(), is(value));
    }

    /**
     * Tests that an entry is created once only.
     */
    @Test
    public void testCreateOnce() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        final Cache cache = manager.getCache("sampleCacheNoIdle");
        final Ehcache selfPopulatingCache = new SelfPopulatingCache(cache, factory);

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
     * Tests refreshing the entries.
     */
    @Test
    public void testRefreshWithException() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        // Check the value
        String explodingKey = "explode";
        assertSame(value, selfPopulatingCache.get(explodingKey).getObjectValue());
        assertEquals(1, factory.getCount());

        // Refresh
        try {
            selfPopulatingCache.refresh();
            fail("This should have exploded!");
        } catch (CacheException e) {
            assertNotNull(e.getCause());
            assertEquals(e.getCause().getMessage() + " on refresh with key " + explodingKey, e.getMessage());
        }
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
        private final Ehcache cache;
        private final String key;

        private CacheAccessorThread(Ehcache cache, String key) {
            this.cache = cache;
            this.key = key;
        }

        /**
         * Thread run method
         */
        @Override
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
     * Original design behaviour of CacheEntryFactory was that its return was
     * treated as the value of the Element and an Element was constructed on the fly.
     * This meant the CacheEntryFactory could not set Element properties, for instance.
     * As of SVN 950, if the returned Object is an Element then this is directly
     * used in the Cache.  This test checked this behaviour by setting the
     * Element version number to an improbable value that is then checked
     */
    @Test
    public void testCacheEntryFactoryReturningElementMake() throws Exception {
        final long specialVersionNumber = 54321L;
        final CacheEntryFactory elementReturningFactory = new CacheEntryFactory() {
            public Object createEntry(final Object key) throws Exception {
                Element e = new Element(key, "V_" + key);
                e.setVersion(specialVersionNumber);
                return e;
            }
        };
        selfPopulatingCache = new SelfPopulatingCache(cache, elementReturningFactory);
        Element e = null;
        e = selfPopulatingCache.get("key1");
        assertEquals("V_key1", e.getValue());
        assertEquals(specialVersionNumber, e.getVersion());
        e = selfPopulatingCache.get("key2");
        assertEquals("V_key2", e.getValue());
        assertEquals(specialVersionNumber, e.getVersion());
        assertEquals(2, selfPopulatingCache.getSize());
    }

    /**
     * See {@link #testCacheEntryFactoryReturningElementMake}
     * this test ensures the Refresh functionality works
     */
    @Test
    public void testCacheEntryFactoryReturningElementRefresh() throws Exception {
        final long specialVersionNumber = 54321L;
        final CacheEntryFactory elementReturningFactory = new CacheEntryFactory() {
            public Object createEntry(final Object key) throws Exception {
                Element e = new Element(key, "V_" + key);
                e.setVersion(specialVersionNumber);
                return e;
            }
        };
        selfPopulatingCache = new SelfPopulatingCache(cache, elementReturningFactory);
        Element e = null;
        e = selfPopulatingCache.get("key1");
        assertEquals("V_key1", e.getValue());
        assertEquals(specialVersionNumber, e.getVersion());
        e = selfPopulatingCache.get("key2");
        assertEquals("V_key2", e.getValue());
        assertEquals(specialVersionNumber, e.getVersion());
        assertEquals(2, selfPopulatingCache.getSize());
        selfPopulatingCache.refresh();
        e = selfPopulatingCache.get("key1");
        assertEquals("V_key1", e.getValue());
        assertEquals(specialVersionNumber, e.getVersion());
        e = selfPopulatingCache.get("key2");
        assertEquals("V_key2", e.getValue());
        assertEquals(specialVersionNumber, e.getVersion());
        assertEquals(2, selfPopulatingCache.getSize());
    }

    /**
     * See {@link #testCacheEntryFactoryReturningElementMake}
     * this test ensures the Refresh functionality works
     */
    @Test
    public void testCacheEntryFactoryReturningElementBadKey() throws Exception {
        final CacheEntryFactory elementReturningFactory = new CacheEntryFactory() {
            public Object createEntry(final Object key) throws Exception {
                Object modifiedKey = key.toString() + "XX";
                Element e = new Element(modifiedKey, "V_" + modifiedKey);
                return e;
            }
        };
        selfPopulatingCache = new SelfPopulatingCache(cache, elementReturningFactory);
        try {
            selfPopulatingCache.get("key");
            fail("Should fail because key was changed");
        } catch (final Exception e) {
            Thread.sleep(20);
            // Check the error
            assertEquals("Could not fetch object for cache entry with key \"key\".", e.getMessage());
        }
    }


    @Test
    public void testRefreshElement() throws Exception {
        final IncrementingCacheEntryFactory factory = new IncrementingCacheEntryFactory();
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        Element e1 = selfPopulatingCache.get("key1");
        Element e2 = selfPopulatingCache.get("key2");
        assertEquals(2, selfPopulatingCache.getSize());
        assertEquals(2, factory.getCount());
        assertEquals(Integer.valueOf(1), e1.getValue());
        assertEquals(Integer.valueOf(2), e2.getValue());

        // full refresh
        selfPopulatingCache.refresh();
        e1 = selfPopulatingCache.get("key1");
        e2 = selfPopulatingCache.get("key2");
        assertEquals(2, selfPopulatingCache.getSize());
        assertEquals(4, factory.getCount());
        //we cannot be sure which order key1 or key2 gets refreshed,
        //as the implementation makes no guarantee over the sequence
        //of the refresh; all we can be sure of is that between
        //them key1&2 must have the values 3 & 4
        int e1i = ((Integer) e1.getValue()).intValue();
        int e2i = ((Integer) e2.getValue()).intValue();
        assertTrue(((e1i == 3) && (e2i == 4)) || ((e1i == 4) && (e2i == 3)));

        // single element refresh
        selfPopulatingCache.get("key2");
        Element e2r = selfPopulatingCache.refresh("key2");
        assertEquals(2, selfPopulatingCache.getSize());
        assertEquals(5, factory.getCount());
        assertNotNull(e2r);
        assertEquals("key2", e2r.getKey());
        assertEquals(Integer.valueOf(5), e2r.getValue());

        // additional element
        Element e3 = selfPopulatingCache.get("key3");
        assertEquals(3, selfPopulatingCache.getSize());
        assertEquals(6, factory.getCount());
        assertNotNull(e3);
        assertEquals("key3", e3.getKey());
        assertEquals(Integer.valueOf(6), e3.getValue());

        // full refresh
        selfPopulatingCache.refresh();
        assertEquals(3, selfPopulatingCache.getSize());
        assertEquals(9, factory.getCount());
    }

    @Test
    public void testRefreshAbsentElement() throws Exception {
        final IncrementingCacheEntryFactory factory = new IncrementingCacheEntryFactory();
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        selfPopulatingCache.get("key1");
        selfPopulatingCache.get("key2");
        assertEquals(2, selfPopulatingCache.getSize());
        assertEquals(2, factory.getCount());

        // full refresh
        selfPopulatingCache.refresh();
        assertEquals(2, selfPopulatingCache.getSize());
        assertEquals(4, factory.getCount());

        // single element refresh which is not in the cache
        Element e3 = selfPopulatingCache.refresh("key3");
        assertEquals(3, selfPopulatingCache.getSize());
        assertEquals(5, factory.getCount());
        assertNotNull(e3);
        assertEquals("key3", e3.getKey());
        assertEquals(Integer.valueOf(5), e3.getValue());
    }

    @Test
    public void testRefreshQuietly() throws Exception {
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory("value");
        CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();
        CountingCacheEventListener.resetCounters();
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        //check initial conditions on counters
        assertEquals(0, CountingCacheEventListener.getCacheElementsPut(cache).size());
        assertEquals(0, CountingCacheEventListener.getCacheElementsUpdated(cache).size());

        Element e1 = selfPopulatingCache.get("key1");
        Element e2 = selfPopulatingCache.get("key2");
        assertEquals(2, factory.getCount());
        assertEquals(2, CountingCacheEventListener.getCacheElementsPut(cache).size());
        assertEquals(0, CountingCacheEventListener.getCacheElementsUpdated(cache).size());
        long lastUpdateTime1 = e1.getLastUpdateTime();
        long lastUpdateTime2 = e2.getLastUpdateTime();

        //wait a little so creation time to allow CPU clock to advance
        Thread.sleep(100L);

        // full refresh
        selfPopulatingCache.refresh();
        assertEquals(4, factory.getCount());
        assertEquals(2, CountingCacheEventListener.getCacheElementsPut(cache).size());
        assertEquals(0, CountingCacheEventListener.getCacheElementsUpdated(cache).size());
        e1 = selfPopulatingCache.get("key1");
        e2 = selfPopulatingCache.get("key2");
        assertTrue("getLastUpdateTime() should be the same", lastUpdateTime1 == e1.getLastUpdateTime());
        assertTrue("getLastUpdateTime() should be the same", lastUpdateTime2 == e2.getLastUpdateTime());
        lastUpdateTime2 = e2.getLastUpdateTime();

        //wait a little to allow CPU clock to advance
        Thread.sleep(100L);

        // single element refresh
        e2 = selfPopulatingCache.refresh("key2");
        assertEquals(5, factory.getCount());
        assertEquals(2, CountingCacheEventListener.getCacheElementsPut(cache).size());
        assertEquals(0, CountingCacheEventListener.getCacheElementsUpdated(cache).size());
        assertTrue("getLastUpdateTime() should be the same", lastUpdateTime2 == e2.getLastUpdateTime());
    }

    @Test
    public void testRefreshNoisily() throws Exception {
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory("value");
        CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();
        CountingCacheEventListener.resetCounters();
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        //check initial conditions on counters
        assertEquals(0, CountingCacheEventListener.getCacheElementsPut(cache).size());
        assertEquals(0, CountingCacheEventListener.getCacheElementsUpdated(cache).size());

        Element e1 = selfPopulatingCache.get("key1");
        Element e2 = selfPopulatingCache.get("key2");
        assertEquals(2, factory.getCount());
        assertEquals(2, CountingCacheEventListener.getCacheElementsPut(cache).size());
        assertEquals(0, CountingCacheEventListener.getCacheElementsUpdated(cache).size());
        long lastUpdateTime1 = e1.getLastUpdateTime();
        long lastUpdateTime2 = e2.getLastUpdateTime();

        //wait a little so creation time to allow CPU clock to advance
        Thread.sleep(100L);

        // full refresh
        selfPopulatingCache.refresh(false);
        assertEquals(4, factory.getCount());
        assertEquals(2, CountingCacheEventListener.getCacheElementsPut(cache).size());
        assertEquals(2, CountingCacheEventListener.getCacheElementsUpdated(cache).size());
        e1 = selfPopulatingCache.get("key1");
        e2 = selfPopulatingCache.get("key2");
        assertEquals(4, factory.getCount());
        assertEquals(2, CountingCacheEventListener.getCacheElementsPut(cache).size());
        assertEquals(2, CountingCacheEventListener.getCacheElementsUpdated(cache).size());
        assertFalse("getLastUpdateTime() should not be the same (" + lastUpdateTime1 + ")", lastUpdateTime1 == e1.getLastUpdateTime());
        assertFalse("getLastUpdateTime() should not be the same (" + lastUpdateTime2 + ")", lastUpdateTime2 == e2.getLastUpdateTime());
        lastUpdateTime2 = e2.getLastUpdateTime();

        //wait a little to allow CPU clock to advance
        Thread.sleep(100L);

        // single element refresh
        e2 = selfPopulatingCache.refresh("key2", false);
        assertEquals(5, factory.getCount());
        assertEquals(2, CountingCacheEventListener.getCacheElementsPut(cache).size());
        assertEquals(3, CountingCacheEventListener.getCacheElementsUpdated(cache).size());
        assertFalse("getLastUpdateTime() should not be the same (" + lastUpdateTime2 + ")", lastUpdateTime2 == e2.getLastUpdateTime());
    }

    /**
     * Much like CountingCacheEntryFactory, but the value in the Element is
     * incremented on every update, in line with the 'count'
     */
    private class IncrementingCacheEntryFactory implements CacheEntryFactory {
        private int count;

        public Object createEntry(Object key) throws Exception {
            count++;
            return Integer.valueOf(count);
        }

        /**
         * @return number of entries the factory has created.
         */
        public int getCount() {
            return count;
        }
    }
}


