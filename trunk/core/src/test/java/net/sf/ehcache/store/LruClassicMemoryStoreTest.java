package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the pre ehcache-1.6 LruMemoryStore, which can be switched in
 *
 * @author Greg Luck
 */
public class LruClassicMemoryStoreTest extends LruMemoryStoreTest {

    /**
     * setup test
     */
    @Before
    public void setUp() throws Exception {
        System.setProperty(Cache.NET_SF_EHCACHE_USE_CLASSIC_LRU, "true");
        super.setUp();
    }


    /**
     * teardown
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        System.setProperty(Cache.NET_SF_EHCACHE_USE_CLASSIC_LRU, "false");
    }

    /**
     * Test the LRU policy
     */
    @Test
    public void testProbabilisticEvictionPolicy() throws Exception {
        super.testProbabilisticEvictionPolicy();
    }

}
