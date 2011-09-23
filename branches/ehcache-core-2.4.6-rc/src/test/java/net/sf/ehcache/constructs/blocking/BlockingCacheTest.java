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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheTest;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.statistics.LiveCacheStatistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test cases for the {@link BlockingCache}.
 *
 * @author Adam Murdoch
 * @author Greg Luck
 * @version $Id$
 */
public class BlockingCacheTest extends CacheTest {

    private BlockingCache blockingCache;

    /**
     * Load up the test cache
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Ehcache cache = manager.getCache("sampleIdlingExpiringCache");
        blockingCache = new BlockingCache(cache);
    }

    /**
     * teardown
     */
    @Override
    @After
    public void tearDown() throws Exception {
        if (manager.getStatus() == Status.STATUS_ALIVE) {
            blockingCache.removeAll();
        }
        super.tearDown();
    }

    @Test
    public void testSupportsStatsCorrectly() {
        blockingCache.setStatisticsEnabled(true);
        LiveCacheStatistics statistics = blockingCache.getLiveCacheStatistics();
        long cacheMisses = statistics.getCacheMissCount();
        long cacheHits = statistics.getCacheHitCount();
        String key = "123451234";
        blockingCache.get(key);
        assertEquals("Misses stat should have incremented by one", cacheMisses + 1, statistics.getCacheMissCount());
        assertEquals("Hits stat should have remain the same", cacheHits, statistics.getCacheHitCount());
        blockingCache.put(new Element(key, "value"));
        assertEquals("Misses stat should have incremented by one", cacheMisses + 1, statistics.getCacheMissCount());
        assertEquals("Hits stat should have remain the same", cacheHits, statistics.getCacheHitCount());
        assertNotNull(blockingCache.get(key));
        assertEquals("Hits stat should have incremented by one", cacheHits + 1, statistics.getCacheHitCount());
        blockingCache.setStatisticsEnabled(false);
        blockingCache.remove(key);
    }

    /**
     * Tests adding and looking up an entry.
     */
    @Test
    public void testAddEntry() throws Exception {
        //some other test was leaving this non-empty
        blockingCache.removeAll();

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
    @Test
    public void testGetEntries() throws Exception {
        Ehcache cache = blockingCache.getCache();
        for (int i = 0; i < 100; i++) {
            cache.put(new Element(Integer.valueOf(i), "value" + i));
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
            Serializable value = (Serializable) map.get(Integer.valueOf(i));
            assertEquals("value" + i, value);
        }
    }

    /**
     * Tests looking up a missing entry, then adding it.
     */
    @Test
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
    @Test
    public void testSecondThreadActuallyBlocks() throws Exception {
        Element element = new Element("key", "value");
        final List threadResults = new ArrayList();

        // Make sure the entry does not exist
        assertNull(blockingCache.get("key"));

        Thread secondThread = new Thread() {
            @Override
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
    @Test
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
    @Test
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
    @Test
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
     * Creates a blocking test cache
     */
    @Override
    protected Ehcache createTestCache() {
        Ehcache cache = super.createTestCache();
        return new BlockingCache(cache);
    }

    /**
     * Gets the sample cache 1
     */
    @Override
    protected Ehcache getSampleCache1() {
        Cache cache = manager.getCache("sampleCache1");
        manager.replaceCacheWithDecoratedCache(cache, new BlockingCache(cache));
        return manager.getEhcache("sampleCache1");
    }

    /**
     * Use to manually test super class tests
     */
    @Test
    public void testInstrumented() throws Exception {
        super.testSizes();
    }

    @Override
    @Test
    public void testGetWithLoader() {
        super.testGetWithLoader();
    }


    @Override
    @Test
    public void testFlushWhenOverflowToDisk() throws Exception {
        super.testFlushWhenOverflowToDisk();
    }

    @Override
    @Test
    public void testConcurrentPutsAreConsistentRepeatedly() throws InterruptedException {
        //do nothing
    }

    @Override
    @Test
    public void testConcurrentPutsAreConsistent() throws InterruptedException {
        //do nothing
    }

    @Test
    public void testInlineEviction() throws InterruptedException {

        final Serializable KEY = "DUH";
        Cache cache = new Cache(new CacheConfiguration("fastExpiry", 1000).timeToIdleSeconds(2).timeToLiveSeconds(2));
        manager.addCache(cache);
        manager.replaceCacheWithDecoratedCache(cache, new BlockingCache(cache));

        Ehcache blockingCache = manager.getEhcache("fastExpiry");
        blockingCache.put(new Element(KEY, "VALUE"));
        assertNotNull(blockingCache.get(KEY));
        // This tests inline eviction (EHC-420)
        Thread.sleep(3000);
        assertNull(blockingCache.get(KEY));
    }

    @Test
    public void testTimeout() throws BrokenBarrierException, InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final String KEY = "BLOCKING_KEY";
        blockingCache.setTimeoutMillis(1000);
        Thread thread = new Thread(new Runnable() {
            public void run() {
                assertNull(blockingCache.get(KEY));
                try {
                    barrier.await();
                    Thread.sleep(5000);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                blockingCache.put(new Element(KEY, "VALUE"));
            }
        });
        thread.start();
        barrier.await();
        try {
            blockingCache.get(KEY);
            fail("BlockingCache.get should have not returned!");
        } catch (CacheException e) {
            // Expected
        }
    }
}

