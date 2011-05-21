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

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for Cache pinning
 *
 * @author Ludovic Orban
 */
public class CachePoolPinningTest {

    private static final int ELEMENT_COUNT = 500;

    private CacheManager cacheManager;

    @Before
    public void setUp() throws Exception {
        cacheManager = new CacheManager("src/test/resources/ehcache-pool-pinning.xml");
    }

    @After
    public void tearDown() {
        cacheManager.shutdown();
        cacheManager = null;
    }


    @Test
    public void testClassicLru() throws Exception {
        tearDown();
        System.setProperty(Cache.NET_SF_EHCACHE_USE_CLASSIC_LRU, "true");
        setUp();
        System.setProperty(Cache.NET_SF_EHCACHE_USE_CLASSIC_LRU, "false");

        
        Cache cache = cacheManager.getCache("memoryOnlyCache");
        Cache cacheNoPinning = cacheManager.getCache("memoryOnlyCacheNoPinning");

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            cacheNoPinning.put(new Element(i, i));
        }

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            cache.put(new Element(i, i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            assertNotNull(cache.get(i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getStatistics().getInMemoryHits());
        Assert.assertEquals(0, cache.getStatistics().getInMemoryMisses());
        Assert.assertEquals(0, cache.getStatistics().getEvictionCount());
    }

    @Test
    public void testMemoryOnly() throws Exception {
        Cache cache = cacheManager.getCache("memoryOnlyCache");
        Cache cacheNoPinning = cacheManager.getCache("memoryOnlyCacheNoPinning");

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            cacheNoPinning.put(new Element(i, i));
        }

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            cache.put(new Element(i, i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            assertNotNull(cache.get(i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getStatistics().getInMemoryHits());
        Assert.assertEquals(0, cache.getStatistics().getInMemoryMisses());
        Assert.assertEquals(0, cache.getStatistics().getEvictionCount());
    }

    @Test
    public void testOverflowToDisk() throws Exception {
        Cache cache = cacheManager.getCache("overflowToDiskCache");
        Cache cacheNoPinning = cacheManager.getCache("overflowToDiskCacheNoPinning");

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            cacheNoPinning.put(new Element(i, i));
        }

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            cache.put(new Element(i, i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());
        Assert.assertEquals(0, cache.getDiskStoreSize());

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            assertNotNull(cache.get(i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getStatistics().getInMemoryHits());
        Assert.assertEquals(0, cache.getStatistics().getInMemoryMisses());
        Assert.assertEquals(0, cache.getStatistics().getOnDiskHits());
        Assert.assertEquals(0, cache.getStatistics().getOnDiskMisses());
        Assert.assertEquals(0, cache.getStatistics().getEvictionCount());
    }

    @Test
    public void testDiskPersistent() throws Exception {
        Cache cache = cacheManager.getCache("diskPersistentCache");
        Cache cacheNoPinning = cacheManager.getCache("diskPersistentCacheNoPinning");

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            cacheNoPinning.put(new Element(i, i));
        }

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            cache.put(new Element(i, i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());

        for (int i = 0; i < ELEMENT_COUNT; i++) {
            assertNotNull(cache.get(i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getStatistics().getInMemoryHits());
        Assert.assertEquals(0, cache.getStatistics().getInMemoryMisses());
        Assert.assertEquals(0, cache.getStatistics().getOnDiskHits());
        Assert.assertEquals(0, cache.getStatistics().getOnDiskMisses());
        Assert.assertEquals(0, cache.getStatistics().getEvictionCount());
    }

}

