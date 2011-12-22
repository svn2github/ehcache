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

package net.sf.ehcache;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import junit.framework.Assert;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.store.disk.DiskStoreHelper;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Tests for Cache pinning
 *
 * @author Ludovic Orban
 */
public class CachePinningTest {

    private static final int ELEMENT_COUNT = 4000;

    private CacheManager cacheManager;

    @Before
    public void setUp() throws Exception {
        cacheManager = new CacheManager(
                new Configuration()
                        .diskStore(new DiskStoreConfiguration().path(System.getProperty("java.io.tmpdir")))
        );
    }

    @After
    public void tearDown() {
        cacheManager.shutdown();
        cacheManager = null;
    }

    @Test
    public void testClassicLru() throws Exception {
        System.setProperty(Cache.NET_SF_EHCACHE_USE_CLASSIC_LRU, "true");
        try {
            testMemoryOnly();
        } finally {
            System.setProperty(Cache.NET_SF_EHCACHE_USE_CLASSIC_LRU, "false");
        }
    }

    @Test
    public void testMemoryOnly() throws Exception {
        cacheManager.addCache(new Cache(
                new CacheConfiguration()
                        .maxEntriesLocalHeap(10)
                        .statistics(true)
                        .name("memoryOnlyCache_onHeap")
                        .pinning(new PinningConfiguration().store(PinningConfiguration.Store.LOCALHEAP))
        ));
        doAssertions(cacheManager.getCache("memoryOnlyCache_onHeap"), ELEMENT_COUNT, 0);

        cacheManager.addCache(new Cache(
                new CacheConfiguration()
                        .maxEntriesLocalHeap(10)
                        .statistics(true)
                        .name("memoryOnlyCache_inMemory")
                        .pinning(new PinningConfiguration().store(PinningConfiguration.Store.LOCALMEMORY))
        ));
        doAssertions(cacheManager.getCache("memoryOnlyCache_inMemory"), ELEMENT_COUNT, 0);

        cacheManager.addCache(new Cache(
                new CacheConfiguration()
                        .maxEntriesLocalHeap(10)
                        .statistics(true)
                        .name("memoryOnlyCache_inCache")
                        .pinning(new PinningConfiguration().store(PinningConfiguration.Store.INCACHE))
        ));
        doAssertions(cacheManager.getCache("memoryOnlyCache_inCache"), ELEMENT_COUNT, 0);
    }

    @Test
    public void testOverflowToDisk() throws Exception {
        cacheManager.addCache(new Cache(
                new CacheConfiguration()
                        .maxEntriesLocalHeap(10)
                        .maxElementsOnDisk(20)
                        .overflowToDisk(true)
                        .statistics(true)
                        .name("overflowToDiskCache_onHeap")
                        .pinning(new PinningConfiguration().store(PinningConfiguration.Store.LOCALHEAP))
        ));
        doAssertions(cacheManager.getCache("overflowToDiskCache_onHeap"), ELEMENT_COUNT, 0);

        cacheManager.addCache(new Cache(
                new CacheConfiguration()
                        .maxEntriesLocalHeap(10)
                        .maxElementsOnDisk(20)
                        .overflowToDisk(true)
                        .statistics(true)
                        .name("overflowToDiskCache_inMemory")
                        .pinning(new PinningConfiguration().store(PinningConfiguration.Store.LOCALMEMORY))
        ));
        doAssertions(cacheManager.getCache("overflowToDiskCache_inMemory"), ELEMENT_COUNT, 0);

        cacheManager.addCache(new Cache(
                new CacheConfiguration()
                        .maxEntriesLocalHeap(10)
                        .maxElementsOnDisk(20)
                        .overflowToDisk(true)
                        .statistics(true)
                        .name("overflowToDiskCache_inCache")
                        .pinning(new PinningConfiguration().store(PinningConfiguration.Store.INCACHE))
        ));
        doAssertions(cacheManager.getCache("overflowToDiskCache_inCache"), 10, ELEMENT_COUNT - 10);
    }

