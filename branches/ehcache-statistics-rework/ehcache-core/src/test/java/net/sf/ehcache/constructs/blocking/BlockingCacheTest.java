/**
 *  Copyright Terracotta, Inc.
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

import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.constructs.blocking.BlockingCacheOperationOutcomes.GetOutcome;
import net.sf.ehcache.statistics.StatisticsPlaceholder;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Result;
import net.sf.ehcache.store.disk.DiskStoreHelper;
import org.hamcrest.collection.IsCollectionWithSize;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases for the {@link BlockingCache}.
 *
 * @author Adam Murdoch
 * @author Greg Luck
 * @version $Id$
 */
public final class BlockingCacheTest {

    private static final String DISK_STORE_PATH = "target/BlockingCacheTest";

    @BeforeClass
    public static void cleanupDisk() {
        File diskStorePath = new File(DISK_STORE_PATH);
        File[] files = diskStorePath.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
    }

    @Before
    public void noCacheManagersBefore() {
        assertThat(CacheManager.ALL_CACHE_MANAGERS, IsEmptyCollection.<CacheManager>empty());
    }

    @After
    public void noCacheManagersAfter() {
        assertThat(CacheManager.ALL_CACHE_MANAGERS, IsEmptyCollection.<CacheManager>empty());
    }

    private CacheManager createCacheManager(String cacheName) {
        return createCacheManager(new CacheConfiguration()
                .name(cacheName)
                .maxEntriesLocalHeap(1)
                .timeToIdleSeconds(2)
                .timeToLiveSeconds(5)
                .overflowToDisk(true)
                .diskPersistent(true));
    }

    private CacheManager createCacheManager(CacheConfiguration config) {
        CacheManager manager = new CacheManager(new Configuration()
                .name("BlockingCacheTest")
                .diskStore(new DiskStoreConfiguration().path(DISK_STORE_PATH)));
        manager.addCache(new Cache(config));
        return manager;
    }

