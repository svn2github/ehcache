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

package net.sf.ehcache.constructs.readthrough;

import java.util.Properties;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.refreshahead.StringifyCacheLoaderFactory;
import net.sf.ehcache.loader.CacheLoader;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

@Category(CheckShorts.class)
public class ReadThroughCacheTest {

    private static final Properties configProperties = new Properties() {
        {
            setProperty("delayMS", "1000");
        }
    };
    private static CacheLoader stringifyCacheLoader = new StringifyCacheLoaderFactory().createCacheLoader(null, configProperties);

    private static void sleepySeconds(int secs) {
        sleepy(secs * 1000);
    }

    private static void sleepy(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {

        }
    }

    @Test
    public void testReadThroughSimpleCase() {

        CacheManager manager = new CacheManager();
        manager.removeAllCaches();

        CacheConfiguration config = new CacheConfiguration().name("sampleCacheReadThru").maxElementsInMemory(100).timeToIdleSeconds(8)
                .timeToLiveSeconds(8).overflowToDisk(false);
        manager.addCache(new Cache(config));


        Ehcache cache = manager.getEhcache("sampleCacheReadThru");
        ReadThroughCacheConfiguration readThroughConfig = new ReadThroughCacheConfiguration().modeGet(true).build();

        ReadThroughCache decorator = new ReadThroughCache(cache, readThroughConfig);

        cache.registerCacheLoader(stringifyCacheLoader);

        // should not be in the cache
        Element got = cache.get(new Integer(1));
        Assert.assertNull(got);

        // now load with decorator via get
        got = decorator.get(new Integer(1));
        Assert.assertNotNull(got);

        // now should be in the base cache
        got = cache.get(new Integer(1));
        Assert.assertNotNull(got);

        // now let it expire.
        sleepySeconds(10);

        // missing
        got = cache.get(new Integer(1));
        Assert.assertNull(got);

        // now load with get with loader
        got = decorator.get(new Integer(1));
        Assert.assertNotNull(got);

        // now should be in the base cache too.
        got = cache.get(new Integer(1));
        Assert.assertNotNull(got);

        manager.removeAllCaches();
        manager.shutdown();
    }
}
