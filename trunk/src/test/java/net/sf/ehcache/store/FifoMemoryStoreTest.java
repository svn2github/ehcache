/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.MemoryStoreTester;
import net.sf.ehcache.AbstractCacheTest;

import java.io.IOException;

/**
 * Test class for FifoMemoryStore
 * <p>
 *
 * @author <a href="ssuravarapu@users.sourceforge.net">Surya Suravarapu</a>
 * @author Greg Luck
 * 
 * @version $Id: FifoMemoryStoreTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class FifoMemoryStoreTest extends MemoryStoreTester {

    /**
     * setup test
     */
    protected void setUp() throws Exception {
        super.setUp();
        createMemoryStore(MemoryStoreEvictionPolicy.FIFO);
    }

    /**
     * Tests adding an entry.
     */
    public void testPut() throws Exception {
        putTest();
    }

    /**
     * Tests put by using the parameters specified in the config file
     */
    public void testPutFromConfig() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleFIFOCache2");
        putTest();
    }

    /**
     * Put test and check policy
     * @throws IOException
     */
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
        assertEquals("value1", element.getValue());
        assertEquals(2, store.getSize());
    }

    /**
     * Tests removing the entries
     */
    public void testRemove() throws Exception {
        removeTest();
    }

    /**
     * Tests remove by using the parameters specified in the config file
     */
    public void testRemoveFromConfig() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleFIFOCache2");
        removeTest();
    }


    /**
     * Benchmark to test speed.
     * v 1.38 DiskStore 7238
     * v 1.42 DiskStore 1907
     */
    public void testBenchmarkPutGetSurya() throws Exception {
        benchmarkPutGetSuryaTest(3000);
    }


    /**
     * Remove test and check policy
     * @throws IOException
     */
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

        // Make sure that the elements are getting removed in the FIFO manner
        store.remove("key1");
        element = ((FifoMemoryStore)store).getFirstElement();
        assertEquals("value2", element.getValue());
        assertEquals(2, store.getSize());

        store.remove("key2");
        element = ((FifoMemoryStore)store).getFirstElement();
        assertEquals("value3", element.getValue());
        assertEquals(1, store.getSize());

        store.remove("key3");
        element = ((FifoMemoryStore)store).getFirstElement();
        assertNull(element);
        assertEquals(0, store.getSize());
    }

    /**
     * Test the policy
     */
    public void testFifoPolicy() throws Exception {
        createMemoryStore(MemoryStoreEvictionPolicy.FIFO, 5);
        fifoPolicyTest();
    }

    /**
     * Test the ploicy by using the parameters specified in the config file
     */
    public void testFifoPolicyFromConfig() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleFIFOCache2");
        fifoPolicyTest();
    }

    private void fifoPolicyTest() throws IOException {
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

        //The element with key "key1" is the First-In element so should be First-Out
        assertNull(store.get("key1"));

        // Make some more accesses
        store.get("key5");
        store.get("key5");

        // Insert another element to force the policy
        element = new Element("key7", "value7");
        store.put(element);
        assertEquals(5, store.getSize());
        assertNull(store.get("key2"));
    }
}
