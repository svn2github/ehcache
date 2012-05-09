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

package net.sf.ehcache.constructs;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.ObjectExistsException;
import net.sf.ehcache.constructs.MockDecoratorFactory.MockDecoratorFactoryCache;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Abhishek Sanoujam
 */
public class DecoratorNoNameTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(DecoratorNoNameTest.class);

    private static final String EHCACHE_DECORATOR_NONAME_TEST_XML = "/ehcache-decorator-noname-test.xml";
    private static final String EHCACHE_DECORATOR_NONAME_FAIL_TEST_XML = "/ehcache-decorator-noname-fail-test.xml";

    @Test
    public void testCacheDecoratorFactory() {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DECORATOR_NONAME_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        LOG.info("" + cacheNames);

        assertEquals(6, cacheNames.size());

        checkDeclaredNamesExists(cacheNames);

        cacheManager.shutdown();

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
        LOG.info("Testing default cache decorator properties");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DECORATOR_NONAME_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(6, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);

        for (String name : cacheNames) {
            Ehcache ehcache = cacheManager.getEhcache(name);
            assertTrue("Should be instance of MockDecoratorFactoryCache", ehcache instanceof MockDecoratorFactoryCache);
        }

        checkDeclaredCachesProperties(cacheManager);

        LOG.info("Testing default cache decorator properties Complete");

        cacheManager.shutdown();
    }

    private void checkDeclaredCachesProperties(CacheManager cacheManager) {
        // every cache is a nonstopcache as defaultCache has a decorator with no name
        checkMockDecoratorCache(cacheManager, "noDecoratorCache", "defaultDecoratorKeyValue");
        checkMockDecoratorCache(cacheManager, "oneDecoratorCache", "defaultDecoratorKeyValue");
        checkMockDecoratorCache(cacheManager, "twoDecoratorCache", "defaultDecoratorKeyValue");
        checkMockDecoratorCache(cacheManager, "oneDecoratorCacheFirst", "oneDecoratorCacheFirstKey");
        checkMockDecoratorCache(cacheManager, "twoDecoratorCacheFirst", "twoDecoratorCacheFirstKey");
        checkMockDecoratorCache(cacheManager, "twoDecoratorCacheSecond", "twoDecoratorCacheSecondKey");
    }

    @Test
    public void testAddDynamicCache() {
        LOG.info("Testing dynamic cache addition");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DECORATOR_NONAME_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(6, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);

        cacheManager.addCache("newDynamicCache");
        cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(7, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);
        assertTrue(cacheNames.contains("newDynamicCache"));

        checkDeclaredCachesProperties(cacheManager);
        checkMockDecoratorCache(cacheManager, "newDynamicCache", "defaultDecoratorKeyValue");

        LOG.info("Testing dynamic cache addition Complete");

        cacheManager.shutdown();
    }

    @Test
    public void testFailingConfig() {
        LOG.info("Testing failing config");
        try {
            CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DECORATOR_NONAME_FAIL_TEST_XML));
            fail("Config having multiple ambiguous decorators should fail to initialize");
        } catch (CacheException e) {
            // expected
        }
        LOG.info("Testing failing config Complete");
    }

    @Test
    public void testAddDynamicCacheIfAbsent() {
        LOG.info("Testing dynamic addCacheIfAbsent");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DECORATOR_NONAME_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(6, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);

        Ehcache orig = cacheManager.addCacheIfAbsent("newDynamicCache");
        assertEquals("Newly added cache should be MockDecoratorFactoryCache as defaultCache has decorator with no name", true,
                orig instanceof MockDecoratorFactoryCache);
        cacheNames = Arrays.asList(cacheManager.getCacheNames());

        assertEquals(7, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);
        assertTrue(cacheNames.contains("newDynamicCache"));

        checkDeclaredCachesProperties(cacheManager);
        checkMockDecoratorCache(cacheManager, "newDynamicCache", "defaultDecoratorKeyValue");

        // do addCacheIfAbsent again, caches shouldn't get replaced
        Ehcache newCache = cacheManager.addCacheIfAbsent("newDynamicCache");
        assertTrue(orig == newCache);

        cacheNames = Arrays.asList(cacheManager.getCacheNames());

        assertEquals(7, cacheNames.size());
        checkDeclaredNamesExists(cacheNames);
        assertTrue(cacheNames.contains("newDynamicCache"));

        checkDeclaredCachesProperties(cacheManager);
        checkMockDecoratorCache(cacheManager, "newDynamicCache", "defaultDecoratorKeyValue");

        // check reference equality
        assertTrue(orig == cacheManager.getEhcache("newDynamicCache"));

        // doing addCache() should blow up
        try {
            cacheManager.addCache("newDynamicCache");
            fail("Adding cache with same name should throw exception");
        } catch (ObjectExistsException e) {
            // expected
        }

        LOG.info("Testing dynamic addCacheIfAbsent Complete");

        cacheManager.shutdown();
    }

    private void checkMockDecoratorCache(CacheManager cacheManager, String name, String expectedKeyValue) {
        Ehcache ehcache = cacheManager.getEhcache(name);
        assertEquals(name, ehcache.getName());
        assertEquals("Should be instanceof MockDecoratorFactoryCache : " + ehcache, true, ehcache instanceof MockDecoratorFactoryCache);
        MockDecoratorFactoryCache ns = (MockDecoratorFactoryCache) ehcache;
        assertEquals(expectedKeyValue, ns.getProperties().get("someKey"));
    }
}
