package net.sf.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Snaps
 */
public class CacheClassicLruMemoryStorePerfTest extends CachePerfTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheClassicLruMemoryStorePerfTest.class.getName());

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty(Cache.NET_SF_EHCACHE_USE_CLASSIC_LRU, "true");
    }


    @AfterClass
    public static void afterClass() throws Exception {
        System.setProperty(Cache.NET_SF_EHCACHE_USE_CLASSIC_LRU, "false");
    }


    /**
     * Classic LRUMemoryStore
     * INFO: Average Get Time for 608440 observations: 0.42026988 ms
     * INFO: Average Put Time for 245549 obervations: 0.39700833 ms
     * INFO: Average Remove Time for 137129 obervations: 0.4974586 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 439951 observations: 0.22111326 ms
     * INFO: Total loads: 229
     * INFO: Total loadAlls: 469
     * <p/>
     * Contrast this with the new one:
     *
     * INFO: Average Get Time for 4754409 observations: 0.0072621014 ms
     * INFO: Average Put Time for 491269 obervations: 0.03955267 ms
     * INFO: Average Remove Time for 1183817 obervations: 0.009963533 ms
     * INFO: Average Remove All Time for 0 observations: NaN ms
     * INFO: Average keySet Time for 364313 observations: 0.2294785 ms
     * INFO: Total loads: 197
     * INFO: Total loadAlls: 511
     *
     *
     * @throws Exception
     */
    @Override
    @Test
    public void testConcurrentReadWriteRemoveLRU() throws Exception {
        testConcurrentReadWriteRemove(MemoryStoreEvictionPolicy.LRU);
    }

    @Override
    @Test
    public void testMemoryEfficiencyOfFlushWhenOverflowToDisk() throws Exception {
        CacheManager manager = new CacheManager(new Configuration().name("testMemoryEfficiencyOfFlushWhenOverflowToDisk")
                .diskStore(new DiskStoreConfiguration().path(diskFolder.getRoot().getAbsolutePath())));
        try {
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

            assertEquals(0, cache.getStatistics().getLocalHeapSize());

            for (int i = 0; i < 80000; i++) {
                cache.put(new Element("" + i, new byte[480]));
            }
            LOG.info("Put time: " + stopWatch.getElapsedTime());
            Thread.sleep(2000);
            assertEquals(40000, cache.getStatistics().getLocalHeapSize());
            assertEquals(40000, cache.getStatistics().getLocalDiskSize());

            long beforeMemory = AbstractCacheTest.measureMemoryUse();
            stopWatch.getElapsedTime();
            cache.flush();
            LOG.info("Flush time: " + stopWatch.getElapsedTime());

            //It takes a while to write all the Elements to disk
            Thread.sleep(1000);

            long afterMemory = AbstractCacheTest.measureMemoryUse();
            long memoryIncrease = afterMemory - beforeMemory;
            assertTrue(memoryIncrease < 40000000);

            assertEquals(0, cache.getStatistics().getLocalHeapSize());
            assertEquals(40000, cache.getStatistics().getLocalDiskSize());
        } finally {
            manager.shutdown();
        }
    }

}
