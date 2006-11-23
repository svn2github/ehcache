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

package net.sf.ehcache;

import net.sf.ehcache.distribution.JVMUtil;
import net.sf.ehcache.store.LruMemoryStoreTest;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Store;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Other than policy differences, the Store implementations should work identically
 *
 * @author Greg Luck
 * @version $Id$
 */
public class MemoryStoreTester extends AbstractCacheTest {

    private static final Log LOG = LogFactory.getLog(MemoryStoreTester.class.getName());

    /**
     * The memory store that tests will be performed on
     */
    protected Store store;


    /**
     * The cache under test
     */
    protected Cache cache;


    /**
     * For automatic suite generators
     */
    public void testNoop() {
        //noop
    }

    /**
     * setup test
     */
    protected void setUp() throws Exception {
        manager = CacheManager.getInstance();
    }

    /**
     * teardown
     */
    protected void tearDown() throws Exception {
        if (manager != null) {
            manager.shutdown();
        }
    }

    /**
     * Creates a cache with the given policy and adds it to the manager.
     *
     * @param evictionPolicy
     * @throws CacheException
     */
    protected void createMemoryStore(MemoryStoreEvictionPolicy evictionPolicy) throws CacheException {
        cache = new Cache("test", 1000, evictionPolicy, true, null, true, 60, 30, false, 60, null);
        manager.addCache(cache);
        store = cache.getMemoryStore();
    }

    /**
     * Creates a cache with the given policy and adds it to the manager.
     *
     * @param evictionPolicy
     * @throws CacheException
     */
    protected void createMemoryStore(MemoryStoreEvictionPolicy evictionPolicy, int memoryStoreSize) throws CacheException {
        manager.removeCache("test");
        cache = new Cache("test", memoryStoreSize, evictionPolicy, true, null, true, 60, 30, false, 60, null);
        manager.addCache(cache);
        store = cache.getMemoryStore();
    }

    /**
     * Creates a store from the given configuration and cache within it.
     *
     * @param filePath
     * @param cacheName
     * @throws CacheException
     */
    protected void createMemoryStore(String filePath, String cacheName) throws CacheException {
        manager.shutdown();
        manager = CacheManager.create(filePath);
        cache = manager.getCache(cacheName);
        store = cache.getMemoryStore();
    }


    /**
     * Test elements can be put in the store
     */
    protected void putTest() throws IOException {
        Element element;

        assertEquals(0, store.getSize());

        element = new Element("key1", "value1");
        store.put(element);
        assertEquals(1, store.getSize());
        assertEquals("value1", store.get("key1").getObjectValue());

        element = new Element("key2", "value2");
        store.put(element);
        assertEquals(2, store.getSize());
        assertEquals("value2", store.get("key2").getObjectValue());

        for (int i = 0; i < 2000; i++) {
            store.put(new Element("" + i, new Date()));
        }

        assertEquals(4, store.getSize());
        assertEquals(2002, cache.getSize());
        assertEquals(1998, cache.getDiskStoreSize());
    }

    /**
     * Test elements can be removed from the store
     */
    protected void removeTest() throws IOException {
        Element element;

        element = new Element("key1", "value1");
        store.put(element);
        assertEquals(1, store.getSize());

        store.remove("key1");
        assertEquals(0, store.getSize());

        store.put(new Element("key2", "value2"));
        store.put(new Element("key3", "value3"));
        assertEquals(2, store.getSize());

        assertNotNull(store.remove("key2"));
        assertEquals(1, store.getSize());

        // Try to remove an object that is not there in the store
        assertNull(store.remove("key4"));
        assertEquals(1, store.getSize());

        //check no NPE on key handling
        assertNull(store.remove(null));

    }


    /**
     * Check no NPE on put
     */
    public void testNullPut() throws IOException {
        store.put(null);
    }

    /**
     * Check no NPE on get
     */
    public void testNullGet() throws IOException {
        assertNull(store.get(null));
    }

    /**
     * Check no NPE on remove
     */
    public void testNullRemove() throws IOException {
        assertNull(store.remove(null));
    }

    /**
     * Tests looking up an entry that does not exist.
     */
    public void testGetUnknown() throws Exception {
        final Element element = store.get("key");
        assertNull(element);
    }

    /**
     * Tests adding an entry.
     */
    public void testPut() throws Exception {
        final String value = "value";
        final String key = "key";

        // Make sure the element is not found
        assertEquals(0, store.getSize());
        Element element = store.get(key);
        assertNull(element);

        // Add the element
        element = new Element(key, value);
        store.put(element);

        // Get the element
        assertEquals(1, store.getSize());
        element = store.get(key);
        assertNotNull(element);
        assertEquals(value, element.getObjectValue());
    }

