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

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.MemoryStoreTester;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.StopWatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Test class for LfuMemoryStore
 * <p/>
 *
 * @author <a href="ssuravarapu@users.sourceforge.net">Surya Suravarapu</a>
 * @version $Id$
 */
public class LfuMemoryStoreTest extends MemoryStoreTester {

    private static final Logger LOG = Logger.getLogger(LfuMemoryStoreTest.class.getName());

    /**
     * setup test
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createMemoryStore(MemoryStoreEvictionPolicy.LFU);
    }


    /**
     * Check no NPE on get
     */
    @Test
    public void testNullGet() throws IOException {
        assertNull(store.get(null));
    }

    /**
     * Check no NPE on remove
     */
    @Test
    public void testNullRemove() throws IOException {
        assertNull(store.remove(null));
    }


    /**
     * todo starting breaking 16/11/07 ?
     * Tests the put by reading the config file
     */
    public void xTtestPutFromConfig() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleLFUCache1");
        putTest();
    }

    /**
     * Tests the put by reading the config file
     */
    @Test
    public void testPutFromConfigZeroMemoryStore() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleLFUCache2");
        Element element = new Element("1", "value");
        store.put(element);
        assertNull(store.get("1"));
    }

    /**
     * Tests the remove() method by using the parameters specified in the config file
     */
    @Test
    public void testRemoveFromConfig() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleLFUCache1");
        removeTest();
    }

    /**
     * Benchmark to test speed.
     * This takes a little longer for LFU than the others.
     * Used to take about 7400ms. Now takes 827.
     */
    @Test
    public void testBenchmarkPutGetSurya() throws Exception {
        benchmarkPutGetSuryaTest(9000);
    }

    /**
     * Tests the LFU policy
     */
    @Test
    public void testLfuPolicy() throws Exception {
        createMemoryStore(MemoryStoreEvictionPolicy.LFU, 4);
        lfuPolicyTest();
    }

    /**
     * Tests the LFU policy by using the parameters specified in the config file
     */
    @Test
    public void testLfuPolicyFromConfig() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleLFUCache1");
        lfuPolicyTest();
    }


    private void lfuPolicyTest() throws IOException {
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

        //Now access the elements to boost the hit count
        store.get("key1");
        store.get("key1");
        store.get("key3");
        store.get("key3");
        store.get("key3");
        store.get("key4");

        //Create a new element and put in the store so as to force the policy
        element = new Element("key5", "value5");
        store.put(element);

        assertEquals(4, store.getSize());
        //The element with key "key2" is the LFU element so should be removed
        assertNull(store.get("key2"));

        // Make some more accesses
        store.get("key5");
        store.get("key5");

        // Insert another element to force the policy
        element = new Element("key6", "value6");
        store.put(element);
        assertEquals(4, store.getSize());
        assertNull(store.get("key4"));


    }

    /**
     * Benchmark to test speed.
     * new sampling LFU 417ms
     */
    @Test
    public void testBenchmarkPutGetRemove() throws Exception {
        super.testBenchmarkPutGetRemove();
    }


    /**
     * Multi-thread read, put and removeAll test.
     * This checks for memory leaks
     * using the removeAll which was the known cause of memory leaks with LruMemoryStore in JCS
     * new sampling LFU has no leaks
     */
    @Test
    public void testMemoryLeak() throws Exception {
        super.testMemoryLeak();
    }

    /**
     * Benchmark to test speed.
     * new sampling LFU 132ms
     */
    @Test
    public void testBenchmarkPutGet() throws Exception {
        super.testBenchmarkPutGet();
    }

    /**
     * Tests how random the java.util.Map iteration is by measuring the differences in iterate order.
     * <p/>
     * If iterate was ordered in either insert or reverse insert order the mean difference would be 1.
     * Using Random gives a mean difference of 343.
     * The observed value is 75, always 75 for a key set of 500 because it always iterates in the same order,
     * just not an obvious order.
     * <p/>
     * Conclusion: Unable to use the iterator as a pseudorandom selector.
     */
    @Test
    public void testRandomnessOfIterator() {
        int mean = 0;
        int absoluteDifferences = 0;
        int lastReading = 0;
        Map map = new ConcurrentHashMap();
        for (int i = 1; i <= 500; i++) {
            mean += i;
            map.put("" + i, " ");
        }
        mean = mean / 500;
        for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
            String string = (String) iterator.next();
            int thisReading = Integer.parseInt(string);
            LOG.info("reading: " + thisReading);
            absoluteDifferences += Math.abs(lastReading - thisReading);
            lastReading = thisReading;
        }
        LOG.fine("Mean difference through iteration: " + absoluteDifferences / 500);

        //Random selection without replacement
        Random random = new Random();
        while (map.size() != 0) {
            int thisReading = random.nextInt(501);
            Object o = map.remove("" + thisReading);
            if (o == null) {
                continue;
            }
            absoluteDifferences += Math.abs(lastReading - thisReading);
            lastReading = thisReading;
        }
        LOG.info("Mean difference with random selection without replacement : " + absoluteDifferences / 500);
        LOG.info("Mean of range 1 - 500 : " + mean);

    }


    /**
     * HashMap
     * INFO: done putting: 128ms
     * INFO: 15ms
     * <p/>
     * ConcurrentHashMap
     * INFO: done putting: 200ms
     * INFO: 117ms
     * <p/>
     * ConcurrentHashMap
     */
    @Test
    public void testSpeedOfIteration() {
        StopWatch stopWatch = new StopWatch();
        Map map = new ConcurrentHashMap(100000);
        for (int i = 1; i <= 100000; i++) {
            map.put(i, i);
        }
        LOG.info("done putting: " + stopWatch.getElapsedTimeString());

        Collection collection =  map.values();
        for (Object o : collection) {
           // 
        }
        LOG.info(stopWatch.getElapsedTimeString());

    }

    /**
     * Check nothing breaks and that we get the right number of samples
     *
     * @throws IOException
     */
    @Test
    public void testSampling() throws IOException {
        createMemoryStore(MemoryStoreEvictionPolicy.LFU, 1000);
        Element[] elements = null;
        for (int i = 0; i < 10; i++) {
            store.put(new Element("" + i, new Date()));
            elements = ((LfuMemoryStore) store).sampleElements(i + 1);
        }

        for (int i = 10; i < 2000; i++) {
            store.put(new Element("" + i, new Date()));
            elements = ((LfuMemoryStore) store).sampleElements(10);
            assertEquals(10, elements.length);
        }
    }


    /**
     * Check we get reasonable results for 2000 entries where entry 0 is accessed once increasing to entry 1999 accessed
     * 2000 times.
     * <p/>
     * 1 to 5000 population, with hit counts ranging from 1 to 500, not selecting lowest half. 5000 tests
     * <p/>
     * Samples  Cost    No
     * 7        38      99.24% confidence
     * 8        27      99.46% confidence
     * 9        10
     * 10       11300 4       99.92% confidence
     * 12       2
     * 20 11428 0  99.99% confidence
     * <p/>
     * 1 to 5000 population, with hit counts ranging from 1 to 500, not selecting lowest quarter. 5000 tests
     * S        No
     * 10       291 94.18% confidence
     * 20       15
     * 30       11536 1 99.99% confidence
     * <p/>
     * For those with a statistical background the branch of stats which deals with this is hypothesis testing and
     * the Student's T distribution. The higher your sample the greater confidence you can have in a hypothesis, in
     * this case whether or not the "lowest" value lies in the bottom half or quarter of the distribution. Adding
     * samples rapidly increases confidence but the return from extra sampling rapidly diminishes.
     * <p/>
     * Cost is not affected much by sample size. Profiling shows that it is the iteration that is causing most of the
     * time. If we had access to the array backing Map, all would work very fast. Still, it is fast enough.
     * <p/>
     * A 99.99% confidence interval can be achieved that the "lowest" element is actually in the bottom quarter of the
     * hit count distribution.
     *
     * @throws IOException Performance:
     *                     With a sample size of 10: 523ms for 5000 runs = 104 ?s per run
     *                     With a sample size of 30: 628ms for 5000 runs = 125 ?s per run
     */
    @Test
    public void testLowest() throws IOException {
        createMemoryStore(MemoryStoreEvictionPolicy.LFU, 5000);
        Element element = null;
        Element newElement = null;
        for (int i = 0; i < 10; i++) {
            newElement = new Element("" + i, new Date());
            store.put(newElement);
            int j;
            for (j = 0; j <= i; j++) {
                store.get("" + i);
            }
            if (i > 0) {
                element = ((LfuMemoryStore) store).findRelativelyUnused(newElement);
                assertTrue(!element.equals(newElement));
                assertTrue(element.getHitCount() < 2);
            }
        }

        int lowestQuarterNotIdentified = 0;

        long findTime = 0;
        StopWatch stopWatch = new StopWatch();
        for (int i = 10; i < 5000; i++) {
            store.put(new Element("" + i, new Date()));
            int j;
            int maximumHitCount = 0;
            for (j = 0; j <= i; j += 10) {
                store.get("" + i);
                maximumHitCount++;
            }

            stopWatch.getElapsedTime();
            element = ((LfuMemoryStore) store).findRelativelyUnused(newElement);
            findTime += stopWatch.getElapsedTime();
            long lowest = element.getHitCount();
            long bottomQuarter = (Math.round(maximumHitCount / 4.0) + 1);
            assertTrue(!element.equals(newElement));
            if (lowest > bottomQuarter) {
                lowestQuarterNotIdentified++;
                //LOG.info(i + " " + maximumHitCount + " " + element);
            }
        }
        LOG.info("Find time: " + findTime);
        assertTrue(findTime < 1000);
        LOG.info("Selections not in lowest quartile: " + lowestQuarterNotIdentified);
        assertTrue(lowestQuarterNotIdentified <= 5);

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
     * Test which reproduced an issue with flushing of an LFU store to disk on shutdown
     */
    @Test
    public void testPersistLFUMemoryStore() {
        manager.shutdown();
        CacheManager cacheManager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml");
        Cache cache = cacheManager.getCache("test-cache");

        getTestBean(cache, "test1");
        getTestBean(cache, "test2");
        getTestBean(cache, "test1");
        getTestBean(cache, "test1");
        getTestBean(cache, "test3");
        getTestBean(cache, "test3");
        getTestBean(cache, "test4");
        getTestBean(cache, "test2");

        Statistics stats = cache.getStatistics();
        LOG.info(stats.toString());

        cacheManager.shutdown();
    }

    private TestBean getTestBean(Cache cache, String key) {
        Element element = cache.get(key);
        if (element == null) {
            element = new Element(key, new TestBean(key + "_value"));
            cache.put(element);
        }
        return (TestBean) element.getValue();
    }


}

/**
 * A simple persistent JavaBean
 *
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
class TestBean implements Serializable {

    private String string;

    /**
     * Constructor
     *
     * @param string
     */
    public TestBean(String string) {
        this.string = string;
    }
}

