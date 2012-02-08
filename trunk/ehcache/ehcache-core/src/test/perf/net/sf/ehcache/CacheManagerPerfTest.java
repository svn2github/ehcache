package net.sf.ehcache;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Snaps
 */
public class CacheManagerPerfTest {
    private static final Logger LOG = LoggerFactory.getLogger(CacheManagerPerfTest.class.getName());

    /**
     * the CacheManager Singleton instance
     */
    protected CacheManager singletonManager;

    /**
     * Tests that we can run 69 caches, most with disk stores, with no ill
     * effects
     * <p/>
     * Check that this is fast.
     */
    @Test
    public void testCreateCacheManagerWithManyCaches() throws CacheException,
            InterruptedException {
        singletonManager = CacheManager
                .create(AbstractCachePerfTest.TEST_CONFIG_DIR + "ehcache-big.xml");
        assertNotNull(singletonManager);
        assertEquals(69, singletonManager.getCacheNames().length);

        String[] names = singletonManager.getCacheNames();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            Ehcache cache = singletonManager.getCache(name);
            for (int j = 0; i < 100; i++) {
                cache.put(new Element(Integer.valueOf(j), "value"));
            }
        }
        StopWatch stopWatch = new StopWatch();
        for (int repeats = 0; repeats < 5000; repeats++) {
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                Ehcache cache = singletonManager.getCache(name);
                for (int j = 0; i < 100; i++) {
                    Element element = cache.get(name + j);
                    if ((element == null)) {
                        cache.put(new Element(Integer.valueOf(j), "value"));
                    }
                }
            }
        }
        long elapsedTime = stopWatch.getElapsedTime();
        LOG.info("Time taken was: " + elapsedTime);
        assertTrue("Time taken was: " + elapsedTime, elapsedTime < 5000);
    }
}
