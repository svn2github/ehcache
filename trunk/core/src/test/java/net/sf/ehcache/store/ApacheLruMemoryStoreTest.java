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

import java.util.Map;


/**
 * Test cases for the Apache version of LruMemoryStore.
 * <p/>
 * @author Greg Luck
 * @version $Id$
 */
public class ApacheLruMemoryStoreTest extends LruMemoryStoreTest {


    /**
     * setup test
     */
    protected void setUp() throws Exception {
        System.setProperty("net.sf.ehcache.useLRUMap", "true");
        super.setUp();
    }                                                                                      


    /**
     * Put back system default.
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        System.getProperties().remove("net.sf.ehcache.useLRUMap");
        super.tearDown();
    }

    /**
     * The LRU map implementation can be overridden by setting the "net.sf.ehcache.useLRUMap" System property.
     */
    public void testCorrectMapImplementation() throws Exception {
        createMemoryStore(MemoryStoreEvictionPolicy.LRU, 5);

        Map map = ((MemoryStore)store).getBackingMap();
        assertTrue(map instanceof org.apache.commons.collections.LRUMap);
    }
}
