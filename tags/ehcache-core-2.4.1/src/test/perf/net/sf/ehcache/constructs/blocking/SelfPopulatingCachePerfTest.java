package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CachePerfTest;
import net.sf.ehcache.Ehcache;
import org.junit.After;
import org.junit.Before;

/**
 * @author Alex Snaps
 */
public class SelfPopulatingCachePerfTest extends CachePerfTest {

    /**
     * Shared with subclass
     */
    protected CacheManager manager;
    /**
     * Shared with subclass
     */
    protected SelfPopulatingCache selfPopulatingCache;
    /**
     * Shared with subclass
     */
    protected Ehcache cache;

    /**
     * Number of factory requests
     */
    protected volatile int cacheEntryFactoryRequests;

    /**
     * Load up the test cache
     */
    @Override
    @Before
    public void setUp() throws Exception {
        //Skip update checks. Causing an OutOfMemoryError
        System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
        super.setUp();
        manager = new CacheManager();
        cache = manager.getCache("sampleIdlingExpiringCache");
        selfPopulatingCache = new SelfPopulatingCache(cache, new CountingCachePerfEntryFactory("value"));
        cacheEntryFactoryRequests = 0;
    }

    /**
     * teardown
     */
    @Override
    @After
    public void tearDown() throws Exception {
        if (selfPopulatingCache != null) {
            selfPopulatingCache.removeAll();
        }
        if (manager != null) {
            manager.shutdown();
        }
        super.tearDown();
    }
}
