/**
 *  Copyright Terracotta, Inc.
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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * Test cases for the {@link UpdatingSelfPopulatingCache}.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class UpdatingSelfPopulatingCacheTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CacheManager cacheManager;
    private Cache cache;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration();
        configuration.name("upSelfPopCM")
                .diskStore(new DiskStoreConfiguration().path(temporaryFolder.newFolder().getAbsolutePath()))
                .addCache(new CacheConfiguration("cache", 1).timeToIdleSeconds(2)
                        .timeToLiveSeconds(5)
                        .overflowToDisk(true)
                        .diskPersistent(true));
        cacheManager = CacheManager.newInstance(configuration);
        cache = cacheManager.getCache("cache");
    }

    @After
    public void tearDown() {
        cacheManager.shutdown();
    }

    /**
     * Tests fetching an entry, and then an update.
     * TODO FIXME: Was noticed breaking 30/8/10
     */
    @Test
    @Ignore
    public void testFetchAndUpdate() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        UpdatingSelfPopulatingCache selfPopulatingCache = new UpdatingSelfPopulatingCache(cache, factory);


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

        UpdatingSelfPopulatingCache selfPopulatingCache = new UpdatingSelfPopulatingCache(cache, factory);

        // Lookup
        try {
            selfPopulatingCache.get("key");
            fail();
        } catch (final Exception e) {
            Thread.sleep(20);

            // Check the error
            assertEquals("Could not update object for cache entry with key \"key\".", e.getMessage());
        }

    }

    /**
     * Tests refreshing the entries.
     */
    @Test
    public void testRefresh() throws Exception {
        final String value = "value";
        final CountingCacheEntryFactory factory = new CountingCacheEntryFactory(value);
        UpdatingSelfPopulatingCache selfPopulatingCache = new UpdatingSelfPopulatingCache(cache, factory);

        // Refresh
        try {
            selfPopulatingCache.refresh();
            fail();
        } catch (CacheException e) {
            //expected.
            assertEquals("UpdatingSelfPopulatingCache objects should not be refreshed.", e.getMessage());
        }

    }
}
