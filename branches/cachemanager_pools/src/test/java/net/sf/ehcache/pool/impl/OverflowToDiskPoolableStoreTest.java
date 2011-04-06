package net.sf.ehcache.pool.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Test;

import java.util.List;

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
                16384 * 20, // == 20 elements
                new RoundRobinOnDiskPoolEvictor(),
                new ConstantSizeOfEngine(
                        1536,  /* 1.5 KB*/
                        14336, /* 14 KB */
                        512    /* 0.5 KB */
                )
        );


        OverflowToDiskPoolableStore overflowToDiskPoolableStore = OverflowToDiskPoolableStore.create(myCache1, "/tmp", cacheManagerOnHeapPool, cacheManagerOnDiskPool);

        long previousOnHeapSize = 0;
        long previousOnDiskSize = 0;
        for (int i = 0; i < 15; i++) {
            overflowToDiskPoolableStore.put(new Element(i, "" + i));

            long currentOnHeapSize = cacheManagerOnHeapPool.getSize();
            long currentOnDiskSize = cacheManagerOnDiskPool.getSize();

            System.out.println(i);

            System.out.println("\t" + currentOnHeapSize + " | " + (currentOnHeapSize - previousOnHeapSize));
            System.out.println("\t" + currentOnDiskSize + " | " + (currentOnDiskSize - previousOnDiskSize));

            previousOnHeapSize = currentOnHeapSize;
            previousOnDiskSize = currentOnDiskSize;
        }

        System.out.println("# # # # # #");

        List keys = overflowToDiskPoolableStore.getKeys();
        System.out.println(keys.size() + " elements in cache1");
        int countOnHeap = 0;
        int countOnDisk = 0;
        for (Object key : keys) {
            boolean elementOnHeap = overflowToDiskPoolableStore.isElementOnHeap(key);
            if (elementOnHeap) {
                countOnHeap++;
            } else {
                countOnDisk++;
            }
            System.out.println(key + " on " + (elementOnHeap ? "heap" : "disk"));
        }
        System.out.println("there are " + countOnHeap + " on heap, " + countOnDisk + " on disk");
    }

}
