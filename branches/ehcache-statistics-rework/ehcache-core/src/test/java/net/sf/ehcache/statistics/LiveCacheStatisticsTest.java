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

package net.sf.ehcache.statistics;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;

import org.junit.Test;

/**
 * Tests for the statistics class
 *
 * @author Abhishek Sanoujam
 * @version $Id: CacheUsageStatisticsTest.java 1221 2009-09-25 17:29:35Z asingh
 *          $
 */
public class LiveCacheStatisticsTest extends AbstractCacheTest {


    /**
     * test enable disable
     */
    @Test
    public void testEnableDisable() {
//        Cache cache = new Cache("test", 1, true, false, 5, 2);
//        manager.addCache(cache);
//
//        cache.setStatisticsEnabled(true);
//        assertTrue(cache.isStatisticsEnabled());
//        assertFalse(cache.isSampledStatisticsEnabled());
//
//        cache.setStatisticsEnabled(false);
//        assertFalse(cache.isStatisticsEnabled());
//        assertFalse(cache.isSampledStatisticsEnabled());
//
//        // enabling sampled enables both
//        cache.setSampledStatisticsEnabled(true);
//        assertTrue(cache.isStatisticsEnabled());
//        assertTrue(cache.isSampledStatisticsEnabled());
//
//        cache.setSampledStatisticsEnabled(false);
//        assertTrue(cache.isStatisticsEnabled());
//        assertFalse(cache.isSampledStatisticsEnabled());
//
//        // enable sampled again
//        cache.setSampledStatisticsEnabled(true);
//        assertTrue(cache.isStatisticsEnabled());
//        assertTrue(cache.isSampledStatisticsEnabled());
//
//        // disabling live disables sampled too
//        cache.setStatisticsEnabled(false);
//        assertFalse(cache.isStatisticsEnabled());
//        assertFalse(cache.isSampledStatisticsEnabled());
    }

    /**
     * Test statistics enabling/disabling/clearing
     *
     * @throws InterruptedException
     */
    @Test
    public void testCacheUsageStatistics() throws InterruptedException {
//        // Set size so the second element overflows to disk.
//        Cache cache = new Cache("test", 1, true, false, 5, 2);
//        manager.addCache(cache);
//
//        cache.setStatisticsEnabled(true);
//        doTestCacheUsageStatistics(cache, true);
//
//        // test enable/disable statistics
//        cache.setStatisticsEnabled(false);
//        doTestCacheUsageStatistics(cache, false);
//
//        assertEquals(Statistics.STATISTICS_ACCURACY_BEST_EFFORT, cache
//                .getLiveCacheStatistics().getStatisticsAccuracy());
//        assertEquals("Best Effort", cache.getLiveCacheStatistics()
//                .getStatisticsAccuracyDescription());

    }

