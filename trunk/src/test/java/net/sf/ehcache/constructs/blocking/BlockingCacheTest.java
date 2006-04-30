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

package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.constructs.valueobject.KeyValuePair;
import net.sf.ehcache.constructs.concurrent.Mutex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Test cases for the {@link BlockingCache}.
 *
 * @author Adam Murdoch
 * @author Greg Luck
 * @version $Id$
 */
public class BlockingCacheTest extends AbstractCacheTest {
    private static final Log LOG = LogFactory.getLog(BlockingCacheTest.class.getName());
    private BlockingCacheManager manager;
    private BlockingCache blockingCache;

    /**
     * Load up the test cache
     */
    protected void setUp() throws Exception {
        super.setUp();
        manager = new BlockingCacheManager();
        blockingCache = manager.getCache("sampleIdlingExpiringCache");
    }

    /**
     * teardown
     */
    protected void tearDown() throws Exception {
        blockingCache.clear();
        super.tearDown();
    }

    /**
     * Tests adding and looking up an entry.
     */
    public void testAddEntry() throws Exception {
        final String key = "key";
        final String value = "value";

        // Check the cache is empty
        assertEquals(0, blockingCache.getKeys().size());

        // Put the entry
        blockingCache.put(key, value);

        // Check there is a single entry
        assertEquals(1, blockingCache.getKeys().size());
        assertTrue(blockingCache.getKeys().contains(key));
        final Object actualValue = blockingCache.get(key);
        assertSame(value, actualValue);

    }

    /**
     * Tests that getting entries matches a list of known entries
     */
    public void testGetEntries() throws Exception {
        Cache cache = blockingCache.getCache();
        for (int i = 0; i < 100; i++) {
            cache.put(new Element(new Integer(i), "value" + i));
        }
        List entries = blockingCache.getEntries();
        assertEquals(100, entries.size());
        Map map = new HashMap();
        for (int i = 0; i < 100; i++) {
            KeyValuePair keyValuePair = (KeyValuePair) entries.get(i);
            map.put(keyValuePair.getKey(), keyValuePair.getValue());
        }
        for (int i = 0; i < 100; i++) {
            Serializable value = (Serializable) map.get(new Integer(i));
            assertEquals("value" + i, value);
        }
    }

    /**
     * The design of the BlockingCache threading uses a small amount of memory per entry.
     * This test checks that it is not excessive.
     *
     * Profiler testing reveals 24 bytes are used per Mutex added to the HashMap
     * 
     * @throws InterruptedException
     */
    public void testMutexSize() throws InterruptedException {

        long startingMemory = measureMemoryUse();
        Map map = new HashMap();
        for (int i = 0; i < 100000; i++) {
            map.put("" + i, new Mutex());
        }
        long endingMemory = measureMemoryUse();
        long memoryUsed = endingMemory - startingMemory;
        LOG.info("Memory used: " + memoryUsed);
        assertTrue(memoryUsed < 10000000);
    }



    /**
     * Tests looking up a missing entry, then adding it.
     */
    public void testAddMissingEntry() throws Exception {

        // Make sure the entry does not exist
        assertNull(blockingCache.get("key"));

        // Put the entry
        final String value = "value";
        blockingCache.put("key", value);

        // Check the entry is in the cache
        assertEquals(1, blockingCache.getKeys().size());
        assertSame(value, blockingCache.get("key"));

    }

    /**
     * Tests looking up a missing entry, then marks it as unknown.
     */
    public void testUnknownEntry() throws Exception {
        // Make sure the entry does not exist
        assertNull(blockingCache.get("key"));
        // Put the entry
        blockingCache.put("key", null);
        assertEquals(0, blockingCache.getKeys().size());
    }

    /**
     * Tests adding and removing an entry.
     */
    public void testRemoveEntry() throws Exception {
        final String key = "key";
        final String value = "value";

        // Add entry and make sure it's there
        blockingCache.put(key, value);
        final Object actualValue = blockingCache.get(key);
        assertSame(value, actualValue);
        // Remove the entry and make sure its gone
        blockingCache.put(key, null);
        assertEquals(0, blockingCache.getKeys().size());

    }

    /**
     * Tests clearing the cache
     */
    public void testClear() throws Exception {
        manager = new BlockingCacheManager();
        blockingCache = manager.getCache("sampleCacheNotEternalButNoIdleOrExpiry");
        // Add some entries
        blockingCache.put("key1", "value1");
        blockingCache.put("key2", "value2");
        blockingCache.put("key3", "value2");
        assertEquals(3, blockingCache.getKeys().size());

        // Clear the cache
        blockingCache.clear();
        assertEquals(0, blockingCache.getKeys().size());
    }


