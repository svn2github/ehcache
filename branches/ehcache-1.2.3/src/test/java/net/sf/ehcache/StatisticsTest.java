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

package net.sf.ehcache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * Tests for the statistics class
 *
 *
 * @author Greg Luck
 * @version $Id$
 */
public class StatisticsTest extends AbstractCacheTest {


    private static final Log LOG = LogFactory.getLog(StatisticsTest.class.getName());

    /**
     * Test statistics directly from Cache
     */
    public void testStatistics() throws InterruptedException {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);
        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));

        //key1 should be in the Disk Store
        cache.get("key1");
        assertEquals(1, cache.getHitCount());
        assertEquals(1, cache.getDiskStoreHitCount());
        assertEquals(0, cache.getMemoryStoreHitCount());
        assertEquals(0, cache.getMissCountExpired());
        assertEquals(0, cache.getMissCountNotFound());

        //key 1 should now be in the LruMemoryStore
        cache.get("key1");
        assertEquals(2, cache.getHitCount());
        assertEquals(1, cache.getDiskStoreHitCount());
        assertEquals(1, cache.getMemoryStoreHitCount());
        assertEquals(0, cache.getMissCountExpired());
        assertEquals(0, cache.getMissCountNotFound());

        //Let the idle expire
        Thread.sleep(5010);

        //key 1 should now be expired
        cache.get("key1");
        assertEquals(2, cache.getHitCount());
        assertEquals(1, cache.getDiskStoreHitCount());
        assertEquals(1, cache.getMemoryStoreHitCount());
        assertEquals(1, cache.getMissCountExpired());
        assertEquals(1, cache.getMissCountNotFound());
    }


    /**
     * Test statistics directly from Statistics Object
     */
    public void testStatisticsFromStatisticsObject() throws InterruptedException {
        //Set size so the second element overflows to disk.
        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);


        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        //key1 should be in the Disk Store
        cache.get("key1");

        Statistics statistics = (Statistics) cache.getStatistics();
        assertEquals(1, statistics.getCacheHits());
        assertEquals(1, statistics.getOnDiskHits());
        assertEquals(0, statistics.getInMemoryHits());
        assertEquals(0, statistics.getCacheMisses());

        //key 1 should now be in the LruMemoryStore
        cache.get("key1");

        statistics = (Statistics) cache.getStatistics();
        assertEquals(2, statistics.getCacheHits());
        assertEquals(1, statistics.getOnDiskHits());
        assertEquals(1, statistics.getInMemoryHits());
        assertEquals(0, statistics.getCacheMisses());

        //Let the idle expire
        Thread.sleep(5010);

        //key 1 should now be expired
        cache.get("key1");
        statistics = (Statistics) cache.getStatistics();
        assertEquals(2, statistics.getCacheHits());
        assertEquals(1, statistics.getOnDiskHits());
        assertEquals(1, statistics.getInMemoryHits());
        assertEquals(2, statistics.getCacheMisses());

        assertNotNull(statistics.toString());
    }


    /**
     * CacheStatistics should always be sensible when the cache has not started.
     */
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
    public void testSerialization() throws IOException, ClassNotFoundException {

        Cache cache = new Cache("test", 1, true, false, 5, 2);
        manager.addCache(cache);

        cache.put(new Element("key1", "value1"));
        cache.put(new Element("key2", "value1"));
        cache.get("key1");
        cache.get("key1");

        Statistics statistics = cache.getStatistics();
        assertEquals(2, statistics.getCacheHits());
        assertEquals(1, statistics.getOnDiskHits());
        assertEquals(1, statistics.getInMemoryHits());
        assertEquals(0, statistics.getCacheMisses());
        assertEquals(Statistics.STATISTICS_ACCURACY_BEST_EFFORT, statistics.getStatisticsAccuracy());
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


        //Check after Serialization
        assertEquals(2, afterDeserializationStatistics.getCacheHits());
        assertEquals(1, afterDeserializationStatistics.getOnDiskHits());
        assertEquals(1, afterDeserializationStatistics.getInMemoryHits());
        assertEquals(0, afterDeserializationStatistics.getCacheMisses());
        assertEquals(Statistics.STATISTICS_ACCURACY_BEST_EFFORT, statistics.getStatisticsAccuracy());
        statistics.clearStatistics();

    }


}