    /**
     * Test statistics directly. Tests
     * - cacheHitCount
     * - onDiskHitCount
     * - inMemoryHitCount
     * - cacheMissCount
     * - size
     * - inMemorySize
     * - onDiskSize
     * - clearing statistics
     * - average get time
     */
    public void doTestCacheUsageStatistics(Cache cache,
                                           boolean statisticsEnabled) throws InterruptedException {
//
//        cache.put(new Element("key1", "value1"));
//        cache.put(new Element("key2", "value1"));
//        // key1 should be in the Disk Store
//        cache.get("key1");
//
//        LiveCacheStatistics statistics = cache.getLiveCacheStatistics();
//        if (statisticsEnabled) {
//            assertEquals(1, statistics.getCacheHitCount());
//            assertEquals(1, statistics.getOnDiskHitCount());
//            assertEquals(0, statistics.getInMemoryHitCount());
//            assertEquals(0, statistics.getCacheMissCount());
//            assertEquals(2, statistics.getSize());
//            assertEquals(1, statistics.getInMemorySize());
//            final LiveCacheStatistics finalStatistics = statistics;
//            RetryAssert.assertBy(5, SECONDS, new Callable<Long>() {
//                public Long call() throws Exception {
//                    return finalStatistics.getOnDiskSize();
//                }
//            }, Is.is(2L));
//        } else {
//            assertEquals(0, statistics.getCacheHitCount());
//            assertEquals(0, statistics.getOnDiskHitCount());
//            assertEquals(0, statistics.getInMemoryHitCount());
//            assertEquals(0, statistics.getCacheMissCount());
//            assertEquals(0, statistics.getSize());
//            assertEquals(0, statistics.getInMemorySize());
//            assertEquals(0, statistics.getOnDiskSize());
//        }
//
//        // key 1 should now be in the LruMemoryStore
//        cache.get("key1");
//
//        statistics = cache.getLiveCacheStatistics();
//        if (statisticsEnabled) {
//            assertEquals(2, statistics.getCacheHitCount());
//            assertEquals(1, statistics.getOnDiskHitCount());
//            assertEquals(1, statistics.getInMemoryHitCount());
//            assertEquals(0, statistics.getCacheMissCount());
//        } else {
//            assertEquals(0, statistics.getCacheHitCount());
//            assertEquals(0, statistics.getOnDiskHitCount());
//            assertEquals(0, statistics.getInMemoryHitCount());
//            assertEquals(0, statistics.getCacheMissCount());
//        }
//
//        // Let the idle expire
//        Thread.sleep(6000);
//
//        // key 1 should now be expired
//        cache.get("key1");
//        statistics = cache.getLiveCacheStatistics();
//        if (statisticsEnabled) {
//            assertEquals(2, statistics.getCacheHitCount());
//            assertEquals(1, statistics.getOnDiskHitCount());
//            assertEquals(1, statistics.getInMemoryHitCount());
//            assertEquals(1, statistics.getCacheMissCount());
//        } else {
//            assertEquals(0, statistics.getCacheHitCount());
//            assertEquals(0, statistics.getOnDiskHitCount());
//            assertEquals(0, statistics.getInMemoryHitCount());
//            assertEquals(0, statistics.getCacheMissCount());
//        }
//
//        // key 2 should also be expired
//        cache.get("key1");
//        statistics = cache.getLiveCacheStatistics();
//        if (statisticsEnabled) {
//            assertEquals(2, statistics.getCacheHitCount());
//            assertEquals(1, statistics.getOnDiskHitCount());
//            assertEquals(1, statistics.getInMemoryHitCount());
//            assertEquals(2, statistics.getCacheMissCount());
//        } else {
//            assertEquals(0, statistics.getCacheHitCount());
//            assertEquals(0, statistics.getOnDiskHitCount());
//            assertEquals(0, statistics.getInMemoryHitCount());
//            assertEquals(0, statistics.getCacheMissCount());
//        }
//
//        cache.clearStatistics();
//        // everything should be zero now
//        assertEquals(0, statistics.getCacheHitCount());
//        assertEquals(0, statistics.getOnDiskHitCount());
//        assertEquals(0, statistics.getInMemoryHitCount());
//        assertEquals(0, statistics.getCacheMissCount());
//
//        assertNotNull(statistics.toString());
    }

    /**
     * Test average get time
     *
     * @throws InterruptedException
     */
    @Test
    public void testAverageGetTime() throws InterruptedException {
//        Cache cache = new Cache("test", 0, true, false, 5, 2);
//        manager.addCache(cache);
//        cache.setStatisticsEnabled(true);
//        doTestAverageGetTime(cache, true);
//
//        // test enable/disable statistics
//        cache.setStatisticsEnabled(false);
//        doTestAverageGetTime(cache, false);
    }

    /**
     * Tests average get time
     */
    public void doTestAverageGetTime(Cache cache, boolean statsEnabled) {
//        LiveCacheStatistics statistics = cache.getLiveCacheStatistics();
//        float averageGetTime = statistics.getAverageGetTimeMillis();
//        assertTrue(0 == statistics.getAverageGetTimeMillis());
//
//        for (int i = 0; i < 10000; i++) {
//            cache.put(new Element("" + i, "value1"));
//        }
//        cache.put(new Element("key1", "value1"));
//        cache.put(new Element("key2", "value1"));
//        for (int i = 0; i < 110000; i++) {
//            cache.get("" + i);
//        }
//
//        statistics = cache.getLiveCacheStatistics();
//        averageGetTime = statistics.getAverageGetTimeMillis();
//        if (statsEnabled) {
//            assertTrue(averageGetTime >= .000001);
//        } else {
//            assertTrue(0 == averageGetTime);
//        }
//        cache.clearStatistics();
//        assertTrue(0 == statistics.getAverageGetTimeMillis());
    }

