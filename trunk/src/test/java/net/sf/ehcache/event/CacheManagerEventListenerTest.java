/**
 *  Copyright 2003-2007 Greg Luck
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

import junit.framework.TestCase;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;

/**
 * Uses a counting listener to make sure all the notifications came through
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CacheManagerEventListenerTest extends TestCase {

    /**
     * {@inheritDoc}
     * @throws Exception
     */
    protected void setUp() throws Exception {
        CountingCacheManagerEventListener.resetCounters();
    }


    /**
     * {@inheritDoc}
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        CountingCacheManagerEventListener.resetCounters();
    }


    /**
     * Tests that we can set the listener through configuration, and that it gets notified of all events.
     */
    public void testListenerSpecifiedInConfigurationFile() throws CacheException {
        CacheManager manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-countinglisteners.xml");
        assertNotNull(manager);
        assertEquals(10, manager.getCacheNames().length);
        //We do not notify initial config (as of 1,3)
        assertEquals(0, CountingCacheManagerEventListener.getCacheNamesAdded().size());


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
    public void testListenerSpecifiedProgrammatically() throws CacheException {
        CacheConfiguration defaultCache = new CacheConfiguration();
        defaultCache.setEternal(false);
        defaultCache.setMaxElementsInMemory(10);

        CountingCacheManagerEventListener countingCacheManagerEventListener = new CountingCacheManagerEventListener();

        CacheManager manager = new CacheManager();
        manager.removalAll();
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
