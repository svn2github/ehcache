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

package net.sf.ehcache.event;


import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;

import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

/**
 * Uses a counting listener to make sure all the notifications came through
 *
 * @author Greg Luck
 * @version $Id$
 */
@Category(CheckShorts.class)
public class CacheManagerEventListenerTest {

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        CountingCacheManagerEventListener.resetCounters();
    }


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        CountingCacheManagerEventListener.resetCounters();
    }


    /**
     * Tests that we can set the listener through configuration, and that it gets notified of all events.
     */
    @Test
    public void testListenerSpecifiedInConfigurationFile() throws CacheException {
        CacheManager manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-countinglisteners.xml");
        assertNotNull(manager);
        assertEquals(10, manager.getCacheNames().length);
        assertEquals(10, CountingCacheManagerEventListener.getCacheNamesAdded().size());


        String[] cacheNames = manager.getCacheNames();
        for (int i = 0; i < cacheNames.length; i++) {
            String cacheName = cacheNames[i];
            manager.removeCache(cacheName);
            assertEquals(i + 1, CountingCacheManagerEventListener.getCacheNamesRemoved().size());
        }

        manager.shutdown();
    }


    /**
     * Tests we can programmatically set the listener, and that it gets notified of all events.
     */
    @Test
    public void testListenerSpecifiedProgrammatically() throws CacheException {
        CacheConfiguration defaultCache = new CacheConfiguration("cache", 10);

        CountingCacheManagerEventListener countingCacheManagerEventListener = new CountingCacheManagerEventListener();

        CacheManager manager = new CacheManager();
        manager.removeAllCaches();
        manager.getCacheManagerEventListenerRegistry().registerListener(countingCacheManagerEventListener);

        for (int i = 0; i < 10; i++) {
            manager.addCache("" + i);
        }
        //make sure that the listener works with Ehcache interface, not just Cache
        manager.replaceCacheWithDecoratedCache(manager.getCache("9"), new SelfPopulatingCache(manager.getCache("9"), null));

        assertNotNull(manager);
        assertEquals(10, manager.getCacheNames().length);
        assertEquals(10, CountingCacheManagerEventListener.getCacheNamesAdded().size());

        for (int i = 0; i < 10; i++) {
            String cacheName = (String) CountingCacheManagerEventListener.getCacheNamesAdded().get(i);
            manager.removeCache(cacheName);
            assertEquals(i + 1, CountingCacheManagerEventListener.getCacheNamesRemoved().size());

        }
        manager.shutdown();
    }


}