    /**
     * Test cache eviction/expiry stats
     *
     * @throws InterruptedException
     */
    @Test
    public void testEvictionStatistics() throws InterruptedException {
//        doTestEvictionStatistics(true);
//
//        doTestEvictionStatistics(false);
    }

    /**
     * Tests eviction statistics
     * - evictedCount
     * - missCountNotFound
     * - missCountExpired
     * - missCount
     * - expiredCount
     * - size
     */
    public void doTestEvictionStatistics(boolean statsEnabled)
            throws InterruptedException {
//        // run 5 times with random total and capacity values
//        Random rand = new Random();
//        int min = 100;
//        for (int loop = 0; loop < 5; loop++) {
//            int a = rand.nextInt(10000) + min;
//            int b = rand.nextInt(10000) + min;
//            if (a == b) {
//                a += min;
//            }
//            int total = Math.max(a, b);
//            int capacity = Math.min(a, b);
//            Ehcache ehcache = new net.sf.ehcache.Cache("test-" + statsEnabled
//                    + "-" + loop, capacity, false, false, 2, 2);
//            manager.addCache(ehcache);
//            ehcache.setStatisticsEnabled(statsEnabled);
//
//            LiveCacheStatistics statistics = ehcache.getLiveCacheStatistics();
//            assertEquals(0, statistics.getEvictedCount());
//
//            for (int i = 0; i < total; i++) {
//                ehcache.put(new Element("" + i, "value1"));
//            }
//            if (statsEnabled) {
//                assertEquals(total - capacity, statistics.getEvictedCount());
//            } else {
//                assertEquals(0, statistics.getEvictedCount());
//            }
//
//            Thread.sleep(3010);
//
//            // expiries do not count as eviction
//            if (statsEnabled) {
//                assertEquals(total - capacity, statistics.getEvictedCount());
//            } else {
//                assertEquals(0, statistics.getEvictedCount());
//            }
//
//            // no expiration till a get is tried
//            assertEquals(0, statistics.getCacheMissCount());
//            assertEquals(0, statistics.getCacheMissCountExpired());
//            assertEquals(0, statistics.getExpiredCount());
//            assertEquals(0, statistics.getCacheMissCount());
//
//            for (int i = 0; i < total; i++) {
//                ehcache.get("" + i);
//            }
//
//            if (statsEnabled) {
//                assertEquals(total, statistics.getCacheMissCount());
//                assertEquals(capacity, statistics.getCacheMissCountExpired());
//                assertEquals(capacity, statistics.getExpiredCount());
//                assertEquals(total, statistics.getCacheMissCount());
//                assertEquals(0, statistics.getSize());
//            } else {
//                assertEquals(0, statistics.getCacheMissCount());
//                assertEquals(0, statistics.getCacheMissCountExpired());
//                assertEquals(0, statistics.getExpiredCount());
//                assertEquals(0, statistics.getCacheMissCount());
//                assertEquals(0, statistics.getSize());
//            }
//
//            ehcache.clearStatistics();
//
//            assertEquals(0, statistics.getCacheMissCount());
//            assertEquals(0, statistics.getCacheMissCountExpired());
//            assertEquals(0, statistics.getExpiredCount());
//            assertEquals(0, statistics.getCacheMissCount());
//            assertEquals(0, statistics.getSize());
//        }

    }

    /**
     * Test element put/update/remove
     * - putCount
     * - updateCount
     * - removeCount
     */
    @Test
    public void testPutUpdateRemoveStats() throws InterruptedException {
//        doTestElementUpdateRemove(true);
//
//        doTestElementUpdateRemove(false);
    }

