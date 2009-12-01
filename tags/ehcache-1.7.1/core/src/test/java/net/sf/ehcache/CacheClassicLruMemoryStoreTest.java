package net.sf.ehcache;

import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does the cache tests using the classic LRUMemoryStore implementation.
 * @author Greg Luck
 */
public class CacheClassicLruMemoryStoreTest extends CacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheClassicLruMemoryStoreTest.class.getName());

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
    @Test
    public void testConcurrentReadWriteRemoveLRU() throws Exception {
        testConcurrentReadWriteRemove(MemoryStoreEvictionPolicy.LRU);
    }

    /**
     * Tests flushing the cache, with the default, which is to clear
     *
     * Has different numbers because LRU works slightly differently
     *
     * @throws Exception
     */
    @Test
    public void testFlushWhenOverflowToDisk() throws Exception {
        if (manager.getCache("testFlushWhenOverflowToDisk") == null) {
            manager.addCache(new Cache("testFlushWhenOverflowToDisk", 50, true, false, 100, 200, true, 120));
        }
        Cache cache = manager.getCache("testFlushWhenOverflowToDisk");
        cache.removeAll();

        assertEquals(0, cache.getMemoryStoreSize());
        assertEquals(0, cache.getDiskStoreSize());


        for (int i = 0; i < 100; i++) {
            cache.put(new Element("" + i, new Date()));
            //hit
            cache.get("" + i);
        }
        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(50, cache.getDiskStoreSize());


        cache.put(new Element("key", new Object()));
        cache.put(new Element("key2", new Object()));
        Object key = new Object();
        cache.put(new Element(key, "value"));

        //get it and make sure it is mru
        Thread.sleep(15);
        cache.get(key);

        assertEquals(103, cache.getSize());
        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(53, cache.getDiskStoreSize());


        //these "null" Elements are ignored and do not get put in
        cache.put(new Element(null, null));
        cache.put(new Element(null, null));

        assertEquals(103, cache.getSize());
        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(53, cache.getDiskStoreSize());

        //this one does
        cache.put(new Element("nullValue", null));

        LOG.info("Size: " + cache.getDiskStoreSize());

        assertEquals(50, cache.getMemoryStoreSize());
        assertEquals(54, cache.getDiskStoreSize());

        cache.flush();
        assertEquals(0, cache.getMemoryStoreSize());
        //Non Serializable Elements get discarded
        assertEquals(101, cache.getDiskStoreSize());

        cache.removeAll();

    }


}
