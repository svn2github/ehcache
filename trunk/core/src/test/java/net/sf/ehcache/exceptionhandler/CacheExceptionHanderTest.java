/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

package net.sf.ehcache.exceptionhandler;

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.event.CountingCacheEventListener;

/**
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 * todo doco
 * todo key in proxy
 * todo JCacheTest
 */
public class CacheExceptionHanderTest extends TestCase {

     /**
     * manager
     */
    protected CacheManager manager;
    /**
     * the cache name we wish to test
     */
    protected String cacheName = "testExceptionHandlingCache";
    /**
     * the cache we wish to test                                                                
     */
    protected Ehcache cache;

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        CountingCacheEventListener.resetCounters();
        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache.xml");
        cache = manager.getCache(cacheName);
        cache.removeAll();
    }


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        CountingCacheEventListener.resetCounters();
        manager.shutdown();
    }


    /**
     * let's get started
     */
    public void testCacheExceptionHandler() {
        Ehcache proxiedCache = ExceptionHandlingDynamicCacheProxy.createProxy(cache);

        //Would normally throw an IllegalArgumentException
        proxiedCache.put(null);

    }




}
