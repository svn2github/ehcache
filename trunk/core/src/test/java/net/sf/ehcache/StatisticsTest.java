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

        cache.setStatisticsEnabled(true);
        
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        // key1 should be in the Disk Store
        cache.get("key1");

        Statistics statistics = cache.getStatistics();
        assertEquals(1, statistics.getCacheHits());
        assertEquals(1, statistics.getOnDiskHits());
        assertEquals(0, statistics.getInMemoryHits());
        assertEquals(0, statistics.getCacheMisses());
        assertEquals(2, statistics.getObjectCount());
        assertEquals(1, statistics.getMemoryStoreObjectCount());
        assertEquals(1, statistics.getDiskStoreObjectCount());

        // key 1 should now be in the LruMemoryStore
        cache.get("key1");

        statistics = cache.getStatistics();
        assertEquals(2, statistics.getCacheHits());
        assertEquals(1, statistics.getOnDiskHits());
        assertEquals(1, statistics.getInMemoryHits());
        assertEquals(0, statistics.getCacheMisses());

        // Let the idle expire
        Thread.sleep(6000);

        // key 1 should now be expired
        cache.get("key1");
        statistics = cache.getStatistics();
        assertEquals(2, statistics.getCacheHits());
        assertEquals(1, statistics.getOnDiskHits());
        assertEquals(1, statistics.getInMemoryHits());
        assertEquals(1, statistics.getCacheMisses());

        // key 2 should also be expired
        cache.get("key2");
        statistics = cache.getStatistics();
        assertEquals(2, statistics.getCacheHits());
        assertEquals(1, statistics.getOnDiskHits());
        assertEquals(1, statistics.getInMemoryHits());
        assertEquals(2, statistics.getCacheMisses());

        assertNotNull(statistics.toString());
    }

    /**
     * Test statistics directly from Statistics Object
     */
    @Test
    public void testClearStatistics() throws InterruptedException {
        // Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);

        cache.setStatisticsEnabled(true);
        
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        // key1 should be in the Disk Store
        cache.get("key1");

        Statistics statistics = cache.getStatistics();
        assertEquals(1, statistics.getCacheHits());
        assertEquals(1, statistics.getOnDiskHits());
        assertEquals(0, statistics.getInMemoryHits());
        assertEquals(0, statistics.getCacheMisses());

        // clear stats
        statistics.clearStatistics();
        statistics = cache.getStatistics();
        assertEquals(0, statistics.getCacheHits());
        assertEquals(0, statistics.getOnDiskHits());
        assertEquals(0, statistics.getInMemoryHits());
        assertEquals(0, statistics.getCacheMisses());
    }

    /**
     * CacheStatistics should always be sensible when the cache has not started.
     */
    @Test
    public void testCacheStatisticsDegradesElegantlyWhenCacheDisposed() {
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        try {
            Statistics statistics = cache.getStatistics();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("The test Cache is not alive.", e.getMessage());
        }

    }

    /**
     * We want to be able to use Statistics as a value object.
     * We need to do some magic with the refernence held to Cache
     */
    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {

        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);

        cache.setStatisticsEnabled(true);
        
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        cache.get("key1");
        cache.get("key1");

        Statistics statistics = cache.getStatistics();
        assertEquals("test", statistics.getAssociatedCacheName());
        assertEquals(2, statistics.getCacheHits());
        assertEquals(1, statistics.getOnDiskHits());
        assertEquals(1, statistics.getInMemoryHits());
        assertEquals(0, statistics.getCacheMisses());
        assertEquals(Statistics.STATISTICS_ACCURACY_BEST_EFFORT, statistics
                .getStatisticsAccuracy());
        statistics.clearStatistics();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bout);
        oos.writeObject(statistics);
        byte[] serializedValue = bout.toByteArray();
        oos.close();
        Statistics afterDeserializationStatistics = null;
        ByteArrayInputStream bin = new ByteArrayInputStream(serializedValue);
        ObjectInputStream ois = new ObjectInputStream(bin);
        afterDeserializationStatistics = (Statistics) ois.readObject();
        ois.close();

        // Check after Serialization
        assertEquals("test", afterDeserializationStatistics.getAssociatedCacheName());
        assertEquals(2, afterDeserializationStatistics.getCacheHits());
        assertEquals(1, afterDeserializationStatistics.getOnDiskHits());
        assertEquals(1, afterDeserializationStatistics.getInMemoryHits());
        assertEquals(0, afterDeserializationStatistics.getCacheMisses());
        assertEquals(Statistics.STATISTICS_ACCURACY_BEST_EFFORT, statistics
                .getStatisticsAccuracy());
        statistics.clearStatistics();

    }

    /**
     * What happens when a long larger than int max value is cast to an int?
     * <p/>
     * The answer is that negative numbers are reported. The cast value is
     * incorrect.
     */
    @Test
    public void testIntOverflow() {

        long value = Integer.MAX_VALUE;
        value += Integer.MAX_VALUE;
        value += 5;
        LOG.info("" + value);
        int valueAsInt = (int) value;
        LOG.info("" + valueAsInt);
        assertEquals(3, valueAsInt);

    }

    /**
     * Tests average get time
     */
    @Test
    public void testAverageGetTime() {
        Ehcache cache = new Cache("test", 0, true, false, 5, 2);
        manager.addCache(cache);
        
        cache.setStatisticsEnabled(true);
        
        Statistics statistics = cache.getStatistics();
        float averageGetTime = statistics.getAverageGetTime();
        assertTrue(0 == statistics.getAverageGetTime());

        for (int i = 0; i < 10000; i++) {
            cache.put(new Element("" + i, "value1"));
        }
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        for (int i = 0; i < 110000; i++) {
            cache.get("" + i);
        }

        statistics = cache.getStatistics();
        averageGetTime = statistics.getAverageGetTime();
        assertTrue(averageGetTime >= .000001);
        statistics.clearStatistics();
        statistics = cache.getStatistics();
        assertTrue(0 == statistics.getAverageGetTime());
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
        
        ehcache.setStatisticsEnabled(true);
        
        Statistics statistics = ehcache.getStatistics();
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