    /**
     * Tests removing an entry.
     */
    public void testRemove() throws Exception {
        final String value = "value";
        final String key = "key";

        // Add the entry

        Element element = new Element(key, value);
        store.put(element);

        // Check the entry is there
        assertEquals(1, store.getSize());
        element = store.get(key);
        assertNotNull(element);

        // Remove it
        store.remove(key);

        // Check the entry is not there
        assertEquals(0, store.getSize());
        element = store.get(key);
        assertNull(element);
    }

    /**
     * Tests removing all the entries.
     */
    public void testRemoveAll() throws Exception {
        final String value = "value";
        final String key = "key";

        // Add the entry
        Element element = new Element(key, value);
        store.put(element);

        // Check the entry is there
        element = store.get(key);
        assertNotNull(element);

        // Remove it
        store.removeAll();

        // Check the entry is not there
        assertEquals(0, store.getSize());
        element = store.get(key);
        assertNull(element);
    }

    /**
     * Tests bulk load.
     */
    public void testBulkLoad() throws Exception {
        final Random random = new Random();
        StopWatch stopWatch = new StopWatch();


        // Add a bunch of entries
        for (int i = 0; i < 500; i++) {
            // Use a random length value
            final String key = "key" + i;
            final String value = "value" + random.nextInt(1000);

            // Add an element, and make sure it is present
            Element element = new Element(key, value);
            store.put(element);
            element = store.get(key);
            assertNotNull(element);

            // Remove the element
            store.remove(key);
            element = store.get(key);
            assertNull(element);

            element = new Element(key, value);
            store.put(element);
            element = store.get(key);
            assertNotNull(element);
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for Bulk Load: " + time);
    }

    /**
     * Benchmark to test speed.
     */
    public void testBenchmarkPutGetRemove() throws Exception {
        final String key = "key";
        byte[] value = new byte[500];
        StopWatch stopWatch = new StopWatch();

        // Add a bunch of entries
        for (int i = 0; i < 50000; i++) {
            Element element = new Element(key, value);
            store.put(element);
            store.get(key + i);
        }
        for (int i = 0; i < 50000; i++) {
            store.remove(key + i);
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for benchmarkPutGetRemove: " + time);
        assertTrue("Too slow. Time was " + time, time < 500);
    }

    /**
     * Benchmark to test speed.
     */
    public void testBenchmarkPutGet() throws Exception {
        final String key = "key";
        byte[] value = new byte[500];
        StopWatch stopWatch = new StopWatch();

        // Add a bunch of entries
        for (int i = 0; i < 50000; i++) {
            Element element = new Element(key, value);
            store.put(element);
        }
        for (int i = 0; i < 50000; i++) {
            store.get(key + i);
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for benchmarkPutGet: " + time);
        assertTrue("Too slow. Time was " + time, time < 300);
    }


    /**
     * Benchmark to test speed.
     * Original implementation 12seconds
     * This implementation 9 seconds
     */
    public void benchmarkPutGetSuryaTest(long allowedTime) throws Exception {
        Random random = new Random();
        byte[] value = new byte[500];
        StopWatch stopWatch = new StopWatch();

        // Add a bunch of entries
        for (int i = 0; i < 50000; i++) {
            String key = "key" + i;

            Element element = new Element(key, value);
            store.put(element);

            //Access each element random number of times, min:0 maximum:9
            int accesses = random.nextInt(5);
            for (int j = 0; j <= accesses; j++) {
                store.get(key);
            }
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("Time for benchmarkPutGetSurya: " + time);
        assertTrue("Too slow. Time was " + time, time < allowedTime);
    }

    /**
     * Multi-thread read-only test.
     */
    public void testReadOnlyThreads() throws Exception {

        // Add a couple of elements
        store.put(new Element("key0", "value"));
        store.put(new Element("key1", "value"));

        // Run a set of threads, that attempt to fetch the elements
        final List executables = new ArrayList();
        for (int i = 0; i < 10; i++) {
            final String key = "key" + (i % 2);
            final MemoryStoreTester.Executable executable = new LruMemoryStoreTest.Executable() {
                public void execute() throws Exception {
                    final Element element = store.get(key);
                    assertNotNull(element);
                    assertEquals("value", element.getObjectValue());
                }
            };
            executables.add(executable);
        }
        runThreads(executables);
    }

    /**
     * Multi-thread read-write test.
     */
    public void testReadWriteThreads() throws Exception {

        final String value = "value";
        final String key = "key";

        // Add the entry
        final Element element = new Element(key, value);
        store.put(element);

        // Run a set of threads that get, put and remove an entry
        final List executables = new ArrayList();
        for (int i = 0; i < 5; i++) {
            final MemoryStoreTester.Executable executable = new MemoryStoreTester.Executable() {
                public void execute() throws Exception {
                    final Element element = store.get("key");
                    assertNotNull(element);
                }
            };
            executables.add(executable);
        }
        for (int i = 0; i < 5; i++) {
            final MemoryStoreTester.Executable executable = new MemoryStoreTester.Executable() {
                public void execute() throws Exception {
                    store.put(element);
                }
            };
            executables.add(executable);
        }

        runThreads(executables);
    }

    /**
     * Multi-thread read, put and removeAll test.
     * This checks for memory leaks
     * using the removeAll which was the known cause of memory leaks with MemoryStore in JCS
     */
    public void testMemoryLeak() throws Exception {
        long differenceMemoryCache = thrashCache();
        assertTrue(differenceMemoryCache < 500000);
    }


    /**
     * This method tries to get the store too leak.
     */
    protected long thrashCache() throws Exception {


        long startingSize = measureMemoryUse();
        LOG.info("Memory Used is: " + startingSize);

        final String value = "value";
        final String key = "key";

        // Add the entry
        final Element element = new Element(key, value);
        store.put(element);

        // Create 15 threads that read the keys;
        final List executables = new ArrayList();
        for (int i = 0; i < 15; i++) {
            final LruMemoryStoreTest.Executable executable = new MemoryStoreTester.Executable() {
                public void execute() throws Exception {
                    for (int i = 0; i < 500; i++) {
                        final String key = "key" + i;
                        store.get(key);
                    }
                    store.get("key");
                }
            };
            executables.add(executable);
        }
        //Create 15 threads that are insert 500 keys with large byte[] as values
        for (int i = 0; i < 15; i++) {
            final LruMemoryStoreTest.Executable executable = new MemoryStoreTester.Executable() {
                public void execute() throws Exception {

                    // Add a bunch of entries
                    for (int i = 0; i < 500; i++) {
                        // Use a random length value
                        final String key = "key" + i;
                        byte[] value = new byte[10000];
                        Element element = new Element(key, value);
                        store.put(element);
                    }
                }
            };
            executables.add(executable);
        }

        runThreads(executables);
        store.removeAll();

        long finishingSize = measureMemoryUse();
        LOG.info("Memory Used is: " + finishingSize);
        return finishingSize - startingSize;
    }


    /**
     * Multi-thread read-write test.
     */
    public void testReadWriteThreadsSurya() throws Exception {

        long start = System.currentTimeMillis();
        final List executables = new ArrayList();
        final Random random = new Random();

        // 50% of the time get data
        for (int i = 0; i < 10; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    store.get("key" + random.nextInt(10000));
                }
            };
            executables.add(executable);
        }

        //25% of the time add data
        for (int i = 0; i < 5; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    store.put(new Element("key" + random.nextInt(20000), "value"));
                }
            };
            executables.add(executable);
        }

        //25% if the time remove the data
        for (int i = 0; i < 5; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    store.remove("key" + random.nextInt(10000));
                }
            };
            executables.add(executable);
        }

        runThreads(executables);
        long end = System.currentTimeMillis();
        LOG.info("Total time for the test: " + (end + start) + " ms");
    }


    /**
     * Test behaviour of memory store using 1 million records.
     * This is expected to run out of memory on a 64MB machine. Where it runs out
     * is asserted so that design changes do not start using more memory per element.
     * <p/>
     * This test will fail (ie not get an out of memory error) on VMs configured to be server which do not have a fixed upper memory limit.
     * <p/>
     * Takes too long to run therefore switch off
     * <p/>
     * These memory size asserts were 100,000 and 60,000. The ApacheLRU map does not get quite as high numbers. 
     */
    public void testMemoryStoreOutOfMemoryLimit() throws Exception {
        //Set size so the second element overflows to disk.
        cache = new Cache("memoryLimitTest", 1000000, false, false, 500, 500);
        manager.addCache(cache);
        int i = 0;
        try {
            for (; i < 1000000; i++) {
                cache.put(new Element("" +
                        i, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        + "AAAAA " + i));
            }
            fail();
        } catch (OutOfMemoryError e) {
            if (JVMUtil.isJDK15()) {
                assertTrue(i > 90000);
            } else {
                assertTrue(i > 50000);
            }
            LOG.info("Ran out of memory putting " + i + "th element");
        }
    }

}
