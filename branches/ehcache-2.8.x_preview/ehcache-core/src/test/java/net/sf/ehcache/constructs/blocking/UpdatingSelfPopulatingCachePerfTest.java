package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Snaps
 */
public class UpdatingSelfPopulatingCachePerfTest {

    @Rule
    public final TemporaryFolder diskFolder = new TemporaryFolder();
    
    private static final Logger LOG = LoggerFactory.getLogger(UpdatingSelfPopulatingCachePerfTest.class.getName());

    /**
     * Thrashes a UpdatingSelfPopulatingCache and looks for liveness problems
     * Note. These timings are without logging. Turn logging off to run this test.
     * <p/>
     * To get this test to fail, add the synchronized keyword to {@link UpdatingSelfPopulatingCache#get(java.io.Serializable)}.
     */
    @Test
    public void testThrashUpdatingSelfPopulatingCache() throws Exception {
        CacheManager manager = new CacheManager(new Configuration().name("testThrashUpdatingSelfPopulatingCache")
               .diskStore(new DiskStoreConfiguration().path(diskFolder.getRoot().getAbsolutePath())));
        try {
            Ehcache cache = new Cache(new CacheConfiguration().name("test")
                    .maxEntriesLocalHeap(1)
                    .timeToIdleSeconds(2)
                    .timeToLiveSeconds(5)
                    .overflowToDisk(true)
                    .diskPersistent(true));
            manager.addCache(cache);

            final String value = "value";
            final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
            SelfPopulatingCache selfPopulatingCache = new UpdatingSelfPopulatingCache(cache, factory);
            long duration = thrashCache((UpdatingSelfPopulatingCache) selfPopulatingCache, 300L, 1500L);
            LOG.debug("Thrash Duration:" + duration);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * This method tries to get the cache to slow up.
     * It creates 40 threads, does blocking gets and monitors the liveness right the way through
     */
    private long thrashCache(final UpdatingSelfPopulatingCache cache, final long liveness, final long retrievalTime)
            throws Exception {
        StopWatch stopWatch = new StopWatch();

        // Create threads that do gets
        final List executables = new ArrayList();
        for (int i = 0; i < 10; i++) {
            final AbstractCacheTest.Executable executable = new AbstractCacheTest.Executable() {
                public void execute() throws Exception {
                    for (int i = 0; i < 10; i++) {
                        final String key = "key" + i;
                        Object value = cache.get(key);
                        checkLiveness(cache, liveness);
                        if (value == null) {
                            cache.put(new Element(key, "value" + i));
                        }
                        //The key will be in. Now check we can get it quickly
                        checkRetrievalOnKnownKey(cache, retrievalTime, key);
                    }
                }
            };
            executables.add(executable);
        }

        AbstractCacheTest.runThreads(executables);
        cache.removeAll();
        return stopWatch.getElapsedTime();
    }

    /**
     * Checks that the liveness method returns in less than a given amount of time.
     * liveness() is a method that simply returns a String. It should be very fast. It can be
     * delayed because it is a synchronized method, and must acquire an object lock before continuing
     * The old blocking cache was taking up to several minutes in production
     *
     * @param cache a BlockingCache
     */
    private void checkLiveness(UpdatingSelfPopulatingCache cache, long liveness) {
        StopWatch stopWatch = new StopWatch();
        cache.liveness();
        long measuredLiveness = stopWatch.getElapsedTime();
        assertTrue("liveness is " + measuredLiveness + " but should be less than " + liveness + "ms",
                measuredLiveness < liveness);
    }

    /**
     * Checks that the liveness method returns in less than a given amount of time.
     * liveness() is a method that simply returns a String. It should be very fast. It can be
     * delayed because it is a synchronized method, and must acquire
     * an object lock before continuing. The old blocking cache was taking up to several minutes in production
     *
     * @param cache a BlockingCache
     */
    private void checkRetrievalOnKnownKey(UpdatingSelfPopulatingCache cache, long requiredRetrievalTime, Serializable key)
            throws LockTimeoutException {
        StopWatch stopWatch = new StopWatch();
        cache.get(key);
        long measuredRetrievalTime = stopWatch.getElapsedTime();
        assertTrue("Retrieval time on known key is " + measuredRetrievalTime
                + " but should be less than " + requiredRetrievalTime + "ms",
                measuredRetrievalTime < requiredRetrievalTime);
    }

}
