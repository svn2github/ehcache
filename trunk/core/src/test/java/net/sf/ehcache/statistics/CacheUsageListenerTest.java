/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;
import java.util.logging.Logger;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;

import org.junit.Test;

/**
 * Tests for the CacheUsageListener
 * 
 * @author Abhishek Sanoujam
 * @version $Id$
 */
public class CacheUsageListenerTest extends AbstractCacheTest {

    private static final Logger LOG = Logger
            .getLogger(CacheUsageListenerTest.class.getName());

    /**
     * Test statistics enabling/disabling/clearing
     * 
     * @throws InterruptedException
     */
    @Test
    public void testCacheUsageStatistics() throws InterruptedException {
        // Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);

        // add as a listener
        AnotherStatistics anotherStats = new AnotherStatistics();
        cache.registerCacheUsageListener(anotherStats);

        cache.setStatisticsEnabled(true);
        doTestCacheUsageStatistics(cache, true, anotherStats);

        // test enable/disable statistics
        cache.setStatisticsEnabled(false);
        doTestCacheUsageStatistics(cache, false, anotherStats);

        // remove the listener
        cache.removeCacheUsageListener(anotherStats);
        // enable statistics but don't check stats as no longer a listener
        cache.setStatisticsEnabled(true);
        doTestCacheUsageStatistics(cache, false, anotherStats);

        assertEquals(Statistics.STATISTICS_ACCURACY_BEST_EFFORT, cache
                .getCacheUsageStatistics().getStatisticsAccuracy());
        assertEquals("Best Effort", cache.getCacheUsageStatistics()
                .getStatisticsAccuracyDescription());
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
     * 
     */
    public void doTestCacheUsageStatistics(Cache cache, boolean checkStats,
            AnotherStatistics anotherStats) throws InterruptedException {

        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        // key1 should be in the Disk Store
        cache.get("key1");

        if (checkStats) {
            assertEquals(1, anotherStats.getCacheHitCount());
            assertEquals(1, anotherStats.getOnDiskHitCount());
            assertEquals(0, anotherStats.getInMemoryHitCount());
            assertEquals(0, anotherStats.getCacheMissCount());
            // assertEquals(2, anotherStats.getSize());
            // assertEquals(1, anotherStats.getInMemorySize());
            // assertEquals(1, anotherStats.getOnDiskSize());
        } else {
            assertEquals(0, anotherStats.getCacheHitCount());
            assertEquals(0, anotherStats.getOnDiskHitCount());
            assertEquals(0, anotherStats.getInMemoryHitCount());
            assertEquals(0, anotherStats.getCacheMissCount());
            // assertEquals(0, anotherStats.getSize());
            // assertEquals(0, anotherStats.getInMemorySize());
            // assertEquals(0, anotherStats.getOnDiskSize());
        }

        // key 1 should now be in the LruMemoryStore
        cache.get("key1");

        if (checkStats) {
            assertEquals(2, anotherStats.getCacheHitCount());
            assertEquals(1, anotherStats.getOnDiskHitCount());
            assertEquals(1, anotherStats.getInMemoryHitCount());
            assertEquals(0, anotherStats.getCacheMissCount());
        } else {
            assertEquals(0, anotherStats.getCacheHitCount());
            assertEquals(0, anotherStats.getOnDiskHitCount());
            assertEquals(0, anotherStats.getInMemoryHitCount());
            assertEquals(0, anotherStats.getCacheMissCount());
        }

        // Let the idle expire
        Thread.sleep(6000);

        // key 1 should now be expired
        cache.get("key1");
        if (checkStats) {
            assertEquals(2, anotherStats.getCacheHitCount());
            assertEquals(1, anotherStats.getOnDiskHitCount());
            assertEquals(1, anotherStats.getInMemoryHitCount());
            assertEquals(1, anotherStats.getCacheMissCount());
        } else {
            assertEquals(0, anotherStats.getCacheHitCount());
            assertEquals(0, anotherStats.getOnDiskHitCount());
            assertEquals(0, anotherStats.getInMemoryHitCount());
            assertEquals(0, anotherStats.getCacheMissCount());
        }

        // key 2 should also be expired
        cache.get("key1");
        if (checkStats) {
            assertEquals(2, anotherStats.getCacheHitCount());
            assertEquals(1, anotherStats.getOnDiskHitCount());
            assertEquals(1, anotherStats.getInMemoryHitCount());
            assertEquals(2, anotherStats.getCacheMissCount());
        } else {
            assertEquals(0, anotherStats.getCacheHitCount());
            assertEquals(0, anotherStats.getOnDiskHitCount());
            assertEquals(0, anotherStats.getInMemoryHitCount());
            assertEquals(0, anotherStats.getCacheMissCount());
        }

        cache.clearStatistics();
        // everything should be zero now
        assertEquals(0, anotherStats.getCacheHitCount());
        assertEquals(0, anotherStats.getOnDiskHitCount());
        assertEquals(0, anotherStats.getInMemoryHitCount());
        assertEquals(0, anotherStats.getCacheMissCount());

        assertNotNull(anotherStats.toString());
    }

    /**
     * Test average get time
     * 
     * @throws InterruptedException
     */
    @Test
    public void testAverageGetTime() throws InterruptedException {
        Cache cache = new Cache("test", 0, true, false, 5, 2);
        manager.addCache(cache);

        // add as a listener
        AnotherStatistics anotherStats = new AnotherStatistics();
        cache.registerCacheUsageListener(anotherStats);

        cache.setStatisticsEnabled(true);
        doTestAverageGetTime(cache, true, anotherStats);

        // test enable/disable statistics
        cache.setStatisticsEnabled(false);
        doTestAverageGetTime(cache, false, anotherStats);

        // remove the listener
        cache.removeCacheUsageListener(anotherStats);
        // enable statistics but don't check stats as no longer a listener
        cache.setStatisticsEnabled(true);
        doTestAverageGetTime(cache, false, anotherStats);

    }

    /**
     * Tests average get time
     */
    public void doTestAverageGetTime(Cache cache, boolean checkStats,
            AnotherStatistics anotherStats) {
        float averageGetTime = anotherStats.getAverageGetTimeMillis();
        assertTrue(0 == anotherStats.getAverageGetTimeMillis());

        for (int i = 0; i < 10000; i++) {
            cache.put(new Element("" + i, "value1"));
        }
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        for (int i = 0; i < 110000; i++) {
            cache.get("" + i);
        }

        averageGetTime = anotherStats.getAverageGetTimeMillis();
        if (checkStats) {
            assertTrue(averageGetTime >= .000001);
        } else {
            assertTrue(0 == averageGetTime);
        }
        cache.clearStatistics();
        assertTrue(0 == anotherStats.getAverageGetTimeMillis());
    }

    /**
     * Test cache eviction/expiry stats
     * 
     * @throws InterruptedException
     */
    @Test
    public void testEvictionStatistics() throws InterruptedException {
        // stats enabled, non-zero stats expected, as listener
        doTestEvictionStatistics(true, true, true);

        // stats disabled, non-zero stats NOT expected, as listener
        doTestEvictionStatistics(false, false, true);

        // stats enabled, non-zero stats NOT expected, NOT a listener
        doTestEvictionStatistics(true, false, false);
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
    public void doTestEvictionStatistics(boolean statsEnabled,
            boolean nonZeroStatsExpected, boolean asListener)
            throws InterruptedException {
        // run 5 times with random total and capacity values
        Random rand = new Random();
        int min = 100;
        for (int loop = 0; loop < 5; loop++) {
            int a = rand.nextInt(10000) + min;
            int b = rand.nextInt(10000) + min;
            if (a == b) {
                a += min;
            }
            int total = Math.max(a, b);
            int capacity = Math.min(a, b);
            Ehcache ehcache = new net.sf.ehcache.Cache("test-"
                    + nonZeroStatsExpected + "-" + loop, capacity, false,
                    false, 2, 2);
            manager.addCache(ehcache);
            AnotherStatistics anotherStats = new AnotherStatistics();
            if (asListener) {
                ehcache.registerCacheUsageListener(anotherStats);
            } else {
                // register and remove as we want to test remove listener
                ehcache.registerCacheUsageListener(anotherStats);
                ehcache.removeCacheUsageListener(anotherStats);
            }
            ehcache.setStatisticsEnabled(statsEnabled);

            assertEquals(0, anotherStats.getEvictedCount());

            for (int i = 0; i < total; i++) {
                ehcache.put(new Element("" + i, "value1"));
            }
            if (nonZeroStatsExpected) {
                assertEquals(total - capacity, anotherStats.getEvictedCount());
            } else {
                assertEquals(0, anotherStats.getEvictedCount());
            }

            Thread.sleep(3010);

            // expiries do not count as eviction
            if (nonZeroStatsExpected) {
                assertEquals(total - capacity, anotherStats.getEvictedCount());
            } else {
                assertEquals(0, anotherStats.getEvictedCount());
            }

            // no expiration till a get is tried
            assertEquals(0, anotherStats.getCacheMissCount());
            assertEquals(0, anotherStats.getCacheMissCountExpired());
            assertEquals(0, anotherStats.getExpiredCount());
            assertEquals(0, anotherStats.getCacheMissCount());

            for (int i = 0; i < total; i++) {
                ehcache.get("" + i);
            }

            if (nonZeroStatsExpected) {
                assertEquals(total, anotherStats.getCacheMissCount());
                assertEquals(capacity, anotherStats.getCacheMissCountExpired());
                assertEquals(capacity, anotherStats.getExpiredCount());
                assertEquals(total, anotherStats.getCacheMissCount());
                // assertEquals(0, anotherStats.getSize());
            } else {
                assertEquals(0, anotherStats.getCacheMissCount());
                assertEquals(0, anotherStats.getCacheMissCountExpired());
                assertEquals(0, anotherStats.getExpiredCount());
                assertEquals(0, anotherStats.getCacheMissCount());
                // assertEquals(0, anotherStats.getSize());
            }

            ehcache.clearStatistics();

            assertEquals(0, anotherStats.getCacheMissCount());
            assertEquals(0, anotherStats.getCacheMissCountExpired());
            assertEquals(0, anotherStats.getExpiredCount());
            assertEquals(0, anotherStats.getCacheMissCount());
            // assertEquals(0, anotherStats.getSize());
            
            manager.removeCache(ehcache.getName());
        }

    }

    /**
     * Test element put/update/remove
     * - putCount
     * - updateCount
     * - removeCount
     */
    @Test
    public void testPutUpdateRemoveStats() throws InterruptedException {

        // stats enabled, non-zero stats expected, as listener
        doTestElementUpdateRemove(true, true, true);

        // stats disabled, non-zero stats NOT expected, as listener
        doTestElementUpdateRemove(false, false, true);

        // stats enabled, non-zero stats NOT expected, NOT a listener
        doTestElementUpdateRemove(true, false, false);
    }

    public void doTestElementUpdateRemove(boolean statsEnabled,
            boolean nonZeroStatsExpected, boolean asListener)
            throws InterruptedException {
        Random rand = new Random();
        int min = 100;
        for (int loop = 0; loop < 5; loop++) {
            int total = rand.nextInt(10000) + min;

            // always ensure enough capacity. Otherwise cannot predict
            // updateCount with eviction (based on capacity)
            Ehcache ehcache = new net.sf.ehcache.Cache("test-"
                    + nonZeroStatsExpected + "-" + loop, total + 1, false,
                    false, 1200, 1200);
            manager.addCache(ehcache);
            // add as a listener
            AnotherStatistics anotherStats = new AnotherStatistics();
            if (asListener) {
                ehcache.registerCacheUsageListener(anotherStats);
            } else {
                // register and remove as we want to test remove listener
                ehcache.registerCacheUsageListener(anotherStats);
                ehcache.removeCacheUsageListener(anotherStats);
            }
            ehcache.setStatisticsEnabled(statsEnabled);

            assertEquals(0, anotherStats.getEvictedCount());
            assertEquals(0, anotherStats.getPutCount());
            assertEquals(0, anotherStats.getRemovedCount());
            assertEquals(0, anotherStats.getUpdateCount());

            for (int i = 0; i < total; i++) {
                ehcache.put(new Element("" + i, "value1"));
            }
            if (nonZeroStatsExpected) {
                assertEquals(total, anotherStats.getPutCount());
                assertEquals(0, anotherStats.getEvictedCount());
                // assertEquals(total, anotherStats.getSize());
                assertEquals(0, anotherStats.getUpdateCount());
                assertEquals(0, anotherStats.getRemovedCount());
            } else {
                assertEquals(0, anotherStats.getPutCount());
                assertEquals(0, anotherStats.getEvictedCount());
                // assertEquals(0, anotherStats.getSize());
                assertEquals(0, anotherStats.getRemovedCount());
                assertEquals(0, anotherStats.getUpdateCount());
            }

            // minimum 1 update
            int updates = rand.nextInt(total - 1) + 1;
            assertTrue(updates >= 1);
            for (int i = 0; i < updates; i++) {
                ehcache.put(new Element("" + i, "value1"));
            }
            if (nonZeroStatsExpected) {
                // assertEquals(total, anotherStats.getSize());
                assertEquals(updates, anotherStats.getUpdateCount());
                assertEquals(total, anotherStats.getPutCount());
                assertEquals(0, anotherStats.getEvictedCount());
                assertEquals(0, anotherStats.getRemovedCount());
            } else {
                // assertEquals(0, anotherStats.getSize());
                assertEquals(0, anotherStats.getPutCount());
                assertEquals(0, anotherStats.getRemovedCount());
                assertEquals(0, anotherStats.getEvictedCount());
                assertEquals(0, anotherStats.getUpdateCount());
            }

            // minimum 1 remove
            int remove = rand.nextInt(total - 1) + 1;
            assertTrue(updates >= 1);
            for (int i = 0; i < remove; i++) {
                ehcache.remove("" + i);
            }
            if (nonZeroStatsExpected) {
                // assertEquals(total - remove, anotherStats.getSize());
                assertEquals(updates, anotherStats.getUpdateCount());
                assertEquals(remove, anotherStats.getRemovedCount());
                assertEquals(total, anotherStats.getPutCount());
                assertEquals(0, anotherStats.getEvictedCount());
            } else {
                // assertEquals(0, anotherStats.getSize());
                assertEquals(0, anotherStats.getPutCount());
                assertEquals(0, anotherStats.getRemovedCount());
                assertEquals(0, anotherStats.getEvictedCount());
                assertEquals(0, anotherStats.getUpdateCount());
            }

            ehcache.clearStatistics();

            assertEquals(0, anotherStats.getPutCount());
            assertEquals(0, anotherStats.getRemovedCount());
            assertEquals(0, anotherStats.getEvictedCount());
            assertEquals(0, anotherStats.getUpdateCount());
            
            manager.removeCache(ehcache.getName());
        }

    }

    /**
     * CacheStatistics should always be sensible when the cache has not
     * started.
     */
    @Test
    public void testCacheAlive() {
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        String string = cache.toString();
        assertTrue(string.contains("test"));
        try {
            CacheUsageStatistics statistics = cache.getCacheUsageStatistics();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The test Cache is not alive.", e.getMessage());
        }
        // initialize cache now
        manager.addCache(cache);
        // add as a listener
        AnotherStatistics anotherStats = new AnotherStatistics();
        cache.registerCacheUsageListener(anotherStats);
        assertEquals(0, anotherStats.getCacheHitCount());
    }

}
