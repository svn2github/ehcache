package net.sf.ehcache.pool.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.store.DefaultElementValueComparator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Ludovic Orban
 */
public class MemoryOnlyPoolableStoreTest {

    private volatile Cache cache;
    private volatile BoundedPool onHeapPool;
    private volatile MemoryOnlyPoolableStore memoryOnlyPoolableStore;
    private volatile Element lastEvicted;

    private static Collection<Object> keysOfOnHeapElements(MemoryOnlyPoolableStore store) {
        List<Object> result = new ArrayList<Object>();

        List keys = store.getKeys();
        for (Object key : keys) {
            if (store.isElementOnHeap(key)) {
                result.add(key);
            }
        }

        return result;
    }

    private static int countElementsOnHeap(MemoryOnlyPoolableStore store) {
        List keys = store.getKeys();
        int countOnHeap = 0;
        for (Object key : keys) {
            boolean elementOnHeap = store.isElementOnHeap(key);
            if (elementOnHeap) {
                countOnHeap++;
            }
        }
        return countOnHeap;
    }

    private void dump() {
        System.out.println("# # # # # #");
        System.out.println(memoryOnlyPoolableStore.getSize() + " elements in cache");
        System.out.println("on heap: " + keysOfOnHeapElements(memoryOnlyPoolableStore));
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

        onHeapPool = new BoundedPool(
                16384 * 2, // == 2 elements
                new RoundRobinOnHeapPoolEvictor(),
                new ConstantSizeOfEngine(
                        1536,  /* 1.5 KB*/
                        14336, /* 14 KB */
                        512    /* 0.5 KB */
                )
        );

        memoryOnlyPoolableStore = MemoryOnlyPoolableStore.create(cache, "/tmp", onHeapPool);
    }

    @After
    public void tearDown() {
        cache.dispose();
        memoryOnlyPoolableStore.dispose();
    }


