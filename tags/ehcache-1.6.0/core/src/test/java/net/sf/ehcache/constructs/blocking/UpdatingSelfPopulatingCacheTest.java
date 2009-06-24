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

package net.sf.ehcache.constructs.blocking;

import static junit.framework.Assert.assertSame;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.StopWatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Test cases for the {@link UpdatingSelfPopulatingCache}.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class UpdatingSelfPopulatingCacheTest extends SelfPopulatingCacheTest {

    private static final Logger LOG = Logger.getLogger(UpdatingSelfPopulatingCacheTest.class.getName());

    /**
     * Tests fetching an entry, and then an update.
     */
    @Test
    public void testFetchAndUpdate() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        selfPopulatingCache = new UpdatingSelfPopulatingCache(cache, factory);


        //test null
        Element element = selfPopulatingCache.get(null);

        // Lookup
        element = selfPopulatingCache.get("key");
        assertSame(value, element.getObjectValue());
        assertEquals(2, factory.getCount());

        Object actualValue = selfPopulatingCache.get("key").getObjectValue();
        assertSame(value, actualValue);
        assertEquals(3, factory.getCount());

        actualValue = selfPopulatingCache.get("key").getObjectValue();
        assertSame(value, actualValue);
        assertEquals(4, factory.getCount());
    }

    /**
     * Tests when fetch fails.
     */
    @Test
    public void testFetchFail() throws Exception {
        final Exception exception = new Exception("Failed.");
        final UpdatingCacheEntryFactory factory = new UpdatingCacheEntryFactory() {
            public Object createEntry(final Object key)
                    throws Exception {
                throw exception;
            }

            public void updateEntryValue(Object key, Object value)
                    throws Exception {
                throw exception;
            }
        };

        selfPopulatingCache = new UpdatingSelfPopulatingCache(cache, factory);

        // Lookup
        try {
            selfPopulatingCache.get("key");
            fail();
        } catch (final Exception e) {
            Thread.sleep(20);

            // Check the error
            assertEquals("Could not fetch object for cache entry with key \"key\".", e.getMessage());
        }

    }

    /**
     * Tests refreshing the entries.
     */
    @Test
    public void testRefresh() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        selfPopulatingCache = new UpdatingSelfPopulatingCache(cache, factory);

        // Refresh
        try {
            selfPopulatingCache.refresh();
            fail();
        } catch (CacheException e) {
            //expected.
            assertEquals("UpdatingSelfPopulatingCache objects should not be refreshed.", e.getMessage());
        }

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
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        selfPopulatingCache = new UpdatingSelfPopulatingCache(cache, factory);
        long duration = thrashCache((UpdatingSelfPopulatingCache) selfPopulatingCache, 300L, 1500L);
        LOG.log(Level.FINE, "Thrash Duration:" + duration);
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
            final UpdatingSelfPopulatingCacheTest.Executable executable = new UpdatingSelfPopulatingCacheTest.Executable() {
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
     * Tests the async load with a single item
     */
    @Override
    @Test
    public void testAsynchronousLoad() throws InterruptedException, ExecutionException {
        super.testAsynchronousLoad();
    }
}
