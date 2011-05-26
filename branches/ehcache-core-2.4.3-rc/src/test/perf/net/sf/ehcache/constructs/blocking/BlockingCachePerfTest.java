package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.AbstractCachePerfTest;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.StopWatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Alex Snaps
 */
public class BlockingCachePerfTest extends AbstractCachePerfTest {

    private static final Logger LOG = LoggerFactory.getLogger(BlockingCachePerfTest.class.getName());

    private BlockingCache blockingCache;

    /**
     * Load up the test cache
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Ehcache cache = manager.getCache("sampleIdlingExpiringCache");
        blockingCache = new BlockingCache(cache);
    }

    /**
     * teardown
     */
    @Override
    @After
    public void tearDown() throws Exception {
        if (manager.getStatus() == Status.STATUS_ALIVE) {
            blockingCache.removeAll();
        }
        super.tearDown();
    }

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
        Ehcache cache = manager.getCache("sampleCache1");
        blockingCache = new BlockingCache(cache);
        long duration = thrashCache(blockingCache, 50, 500L, 1000L);
        LOG.debug("Thrash Duration:" + duration);
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
    public void testThrashBlockingCacheTinyTimeout() throws Exception {
        Ehcache cache = manager.getCache("sampleCache1");
        blockingCache = new BlockingCache(cache);
        blockingCache.setTimeoutMillis(1);
        long duration = 0;
        try {
            duration = thrashCache(blockingCache, 50, 400L, 100L);
            fail("Shouldn't have been able to acquire all locks in " + blockingCache.getTimeoutMillis() + " ms");
        } catch (Exception e) {
            //expected
        }
        LOG.debug("Thrash Duration:" + duration);
    }

    /**
     * Thrashes a BlockingCache which has a reasonable timeout. Should work.
     * The old implementation, which had scalability limits, needed 5, 1000L, 5000L to pass
     */
    @Test
    public void testThrashBlockingCacheReasonableTimeout() throws Exception {
        Ehcache cache = manager.getCache("sampleCache1");
        blockingCache = new BlockingCache(cache);
        blockingCache.setTimeoutMillis((int) (400 * StopWatch.getSpeedAdjustmentFactor()));
        long duration = thrashCache(blockingCache, 50, 400L, (long) (1000L * StopWatch.getSpeedAdjustmentFactor()));
        LOG.debug("Thrash Duration:" + duration);
    }

    /**
     * This method tries to get the cache to slow up.
     * It creates 300 threads, does blocking gets and monitors the liveness right the way through
     */
    private long thrashCache(final BlockingCache cache, final int numberOfThreads,
                             final long liveness, final long retrievalTime) throws Exception {
        StopWatch stopWatch = new StopWatch();

        // Create threads that do gets
        final List executables = new ArrayList();
        for (int i = 0; i < numberOfThreads; i++) {
            final AbstractCachePerfTest.Executable executable = new AbstractCachePerfTest.Executable() {
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

        int failures = runThreadsNoCheck(executables, true);
        if (failures > 0) {

            throw new Exception("failures");
        }
        assertTrue("Failures: " + failures, failures <= 0);
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
    private void checkLiveness(BlockingCache cache, long liveness) {
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
