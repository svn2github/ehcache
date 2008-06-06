/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

import java.util.Map;


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

    /**
     * setup test
     */
    protected void setUp() throws Exception {
        super.setUp();
        createMemoryStore(MemoryStoreEvictionPolicy.LRU);
    }

    /**
     * The LRU map implementation can be overridden by setting the "net.sf.ehcache.useLRUMap" System property.
     * Here we do not do that and it should be the java.util.LinkedHashMap.
     */
    public void testCorrectMapImplementation() throws Exception {
        createMemoryStore(MemoryStoreEvictionPolicy.LRU, 5);

        Map map = ((MemoryStore) store).getBackingMap();
        assertTrue(map instanceof java.util.LinkedHashMap);
    }


    /**
     * Test the LRU policy
     */
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
        store.get("key3");
        store.get("key3");
        store.get("key3");
        store.get("key4");

        //Create a new element and put in the store so as to force the policy
        element = new Element("key6", "value6");
        store.put(element);

        //max size
        assertEquals(5, store.getSize());

        //The element with key "key2" should be the least recently used
        assertNull(store.get("key2"));

        // Make some more accesses
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
    public void testBenchmarkPutGetSurya() throws Exception {
        benchmarkPutGetSuryaTest(2500);
    }


}
