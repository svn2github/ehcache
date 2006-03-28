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

package net.sf.ehcache.event;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.AbstractCacheTest;

/**
 * Same as {@link CacheEventListenerTest} except that the listener is set programmatically. This test inherits because
 * all of the tests should behave identically.
 *
 * @author Greg Luck
 * @version $Id: ProgrammaticallyCreatedCacheEventListenerTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class ProgrammaticallyCreatedCacheEventListenerTest extends CacheEventListenerTest {
    private CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();

    /**
     * {@inheritDoc}
     * @throws Exception
     */
    protected void setUp() throws Exception {
        CountingCacheEventListener.resetCounters();
        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-nolisteners.xml");
        cache = manager.getCache(cacheName);
        cache.removeAll();
        //this call can be repeated. Attempts to further register the listener are ignored.
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
    }

    /**
     * An instance that <code>equals</code> one already registered is ignored
     */
    public void testAttemptDoubleRegistrationOfSameInstance() {
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        //should just be the one from setUp
        assertEquals(1, cache.getCacheEventNotificationService().getCacheEventListeners().size());
    }

    /**
     * An new instance of the same class will be registered
     */
    public void testAttemptDoubleRegistrationOfSeparateInstance() {
        cache.getCacheEventNotificationService().registerListener(new CountingCacheEventListener());
        //should just be the one from setUp
        assertEquals(2, cache.getCacheEventNotificationService().getCacheEventListeners().size());
    }






}
