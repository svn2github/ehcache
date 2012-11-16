package net.sf.ehcache.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.impl.ConstantSizeOfEngine;
import net.sf.ehcache.pool.impl.FromLargestCachePoolEvictor;
import net.sf.ehcache.pool.impl.StrictlyBoundedPool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ludovic Orban
 */
public class MemoryStorePoolingTest {

    private static final int ITERATIONS = 10000;
    private static final DefaultElementValueComparator COMPARATOR = new DefaultElementValueComparator(new CacheConfiguration()
        .copyOnRead(true).copyOnWrite(false));
    private volatile Cache cache;
    private volatile Pool onHeapPool;
    private volatile MemoryStore memoryStore;
    private volatile Element lastEvicted;

    private void dump() {
        System.out.println("# # # # # #");
        System.out.println(memoryStore.getSize() + " elements in cache");
        System.out.println("on heap: " + memoryStore.getKeys());
        System.out.println("on heap size: " + onHeapPool.getSize());
        System.out.println("# # # # # #");
    }

    @Before
    public void setUp() {
        cache = new Cache(new CacheConfiguration("myCache1", 0).eternal(true));

        lastEvicted = null;
        cache.getCacheEventNotificationService().registerListener(new CacheEventListener() {
            public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException { }

            public void notifyElementPut(Ehcache cache, Element element) throws CacheException { }

            public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException { }

            public void notifyElementExpired(Ehcache cache, Element element) { }

            public void notifyElementEvicted(Ehcache cache, Element element) {
                lastEvicted = element;
            }

            public void notifyRemoveAll(Ehcache cache) { }

            public void dispose() { }

            @Override
            public Object clone() throws CloneNotSupportedException {
                return super.clone();
            }
        });

        onHeapPool = new StrictlyBoundedPool(
                16384 * 2, // == 2 elements
                new FromLargestCachePoolEvictor(),
                new ConstantSizeOfEngine(
                        1536,  /* 1.5 KB*/
                        14336, /* 14 KB */
                        512    /* 0.5 KB */
                )
        );

        memoryStore = NotifyingMemoryStore.create(cache, onHeapPool);
    }

    @After
    public void tearDown() {
        cache.dispose();
        memoryStore.dispose();
    }


    @Test
    public void testElementPinning() throws Exception {
        Cache cache2 = new Cache(new CacheConfiguration("myCache2", 0).eternal(true));
        MemoryStore memoryOnlyStore2 = MemoryStore.create(cache2, onHeapPool);

        for (int i = 0; i < 100; i++) {
            memoryStore.put(new Element(i, i));
        }

        assertEquals(0, memoryOnlyStore2.getSize());
        assertEquals(0, memoryOnlyStore2.getInMemorySizeInBytes());
        assertEquals(2, memoryStore.getSize());
        assertEquals(16384 * 2, memoryStore.getInMemorySizeInBytes());

        for (int i = 0; i < 100; i++) {
            Element element = new Element(-i, i);
            element.setTimeToIdle(1);
            element.setTimeToLive(1);
            memoryOnlyStore2.setPinned(element.getObjectKey(), true);
            memoryOnlyStore2.put(element);
        }

        assertEquals(0, memoryStore.getSize());
        assertEquals(0, memoryStore.getInMemorySizeInBytes());
        assertEquals(100, memoryOnlyStore2.getSize());
        assertEquals(16384 * 100, memoryOnlyStore2.getInMemorySizeInBytes());

        // wait until the elements expired
        Thread.sleep(1200);

        for (int i = 0; i < 100; i++) {
            memoryStore.put(new Element(i, i));
        }

        assertEquals(1, memoryOnlyStore2.getSize());
        assertEquals(16384, memoryOnlyStore2.getInMemorySizeInBytes());
        assertEquals(1, memoryStore.getSize());
        assertEquals(16384, memoryStore.getInMemorySizeInBytes());
        assertEquals(16384 * 2, onHeapPool.getSize());
    }

    @Test
    public void testPutNew() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            putNew();

