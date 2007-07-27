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

package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import net.sf.ehcache.CacheTest;
import net.sf.ehcache.Status;
import net.sf.ehcache.Cache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases for the {@link BlockingCache}.
 *
 * @author Adam Murdoch
 * @author Greg Luck
 * @version $Id$
 */
public class BlockingCacheTest extends CacheTest {
    private static final Log LOG = LogFactory.getLog(BlockingCacheTest.class.getName());
    private BlockingCache blockingCache;

    /**
     * Load up the test cache
     */
    protected void setUp() throws Exception {
        super.setUp();
        Ehcache cache = manager.getCache("sampleIdlingExpiringCache");
        blockingCache = new BlockingCache(cache);
    }

    /**
     * teardown
     */
    protected void tearDown() throws Exception {
        if (manager.getStatus() == Status.STATUS_ALIVE) {
            blockingCache.removeAll();
        }
        super.tearDown();
    }

    /**
     * Tests adding and looking up an entry.
     */
    public void testAddEntry() throws Exception {
        final String key = "key";
        final String value = "value";
        Element element = new Element(key, value);

        // Check the cache is empty
        assertEquals(0, blockingCache.getKeys().size());

        // Put the entry
        blockingCache.put(new Element(key, value));

        // Check there is a single entry
        assertEquals(1, blockingCache.getKeys().size());
        assertTrue(blockingCache.getKeys().contains(key));
        final Element returnedElement = blockingCache.get(key);
        assertEquals(element, returnedElement);

    }

    /**
     * Tests that getting entries matches a list of known entries
     */
    public void testGetEntries() throws Exception {
        Ehcache cache = blockingCache.getCache();
        for (int i = 0; i < 100; i++) {
            cache.put(new Element(new Integer(i), "value" + i));
        }
        List keys = blockingCache.getKeys();
        List elements = new ArrayList();
        for (int i = 0; i < keys.size(); i++) {
            Object key = keys.get(i);
            elements.add(blockingCache.get(key));
        }
        assertEquals(100, elements.size());
        Map map = new HashMap();
        for (int i = 0; i < elements.size(); i++) {
            Element element = (Element) elements.get(i);
            map.put(element.getObjectKey(), element.getObjectValue());
        }
        for (int i = 0; i < 100; i++) {
            Serializable value = (Serializable) map.get(new Integer(i));
            assertEquals("value" + i, value);
        }
    }

    /**
     * Tests looking up a missing entry, then adding it.
     */
    public void testAddMissingEntry() throws Exception {
        Element element = new Element("key", "value");

        // Make sure the entry does not exist
        assertNull(blockingCache.get("key"));

        // Put the entry
        blockingCache.put(element);

        // Check the entry is in the cache
        assertEquals(1, blockingCache.getKeys().size());
        assertEquals(element, blockingCache.get("key"));

    }


    /**
     * Does a second tread block until the first thread puts the entry?
     */
    public void testSecondThreadActuallyBlocks() throws Exception {
        Element element = new Element("key", "value");
        final List threadResults = new ArrayList();

        // Make sure the entry does not exist
        assertNull(blockingCache.get("key"));

        Thread secondThread = new Thread() {
                public void run() {
                    threadResults.add(blockingCache.get("key"));
                }
            };
        secondThread.start();
        assertEquals(0, threadResults.size());

        // Put the entry
        blockingCache.put(element);
        Thread.sleep(30);
        assertEquals(1, threadResults.size());
        assertEquals(element, threadResults.get(0));

        // Check the entry is in the cache
        assertEquals(1, blockingCache.getKeys().size());
        assertEquals(element, blockingCache.get("key"));
    }

    /**
     * Elements with null valuea are not stored in the blocking cache
     */
    public void testUnknownEntry() throws Exception {
        // Make sure the entry does not exist
        assertNull(blockingCache.get("key"));
        // Put the entry
        blockingCache.put(new Element("key", null));
        assertEquals(0, blockingCache.getKeys().size());
    }

