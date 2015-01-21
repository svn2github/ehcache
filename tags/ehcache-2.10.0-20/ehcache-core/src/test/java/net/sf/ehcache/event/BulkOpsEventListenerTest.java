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

package net.sf.ehcache.event;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.test.categories.CheckShorts;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Category(CheckShorts.class)
public class BulkOpsEventListenerTest extends AbstractCacheTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkOpsEventListenerTest.class);
    private static final int BATCH_SIZE = 10000;
    private static final int MAX_RETRY = 100;

    @Test
    public void testBulkOpsEventListener() throws Throwable {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName("cache");
        cacheConfiguration.setMaxElementsInMemory(1000);
        cacheConfiguration.setOverflowToDisk(true);
        cacheConfiguration.setEternal(false);
        cacheConfiguration.setTimeToLiveSeconds(100000);
        cacheConfiguration.setTimeToIdleSeconds(200000);
        cacheConfiguration.setDiskPersistent(false);
        cacheConfiguration.setDiskExpiryThreadIntervalSeconds(1);
        Cache cache = new Cache(cacheConfiguration);
        manager.addCache(cache);

        TestCacheEventListener eventListener = new TestCacheEventListener();
        cache.getCacheEventNotificationService().registerListener(eventListener);

        int numOfElements = 100;
        Set<Element> elements = new HashSet<Element>();
        for(int i = 0; i < numOfElements; i++){
            elements.add(new Element("key" + i, "value" + i));
        }
        cache.putAll(elements);
        assertEquals(numOfElements, cache.getSize());
        assertEquals(numOfElements, eventListener.elementsPut.size());
        assertEquals(elements, eventListener.elementsPut);

        Set<String> keySet1 = new HashSet<String>();
        for(int i = 0; i < numOfElements; i++){
            keySet1.add("key"+i);
        }

        Map<Object, Element> rv = cache.getAll(keySet1);
        assertEquals(numOfElements, rv.size());

        for(Element element : rv.values()){
            assertTrue(elements.contains(element));
        }

        Collection<Element> values = rv.values();
        for(Element element : elements){
            assertTrue(values.contains(element));
        }

        Random rand = new Random();
        Set<String> keySet2 = new HashSet<String>();
        for(int i = 0; i < numOfElements/2; i++){
            keySet2.add("key" + rand.nextInt(numOfElements));
        }

        rv = cache.getAll(keySet2);
        assertEquals(keySet2.size(), rv.size());

        for(Element element : rv.values()){
            assertTrue(elements.contains(element));
        }

        assertEquals(keySet2, rv.keySet());

        cache.removeAll(keySet2);
        assertEquals(numOfElements - keySet2.size(), cache.getSize());
        assertEquals(keySet2.size(), eventListener.elementsRemoved.size());
        Set<String> removedKeySet = new HashSet<String>();
        for(Element element : eventListener.elementsRemoved){
            removedKeySet.add(element.getObjectKey().toString());
        }
        assertEquals(keySet2, removedKeySet);

        for(Object key : keySet2){
            assertNull(cache.get(key));
        }

        cache.removeAll();
        assertEquals(0, cache.getSize());
    }

    @Test
    public void testMultiThreadedBulkOpsPut() throws Exception{
        final Cache cache = new Cache("putCache", 1000000, true, false, 100000, 200000, false, 1);
        manager.addCache(cache);

        final TestCacheEventListener eventListener = new TestCacheEventListener();
        cache.getCacheEventNotificationService().registerListener(eventListener);

        final AtomicBoolean stopCondition = new AtomicBoolean(false);
        final String p1Value = "p1Value";
        final String p2Value = "p2Value";

        final AtomicInteger retryCount = new AtomicInteger(0);

        CyclicBarrier barrier = new CyclicBarrier(2, new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Checking put did interleave (run {})", retryCount.incrementAndGet());
                boolean p1Seen = false;
                boolean p2Seen = false;
                for (Object key : cache.getKeys()) {
                    if (cache.get(key).getObjectValue().equals(p1Value)) {
                        p1Seen = true;
                    }
                    if (cache.get(key).getObjectValue().equals(p2Value)) {
                        p2Seen = true;
                    }
                    if (p1Seen && p2Seen) {
                        stopCondition.set(true);
                        return;
                    }
                }
                if (retryCount.get() >= MAX_RETRY) {
                    stopCondition.set(true);
                    LOGGER.warn("Could not interleave - stopping as MAX_RETRY reached");
                    return;
                }
                cache.removeAll();
                eventListener.reset();
            }
        });

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Producer p1 = new Producer(cache, p1Value, stopCondition, barrier);
        Producer p2 = new Producer(cache, p2Value, stopCondition, barrier);

        List<Future<Void>> futures = executorService.invokeAll(asList(p1, p2), 1, TimeUnit.MINUTES);

        for (Future<Void> future : futures) {
            assertThat(future.get(), nullValue());
        }

        assertThat(stopCondition.get(), is(true));
        assertThat(eventListener.putCount.get(), is(p1.batchPut + p2.batchPut));
    }

    @Test
    public void testMultiThreadedBulkOpsPutAndRemove() throws Exception{
        final Cache cache = new Cache("putRemoveCache", 1000000, true, false, 100000, 200000, false, 1);
        manager.addCache(cache);

        final TestCacheEventListener eventListener = new TestCacheEventListener();
        cache.getCacheEventNotificationService().registerListener(eventListener);

        final AtomicBoolean stopCondition = new AtomicBoolean(false);

        final AtomicInteger retryCount = new AtomicInteger(0);

        final CyclicBarrier barrier = new CyclicBarrier(2, new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Checking put/remove did interleave (run {})", retryCount.incrementAndGet());
                if (cache.getSize() != 0 && cache.getSize() != BATCH_SIZE) {
                    stopCondition.set(true);
                } else {
                    if (retryCount.get() >= MAX_RETRY) {
                        stopCondition.set(true);
                        LOGGER.warn("Could not interleave - stopping as MAX_RETRY reached");
                        return;
                    }
                    LOGGER.info("Cache size:" + cache.getSize());
                    cache.removeAll();
                    eventListener.reset();
                }
            }
        });

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Producer producer = new Producer(cache, "p1Value", stopCondition, barrier);
        Consumer consumer = new Consumer(cache, stopCondition, barrier);

        List<Future<Void>> futures = executorService.invokeAll(asList(producer, consumer), 1, TimeUnit.MINUTES);

        for (Future<Void> future : futures) {
            assertThat(future.get(), nullValue());
        }

        assertThat(stopCondition.get(), is(true));
        assertThat(eventListener.putCount.get(), is(producer.batchPut));
        assertThat(eventListener.elementsRemoved.size(), is(consumer.batchRemoved));

    }

    private static class Producer implements Callable<Void> {
        private final int batchPut = BATCH_SIZE;
        private final Cache cache;
        private final CyclicBarrier barrier;
        private final String value;
        private final AtomicBoolean stopCondition;

        public Producer(Cache cache, String value, AtomicBoolean stopCondition, CyclicBarrier barrier) {
            this.cache = cache;
            this.value = value;
            this.stopCondition = stopCondition;
            this.barrier = barrier;
        }

        @Override
        public Void call() throws Exception {
            while (!stopCondition.get()) {
                Set<Element> elements = new HashSet<Element>();
                for (int j = 0; j < batchPut; j++) {
                    elements.add(new Element("key" + j, value));
                }
                this.cache.putAll(elements);
                LOGGER.info("Producer done with run");
                barrier.await(1, TimeUnit.MINUTES);
            }
            return null;
        }
    }

    private static class Consumer implements Callable<Void>{
        private final int batchRemoved = BATCH_SIZE;
        private final Cache cache;
        private final AtomicBoolean stopCondition;
        private final CyclicBarrier barrier;

        public Consumer(Cache cache, AtomicBoolean stopCondition, CyclicBarrier barrier) {
            this.cache = cache;
            this.stopCondition = stopCondition;
            this.barrier = barrier;
        }

        @Override
        public Void call() throws Exception {
            while (!stopCondition.get()) {
                Set<String> elements = new HashSet<String>();
                for (int j = batchRemoved - 1; j >= 0; j--) {
                    elements.add("key" + j);
                }
                this.cache.removeAll(elements);
                LOGGER.info("Consumer done with run");
                barrier.await(1, TimeUnit.MINUTES);
            }
            return null;
        }
    }

    private static class TestCacheEventListener implements CacheEventListener{
        AtomicInteger putCount = new AtomicInteger(0);
        Set<Element> elementsPut = Collections.synchronizedSet(new HashSet<Element>());
        Set<Element> elementsUpdated = Collections.synchronizedSet(new HashSet<Element>());
        Set<Element> elementsRemoved = Collections.synchronizedSet(new HashSet<Element>());

        public void dispose() {
            // TODO Auto-generated method stub

        }

        public void notifyElementEvicted(Ehcache cache, Element element) {
            // TODO Auto-generated method stub

        }

        public void notifyElementExpired(Ehcache cache, Element element) {
            // TODO Auto-generated method stub

        }

        public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
            putCount.incrementAndGet();
            elementsPut.add(element);
        }

        public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
            elementsRemoved.add(element);
        }

        public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
            elementsUpdated.add(element);
        }

        public void notifyRemoveAll(Ehcache cache) {
            // TODO Auto-generated method stub
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public void reset() {
            elementsUpdated.clear();
            elementsPut.clear();
            putCount.set(0);

        }
    }
}
