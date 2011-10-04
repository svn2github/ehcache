package net.sf.ehcache;

import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

}
