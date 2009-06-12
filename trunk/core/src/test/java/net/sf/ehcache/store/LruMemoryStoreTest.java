/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

import net.sf.ehcache.Element;
import net.sf.ehcache.MemoryStoreTester;
import net.sf.ehcache.StopWatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    private static final Logger LOG = LoggerFactory.getLogger(LruMemoryStoreTest.class.getName());

    /**
     * setup test
     */
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
        assertEquals(0, store.getSize());

        // Populate the store till the max limit
        Element element = new Element("key1", "value1");
        store.put(element);
        assertEquals(1, store.getSize());

        element = new Element("key2", "value2");
        store.put(element);
        assertEquals(2, store.getSize());

        element = new Element("key3", "value3");
        store.put(element);
        assertEquals(3, store.getSize());

        element = new Element("key4", "value4");
        store.put(element);
        assertEquals(4, store.getSize());

        element = new Element("key5", "value5");
        store.put(element);
        assertEquals(5, store.getSize());

        // Now access the elements to boost the hits count, although irrelevant for this test just to demonstrate
        // hit count is immaterial for this test.
        store.get("key1");
        store.get("key1");
        Thread.sleep(15);
        store.get("key3");
        store.get("key3");
        store.get("key3");
        Thread.sleep(15);
        store.get("key4");

        //Create a new element and put in the store so as to force the policy
        element = new Element("key6", "value6");
        store.put(element);

        Thread.sleep(15);
        store.get("key6");

        //max size
        assertEquals(5, store.getSize());

        //The element with key "key2" should be the least recently used
        assertNull(store.get("key2"));

        // Make some more accesses
        Thread.sleep(15);
        store.get("key5");
        store.get("key5");

        // Insert another element to force the policy
        element = new Element("key7", "value7");
        store.put(element);
        assertEquals(5, store.getSize());

        //key1 should now be the least recently used.
        assertNull(store.get("key1"));
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
     *
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
            LOG.info("Store size is: " + store.getSize());
        }
    }




}
