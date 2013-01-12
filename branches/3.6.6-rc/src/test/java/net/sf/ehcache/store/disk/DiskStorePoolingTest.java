package net.sf.ehcache.store.disk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import net.sf.ehcache.pool.impl.FromLargestCacheOnDiskPoolEvictor;
import net.sf.ehcache.pool.impl.FromLargestCacheOnHeapPoolEvictor;
import net.sf.ehcache.pool.impl.StrictlyBoundedPool;
import net.sf.ehcache.store.DefaultElementValueComparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Ludovic Orban
 */
public class DiskStorePoolingTest {

    private static final int ELEMENT_SIZE_ON_DISK = 298;
    private static final int ITERATIONS = 100;

    private final static DefaultElementValueComparator COMPARATOR = new DefaultElementValueComparator(new CacheConfiguration()
        .copyOnRead(true).copyOnWrite(false));

    private volatile Cache cache;
    private volatile Pool onHeapPool;
    private volatile Pool onDiskPool;
    private volatile DiskStore diskStore;
    private volatile Element lastEvicted;

    private void dump() {
        System.out.println("# # # # # #");
        System.out.println(diskStore.getSize() + " elements in cache");
        System.out.println("on heap size: " + onHeapPool.getSize() + ", on disk size: " + onDiskPool.getSize());
        System.out.println("# # # # # #");
    }

    @Before
    public void setUp() {
        cache = new Cache(new CacheConfiguration("myCache1", 0).eternal(true).diskPersistent(true));

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
                16384 * 3, // == 3 elements
                new FromLargestCacheOnHeapPoolEvictor(),
                new ConstantSizeOfEngine(
                        1536,  /* 1.5 KB*/
                        14336, /* 14 KB */
                        512    /* 0.5 KB */
                )
        );

        onDiskPool = new StrictlyBoundedPool(
                ELEMENT_SIZE_ON_DISK * 2, // == 2 elements
                new FromLargestCacheOnDiskPoolEvictor(),
                null
        );

