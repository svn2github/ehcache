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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.sf.ehcache.CacheException;

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
     * Tests fetching an entry.
     */
    public void testFetch() throws Exception {
        final String value = "value";
        final CacheEntryFactory factory = new CountingCacheEntryFactory(value);
        final SelfPopulatingCache cache = new SelfPopulatingCache("sampleIdlingExpiringCache", factory);

        // Lookup
        final Object actualValue = cache.get("key");
        assertSame(value, actualValue);

        cache.clear();
    }

    /**
     * Tests fetching an unknown entry.
     */
    public void testFetchUnknown() throws Exception {
        final CacheEntryFactory factory = new CountingCacheEntryFactory(null);
        final SelfPopulatingCache cache = new SelfPopulatingCache("sampleIdlingExpiringCache", factory);

        // Lookup
        assertNull(cache.get("key"));

        cache.clear();
    }

    /**
     * Tests when fetch fails.
     */
    public void testFetchFail() throws Exception {
        final Exception exception = new Exception("Failed.");
        final CacheEntryFactory factory = new CacheEntryFactory() {
            public Serializable createEntry(final Serializable key) throws Exception {
                throw exception;
            }
        };
        final SelfPopulatingCache cache = new SelfPopulatingCache("sampleIdlingExpiringCache", factory);

        // Lookup
        try {
            cache.get("key");
            fail();
        } catch (final Exception e) {
            Thread.sleep(1);
            // Check the error
            assertEquals("Could not fetch object for cache entry \"key\".", e.getMessage());
        }
        cache.clear();
    }

    /**
     * Tests that an entry is created once only.
     */
    public void testCreateOnce() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        final SelfPopulatingCache cache = new SelfPopulatingCache("sampleIdlingExpiringCache", factory);

        // Fetch the value several times
        for (int i = 0; i < 5; i++) {
            assertSame(value, cache.get("key"));
            assertEquals(1, factory.getCount());
        }

        cache.clear();
    }

    /**
     * Tests refreshing the entries.
     */
    public void testRefresh() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        final SelfPopulatingCache cache = new SelfPopulatingCache("sampleIdlingExpiringCache", factory);

        // Check the value
        assertSame(value, cache.get("key"));
        assertEquals(1, factory.getCount());

        // Refresh
        cache.refresh();
        assertEquals(2, factory.getCount());

        // Check the value
        assertSame(value, cache.get("key"));
        assertEquals(2, factory.getCount());

        cache.clear();
    }

    /**
     * Tests that the current thread, which gets renamed when it enters a SelfPopulatingCache, comes out with
     * its old name.
     */
    public void testThreadNaming() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        final SelfPopulatingCache cache = new SelfPopulatingCache("sampleIdlingExpiringCache", factory);

        String originalThreadName = Thread.currentThread().getName();

        // Check the value
        cache.get("key");
        assertEquals(originalThreadName, Thread.currentThread().getName());

        // Refresh
        cache.refresh();
        assertEquals(originalThreadName, Thread.currentThread().getName());

        // Check the value with null key
        cache.get(null);
        assertEquals(originalThreadName, Thread.currentThread().getName());

        cache.clear();

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
        final SelfPopulatingCache cache = new SelfPopulatingCache("sampleIdlingExpiringCache", factory);

        cache.get("key1");
        cache.get("key2");
        assertEquals(2, getNumberOfKeys(cache));
        cache.refresh();
        assertEquals(2, getNumberOfKeys(cache));
        Thread.sleep(2001);


        //Will be two, because counting expired elements
        assertEquals(2, getNumberOfKeys(cache));

        // Check the cache
        cache.clear();
        assertEquals(0, getNumberOfKeys(cache));
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
            public Serializable createEntry(final Serializable key) throws Exception {
                Thread.sleep(200);
                return key;
            }
        };
        final SelfPopulatingCache cache = new SelfPopulatingCache("sampleIdlingExpiringCache", factory);
        cache.clear();
    }

    private int getNumberOfKeys(BlockingCache blockingCache) throws CacheException {
        Collection keys = blockingCache.getKeys();
        int size = keys.size();
        String list = new String();
        for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            Serializable serializable = (Serializable) iterator.next();
            list = list + " " + serializable;
        }
        LOG.debug("Keys: " + list);
        return size;
    }
}
