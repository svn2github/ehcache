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

/**
 * @author Ludovic Orban
 */
public class DiskPersistentPoolableStoreTest {

    private volatile Cache cache;
    private volatile BoundedPool onHeapPool;
    private volatile BoundedPool onDiskPool;
    private volatile DiskPersistentPoolableStore diskPersistentPoolableStore;
    private volatile Element lastEvicted;

    private static Collection<Object> keysOfOnHeapElements(DiskPersistentPoolableStore store) {
        List<Object> result = new ArrayList<Object>();

        List keys = store.getKeys();
        for (Object key : keys) {
            if (store.isElementOnHeap(key)) {
                result.add(key);
            }
        }

        return result;
    }

    private static Collection<Object> keysOfOnDiskElements(DiskPersistentPoolableStore store) {
        List<Object> result = new ArrayList<Object>();

        List keys = store.getKeys();
        for (Object key : keys) {
            if (store.isElementOnDisk(key)) {
                result.add(key);
            }
        }

        return result;
    }

    private static int countElementsOnHeap(DiskPersistentPoolableStore store) {
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

    private static int countElementsOnDisk(DiskPersistentPoolableStore store) {
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
        System.out.println(diskPersistentPoolableStore.getSize() + " elements in cache");
        System.out.println("on heap: " + keysOfOnHeapElements(diskPersistentPoolableStore) + ", on disk: " + keysOfOnDiskElements(diskPersistentPoolableStore));
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
                16384 * 3, // == 3 elements
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

        diskPersistentPoolableStore = DiskPersistentPoolableStore.create(cache, "/tmp", onHeapPool, onDiskPool);
    }

    @After
    public void tearDown() {
        cache.dispose();
        diskPersistentPoolableStore.dispose();
    }


    @Test
    public void testPutNew() throws Exception {
        // put 20 new elements in, making sure eviction is working
        for (int i = 0; i < 20; i++) {
            Element e = new Element(i, "" + i);
            diskPersistentPoolableStore.put(e);
            assertTrue("#" + i, countElementsOnHeap(diskPersistentPoolableStore) <= 2);
            assertTrue("#" + i, countElementsOnDisk(diskPersistentPoolableStore) <= 2);
        }

        assertEquals(2, countElementsOnHeap(diskPersistentPoolableStore));
        assertEquals(2, countElementsOnDisk(diskPersistentPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // put a new element on-heap
        Thread.sleep(100);
        diskPersistentPoolableStore.put(new Element(-1, "-1"));

        assertNotNull(diskPersistentPoolableStore.get(-1));
        assertEquals(2, countElementsOnHeap(diskPersistentPoolableStore));
        assertEquals(2, countElementsOnDisk(diskPersistentPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());
    }

    @Test
    public void testPutIfAbsentNew() throws Exception {
        // put 20 new elements in, making sure eviction is working
        for (int i = 0; i < 20; i++) {
            Element e = new Element(i, "" + i);
            diskPersistentPoolableStore.putIfAbsent(e);
            assertTrue("#" + i, countElementsOnHeap(diskPersistentPoolableStore) <= 2);
            assertTrue("#" + i, countElementsOnDisk(diskPersistentPoolableStore) <= 2);
        }

        assertEquals(2, countElementsOnHeap(diskPersistentPoolableStore));
        assertEquals(2, countElementsOnDisk(diskPersistentPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // put a new element on-heap
        Thread.sleep(100);
        diskPersistentPoolableStore.putIfAbsent(new Element(-1, "-1"));

        assertNotNull(diskPersistentPoolableStore.get(-1));
        assertEquals(2, countElementsOnHeap(diskPersistentPoolableStore));
        assertEquals(2, countElementsOnDisk(diskPersistentPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());
    }

    @Test
    public void testPutUpdate() throws Exception {
        // warm up, sleeps are necessary as non-flushed elements are not evictable
        diskPersistentPoolableStore.put(new Element(1, "1"));
        diskPersistentPoolableStore.put(new Element(2, "2"));
        Thread.sleep(100);
        diskPersistentPoolableStore.put(new Element(3, "3"));

        assertNotNull(diskPersistentPoolableStore.get(3));
        assertEquals(2, countElementsOnHeap(diskPersistentPoolableStore));
        assertEquals(2, countElementsOnDisk(diskPersistentPoolableStore));
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // update element 1x
        Object key = keysOfOnHeapElements(diskPersistentPoolableStore).iterator().next();
        diskPersistentPoolableStore.put(new Element(key, key.toString()));
        Thread.sleep(100);

        if (lastEvicted.getObjectKey().equals(key)) {
            assertEquals(2, countElementsOnHeap(diskPersistentPoolableStore));
            assertEquals(2, countElementsOnDisk(diskPersistentPoolableStore));
            assertEquals(16384 * 2, onHeapPool.getSize());
            assertEquals(16384 * 2, onDiskPool.getSize());
        } else {
            assertEquals(1, countElementsOnHeap(diskPersistentPoolableStore));
            assertEquals(1, countElementsOnDisk(diskPersistentPoolableStore));
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }


        // update element 2x
        key = keysOfOnDiskElements(diskPersistentPoolableStore).iterator().next();
        diskPersistentPoolableStore.put(new Element(key, key.toString()));
        Thread.sleep(100);

        if (lastEvicted.getObjectKey().equals(key)) {
            assertEquals(2, countElementsOnHeap(diskPersistentPoolableStore));
            assertEquals(2, countElementsOnDisk(diskPersistentPoolableStore));
            assertEquals(16384 * 2, onHeapPool.getSize());
            assertEquals(16384 * 2, onDiskPool.getSize());
        } else {
            assertEquals(1, countElementsOnHeap(diskPersistentPoolableStore));
            assertEquals(1, countElementsOnDisk(diskPersistentPoolableStore));
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }
    }

    @Test
    public void testPutIfAbsentUpdate() throws Exception {
        // warm up
        assertNull(diskPersistentPoolableStore.putIfAbsent(new Element(1, "1#1")));
        Thread.sleep(100);
        assertNotNull(diskPersistentPoolableStore.putIfAbsent(new Element(1, "1#2")));
        Thread.sleep(100);
        assertNull(diskPersistentPoolableStore.putIfAbsent(new Element(2, "2#1")));
        Thread.sleep(100);
        assertNotNull(diskPersistentPoolableStore.putIfAbsent(new Element(2, "2#2")));
        Thread.sleep(100);

        assertNull(diskPersistentPoolableStore.putIfAbsent(new Element(3, "3#1")));
        Thread.sleep(100);
        Element oldElement = diskPersistentPoolableStore.putIfAbsent(new Element(3, "3#2"));
        if (lastEvicted.getObjectKey().equals(3)) {
            assertNull(oldElement);
            assertEquals(2, diskPersistentPoolableStore.getSize());
            assertEquals(16384 * 2, onHeapPool.getSize());
            assertEquals(16384 * 2, onDiskPool.getSize());
        } else {
            assertNotNull(oldElement);
            assertEquals(1, diskPersistentPoolableStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }
    }

    @Test
    public void testRemove() throws Exception {
        // warm up
        diskPersistentPoolableStore.put(new Element(1, "1"));
        diskPersistentPoolableStore.put(new Element(2, "2"));
        diskPersistentPoolableStore.put(new Element(3, "3"));

        assertEquals(2, diskPersistentPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // remove element on disk
        Object key = keysOfOnDiskElements(diskPersistentPoolableStore).iterator().next();
        diskPersistentPoolableStore.remove(key);

        assertEquals(1, diskPersistentPoolableStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());
    }

    @Test
    public void testReplace1Arg() throws Exception {
        // warm up
        diskPersistentPoolableStore.put(new Element(1, "1#1"));
        diskPersistentPoolableStore.put(new Element(2, "2#1"));
        diskPersistentPoolableStore.put(new Element(3, "3#1"));
        Thread.sleep(100);

        assertEquals(2, diskPersistentPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // replace element on disk
        Object key = keysOfOnDiskElements(diskPersistentPoolableStore).iterator().next();
        Element replaced = diskPersistentPoolableStore.replace(new Element(key, key + "#2"));
        Thread.sleep(100);
        if (lastEvicted.getObjectKey().equals(key)) {
            assertNull(replaced);
            assertEquals(1, diskPersistentPoolableStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        } else {
            assertEquals(new Element(key, key + "#1"), replaced);
            assertEquals(1, diskPersistentPoolableStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }


        // replace non-existent key
        assertNull(diskPersistentPoolableStore.replace(new Element(-1, -1 + "#2")));
        Thread.sleep(100);

        assertEquals(1, diskPersistentPoolableStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());
    }

    @Test
    public void testReplace3Args() throws Exception {
        // warm up
        diskPersistentPoolableStore.put(new Element(1, "1#1"));
        diskPersistentPoolableStore.put(new Element(2, "2#1"));
        diskPersistentPoolableStore.put(new Element(3, "3#1"));
        Thread.sleep(100);

        assertEquals(2, diskPersistentPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // replace element on disk
        Object key = keysOfOnDiskElements(diskPersistentPoolableStore).iterator().next();
        boolean replaced = diskPersistentPoolableStore.replace(new Element(key, key + "#1"), new Element(key, key + "#2"), new DefaultElementValueComparator());
        Thread.sleep(100);
        if (lastEvicted.getObjectKey().equals(key)) {
            assertFalse(replaced);
        } else {
            assertTrue(replaced);
        }

        assertEquals(1, diskPersistentPoolableStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());

        // replace non-existent key
        assertFalse(diskPersistentPoolableStore.replace(new Element(-1, -1 + "#2"), new Element(-1, -1 + "#2"), new DefaultElementValueComparator()));
        Thread.sleep(100);

        assertEquals(1, diskPersistentPoolableStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());
    }

    @Test
    public void testRemoveElement() throws Exception {
        // warm up
        diskPersistentPoolableStore.put(new Element(1, "1"));
        diskPersistentPoolableStore.put(new Element(2, "2"));
        diskPersistentPoolableStore.put(new Element(3, "3"));

        assertEquals(2, diskPersistentPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // remove non-existent element
        assertNull(diskPersistentPoolableStore.removeElement(new Element(-1, -1 + ""), new DefaultElementValueComparator()));

        assertEquals(2, diskPersistentPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        // remove element on disk
        Object key = keysOfOnDiskElements(diskPersistentPoolableStore).iterator().next();
        assertEquals(new Element(key, key + ""), diskPersistentPoolableStore.removeElement(new Element(key, key + ""), new DefaultElementValueComparator()));

        assertEquals(1, diskPersistentPoolableStore.getSize());
        if (countElementsOnHeap(diskPersistentPoolableStore) == 1) {
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        } else {
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }

        // make sure one element is on-heap
        diskPersistentPoolableStore.put(new Element(4, "4"));

        // remove element in memory
        key = keysOfOnHeapElements(diskPersistentPoolableStore).iterator().next();
        assertEquals(new Element(key, key + ""), diskPersistentPoolableStore.removeElement(new Element(key, key + ""), new DefaultElementValueComparator()));

        assertEquals(1, diskPersistentPoolableStore.getSize());
        if (countElementsOnHeap(diskPersistentPoolableStore) == 1) {
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        } else {
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        }
    }

    @Test
    public void testRemoveAll() throws Exception {
        // warm up
        diskPersistentPoolableStore.put(new Element(1, "1"));
        diskPersistentPoolableStore.put(new Element(2, "2"));
        diskPersistentPoolableStore.put(new Element(3, "3"));

        assertEquals(2, diskPersistentPoolableStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(16384 * 2, onDiskPool.getSize());

        diskPersistentPoolableStore.removeAll();

        assertEquals(0, diskPersistentPoolableStore.getSize());
        assertEquals(0, onHeapPool.getSize());
        assertEquals(0, onDiskPool.getSize());
    }

    @Test
    public void testMultithreaded() throws Exception {
        final int nThreads = 16;

        final ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        final ConcurrentLinkedQueue<Future<?>> queue = new ConcurrentLinkedQueue<Future<?>>();

        for (int i = 0; i < nThreads; i++) {
            Future<?> f = executor.submit(new Runnable() {
                public void run() {
                    for (int i = 0; i < 10000; i++) {
                        Element e = new Element(i, "" + i);
                        diskPersistentPoolableStore.put(e);

                        Thread.yield();
                        if ((i + 1) % 1000 == 0) { dump(); diskPersistentPoolableStore.removeAll(); }
                    }
                }
            });
            queue.add(f);
        }

        while (!queue.isEmpty()) {
            Future<?> f = queue.poll();
            f.get();
        }

        assertTrue(16384 * 2 >= onHeapPool.getSize());
        assertTrue(16384 * 2 >= onDiskPool.getSize());
        assertEquals(onHeapPool.getSize(), onDiskPool.getSize());
    }

}
