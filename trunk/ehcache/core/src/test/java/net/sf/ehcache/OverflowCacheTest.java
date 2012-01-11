package net.sf.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class OverflowCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheTest.class.getName());

    private CacheManager manager;

    @Before
    public void setUp() throws Exception {
        manager = CacheManager.create();
    }

    /**
     * teardown
     */
    @After
    public void tearDown() throws Exception {
        if (manager != null) {
            manager.shutdown();
        }
    }


    /**
     * Shows the effect of jamming large amounts of puts into a cache that overflows to disk.
     * The DiskStore should cause puts to back off and avoid an out of memory error.
     */
    @Test
    public void testBehaviourOnDiskStoreBackUp() throws Exception {
        Cache cache = new Cache(new CacheConfiguration().name("testBehaviourOnDiskStoreBackUp")
            .maxElementsInMemory(1000)
            .overflowToDisk(true)
            .eternal(false)
            .timeToLiveSeconds(100)
            .timeToIdleSeconds(200)
            .diskPersistent(false)
            .diskExpiryThreadIntervalSeconds(0)
            .diskSpoolBufferSizeMB(10));
        manager.addCache(cache);

        assertEquals(0, cache.getMemoryStoreSize());

        Element a;
        int i = 0;
        try {
            for (; i < 150000; i++) {
                String key = i + "";
                String value = key;
                a = new Element(key, value + "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
                cache.put(a);
            }
        } catch (OutOfMemoryError e) {
            LOG.info("OutOfMemoryError: " + e.getMessage() + " " + i);
            fail();
        }
    }


}
