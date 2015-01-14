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
import net.sf.ehcache.config.PersistenceConfiguration;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.terracotta.test.categories.CheckShorts;

import java.util.Arrays;
import java.util.Collection;

@Category(CheckShorts.class)
@RunWith(Parameterized.class)
public class CacheCopyOnRwReplaceRemoveTest {

    public static final String MEMORY_CACHE = "memoryCache";
    public static final String DISK_CACHE = "diskCache";

    @Parameters(name = "copyOnRead:{0}, copyOnWrite:{1}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { true, false }, { false, true }, { true, true } };
        return Arrays.asList(data);
    }

    private final boolean copyOnRead;
    private final boolean copyOnWrite;

    private CacheManager cacheManager;

    public CacheCopyOnRwReplaceRemoveTest(boolean copyOnRead, boolean copyOnWrite) {
        this.copyOnRead = copyOnRead;
        this.copyOnWrite = copyOnWrite;
    }

    @Before
    public void setUp() throws Exception {
        cacheManager = CacheManager.create( new Configuration()
                .name("copyOnRWReplaceRemoveManager")
                .diskStore(new DiskStoreConfiguration().path(System.getProperty("java.io.tmpdir")))
                .maxBytesLocalHeap(100, MemoryUnit.KILOBYTES)
                .maxBytesLocalDisk(200, MemoryUnit.KILOBYTES));
        cacheManager.addCache(new Cache(new CacheConfiguration().name(MEMORY_CACHE)
                .copyOnRead(copyOnRead)
                .copyOnWrite(copyOnWrite)));
        cacheManager.addCache(new Cache(new CacheConfiguration().name(DISK_CACHE)
                .persistence(new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.LOCALTEMPSWAP))
                .copyOnRead(copyOnRead)
                .copyOnWrite(copyOnWrite)));
    }

    @After
    public void tearDown() {
        cacheManager.shutdown();
    }

    @Test
    public void testMemoryCache() throws Exception {
        Ehcache cache = cacheManager.getCache(MEMORY_CACHE);
        testReplaceElement(cache);
        testRemoveElement(cache);
    }

    @Test
    public void testDiskCache() throws Exception {
        Ehcache cache = cacheManager.getCache(DISK_CACHE);
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
