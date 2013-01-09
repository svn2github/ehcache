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

import java.util.concurrent.ExecutionException;
import net.sf.ehcache.statistics.FlatStatistics;
import net.sf.ehcache.statistics.StatisticsPlaceholder;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Statistic;
import net.sf.ehcache.store.disk.DiskStoreHelper;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNot.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
    public void testStatisticsFromStatisticsObject() throws InterruptedException, ExecutionException {
        // Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        cache.getCacheConfiguration().setMaxEntriesLocalDisk(2);
        manager.addCache(cache);

        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        // allow disk write thread to complete
        DiskStoreHelper.flushAllEntriesToDisk(cache).get();

        cache.get("key2");
        cache.get("key1");

        FlatStatistics statistics = cache.getStatistics();
        assertEquals(2, statistics.cacheHitCount());
        assertEquals(1, statistics.localDiskHitCount());
        assertEquals(1, statistics.localHeapHitCount());
        assertEquals(0, statistics.cacheMissCount());
        assertEquals(2, statistics.getSize());
        assertEquals(1, statistics.getLocalHeapSize());
        assertEquals(2, statistics.getLocalDiskSize());

        // key 2 should now be in the MemoryStore
        cache.get("key1");

        assertEquals(3, statistics.cacheHitCount());
        assertEquals(1, statistics.localDiskHitCount());
        assertEquals(2, statistics.localHeapHitCount());
        assertEquals(0, statistics.cacheMissCount());

        // Let the idle expire
        Thread.sleep(6000);

        // key 1 should now be expired
        assertThat(cache.get("key1"), nullValue());
        assertEquals(3, statistics.cacheHitCount());
        assertEquals(1, statistics.localDiskHitCount());
        assertEquals(3, statistics.localHeapHitCount());
        assertEquals(1, statistics.cacheMissCount());

        // key 2 should also be expired
        assertThat(cache.get("key2"), nullValue());
        assertEquals(3, statistics.cacheHitCount());
        assertEquals(2, statistics.localDiskHitCount());
        assertEquals(3, statistics.localHeapHitCount());
        assertEquals(2, statistics.cacheMissCount());

        assertNotNull(statistics.toString());
    }

    /**
     * CacheStatistics should always be sensible when the cache has not started.
     */
    @Test
    public void testCacheStatisticsDegradesElegantlyWhenCacheDisposed() {
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        try {
            cache.getStatistics();
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

        StatisticsPlaceholder statistics = cache.getStatistics();
        Statistic<Double> averageGetTime = statistics.cacheGetOperation().latency().average();
        assertThat(averageGetTime.value(), is(Double.NaN));

        for (int i = 0; i < 10000; i++) {
            cache.put(new Element(Integer.valueOf(i), "value1"));
        }
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        for (int i = 0; i < 110000; i++) {
            cache.get(Integer.valueOf(i));
        }

        assertThat(averageGetTime.value(), not(Double.NaN));
    }

    /**
     * Tests eviction statistics
     */
    @Test
    public void testEvictionStatistics() throws InterruptedException {
        // set to 0 to make it run slow
        Ehcache ehcache = new net.sf.ehcache.Cache("test", 10, false, false, 2, 2);
        manager.addCache(ehcache);

        StatisticsPlaceholder statistics = ehcache.getStatistics();
        assertEquals(0, statistics.cacheEvictedCount());

        for (int i = 0; i < 10000; i++) {
            ehcache.put(new Element("" + i, "value1"));
        }
        assertEquals(9990, statistics.cacheEvictedCount());

        Thread.sleep(2010);

        // expiries do not count
        assertEquals(9990, statistics.cacheEvictedCount());
    }
}
