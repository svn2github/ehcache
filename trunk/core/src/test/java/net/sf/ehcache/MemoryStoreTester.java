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

package net.sf.ehcache;

import net.sf.ehcache.store.LruMemoryStoreTest;
import net.sf.ehcache.store.MemoryStore;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.Store;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Other than policy differences, the Store implementations should work identically
 *
 * @author Greg Luck
 * @version $Id$
 */
@Ignore
public class MemoryStoreTester extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryStoreTester.class.getName());

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
    @Test
    public void testNoop() {
        //noop
    }

    /**
     * setup test
     */
    @Override
    @Before
    public void setUp() throws Exception {
        manager = CacheManager.getInstance();
    }

    /**
     * teardown
     */
    @Override
    @After
    public void tearDown() throws Exception {
        try {
            if (cache != null) {
                cache.removeAll();
                cache = null;
            }
            if (manager != null) {
                manager.shutdown();
                manager = null;
            }
        } catch (OutOfMemoryError e) {
            //OutOfMemoryError Happens at different places on Apache LRU for some reason
            LOG.info(e.getMessage());
        } catch (Throwable t) {
            //OutOfMemoryError Happens at different places on Apache LRU for some reason
            LOG.info(t.getMessage());
        }
    }

    /**
     * Creates a cache with the given policy and adds it to the manager.
     *
     * @param evictionPolicy
     * @throws CacheException
     */
    protected void createMemoryOnlyStore(MemoryStoreEvictionPolicy evictionPolicy) throws CacheException {
        manager.removeCache("testMemoryOnly");
        cache = new Cache("testMemoryOnly", 12000, evictionPolicy, false, System.getProperty("java.io.tmpdir"),
                false, 60, 30, false, 60, null);
        manager.addCache(cache);
        store = cache.getStore();
    }

    /**
     * Creates a cache with the given policy with a MemoryStore only and adds it to the manager.
     *
     * @param evictionPolicy
     * @throws CacheException
     */
    protected void createMemoryOnlyStore(MemoryStoreEvictionPolicy evictionPolicy, int memoryStoreSize) throws CacheException {
        manager.removeCache("test");
        cache = new Cache("test", memoryStoreSize, evictionPolicy, false, null, false, 60, 30, false, 60, null);
        manager.addCache(cache);
        store = cache.getStore();
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
        store = cache.getStore();
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

        for (int i = 0; i < 1999; i++) {
            store.put(new Element("" + i, new Date()));
        }

        assertEquals(4, store.getSize());
        assertEquals(2001, cache.getSize());
        assertEquals(3998, cache.getDiskStoreSize());

        /**
         * Non serializable test class
         */
        class NonSerializable {
            //
        }

        store.put(new Element(new NonSerializable(), new NonSerializable()));

        assertEquals(4, store.getSize());
        assertEquals(2002, cache.getSize());
        assertEquals(1999, cache.getDiskStoreSize());
//        assertEquals(1998, cache.getDiskStoreSize());    ???

        //smoke test
        for (int i = 0; i < 2000; i++) {
            store.get("" + i);
        }
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
    @Test
    public void testNullPut() throws IOException {
        store.put(null);
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
     * Tests looking up an entry that does not exist.
     */
    @Test
    public void testGetUnknown() throws Exception {
        final Element element = store.get("key");
        assertNull(element);
    }

    /**
     * Tests adding an entry.
     */
    @Test
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
    @Test
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
    @Test
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
     * Multi-thread read-only test.
     */
    @Test
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
    @Test
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
    @Test
    public void testMemoryLeak() throws Exception {
        long differenceMemoryCache = thrashCache();
        LOG.info("Difference is : " + differenceMemoryCache);
        //Sometimes this can be higher but a three hour run confirms no memory leak. Consider increasing.
        assertTrue("Memory difference was expected to be less than 500000, but was " + differenceMemoryCache, differenceMemoryCache < 500000);
    }


    /**
     * This method tries to get the store too leak.
     */
    protected long thrashCache() throws Exception {


        long startingSize = measureMemoryUse();
        LOG.info("Starting memory used is: " + startingSize);

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
            final MemoryStoreTester.Executable executable = new MemoryStoreTester.Executable() {
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
        LOG.info("Ending memory used is: " + finishingSize);
        return finishingSize - startingSize;
    }


    /**
     * Multi-thread read-write test.
     */
    @Test
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
     * This test varies according to architecture. 64 bit architectures
     */
    @Test
    public void testMemoryStoreOutOfMemoryLimit() throws Exception {
        LOG.info("Starting out of memory limit test");
        //Set size so the second element overflows to disk.
        cache = manager.getCache("memoryLimitTest");
        if (cache == null) {
            cache = new Cache("memoryLimitTest", 1000000, false, false, 500, 500);
            manager.addCache(cache);
        }
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
            LOG.info("About to fail out of memory limit test");
            fail();
        } catch (OutOfMemoryError e) {
            cache.removeAll();
            Thread.sleep(1000);
            System.gc();
            Thread.sleep(2000);
            System.gc();

            try {
                LOG.info("Ran out of memory putting " + i + "th element");
                assertTrue(i > 65000);
            } catch (OutOfMemoryError e1) {
                //sometimes if we are really out of memory we cannot do anything
            }

        }
        Thread.sleep(1000);
        System.gc();
        Thread.sleep(1000);
    }

    @Test
    public void testShrinkingAndGrowingMemoryStore() {
        cache = new Cache("testShrinkingAndGrowingMemoryStore", 50, false, true, 120, 120);
        manager.addCache(cache);
        store = cache.getStore();

        if (!(store instanceof MemoryStore)) {
            LOG.info("Skipping Growing/Shrinking Memory Store Test - Store is not a subclass of MemoryStore!");
            return;
        }

        int i = 0;
        for (;;) {
            int size = store.getSize();
            store.put(new Element(Integer.valueOf(i++), new byte[100]));
            if (store.getSize() <= size) break;
        }

        final int initialSize = store.getSize();
        final int shrinkSize = initialSize / 2;
        ((MemoryStore) store).memoryCapacityChanged(initialSize, shrinkSize);

        for (;;) {
            int size = store.getSize();
            store.put(new Element(Integer.valueOf(i++), new byte[100]));
            if (store.getSize() >= size) break;
        }

        {
            int size = store.getSize();
            assertTrue(size < (shrinkSize * 1.1));
            assertTrue(size > (shrinkSize * 0.9));
        }

        final int growSize = initialSize * 2;
        ((MemoryStore) store).memoryCapacityChanged(shrinkSize, growSize);

        for (;;) {
            int size = store.getSize();
            store.put(new Element(Integer.valueOf(i++), new byte[100]));
            if (store.getSize() <= size) break;
        }

        {
            int size = store.getSize();
            assertTrue(size < (growSize * 1.1));
            assertTrue(size > (growSize * 0.9));
        }
    }
}
