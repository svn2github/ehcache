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

package net.sf.ehcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.sf.ehcache.statisticsV2.CoreStatistics;
import net.sf.ehcache.statisticsV2.FlatCoreStatistics;
import net.sf.ehcache.statisticsV2.FlatStatistics;
import net.sf.ehcache.statisticsV2.StatisticsPlaceholder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;

/**
 * Tests for the statistics class
 *
 * @author Greg Luck
 * @version $Id$
 */
public class StatisticsTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsTest.class.getName());

    /**
     * Test statistics directly from Statistics Object
     */
    @Test
    public void testStatisticsFromStatisticsObject()
            throws InterruptedException {
        // Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);

        cache.getStatistics().setStatisticsEnabled(true);

        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        // allow disk write thread to complete
        Thread.sleep(100);

        cache.get("key1");
        cache.get("key2");

        FlatStatistics statistics = cache.getStatistics();
        assertEquals(2, statistics.cacheHitCount());
        assertEquals(1, statistics.diskHitCount());
        assertEquals(1, statistics.localHeapHitCount());
        assertEquals(0, statistics.cacheMissCount());
        assertEquals(2, statistics.getObjectCount());
        assertEquals(1, statistics.getMemoryStoreObjectCount());
        assertEquals(2, statistics.getDiskStoreObjectCount());

        // key 2 should now be in the MemoryStore
        cache.get("key2");

        assertEquals(3, statistics.cacheHitCount());
        assertEquals(1, statistics.diskHitCount());
        assertEquals(2, statistics.localHeapHitCount());
        assertEquals(0, statistics.cacheMissCount());

        // Let the idle expire
        Thread.sleep(6000);

        // key 1 should now be expired
        cache.get("key1");
        assertEquals(3, statistics.cacheHitCount());
        assertEquals(1, statistics.diskHitCount());
        assertEquals(2, statistics.localHeapHitCount());
        assertEquals(1, statistics.cacheMissCount());

        // key 2 should also be expired
        cache.get("key2");
        assertEquals(3, statistics.cacheHitCount());
        assertEquals(1, statistics.diskHitCount());
        assertEquals(2, statistics.localHeapHitCount());
        assertEquals(2, statistics.cacheMissCount());

        assertNotNull(statistics.toString());
    }

    /**
     * Test statistics directly from Statistics Object
     */
    @Test
    public void testClearStatistics() throws InterruptedException {
        // TODO CRSS
//        // Set size so the second element overflows to disk.
//        Cache cache = new Cache("test", 1, true, false, 5, 2);
//        manager.addCache(cache);
//
//        cache.getStatistics().setStatisticsEnabled(true);
//
//        cache.put(new Element("key1", "value1"));
//        cache.put(new Element("key2", "value1"));
//
//        // allow disk write thread to complete
//        Thread.sleep(100);
//
//        cache.get("key1");
//        cache.get("key2");
//
//        CoreStatistics statistics = cache.getStatistics().getCore();
//        assertEquals(2, statistics.getCacheHits());
//        assertEquals(1, statistics.getOnDiskHits());
//        assertEquals(1, statistics.getInMemoryHits());
//        assertEquals(0, statistics.getCacheMisses());
//
//        // clear stats
//        statistics.clearStatistics();
//        statistics = cache.getStatistics().getCore();
//        assertEquals(0, statistics.getCacheHits());
//        assertEquals(0, statistics.getOnDiskHits());
//        assertEquals(0, statistics.getInMemoryHits());
//        assertEquals(0, statistics.getCacheMisses());
    }

    /**
     * CacheStatistics should always be sensible when the cache has not started.
     */
    @Test
    public void testCacheStatisticsDegradesElegantlyWhenCacheDisposed() {
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        try {
            StatisticsPlaceholder statistics = cache.getStatistics();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The test Cache is not alive (STATUS_UNINITIALISED)", e.getMessage());
        }

    }



    /**
     * Tests average get time
     */
    @Test
    public void testAverageGetTime() {
        Ehcache cache = new Cache("test", 0, true, false, 5, 2);
        manager.addCache(cache);

        cache.getStatistics().setStatisticsEnabled(true);

        StatisticsPlaceholder statistics = cache.getStatistics();
        double averageGetTime = statistics.cacheGetOperation().latency().average().value();
        assertTrue(0 == statistics.cacheGetOperation().latency().average().value().intValue());

        for (int i = 0; i < 10000; i++) {
            cache.put(new Element("" + i, "value1"));
        }
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        for (int i = 0; i < 110000; i++) {
            cache.get("" + i);
        }

        statistics = cache.getStatistics();
        averageGetTime = statistics.cacheGetOperation().latency().average().value();
        assertTrue(averageGetTime >= .000001);
        statistics.clearStatistics();
        statistics = cache.getStatistics();
        assertTrue(0 == statistics.cacheGetOperation().latency().average().value().intValue());
    }

    /**
     * Tests eviction statistics
     */
    @Test
    public void testEvictionStatistics() throws InterruptedException {
        // set to 0 to make it run slow
        Ehcache ehcache = new net.sf.ehcache.Cache("test", 10, false, false, 2,
                2);
        manager.addCache(ehcache);

        ehcache.getStatistics().setStatisticsEnabled(true);

        StatisticsPlaceholder statistics = ehcache.getStatistics();
        assertEquals(0, statistics.getEvictionCount());

        for (int i = 0; i < 10000; i++) {
            ehcache.put(new Element("" + i, "value1"));
        }
        statistics = ehcache.getStatistics();
        assertEquals(9990, statistics.getEvictionCount());

        Thread.sleep(2010);

        // expiries do not count
        statistics = ehcache.getStatistics();
        assertEquals(9990, statistics.getEvictionCount());

        statistics.clearStatistics();

        statistics = ehcache.getStatistics();
        assertEquals(0, statistics.getEvictionCount());

    }

}