            tearDown();
            setUp();
        }
    }

    public void putNew() throws Exception {
        // put 20 new elements in, making sure eviction is working
        for (int i = 0; i < 20; i++) {
            Element e = new Element(i, "" + i);
            memoryStore.put(e);
            assertTrue("#" + i, memoryStore.getInMemorySize() <= 2);
        }

        assertEquals(2, memoryStore.getInMemorySize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // get an on-heap element
        Object key = memoryStore.getKeys().iterator().next();
        memoryStore.get(key);

        assertEquals(2, memoryStore.getInMemorySize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // put a new element on-heap
        memoryStore.put(new Element(-1, "-1"));

        assertEquals(2, memoryStore.getInMemorySize());
        assertEquals(16384 * 2, onHeapPool.getSize());
    }

    @Test
    public void testPutThenRemove() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            putThenRemove();

            tearDown();
            setUp();
        }
    }

    public void putThenRemove() throws Exception {
        for (int i = 0; i < 20; i++) {
            memoryStore.put(new Element(i, "" + i));
            assertTrue(memoryStore.getSize() <= 2);

            if (i % 2 == 1) {
                memoryStore.remove(i);
            }
            assertTrue(memoryStore.getSize() <= 2);
        }

        assertTrue(memoryStore.getSize() >= 1);
        assertTrue(memoryStore.getSize() <= 2);
        assertTrue(onHeapPool.getSize() >= 16384);
        assertTrue(onHeapPool.getSize() <= 16384 * 2);

        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                memoryStore.remove(i);
            }
        }

        assertEquals(0, memoryStore.getSize());
        assertEquals(0, onHeapPool.getSize());
    }

    @Test
    public void testPutIfAbsentNew() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            putIfAbsentNew();

            tearDown();
            setUp();
        }
    }

    public void putIfAbsentNew() throws Exception {
        // put 20 new elements in, making sure eviction is working
        for (int i = 0; i < 20; i++) {
            Element e = new Element(i, "" + i);
            assertNull(memoryStore.putIfAbsent(e));
            assertTrue("#" + i, memoryStore.getInMemorySize() <= 2);
        }

        assertEquals(2, memoryStore.getInMemorySize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // get an on-heap element
        Object key = memoryStore.getKeys().iterator().next();
        memoryStore.get(key);

        assertEquals(2, memoryStore.getInMemorySize());
        assertEquals(16384 * 2, onHeapPool.getSize());


        // put a new element on-heap
        assertNull(memoryStore.putIfAbsent(new Element(-1, "-1")));

        assertEquals(2, memoryStore.getInMemorySize());
        assertEquals(16384 * 2, onHeapPool.getSize());
    }

    @Test
    public void testPutUpdate() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            putUpdate();

            tearDown();
            setUp();
        }
    }

    public void putUpdate() throws Exception {
        // warm up
        memoryStore.put(new Element(1, "1"));
        memoryStore.put(new Element(2, "2"));
        memoryStore.put(new Element(3, "3"));

        assertEquals(2, memoryStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // update element in memory
        Object key = memoryStore.getKeys().iterator().next();
        memoryStore.put(new Element(key, key.toString()));

        // size can be 1 or 2, depending if the evicted element is the updated one or not
        if (memoryStore.getSize() == 2) {
            assertEquals(16384 * 2, onHeapPool.getSize());
        } else if (memoryStore.getSize() == 1) {
            assertEquals(16384, onHeapPool.getSize());
        } else {
            fail();
        }
    }

    @Test
    public void testPutIfAbsentUpdate() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            putIfAbsentUpdate();

            tearDown();
            setUp();
        }
    }

    public void putIfAbsentUpdate() throws Exception {
        // warm up
        Element element;
        assertNull(memoryStore.putIfAbsent(new Element(1, "1#1")));
        assertNotNull(memoryStore.putIfAbsent(new Element(1, "1#2")));
        assertNull(memoryStore.putIfAbsent(new Element(2, "2#1")));
        element = memoryStore.putIfAbsent(new Element(2, "2#2"));
        if (lastEvicted.getObjectKey().equals(2)) {
            assertNull(element);
        } else {
            assertNotNull(element);
        }
        assertNull(memoryStore.putIfAbsent(new Element(3, "3#1")));
        element = memoryStore.putIfAbsent(new Element(3, "3#2"));
        if (lastEvicted.getObjectKey().equals(3)) {
            assertNull(element);
            assertEquals(2, memoryStore.getSize());
            assertEquals(16384 * 2, onHeapPool.getSize());
        } else {
            assertNotNull(element);
            assertEquals(1, memoryStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
        }

        Object key;

        // try to update element in memory
        key = memoryStore.getKeys().iterator().next();
        element = memoryStore.putIfAbsent(new Element(key, key.toString()));

        if (lastEvicted.getObjectKey().equals(key)) {
            assertNull(element);
            assertEquals(2, memoryStore.getSize());
            assertEquals(16384 * 2, onHeapPool.getSize());
        } else {
            assertNotNull(element);
            assertEquals(1, memoryStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
        }
    }

    @Test
    public void testRemove() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            remove();

            tearDown();
            setUp();
        }
    }

    public void remove() throws Exception {
        // warm up
        memoryStore.put(new Element(1, "1"));
        memoryStore.put(new Element(2, "2"));
        memoryStore.put(new Element(3, "3"));

        assertEquals(2, memoryStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // remove element on heap
        Object key = memoryStore.getKeys().iterator().next();
        memoryStore.remove(key);

        assertEquals(1, memoryStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
    }

    @Test
    public void testReplace1Arg() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            replace1Arg();

            tearDown();
            setUp();
        }
    }

    public void replace1Arg() throws Exception {
        // warm up
        memoryStore.put(new Element(1, "1#1"));
        memoryStore.put(new Element(2, "2#1"));
        memoryStore.put(new Element(3, "3#1"));

        assertEquals(2, memoryStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // replace element on heap
        Object key = memoryStore.getKeys().iterator().next();
        Element element = memoryStore.replace(new Element(key, key + "#2"));

        if (lastEvicted.getObjectKey().equals(key)) {
            assertNull(element);
        } else {
            assertEquals(new Element(key, key + "#1"), element);
        }
        assertEquals(1, memoryStore.getSize());
        assertEquals(16384, onHeapPool.getSize());

        // replace non-existent key
        assertNull(memoryStore.replace(new Element(-1, -1 + "#2")));

        assertEquals(1, memoryStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
    }

    @Test
    public void testReplace3Args() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            replace3Arg();

            tearDown();
            setUp();
        }
    }

    public void replace3Arg() throws Exception {
        // warm up
        memoryStore.put(new Element(1, "1#1"));
        memoryStore.put(new Element(2, "2#1"));
        memoryStore.put(new Element(3, "3#1"));

        assertEquals(2, memoryStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // replace element on heap
        Object key = memoryStore.getKeys().iterator().next();
        boolean replaced = memoryStore.replace(new Element(key, key + "#1"), new Element(key, key + "#2"), COMPARATOR);

        if (lastEvicted.getObjectKey().equals(key)) {
            assertFalse(replaced);
        } else {
            assertTrue(replaced);
        }
        assertEquals(1, memoryStore.getSize());
        assertEquals(16384, onHeapPool.getSize());

        memoryStore.put(new Element(4, "4#1"));

        // replace non-existent key
        assertFalse(memoryStore.replace(new Element(-1, -1 + "#2"), new Element(-1, -1 + "#2"), COMPARATOR));

        assertEquals(1, memoryStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
    }

    @Test
    public void testRemoveElement() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            removeElement();

            tearDown();
            setUp();
        }
    }

    public void removeElement() throws Exception {
        // warm up
        memoryStore.put(new Element(1, "1"));
        memoryStore.put(new Element(2, "2"));
        memoryStore.put(new Element(3, "3"));

        assertEquals(2, memoryStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // remove non-existent element
        assertNull(memoryStore.removeElement(new Element(-1, -1 + ""), COMPARATOR));

        assertEquals(2, memoryStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // remove element on heap
        Object key = memoryStore.getKeys().iterator().next();
        assertEquals(new Element(key, key + ""), memoryStore.removeElement(new Element(key, key + ""), COMPARATOR));

        assertEquals(1, memoryStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
    }

    @Test
    public void testRemoveAll() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            removeAll();

            tearDown();
            setUp();
        }
    }

    public void removeAll() throws Exception {
        // warm up
        memoryStore.put(new Element(1, "1"));
        memoryStore.put(new Element(2, "2"));
        memoryStore.put(new Element(3, "3"));

        assertEquals(2, memoryStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        memoryStore.removeAll();

        assertEquals(0, memoryStore.getSize());
        assertEquals(0, onHeapPool.getSize());
    }

    @Test
    public void testMultithreaded() throws Exception {
        final int nThreads = 1;

        final ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        final ConcurrentLinkedQueue<Future<?>> queue = new ConcurrentLinkedQueue<Future<?>>();

        for (int i = 0; i < nThreads; i++) {
            Future<?> f = executor.submit(new Runnable() {
                public void run() {
                    for (int i = 0; i < 100000; i++) {
                        Element e = new Element(i, "" + i);
                        memoryStore.put(e);

                        memoryStore.replace(new Element(i, "2#" + i));

                        Thread.yield();
                    }
                }
            });
            queue.add(f);
        }

        while (!queue.isEmpty()) {
            Future<?> f = queue.poll();
            f.get();
        }

        assertEquals(16384, onHeapPool.getSize());
        assertEquals(1, memoryStore.getSize());
    }

}
