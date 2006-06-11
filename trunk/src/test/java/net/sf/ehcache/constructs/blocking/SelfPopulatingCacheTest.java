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

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test cases for the {@link SelfPopulatingCache}.
 *
 * @version $Id$
 * @author Adam Murdoch
 * @author Greg Luck
 */
public class SelfPopulatingCacheTest extends TestCase {
    private static final Log LOG = LogFactory.getLog(SelfPopulatingCache.class.getName());

    /**
     * Shared with subclass
     */
    protected CacheManager manager;
    /**
     * Shared with subclass
     */
    protected SelfPopulatingCache selfPopulatingCache;
    /**
     * Shared with subclass
     */
    protected Ehcache cache;

    /**
     * Load up the test cache
     */
    protected void setUp() throws Exception {
        super.setUp();
        manager = new CacheManager();
        cache = manager.getCache("sampleIdlingExpiringCache");
        selfPopulatingCache = new SelfPopulatingCache(cache, new CountingCacheEntryFactory("value"));
    }

    /**
     * teardown
     */
    protected void tearDown() throws Exception {
        selfPopulatingCache.removeAll();
        manager.shutdown();
        super.tearDown();
    }

    /**
     * Tests fetching an entry.
     */
    public void testFetch() throws Exception {

        // Lookup
        final Element element = selfPopulatingCache.get("key");
        assertEquals("value", element.getValue());
    }

    /**
     * Tests fetching an unknown entry.
     */
    public void testFetchUnknown() throws Exception {
        final CacheEntryFactory factory = new CountingCacheEntryFactory(null);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        // Lookup
        assertNull(cache.get("key"));
    }

    /**
     * Tests when fetch fails.
     */
    public void testFetchFail() throws Exception {
        final Exception exception = new Exception("Failed.");
        final CacheEntryFactory factory = new CacheEntryFactory() {
            public Object createEntry(final Object key) throws Exception {
                throw exception;
            }
        };
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        // Lookup
        try {
            selfPopulatingCache.get("key");
            fail();
        } catch (final Exception e) {
            Thread.sleep(1);
            // Check the error
            assertEquals("Could not fetch object for cache entry \"key\".", e.getMessage());
        }
    }

    /**
     * Tests that an entry is created once only.
     */
    public void testCreateOnce() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        // Fetch the value several times
        for (int i = 0; i < 5; i++) {
            assertSame(value, selfPopulatingCache.get("key").getObjectValue());
            assertEquals(1, factory.getCount());
        }
    }

    /**
     * Tests refreshing the entries.
     */
    public void testRefresh() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        // Check the value
        assertSame(value, selfPopulatingCache.get("key").getObjectValue());
        assertEquals(1, factory.getCount());

        // Refresh
        selfPopulatingCache.refresh();
        assertEquals(2, factory.getCount());

        // Check the value
        assertSame(value, selfPopulatingCache.get("key").getObjectValue());
        assertEquals(2, factory.getCount());

    }

    /**
     * Tests that the current thread, which gets renamed when it enters a SelfPopulatingCache, comes out with
     * its old name.
     */
    public void testThreadNaming() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);

        String originalThreadName = Thread.currentThread().getName();

        // Check the value
        selfPopulatingCache.get("key");
        assertEquals(originalThreadName, Thread.currentThread().getName());

        // Refresh
        selfPopulatingCache.refresh();
        assertEquals(originalThreadName, Thread.currentThread().getName());

        // Check the value with null key
        selfPopulatingCache.get(null);
        assertEquals(originalThreadName, Thread.currentThread().getName());


    }

    /**
     * Tests discarding little used entries.
     * <cache name="sampleIdlingExpiringCache"
     *   maxElementsInMemory="1"
     *   eternal="false"
     *   timeToIdleSeconds="2"
     *   timeToLiveSeconds="5"
     *   overflowToDisk="true"
     *   />
     */
    public void testDiscardLittleUsed() throws Exception {
        final CacheEntryFactory factory = new CountingCacheEntryFactory("value");
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);


        selfPopulatingCache.get("key1");
        selfPopulatingCache.get("key2");
        assertEquals(2, selfPopulatingCache.getSize());
        selfPopulatingCache.refresh();
        assertEquals(2, selfPopulatingCache.getSize());
        Thread.sleep(2001);


        //Will be two, because counting expired elements
        assertEquals(2, selfPopulatingCache.getSize());

        // Check the cache
        selfPopulatingCache.removeAll();
        assertEquals(0, selfPopulatingCache.getSize());
    }

    /**
     * Tests discarding little used entries, where refreshing is slow.
     *  <cache name="sampleIdlingExpiringCache"
     *  maxElementsInMemory="1"
     *  eternal="false"
     *  timeToIdleSeconds="2"
     *  timeToLiveSeconds="5"
     *  overflowToDisk="true"
     *  />
     */
    public void testDiscardLittleUsedSlow() throws Exception {
        final CacheEntryFactory factory = new CacheEntryFactory() {
            public Object createEntry(final Object key) throws Exception {
                Thread.sleep(200);
                return key;
            }
        };
        selfPopulatingCache = new SelfPopulatingCache(cache, factory);
    }

}
