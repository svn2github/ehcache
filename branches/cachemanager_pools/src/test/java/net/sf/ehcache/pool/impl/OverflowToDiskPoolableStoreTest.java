package net.sf.ehcache.pool.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ludovic Orban
 */
public class OverflowToDiskPoolableStoreTest {

    private Cache cache;
    private BoundedPool onHeapPool;
    private BoundedPool onDiskPool;
    private OverflowToDiskPoolableStore overflowToDiskPoolableStore;

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


    @Before
    public void setUp() {
        cache = new Cache(new CacheConfiguration("myCache1", 0));

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
        assertEquals(16384 + 1 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 1, onDiskPool.getSize());


        // put a new element on-heap
        overflowToDiskPoolableStore.put(new Element(-1, "-1"));

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
        if (overflowToDiskPoolableStore.getSize() == 3) {
            assertEquals(16384 + 2 * 2048, onHeapPool.getSize());
            assertEquals(16384 * 2, onDiskPool.getSize());
        } else if (overflowToDiskPoolableStore.getSize() == 2) {
            assertEquals(16384 + 2048, onHeapPool.getSize());
            assertEquals(16384, onDiskPool.getSize());
        } else {
            fail("overflowToDiskPoolableStore.getSize() must be 2 or 3");
        }

        // update element on disk
        key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        System.out.println( + overflowToDiskPoolableStore.getSize() + " ********** putting " + key);
        overflowToDiskPoolableStore.put(new Element(key, key.toString()));

        assertEquals(2, overflowToDiskPoolableStore.getSize());
        assertEquals(16384 + 2048, onHeapPool.getSize());
        assertEquals(16384, onDiskPool.getSize());
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
        assertEquals(16384 + 1 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 1, onDiskPool.getSize());

        // remove element in memory
        key = keysOfOnHeapElements(overflowToDiskPoolableStore).iterator().next();
        overflowToDiskPoolableStore.remove(key);

        assertEquals(1, overflowToDiskPoolableStore.getSize());
        assertEquals(1 * 2048, onHeapPool.getSize());
        assertEquals(16384 * 1, onDiskPool.getSize());
    }

    private void dump() {
        System.out.println("# # # # # #");
        System.out.println(overflowToDiskPoolableStore.getSize() + " elements in cache1");
        System.out.println("on heap: " + keysOfOnHeapElements(overflowToDiskPoolableStore) + ", on disk: " + keysOfOnDiskElements(overflowToDiskPoolableStore));
        System.out.println("on heap size: " + onHeapPool.getSize() + ", on disk size: " + onDiskPool.getSize());
        System.out.println("# # # # # #");
    }

}
