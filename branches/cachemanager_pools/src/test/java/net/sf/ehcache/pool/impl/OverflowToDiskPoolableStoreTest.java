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
import org.junit.Ignore;
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
public class OverflowToDiskPoolableStoreTest {

    private volatile Cache cache;
    private volatile BoundedPool onHeapPool;
    private volatile BoundedPool onDiskPool;
    private volatile OverflowToDiskPoolableStore overflowToDiskPoolableStore;
    private volatile Element lastEvicted;

    private static Collection<Object> keysOfOnHeapElements(OverflowToDiskPoolableStore store) {
        List<Object> result = new ArrayList<Object>();

        List keys = store.getKeys();
        for (Object key : keys) {
            if (store.isElementOnHeap(key)) {
                result.add(key);
            }
        }

        return result;
    }

    private static Collection<Object> keysOfOnDiskElements(OverflowToDiskPoolableStore store) {
        List<Object> result = new ArrayList<Object>();

        List keys = store.getKeys();
        for (Object key : keys) {
            if (store.isElementOnDisk(key)) {
                result.add(key);
            }
        }

        return result;
    }

    private static int countElementsOnHeap(OverflowToDiskPoolableStore store) {
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

    private static int countElementsOnDisk(OverflowToDiskPoolableStore store) {
        List keys = store.getKeys();
        int countOnDisk = 0;
        for (Object key : keys) {
            boolean elementOnDisk = store.isElementOnDisk(key);
            if (elementOnDisk) {
                countOnDisk++;
            }
        }
        return countOnDisk;
    }

    private void dump() {
        System.out.println("# # # # # #");
        System.out.println(overflowToDiskPoolableStore.getSize() + " elements in cache");
        System.out.println("on heap: " + keysOfOnHeapElements(overflowToDiskPoolableStore) + ", on disk: " + keysOfOnDiskElements(overflowToDiskPoolableStore));
        System.out.println("on heap size: " + onHeapPool.getSize() + ", on disk size: " + onDiskPool.getSize());
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

        onDiskPool = new BoundedPool(
                16384 * 2, // == 2 elements
                new RoundRobinOnDiskPoolEvictor(),
                new ConstantSizeOfEngine(
                        1536,  /* 1.5 KB*/
                        14336, /* 14 KB */
                        512    /* 0.5 KB */
                )
        );

        overflowToDiskPoolableStore = OverflowToDiskPoolableStore.create(cache, "/tmp", onHeapPool, onDiskPool);
    }

    @After
    public void tearDown() {
        cache.dispose();
        overflowToDiskPoolableStore.dispose();
    }


    @Test
    public void testPutNew() throws Exception {
        // put 20 new elements in, making sure eviction is working
        for (int i = 0; i < 20; i++) {
            Element e = new Element(i, "" + i);
            overflowToDiskPoolableStore.put(e);
            assertTrue("#" + i, countElementsOnHeap(overflowToDiskPoolableStore) <= 2);
            assertTrue("#" + i, countElementsOnDisk(overflowToDiskPoolableStore) <= 2);
        }

        assertEquals(1, countElementsOnHeap(overflowToDiskPoolableStore));
        assertEquals(2, countElementsOnDisk(overflowToDiskPoolableStore));
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // get an on-disk element
        Object key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        overflowToDiskPoolableStore.get(key);

        assertEquals(1, countElementsOnHeap(overflowToDiskPoolableStore));
        assertEquals(1, countElementsOnDisk(overflowToDiskPoolableStore));
        assertEquals(16384 + 2048, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());


        // put a new element on-heap
        overflowToDiskPoolableStore.put(new Element(-1, "-1"));

        assertEquals(1, countElementsOnHeap(overflowToDiskPoolableStore));
        assertEquals(2, countElementsOnDisk(overflowToDiskPoolableStore));
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());
    }

