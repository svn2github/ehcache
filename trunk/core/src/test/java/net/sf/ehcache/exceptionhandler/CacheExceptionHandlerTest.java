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

package net.sf.ehcache.exceptionhandler;

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.loader.ExceptionThrowingLoader;
import net.sf.ehcache.event.CountingCacheEventListener;

/**
 * @author <a href="mailto:gluck@gregluck.com">Greg Luck</a>
 * @version $Id$
 */
public class CacheExceptionHandlerTest extends TestCase {

    /**
     * manager
     */
    protected CacheManager manager;
    /**
     * the cache name we wish to test
     */
    protected String cacheName = "exceptionHandlingCache";
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
        cache = manager.getEhcache(cacheName);
        cache.removeAll();
    }


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        CountingExceptionHandler.resetCounters();
        manager.shutdown();
    }

    /**
     * Test a cache which has been configured to have a CountingExceptionHandler configured
     */
    public void testConfiguredCache() {
        manager.removeCache("exceptionHandlingCache");
        //Would normally throw an IllegalStateException
        cache.get("key1");

        assertEquals(1, CountingExceptionHandler.HANDLED_EXCEPTIONS.size());
        assertEquals(null, ((CountingExceptionHandler.HandledException) CountingExceptionHandler.HANDLED_EXCEPTIONS.get(0)).getKey());
        assertEquals(IllegalStateException.class, ((CountingExceptionHandler.HandledException) CountingExceptionHandler.HANDLED_EXCEPTIONS
                .get(0)).getException().getClass());
    }

    /**
     * Test a cache which has been configured to have an ExceptionThrowingLoader screw up loading.
     * This one should have a key set.
     */
    public void testKeyWithConfiguredCache() {

        cache.setCacheLoader(new ExceptionThrowingLoader());
        cache.getWithLoader("key1", null, null);

        assertEquals(1, CountingExceptionHandler.HANDLED_EXCEPTIONS.size());
        assertEquals("key1", ((CountingExceptionHandler.HandledException) CountingExceptionHandler.HANDLED_EXCEPTIONS.get(0)).getKey());
        assertEquals(CacheException.class, ((CountingExceptionHandler.HandledException) CountingExceptionHandler.HANDLED_EXCEPTIONS
                .get(0)).getException().getClass());
    }

    /**
     * Double proxy test
     */
    public void testCacheExceptionHandler() {
        Ehcache proxiedCache = ExceptionHandlingDynamicCacheProxy.createProxy(cache);

        //Would normally throw an IllegalArgumentException
        proxiedCache.put(null);

        assertEquals(1, CountingExceptionHandler.HANDLED_EXCEPTIONS.size());
        assertEquals(null, ((CountingExceptionHandler.HandledException) CountingExceptionHandler.HANDLED_EXCEPTIONS.get(0)).getKey());
        assertEquals(IllegalArgumentException.class, ((CountingExceptionHandler.HandledException) CountingExceptionHandler.HANDLED_EXCEPTIONS
                .get(0)).getException().getClass());
    }


    /**
     * Test some gnarly parsing code
     */
    public void testKeyExtraction() {

        String testMessage = "For key 1234";
        String key = ExceptionHandlingDynamicCacheProxy.extractKey(testMessage);
        assertEquals("1234", key);

        testMessage = "key 1234";
        key = ExceptionHandlingDynamicCacheProxy.extractKey(testMessage);
        assertEquals("1234", key);

        testMessage = null;
        key = ExceptionHandlingDynamicCacheProxy.extractKey(testMessage);
        assertEquals(null, key);

        testMessage = "";
        key = ExceptionHandlingDynamicCacheProxy.extractKey(testMessage);
        assertEquals(null, key);

        testMessage = "key 1234 ";
        key = ExceptionHandlingDynamicCacheProxy.extractKey(testMessage);
        assertEquals("1234", key);

        testMessage = "key 1234 .";
        key = ExceptionHandlingDynamicCacheProxy.extractKey(testMessage);
        assertEquals("1234", key);

        testMessage = "key .";
        key = ExceptionHandlingDynamicCacheProxy.extractKey(testMessage);
        assertEquals(".", key);

        testMessage = "key";
        key = ExceptionHandlingDynamicCacheProxy.extractKey(testMessage);
        assertEquals(null, key);

    }

    /**
     * Tests that the exception thrown by a configured loader, is
     * actually passed on to exception handler
     */
    public void testExceptionThrown() {
        cache.setCacheLoader(new CustomExceptionThrowingLoader());

        cache.getWithLoader("key1", null, null);

        assertEquals(1, CountingExceptionHandler.HANDLED_EXCEPTIONS.size());
        assertEquals("key1", ((CountingExceptionHandler.HandledException) CountingExceptionHandler.HANDLED_EXCEPTIONS.get(0)).getKey());


        Class expectedExceptionClass = UnsupportedOperationException.class;

        Exception e = ((CountingExceptionHandler.HandledException) CountingExceptionHandler.HANDLED_EXCEPTIONS
                .get(0)).getException();

        Throwable cause = e;
        boolean foundExceptionInChain = false;


        //Recurse through the chain
        while ((cause = cause.getCause()) != null) {
            
            if (cause.getClass().equals(expectedExceptionClass)) {
                foundExceptionInChain = true;
                break;
            }
        }

        if (foundExceptionInChain == false) {
            fail();
        }


    }
}
