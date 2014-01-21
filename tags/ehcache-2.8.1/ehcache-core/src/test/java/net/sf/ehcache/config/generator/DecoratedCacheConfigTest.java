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

package net.sf.ehcache.config.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import net.sf.ehcache.CacheManager;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

@Category(CheckShorts.class)
public class DecoratedCacheConfigTest {

    private static final List<String> ALL_CACHE_NAMES = Arrays.asList(new String[] {"noDecoratorCache", "oneDecoratorCache",
            "oneDecoratorCacheFirst", "twoDecoratorCache", "twoDecoratorCacheSecond", "twoDecoratorCacheFirst"});

    @Test
    public void testDecoratedCacheConfig() {
      CacheManager cm = CacheManager.newInstance(DecoratedCacheConfigTest.class.getClassLoader().getResource(
                "ehcache-decorator-noname-test.xml"));
      try {
        List<String> names = new ArrayList<String>(Arrays.asList(cm.getCacheNames()));
        names.removeAll(ALL_CACHE_NAMES);
        Assert.assertEquals("This list should be empty - " + names, 0, names.size());
        // System.out.println("Original config: " + cm.getOriginalConfigurationText());
        String text = cm.getActiveConfigurationText();
        // System.out.println("Cache manager config: " + text);
        for (String name : ALL_CACHE_NAMES) {
            Assert.assertTrue("Config not generated for cache name: " + name, text.contains("name=\"" + name + "\""));
            String cacheConfigTest = cm.getActiveConfigurationText(name);
            // System.out.println("Config for cache: '"+name+"': " + cacheConfigTest);
            Assert.assertTrue("Config not generated for cache name: " + name + ", with explicit call: ",
                    cacheConfigTest.contains("name=\"" + name + "\""));
        }

      } finally {
        cm.shutdown();
      }

    }
}
