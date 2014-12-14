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
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.constructs.MockDecoratorFactory.MockDecoratorFactoryCache;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.test.categories.CheckShorts;

/**
 * @author Abhishek Sanoujam
 */
@Category(CheckShorts.class)
public class DefaultCacheDecoratorFactoryTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCacheDecoratorFactoryTest.class);

    private static final String EHCACHE_DEFAULT_DECORATOR_TEST_XML = "/ehcache-default-decorator-test.xml";

    @Test
    public void testCacheDecoratorFactory() {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DEFAULT_DECORATOR_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        LOG.info("" + cacheNames);

        assertEquals(7, cacheNames.size());

        assertTrue(cacheNames.contains("noDecoratorCache"));
        assertTrue(cacheNames.contains("noDecoratorCache-defaultDecoratorOne"));
        assertTrue(cacheNames.contains("noDecoratorCache-defaultDecoratorTwo"));
        assertTrue(cacheNames.contains("oneDecoratorCache"));
        assertTrue(cacheNames.contains("oneDecoratorCacheFirst"));
        assertTrue(cacheNames.contains("oneDecoratorCache-defaultDecoratorOne"));
        assertTrue(cacheNames.contains("oneDecoratorCache-defaultDecoratorTwo"));

        cacheManager.shutdown();

    }

    @Test
    public void testCacheDecoratorFactoryProperties() {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream(EHCACHE_DEFAULT_DECORATOR_TEST_XML));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(7, cacheNames.size());

        MockDecoratorFactoryCache cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("noDecoratorCache-defaultDecoratorOne");
        assertEquals("defaultDecoratorOne", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("noDecoratorCache-defaultDecoratorTwo");
        assertEquals("defaultDecoratorTwo", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("oneDecoratorCache-defaultDecoratorOne");
        assertEquals("defaultDecoratorOne", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("oneDecoratorCache-defaultDecoratorTwo");
        assertEquals("defaultDecoratorTwo", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("oneDecoratorCacheFirst");
        assertEquals("oneFirst", cache.getProperties().getProperty("someKey"));

        cacheManager.shutdown();

    }
}
