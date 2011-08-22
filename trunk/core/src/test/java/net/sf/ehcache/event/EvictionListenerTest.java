package net.sf.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

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
        CacheConfiguration configuration = new CacheConfiguration()
            .name(CACHE_NAME)
            .maxBytesLocalHeap(100, MemoryUnit.KILOBYTES);
//            .overflowToDisk(true)
//            .maxEntriesLocalDisk(2000)
        cache = new Cache(configuration);
        cacheManager.addCache(cache);
    }

    @Test
    public void testEvictedOnlyOnce() throws InterruptedException {
        CountingCacheEventListener.resetCounters();
        CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        final int amountOfElements = 10000;
        for(int i = 0; i < amountOfElements; i++) {
            cache.get("key" + (1000 + (i % 10)));
            cache.put(new Element("key" + i, "value" + i));
        }
        assertThat(amountOfElements - CountingCacheEventListener.getCacheElementsEvicted(cache).size(), equalTo(cache.getSize()));
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
