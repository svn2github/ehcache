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

package net.sf.ehcache.store;

import static junit.framework.Assert.assertTrue;
import net.sf.ehcache.Element;
import net.sf.ehcache.MemoryStoreTester;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Test cases for the LruMemoryStore.
 * <p/>
 * There are no tests of expiry because this is handled by {@link net.sf.ehcache.Cache#get}
 * <p/>
 * <b>Performance:</b>
 * v1.38 of DiskStore
 * INFO: Time for benhmarkPutGetSurya: 7355
 * INFO: Time for Bulk Load: 13
 * INFO: Time for benhmarkPutGetRemove: 264
 * INFO: Time for benhmarkPutGet: 154
 * <p/>
 * v 1.42 of DiskStore
 * INFO: Time for Bulk Load: 12
 * INFO: Time for benhmarkPutGetRemove: 256
 * INFO: Time for benhmarkPutGet: 165
 *
 * @author Greg Luck
 * @version $Id$
 */
public class LruMemoryStoreTest extends MemoryStoreTester {

    private static final Logger LOG = Logger.getLogger(LruMemoryStoreTest.class.getName());

    /**
     * setup test
     */
    @Override
    @Before
    public void setUp() throws Exception {

        super.setUp();
        createMemoryStore(MemoryStoreEvictionPolicy.LRU);
    }


    /**
     * Test the LRU policy
     */
    @Test
    public void testPolicy() throws Exception {
        createMemoryStore(MemoryStoreEvictionPolicy.LRU, 5);

        //Make sure that the store is empty to start with
        assertEquals(0, cache.getSize());

        // Populate the store till the max limit
        Element element = new Element("key1", "value1");
        cache.put(element);
        assertEquals(1, store.getSize());

        element = new Element("key2", "value2");
        cache.put(element);
        assertEquals(2, store.getSize());

        Thread.sleep(1020);
        element = new Element("key3", "value3");
        cache.put(element);
        assertEquals(3, store.getSize());

        element = new Element("key4", "value4");
        cache.put(element);
        assertEquals(4, store.getSize());

        element = new Element("key5", "value5");
        cache.put(element);
        assertEquals(5, store.getSize());

        // Now access the elements to boost the hits count, although irrelevant for this test just to demonstrate
        // hit count is immaterial for this test.
        cache.get("key1");
        cache.get("key1");
        Thread.sleep(1020);
        cache.get("key3");
        cache.get("key3");
        cache.get("key3");
        Thread.sleep(1020);
        cache.get("key4");

        //Create a new element and put in the store so as to force the policy
        element = new Element("key6", "value6");
        cache.put(element);

        Thread.sleep(1020);
        cache.get("key6");

        //max size
        assertEquals(5, store.getSize());

        //The element with key "key2" should be the least recently used
        assertNull(store.get("key2"));
        cache.get("key2");

        // Make some more accesses
        Thread.sleep(1020);
        cache.get("key5");
        cache.get("key5");

        // Insert another element to force the policy
        element = new Element("key7", "value7");
        cache.put(element);
        assertEquals(5, store.getSize());

        //key1 should now be the least recently used.
        assertNull(store.get("key1"));
    }

    /**
     * Test the LRU policy
     */
    @Test
    public void testProbabilisticEvictionPolicy() throws Exception {
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.LRU, 500);

        //Make sure that the store is empty to start with
        assertEquals(0, cache.getSize());

        // Populate the store till the max limit
        Element element = new Element("key1", "value1");
        for (int i = 0; i < 500; i++) {
            cache.put(new Element("" + i, "value1"));
        }
        Thread.sleep(1010);
        for (int i = 0; i < 500; i++) {
            cache.get("" + i);
        }

        //evict some
        for (int i = 501; i < 750; i++) {
            cache.put(new Element("" + i, "value1"));
        }

        int lastPutCount = 0;
        for (int i = 501; i < 750; i++) {
            if (cache.get("" + i) != null) {
                lastPutCount++;
            }
        }

        assertTrue("Ineffective eviction algorithm. Less than 230 of the last 249 put Elements remain.", lastPutCount >= 230);
    }


    /**
     * Benchmark to test speed. This uses both memory and disk and tries to be realistic
     * v 1.38 DiskStore 7355
     * v 1.41 DiskStore 1609
     * Adjusted for change to laptop
     */
    @Test
    public void testBenchmarkPutGetSurya() throws Exception {
        benchmarkPutGetSuryaTest(2500);
    }


    /**
     * Multi-thread read, put and removeAll test.
     * This checks for memory leaks
     * using the removeAll which was the known cause of memory leaks with MemoryStore in JCS
     */
    @Override
    @Test
    public void testMemoryLeak() throws Exception {
        super.testMemoryLeak();
    }


    /**
     * Specifically to verify the sampling algorithm.
     * <p/>
     * This test demonstrates a memory leak if we let the store simply get bigger on an eviction sampling miss.
     * as is done in r960. e.g.
     * Jun 9, 2009 10:47:30 AM net.sf.ehcache.store.LruMemoryStoreTest testMemoryLeakPutGetRemove
     * INFO: Store size is: 12538
     * Jun 9, 2009 10:47:32 AM net.sf.ehcache.store.LruMemoryStoreTest testMemoryLeakPutGetRemove
     * INFO: Store size is: 13527
     * Jun 9, 2009 10:47:34 AM net.sf.ehcache.store.LruMemoryStoreTest testMemoryLeakPutGetRemove
     * INFO: Store size is: 14475
     * Jun 9, 2009 10:47:37 AM net.sf.ehcache.store.LruMemoryStoreTest testMemoryLeakPutGetRemove
     * INFO: Store size is: 15446
     * Jun 9, 2009 10:47:39 AM net.sf.ehcache.store.LruMemoryStoreTest testMemoryLeakPutGetRemove
     * INFO: Store size is: 16424
     * Jun 9, 2009 10:47:41 AM net.sf.ehcache.store.LruMemoryStoreTest testMemoryLeakPutGetRemove
     * INFO: Store size is: 17399
     * Jun 9, 2009 10:47:43 AM net.sf.ehcache.store.LruMemoryStoreTest testMemoryLeakPutGetRemove
     * INFO: Store size is: 18353
     * Jun 9, 2009 10:47:46 AM net.sf.ehcache.store.LruMemoryStoreTest testMemoryLeakPutGetRemove
     * INFO: Store size is: 19324
     * Jun 9, 2009 10:47:48 AM net.sf.ehcache.store.LruMemoryStoreTest testMemoryLeakPutGetRemove
     * INFO: Store size is: 20300
     * <p/>
     * Now fixed and this test consistently gives a size of 12000
     */
    @Test
    public void testMemoryLeakPutGetRemove() throws Exception {

        //use MemoryOnly to isolate out the effects of the DiskStore
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.LRU);

        final String key = "key";
        String value = "value";

        for (int j = 0; j < 1200000; j += 300000) {

            for (int i = j; i < 300000 + j; i++) {
                Element element = new Element(key + i, value);
                store.put(element);
            }
            assertEquals(12000, store.getSize());
            LOG.log(Level.INFO, "Store size is: " + store.getSize());
        }
    }


}
