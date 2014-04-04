package net.sf.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.exceptionhandler.ExceptionHandlingDynamicCacheProxy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import net.sf.ehcache.store.disk.PerfDiskStoreHelper;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

//import net.sf.ehcache.loader.CountingCacheLoader;

/**
 * @author Alex Snaps
 */
public class CachePerfTest extends AbstractCachePerfTest {

    private static final Logger LOG = LoggerFactory.getLogger(CachePerfTest.class.getName());


    /**
     * Checks the expense of checking for duplicates
     * Typical Results Duplicate Check: 8ms versus 3ms for No Duplicate Check
     * <p/>
     * 66ms for 1000, 6ms for no duplicate/expiry
     * 187565 for 100000, where 500 is the in-memory size. 964ms without checking expiry. 134ms for getKeysNoDuplicateCheckTime
     * 18795 for 100000, where 50000 is in-memory size. 873ms without checking expiry. 158ms for getKeysNoDuplicateCheckTime
     */
    @Test
    public void testGetKeysPerformance() throws Exception {
        //Set size so the second element overflows to disk.
        Ehcache cache = createTestCache();

        for (int i = 0; i < 2000; i++) {
            cache.put(new Element("key" + i, "value"));
        }
        //let the notifiers cool down
        Thread.sleep(1000);
        StopWatch stopWatch = new StopWatch();
        List keys = cache.getKeys();
        assertTrue("Should be 2000 keys. ", keys.size() == 2000);
        long getKeysTime = stopWatch.getElapsedTime();
        cache.getKeysNoDuplicateCheck();
        long getKeysNoDuplicateCheckTime = stopWatch.getElapsedTime();
        LOG.info("Time to get 1000 keys: With Duplicate Check: " + getKeysTime
                + " Without Duplicate Check: " + getKeysNoDuplicateCheckTime);
        assertTrue("Getting keys took more than 150ms", getKeysTime < 100);
    }


