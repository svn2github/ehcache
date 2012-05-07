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

import org.junit.Test;

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.constructs.MockDecoratorFactory.MockDecoratorFactoryCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Abhishek Sanoujam
 */
public class CacheDecoratorFactoryTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(CacheDecoratorFactoryTest.class);

    @Test
    public void testCacheDecoratorFactory() {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/ehcache-decorator-test.xml"));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        LOG.info("" + cacheNames);

        assertEquals(12, cacheNames.size());

        assertTrue(cacheNames.contains("noDecoratorCache"));
        assertTrue(cacheNames.contains("oneDecoratorCache"));
        assertTrue(cacheNames.contains("oneDecoratorFirst"));
        assertTrue(cacheNames.contains("twoDecoratorCache"));
        assertTrue(cacheNames.contains("twoDecoratorFirst"));
        assertTrue(cacheNames.contains("twoDecoratorSecond"));
        assertTrue(cacheNames.contains("fiveDecoratorCache"));
        assertTrue(cacheNames.contains("fiveDecoratorFirst"));
        assertTrue(cacheNames.contains("fiveDecoratorSecond"));
        assertTrue(cacheNames.contains("fiveDecoratorThird"));
        assertTrue(cacheNames.contains("fiveDecoratorFourth"));
        assertTrue(cacheNames.contains("fiveDecoratorFifth"));

        cacheManager.shutdown();

    }

    @Test
    public void testCacheDecoratorFactoryProperties() {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/ehcache-decorator-test.xml"));
        List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
        assertEquals(12, cacheNames.size());

        MockDecoratorFactoryCache cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("oneDecoratorFirst");
        assertEquals("oneFirst", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("twoDecoratorFirst");
        assertEquals("twoFirst", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("twoDecoratorSecond");
        assertEquals("twoSecond", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("fiveDecoratorFirst");
        assertEquals("fiveFirst", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("fiveDecoratorSecond");
        assertEquals("fiveSecond", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("fiveDecoratorThird");
        assertEquals("fiveThird", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("fiveDecoratorFourth");
        assertEquals("fiveFourth", cache.getProperties().getProperty("someKey"));

        cache = (MockDecoratorFactoryCache) cacheManager.getEhcache("fiveDecoratorFifth");
        assertEquals("fiveFifth", cache.getProperties().getProperty("someKey"));

        cacheManager.shutdown();

    }
}
