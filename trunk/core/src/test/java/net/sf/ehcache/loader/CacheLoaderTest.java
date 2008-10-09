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

package net.sf.ehcache.loader;


import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.extension.TestCacheExtension;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class CacheLoaderTest {

    /**
     * manager
     */
    protected CacheManager manager;


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-loaderinteractions.xml");
    }


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        if (!manager.getStatus().equals(Status.STATUS_SHUTDOWN)) {
            manager.shutdown();
        }
    }

    @Test
    public void testLoaderChainNullFirst() {
        Cache cache = manager.getCache("NullLoaderFirstCache");
        assertNotNull(cache.getWithLoader("key", null, null));
        NullCountingCacheLoader nullCountingCacheLoader = getNullCountingCacheLoader(cache);
        assertEquals(1, nullCountingCacheLoader.getLoadCounter());
        CountingCacheLoader countingCacheLoader = getCountingCacheLoader(cache);
        assertEquals(1, countingCacheLoader.getLoadCounter());
    }

    @Test
    public void testLoaderChainNullLast() {
        Cache cache = manager.getCache("NullLoaderLastCache");
        assertNotNull(cache.getWithLoader("key", null, null));
        CountingCacheLoader countingCacheLoader = getCountingCacheLoader(cache);
        assertEquals(1, countingCacheLoader.getLoadCounter());
        NullCountingCacheLoader nullCountingCacheLoader = getNullCountingCacheLoader(cache);
        assertEquals(0, nullCountingCacheLoader.getLoadCounter());
    }

    @Test
    public void testLoaderChainNullBoth() {
        Cache cache = manager.getCache("NullLoaderTwiceCache");
        Element element = cache.getWithLoader("key", null, null);
        assertNull(element);
        NullCountingCacheLoader nullCountingCacheLoader = getNullCountingCacheLoader(cache);
        List<CacheLoader> list = cache.getRegisteredCacheLoaders();
        assertEquals(2, list.size());
        for (CacheLoader cacheLoader : list) {
            if (cacheLoader instanceof NullCountingCacheLoader) {
                nullCountingCacheLoader = (NullCountingCacheLoader) cacheLoader;
                assertEquals(1, nullCountingCacheLoader.getLoadCounter());
            }
        }

    }

    private CountingCacheLoader getCountingCacheLoader(Cache cache) {
        List<CacheLoader> list = cache.getRegisteredCacheLoaders();
        for (CacheLoader cacheLoader : list) {
            if (cacheLoader instanceof CountingCacheLoader) {
                return (CountingCacheLoader) cacheLoader;
            }
        }
        return null;
    }

    private NullCountingCacheLoader getNullCountingCacheLoader(Cache cache) {
        List<CacheLoader> list = cache.getRegisteredCacheLoaders();
        for (CacheLoader cacheLoader : list) {
            if (cacheLoader instanceof NullCountingCacheLoader) {
                return (NullCountingCacheLoader) cacheLoader;
            }
        }
        return null;
    }


    /**
     * Tests the put listener.
     */
    @Test
    public void testExtensionDirectly() {

        manager.addCache("test");
        TestCacheExtension testCacheExtension = new TestCacheExtension(manager.getCache("test"), "valueA");
        assertEquals(Status.STATUS_UNINITIALISED, testCacheExtension.getStatus());
        assertEquals("valueA", testCacheExtension.getPropertyA());

        testCacheExtension.init();
        assertEquals(Status.STATUS_ALIVE, testCacheExtension.getStatus());

        testCacheExtension.dispose();
        assertEquals(Status.STATUS_SHUTDOWN, testCacheExtension.getStatus());

    }


    /**
     * We need to make sure that cloning a default cache results in a new cache with its own
     * set of cache extensions.
     */
    @Test
    public void testClone() {

        //just test it does not blow up
        manager.addCache("clonedCache");
    }
}