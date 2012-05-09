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

package net.sf.ehcache;

import static org.junit.Assert.assertEquals;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.junit.Test;

public class AutoVersioningElementTest {

    @Test
    public void testVersioningCanRevertToOldBehavior() {
        System.setProperty("net.sf.ehcache.element.version.auto", "true");
        try {
            CacheManager cacheManager = CacheManager.getInstance();
            cacheManager.addCache(new Cache("mltest", 50,
                    MemoryStoreEvictionPolicy.LRU, true, null, true, 0, 0, false, 120, null, null, 0, 2, false));
            Cache cache = cacheManager.getCache("mltest");

            Element a = new Element("a key", "a value", 1L);
            cache.put(a);
            Element aAfter = cache.get("a key");
            assertEquals(aAfter.getLastUpdateTime(), aAfter.getVersion());

            Element b = new Element("a key", "a value");
            cache.put(b);
            Element bAfter = cache.get("a key");
            assertEquals(bAfter.getLastUpdateTime(), bAfter.getVersion());

            Element c = new Element("a key", "a value", 3L);
            cache.put(c);
            Element cAfter = cache.get("a key");
            assertEquals(cAfter.getLastUpdateTime(), cAfter.getVersion());
        } finally {
            System.getProperties().remove("net.sf.ehcache.element.version.auto");
        }
    }
}
