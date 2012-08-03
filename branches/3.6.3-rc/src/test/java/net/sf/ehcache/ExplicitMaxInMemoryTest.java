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

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.event.CacheEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExplicitMaxInMemoryTest extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExplicitMaxInMemoryTest.class);

    private static final int MB = 1024 * 1024;

    public void testExplicitMaxInMemory() throws Exception {
        Configuration config = new Configuration();
        config.maxBytesLocalHeap(10, MemoryUnit.MEGABYTES);
        CacheManager cm = new CacheManager(config);

        CacheConfiguration cc = new CacheConfiguration("testCache", 0);
        cm.addCache(new Cache(cc));

        Cache cache = cm.getCache("testCache");
        Assert.assertEquals(0, cache.getCacheConfiguration().getMaxEntriesLocalHeap());

        CountingEvictionListener countingEvictionListener = new CountingEvictionListener();
        cache.getCacheEventNotificationService().registerListener(countingEvictionListener);

        for (int i = 0; i < 20; i++) {
            cache.put(new Element("key-" + i, new byte[MB]));
            LOGGER.info("After put: i=" + i + ", size: " + cache.getSize() + ", sizeBytes: " + cache.calculateInMemorySize());
        }

        Assert.assertTrue(9 <= cache.getMemoryStoreSize());
        Assert.assertTrue(11 >= cache.getMemoryStoreSize());

        Assert.assertTrue(cache.calculateInMemorySize() > 9 * MB);
        Assert.assertTrue(cache.calculateInMemorySize() < 11 * MB);

        Assert.assertTrue(9 <= countingEvictionListener.evictionCounter.get());
        Assert.assertTrue(11 >= countingEvictionListener.evictionCounter.get());

    }

    private static class CountingEvictionListener implements CacheEventListener {
        private static final AtomicInteger evictionCounter = new AtomicInteger();

        public void notifyRemoveAll(Ehcache cache) {
            //
        }

        public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
            //

        }

        public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
            //
        }

        public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
            //
        }

        public void notifyElementExpired(Ehcache cache, Element element) {
            //
        }

        public void notifyElementEvicted(Ehcache cache, Element element) {
            evictionCounter.incrementAndGet();
            LOGGER.info("XXXXXXX element evicted: key: " + element.getKey());
        }

        public void dispose() {
            //
        }

        @Override
        public Object clone() {
            throw new RuntimeException();
        }
    }
}
