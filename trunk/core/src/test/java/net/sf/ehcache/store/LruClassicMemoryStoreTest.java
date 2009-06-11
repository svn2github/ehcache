package net.sf.ehcache.store;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.AbstractCacheTest;

import java.util.Date;

/**
 * Tests the pre ehcache-1.6 LruMemoryStore, which can be switched in
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

}
