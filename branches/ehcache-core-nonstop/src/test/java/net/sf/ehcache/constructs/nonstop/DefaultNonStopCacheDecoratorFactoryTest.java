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
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.ObjectExistsException;

import org.junit.Test;

/**
 *
 * @author Abhishek Sanoujam
 *
 */
public class DefaultNonStopCacheDecoratorFactoryTest extends TestCase {

    private static final String EHCACHE_DEFAULT_DECORATOR_TEST_XML = "/nonstop/ehcache-default-nonstop-decorator-test.xml";

    @Test
    public void testCacheDecoratorFactory() {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DEFAULT_DECORATOR_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        System.out.println(cacheNames);

        assertEquals(7, cacheNames.size());

        assertTrue(cacheNames.contains("noDecoratorCache"));
        assertTrue(cacheNames.contains("noDecoratorCache-defaultDecoratorOne"));
        assertTrue(cacheNames.contains("noDecoratorCache-defaultDecoratorTwo"));
        assertTrue(cacheNames.contains("oneDecoratorCache"));
        assertTrue(cacheNames.contains("oneDecoratorCacheFirst"));
        assertTrue(cacheNames.contains("oneDecoratorCache-defaultDecoratorOne"));
        assertTrue(cacheNames.contains("oneDecoratorCache-defaultDecoratorTwo"));

    }

    @Test
    public void testCacheDecoratorFactoryProperties() {
        System.out.println("Testing default cache decorator properties");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DEFAULT_DECORATOR_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(7, cacheNames.size());

        for (String name : cacheNames) {
            Ehcache ehcache = cacheManager.getEhcache(name);
            if (name.equals("noDecoratorCache") || name.equals("oneDecoratorCache")) {
                assertTrue("Should be instance of Cache", ehcache instanceof Cache);
            } else {
                assertTrue("Should be instance of nonstopcache", ehcache instanceof NonStopCache);
            }
        }

        checkNonStopCache(cacheManager, "noDecoratorCache-defaultDecoratorOne", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
        checkNonStopCache(cacheManager, "noDecoratorCache-defaultDecoratorTwo", NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT);

        checkNonStopCache(cacheManager, "oneDecoratorCache-defaultDecoratorOne", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
        checkNonStopCache(cacheManager, "oneDecoratorCache-defaultDecoratorTwo", NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT);

        System.out.println("Testing default cache decorator properties Complete");
    }

    @Test
    public void testAddDynamicCache() {
        System.out.println("Testing dynamic cache addition");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DEFAULT_DECORATOR_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(7, cacheNames.size());

        cacheManager.addCache("newDynamicCache");
        cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(10, cacheNames.size());
        cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertTrue(cacheNames.contains("newDynamicCache"));
        assertTrue(cacheNames.contains("newDynamicCache-defaultDecoratorOne"));
        assertTrue(cacheNames.contains("newDynamicCache-defaultDecoratorTwo"));

        checkNonStopCache(cacheManager, "newDynamicCache-defaultDecoratorOne", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
        checkNonStopCache(cacheManager, "newDynamicCache-defaultDecoratorTwo", NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT);

        System.out.println("Testing dynamic cache addition Complete");
    }

    @Test
    public void testAddDynamicCacheIfAbsent() {
        System.out.println("Testing dynamic addCacheIfAbsent");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DEFAULT_DECORATOR_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(7, cacheNames.size());

        Ehcache orig = cacheManager.addCacheIfAbsent("newDynamicCache");
        cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(10, cacheNames.size());
        cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertTrue(cacheNames.contains("newDynamicCache"));
        assertTrue(cacheNames.contains("newDynamicCache-defaultDecoratorOne"));
        assertTrue(cacheNames.contains("newDynamicCache-defaultDecoratorTwo"));

        checkNonStopCache(cacheManager, "newDynamicCache-defaultDecoratorOne", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
        checkNonStopCache(cacheManager, "newDynamicCache-defaultDecoratorTwo", NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT);

        Ehcache origOne = cacheManager.getEhcache("newDynamicCache-defaultDecoratorOne");
        Ehcache origTwo = cacheManager.getEhcache("newDynamicCache-defaultDecoratorTwo");

        // do addCacheIfAbsent again, caches shouldn't get replaced
        cacheManager.addCacheIfAbsent("newDynamicCache");
        cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(10, cacheNames.size());
        cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertTrue(cacheNames.contains("newDynamicCache"));
        assertTrue(cacheNames.contains("newDynamicCache-defaultDecoratorOne"));
        assertTrue(cacheNames.contains("newDynamicCache-defaultDecoratorTwo"));

        checkNonStopCache(cacheManager, "newDynamicCache-defaultDecoratorOne", NonStopCacheBehaviorType.NO_OP_ON_TIMEOUT);
        checkNonStopCache(cacheManager, "newDynamicCache-defaultDecoratorTwo", NonStopCacheBehaviorType.EXCEPTION_ON_TIMEOUT);

        // check reference equality
        assertTrue(orig == cacheManager.getEhcache("newDynamicCache"));
        assertTrue(origOne == cacheManager.getEhcache("newDynamicCache-defaultDecoratorOne"));
        assertTrue(origTwo == cacheManager.getEhcache("newDynamicCache-defaultDecoratorTwo"));

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
        assertTrue(ehcache instanceof NonStopCache);
        NonStopCache ns = (NonStopCache) ehcache;
        assertEquals(ns.getTimeoutBehaviorType(), type);
    }
}
