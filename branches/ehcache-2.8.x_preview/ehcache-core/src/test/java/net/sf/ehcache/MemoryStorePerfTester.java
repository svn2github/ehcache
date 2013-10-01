package net.sf.ehcache;

import net.sf.ehcache.store.Store;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import net.sf.ehcache.config.Configuration;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Snaps
 */
public abstract class MemoryStorePerfTester {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryStorePerfTester.class.getName());

    protected abstract Cache createCache() throws CacheException;

    /**
     * Tests bulk load.
     */
    @Test
    public void testBulkLoad() throws Exception {
        CacheManager manager = new CacheManager(new Configuration().name("testBulkLoad"));
        try {
            Cache cache = createCache();
            manager.addCache(cache);
            Store store = cache.getStore();
            final Random random = new Random();
            StopWatch stopWatch = new StopWatch();

            // Add a bunch of entries
            for (int i = 0; i < 500; i++) {
                // Use a random length value
                final String key = "key" + i;
                final String value = "value" + random.nextInt(1000);

                // Add an element, and make sure it is present
                Element element = new Element(key, value);
                store.put(element);
                element = store.get(key);
                assertNotNull(element);

                // Remove the element
                store.remove(key);
                element = store.get(key);
                assertNull(element);

                element = new Element(key, value);
                store.put(element);
                element = store.get(key);
                assertNotNull(element);
            }
            long time = stopWatch.getElapsedTime();
            LOG.info("Time for Bulk Load: " + time);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Benchmark to test speed.
     */
    @Test
    public void testBenchmarkPutGetRemove() throws Exception {
        CacheManager manager = new CacheManager(new Configuration().name("testBenchmarkPutGetRemove"));
        try {
            Cache cache = createCache();
            manager.addCache(cache);
            Store store = cache.getStore();
            final String key = "key";
            byte[] value = new byte[500];
            StopWatch stopWatch = new StopWatch();

            // Add a bunch of entries
            for (int i = 0; i < 50000; i++) {
                Element element = new Element(key, value);
                store.put(element);
                store.get(key + i);
            }
            for (int i = 0; i < 50000; i++) {
                store.remove(key + i);
            }
            long time = stopWatch.getElapsedTime();
            LOG.info("Time for benchmarkPutGetRemove: " + time);
            assertTrue("Too slow. Time was " + time, time < 500);
        } finally {
            manager.shutdown();
        }
    }


    /**
     * Benchmark to test speed.
     */
    @Test
    public void testBenchmarkPutGet() throws Exception {
        CacheManager manager = new CacheManager(new Configuration().name("testBenchmarkPutGet"));
        try {
            Cache cache = createCache();
            manager.addCache(cache);
            Store store = cache.getStore();
            final String key = "key";
            byte[] value = new byte[500];
            StopWatch stopWatch = new StopWatch();

            // Add a bunch of entries
            for (int i = 0; i < 50000; i++) {
                Element element = new Element(key, value);
                store.put(element);
            }
            for (int i = 0; i < 50000; i++) {
                store.get(key + i);
            }
            long time = stopWatch.getElapsedTime();
            LOG.info("Time for benchmarkPutGet: " + time);
            assertTrue("Too slow. Time was " + time, time < 300);
        } finally {
            manager.shutdown();
        }
    }


    /**
     * Benchmark to test speed.
     * <p/>
     * With iteration up to 5000:
     * 100: Time for putSpeed: 2772
     * 1000: Time for putSpeed: 10943
     * 4000: Time for putSpeed: 42367
     * 10000: Time for putSpeed: 4179
     * 300000: Time for putSpeed: 6616
     * <p/>
     * With no iteration:
     * 100: Time for putSpeed: 2358
     * 1000: Time for putSpeed: 2692
     * 4000: Time for putSpeed: 2833
     * 10000: Time for putSpeed: 3630
     * 300000: Time for putSpeed: 6616
     */
    @Test
    public void testPutSpeed() throws Exception {
        CacheManager manager = new CacheManager(new Configuration().name("testOverflowToDiskWithLargeNumberofCacheEntries"));
        try {
            Cache cache = new Cache("testPutSpeed", 4000, false, true, 120, 120);
            manager.addCache(cache);
            Store store = cache.getStore();
            final Long key = 0L;
            byte[] value = new byte[1];
            StopWatch stopWatch = new StopWatch();

            // Add a bunch of entries
            for (int i = 0; i < 500000; i++) {
                Element element = new Element(key + i, value);
                store.put(element);
            }
            long time = stopWatch.getElapsedTime();
            LOG.info("Time for putSpeed: " + time);
            assertTrue("Too slow. Time was " + time, time < 4000);
        } finally {
            manager.shutdown();
        }
    }


    /**
     * Benchmark to test speed.
     * Original implementation 12seconds
     * This implementation 9 seconds
     */
    public void benchmarkPutGetSuryaTest(long allowedTime) throws Exception {
        CacheManager manager = new CacheManager(new Configuration().name("testBenchmarkPutGetRemove"));
        try {
            Cache cache = createCache();
            manager.addCache(cache);
            Store store = cache.getStore();
            Random random = new Random();
            byte[] value = new byte[500];
            StopWatch stopWatch = new StopWatch();

            // Add a bunch of entries
            for (int i = 0; i < 50000; i++) {
                String key = "key" + i;

                Element element = new Element(key, value);
                store.put(element);

                //Access each element random number of times, min:0 maximum:9
                int accesses = random.nextInt(5);
                for (int j = 0; j <= accesses; j++) {
                    store.get(key);
                }
            }
            long time = stopWatch.getElapsedTime();
            LOG.info("Time for benchmarkPutGetSurya: " + time);
            assertTrue("Too slow. Time was " + time, time < allowedTime);
        } finally {
            manager.shutdown();
        }
    }
    
    protected static Store getStore(Cache cache) {
        return cache.getStore();
    }
}
