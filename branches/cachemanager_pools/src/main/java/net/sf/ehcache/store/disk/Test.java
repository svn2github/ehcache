package net.sf.ehcache.store.disk;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.impl.BoundedPool;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;
import net.sf.ehcache.pool.impl.FromLargestCacheOnHeapPoolEvictor;
import net.sf.ehcache.pool.impl.UnboundedPool;

/**
 * @author Ludovic Orban
 */
public class Test {

    public static void main(String[] args) throws Exception {
        CacheManager cm = new CacheManager();
        Cache cache = new Cache(new CacheConfiguration("myCache", 0).maxElementsOnDisk(0));
        BoundedPool onHeapPool = new BoundedPool(272, new FromLargestCacheOnHeapPoolEvictor(), new DefaultSizeOfEngine());
        BoundedPool onDiskPool = new BoundedPool(100 * 1024, new FromLargestCacheOnHeapPoolEvictor(), new DefaultSizeOfEngine());
        DiskStore store = DiskStore.create(cache, "/var/tmp",
                onHeapPool,
                onDiskPool
        );
        cm.addCache(cache);

        for (int i = 1000; i < 2000; i++) {
            store.put(new Element(i, i));
            System.out.println("H: " + onHeapPool.getSize() + " D: " + onDiskPool.getSize());
        }

/*
        Thread.sleep(1000);
        System.out.println("final H: " + onHeapPool.getSize() + " D: " + onDiskPool.getSize());
*/

        for (int i = 1000; i < 2000; i++) {
            store.remove(i);
            System.out.println("H: " + onHeapPool.getSize() + " D: " + onDiskPool.getSize());
        }

        Thread.sleep(1000);
        System.out.println("final H: " + onHeapPool.getSize() + " D: " + onDiskPool.getSize());

        System.out.println(store.getKeys());
        System.out.println(store.getSize());


        store.dispose();
        cm.shutdown();
    }

}
