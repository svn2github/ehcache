package net.sf.ehcache;

import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import org.junit.Assert;

import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cdennis
 */
public class ChrisCachePerformanceTest {

    private static final Logger LOG = LoggerFactory.getLogger(ChrisCachePerformanceTest.class);

    @Test
    public void testSingleThreadedPut() {
        Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);
        
        testPut("Single-Threaded Put Test", testCache, 100);

        CacheManager.getInstance().removeCache(testCache.getName());
    }

    @Test
    public void testMultiThreadedPut() throws Exception {
        final Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);

        final int cpuCount = Runtime.getRuntime().availableProcessors();
        final CyclicBarrier completion = new CyclicBarrier(cpuCount + 1);

        Thread[] threads = new Thread[cpuCount];
        for (int i = 0; i < cpuCount; i++) {
            final int cpu = i;
            threads[i] = new Thread(){
                public void run() {
                    try {
                        testPut("Multi-Threaded Put Test, Partition " + cpu, testCache, 100, cpu, cpuCount);
                    } finally {
                        try {
                            completion.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (BrokenBarrierException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
        }
        for (Thread t : threads) {
            t.start();
        }

        completion.await();

        CacheManager.getInstance().removeCache(testCache.getName());
    }

    @Test
    public void testSingleThreadedPutAtThreshold() {
        final Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);

        testPut("Warming Cache For Single-Threaded Threshold Put Test", testCache, 1);

        testThresholdPut("Single-Threaded Threshold Put Test", testCache, 100);
        
        CacheManager.getInstance().removeCache(testCache.getName());
    }

    @Test
    public void testMultiThreadedPutAtThreshold() throws Exception {
        final Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);

        testPut("Warming Cache For Multi-Threaded Threshold Put Test", testCache, 1);

        final int cpuCount = Runtime.getRuntime().availableProcessors();
        final CyclicBarrier completion = new CyclicBarrier(cpuCount + 1);

        Thread[] threads = new Thread[cpuCount];
        for (int i = 0; i < cpuCount; i++) {
            final int cpu = i;
            threads[i] = new Thread(){
                public void run() {
                    try {
                        testThresholdPut("Multi-Threaded Threshold Put Test, Partition " + cpu, testCache, 100, cpu, cpuCount);
                    } finally {
                        try {
                            completion.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (BrokenBarrierException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
        }
        for (Thread t : threads) {
            t.start();
        }

        completion.await();

        CacheManager.getInstance().removeCache(testCache.getName());
    }

    @Test
    public void testSingleThreadedGet() {
        final Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);

        testPut("Warming Cache For Single-Threaded Get Test", testCache, 1);

        testGet("Single-Threaded Get Test", testCache, 100);

        CacheManager.getInstance().removeCache(testCache.getName());
    }

    @Test
    public void testMultiThreadedGet() throws Exception {
        final Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);

        testPut("Warming Cache For Multi-Threaded Get Test", testCache, 1);

        final int cpuCount = Runtime.getRuntime().availableProcessors();
        final CyclicBarrier completion = new CyclicBarrier(cpuCount + 1);

        Thread[] threads = new Thread[cpuCount];
        for (int i = 0; i < cpuCount; i++) {
            final int cpu = i;
            threads[i] = new Thread(){
                public void run() {
                    try {
                        testGet("Multi-Threaded Get Test, Partition " + cpu, testCache, 100, cpu, cpuCount);
                    } finally {
                        try {
                            completion.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (BrokenBarrierException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
        }
        for (Thread t : threads) {
            t.start();
        }

        completion.await();

        CacheManager.getInstance().removeCache(testCache.getName());
    }


    private static void testPut(String test, Cache cache, int cycles) {
        testPut(test, cache, cycles, 0, 1);
    }
    
    private static void testPut(String test, Cache cache, int cycles, int index, int partitions) {
        int max = cache.getCacheConfiguration().getMaxElementsInMemory();

        int begin = (max / partitions) * index;
        int finish  = (max / partitions) * (index + 1);

        long totalPuts = 0;
        long totalTime = 0;
        for (int c = 0; c < cycles; c++) {
            cache.removeAll();
            System.gc();
            
            long start = System.currentTimeMillis();
            for (int i = begin; i < finish; i++) {
                cache.put(new Element(Integer.valueOf(i), new Object()));
            }
            long end = System.currentTimeMillis();

            totalPuts += (finish - begin);
            totalTime += (end - start);
            LOG.info(test + ": Test Cycle " + (c + 1) + ": " + (finish - begin) + " puts took " + (end - start) + "ms [" + (((double) (end-start))/(finish - begin)) + "ms/put]\n"
                    + "\t\tRunning Average: " + totalPuts + " puts took " + totalTime + "ms [" + (((double) totalTime) / totalPuts) + "ms/put]");

        }
    }

    private static void testThresholdPut(String test, Cache cache, int cycles) {
        testThresholdPut(test, cache, cycles, 0, 1);
    }
    
    private static void testThresholdPut(String test, Cache cache, int cycles, int index, int partitions) {
        int max = cache.getCacheConfiguration().getMaxElementsInMemory();

        long totalPuts = 0;
        long totalTime = 0;
        for (int c = 0; c < cycles; c++) {
            System.gc();

            int begin = ((c + 1) * max) + (max / partitions) * index;
            int finish  = ((c + 1) * max) + (max / partitions) * (index + 1);

            long start = System.currentTimeMillis();
            for (int i = begin; i < finish; i++) {
                cache.put(new Element(Integer.valueOf(i), new Object()));
            }
            long end = System.currentTimeMillis();

            totalPuts += (finish - begin);
            totalTime += (end - start);
            LOG.info(test + ": Test Cycle " + (c + 1) + ": " + (finish - begin) + " puts took " + (end - start) + "ms [" + (((double) (end-start))/(finish - begin)) + "ms/put]\n"
                    + "\t\tRunning Average: " + totalPuts + " puts took " + totalTime + "ms [" + (((double) totalTime) / totalPuts) + "ms/put]");

        }
    }

    private static void testGet(String test, Cache cache, int cycles) {
        testGet(test, cache, cycles, 0, 1);
    }

    private static void testGet(String test, Cache cache, int cycles, int index, int partitions) {
        int max = cache.getCacheConfiguration().getMaxElementsInMemory();

        int begin = (max / partitions) * index;
        int finish  = (max / partitions) * (index + 1);

        long totalGets = 0;
        long totalTime = 0;
        for (int c = 0; c < cycles; c++) {
            long start = System.currentTimeMillis();
            for (int i = begin; i < finish; i++) {
                Assert.assertNotNull("Element @ " + i, cache.get(Integer.valueOf(i)));
            }
            long end = System.currentTimeMillis();

            totalGets += (finish - begin);
            totalTime += (end - start);
            LOG.info(test + ": Test Cycle " + (c + 1) + ": " + (finish - begin) + " gets took " + (end - start) + "ms [" + (((double) (end-start))/(finish - begin)) + "ms/get]\n"
                    + "\t\tRunning Average: " + totalGets + " gets took " + totalTime + "ms [" + (((double) totalTime) / totalGets) + "ms/get]");
        }
    }
}
