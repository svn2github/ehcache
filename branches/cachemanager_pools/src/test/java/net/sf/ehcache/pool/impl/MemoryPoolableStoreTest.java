package net.sf.ehcache.pool.impl;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertTrue;

/**
 * @author Ludovic Orban
 */
public class MemoryPoolableStoreTest {

    @Test
    public void test() throws Exception {
        Ehcache myCache1 = new Cache(new CacheConfiguration("myCache1", 0));
        Ehcache myCache2 = new Cache(new CacheConfiguration("myCache2", 0));

        BoundedPool cacheManagerPool = new BoundedPool(
                16384 * 10, // == 10 elements
                new RoundRobinOnHeapPoolEvictor(),
                new ConstantSizeOfEngine(
                        1536,  /* 1.5 KB*/
                        14336, /* 14 KB */
                        512    /* 0.5 KB */
                )
        );

        MemoryPoolableStore memoryPoolableStore1 = new MemoryPoolableStore(myCache1, cacheManagerPool);
        MemoryPoolableStore memoryPoolableStore2 = new MemoryPoolableStore(myCache2, cacheManagerPool);

        for (int i = 0; i < 5; i++) {
            memoryPoolableStore1.put(new Element(i, "" + i));
            System.out.println(cacheManagerPool.getSize());
        }

        for (int i = 0; i < 20; i++) {
            memoryPoolableStore2.put(new Element(i, "" + i));
            System.out.println(cacheManagerPool.getSize() + " bytes in cache2, " + memoryPoolableStore1.getSize() + " elements in cache 1, " + memoryPoolableStore2.getSize() + " elements in cache 2");

            assertTrue(memoryPoolableStore1.getSize() + memoryPoolableStore2.getSize() <= 10);
            assertTrue(cacheManagerPool.getSize() <= 16384 * 10);
        }

        System.out.println("# # # # # #");

        List keys = memoryPoolableStore1.getKeys();
        System.out.println(keys.size() + " elements in cache1");
        for (Object key : keys) {
            System.out.println(memoryPoolableStore1.get(key));
        }

        keys = memoryPoolableStore2.getKeys();
        System.out.println(keys.size() + " elements in cache2");
        for (Object key : keys) {
            System.out.println(memoryPoolableStore2.get(key));
        }

    }

    @Test
    public void testMultithreaded() throws Exception {
        Ehcache myCache1 = new Cache(new CacheConfiguration("myCache1", 0));
        final BoundedPool cacheManagerPool = new BoundedPool(
                16384 * 10, // == 10 elements
                new RoundRobinOnHeapPoolEvictor(),
                new ConstantSizeOfEngine(
                        1536,  /* 1.5 KB*/
                        14336, /* 14 KB */
                        512    /* 0.5 KB */
                )
        );

        final MemoryPoolableStore memoryPoolableStore1 = new MemoryPoolableStore(myCache1, cacheManagerPool);


        final int THREADS = 16;

        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
        List<Future<?>> futures = new ArrayList<Future<?>>();

        for (int i = 0; i < THREADS; i++) {
            final int threadId = i;
            Future<?> f = executorService.submit(new Runnable() {
                public void run() {
                    for (int i = 0; i < 50000; i++) {
                        String key = threadId + "#" + i;
                        memoryPoolableStore1.put(new Element(key, "" + i));

                        assertTrue(key + " | " + memoryPoolableStore1.getSize() + " | " + cacheManagerPool.getSize(), memoryPoolableStore1.getSize() <= 10);
                        assertTrue(key + " | " + memoryPoolableStore1.getSize() + " | " + cacheManagerPool.getSize(), cacheManagerPool.getSize() <= 16384 * 10);
                    }
                }
            });
            futures.add(f);
        }

        for (Future<?> future : futures) {
            future.get();
        }

    }

}
