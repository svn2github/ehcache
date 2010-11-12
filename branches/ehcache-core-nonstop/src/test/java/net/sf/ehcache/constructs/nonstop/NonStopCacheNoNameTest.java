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

package net.sf.ehcache.constructs.nonstop;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.ObjectExistsException;

import org.junit.Test;

/**
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonStopCacheNoNameTest extends TestCase {

    private static final String EHCACHE_NONSTOP_NONAME_TEST_XML = "/nonstop/ehcache-nonstop-noname-test.xml";
    private static final String EHCACHE_NONSTOP_NONAME_FAIL_TEST_XML = "/nonstop/ehcache-nonstop-noname-fail-test.xml";

    @Test
    public void testCacheDecoratorFactory() {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_NONSTOP_NONAME_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        System.out.println(cacheNames);

        assertEquals(6, cacheNames.size());

        checkDeclaredNamesExists(cacheNames);

    }

    private void checkDeclaredNamesExists(List<String> cacheNames) {
        assertTrue(cacheNames.contains("noDecoratorCache"));
        assertTrue(cacheNames.contains("oneDecoratorCache"));
        assertTrue(cacheNames.contains("oneDecoratorCacheFirst"));
        assertTrue(cacheNames.contains("twoDecoratorCache"));
        assertTrue(cacheNames.contains("twoDecoratorCacheFirst"));
        assertTrue(cacheNames.contains("twoDecoratorCacheSecond"));
    }

    @Test
    public void testCacheDecoratorFactoryProperties() {
        System.out.println("Testing default cache decorator properties");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_NONSTOP_NONAME_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(6, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);

        for (String name : cacheNames) {
            Ehcache ehcache = cacheManager.getEhcache(name);
            assertTrue("Should be instance of nonstopcache", ehcache instanceof NonStopCache);
        }

        checkDeclaredCachesProperties(cacheManager);

        System.out.println("Testing default cache decorator properties Complete");
    }

    private void checkDeclaredCachesProperties(CacheManager cacheManager) {
        // every cache is a nonstopcache as defaultCache has a decorator with no name
        checkNonStopCache(cacheManager, "noDecoratorCache", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
        checkNonStopCache(cacheManager, "oneDecoratorCache", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
        checkNonStopCache(cacheManager, "twoDecoratorCache", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
        checkNonStopCache(cacheManager, "oneDecoratorCacheFirst", NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT);
        checkNonStopCache(cacheManager, "twoDecoratorCacheFirst", NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT);
        checkNonStopCache(cacheManager, "twoDecoratorCacheSecond", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
    }

    @Test
    public void testAddDynamicCache() {
        System.out.println("Testing dynamic cache addition");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_NONSTOP_NONAME_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(6, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);

        cacheManager.addCache("newDynamicCache");
        cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(7, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);
        assertTrue(cacheNames.contains("newDynamicCache"));

        checkDeclaredCachesProperties(cacheManager);
        checkNonStopCache(cacheManager, "newDynamicCache", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);

        System.out.println("Testing dynamic cache addition Complete");
    }

    @Test
    public void testAddDynamicCacheIfAbsent() {
        System.out.println("Testing dynamic addCacheIfAbsent");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_NONSTOP_NONAME_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(6, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);

        Ehcache orig = cacheManager.addCacheIfAbsent("newDynamicCache");
        assertEquals("Newly added cache should be nonstopcache as defaultCache has decorator with no name", true,
                orig instanceof NonStopCache);
        cacheNames = Arrays.asList(cacheManager.getCacheNames());

        assertEquals(7, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);
        assertTrue(cacheNames.contains("newDynamicCache"));

        checkDeclaredCachesProperties(cacheManager);
        checkNonStopCache(cacheManager, "newDynamicCache", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);

        // do addCacheIfAbsent again, caches shouldn't get replaced
        Ehcache newCache = cacheManager.addCacheIfAbsent("newDynamicCache");
        assertTrue(orig == newCache);

        cacheNames = Arrays.asList(cacheManager.getCacheNames());

        assertEquals(7, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);
        assertTrue(cacheNames.contains("newDynamicCache"));

        checkDeclaredCachesProperties(cacheManager);
        checkNonStopCache(cacheManager, "newDynamicCache", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);

        // check reference equality
        assertTrue(orig == cacheManager.getEhcache("newDynamicCache"));

        // doing addCache() should blow up
        try {
            cacheManager.addCache("newDynamicCache");
            fail("Adding cache with same name should throw exception");
        } catch (ObjectExistsException e) {
            // expected
        }

        System.out.println("Testing dynamic addCacheIfAbsent Complete");
    }

    private void checkNonStopCache(CacheManager cacheManager, String name, NonStopCacheBehaviorType type) {
        Ehcache ehcache = cacheManager.getEhcache(name);
        assertEquals(name, ehcache.getName());
        assertEquals("Should be instanceof NonStopCache : " + ehcache, true, ehcache instanceof NonStopCache);
        NonStopCache ns = (NonStopCache) ehcache;
        assertEquals(type, ns.getTimeoutBehaviorType());
    }

    @Test
    public void testFailingConfig() {
        System.out.println("Testing failing config");
        try {
            CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_NONSTOP_NONAME_FAIL_TEST_XML));
            fail("Config having multiple ambiguous decorators should fail to initialize");
        } catch (CacheException e) {
            // expected
        }
        System.out.println("Testing failing config Complete");
    }
}
