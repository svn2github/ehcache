package net.sf.ehcache;

import org.junit.After;
import org.junit.Before;
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
public class ConcurrencyProblemCachePerfTest extends AbstractCachePerfTest {

    private static final Logger LOG = LoggerFactory.getLogger(ConcurrencyProblemCachePerfTest.class.getName());

    private Cache cache;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * teardown
     */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testContinuousThrashConfiguration() throws Exception {
        cache = manager.getCache("sampleIdlingExpiringCache");
        for (int i = 0; i < 5; i++) {
            thrashCache(cache, 1500L);
            LOG.info("Finished run.");
        }
    }

    @Test
    public void testContinuousThrashProgrammatic() throws Exception {
        cache = new Cache("thrashcache", 5, false, false, 2, 5);
        manager.addCache(cache);
        for (int i = 0; i < 5; i++) {
            thrashCache(cache, 1500L);
            LOG.info("Finished run.");

        }
    }

    /**
     * This method tries to get the cache to slow up.
     * It creates 10 threads, does gets and puts.
     */
    private long thrashCache(final Cache cache, final long retrievalTime)
            throws Exception {
        StopWatch stopWatch = new StopWatch();

        // Create threads that do gets
        final List executables = new ArrayList();
        for (int i = 0; i < 10; i++) {
            final Executable executable = new Executable() {
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

        runThreads(executables);
        cache.removeAll();
        return stopWatch.getElapsedTime();
    }


    /**
     * Checks that the liveness method returns in less than a given amount of time.
     * liveness() is a method that simply returns a String. It should be very fast. It can be
     * delayed because it is a synchronized method, and must acquire
     * an object lock before continuing. The old blocking cache was taking up to several minutes in production
     *
     * @param cache a BlockingCache
     */
    private void checkRetrievalOnKnownKey(Cache cache, long requiredRetrievalTime, Serializable key) {
        StopWatch stopWatch = new StopWatch();
        cache.get(key);
        long measuredRetrievalTime = stopWatch.getElapsedTime();
        assertTrue("Retrieval time on known key is " + measuredRetrievalTime
                + " but should be less than " + requiredRetrievalTime + "ms",
                measuredRetrievalTime < requiredRetrievalTime);
    }
}

