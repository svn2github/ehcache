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

package net.sf.ehcache.store;

/**
 * Runs DiskStoreTest using the LRUMap, which is used in JDK1.3 rather than the JDK 1.4 LinkedHashMap
 * @author Greg Luck
 * @version $Id: ApacheLruMemoryStoreTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
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
     * Benchmark to test speed. This uses both memory and disk and tries to be realistic
     * This one is a little slower than the JDK1.4.2 map.
     * 2079ms
     */
    public void testBenchmarkPutGetSurya() throws Exception {
        benchmarkPutGetSuryaTest(2500);
    }

}
