package net.sf.ehcache.constructs.locking;

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class ExplicitLockingCacheTest extends TestCase {

    private static final String TEST_CACHE = "ExplicitLockCacheTest";
    private ExplicitLockingCache testCache;

    protected void setUp() throws Exception {
        super.setUp();
        Configuration configuration = new Configuration();
        CacheConfiguration defaultCacheConfiguration = new CacheConfiguration();
        defaultCacheConfiguration.setEternal(false);
        defaultCacheConfiguration.setName("default");
        configuration.addDefaultCache(defaultCacheConfiguration);

        CacheConfiguration theCache = new CacheConfiguration();
        theCache.setName(TEST_CACHE);
        theCache.setEternal(false);
        theCache.setMaxElementsInMemory(10000);
        // add more settings here!
        configuration.addCache(theCache);

        CacheManager mgr = new CacheManager(configuration);
        testCache = new ExplicitLockingCache(mgr.getCache(TEST_CACHE));
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
