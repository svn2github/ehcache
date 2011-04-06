package net.sf.ehcache.pool.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Ludovic Orban
 */
public class OverflowToDiskPoolableStoreTest {

    @Test
    public void test() throws Exception {
        Cache myCache1 = new Cache(new CacheConfiguration("myCache1", 0));

        BoundedPool cacheManagerOnHeapPool = new BoundedPool(
                16384 * 10, // == 10 elements
                new RoundRobinOnHeapPoolEvictor(),
                new ConstantSizeOfEngine(
                        1536,  /* 1.5 KB*/
                        14336, /* 14 KB */
                        512    /* 0.5 KB */
                )
        );

        BoundedPool cacheManagerOnDiskPool = new BoundedPool(
                16384 * 2, // == 2 elements
                new RoundRobinOnDiskPoolEvictor(),
                new ConstantSizeOfEngine(
                        1536,  /* 1.5 KB*/
                        14336, /* 14 KB */
                        512    /* 0.5 KB */
                )
        );


        OverflowToDiskPoolableStore overflowToDiskPoolableStore = OverflowToDiskPoolableStore.create(myCache1, "/tmp", cacheManagerOnHeapPool, cacheManagerOnDiskPool);

        for (int i = 0; i < 15; i++) {
            Element e = new Element(i, "" + i);
            System.out.println("********** #"+i);
            overflowToDiskPoolableStore.put(e);
            assertTrue("#" + i, countElementsOnHeap(overflowToDiskPoolableStore) <= 10);
            assertTrue("#" + i, countElementsOnDisk(overflowToDiskPoolableStore) <= 2);
        }

        System.out.println("# # # # # #");
        System.out.println(overflowToDiskPoolableStore.getSize() + " elements in cache1");
        System.out.println("on heap: " + keysOfOnHeapElements(overflowToDiskPoolableStore) + ", on disk: " + keysOfOnDiskElements(overflowToDiskPoolableStore));
        System.out.println("on heap size: " + cacheManagerOnHeapPool.getSize() + ", on disk size: " + cacheManagerOnDiskPool.getSize());

        Object key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        System.out.println("get: " + key);
        overflowToDiskPoolableStore.get(key);

        System.out.println("# # # # # #");
        System.out.println(overflowToDiskPoolableStore.getSize() + " elements in cache1");
        System.out.println("on heap: " + keysOfOnHeapElements(overflowToDiskPoolableStore) + ", on disk: " + keysOfOnDiskElements(overflowToDiskPoolableStore));
        System.out.println("on heap size: " + cacheManagerOnHeapPool.getSize() + ", on disk size: " + cacheManagerOnDiskPool.getSize());

/*
        key = keysOfOnDiskElements(overflowToDiskPoolableStore).iterator().next();
        System.out.println("remove: " + key);
        overflowToDiskPoolableStore.remove(key);

        System.out.println("# # # # # #");
        System.out.println(overflowToDiskPoolableStore.getSize() + " elements in cache1");
        System.out.println("on heap: " + keysOfOnHeapElements(overflowToDiskPoolableStore) + ", on disk: " + keysOfOnDiskElements(overflowToDiskPoolableStore));
        System.out.println("on heap size: " + cacheManagerOnHeapPool.getSize() + ", on disk size: " + cacheManagerOnDiskPool.getSize());
*/
    }

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

}
