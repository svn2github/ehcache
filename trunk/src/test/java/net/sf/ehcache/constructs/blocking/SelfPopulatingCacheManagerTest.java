/**
 *  Copyright 2003-2006 Greg Luck
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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.AbstractCacheTest;

import java.util.ArrayList;

/**
 * Test cases
 *
 * @author Greg Luck
 * @version $Id$
 */
public class SelfPopulatingCacheManagerTest extends AbstractCacheTest {
    private TestSelfPopulatingCacheManager manager;
    private SelfPopulatingCache selfPopulatingCache;

    /**
     * Load up the test cache
     */
    protected void setUp() throws Exception {
        super.setUp();
        manager = new TestSelfPopulatingCacheManager();

    }

    /**
     * teardown
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Checks that the TestSelfPopulatingCacheManager can access a cache known to {@link net.sf.ehcache.CacheManager}
     *
     * @throws net.sf.ehcache.CacheException
     */
    public void testCreateCacheKnownToExist() throws CacheException {
        selfPopulatingCache = manager.getSelfPopulatingCache("sampleIdlingExpiringCache");
        assertEquals("sampleIdlingExpiringCache", selfPopulatingCache.getName());
    }

    /**
     * Checks that the TestSelfPopulatingCacheManager cannot access a cache not known to {@link net.sf.ehcache.CacheManager}
     */
    public void testCreateCacheKnownNotToExist() {
        try {
            selfPopulatingCache = manager.getSelfPopulatingCache("sampleIdlingExpiringCacheDoesNotExist");
            fail();
        } catch (CacheException e) {
            assertEquals("Cache sampleIdlingExpiringCacheDoesNotExist cannot be retrieved. "
                    + "Please check ehcache.xml", e.getMessage());
        }
    }

    /**
     * A cache manager for self populating caches
     *
     * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
     * @version $Id$
     */
    private class TestSelfPopulatingCacheManager extends SelfPopulatingCacheManager {


        /**
         * Constructor. Caches are set up here.
         */
        public TestSelfPopulatingCacheManager() throws CacheException {
            super();
        }

        /**
         * A template method to set up caches. It is wrapped by {@link #setupCaches}
         * to ensure that caches are created within a synchronized method.
         * <p/>
         * Implementations of this method should typically call {@link #createSelfPopulatingCache(String, CacheEntryFactory)},
         * or {@link SelfPopulatingCacheManager#createUpdatingSelfPopulatingCache(String, UpdatingCacheEntryFactory)}
         * for each required cache.
         */
        protected void doSetupCaches() throws CacheException {

            CountingCacheEntryFactory countingCacheEntryFactory = new CountingCacheEntryFactory("value");
            //Create based on a configured cache
            createSelfPopulatingCache("sampleIdlingExpiringCache", countingCacheEntryFactory);

            ArrayList list = new ArrayList();
            list.add("value");
            CountingCollectionsCacheEntryFactory countingCollectionsCacheEntryFactory =
                    new CountingCollectionsCacheEntryFactory(list);
            //Create based on a configured cache
            createUpdatingSelfPopulatingCache("sampleCache1", countingCollectionsCacheEntryFactory);


        }
    }

}

