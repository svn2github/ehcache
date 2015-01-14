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

import java.net.URL;
import java.net.URLClassLoader;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;

import org.junit.Assert;
import org.junit.Test;

public class DifferingClassLoaderTest {

    @Test
    public void test() {
        CacheConfiguration cacheConfig = new CacheConfiguration().name("cache").maxBytesLocalHeap(1, MemoryUnit.MEGABYTES);
        cacheConfig.setClassLoader(new URLClassLoader(new URL[] {}));

        Configuration cacheMgrConfig = new Configuration();
        CacheManager cm = CacheManager.create(cacheMgrConfig);

        try {
            cm.addCache(new Cache(cacheConfig));
            throw new AssertionError();
        } catch (CacheException ce) {
            String msg = ce.getMessage();
            Assert.assertEquals(
                    "This cache (cache) is configurated with a different classloader reference than its containing cache manager", msg);
        }

    }
}
