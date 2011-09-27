/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.extension;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CountingCacheEventListener;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class CacheExtensionTest {

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
        CountingCacheEventListener.resetCounters();
        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-cacheextension.xml");
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
     * Tests the put listener.
     */
    @Test
    public void testExtensionFromConfiguration() {

        assertEquals(Status.STATUS_ALIVE, TestCacheExtension.getStaticStatus());
        assertEquals("valueA", TestCacheExtension.getPropertyA());

        Cache cache = manager.getCache("testCacheExtensionCache");

        //Our cache extension should have populated the cache for this key
        assertNotNull(cache.get("key1"));

        manager.shutdown();
        assertEquals(Status.STATUS_SHUTDOWN, TestCacheExtension.getStaticStatus());

    }

    /**
     * Tests the put listener.
     */
    @Test
    public void testProgrammaticAdd() {

        manager.addCache("test");
        Cache cache = manager.getCache("test");
        TestCacheExtension testCacheExtension = new TestCacheExtension(cache, "valueA");
        assertEquals(Status.STATUS_UNINITIALISED, testCacheExtension.getStatus());
        assertEquals("valueA", testCacheExtension.getPropertyA());

        testCacheExtension.init();
        assertEquals(Status.STATUS_ALIVE, testCacheExtension.getStatus());

        cache.registerCacheExtension(testCacheExtension);
        manager.shutdown();
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
