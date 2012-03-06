package net.sf.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.util.RetryAssert;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class DiskStorePerfTest extends AbstractCachePerfTest {

    private static final Logger LOG = LoggerFactory.getLogger(DiskStorePerfTest.class.getName());
    private CacheManager manager2;

    /**
     * Test overflow to disk = true, using 100000 records.
     * 15 seconds v1.38 DiskStore
     * 2 seconds v1.42 DiskStore
     * Adjusted for change to laptop
     */
    @Test
    public void testOverflowToDiskWithLargeNumberofCacheEntries() throws Exception {

        //Set size so the second element overflows to disk.
        //Cache cache = new Cache("test", 1000, MemoryStoreEvictionPolicy.LRU, true, null, true, 500, 500, false, 1, null);
        Cache cache = new Cache(new CacheConfiguration("test", 1000)
            .memoryStoreEvictionPolicy("LRU")
            .eternal(true)
            .overflowToDisk(true)
            .timeToLiveSeconds(1)
            .diskAccessStripes(1)
            .diskExpiryThreadIntervalSeconds(60));
        manager.addCache(cache);
        int i = 0;
        StopWatch stopWatch = new StopWatch();
        for (; i < 100000; i++) {
            cache.put(new Element("" + i,
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        }
        long time = stopWatch.getElapsedTime();
        LOG.info("time: " + time);
        //XXX this assertion is nonsensical - what is it here for?
        assertThat(time, greaterThan(4L));
    }


    /**
     * Test overflow to disk = true, using 100000 records.
     * 35 seconds v1.38 DiskStore
     * 26 seconds v1.42 DiskStore
     */
    @Test
    public void testOverflowToDiskWithLargeNumberofCacheEntriesAndGets() throws Exception {
        final int ELEMENTS = 100000;
        final Cache cache = new Cache(new CacheConfiguration("test", 1000)
            .memoryStoreEvictionPolicy("LRU")
            .eternal(true)
            .overflowToDisk(true)
            .diskAccessStripes(5)
            .diskExpiryThreadIntervalSeconds(60));
        manager.addCache(cache);
        Random random = new Random();
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < ELEMENTS; i++) {
            cache.put(new Element(Integer.toString(i),
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

            cache.get(Integer.toString(random.nextInt(i + 1)));
        }

        RetryAssert.assertBy(10, TimeUnit.SECONDS, new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return cache.getDiskStoreSize();
            }
        }, equalTo(ELEMENTS));
        assertThat(cache.getSize(), equalTo(ELEMENTS));
        
        long elapsed = stopWatch.getElapsedTime();
        LOG.info("Elapsed time: " + elapsed);
    }

    /**
     * Runs out of memory at 5,099,999 elements with the standard 64MB VM size on 32 bit architectures.
     * Around 3,099,999 for AMD64. Why? See abstract citation below from
     * http://www3.interscience.wiley.com/cgi-bin/abstract/111082816/ABSTRACT?CRETRY=1&SRETRY=0
     * <pre>
     * By running the PowerPC machine in both 32-bit and 64-bit mode we are able to compare 32-bit and 64-bit VMs.
     * We conclude that the space an object takes in the heap in 64-bit mode is 39.3% larger on average than in
     * 32-bit mode. We identify three reasons for this: (i) the larger pointer size, (ii) the increased header
     * and (iii) the increased alignment. The minimally required heap size is 51.1% larger on average in 64-bit
     * than in 32-bit mode. From our experimental setup using hardware performance monitors, we observe that 64-bit
     * computing typically results in a significantly larger number of data cache misses at all levels of the memory
     * hierarchy. In addition, we observe that when a sufficiently large heap is available, the IBM JDK 1.4.0 VM is
     * 1.7% slower on average in 64-bit mode than in 32-bit mode. Copyright © 2005 John Wiley & Sons, Ltd.
     * </pre>
     * The reason that it is not infinite is because of a small amount of memory used (about 12 bytes) used for
     * the disk store index in this case.
     * <p/>
     * Slow tests
     */
    @Test
    @Ignore
    public void testMaximumCacheEntriesIn64MBWithOverflowToDisk() throws Exception {

        Cache cache = new Cache("test", 1000, MemoryStoreEvictionPolicy.LRU, true, null, true, 500, 500, false, 1, null);
        manager.addCache(cache);
        StopWatch stopWatch = new StopWatch();
        int index = 0;
        try {
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100000; j++) {
                    index = (1000000 * i) + j;
                    cache.put(new Element(Integer.valueOf(index),
                            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                }
                //wait to write entries
                int size = cache.getSize();
                Thread.sleep(2000);
            }
            long elapsed = stopWatch.getElapsedTime();
            LOG.info("Elapsed time: " + elapsed / 1000);
            fail();
        } catch (OutOfMemoryError e) {
            LOG.info("All heap consumed after " + index + " entries created.");
            assertThat(index, greaterThanOrEqualTo(3090000));
        }
    }

        /**
     * Perf test used by Venkat Subramani
     * Get took 119s with Cache svn21
     * Get took 42s
     * The change was to stop adding DiskStore retrievals into the MemoryStore. This made sense when the only
     * policy was LRU. In the new version an Element, once evicted from the MemoryStore, stays in the DiskStore
     * until expiry or removal. This avoids a lot of serialization overhead.
     * <p/>
     * Slow tests
     * 235 with get. 91 for 1.2.3. 169 with remove.
     *
     * put 14, get 32
     * put 13, get 19
     *
     */
    @Test
    @Ignore
    public void testLargePutGetPerformanceWithOverflowToDisk() throws Exception {

        Cache cache = new Cache("test", 1000, MemoryStoreEvictionPolicy.LRU, true, null, true, 500, 500, false, 10000, null);
        manager.addCache(cache);
        StopWatch stopWatch = new StopWatch();
        int i = 0;
        int j = 0;
        Integer index;
        for (; i < 5; i++) {
            for (j = 0; j < 100000; j++) {
                index = Integer.valueOf(((1000000 * i) + j));
                cache.put(new Element(index,
                        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
            }
        }
        long elapsed = stopWatch.getElapsedTime();
        long putTime = ((elapsed / 1000) - 10);
        LOG.info("Put Elapsed time: " + putTime);
        assertThat(putTime, lessThan(20L));

        //wait for Disk Store to finish spooling
        while (cache.getStore().bufferFull()) {
            Thread.sleep(2000);
        }
        Random random = new Random();
        StopWatch getStopWatch = new StopWatch();
        long getStart = stopWatch.getElapsedTime();

        for (int k = 0; k < 1000000; k++) {
            Integer key = Integer.valueOf(random.nextInt(500000));
            cache.get(key);
        }

        long getElapsedTime = getStopWatch.getElapsedTime();
        int time = (int) ((getElapsedTime - getStart) / 1000);
        LOG.info("Get Elapsed time: " + time);

        assertThat(time, lessThan(180));
    }

    /**
     * teardown
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (manager2 != null) {
            manager2.shutdown();
        }
        deleteFile("persistentLongExpiryIntervalCache");
        deleteFile("fileTest");
        deleteFile("testPersistent");
        deleteFile("testLoadPersistent");
        deleteFile("testPersistentWithDelete");
    }

}
