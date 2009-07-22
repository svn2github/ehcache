/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package net.sf.ehcache;

import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Demonstrates a problem with Ben Manes' ConcurrentLinkedHashmap. This was used in beta4 but was found not to be
 * threadsafe. After a few days of work isolating the issue to ConcurrentLinkedHashmap, this test pinpoints the problem
 * in about 30 seconds.
 *
 * Though ConcurrentLinkedHashmap is no longer used, leave this test here so that we can retest new versions that
 * may come out.
 *
 * @author Greg Luck
 */
public class ConcurrencyProblemCacheTest extends AbstractCacheTest {

    private static final Logger LOG = Logger.getLogger(ConcurrencyProblemCacheTest.class.getName());

    private CacheManager manager;

    private Cache cache;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        manager = new CacheManager();
    }

    /**
     * teardown
     */
    @After
    public void tearDown() throws Exception {
        manager.shutdown();
        super.tearDown();
    }

    @Test
    public void testContinuousThrashConfiguration() throws Exception {
        cache = manager.getCache("sampleIdlingExpiringCache");
        for (int i = 0; i < 5; i++) {
            thrashCache(cache, 1500L);
            LOG.log(Level.INFO, "Finished run.");
        }
    }

    @Test
    public void testContinuousThrashProgrammatic() throws Exception {
        cache = new Cache("thrashcache", 5, false, false, 2, 5);
        manager.addCache(cache);
        for (int i = 0; i < 5; i++) {
            thrashCache(cache, 1500L);
            LOG.log(Level.INFO, "Finished run.");

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
