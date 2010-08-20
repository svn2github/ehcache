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

import java.lang.reflect.Field;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;

/**
 * Simple test to reuse nonstopCache after adding to cacheManager
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class SharingNonStopCacheTest extends TestCase {

    private static final int SYSTEM_CLOCK_EPSILON_MILLIS = 90;

    public void testShare() throws Exception {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-cache-test.xml"));
        Cache cache = cacheManager.getCache("test");

        String name = "sharedNonStopCache";
        NonStopCache nonStopCache = new NonStopCache(cache, name);

        // using cacheManager.addCache(nonStopCache) will result in exception
        // decorated caches always use cacheManager.addDecoratedCache()
        cacheManager.addDecoratedCache(nonStopCache);

        nonStopCache.put(new Element("key", "value"));

        useSharedCache(cacheManager, name, false);

        replaceWithBlockingStore(cache);

        useSharedCache(cacheManager, name, true);

    }

    private void useSharedCache(CacheManager cacheManager, String name, boolean expectException) {
        Ehcache cache = cacheManager.getEhcache(name);
        assertTrue(cache instanceof NonStopCache);
        NonStopCache nonStopCache = (NonStopCache) cache;

        if (!expectException) {
            Element element = nonStopCache.get("key");
            assertNotNull("Element should not be null", element);
            assertEquals("value", element.getValue());
        } else {
            int timeout = 100;
            nonStopCache.setTimeoutMillis(timeout);
            long start = System.currentTimeMillis();
            try {
                Element element = nonStopCache.get("key");
                fail("Get operation should have thrown exception");
            } catch (NonStopCacheException e) {
                System.out.println("Caught expected exception - " + e);
                long duration = System.currentTimeMillis() - start;
                assertTrue("Get operation should have taken at least " + timeout + " ms. actual: " + duration, duration + SYSTEM_CLOCK_EPSILON_MILLIS >= timeout);
            }
        }
    }

    private void replaceWithBlockingStore(Cache cache) throws Exception {
        BlockingMockStore mockClusteredStore = new BlockingMockStore();
        replaceStoreField(cache, mockClusteredStore);
    }

    private void replaceStoreField(Cache cache, Store replaceWith) throws Exception {
        Field storeField = Cache.class.getDeclaredField("compoundStore");
        storeField.setAccessible(true);
        storeField.set(cache, replaceWith);
    }

}