    @Test
    public void testPutIfAbsentNew() throws Exception {
        // put 20 new elements in, making sure eviction is working
        for (int i = 0; i < 20; i++) {
            Element e = new Element(i, "" + i);
            overflowToDiskPoolableStore.putIfAbsent(e);
            assertTrue("#" + i, countElementsOnHeap(overflowToDiskPoolableStore) <= 2);
            assertTrue("#" + i, countElementsOnDisk(overflowToDiskPoolableStore) <= 2);
        }

        assertEquals(1, countElementsOnHeap(overflowToDiskPoolableStore));
        assertEquals(2, countElementsOnDisk(overflowToDiskPoolableStore));
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // get an on-disk element
        Object key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        overflowToDiskPoolableStore.get(key);

        assertEquals(1, countElementsOnHeap(overflowToDiskPoolableStore));
        assertEquals(1, countElementsOnDisk(overflowToDiskPoolableStore));
        assertEquals(16384 + 2048, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());


        // put a new element on-heap
        overflowToDiskPoolableStore.putIfAbsent(new Element(-1, "-1"));

        assertEquals(1, countElementsOnHeap(overflowToDiskPoolableStore));
        assertEquals(2, countElementsOnDisk(overflowToDiskPoolableStore));
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());
    }

    @Test
    public void testPutUpdate() throws Exception {
        // warm up
        overflowToDiskPoolableStore.put(new Element(1, "1"));
        overflowToDiskPoolableStore.put(new Element(2, "2"));
        overflowToDiskPoolableStore.put(new Element(3, "3"));

        assertEquals(3, overflowToDiskPoolableStore.getSize());
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // update element in memory
        Object key = keysOfOnHeapElements(overflowToDiskPoolableStore).iterator().next();
        overflowToDiskPoolableStore.put(new Element(key, key.toString()));

        // if both the Heap and Disk evictors decide to evict the updated key, the store will manage to keep all 3 elements
        if (lastEvicted.getObjectKey().equals(key)) {
            assertEquals(3, overflowToDiskPoolableStore.getSize());
            assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
            assertEquals(16384 * 2, onDiskPool.getSize());
        } else {
            assertEquals(2, overflowToDiskPoolableStore.getSize());
            assertEquals(16384 + 2048, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }

        // update element on disk
        key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        overflowToDiskPoolableStore.put(new Element(key, key.toString()));

        if (lastEvicted.getObjectKey().equals(key)) {
            assertEquals(3, overflowToDiskPoolableStore.getSize());
            assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
            assertEquals(16384 * 2, onDiskPool.getSize());
        } else {
            assertEquals(2, overflowToDiskPoolableStore.getSize());
            assertEquals(16384 + 2048, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }
    }

    @Test
    public void testPutIfAbsentUpdate() throws Exception {
        // warm up
        assertNull(overflowToDiskPoolableStore.putIfAbsent(new Element(1, "1#1")));
        assertNotNull(overflowToDiskPoolableStore.putIfAbsent(new Element(1, "1#2")));
        assertNull(overflowToDiskPoolableStore.putIfAbsent(new Element(2, "2#1")));
        assertNotNull(overflowToDiskPoolableStore.putIfAbsent(new Element(2, "2#2")));
        assertNull(overflowToDiskPoolableStore.putIfAbsent(new Element(3, "3#1")));
        assertNotNull(overflowToDiskPoolableStore.putIfAbsent(new Element(3, "3#2")));

        assertEquals(1, overflowToDiskPoolableStore.getSize());
        assertEquals(2048, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());

        Object key;

         // make sure one element is on-heap
        overflowToDiskPoolableStore.putIfAbsent(new Element(4, "4#1"));

        // try to update element in memory
        key = keysOfOnHeapElements(overflowToDiskPoolableStore).iterator().next();
        overflowToDiskPoolableStore.putIfAbsent(new Element(key, key.toString()));

        assertEquals(1, overflowToDiskPoolableStore.getSize());
        assertEquals(2048, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());

        // update element on disk
        key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        overflowToDiskPoolableStore.putIfAbsent(new Element(key, key.toString()));

        assertEquals(1, overflowToDiskPoolableStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
        assertEquals(0, onDiskPool.getSize());
    }

    @Test
    public void testRemove() throws Exception {
        // warm up
        overflowToDiskPoolableStore.put(new Element(1, "1"));
        overflowToDiskPoolableStore.put(new Element(2, "2"));
        overflowToDiskPoolableStore.put(new Element(3, "3"));

        assertEquals(3, overflowToDiskPoolableStore.getSize());
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // remove element on disk
        Object key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        overflowToDiskPoolableStore.remove(key);

        assertEquals(2, overflowToDiskPoolableStore.getSize());
        assertEquals(16384 + 2048, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());

        // remove element in memory
        key = keysOfOnHeapElements(overflowToDiskPoolableStore).iterator().next();
        overflowToDiskPoolableStore.remove(key);

        assertEquals(1, overflowToDiskPoolableStore.getSize());
        assertEquals(2048, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());
    }

    @Test
    public void testReplace1Arg() throws Exception {
        // warm up
        overflowToDiskPoolableStore.put(new Element(1, "1#1"));
        overflowToDiskPoolableStore.put(new Element(2, "2#1"));
        overflowToDiskPoolableStore.put(new Element(3, "3#1"));

        assertEquals(3, overflowToDiskPoolableStore.getSize());
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // replace element on disk
        Object key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        Element replaced = overflowToDiskPoolableStore.replace(new Element(key, key + "#2"));
        if (lastEvicted.equals(new Element(key, key + "#1"))) {
            assertNull(replaced);
            assertEquals(2, overflowToDiskPoolableStore.getSize());
            assertEquals(2 * 2048, onHeapPool.getSize());
            assertEquals(2 * 16384, onDiskPool.getSize());
        } else {
            assertEquals(new Element(key, key + "#1"), replaced);
            assertEquals(2, overflowToDiskPoolableStore.getSize());
            assertEquals(2048 + 16384, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }


        // make sure an element is in memory
        overflowToDiskPoolableStore.put(new Element(1, "1#1"));

        // replace element in memory
        key = keysOfOnHeapElements(overflowToDiskPoolableStore).iterator().next();
        replaced = overflowToDiskPoolableStore.replace(new Element(key, key + "#2"));
        if (lastEvicted.equals(new Element(key, key + "#1"))) {
            assertNull(replaced);
            assertEquals(2, overflowToDiskPoolableStore.getSize());
            assertEquals(2 * 2048, onHeapPool.getSize());
            assertEquals(2 * 16384, onDiskPool.getSize());
        } else {
            assertEquals(new Element(key, key + "#1"), replaced);
            assertEquals(2, overflowToDiskPoolableStore.getSize());
            assertEquals(16384 + 2048, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }


        // replace non-existent key
        assertNull(overflowToDiskPoolableStore.replace(new Element(-1, -1 + "#2")));

        assertEquals(2, overflowToDiskPoolableStore.getSize());
        assertEquals(2048 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());
    }

    @Test
    public void testReplace3Args() throws Exception {
        // warm up
        overflowToDiskPoolableStore.put(new Element(1, "1#1"));
        overflowToDiskPoolableStore.put(new Element(2, "2#1"));
        overflowToDiskPoolableStore.put(new Element(3, "3#1"));

        assertEquals(3, overflowToDiskPoolableStore.getSize());
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // replace element on disk
        Object key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        boolean replaced = overflowToDiskPoolableStore.replace(new Element(key, key + "#1"), new Element(key, key + "#2"), new DefaultElementValueComparator());

        if (lastEvicted.equals(new Element(key, key + "#1"))) {
            assertFalse(replaced);
            if (overflowToDiskPoolableStore.getSize() == 1) {
                assertEquals(2048, onHeapPool.getSize());
                assertEquals(16384, onDiskPool.getSize());
            } else if (overflowToDiskPoolableStore.getSize() == 2) {
                assertEquals(2 * 2048, onHeapPool.getSize());
                assertEquals(2 * 16384, onDiskPool.getSize());
            } else {
                fail("size can only be 1 or 2");
            }
        } else {
            assertTrue(replaced);
            assertEquals(1, overflowToDiskPoolableStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(0, onDiskPool.getSize());
        }

        // make sure an element is in memory
        overflowToDiskPoolableStore.put(new Element(1, "1#1"));

        // replace element in memory
        key = keysOfOnHeapElements(overflowToDiskPoolableStore).iterator().next();
        replaced = overflowToDiskPoolableStore.replace(new Element(key, key + "#1"), new Element(key, key + "#2"), new DefaultElementValueComparator());

        if (replaced) {
            assertEquals(1, overflowToDiskPoolableStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(0, onDiskPool.getSize());
        } else {
            assertEquals(1, overflowToDiskPoolableStore.getSize());
            assertEquals(2048, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }

        // re-warm up
        overflowToDiskPoolableStore.put(new Element(1, "1#1"));
        overflowToDiskPoolableStore.put(new Element(2, "2#1"));
        overflowToDiskPoolableStore.put(new Element(3, "3#1"));

        // replace non-existent key
        assertFalse(overflowToDiskPoolableStore.replace(new Element(-1, -1 + "#2"), new Element(-1, -1 + "#2"), new DefaultElementValueComparator()));

        assertEquals(2, overflowToDiskPoolableStore.getSize());
        assertEquals(2 * 2048, onHeapPool.getSize());
        assertEquals(2 * 16384, onDiskPool.getSize());
    }

    @Test
    public void testRemoveElement() throws Exception {
        // warm up
        overflowToDiskPoolableStore.put(new Element(1, "1"));
        overflowToDiskPoolableStore.put(new Element(2, "2"));
        overflowToDiskPoolableStore.put(new Element(3, "3"));

        assertEquals(3, overflowToDiskPoolableStore.getSize());
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // remove non-existent element
        assertNull(overflowToDiskPoolableStore.removeElement(new Element(-1, -1 + ""), new DefaultElementValueComparator()));

        assertEquals(3, overflowToDiskPoolableStore.getSize());
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // remove element on disk
        Object key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        assertEquals(new Element(key, key + ""), overflowToDiskPoolableStore.removeElement(new Element(key, key + ""), new DefaultElementValueComparator()));

        assertEquals(1, overflowToDiskPoolableStore.getSize());
        if (countElementsOnHeap(overflowToDiskPoolableStore) == 1) {
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(0, onDiskPool.getSize());
        } else {
            assertEquals(2048, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }

        // make sure one element is on-heap
        overflowToDiskPoolableStore.put(new Element(4, "4"));

        // remove element in memory
        key = keysOfOnHeapElements(overflowToDiskPoolableStore).iterator().next();
        assertEquals(new Element(key, key + ""), overflowToDiskPoolableStore.removeElement(new Element(key, key + ""), new DefaultElementValueComparator()));

        assertEquals(1, overflowToDiskPoolableStore.getSize());
        if (countElementsOnHeap(overflowToDiskPoolableStore) == 1) {
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(0, onDiskPool.getSize());
        } else {
            assertEquals(2048, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }
    }

    @Test
    public void testRemoveAll() throws Exception {
        // warm up
        overflowToDiskPoolableStore.put(new Element(1, "1"));
        overflowToDiskPoolableStore.put(new Element(2, "2"));
        overflowToDiskPoolableStore.put(new Element(3, "3"));

        assertEquals(3, overflowToDiskPoolableStore.getSize());
        assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        overflowToDiskPoolableStore.removeAll();

        assertEquals(0, overflowToDiskPoolableStore.getSize());
        assertEquals(0, onHeapPool.getSize());
        assertEquals(0, onDiskPool.getSize());
    }

    @Test
    @Ignore
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
                        overflowToDiskPoolableStore.put(e);

                        assertTrue(threadId + "#" + i + " - " + onHeapPool.getSize(), 16384 * 2 >= onHeapPool.getSize());
                        assertTrue(threadId + "#" + i + " - " + onDiskPool.getSize(), 16384 * 2 >= onDiskPool.getSize());

                        Thread.yield();
                        if ((i + 1) % 1000 == 0) { dump(); overflowToDiskPoolableStore.removeAll(); }
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
