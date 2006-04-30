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

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheException;

/**
 * Test cases for the {@link BlockingCache}.
 *
 * @author Adam Murdoch
 * @author Greg Luck
 * @version $Id$
 */
public class BlockingCacheManagerTest extends AbstractCacheTest {
    private BlockingCacheManager manager;
    private BlockingCache blockingCache;

    /**
     * Load up the test cache
     */
    protected void setUp() throws Exception {
        super.setUp();
        manager = new BlockingCacheManager();

    }

    /**
     * teardown
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Checks that the BlockingCacheManager can access a cache known to {@link net.sf.ehcache.CacheManager}
     * @throws CacheException
     */
    public void testCreateCacheKnownToExist() throws CacheException {
        blockingCache = manager.getCache("sampleIdlingExpiringCache");
        assertEquals("sampleIdlingExpiringCache", blockingCache.getName());
    }

    /**
     * Checks that the BlockingCacheManager cannot access a cache not known to {@link net.sf.ehcache.CacheManager}
     */
    public void testCreateCacheKnownNotToExist() {
        try {
            blockingCache = manager.getCache("sampleIdlingExpiringCacheDoesNotExist");
            fail();
        } catch (CacheException e) {
            assertEquals("Cache sampleIdlingExpiringCacheDoesNotExist cannot be retrieved. "
                    + "Please check ehcache.xml", e.getMessage());
        }
    }

}