    @Test
    public void testSupportsStatsCorrectly() {
        CacheManager manager = createCacheManager("testSupportsStatsCorrectly");
        try {
            BlockingCache blockingCache = new BlockingCache(manager.getEhcache("testSupportsStatsCorrectly"));
            ExtendedStatistics statistics = blockingCache.getStatistics().getExtended();
            Set<Operation<GetOutcome>> stats = statistics.operations(GetOutcome.class, "get", "blocking-cache");
            assertThat(stats, IsCollectionWithSize.hasSize(1));
            Operation<GetOutcome> blockingGet = stats.iterator().next();
            Result misses = blockingGet.component(GetOutcome.MISS_AND_LOCKED);
            Result hits = blockingGet.component(GetOutcome.HIT);
            long baselineMisses = misses.count().value();
            long baselineHits = hits.count().value();
            String key = "123451234";
            blockingCache.get(key);
            assertEquals("Misses stat should have incremented by one", Long.valueOf(baselineMisses + 1L), misses.count().value());
            assertEquals("Hits stat should have remain the same", Long.valueOf(baselineHits), hits.count().value());
            blockingCache.put(new Element(key, "value"));
            assertEquals("Misses stat should have incremented by one", Long.valueOf(baselineMisses + 1), misses.count().value());
            assertEquals("Hits stat should have remain the same", Long.valueOf(baselineHits), hits.count().value());
            assertNotNull(blockingCache.get(key));
            assertEquals("Hits stat should have incremented by one", Long.valueOf(baselineHits + 1), hits.count().value());
            blockingCache.remove(key);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests adding and looking up an entry.
     */
    @Test
    public void testAddEntry() throws Exception {
        CacheManager manager = createCacheManager("testAddEntry");
        try {
            BlockingCache blockingCache = new BlockingCache(manager.getEhcache("testAddEntry"));

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
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests that getting entries matches a list of known entries
     */
    @Test
    public void testGetEntries() throws Exception {
        CacheManager manager = createCacheManager("testGetEntries");
        try {
            BlockingCache blockingCache = new BlockingCache(manager.getEhcache("testGetEntries"));

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
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests looking up a missing entry, then adding it.
     */
    @Test
    public void testAddMissingEntry() throws Exception {
        CacheManager manager = createCacheManager("testAddMissingEntry");
        try {
            BlockingCache blockingCache = new BlockingCache(manager.getEhcache("testAddMissingEntry"));
            Element element = new Element("key", "value");

            // Make sure the entry does not exist
            assertNull(blockingCache.get("key"));

            // Put the entry
            blockingCache.put(element);

            // Check the entry is in the cache
            assertEquals(1, blockingCache.getKeys().size());
            assertEquals(element, blockingCache.get("key"));
        } finally {
            manager.shutdown();
        }
    }


    /**
     * Does a second tread block until the first thread puts the entry?
     */
    @Test
    public void testSecondThreadActuallyBlocks() throws Exception {
        CacheManager manager = createCacheManager("testSecondThreadActuallyBlocks");
        try {
            final BlockingCache blockingCache = new BlockingCache(manager.getEhcache("testSecondThreadActuallyBlocks"));
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
            secondThread.join();
            assertEquals(1, threadResults.size());
            assertEquals(element, threadResults.get(0));

            // Check the entry is in the cache
            assertEquals(1, blockingCache.getKeys().size());
            assertEquals(element, blockingCache.get("key"));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Does a second tread block until the first thread puts the entry?
     */
    @Test
    public void testSecondThreadActuallyBlocksUntilPutBoolean() throws Exception {
        final String cacheName = "testSecondThreadActuallyBlocksUntilPutBoolean";
        CacheManager manager = createCacheManager(cacheName);
        try {
            final BlockingCache blockingCache = new BlockingCache(manager.getEhcache(cacheName));
            blockingCache.setTimeoutMillis((int)TimeUnit.SECONDS.toMillis(5));
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
            blockingCache.put(element, true);
            secondThread.join();
            assertEquals(1, threadResults.size());
            assertEquals(element, threadResults.get(0));

            // Check the entry is in the cache
            assertEquals(1, blockingCache.getKeys().size());
            assertEquals(element, blockingCache.get("key"));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Does a second tread block until the first thread puts the entry?
     */
    @Test
    public void testSecondThreadActuallyBlocksUntilPutIfAbsent() throws Exception {
        final String cacheName = "testSecondThreadActuallyBlocksUntilPutIfAbsent";
        CacheManager manager = createCacheManager(cacheName);
        try {
            final BlockingCache blockingCache = new BlockingCache(manager.getEhcache(cacheName));
            blockingCache.setTimeoutMillis((int)TimeUnit.SECONDS.toMillis(5));
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
            blockingCache.putIfAbsent(element);
            secondThread.join();
            assertEquals(1, threadResults.size());
            assertEquals(element, threadResults.get(0));

            // Check the entry is in the cache
            assertEquals(1, blockingCache.getKeys().size());
            assertEquals(element, blockingCache.get("key"));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Does a second tread block until the first thread puts the entry?
     */
    @Test
    public void testSecondThreadActuallyBlocksUntilPutIfAbsentBoolean() throws Exception {
        final String cacheName = "testSecondThreadActuallyBlocksUntilPutIfAbsentBoolean";
        CacheManager manager = createCacheManager(cacheName);
        try {
            final BlockingCache blockingCache = new BlockingCache(manager.getEhcache(cacheName));
            blockingCache.setTimeoutMillis((int)TimeUnit.SECONDS.toMillis(5));
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
            blockingCache.putIfAbsent(element, true);
            secondThread.join();
            assertEquals(1, threadResults.size());
            assertEquals(element, threadResults.get(0));

            // Check the entry is in the cache
            assertEquals(1, blockingCache.getKeys().size());
            assertEquals(element, blockingCache.get("key"));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Does a second tread block until the first thread puts the entry?
     */
    @Test
    public void testSecondThreadActuallyBlocksUntilPutQuiet() throws Exception {
        final String cacheName = "testSecondThreadActuallyBlocksUntilPutQuiet";
        CacheManager manager = createCacheManager(cacheName);
        try {
            final BlockingCache blockingCache = new BlockingCache(manager.getEhcache(cacheName));
            blockingCache.setTimeoutMillis((int)TimeUnit.SECONDS.toMillis(5));
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
            blockingCache.putQuiet(element);
            secondThread.join();
            assertEquals(1, threadResults.size());
            assertEquals(element, threadResults.get(0));

            // Check the entry is in the cache
            assertEquals(1, blockingCache.getKeys().size());
            assertEquals(element, blockingCache.get("key"));
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Elements with null valuea are not stored in the blocking cache
     */
    @Test
    public void testUnknownEntry() throws Exception {
        CacheManager manager = createCacheManager("testUnknownEntry");
        try {
            BlockingCache blockingCache = new BlockingCache(manager.getEhcache("testUnknownEntry"));
            // Make sure the entry does not exist
            assertNull(blockingCache.get("key"));
            // Put the entry
            blockingCache.put(new Element("key", null));
            assertEquals(0, blockingCache.getKeys().size());
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Overwriting an Element with an element with a null value effectively removes it from the cache
     */
    @Test
    public void testRemoveEntry() throws Exception {
        CacheManager manager = createCacheManager("testRemoveEntry");
        try {
            BlockingCache blockingCache = new BlockingCache(manager.getEhcache("testRemoveEntry"));
            Element element = new Element("key", "value");

            // Add entry and make sure it's there
            blockingCache.put(element);
            assertEquals(element, blockingCache.get("key"));

            // Remove the entry and make sure its gone
            blockingCache.put(new Element("key", null));
            assertEquals(0, blockingCache.getKeys().size());
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests clearing the cache
     */
    @Test
    public void testClear() throws Exception {
        CacheManager manager = createCacheManager(new CacheConfiguration()
                .name("testClear")
                .maxEntriesLocalHeap(1000)
                .eternal(false)
                .overflowToDisk(false));
        try {
            BlockingCache blockingCache = new BlockingCache(manager.getCache("testClear"));
            // Add some entries
            blockingCache.put(new Element("key1", "value1"));
            blockingCache.put(new Element("key2", "value2"));
            blockingCache.put(new Element("key3", "value2"));
            assertEquals(3, blockingCache.getKeys().size());

            // Clear the cache
            blockingCache.removeAll();
            assertEquals(0, blockingCache.getKeys().size());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testInlineEviction() throws InterruptedException {
        CacheManager manager = createCacheManager(new CacheConfiguration()
                .name("testInlineEviction")
                .maxEntriesLocalHeap(1000)
                .timeToIdleSeconds(2)
                .timeToLiveSeconds(2));
        try {
            final Serializable KEY = "DUH";
            Cache cache = manager.getCache("testInlineEviction");
            manager.replaceCacheWithDecoratedCache(cache, new BlockingCache(cache));
            Ehcache blockingCache = manager.getEhcache("testInlineEviction");

            blockingCache.put(new Element(KEY, "VALUE"));
            assertNotNull(blockingCache.get(KEY));
            // This tests inline eviction (EHC-420)
            Thread.sleep(3000);
            assertNull(blockingCache.get(KEY));
        } finally {
            manager.shutdown();
        }
    }

    @Test
    public void testTimeout() throws BrokenBarrierException, InterruptedException {
        CacheManager manager = createCacheManager("testTimeout");
        try {
            final BlockingCache blockingCache = new BlockingCache(manager.getEhcache("testTimeout"));
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
            thread.join();
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Checks we cannot use a cache after shutdown
     */
    @Test
    public void testUseCacheAfterManagerShutdown() throws CacheException {
        CacheManager manager = createCacheManager(new CacheConfiguration()
                .name("testUseCacheAfterManagerShutdown")
                .maxEntriesLocalHeap(10000)
                .maxEntriesLocalDisk(1000)
                .timeToIdleSeconds(360)
                .timeToLiveSeconds(1000)
                .overflowToDisk(true)
                .memoryStoreEvictionPolicy("LRU"));
        try {
            Cache cache = manager.getCache("testUseCacheAfterManagerShutdown");
            manager.replaceCacheWithDecoratedCache(cache, new BlockingCache(cache));
            Ehcache blockingCache = manager.getEhcache("testUseCacheAfterManagerShutdown");

            manager.shutdown();
            Element element = new Element("key", "value");
            try {
                blockingCache.getSize();
                fail();
            } catch (IllegalStateException e) {
                assertEquals("The testUseCacheAfterManagerShutdown Cache is not alive (STATUS_SHUTDOWN)", e.getMessage());
            }
            try {
                blockingCache.put(element);
                fail();
            } catch (IllegalStateException e) {
                assertEquals("The testUseCacheAfterManagerShutdown Cache is not alive (STATUS_SHUTDOWN)", e.getMessage());
            }
            try {
                blockingCache.get("key");
                fail();
            } catch (IllegalStateException e) {
                assertEquals("The testUseCacheAfterManagerShutdown Cache is not alive (STATUS_SHUTDOWN)", e.getMessage());
            }
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Tests cache, memory store and disk store sizes from config
     */
    @Test
    public void testSizes() throws Exception {
        CacheManager manager = createCacheManager(new CacheConfiguration()
                .name("testUseCacheAfterManagerShutdown")
                .maxEntriesLocalHeap(10000)
                .maxEntriesLocalDisk(1000)
                .timeToIdleSeconds(360)
                .timeToLiveSeconds(1000)
                .overflowToDisk(true)
                .memoryStoreEvictionPolicy("LRU"));
        try {
            Cache cache = manager.getCache("testUseCacheAfterManagerShutdown");
            manager.replaceCacheWithDecoratedCache(cache, new BlockingCache(cache));
            Ehcache blockingCache = manager.getEhcache("testUseCacheAfterManagerShutdown");

            assertEquals(0, blockingCache.getStatistics().getLocalHeapSize());

            for (int i = 0; i < 10010; i++) {
                blockingCache.put(new Element("key" + i, "value1"));
            }

            DiskStoreHelper.flushAllEntriesToDisk(cache).get();
            assertThat(cache.getSize(), lessThanOrEqualTo(10000));
            assertThat(cache.getStatistics().getLocalHeapSize(), lessThanOrEqualTo(10000L));
            // TODO Lower tier will _never_ be smaller than higher ones now
//            assertThat(cache.getStatistics().getLocalDiskSize(), lessThanOrEqualTo(1000));

            //NonSerializable
            DiskStoreHelper.flushAllEntriesToDisk(cache).get();
            blockingCache.put(new Element(new Object(), Object.class));

            int size = cache.getSize();
            assertThat(size, lessThanOrEqualTo(10000));
            assertThat(cache.getStatistics().getLocalHeapSize(), lessThanOrEqualTo(10000L));
            // TODO Lower tier will _never_ be smaller than higher ones now
//            assertThat(cache.getStatistics().getLocalDiskSize(), lessThanOrEqualTo(1000));

            if(cache.remove("key4")) {
                size--;
            }
            if(cache.remove("key3")) {
                size--;
            }

            DiskStoreHelper.flushAllEntriesToDisk(cache).get();

            assertEquals(size, cache.getSize());

            //cannot make any guarantees as no elements have been getted, and all are equally likely to be evicted.
            //assertEquals(10000, cache.getStatistics().getLocalHeapSize());
            //assertEquals(9, cache.getStatistics().getLocalDiskSize());


            DiskStoreHelper.flushAllEntriesToDisk(cache).get();

            blockingCache.removeAll();
            assertEquals(0, blockingCache.getSize());
            assertEquals(0, blockingCache.getStatistics().getLocalHeapSize());
            assertEquals(0, blockingCache.getStatistics().getLocalDiskSize());
        } finally {
            manager.shutdown();
        }
    }
}

