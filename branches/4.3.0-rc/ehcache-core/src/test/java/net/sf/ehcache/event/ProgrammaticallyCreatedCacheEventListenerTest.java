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


import net.sf.ehcache.CacheManager;

import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

/**
 * Same as {@link CacheEventListenerTest} except that the listener is set programmatically. This test inherits because
 * all of the tests should behave identically.
 *
 * @author Greg Luck
 * @version $Id$
 */
@Category(CheckShorts.class)
public class ProgrammaticallyCreatedCacheEventListenerTest extends CacheEventListenerTest {

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        manager = CacheManager.create(getClass().getResourceAsStream("/ehcache-nolisteners.xml"));
        cache = manager.getCache(cacheName);
        cache.removeAll();
        //this call can be repeated. Attempts to further register the listener are ignored.
        cache.getCacheEventNotificationService().registerListener(new CountingCacheEventListener());
    }

}