    @Test
    public void testPutNew() throws Exception {
        // put 20 new elements in, making sure eviction is working
        for (int i = 0; i < 20; i++) {
            Element e = new Element(i, "" + i);
            memoryOnlyPoolableStore.put(e);
            assertTrue("#" + i, countElementsOnHeap(memoryOnlyPoolableStore) <= 2);
        }

        assertEquals(2, countElementsOnHeap(memoryOnlyPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());

        // get an on-heap element
        Object key = keysOfOnHeapElements(memoryOnlyPoolableStore).iterator().next();
        memoryOnlyPoolableStore.get(key);

        assertEquals(2, countElementsOnHeap(memoryOnlyPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());

        // put a new element on-heap
        memoryOnlyPoolableStore.put(new Element(-1, "-1"));

        assertEquals(2, countElementsOnHeap(memoryOnlyPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());
    }

    @Test
    public void testPutIfAbsentNew() throws Exception {
        // put 20 new elements in, making sure eviction is working
        for (int i = 0; i < 20; i++) {
            Element e = new Element(i, "" + i);
            assertNull(memoryOnlyPoolableStore.putIfAbsent(e));
            assertTrue("#" + i, countElementsOnHeap(memoryOnlyPoolableStore) <= 2);
        }

        assertEquals(2, countElementsOnHeap(memoryOnlyPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());

        // get an on-heap element
        Object key = keysOfOnHeapElements(memoryOnlyPoolableStore).iterator().next();
        memoryOnlyPoolableStore.get(key);

        assertEquals(2, countElementsOnHeap(memoryOnlyPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());


        // put a new element on-heap
        assertNull(memoryOnlyPoolableStore.putIfAbsent(new Element(-1, "-1")));

        assertEquals(2, countElementsOnHeap(memoryOnlyPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());
    }

    @Test
    public void testPutUpdate() throws Exception {
        // warm up
        memoryOnlyPoolableStore.put(new Element(1, "1"));
        memoryOnlyPoolableStore.put(new Element(2, "2"));
        memoryOnlyPoolableStore.put(new Element(3, "3"));

        assertEquals(2, memoryOnlyPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // update element in memory
        Object key = keysOfOnHeapElements(memoryOnlyPoolableStore).iterator().next();
        memoryOnlyPoolableStore.put(new Element(key, key.toString()));

        // size can be 1 or 2, depending if the evicted element is the updated one or not
        if (memoryOnlyPoolableStore.getSize() == 2) {
            assertEquals(16384 * 2, onHeapPool.getSize());
        } else if (memoryOnlyPoolableStore.getSize() == 1) {
            assertEquals(16384, onHeapPool.getSize());
        } else {
            fail();
        }
    }

    @Test
    public void testPutIfAbsentUpdate() throws Exception {
        // warm up
        Element element;
        assertNull(memoryOnlyPoolableStore.putIfAbsent(new Element(1, "1#1")));
        assertNotNull(memoryOnlyPoolableStore.putIfAbsent(new Element(1, "1#2")));
        assertNull(memoryOnlyPoolableStore.putIfAbsent(new Element(2, "2#1")));
        element = memoryOnlyPoolableStore.putIfAbsent(new Element(2, "2#2"));
        if (lastEvicted.getObjectKey().equals(2)) {
            assertNull(element);
        } else {
            assertNotNull(element);
        }
        assertNull(memoryOnlyPoolableStore.putIfAbsent(new Element(3, "3#1")));
        element = memoryOnlyPoolableStore.putIfAbsent(new Element(3, "3#2"));
        if (lastEvicted.getObjectKey().equals(3)) {
            assertNull(element);            
            assertEquals(2, memoryOnlyPoolableStore.getSize());
            assertEquals(16384 * 2, onHeapPool.getSize());
        } else {
            assertNotNull(element);
            assertEquals(1, memoryOnlyPoolableStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
        }

        Object key;

        // try to update element in memory
        key = keysOfOnHeapElements(memoryOnlyPoolableStore).iterator().next();
        element = memoryOnlyPoolableStore.putIfAbsent(new Element(key, key.toString()));

        if (lastEvicted.getObjectKey().equals(key)) {
            assertNull(element);
            assertEquals(2, memoryOnlyPoolableStore.getSize());
            assertEquals(16384 * 2, onHeapPool.getSize());
        } else {
            assertNotNull(element);
            assertEquals(1, memoryOnlyPoolableStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
        }
    }

    @Test
    public void testRemove() throws Exception {
        // warm up
        memoryOnlyPoolableStore.put(new Element(1, "1"));
        memoryOnlyPoolableStore.put(new Element(2, "2"));
        memoryOnlyPoolableStore.put(new Element(3, "3"));

        assertEquals(2, memoryOnlyPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // remove element on heap
        Object key = keysOfOnHeapElements(memoryOnlyPoolableStore).iterator().next();
        memoryOnlyPoolableStore.remove(key);

        assertEquals(1, memoryOnlyPoolableStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
    }

    @Test
    public void testReplace1Arg() throws Exception {
        // warm up
        memoryOnlyPoolableStore.put(new Element(1, "1#1"));
        memoryOnlyPoolableStore.put(new Element(2, "2#1"));
        memoryOnlyPoolableStore.put(new Element(3, "3#1"));

        assertEquals(2, memoryOnlyPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // replace element on heap
        Object key = keysOfOnHeapElements(memoryOnlyPoolableStore).iterator().next();
        Element element = memoryOnlyPoolableStore.replace(new Element(key, key + "#2"));

        if (lastEvicted.getObjectKey().equals(key)) {
            assertNull(element);
            assertEquals(2, memoryOnlyPoolableStore.getSize());
            assertEquals(16384 * 2, onHeapPool.getSize());
        } else {
            assertEquals(new Element(key, key + "#1"), element);
            assertEquals(1, memoryOnlyPoolableStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
        }

        // replace non-existent key
        assertNull(memoryOnlyPoolableStore.replace(new Element(-1, -1 + "#2")));

        assertEquals(1, memoryOnlyPoolableStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
    }

    @Test
    public void testReplace3Args() throws Exception {
        // warm up
        memoryOnlyPoolableStore.put(new Element(1, "1#1"));
        memoryOnlyPoolableStore.put(new Element(2, "2#1"));
        memoryOnlyPoolableStore.put(new Element(3, "3#1"));

        assertEquals(2, memoryOnlyPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // replace element on heap
        Object key = keysOfOnHeapElements(memoryOnlyPoolableStore).iterator().next();
        boolean replaced = memoryOnlyPoolableStore.replace(new Element(key, key + "#1"), new Element(key, key + "#2"), new DefaultElementValueComparator());

        if (lastEvicted.getObjectKey().equals(key)) {
            assertFalse(replaced);
        } else {
            assertTrue(replaced);
        }
        assertEquals(1, memoryOnlyPoolableStore.getSize());
        assertEquals(16384, onHeapPool.getSize());

        memoryOnlyPoolableStore.put(new Element(4, "4#1"));

        // replace non-existent key
        assertFalse(memoryOnlyPoolableStore.replace(new Element(-1, -1 + "#2"), new Element(-1, -1 + "#2"), new DefaultElementValueComparator()));

        assertEquals(1, memoryOnlyPoolableStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
    }

    @Test
    public void testRemoveElement() throws Exception {
        // warm up
        memoryOnlyPoolableStore.put(new Element(1, "1"));
        memoryOnlyPoolableStore.put(new Element(2, "2"));
        memoryOnlyPoolableStore.put(new Element(3, "3"));

        assertEquals(2, memoryOnlyPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // remove non-existent element
        assertNull(memoryOnlyPoolableStore.removeElement(new Element(-1, -1 + ""), new DefaultElementValueComparator()));

        assertEquals(2, memoryOnlyPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // remove element on heap
        Object key = keysOfOnHeapElements(memoryOnlyPoolableStore).iterator().next();
        assertEquals(new Element(key, key + ""), memoryOnlyPoolableStore.removeElement(new Element(key, key + ""), new DefaultElementValueComparator()));

        assertEquals(1, memoryOnlyPoolableStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
    }

    @Test
    public void testRemoveAll() throws Exception {
        // warm up
        memoryOnlyPoolableStore.put(new Element(1, "1"));
        memoryOnlyPoolableStore.put(new Element(2, "2"));
        memoryOnlyPoolableStore.put(new Element(3, "3"));

        assertEquals(2, memoryOnlyPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        memoryOnlyPoolableStore.removeAll();

        assertEquals(0, memoryOnlyPoolableStore.getSize());
        assertEquals(0, onHeapPool.getSize());
    }

    @Test
    public void testMultithreaded() throws Exception {
        final int nThreads = 16;

        final ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        final ConcurrentLinkedQueue<Future<?>> queue = new ConcurrentLinkedQueue<Future<?>>();

        for (int i = 0; i < nThreads; i++) {
            final int threadId = i;
            Future<?> f = executor.submit(new Runnable() {
                public void run() {
                    for (int i = 0; i < 10000; i++) {
                        Element e = new Element(i, "" + i);
                        memoryOnlyPoolableStore.put(e);

                        assertTrue(threadId + "#" + i + " - " + onHeapPool.getSize(), 16384 * 2 >= onHeapPool.getSize());

                        Thread.yield();
                        if ((i + 1) % 1000 == 0) { dump(); memoryOnlyPoolableStore.removeAll(); }
                    }
                }
            });
            queue.add(f);
        }

        while (!queue.isEmpty()) {
            Future<?> f = queue.poll();
            f.get();
        }
    }

}
