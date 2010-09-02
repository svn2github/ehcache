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

package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author hhuynh
 *
 */
public class OffheapStoreInOssTest {

    @Test
    public void testOffheapInOss() throws Exception {
        try {
            CacheManager manager = CacheManager.create();
            Cache cache = new Cache(new CacheConfiguration("test", 1).overflowToOffHeap(true).maxMemoryOffHeap("1M"));
            manager.addCache(cache);
            Assert.fail();
        } catch (CacheException e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("You need Enterprise version"));
        }
    }
}
