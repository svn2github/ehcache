package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class BlockingCachePerfTest {

    @Rule
    public final TemporaryFolder diskFolder = new TemporaryFolder();
    
    private static final Logger LOG = LoggerFactory.getLogger(BlockingCachePerfTest.class.getName());

    /**
     * TODO: FIX ME!
     *
     * What exactly is the expected behavior here ? Right now, we get faster meantime, but the edge cases can be slower... issue ?
     *
     * Thrashes a BlockingCache and looks for liveness problems
     * Note. These timings are without logging. Turn logging off to run this test.
     */
    @Test
    public void testThrashBlockingCache() throws Exception {
        CacheManager manager = new CacheManager(new Configuration().name("testThrashBlockingCache")
               .diskStore(new DiskStoreConfiguration().path(diskFolder.getRoot().getAbsolutePath())));
        try {
            Ehcache cache = new Cache(new CacheConfiguration().name("test")
                    .maxEntriesLocalHeap(10000)
                    .maxEntriesLocalDisk(1000)
                    .timeToIdleSeconds(360)
                    .timeToLiveSeconds(1000)
                    .overflowToDisk(true));
            manager.addCache(cache);
            long duration = thrashCache(new BlockingCache(cache), 50, 1000L);
            LOG.debug("Thrash Duration:" + duration);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * TODO: FIX ME!
     *
     * This test doesn't reflect the new RRWL based locking. We should either refactor it (though I can't quite get what it is
     * trying to test or proof) or delete it
     *
     * Thrashes a BlockingCache which has a tiny timeout. Should throw
     * a LockTimeoutException caused by queued threads not getting the lock
     * in the required time.
     */
    @Test
    @Ignore
    public void testThrashBlockingCacheTinyTimeout() throws Exception {
        CacheManager manager = new CacheManager(new Configuration().name("testThrashBlockingCacheTinyTimeout")
               .diskStore(new DiskStoreConfiguration().path(diskFolder.getRoot().getAbsolutePath())));
        try {
            Ehcache cache = new Cache(new CacheConfiguration().name("test")
                    .maxEntriesLocalHeap(10000)
                    .maxEntriesLocalDisk(1000)
                    .timeToIdleSeconds(360)
                    .timeToLiveSeconds(1000)
                    .overflowToDisk(true));
            manager.addCache(cache);
            BlockingCache blockingCache = new BlockingCache(cache);
            blockingCache.setTimeoutMillis(1);
            long duration = 0;
            try {
                duration = thrashCache(blockingCache, 50, 100L);
                fail("Shouldn't have been able to acquire all locks in " + blockingCache.getTimeoutMillis() + " ms");
            } catch (Exception e) {
                //expected
            }
            LOG.debug("Thrash Duration:" + duration);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * Thrashes a BlockingCache which has a reasonable timeout. Should work.
     * The old implementation, which had scalability limits, needed 5, 1000L, 5000L to pass
     */
    @Test
    public void testThrashBlockingCacheReasonableTimeout() throws Exception {
        CacheManager manager = new CacheManager(new Configuration().name("testThrashBlockingCacheReasonableTimeout")
               .diskStore(new DiskStoreConfiguration().path(diskFolder.getRoot().getAbsolutePath())));
        try {
            Ehcache cache = new Cache(new CacheConfiguration().name("test")
                    .maxEntriesLocalHeap(10000)
                    .maxEntriesLocalDisk(1000)
                    .timeToIdleSeconds(360)
                    .timeToLiveSeconds(1000)
                    .overflowToDisk(true));
            manager.addCache(cache);
            BlockingCache blockingCache = new BlockingCache(cache);
            blockingCache.setTimeoutMillis((int) (400 * StopWatch.getSpeedAdjustmentFactor()));
            long duration = thrashCache(blockingCache, 50, (long) (1000L * StopWatch.getSpeedAdjustmentFactor()));
            LOG.debug("Thrash Duration:" + duration);
        } finally {
            manager.shutdown();
        }
    }

    /**
     * This method tries to get the cache to slow up.
     * It creates 300 threads, does blocking gets and monitors the liveness right the way through
     */
    private long thrashCache(final BlockingCache cache, final int numberOfThreads, final long retrievalTime) throws Exception {
        StopWatch stopWatch = new StopWatch();

        // Create threads that do gets
        final List executables = new ArrayList();
        for (int i = 0; i < numberOfThreads; i++) {
            final AbstractCacheTest.Executable executable = new AbstractCacheTest.Executable() {
                public void execute() throws Exception {
                    for (int i = 0; i < 10; i++) {
                        final String key = "key" + i;
                        Object value = cache.get(key);
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

        int failures = AbstractCacheTest.runThreadsNoCheck(executables, true);
        if (failures > 0) {

            throw new Exception("failures");
        }
        assertTrue("Failures: " + failures, failures <= 0);
        cache.removeAll();
        return stopWatch.getElapsedTime();
    }

    private void checkRetrievalOnKnownKey(BlockingCache cache, long requiredRetrievalTime, Serializable key)
            throws LockTimeoutException {
        StopWatch stopWatch = new StopWatch();
        cache.get(key);
        long measuredRetrievalTime = stopWatch.getElapsedTime();
        assertTrue("Retrieval time on known key is " + measuredRetrievalTime
                + " but should be less than " + requiredRetrievalTime + "ms",
                measuredRetrievalTime < requiredRetrievalTime);
    }
}
