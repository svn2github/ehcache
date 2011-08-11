package net.sf.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Alex Snaps
 */
public class EvictionListenerTest {

    private static CacheManager cacheManager;
    private static final String CACHE_NAME = "listening";
    private Cache cache;

    @BeforeClass
    public static void createCacheManager() {
        Configuration configuration = new Configuration();
        configuration.diskStore(new DiskStoreConfiguration().path("./target/tmp/"));
        cacheManager = new CacheManager(configuration);
    }

    @Before
    public void setup() {
        CacheConfiguration configuration = new CacheConfiguration(CACHE_NAME, 100)
            .overflowToDisk(true)
            .maxEntriesLocalDisk(2000);
        cache = new Cache(configuration);
        cacheManager.addCache(cache);
    }

    @Test
    public void testEvictedOnlyOnce() throws InterruptedException {
        CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        for(int i = 0; i < 10000; i++) {
            cache.get("key" + (1000 + (i % 10)));
            cache.put(new Element("key" + i, "value" + i));
        }
        for (Object o : countingCacheEventListener.getCacheElementsEvicted(cache)) {
            System.out.println(o.getClass().getSimpleName() + ": " + o);
        }
    }

    @After
    public void tearDown() {
        cacheManager.removeCache(CACHE_NAME);
    }

    @AfterClass
    public static void cleanUp() {
        cacheManager.shutdown();
    }
}
