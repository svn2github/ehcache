package net.sf.ehcache.store.disk;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.impl.BoundedPool;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;
import net.sf.ehcache.pool.impl.FromLargestCacheOnHeapPoolEvictor;
import net.sf.ehcache.pool.impl.StrictlyBoundedPool;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ludovic Orban
 */
public class PoolableDiskStoreTest {

    private final static boolean DEBUG = false;

    @Test
    public void testPutThenRemove() throws Exception {
        CacheManager cm = new CacheManager();
        Cache cache = new Cache(new CacheConfiguration("myCache", 0).maxElementsOnDisk(0));
        Pool onHeapPool = new StrictlyBoundedPool(96 * 10, new FromLargestCacheOnHeapPoolEvictor(), new DefaultSizeOfEngine());
        Pool onDiskPool = new StrictlyBoundedPool(1024 * 1024, new FromLargestCacheOnHeapPoolEvictor(), new DefaultSizeOfEngine());
        DiskStore store = DiskStore.create(cache, "/var/tmp",
                onHeapPool,
                onDiskPool
        );
        cm.addCache(cache);
        for (int i = 1000; i < 2000; i++) {
            store.put(new Element(i, i));
            if (DEBUG) System.out.println("H: " + onHeapPool.getSize() + " D: " + onDiskPool.getSize() + " S: " + store.getSize());
        }

        Thread.sleep(1000);
        if (DEBUG) System.out.println("final H: " + onHeapPool.getSize() + " D: " + onDiskPool.getSize() + " S: " + store.getSize());

        for (int i = 1000; i < 2000; i++) {
            store.remove(i);
            if (DEBUG) System.out.println("H: " + onHeapPool.getSize() + " D: " + onDiskPool.getSize() + " S: " + store.getSize());
        }

        Thread.sleep(1000);
        if (DEBUG) System.out.println("final H: " + onHeapPool.getSize() + " D: " + onDiskPool.getSize() + " S: " + store.getSize());

        if (DEBUG) System.out.println(store.getKeys());
        if (DEBUG) System.out.println(store.getSize());

        assertEquals(0, store.getSize());
        assertEquals(0, onHeapPool.getSize());
        assertEquals(0, onDiskPool.getSize());


        store.dispose();
        cm.shutdown();
    }

}
