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
 * @version $Id: LruMemoryStoreTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
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
     * Benchmark to test speed. This uses both memory and disk and tries to be realistic
     * v 1.38 DiskStore 7355
     * v 1.41 DiskStore 1609
     * Adjusted for change to laptop
     */
    public void testBenchmarkPutGetSurya() throws Exception {
        benchmarkPutGetSuryaTest(2500);
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


}
