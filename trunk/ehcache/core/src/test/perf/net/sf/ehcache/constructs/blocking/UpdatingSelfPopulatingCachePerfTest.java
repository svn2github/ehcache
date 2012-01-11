package net.sf.ehcache.constructs.blocking;

import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Alex Snaps
 */
public class UpdatingSelfPopulatingCachePerfTest extends SelfPopulatingCachePerfTest {

    private static final Logger LOG = LoggerFactory.getLogger(UpdatingSelfPopulatingCachePerfTest.class.getName());

    /**
     * When flushing large MemoryStores, OutOfMemory issues can happen if we are
     * not careful to move each to Element to the DiskStore, rather than copy them all
     * and then delete them from the MemoryStore.
     * <p/>
     * This test manipulates a MemoryStore right on the edge of what can fit into the 64MB standard VM size.
     * An inefficient spool will cause an OutOfMemoryException.
     *
     * @throws Exception
     */
    @Test
    public void testMemoryEfficiencyOfFlushWhenOverflowToDisk() throws Exception {
        super.testMemoryEfficiencyOfFlushWhenOverflowToDisk();
    }

        /**
     * Thrashes a UpdatingSelfPopulatingCache and looks for liveness problems
     * Note. These timings are without logging. Turn logging off to run this test.
     * <p/>
     * To get this test to fail, add the synchronized keyword to {@link UpdatingSelfPopulatingCache#get(java.io.Serializable)}.
     */
    @Test
    public void testThrashUpdatingSelfPopulatingCache() throws Exception {
        final String value = "value";
        final CountingCachePerfEntryFactory factory = new CountingCachePerfEntryFactory(value);
        selfPopulatingCache = new UpdatingSelfPopulatingCache(cache, factory);
        long duration = thrashCache((UpdatingSelfPopulatingCache) selfPopulatingCache, 300L, 1500L);
        LOG.debug("Thrash Duration:" + duration);
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
            final UpdatingSelfPopulatingCachePerfTest.Executable executable = new UpdatingSelfPopulatingCachePerfTest.Executable() {
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

        runThreads(executables);
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
