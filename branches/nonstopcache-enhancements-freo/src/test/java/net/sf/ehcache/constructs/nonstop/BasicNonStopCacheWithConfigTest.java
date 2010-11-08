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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.junit.Test;

public class BasicNonStopCacheWithConfigTest extends AbstractBasicNonStopCacheTest {

    @Test
    public void testBasics() throws Exception {
        System.out.println("############## Testing NonStopCache's created from ehcache.xml ################");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/ehcache-decorator-config-test.xml"));
        List names = Arrays.asList(cacheManager.getCacheNames());

        assertEquals(4, names.size());

        assertTrue(names.contains("test"));
        assertTrue(names.contains("exceptionCacheDecorator"));
        assertTrue(names.contains("noopCacheDecorator"));
        assertTrue(names.contains("localReadsCacheDecorator"));

        Cache cache = cacheManager.getCache("test");
        int timeoutMillis = 50;

        doExceptionOnTimeout(cache, timeoutMillis, (NonStopCache) cacheManager.getEhcache("exceptionCacheDecorator"));
        doNoopTest(cache, timeoutMillis, (NonStopCache) cacheManager.getEhcache("noopCacheDecorator"));
        doLocalReadsTest(cache, timeoutMillis, (NonStopCache) cacheManager.getEhcache("localReadsCacheDecorator"));

    }

}
