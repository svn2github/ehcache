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

package net.sf.ehcache.jcache;

import junit.framework.TestCase;

import javax.cache.CacheManager;
import javax.cache.CacheFactory;
import javax.cache.CacheException;

/**
 * @author Greg Luck
 * @version $Id$
 */
public class CacheManagerTest extends TestCase {


    /**
     * Tests the constructors.
     *
     * The factory method and new return different instances.
     *
     * getInstance always returns the same instance
     */
    public void testCacheManagerConstructor() {
        CacheManager cacheManager = new CacheManager();
        CacheManager cacheManager2 = CacheManager.getInstance();
        CacheManager cacheManager3 = CacheManager.getInstance();
        assertTrue(cacheManager != cacheManager2);
        assertTrue(cacheManager2 == cacheManager3);
    }


    /**
     * Funky. This seems to presuppose a whole infrastrucutre.
     * @throws CacheException
     */
    public void testCacheFactory() throws CacheException {

        CacheManager cacheManager = new CacheManager();
        try {
            CacheFactory cacheFactory = cacheManager.getCacheFactory();
            fail();
        } catch (CacheException e) {
            assertEquals("No implementation of JCache can be found on the classpath", e.getMessage());
        }


    }
}