    /**
     * Performance tests for a range of Memory Store - Disk Store combinations.
     * <p/>
     * This demonstrates that a memory only store is approximately an order of magnitude
     * faster than a disk only store.
     * <p/>
     * It also shows that double the performance of a Disk Only store can be obtained
     * with a maximum memory size of only 1. Accordingly a Cache created without a
     * maximum memory size of less than 1 will issue a warning.
     * <p/>
     * Threading changes were made in v1.41 of DiskStore. The before and after numbers are shown.
     * <p/>
     * This test also has a cache with a CacheExceptionHandler registered. The performance effect is not detectable.
     */
    @Test
    public void testProportionMemoryAndDiskPerformance() throws Exception {
        StopWatch stopWatch = new StopWatch();
        long time = 0;

        //Memory only Typical 192ms
        Cache memoryOnlyCache = new Cache("testMemoryOnly", 5000, false, false, 5, 2);
        manager.addCache(memoryOnlyCache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = Integer.valueOf(i);
            memoryOnlyCache.put(new Element(Integer.valueOf(i), "value"));
            memoryOnlyCache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for MemoryStore: " + time);
        assertTrue("Time to put and get 5000 entries into MemoryStore", time < 300);

        //Memory only Typical 192ms
        for (int j = 0; j < 10; j++) {
            time = stopWatch.getElapsedTime();
            for (int i = 0; i < 5000; i++) {
                Integer key = Integer.valueOf(i);
                memoryOnlyCache.put(new Element(Integer.valueOf(i), "value"));
                memoryOnlyCache.get(key);
            }
            time = stopWatch.getElapsedTime();
            LOG.info("Time for MemoryStore: " + time);
            assertTrue("Time to put and get 5000 entries into MemoryStore", time < 300);
            Thread.sleep(500);
        }

        //Memory only with ExceptionHandlingTypical 192ms
        manager.replaceCacheWithDecoratedCache(memoryOnlyCache, ExceptionHandlingDynamicCacheProxy.createProxy(memoryOnlyCache));
        Ehcache exceptionHandlingMemoryOnlyCache = manager.getEhcache("testMemoryOnly");
        for (int j = 0; j < 10; j++) {
            time = stopWatch.getElapsedTime();
            for (int i = 0; i < 5000; i++) {
                Integer key = Integer.valueOf(i);
                exceptionHandlingMemoryOnlyCache.put(new Element(Integer.valueOf(i), "value"));
                exceptionHandlingMemoryOnlyCache.get(key);
            }
            time = stopWatch.getElapsedTime();
            LOG.info("Time for exception handling MemoryStore: " + time);
            assertTrue("Time to put and get 5000 entries into exception handling MemoryStore", time < 300);
            Thread.sleep(500);
        }

        //Set size so that all elements overflow to disk.
        // 1245 ms v1.38 DiskStore
        // 273 ms v1.42 DiskStore
        Cache diskOnlyCache = new Cache("testDiskOnly", 1, true, false, 5, 2);
        manager.addCache(diskOnlyCache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = Integer.valueOf(i);
            diskOnlyCache.put(new Element(key, "value"));
            diskOnlyCache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for DiskStore: " + time);
        assertTrue("Time to put and get 5000 entries into DiskStore was less than 2 sec", time < 2000);

        // 1 Memory, 999 Disk
        // 591 ms v1.38 DiskStore
        // 56 ms v1.42 DiskStore
        Cache m1d999Cache = new Cache("m1d999Cache", 1, true, false, 5, 2);
        manager.addCache(m1d999Cache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = Integer.valueOf(i);
            m1d999Cache.put(new Element(key, "value"));
            m1d999Cache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for m1d999Cache: " + time);
        assertTrue("Time to put and get 5000 entries into m1d999Cache", time < 2000);

        // 500 Memory, 500 Disk
        // 669 ms v1.38 DiskStore
        // 47 ms v1.42 DiskStore
        Cache m500d500Cache = new Cache("m500d500Cache", 500, true, false, 5, 2);
        manager.addCache(m500d500Cache);
        time = stopWatch.getElapsedTime();
        for (int i = 0; i < 5000; i++) {
            Integer key = Integer.valueOf(i);
            m500d500Cache.put(new Element(key, "value"));
            m500d500Cache.get(key);
        }
        time = stopWatch.getElapsedTime();
        LOG.info("Time for m500d500Cache: " + time);
        assertTrue("Time to put and get 5000 entries into m500d500Cache", time < 2000);

    }


    /**
     * Checks the expense of checking in-memory size
     * 3467890 bytes in 1601ms for JDK1.4.2
     */
    @Test
    public void testCalculateInMemorySizePerformanceAndReasonableness() throws Exception {
        //Set size so the second element overflows to disk.
        Cache cache1 = new Cache("test4", 1000, false, true, 0, 0);
        manager.addCache(cache1);
        Ehcache cache = cache1;

        //Set up object graphs
        for (int i = 0; i < 1000; i++) {
            HashMap map = new HashMap(100);
            for (int j = 0; j < 100; j++) {
                map.put("key" + j, new String[]{"adfdafs", "asdfdsafa", "sdfasdf"});
            }
            cache.put(new Element("key" + i, map));
        }

        StopWatch stopWatch = new StopWatch();
        long size = cache.calculateInMemorySize();
        assertTrue("Size is " + size + ". Check it for reasonableness.", size > 10000000 && size < 22000000);
        long elapsed = stopWatch.getElapsedTime();
        LOG.info("In-memory size in bytes: " + size
                + " time to calculate in ms: " + elapsed);
        assertTrue("Calculate memory size takes less than 3.5 seconds", elapsed < 3500);
    }

    /**
     * When flushing large MemoryStores, OutOfMemory issues can happen if we are
     * not careful to move each Element to the DiskStore, rather than copy them all
     * and then delete them from the MemoryStore.
     * <p/>
     * This test manipulates a MemoryStore right on the edge of what can fit into the 64MB standard VM size.
     * An inefficient spool will cause an OutOfMemoryException.
     *
     * @throws Exception
     */
    @Test
    public void testMemoryEfficiencyOfFlushWhenOverflowToDisk() throws Exception {
        CacheConfiguration config = new CacheConfiguration("testGetMemoryStoreSize", 40000);
        config.setOverflowToDisk(true);
        config.setEternal(false);
        config.setTimeToLiveSeconds(100);
        config.setTimeToIdleSeconds(200);
        config.setDiskPersistent(false);
        config.setDiskExpiryThreadIntervalSeconds(120);
        Cache cache = new Cache(config);

        manager.addCache(cache);
        StopWatch stopWatch = new StopWatch();

        assertEquals(0, cache.getMemoryStoreSize());

        for (int i = 0; i < 80000; i++) {
            cache.put(new Element("" + i, new byte[480]));
        }
        LOG.info("Put time: " + stopWatch.getElapsedTime());
        PerfDiskStoreHelper.flushAllEntriesToDisk(cache).get();
        assertEquals(40000, cache.getMemoryStoreSize());
        assertEquals(80000, cache.getDiskStoreSize());

        long beforeMemory = measureMemoryUse();
        stopWatch.getElapsedTime();
        cache.flush();
        LOG.info("Flush time: " + stopWatch.getElapsedTime());

        //It takes a while to write all the Elements to disk
        Thread.sleep(1000);

        long afterMemory = measureMemoryUse();
        long memoryIncrease = afterMemory - beforeMemory;
        assertTrue(memoryIncrease < 40000000);

        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(80000, cache.getDiskStoreSize());

    }

    /**
     * Orig.
     * INFO: Average Get Time: 0.37618342 ms
     * INFO: Average Put Time: 0.61346555 ms
     * INFO: Average Remove Time: 0.43651128 ms
     * INFO: Average Remove All Time: 0.20818481 ms
     * INFO: Average keySet Time: 0.11898771 ms
     * <p/>
     * CLHM
     * INFO: Average Get Time for 3611277 observations: 0.0043137097 ms
     * INFO: Average Put Time for 554433 obervations: 0.011824693 ms
     * INFO: Average Remove Time for 802361 obervations: 0.008200797 ms
     * INFO: Average Remove All Time for 2887862 observations: 4.685127E-4 ms
     * INFO: Average keySet Time for 2659524 observations: 0.003155828 ms
     * <p/>
     * CHM with sampling
     * INFO: Average Get Time for 5424446 observations: 0.0046010227 ms
     * INFO: Average Put Time for 358907 obervations: 0.027190888 ms
     * INFO: Average Remove Time for 971741 obervations: 0.00924732 ms
     * INFO: Average keySet Time for 466812 observations: 0.15059596 ms
     * <p/>
     * After putting back synchronized:
     * <p/>
     * INFO: Average Get Time for 7184321 observations: 0.009596036 ms
     * INFO: Average Put Time for 15853 obervations: 0.117264874 ms
     * INFO: Average Remove Time for 385518 obervations: 0.017298803 ms
     * INFO: Average Remove All Time for 456174 observations: 0.10433519 ms
     * INFO: Average keySet Time for 4042893 observations: 0.0029669348 ms
     * INFO: Total loads: 123
     * <p/>
     * Ehcache 2.0: After turning off statistics.
     * Feb 3, 2010 1:50:32 PM net.sf.ehcache.CacheTest testConcurrentReadWriteRemove
     * INFO: Average Get Time for 7251897 observations: 0.006588345 ms
     * INFO: Average Put Time for 6190 obervations: 0.07479806 ms
     * INFO: Average Remove Time for 4428 obervations: 0.7606143 ms
     * INFO: Average Remove All Time for 5183786 observations: 0.0020039408 ms
     * INFO: Average keySet Time for 4973208 observations: 0.0020630546 ms
     * INFO: Total loadAlls: 189
     * <p/>
     * Aug 10, 2011 4:43:47 PM Ehcache 2.1 Revalidation on Mac OS X Lion and same machine
     * INFO: Average Get Time for 7474731 observations: 0.0066825147 ms
     * INFO: Average Put Time for 12918 obervations: 0.6070599 ms
     * INFO: Average Remove Time for 57024 obervations: 0.06649832 ms
     * INFO: Average Remove All Time for 5147782 observations: 0.0026411375 ms
     * INFO: Average keySet Time for 4717506 observations: 0.002453627 ms
     * <p/>
     * Aug 10, 2011 4:47:16 PM 2.5 beta
     * INFO: Average Get Time for 517885 observations: 0.16877685 ms
     * INFO: Average Put Time for 52501 obervations: 0.5059332 ms
     * INFO: Average Remove Time for 30177 obervations: 0.7006329 ms
     * INFO: Average Remove All Time for 107152 observations: 0.8572775 ms
     * INFO: Average keySet Time for 98991 observations: 0.92390215 ms
     * <p/>
     * Aug 12, 2011 11:24:40 AM Java 6 Agent sizeof with "value" value
     * INFO: Average Get Time for 1378042 observations: 0.18543556 ms
     * INFO: Average Put Time for 1056477 obervations: 0.092374 ms
     * INFO: Average Remove Time for 2013940 obervations: 0.032961756 ms
     * INFO: Average Remove All Time for 894820 observations: 0.10991708 ms
     * INFO: Average keySet Time for 114488 observations: 0.8662655 ms
     * <p/>
     * Aug 12, 2011 11:32:38 AM with Java 6 Agent sizeof with list of stacktraces value
     * INFO: Average Put Time for 753610 obervations: 0.119633496 ms
     */
    @Test
    public void testConcurrentReadWriteRemoveLRU() throws Exception {
        testConcurrentReadWriteRemove(MemoryStoreEvictionPolicy.LRU);
    }

    /**
     * <pre>
     * Orig.
     * INFO: Average Get Time: 1.2396777 ms
     * INFO: Average Put Time: 1.4968935 ms
     * INFO: Average Remove Time: 1.3399061 ms
     * INFO: Average Remove All Time: 0.22590445 ms
     * INFO: Average keySet Time: 0.20492058 ms
     *
     * INFO: Average Get Time: 1.081209 ms
     * INFO: Average Put Time: 1.2307026 ms
     * INFO: Average Remove Time: 1.1294961 ms
     * INFO: Average Remove All Time: 0.16385451 ms
     * INFO: Average keySet Time: 0.1549516 ms
     *
     * CHM version with no sync on get.
     * INFO: Average Get Time for 2582432 observations: 0.019930825 ms
     * INFO: Average Put Time for 297 obervations: 41.40404 ms
     * INFO: Average Remove Time for 1491 obervations: 13.892018 ms
     * INFO: Average Remove All Time for 135893 observations: 0.54172766 ms
     * INFO: Average keySet Time for 112686 observations: 0.7157411 ms
     *
     * 1.6
     * INFO: Average Get Time for 4984448 observations: 0.006596317 ms
     * INFO: Average Put Time for 7266 obervations: 0.42361686 ms
     * INFO: Average Remove Time for 2024066 obervations: 0.012883473 ms
     * INFO: Average Remove All Time for 3572412 observations: 8.817572E-5 ms
     * INFO: Average keySet Time for 2653539 observations: 0.002160511 ms
     * INFO: Total loads: 38
     * </pre>
     * With iterator
     * 1.6 with 100,000 store size: puts take 45ms. keySet 7ms
     * 1.6 with 1000,000 store size: puts take 381ms. keySet 7ms
     * 1,000,000 - using FastRandom (j.u.Random was dog slow)
     * INFO: Average Get Time for 2065131 observations: 0.013553619 ms
     * INFO: Average Put Time for 46404 obervations: 0.1605034 ms
     * INFO: Average Remove Time for 20515 obervations: 0.1515964 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 198 observations: 0.0 ms
     * <p/>
     * 9999 - using iterator
     * INFO: Average Get Time for 4305030 observations: 0.006000423 ms
     * INFO: Average Put Time for 3216 obervations: 0.92008704 ms
     * INFO: Average Remove Time for 5294 obervations: 0.048545524 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 147342 observations: 0.5606073 ms
     * 10001 - using FastRandom
     * INFO: Average Get Time for 4815249 observations: 0.005541354 ms
     * INFO: Average Put Time for 5186 obervations: 0.49826455 ms
     * INFO: Average Remove Time for 129163 obervations: 0.015120429 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 177342 observations: 0.500733 ms
     * 4999 - using iterator
     * INFO: Average Get Time for 4317409 observations: 0.0061599445 ms
     * INFO: Average Put Time for 2708 obervations: 1.0768094 ms
     * INFO: Average Remove Time for 17664 obervations: 0.11713089 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 321180 observations: 0.26723954 ms
     * 5001 - using FastRandom
     * INFO: Average Get Time for 3203904 observations: 0.0053447294 ms
     * INFO: Average Put Time for 152905 obervations: 0.056616854 ms
     * INFO: Average Remove Time for 737289 obervations: 0.008854059 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 272898 observations: 0.3118601 ms
     *
     * @throws Exception
     */
    @Test
    public void testConcurrentReadWriteRemoveLFU() throws Exception {
        testConcurrentReadWriteRemove(MemoryStoreEvictionPolicy.LFU);
    }

    /**
     * INFO: Average Get Time: 0.28684255 ms
     * INFO: Average Put Time: 0.34759903 ms
     * INFO: Average Remove Time: 0.31298608 ms
     * INFO: Average Remove All Time: 0.21396147 ms
     * INFO: Average keySet Time: 0.11740683 ms
     * <p/>
     * CLHM
     * INFO: Average Get Time for 4567959 observations: 0.005231658 ms
     * INFO: Average Put Time for 437078 obervations: 0.01527645 ms
     * INFO: Average Remove Time for 178915 obervations: 0.013335941 ms
     * INFO: Average Remove All Time for 3500724 observations: 0.0070434003 ms
     * INFO: Average keySet Time for 3207776 observations: 0.011053764 ms
     */
    @Test
    public void testConcurrentReadWriteRemoveFIFO() throws Exception {
        testConcurrentReadWriteRemove(MemoryStoreEvictionPolicy.FIFO);
    }

    public void testConcurrentReadWriteRemove(MemoryStoreEvictionPolicy policy) throws Exception {

        final int size = 10000;
        //set it higher for normal continuous integration so occasional higher numbes do not break tests
        final int maxTime = (int) (500 * StopWatch.getSpeedAdjustmentFactor());
        CacheConfiguration cacheConfigurationTest3Cache = new CacheConfiguration("test3cache", size)
                .eternal(true).memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU).overflowToDisk(false)
                .statistics(false);
//        CacheConfiguration cacheConfigurationTest3Cache = new CacheConfiguration()
//                .name("test3cache").maxBytesLocalHeap(40, MemoryUnit.MEGABYTES)
//                .eternal(true).memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU).overflowToDisk(false)
//                .statistics(false);
        manager.addCache(new Cache(cacheConfigurationTest3Cache));
        final Ehcache cache = manager.getEhcache("test3cache");

        System.gc();
        Thread.sleep(500);
        System.gc();
        Thread.sleep(500);

        final AtomicLong getTimeSum = new AtomicLong();
        final AtomicLong getTimeCount = new AtomicLong();
        final AtomicLong putTimeSum = new AtomicLong();
        final AtomicLong putTimeCount = new AtomicLong();
        final AtomicLong removeTimeSum = new AtomicLong();
        final AtomicLong removeTimeCount = new AtomicLong();
        final AtomicLong removeAllTimeSum = new AtomicLong();
        final AtomicLong removeAllTimeCount = new AtomicLong();
        final AtomicLong keySetTimeSum = new AtomicLong();
        final AtomicLong keySetTimeCount = new AtomicLong();

        // TODO : Reenable that somehow!
//        CountingCacheLoader countingCacheLoader = new CountingCacheLoader();
//        cache.registerCacheLoader(countingCacheLoader);

        final List executables = new ArrayList();
        final Random random = new Random();

        ArrayList list = new ArrayList();
        StackTraceElement[] stackTraceElements;
        try {
            throw new CacheException("test");
        } catch (CacheException e) {
            stackTraceElements = e.getStackTrace();
        }
        for (int i = 0; i < 1000; i++) {
            list.add(stackTraceElements);
        }

        for (int i = 0; i < size; i++) {
            cache.put(new Element("" + i, "value"));
//            cache.put(new Element("" + i, list));
        }

        //some of the time get data
        for (int i = 0; i < 26; i++) {
            final AbstractCachePerfTest.Executable executable = new AbstractCachePerfTest.Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    cache.get("key" + random.nextInt(size));
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    assertTrue("Get time outside of allowed range: " + elapsed, elapsed < maxTime);
                    getTimeSum.getAndAdd(elapsed);
                    getTimeCount.getAndIncrement();
                }
            };
            executables.add(executable);
        }

        //some of the time add data
        for (int i = 0; i < 10; i++) {
            final AbstractCachePerfTest.Executable executable = new AbstractCachePerfTest.Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    cache.put(new Element("key" + random.nextInt(size), "value"));
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    assertTrue("Put time outside of allowed range: " + elapsed, elapsed < maxTime);
                    putTimeSum.getAndAdd(elapsed);
                    putTimeCount.getAndIncrement();
                }
            };
            executables.add(executable);
        }

        //some of the time remove the data
        for (int i = 0; i < 7; i++) {
            final AbstractCachePerfTest.Executable executable = new AbstractCachePerfTest.Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    cache.remove("key" + random.nextInt(size));
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    assertTrue("Remove time outside of allowed range: " + elapsed, elapsed < maxTime);
                    removeTimeSum.getAndAdd(elapsed);
                    removeTimeCount.getAndIncrement();
                }
            };
            executables.add(executable);
        }


        //some of the time removeAll the data
        for (int i = 0; i < 10; i++) {
            final AbstractCachePerfTest.Executable executable = new AbstractCachePerfTest.Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    int randomInteger = random.nextInt(20);
                    if (randomInteger == 3) {
                        cache.removeAll();
                    }
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    //remove all is slower
                    assertTrue("RemoveAll time outside of allowed range: " + elapsed, elapsed < (maxTime * 3));
                    removeAllTimeSum.getAndAdd(elapsed);
                    removeAllTimeCount.getAndIncrement();
                }
            };
            executables.add(executable);
        }


        //some of the time iterate
        for (int i = 0; i < 10; i++) {
            final AbstractCachePerfTest.Executable executable = new AbstractCachePerfTest.Executable() {
                public void execute() throws Exception {
                    final StopWatch stopWatch = new StopWatch();
                    long start = stopWatch.getElapsedTime();
                    int randomInteger = random.nextInt(20);
                    if (randomInteger == 3) {
                        cache.getKeys();
                    }
                    long end = stopWatch.getElapsedTime();
                    long elapsed = end - start;
                    //remove all is slower
                    assertTrue("cache.getKeys() time outside of allowed range: " + elapsed, elapsed < (maxTime * 3));
                    keySetTimeSum.getAndAdd(elapsed);
                    keySetTimeCount.getAndIncrement();
                }
            };
            executables.add(executable);
        }

        //some of the time exercise the loaders through their various methods. Loader methods themselves make no performance
        //guarantees. They should only lock the cache when doing puts and gets, which the time limits on the other threads
        //will check for.
        for (int i = 0; i < 4; i++) {
            final AbstractCachePerfTest.Executable executable = new AbstractCachePerfTest.Executable() {
                public void execute() throws Exception {
                    int randomInteger = random.nextInt(20);
                    List keys = new ArrayList();
                    for (int i = 0; i < 2; i++) {
                        keys.add("key" + random.nextInt(size));
                    }
                    if (randomInteger == 1) {
                        cache.load("key" + random.nextInt(size));
                    } else if (randomInteger == 2) {
                        cache.loadAll(keys, null);
                    } else if (randomInteger == 3) {
                        cache.getWithLoader("key" + random.nextInt(size), null, null);
                    } else if (randomInteger == 4) {
                        cache.getAllWithLoader(keys, null);
                    }
                }
            };
            executables.add(executable);
        }


        try {
            int failures = runThreadsNoCheck(executables);
            LOG.info(failures + " failures");
            //CHM does have the occasional very slow time.
            assertTrue("Failures = " + failures, failures <= 50);
        } finally {
            LOG.info("Average Get Time for " + getTimeCount.get() + " observations: "
                    + getTimeSum.floatValue() / getTimeCount.get() + " ms");
            LOG.info("Average Put Time for " + putTimeCount.get() + " obervations: "
                    + putTimeSum.floatValue() / putTimeCount.get() + " ms");
            LOG.info("Average Remove Time for " + removeTimeCount.get() + " obervations: "
                    + removeTimeSum.floatValue() / removeTimeCount.get() + " ms");
            LOG.info("Average Remove All Time for " + removeAllTimeCount.get() + " observations: "
                    + removeAllTimeSum.floatValue() / removeAllTimeCount.get() + " ms");
            LOG.info("Average keySet Time for " + keySetTimeCount.get() + " observations: "
                    + keySetTimeSum.floatValue() / keySetTimeCount.get() + " ms");
//            LOG.info("Total loads: " + countingCacheLoader.getLoadCounter());
//            LOG.info("Total loadAlls: " + countingCacheLoader.getLoadAllCounter());
        }
    }


    /**
     * Multi-thread read-write test with 20 threads
     * Just use MemoryStore to put max stress on cache
     * Values that work:
     * <pre>
     * Results 3/2/09
     * Feb 3, 2009 5:57:35 PM net.sf.ehcache.CacheTest testConcurrentReadPerformanceMemoryOnly
     * INFO: 400 threads. Average Get time: 0.033715356 ms
     * INFO: 800 threads. Average Get time: 18.419634 ms
     * INFO: 1200 threads. Average Get time: 56.21161 ms
     * INFO: 1600 threads. Average Get time: 85.19998 ms
     * INFO: 2000 threads. Average Get time: 85.83994 ms
     * </pre>
     * With ConcurrentHashMap
     * <pre>
     * INFO: 1 threads. Average Get time: 0.082987554 ms
     * INFO: 401 threads. Average Get time: 0.0070842816 ms
     * INFO: 801 threads. Average Get time: 0.0066290447 ms
     * INFO: 1201 threads. Average Get time: 0.0063261427 ms
     * INFO: 1601 threads. Average Get time: 0.005570657 ms
     * INFO: 2001 threads. Average Get time: 0.015918251 ms
     *
     * v207
     * INFO: 1 threads. Average Get time: 0.051759835 ms
     * INFO: 401 threads. Average Get time: 0.0118925795 ms
     * INFO: 801 threads. Average Get time: 0.021494854 ms
     * INFO: 1201 threads. Average Get time: 0.07880102 ms
     * INFO: 1601 threads. Average Get time: 0.067811936 ms
     * INFO: 2001 threads. Average Get time: 0.12559706 ms
     *
     * Before AtomicLong
     * INFO: 1 threads. Average Get time: 0.024948025 ms
     * INFO: 401 threads. Average Get time: 0.0079776095 ms
     * INFO: 801 threads. Average Get time: 0.0049358485 ms
     * INFO: 1201 threads. Average Get time: 0.059032038 ms
     * INFO: 1601 threads. Average Get time: 0.039221533 ms
     * INFO: 2001 threads. Average Get time: 0.03138067 ms
     *
     * INFO: 1 threads. Average Get time: 0.039014373 ms
     * INFO: 401 threads. Average Get time: 0.005683447 ms
     * INFO: 801 threads. Average Get time: 0.0041153855 ms
     * INFO: 1201 threads. Average Get time: 0.02003592 ms
     * INFO: 1601 threads. Average Get time: 0.039240483 ms
     * INFO: 2001 threads. Average Get time: 0.04503215 ms
     *
     * INFO: 1 threads. Average Get time: 0.026694044 ms
     * INFO: 401 threads. Average Get time: 0.0076737576 ms
     * INFO: 801 threads. Average Get time: 0.003894474 ms
     * INFO: 1201 threads. Average Get time: 0.06022612 ms
     * INFO: 1601 threads. Average Get time: 0.03710788 ms
     * INFO: 2001 threads. Average Get time: 0.064271376 ms
     *
     * After AtomicLong counters
     * INFO: 1 threads. Average Get time: 0.02566735 ms
     * INFO: 401 threads. Average Get time: 0.0054228795 ms
     * INFO: 801 threads. Average Get time: 0.0046341107 ms
     * INFO: 1201 threads. Average Get time: 0.075431876 ms
     * INFO: 1601 threads. Average Get time: 0.10669952 ms
     * INFO: 2001 threads. Average Get time: 0.051209673 ms
     *
     * INFO: 1 threads. Average Get time: 0.028481012 ms
     * INFO: 401 threads. Average Get time: 0.003833565 ms
     * INFO: 801 threads. Average Get time: 0.005232163 ms
     * INFO: 1201 threads. Average Get time: 0.06157142 ms
     * INFO: 1601 threads. Average Get time: 0.08064302 ms
     * INFO: 2001 threads. Average Get time: 0.048335962 ms
     *
     *
     * </pre>
     */
    @Test
    public void testConcurrentReadPerformanceMemoryOnly() throws Exception {

        final int size = 10000;

        manager.addCache(new Cache("test3cache", size, false, true, 1000, 1000));
        final Ehcache cache = manager.getEhcache("test3cache");
        final Vector<Long> readTimes = new Vector<Long>();


        for (int threads = 1; threads <= 2100; threads += 400) {

            readTimes.clear();

            final List executables = new ArrayList();
            final Random random = new Random();

            for (int i = 0; i < size; i++) {
                cache.put(new Element("" + i, "value"));
            }

            //some of the time get data
            for (int i = 0; i < threads; i++) {
                final AbstractCachePerfTest.Executable executable = new AbstractCachePerfTest.Executable() {
                    public void execute() throws Exception {
                        final StopWatch stopWatch = new StopWatch();
                        long start = stopWatch.getElapsedTime();
                        cache.get("key" + random.nextInt(size));
                        long end = stopWatch.getElapsedTime();
                        long elapsed = end - start;
                        readTimes.add(elapsed);
                        Thread.sleep(10);
                    }
                };
                executables.add(executable);
            }


            int failures = runThreadsNoCheck(executables);
            LOG.info(failures + " failures");
            assertTrue(failures == 0);
            long totalReadTime = 0;
            for (Long readTime : readTimes) {
                totalReadTime += readTime;
            }
            LOG.info(threads + " threads. Average Get time: " + totalReadTime / (float) readTimes.size() + " ms");

        }

    }


    /**
     * Creates a cache
     *
     * @return
     */
    protected Ehcache createTestCache() {
        Cache cache = new Cache("test4", 1000, true, true, 0, 0);
        manager.addCache(cache);
        return cache;
    }

}
