package net.sf.ehcache.constructs.locking;

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

import net.sf.ehcache.exceptionhandler.ExceptionHandlingDynamicCacheProxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class ExplicitLockingCacheTest extends TestCase {

    private static final String TEST_CACHE = "ExplicitLockCacheTest";
    private ExplicitLockingCache testCache;
    private CacheManager manager;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration();
        CacheConfiguration defaultCacheConfiguration = new CacheConfiguration("default", 2000);
        configuration.addDefaultCache(defaultCacheConfiguration);

        CacheConfiguration theCache = new CacheConfiguration(TEST_CACHE, 10000);
        // add more settings here!
        configuration.addCache(theCache);

        manager = new CacheManager(configuration);
        testCache = new ExplicitLockingCache(manager.getCache(TEST_CACHE));
    }

    @After
    public void teardown() {
        manager.shutdown();
    }


    /**
     * Can we add one as a decorated cache?
     * https://jira.terracotta.org/jira/browse/EHC-709 reports that this does not work
     */
    @Test
    public void testCanAddAsDecoratedCache() {
        manager.replaceCacheWithDecoratedCache(manager.getCache(TEST_CACHE), testCache);
        assertNotNull(manager.getEhcache(TEST_CACHE));
    }

    /**
     * Show that if an entry already exists for the given key it is not re-added
     */
    @Test
    public void testPutIfAbsent() {
        assertEquals(testCache.get("putIfAbsent"), null);
        testCache.putIfAbsent(new Element("putIfAbsent", "1"));
        assertEquals(testCache.get("putIfAbsent").getValue(), "1");
        testCache.putIfAbsent(new Element("putIfAbsent", "2"));
        assertEquals(testCache.get("putIfAbsent").getValue(), "1");
    }
}
