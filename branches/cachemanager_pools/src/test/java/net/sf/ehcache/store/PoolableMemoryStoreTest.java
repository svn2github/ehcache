package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.pool.impl.BoundedPool;
import net.sf.ehcache.pool.impl.ConstantSizeOfEngine;
import net.sf.ehcache.pool.impl.RoundRobinOnHeapPoolEvictor;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Ludovic Orban
 */
public class PoolableMemoryStoreTest {

    private volatile Cache cache;
    private volatile BoundedPool onHeapPool;
    private volatile MemoryStore memoryStore;
    private volatile Element lastEvicted;

    private static Collection<Object> keysOfOnHeapElements(MemoryStore store) {
        return (Collection<Object>) store.getKeys();
    }

    private static int countElementsOnHeap(MemoryStore store) {
        return keysOfOnHeapElements(store).size();
    }

    private void dump() {
        System.out.println("# # # # # #");
        System.out.println(memoryStore.getSize() + " elements in cache");
        System.out.println("on heap: " + keysOfOnHeapElements(memoryStore));
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

        memoryStore = MemoryStore.create(cache, onHeapPool);
    }

    @After
    public void tearDown() {
        cache.dispose();
        memoryStore.dispose();
    }


    @Test
    public void testPutNew() throws Exception {
        // put 20 new elements in, making sure eviction is working
        for (int i = 0; i < 20; i++) {
            Element e = new Element(i, "" + i);
            memoryStore.put(e);
            assertTrue("#" + i, countElementsOnHeap(memoryStore) <= 2);
        }

        assertEquals(2, countElementsOnHeap(memoryStore));
        assertEquals(16384 * 2, onHeapPool.getSize());

        // get an on-heap element
        Object key = keysOfOnHeapElements(memoryStore).iterator().next();
        memoryStore.get(key);

        assertEquals(2, countElementsOnHeap(memoryStore));
        assertEquals(16384 * 2, onHeapPool.getSize());

        // put a new element on-heap
        memoryStore.put(new Element(-1, "-1"));

        assertEquals(2, countElementsOnHeap(memoryStore));
        assertEquals(16384 * 2, onHeapPool.getSize());
    }

    @Test
    public void testPutUpdate() throws Exception {
        // warm up
        memoryStore.put(new Element(1, "1"));
        memoryStore.put(new Element(2, "2"));
        memoryStore.put(new Element(3, "3"));

        assertEquals(2, memoryStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // update element in memory
        Object key = keysOfOnHeapElements(memoryStore).iterator().next();
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
    public void testRemove() throws Exception {
        // warm up
        memoryStore.put(new Element(1, "1"));
        memoryStore.put(new Element(2, "2"));
        memoryStore.put(new Element(3, "3"));

        assertEquals(2, memoryStore.getSize());
        assertEquals(16384 * 2, onHeapPool.getSize());

        // remove element on heap
        Object key = keysOfOnHeapElements(memoryStore).iterator().next();
        memoryStore.remove(key);

        assertEquals(1, memoryStore.getSize());
        assertEquals(16384, onHeapPool.getSize());
    }

    @Test
    public void testRemoveAll() throws Exception {
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
                        memoryStore.put(e);

                        assertTrue(threadId + "#" + i + " - " + onHeapPool.getSize(), 16384 * 2 >= onHeapPool.getSize());

                        Thread.yield();
                        if ((i + 1) % 1000 == 0) { dump(); memoryStore.removeAll(); }
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
