package net.sf.ehcache;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import java.util.logging.Logger;
import java.util.Date;

/**
 * Does the cache tests using the classic LRUMemoryStore implementation.
 */
public class CacheClassicLruMemoryStoreTest extends CacheTest {

    private static final Logger LOG = Logger.getLogger(CacheClassicLruMemoryStoreTest.class.getName());

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
     * CHM with sampling
     * INFO: Average Get Time for 5424446 observations: 0.0046010227 ms
     * INFO: Average Put Time for 358907 obervations: 0.027190888 ms
     * INFO: Average Remove Time for 971741 obervations: 0.00924732 ms
     * INFO: Average keySet Time for 466812 observations: 0.15059596 ms
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
