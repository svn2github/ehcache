package net.sf.ehcache;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Ignore;

import org.junit.Test;

import java.util.logging.Logger;

/**
 *
 * @author cdennis
 */
@Ignore
public class ChrisCachePerformanceTest {

    private static final Logger LOG = Logger.getLogger(ChrisCachePerformanceTest.class.getName());
    static {
        //System.setProperty("net.sf.ehcache.use.classic.lru", "true");
    }
    
    @Test
    public void testSingleThreadedPut() {
        Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);
        
        Double time = testPut("Single-Threaded Put Test", testCache, 20);

        LOG.info("Single-Threaded Put Test, Average Time " + time + "ms/put");

        CacheManager.getInstance().removeCache(testCache.getName());
    }

    @Test
    public void testMultiThreadedPut() throws Exception {
        testMultiThreadedPut(Math.max(2, Runtime.getRuntime().availableProcessors()));
    }
    
    @Test
    public void testOverThreadedPut() throws Exception {
        testMultiThreadedPut(4 * Math.max(2, Runtime.getRuntime().availableProcessors()));
    }
    
    private void testMultiThreadedPut(final int cpuCount) throws Exception {
        final Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);

        Future<Double>[] futures = new Future[cpuCount];
        ExecutorService[] executors = new ExecutorService[cpuCount];
        
        for (int i = 0; i < cpuCount; i++) {
            final int cpu = i;
            executors[i] = Executors.newSingleThreadExecutor();
            futures[i] = executors[i].submit(new Callable<Double>() {
                public Double call() {
                    return testPut("Multi-Threaded Put Test [" + cpuCount + " Threads], Partition " + cpu, testCache, 20, cpu, cpuCount);
                }
            });
        }

        double total = 0;
        for (Future<Double> f : futures) {
            total += f.get().doubleValue();
        }
        
        LOG.info("Multi-Threaded Put Test, Average Time For " + cpuCount + " Threads " + (total/cpuCount) + "ms/put");

        for (ExecutorService e : executors) {
            e.shutdown();
        }
        
        CacheManager.getInstance().removeCache(testCache.getName());
    }

    @Test
    public void testSingleThreadedPutAtThreshold() {
        final Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);

        testPut("Warming Cache For Single-Threaded Threshold Put Test", testCache, 1);

        Double time = testThresholdPut("Single-Threaded Threshold Put Test", testCache, 20);
        
        LOG.info("Single-Threaded Threshold Put Test, Average Time " + time + "ms/put");

        CacheManager.getInstance().removeCache(testCache.getName());
    }

    @Test
    public void testMultiThreadedPutAtThreshold() throws Exception {
        testMultiThreadedPutAtThreshold(Math.max(2, Runtime.getRuntime().availableProcessors()));
    }
    
    @Test
    public void testOverThreadedPutAtThreshold() throws Exception {
        testMultiThreadedPutAtThreshold(4 * Math.max(2, Runtime.getRuntime().availableProcessors()));
    }
    
    private void testMultiThreadedPutAtThreshold(final int cpuCount) throws Exception {
        final Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);

        testPut("Warming Cache For Multi-Threaded Threshold Put Test", testCache, 1);

        Future<Double>[] futures = new Future[cpuCount];
        ExecutorService[] executors = new ExecutorService[cpuCount];
        
        for (int i = 0; i < cpuCount; i++) {
            final int cpu = i;
            executors[i] = Executors.newSingleThreadExecutor();
            futures[i] = executors[i].submit(new Callable<Double>() {
                public Double call() {
                    return testThresholdPut("Multi-Threaded Threshold Put Test, Partition " + cpu, testCache, 20, cpu, cpuCount);
                }
            });
        }

        double total = 0;
        for (Future<Double> f : futures) {
            total += f.get().doubleValue();
        }
        
        LOG.info("Multi-Threaded Threshold Put Test, Average Time For " + cpuCount + " Threads " + (total/cpuCount) + "ms/put");

        for (ExecutorService e : executors) {
            e.shutdown();
        }
        
        CacheManager.getInstance().removeCache(testCache.getName());
    }

    @Test
    public void testSingleThreadedGet() {
        final Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);

        testPut("Warming Cache For Single-Threaded Get Test", testCache, 1);

        Double time = testGet("Single-Threaded Get Test", testCache, 20);

        LOG.info("Single-Threaded Get Test, Average Time " + time + "ms/get");

        CacheManager.getInstance().removeCache(testCache.getName());
    }

    @Test
    public void testMultiThreadedGet() throws Exception {
        testMultiThreadedGet(Math.max(2, Runtime.getRuntime().availableProcessors()));
    }
    
    @Test
    public void testOverThreadedGet() throws Exception {
        testMultiThreadedGet(4 * Math.max(2, Runtime.getRuntime().availableProcessors()));
    }
    
    private void testMultiThreadedGet(final int cpuCount) throws Exception {
        final Cache testCache = new Cache(UUID.randomUUID().toString(), 100000, false, true, 0, 0);
        CacheManager.getInstance().addCache(testCache);

        testPut("Warming Cache For Multi-Threaded Get Test", testCache, 1);

        Future<Double>[] futures = new Future[cpuCount];
        ExecutorService[] executors = new ExecutorService[cpuCount];
        
        for (int i = 0; i < cpuCount; i++) {
            final int cpu = i;
            executors[i] = Executors.newSingleThreadExecutor();
            futures[i] = executors[i].submit(new Callable<Double>() {
                public Double call() {
                    return testGet("Multi-Threaded Get Test, Partition " + cpu, testCache, 20, cpu, cpuCount);
                }
            });
        }

        double total = 0;
        for (Future<Double> f : futures) {
            total += f.get().doubleValue();
        }
        
        LOG.info("Multi-Threaded Get Test, Average Time For " + cpuCount + " Threads " + (total/cpuCount) + "ms/get");

        for (ExecutorService e : executors) {
            e.shutdown();
        }
        
        CacheManager.getInstance().removeCache(testCache.getName());
        
    }


    private static Double testPut(String test, Cache cache, int cycles) {
        return testPut(test, cache, cycles, 0, 1);
    }
    
    private static Double testPut(String test, Cache cache, int cycles, int index, int partitions) {
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
            LOG.fine(test + ": Test Cycle " + (c + 1) + ": " + (finish - begin) + " puts took " + (end - start) + "ms [" + (((double) (end-start))/(finish - begin)) + "ms/put]\n"
                    + "\t\tRunning Average: " + totalPuts + " puts took " + totalTime + "ms [" + (((double) totalTime) / totalPuts) + "ms/put]");

        }
        
        return Double.valueOf(((double) totalTime) / totalPuts);
    }

    private static Double testThresholdPut(String test, Cache cache, int cycles) {
        return testThresholdPut(test, cache, cycles, 0, 1);
    }
    
    private static Double testThresholdPut(String test, Cache cache, int cycles, int index, int partitions) {
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
            LOG.fine(test + ": Test Cycle " + (c + 1) + ": " + (finish - begin) + " puts took " + (end - start) + "ms [" + (((double) (end-start))/(finish - begin)) + "ms/put]\n"
                    + "\t\tRunning Average: " + totalPuts + " puts took " + totalTime + "ms [" + (((double) totalTime) / totalPuts) + "ms/put]");

        }
        
        return Double.valueOf(((double) totalTime) / totalPuts);
    }

    private static Double testGet(String test, Cache cache, int cycles) {
        return testGet(test, cache, cycles, 0, 1);
    }

    private static Double testGet(String test, Cache cache, int cycles, int index, int partitions) {
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
            LOG.fine(test + ": Test Cycle " + (c + 1) + ": " + (finish - begin) + " gets took " + (end - start) + "ms [" + (((double) (end-start))/(finish - begin)) + "ms/get]\n"
                    + "\t\tRunning Average: " + totalGets + " gets took " + totalTime + "ms [" + (((double) totalTime) / totalGets) + "ms/get]");
        }
        
        return Double.valueOf(((double) totalTime) / totalGets);
    }
}