    /**
     * Overwriting an Element with an element with a null value effectively removes it from the cache
     */
    public void testRemoveEntry() throws Exception {
        Element element = new Element("key", "value");

        // Add entry and make sure it's there
        blockingCache.put(element);
        assertEquals(element, blockingCache.get("key"));

        // Remove the entry and make sure its gone
        blockingCache.put(new Element("key", null));
        assertEquals(0, blockingCache.getKeys().size());

    }

    /**
     * Tests clearing the cache
     */
    public void testClear() throws Exception {
        Ehcache cache = manager.getCache("sampleCacheNotEternalButNoIdleOrExpiry");
        blockingCache = new BlockingCache(cache);
        // Add some entries
        blockingCache.put(new Element("key1", "value1"));
        blockingCache.put(new Element("key2", "value2"));
        blockingCache.put(new Element("key3", "value2"));
        assertEquals(3, blockingCache.getKeys().size());

        // Clear the cache
        blockingCache.removeAll();
        assertEquals(0, blockingCache.getKeys().size());
    }


    /**
     * Thrashes a BlockingCache and looks for liveness problems
     * Note. These timings are without logging. Turn logging off to run this test.
     */
    public void testThrashBlockingCache() throws Exception {
        Ehcache cache = manager.getCache("sampleCache1");
        blockingCache = new BlockingCache(cache);
        long duration = thrashCache(blockingCache, 50, 500L, 1000L);
        LOG.debug("Thrash Duration:" + duration);
    }

    /**
     * Thrashes a BlockingCache which has a tiny timeout. Should throw
     * a LockTimeoutException caused by queued threads not getting the lock
     * in the required time.
     */
    public void testThrashBlockingCacheTinyTimeout() throws Exception {
        Ehcache cache = manager.getCache("sampleCache1");
        blockingCache = new BlockingCache(cache);
        blockingCache.setTimeoutMillis(1);
        long duration = 0;
        try {
            duration = thrashCache(blockingCache, 50, 400L, 1000L);
            fail();
        } catch (Exception e) {
            assertEquals(LockTimeoutException.class, e.getCause().getClass());
        }
        LOG.debug("Thrash Duration:" + duration);
    }

    /**
     * Thrashes a BlockingCache which has a reasonable timeout. Should work.
     * The old implementation, which had scalability limits, needed 5, 1000L, 5000L to pass
     */
    public void testThrashBlockingCacheReasonableTimeout() throws Exception {
        Ehcache cache = manager.getCache("sampleCache1");
        blockingCache = new BlockingCache(cache);
        blockingCache.setTimeoutMillis((int) (400 * StopWatch.getSpeedAdjustmentFactor()));
        long duration = thrashCache(blockingCache, 50, 400L, (long) (1000L * StopWatch.getSpeedAdjustmentFactor()));
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
                            Object value = cache.get(key);
                            checkLiveness(cache, liveness);
                            if (value == null) {
                                cache.put(new Element(key, "value" + i));
                            }
                            //The key will be in. Now check we can get it quickly
                            checkRetrievalOnKnownKey(cache, retrievalTime, key);
                        }
                    }
                };
                executables.add(executable);
            }

            runThreads(executables);
            cache.removeAll();
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
            throws LockTimeoutException {
        StopWatch stopWatch = new StopWatch();
        cache.get(key);
        long measuredRetrievalTime = stopWatch.getElapsedTime();
        assertTrue("Retrieval time on known key is " + measuredRetrievalTime
                + " but should be less than " + requiredRetrievalTime + "ms",
                measuredRetrievalTime < requiredRetrievalTime);
    }

    /**
     * Creates a blocking test cache
     */
    protected Ehcache createTestCache() {
        Ehcache cache = super.createTestCache();
        return new BlockingCache(cache);
}

    /**
     * Gets the sample cache 1
     */
    protected Ehcache getSampleCache1() {
        Cache cache = manager.getCache("sampleCache1");
        manager.replaceCacheWithDecoratedCache(cache, new BlockingCache(cache));
        return manager.getEhcache("sampleCache1");
    }

    /**
     * Use to manually test super class tests
     */
    public void testInstrumented() throws Exception {
        super.testSizes();
    }
}

