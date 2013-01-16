
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

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.MemoryUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CacheCopyOnRwTest {
    private CacheManager cacheManager;

    @Before
    public void setUp() throws Exception {
        cacheManager = CacheManager.create( new Configuration()
        .diskStore(new DiskStoreConfiguration().path(System.getProperty("java.io.tmpdir")))
        .maxBytesLocalHeap(100, MemoryUnit.KILOBYTES)
        .maxBytesLocalDisk(200, MemoryUnit.KILOBYTES));

    }

    @After
    public void tearDown() {
        cacheManager.shutdown();
        cacheManager = null;
    }

    @Test
    public void testCopyOnReadWriteCache() throws Exception {
        cacheManager.addCache(new Cache(new CacheConfiguration().name("copyOnReadWriteCache").copyOnRead(true)
            .copyOnWrite(true)));
        Ehcache cache = cacheManager.getCache("copyOnReadWriteCache");
        testReplaceElement(cache);
        testRemoveElement(cache);
    }

    @Test
    public void testSimpleCache() throws Exception {
        cacheManager
                .addCache(new Cache(new CacheConfiguration().name("simpleCache").copyOnRead(false).copyOnWrite(false)));
        Ehcache cache = cacheManager.getCache("simpleCache");
        testReplaceElement(cache);
        testRemoveElement(cache);

    }

    @Test
    public void testCopyOnReadOnlyCache() throws Exception {
        cacheManager.addCache(new Cache(new CacheConfiguration().name("copyOnReadOnlyCache").copyOnRead(true)
                .copyOnWrite(false)));
        Ehcache cache = cacheManager.getCache("copyOnReadOnlyCache");
        testReplaceElement(cache);
        testRemoveElement(cache);
    }

    @Test
    public void testCopyOnWriteOnlyCache() throws Exception {
        cacheManager.addCache(new Cache(new CacheConfiguration().name("copyOnWriteOnlyCache").copyOnRead(false)
                .copyOnWrite(true)));
        Ehcache cache = cacheManager.getCache("copyOnWriteOnlyCache");
        testReplaceElement(cache);
        testRemoveElement(cache);
    }

    private void testReplaceElement(Ehcache cache) {
        Long key = System.nanoTime();
        String value = "value" + key;
        cache.put(new Element(new Long(key), new String(value)));
        Assert.assertEquals(cache.get(key).getValue(), new Element(new Long(key), new String(value)).getValue());
        String nextValue = value + "1";
        Assert.assertTrue(cache.replace(new Element(new Long(key), new String(value)), new Element(new Long(key), new String(nextValue))));
    }

    private void testRemoveElement(Ehcache cache) {
        Long key = System.nanoTime();
        String value = "value" + key;
        cache.put(new Element(new Long(key), new String(value)));
        Assert.assertEquals(cache.get(key).getValue(), new Element(new Long(key), new String(value)).getValue());
        Assert.assertTrue(cache.removeElement(new Element(new Long(key), new String(value))));
    }

}
