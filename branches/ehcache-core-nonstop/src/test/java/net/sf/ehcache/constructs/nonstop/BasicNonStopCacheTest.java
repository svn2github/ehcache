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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.junit.Test;

public class BasicNonStopCacheTest extends AbstractBasicNonStopCacheTest {

    @Test
    public void testBasics() throws Exception {
        System.out.println("############## Testing with 100 millis cache timeout ################");
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/nonstop/basic-cache-test.xml"));
        Cache cache = cacheManager.getCache("test");
        NonStopCache nonStopCache = new NonStopCache(cache, "non-stop-test-cache");
        doTestForCacheTimeout(cache, 100, nonStopCache);
    }

}
