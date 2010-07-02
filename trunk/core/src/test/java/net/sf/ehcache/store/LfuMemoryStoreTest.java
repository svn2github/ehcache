/**
 *  Copyright 2003-2010 Terracotta, Inc.
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
import net.sf.ehcache.store.compound.CompoundStore;
import net.sf.ehcache.store.compound.ElementSubstituteFilter;
import net.sf.ehcache.store.compound.factories.CapacityLimitedInMemoryFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for LfuMemoryStore
 * <p/>
 *
 * @author <a href="ssuravarapu@users.sourceforge.net">Surya Suravarapu</a>
 * @version $Id$
 */
public class LfuMemoryStoreTest extends MemoryStoreTester {

    private static final Logger LOG = LoggerFactory.getLogger(LfuMemoryStoreTest.class.getName());

    private static final Field  PRIMARY_FACTORY;
    private static final Method GET_EVICTION_TARGET;
    static {
        try {
            PRIMARY_FACTORY = CompoundStore.class.getDeclaredField("primary");
            PRIMARY_FACTORY.setAccessible(true);
            GET_EVICTION_TARGET = CapacityLimitedInMemoryFactory.class.getDeclaredMethod("getEvictionTarget", Object.class, Integer.TYPE);
            GET_EVICTION_TARGET.setAccessible(true);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * setup test
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.LFU);
    }


    /**
     * Check no NPE on get
     */
    @Override
    @Test
    public void testNullGet() throws IOException {
        assertNull(store.get(null));
    }

    /**
     * Check no NPE on remove
     */
    @Override
    @Test
    public void testNullRemove() throws IOException {
        assertNull(store.remove(null));
    }

    /**
     * Tests the put by reading the config file
     */
    @Test
    public void testPutFromConfigZeroMemoryStore() throws Exception {
        createMemoryStore(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-policy-test.xml", "sampleLFUCache2");
        Element element = new Element("1", "value");
        store.put(element);
        assertNotNull(store.get("1"));
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
     * Tests the LFU policy
     */
    @Test
    public void testLfuPolicy() throws Exception {
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.LFU, 4);
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


    private void lfuPolicyTest() throws IOException, InterruptedException {
        //Make sure that the store is empty to start with
        assertEquals(0, cache.getSize());

        // Populate the store till the max limit
        Element element = new Element("key1", "value1");
        cache.put(element);
        assertEquals(1, store.getInMemorySize());

        element = new Element("key2", "value2");
        cache.put(element);
        assertEquals(2, store.getInMemorySize());

        element = new Element("key3", "value3");
        cache.put(element);
        assertEquals(3, store.getInMemorySize());

        element = new Element("key4", "value4");
        cache.put(element);
        assertEquals(4, store.getInMemorySize());

        //Now access the elements to boost the hit count
        cache.get("key1");
        cache.get("key1");
        cache.get("key3");
        cache.get("key3");
        cache.get("key3");
        cache.get("key4");

        //Create a new element and put in the store so as to force the policy
        element = new Element("key5", "value5");
        cache.put(element);

        Thread.sleep(200);
        
        assertEquals(4, store.getInMemorySize());
        //The element with key "key2" is the LFU element so should be removed
        // directly access the memory store here since the LFU evicted elements have been flushed to the disk store
        assertFalse(((CompoundStore) store).unretrievedGet("key2") instanceof Element);

        // Make some more accesses
        cache.get("key5");
        cache.get("key5");

        // Insert another element to force the policy
        element = new Element("key6", "value6");
        cache.put(element);
        
        Thread.sleep(200);
        
        assertEquals(4, store.getInMemorySize());
        assertFalse(((CompoundStore) store).unretrievedGet("key2") instanceof Element);
    }


    /**
     * Multi-thread read, put and removeAll test.
     * This checks for memory leaks
     * using the removeAll which was the known cause of memory leaks with LruMemoryStore in JCS
     * new sampling LFU has no leaks
     */
    @Override
    @Test
    public void testMemoryLeak() throws Exception {
        super.testMemoryLeak();
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
        LOG.debug("Mean difference through iteration: " + absoluteDifferences / 500);

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


    private static final ElementSubstituteFilter<Element> IDENTITY_FILTER = new ElementSubstituteFilter<Element>() {
        public boolean allows(Object object) {
            return object instanceof Element;
        }
    };
    
    /**
     * Check nothing breaks and that we get the right number of samples
     *
     * @throws IOException
     */
    @Test
    public void testSampling() throws IOException {
        createMemoryOnlyStore(MemoryStoreEvictionPolicy.LFU, 1000);
        List<Element> elements = null;
        for (int i = 0; i < 10; i++) {
            store.put(new Element("" + i, new Date()));
            elements = ((CompoundStore) store).getRandomSample(IDENTITY_FILTER, i + 1, new Object());
        }

        for (int i = 10; i < 2000; i++) {
            store.put(new Element("" + i, new Date()));
            elements = ((CompoundStore) store).getRandomSample(IDENTITY_FILTER, 10, new Object());
            assertTrue(elements.size() >= 10);
        }
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


    /**
     * A simple persistent JavaBean
     *
     * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
     * @version $Id$
     */
    private final class TestBean implements Serializable {

        private String string;

        private TestBean() {
            //noop
        }

        /**
         * Constructor
         *
         * @param string
         */
        private TestBean(String string) {
            this.string = string;
        }
    }


}