    @Test
    public void testDiskPersistent() throws Exception {
        cacheManager.addCache(new Cache(
                new CacheConfiguration()
                        .maxEntriesLocalHeap(10)
                        .maxElementsOnDisk(20)
                        .overflowToDisk(true)
                        .diskPersistent(true)
                        .statistics(true)
                        .name("diskPersistentCache_onHeap")
                        .pinning(new PinningConfiguration().store(PinningConfiguration.Store.LOCALHEAP))
        ));
        doAssertions(cacheManager.getCache("diskPersistentCache_onHeap"), ELEMENT_COUNT, 0);

        cacheManager.addCache(new Cache(
                new CacheConfiguration()
                        .maxEntriesLocalHeap(10)
                        .maxElementsOnDisk(20)
                        .overflowToDisk(true)
                        .diskPersistent(true)
                        .statistics(true)
                        .name("diskPersistentCache_inMemory")
                        .pinning(new PinningConfiguration().store(PinningConfiguration.Store.LOCALMEMORY))
        ));
        doAssertions(cacheManager.getCache("diskPersistentCache_inMemory"), ELEMENT_COUNT, 0);

        cacheManager.addCache(new Cache(
                new CacheConfiguration()
                        .maxEntriesLocalHeap(10)
                        .maxElementsOnDisk(20)
                        .overflowToDisk(true)
                        .diskPersistent(true)
                        .statistics(true)
                        .name("diskPersistentCache_inCache")
                        .pinning(new PinningConfiguration().store(PinningConfiguration.Store.INCACHE))
        ));
        doAssertions(cacheManager.getCache("diskPersistentCache_inCache"), 10, ELEMENT_COUNT - 10);
    }

    private void doAssertions(Cache cache, long expectedMemoryHits, long expectedDiskHits) throws ExecutionException, InterruptedException {
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            cache.put(new Element(i, i));
        }

        flushDisk(cache);
        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            assertNotNull(cache.get(i));
        }

        Assert.assertEquals(expectedMemoryHits, cache.getStatistics().getInMemoryHits());
        Assert.assertEquals(ELEMENT_COUNT - expectedMemoryHits, cache.getStatistics().getInMemoryMisses());
        Assert.assertEquals(expectedDiskHits, cache.getStatistics().getOnDiskHits());
        Assert.assertEquals(0, cache.getStatistics().getOnDiskMisses());
        Assert.assertEquals(0, cache.getStatistics().getEvictionCount());
    }

    @Test
    public void testElementPinningWithMaxElements() throws Exception {

        int maxElementsInMemory = 100;
        int unpinCount = maxElementsInMemory * 2;
        int pinCount = unpinCount * 2;

        Cache cache = new Cache(new CacheConfiguration("myCache", maxElementsInMemory));
        cacheManager.addCache(cache);

        for (int i = 0; i < unpinCount; i++) {
            Element element = new Element("Ku-" + i, "" + i);
            cache.put(element);
        }

        flushDisk(cache);
        Assert.assertEquals(maxElementsInMemory, cache.getSize());

        for (int i = 0; i < pinCount; i++) {
            Element element = new Element("Kp-" + i, new Object());
            cache.setPinned(element.getObjectKey(), true);
            cache.put(element);
        }

        flushDisk(cache);
        Assert.assertEquals(pinCount, cache.getSize());

        for (int i = 0; i < pinCount; i++) {
            Assert.assertTrue(cache.isPinned("Kp-" + i));
        }

        cache.unpinAll();
        flushDisk(cache);
        for (int i = 0; i < pinCount; i++) {
            Assert.assertFalse(cache.isPinned("Kp-" + i));
            Element element = new Element("Ku-" + i, new Object());
            cache.put(element);
        }
        flushDisk(cache);
        Assert.assertEquals(maxElementsInMemory, cache.getSize());
    }

    @Test
    public void testNonPresentPinnedKeysAreNotInCache()  {
        final Cache cache = new Cache(new CacheConfiguration().name("nonPresentPinned")
            .diskPersistent(false)
            .maxEntriesLocalDisk(10)
            .maxEntriesLocalHeap(5)
            .eternal(true));
        cacheManager.addCache(cache);
        final Object key = new Object();
        assertThat(cache.isKeyInCache(key), Matchers.is(false));
        cache.setPinned(key, true);
        assertThat(cache.get(key), nullValue());
        assertThat(cache.isKeyInCache(key), Matchers.is(false));
    }

    private void flushDisk(final Cache cache) throws InterruptedException, ExecutionException {
        final Future<Void> future = DiskStoreHelper.flushAllEntriesToDisk(cache);
        if(future != null) {
            future.get();
        }
    }
}