        diskStore = DiskStore.create(cache, System.getProperty("java.io.tmpdir"), onHeapPool, onDiskPool);
        diskStore.removeAll();
    }

    @After
    public void tearDown() {
        cache.dispose();
        diskStore.dispose();
    }

    @Test
    public void testPersistence() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            persistence();

            tearDown();
            setUp();
        }
    }

    public void persistence() throws Exception {
        // fill the store
        diskStore.put(new Element(1000, "1000"));
        diskStore.put(new Element(1001, "1001"));

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(2 * 16384, onHeapPool.getSize());
        assertEquals(2 * ELEMENT_SIZE_ON_DISK, onDiskPool.getSize());
        assertEquals(2 * 16384, diskStore.getInMemorySizeInBytes());
        assertEquals(2 * ELEMENT_SIZE_ON_DISK, diskStore.getOnDiskSizeInBytes());
        diskStore.dispose();

        for (int i = 1000; i < 1030; i++) {
            diskStore = DiskStore.create(cache, System.getProperty("java.io.tmpdir"), onHeapPool, onDiskPool);
            assertEquals(2, diskStore.getSize());
            assertEquals(2 * 16384, onHeapPool.getSize());
            assertEquals(2 * ELEMENT_SIZE_ON_DISK, onDiskPool.getSize());
            assertEquals(2 * 16384, diskStore.getInMemorySizeInBytes());
            assertEquals(2 * ELEMENT_SIZE_ON_DISK, diskStore.getOnDiskSizeInBytes());
            assertEquals(new Element(1000, "1000"), diskStore.get(1000));
            assertEquals(new Element(1001, "1001"), diskStore.get(1001));
            diskStore.dispose();
        }
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
        for (int i = 1000; i < 1020; i++) {
            Element e = new Element(i, "" + i);
            diskStore.put(e);
        }

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());

        // put a new element on-heap

        diskStore.put(new Element(1999, "1999"));
        assertNotNull(diskStore.get(1999));

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());
    }

    @Test
    public void testPutManyAtSameKey() throws Exception {
        // put 20 new elements in, making sure eviction is working
        for (int i = 0; i < 100; i++) {
            Element e = new Element(1, "" + i);
            diskStore.put(e);
        }

        for (int i = 0; i < 1000; i++) {
            Element element = diskStore.get(1);
            assertNotNull(element);
        }

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
        for (int i = 1000; i < 2000; i++) {
            diskStore.put(new Element(i, "" + i));
            assertTrue(diskStore.getSize() <= 10);

            if (i % 2 == 1) {
                diskStore.remove(i);
            }
            assertTrue(diskStore.getSize() <= 10);
        }

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertTrue(diskStore.getSize() >= 1);
        assertTrue(diskStore.getSize() <= 2);
        assertTrue(onHeapPool.getSize() >= 16384);
        assertTrue(onHeapPool.getSize() <= 16384 * 2);
        assertTrue(onDiskPool.getSize() >= ELEMENT_SIZE_ON_DISK);
        assertTrue(onDiskPool.getSize() <= ELEMENT_SIZE_ON_DISK * 2);

        for (int i = 1000; i < 2000; i++) {
            if (i % 2 == 0) {
                diskStore.remove(i);
            }
        }

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(0, diskStore.getSize());
        assertEquals(0, onHeapPool.getSize());
        assertEquals(0, onDiskPool.getSize());
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
        for (int i = 1000; i < 1020; i++) {
            Element e = new Element(i, "" + i);
            diskStore.putIfAbsent(e);
        }

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());

        // put a new element on-heap
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();
        diskStore.putIfAbsent(new Element(1999, "1999"));
        assertNotNull(diskStore.get(1999));

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());
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
        diskStore.put(new Element(1001, "1001"));
        diskStore.put(new Element(1002, "1002"));
        diskStore.put(new Element(1003, "1003"));

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());

        // update element 1x
        Object key = diskStore.getKeys().iterator().next();
        diskStore.put(new Element(key, key.toString()));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());


        // update element 2x
        key = diskStore.getKeys().iterator().next();
        diskStore.put(new Element(key, key.toString()));

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());
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
        assertNull(diskStore.putIfAbsent(new Element(1001, "11#1")));
        assertNotNull(diskStore.putIfAbsent(new Element(1001, "11#2")));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();
        assertNull(diskStore.putIfAbsent(new Element(1002, "12#1")));
        assertNotNull(diskStore.putIfAbsent(new Element(1002, "12#2")));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertNull(diskStore.putIfAbsent(new Element(1003, "13#1")));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();
        Element oldElement = diskStore.putIfAbsent(new Element(1003, "13#2"));

        assertNotNull(oldElement);
        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());
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
        diskStore.put(new Element(1001, "1001"));
        diskStore.put(new Element(1002, "1002"));
        diskStore.put(new Element(1003, "1003"));

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());

        // remove element on disk
        Object key = diskStore.getKeys().iterator().next();
        diskStore.remove(key);

        assertEquals(1, diskStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK, onDiskPool.getSize());
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
        diskStore.put(new Element(1001, "11#1"));
        diskStore.put(new Element(1002, "12#1"));
        diskStore.put(new Element(1003, "13#1"));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());

        // replace element on disk
        Object key = diskStore.getKeys().iterator().next();
        Element replaced = diskStore.replace(new Element(key, "22#2"));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(new Element(key, "22#2"), replaced);
        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());

        // replace non-existent key
        assertNull(diskStore.replace(new Element(1999, 1999 + "19#2")));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());
    }

    @Test
    public void testReplace3Args() throws Exception {
        for (int i = 0; i < ITERATIONS; i++) {
            replace3Args();

            tearDown();
            setUp();
        }
    }

    public void replace3Args() throws Exception {
        // warm up
        diskStore.put(new Element(1001, "11#1"));
        diskStore.put(new Element(1002, "12#1"));
        diskStore.put(new Element(1003, "13#1"));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());

        // replace element on disk
        Object key = diskStore.getKeys().iterator().next();
        assertTrue(diskStore.replace(diskStore.getQuiet(key), new Element(key, "20#2"), COMPARATOR));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        if (lastEvicted.getObjectKey().equals(key)) {
            // the replaced object itself got evicted -> pool reserved space for it then freed it
            assertEquals(1, diskStore.getSize());
            assertEquals(16384, onHeapPool.getSize());
            assertEquals(ELEMENT_SIZE_ON_DISK, onDiskPool.getSize());
        } else {
            assertEquals(2, diskStore.getSize());
            assertEquals(16384 * 2, onHeapPool.getSize());
            assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());
        }

        // replace non-existent key
        assertFalse(diskStore.replace(new Element(1999, 1999 + "19#1"), new Element(1999, "19#2"), COMPARATOR));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());
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
        diskStore.put(new Element(1001, "1001"));
        diskStore.put(new Element(1002, "1002"));
        diskStore.put(new Element(1003, "1003"));
        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());

        // remove non-existent element
        assertNull(diskStore.removeElement(new Element(1999, 1999 + ""), COMPARATOR));

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());

        // remove element on disk
        Object key = diskStore.getKeys().iterator().next();
        assertEquals(new Element(key, key + ""), diskStore.removeElement(new Element(key, key + ""), COMPARATOR));

        assertEquals(1, diskStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK, onDiskPool.getSize());

        // make sure one element is on-heap
        diskStore.put(new Element(1004, "1004"));

        key = diskStore.getKeys().iterator().next();
        assertEquals(new Element(key, key + ""), diskStore.removeElement(new Element(key, key + ""), COMPARATOR));

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(1, diskStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK, onDiskPool.getSize());
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
        diskStore.put(new Element(1001, "1001"));
        diskStore.put(new Element(1002, "1002"));
        diskStore.put(new Element(1003, "1003"));

        DiskStoreHelper.flushAllEntriesToDisk(diskStore).get();

        assertEquals(2, diskStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());
        assertEquals(ELEMENT_SIZE_ON_DISK * 2, onDiskPool.getSize());

        diskStore.removeAll();

        assertEquals(0, diskStore.getSize());
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
                        diskStore.put(e);

                        Thread.yield();
                        if ((i + 1) % 1000 == 0) { diskStore.removeAll(); }
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