    /**
     * Thrashes a BlockingCache and looks for liveness problems
     * Note. These timings are without logging. Turn logging off to run this test.
     */
    public void testThrashBlockingCache() throws Exception {
        blockingCache = new BlockingCache("sampleCache1");
        long duration = thrashCache(blockingCache, 50, 400L, 1000L);
        LOG.debug("Thrash Duration:" + duration);
    }

    /**
     * Thrashes an NonScalableBlockingCache and looks for liveness problems
     * This test requires much large values than for the {@link BlockingCache},
     * demonstrating the scalability problems with it.
     * Note. These timings are without logging. Turn logging off to run this test.
     */
    public void testThrashNonScalableBlockingCache() throws Exception {
        NonScalableBlockingCache nonScalableBlockingCache = new NonScalableBlockingCache("sampleCache1");
        long duration = thrashCache(nonScalableBlockingCache, 5, 1000L, 5000L);
        LOG.debug("Thrash Duration:" + duration);
    }

    /**
     * This method tries to get the cache to slow up.
     * It creates 300 threads, does blocking gets and monitors the liveness right the way through
     */
    private long thrashCache(final BlockingCache cache, final int numberOfThreads,
                             final long liveness, final long retrievalTime) throws Exception {
        StopWatch stopWatch = new StopWatch();

        // Create threads that do gets
        final List executables = new ArrayList();
        for (int i = 0; i < numberOfThreads; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    for (int i = 0; i < 10; i++) {
                        final String key = "key" + i;
                        Serializable value = cache.get(key);
                        checkLiveness(cache, liveness);
                        if (value == null) {
                            cache.put(key, "value" + i);
                        }
                        //The key will be in. Now check we can get it quickly
                        checkRetrievalOnKnownKey(cache, retrievalTime, key);
                    }
                }
            };
            executables.add(executable);
        }

        runThreads(executables);
        cache.clear();
        return stopWatch.getElapsedTime();
    }

    /**
     * Checks that the liveness method returns in less than a given amount of time.
     * liveness() is a method that simply returns a String. It should be very fast. It can be
     * delayed because it is a synchronized method, and must acquire an object lock before continuing
     * The old blocking cache was taking up to several minutes in production
     *
     * @param cache a BlockingCache
     */
    private void checkLiveness(BlockingCache cache, long liveness) {
        StopWatch stopWatch = new StopWatch();
        cache.liveness();
        long measuredLiveness = stopWatch.getElapsedTime();
        assertTrue("liveness is " + measuredLiveness + " but should be less than " + liveness + "ms",
                measuredLiveness < liveness);
    }

    /**
     * Checks that the liveness method returns in less than a given amount of time.
     * liveness() is a method that simply returns a String. It should be very fast. It can be
     * delayed because it is a synchronized method, and must acquire
     * an object lock before continuing. The old blocking cache was taking up to several minutes in production
     *
     * @param cache a BlockingCache
     */
    private void checkRetrievalOnKnownKey(BlockingCache cache, long requiredRetrievalTime, Serializable key)
            throws BlockingCacheException {
        StopWatch stopWatch = new StopWatch();
        cache.get(key);
        long measuredRetrievalTime = stopWatch.getElapsedTime();
        assertTrue("Retrieval time on known key is " + measuredRetrievalTime
                + " but should be less than " + requiredRetrievalTime + "ms",
                measuredRetrievalTime < requiredRetrievalTime);
    }

    /**
     * Runs a set of threads, for a fixed amount of time.
     */
    private void runThreads(final List executables) throws Exception {

        final long endTime = System.currentTimeMillis() + 10000;
        final Throwable[] errors = new Throwable[1];

        // Spin up the threads
        final Thread[] threads = new Thread[executables.size()];
        for (int i = 0; i < threads.length; i++) {
            final Executable executable = (Executable) executables.get(i);
            threads[i] = new Thread() {
                public void run() {
                    try {
                        // Run the thread until the given end time
                        while (System.currentTimeMillis() < endTime) {
                            executable.execute();
                        }
                    } catch (Throwable t) {
                        // Hang on to any errors
                        errors[0] = t;
                    }
                }
            };

            threads[i].start();
        }
        LOG.debug("Started " + threads.length + " threads");

        // Wait for the threads to finish
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }

        // Throw any error that happened
        if (errors[0] != null) {
            throw new Exception("Test thread failed.", errors[0]);
        }
    }

    /**
     * A runnable, that can throw an exception.
     */
    private interface Executable {
        // Executes this object.
        void execute() throws Exception;
    }

}