    public void doTestElementUpdateRemove(boolean statsEnabled)
            throws InterruptedException {
//        Random rand = new Random();
//        int min = 100;
//        for (int loop = 0; loop < 5; loop++) {
//            int total = rand.nextInt(10000) + min;
//
//            // always ensure enough capacity. Otherwise cannot predict
//            // updateCount with eviction (based on capacity)
//            Ehcache ehcache = new net.sf.ehcache.Cache("test-" + statsEnabled
//                    + "-" + loop, total + 1, false, false, 1200, 1200);
//            manager.addCache(ehcache);
//            ehcache.setStatisticsEnabled(statsEnabled);
//
//            LiveCacheStatistics statistics = ehcache.getLiveCacheStatistics();
//
//            assertEquals(0, statistics.getEvictedCount());
//            assertEquals(0, statistics.getPutCount());
//            assertEquals(0, statistics.getRemovedCount());
//            assertEquals(0, statistics.getUpdateCount());
//
//            for (int i = 0; i < total; i++) {
//                ehcache.put(new Element("" + i, "value1"));
//            }
//            if (statsEnabled) {
//                assertEquals(total, statistics.getPutCount());
//                assertEquals(0, statistics.getEvictedCount());
//                assertEquals(total, statistics.getSize());
//                assertEquals(0, statistics.getUpdateCount());
//                assertEquals(0, statistics.getRemovedCount());
//            } else {
//                assertEquals(0, statistics.getPutCount());
//                assertEquals(0, statistics.getEvictedCount());
//                assertEquals(0, statistics.getSize());
//                assertEquals(0, statistics.getRemovedCount());
//                assertEquals(0, statistics.getUpdateCount());
//            }
//
//            // minimum 1 update
//            int updates = rand.nextInt(total - 1) + 1;
//            assertTrue(updates >= 1);
//            for (int i = 0; i < updates; i++) {
//                ehcache.put(new Element("" + i, "value1"));
//            }
//            if (statsEnabled) {
//                assertEquals(total, statistics.getSize());
//                assertEquals(updates, statistics.getUpdateCount());
//                assertEquals(total, statistics.getPutCount());
//                assertEquals(0, statistics.getEvictedCount());
//                assertEquals(0, statistics.getRemovedCount());
//            } else {
//                assertEquals(0, statistics.getSize());
//                assertEquals(0, statistics.getPutCount());
//                assertEquals(0, statistics.getRemovedCount());
//                assertEquals(0, statistics.getEvictedCount());
//                assertEquals(0, statistics.getUpdateCount());
//            }
//
//            // minimum 1 remove
//            int remove = rand.nextInt(total - 1) + 1;
//            assertTrue(updates >= 1);
//            for (int i = 0; i < remove; i++) {
//                ehcache.remove("" + i);
//            }
//            if (statsEnabled) {
//                assertEquals(total - remove, statistics.getSize());
//                assertEquals(updates, statistics.getUpdateCount());
//                assertEquals(remove, statistics.getRemovedCount());
//                assertEquals(total, statistics.getPutCount());
//                assertEquals(0, statistics.getEvictedCount());
//            } else {
//                assertEquals(0, statistics.getSize());
//                assertEquals(0, statistics.getPutCount());
//                assertEquals(0, statistics.getRemovedCount());
//                assertEquals(0, statistics.getEvictedCount());
//                assertEquals(0, statistics.getUpdateCount());
//            }
//
//            ehcache.clearStatistics();
//
//            assertEquals(0, statistics.getPutCount());
//            assertEquals(0, statistics.getRemovedCount());
//            assertEquals(0, statistics.getEvictedCount());
//            assertEquals(0, statistics.getUpdateCount());
//        }

    }

    /**
     * CacheStatistics should always be sensible when the cache has not
     * started.
     */
    @Test
    public void testCacheAlive() {
//        Cache cache = new Cache("test", 1, true, false, 5, 2);
//        String string = cache.toString();
//        assertTrue(string.contains("test"));
//        try {
//            LiveCacheStatistics statistics = cache.getLiveCacheStatistics();
//            fail();
//        } catch (IllegalStateException e) {
//            assertEquals("The test Cache is not alive (STATUS_UNINITIALISED)", e.getMessage());
//        }
//        // initialize cache now
//        manager.addCache(cache);
//        LiveCacheStatistics statistics = cache.getLiveCacheStatistics();
//        assertEquals(0, statistics.getCacheHitCount());
    }

}
