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

package net.sf.ehcache.store;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Element;
import net.sf.ehcache.MemoryStoreTester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Test class for FifoMemoryStore
 * <p/>
 *
 * @author <a href="ssuravarapu@users.sourceforge.net">Surya Suravarapu</a>
 * @author Greg Luck
 * @version $Id$
 */
public class FifoMemoryStoreTest extends MemoryStoreTester {

    /**
     * setup test
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.FIFO);
    }

    /**
     * Tests adding an entry.
     */
    @Override
    @Test
    public void testPut() throws Exception {
        putTest();
    }

    /**
     * Tests put by using the parameters specified in the config file
     */
    @Test
    public void testPutFromConfig() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleFIFOCache2");
        putTest();
    }

    /**
     * Put test and check policy
     *
     * @throws IOException
     */
    @Override
    protected void putTest() throws IOException {
        Element element;

        // Make sure the element is not found
        assertEquals(0, store.getSize());

        // Add the element
        element = new Element("key1", "value1");
        store.put(element);

        //Add another element
        store.put(new Element("key2", "value2"));
        assertEquals(2, store.getSize());

        // Get the element
        element = store.get("key1");
        assertNotNull(element);
        //FIFO
        assertEquals("value1", element.getObjectValue());
        assertEquals(2, store.getSize());


    }

    /**
     * Can we deal with NonSerializable objects?
     */
    @Test
    public void testNonSerializable() {
        /**
         * Non-serializable test class
         */
        class NonSerializable {
            //
        }
        NonSerializable key = new NonSerializable();
        store.put(new Element(key, new NonSerializable()));
        store.get(key);
    }

    /**
     * Tests removing the entries
     */
    @Override
    @Test
    public void testRemove() throws Exception {
        removeTest();
    }

    /**
     * Tests remove by using the parameters specified in the config file
     */
    @Test
    public void testRemoveFromConfig() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleFIFOCache2");
        removeTest();
    }


    /**
     * Remove test and check policy
     *
     * @throws IOException
     */
    @Override
    protected void removeTest() throws IOException {
        Element element;

        //Make sure there are no elements
        assertEquals(0, store.getSize());

        //Add a few elements
        element = new Element("key1", "value1");
        store.put(element);

        element = new Element("key2", "value2");
        store.put(element);

        element = new Element("key3", "value3");
        store.put(element);

        // Make sure that the all the above elements are added to the list
        assertEquals(3, store.getSize());

        store.remove("key1");
        assertEquals(2, store.getSize());

        store.remove("key2");
        assertEquals(1, store.getSize());

        store.remove("key3");
        assertEquals(0, store.getSize());
    }

    /**
     * Test the policy
     */
    @Test
    public void testFifoPolicy() throws Exception {
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.FIFO, 5);
        fifoPolicyTest();
    }

    /**
     * Test the ploicy by using the parameters specified in the config file
     */
    @Test
    public void testFifoPolicyFromConfig() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleFIFOCache2");
        fifoPolicyTest();
    }

    private void fifoPolicyTest() throws IOException, InterruptedException {
        //Make sure that the store is empty to start with
        assertEquals(0, cache.getSize());

        // Populate the store till the max limit
        Element element = new Element("key1", "value1");
        cache.put(element);
        Thread.sleep(1015);
        assertEquals(1, store.getSize());

        element = new Element("key2", "value2");
        cache.put(element);
        Thread.sleep(1015);
        assertEquals(2, store.getSize());

        element = new Element("key3", "value3");
        cache.put(element);
        Thread.sleep(1015);
        assertEquals(3, store.getSize());

        element = new Element("key4", "value4");
        cache.put(element);
        Thread.sleep(1015);
        assertEquals(4, store.getSize());

        element = new Element("key5", "value5");
        cache.put(element);
        Thread.sleep(1015);
        assertEquals(5, store.getSize());

        // Now access the elements to boost the hits count, although irrelevant for this test just to demonstrate
        // hit count is immaterial for this test.
        cache.get("key1");
        cache.get("key1");
        cache.get("key3");
        cache.get("key3");
        cache.get("key3");
        cache.get("key4");

        //Create a new element and put in the store so as to force the policy
        element = new Element("key6", "value6");
        cache.put(element);
        Thread.sleep(1015);

        // max size
        assertEquals(5, store.getSize());

        // The element with key "key1" is the First-In element so should be First-Out
        // directly access the memory store here since the LFU evicted elements have been flushed to the disk store
        assertNull(store.get("key1"));

        // Make some more accesses
        cache.get("key5");
        cache.get("key5");
        Thread.sleep(1015);

        // Insert another element to force the policy
        element = new Element("key7", "value7");
        cache.put(element);
        assertEquals(5, store.getSize());
        assertNull(store.get("key2"));
    }
}
