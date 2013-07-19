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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

@Category(CheckShorts.class)
public class BulkOpsEventListenerTest extends AbstractCacheTest {

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

        Set keySet1 = new HashSet<String>();
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
        Set keySet2 = new HashSet<String>();
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
            removedKeySet.add(element.getKey().toString());
        }
        assertEquals(keySet2, removedKeySet);

        for(Object key : keySet2){
            assertNull(cache.get(key));
        }

        cache.removeAll();
        assertEquals(0, cache.getSize());
    }

    @Test
    public void testMultiThreadedBulkOps() throws InterruptedException{
        Cache cache = new Cache("cache", 1000000, true, false, 100000, 200000, false, 1);
        manager.addCache(cache);

        TestCacheEventListener eventListener = new TestCacheEventListener();
        cache.getCacheEventNotificationService().registerListener(eventListener);

        Producer p1 = new Producer(cache, 0, 3 * 60 * 1000);
        Producer p2 = new Producer(cache, 1000000, 3 * 60 * 1000);
        Thread[] th = new Thread[4];
        th[0] = new Thread(p1, "p1");
        th[1] = new Thread(p2, "p2");
        th[0].start();
        th[1].start();

        Consumer c1 = new Consumer(cache, 0, 2 * 60 * 1000);
        Consumer c2 = new Consumer(cache, 1000000, 2 * 60 * 1000);
        th[2] = new Thread(c1, "c1");
        th[3] = new Thread(c2, "c2");

        Thread.sleep(10000);
        th[2].start();
        th[3].start();

        for(Thread t : th){
            t.join();
        }

        assertEquals(p1.numPuts.intValue() + p2.numPuts.intValue(), eventListener.elementsPut.size());
        assertEquals(c1.numRemoved.intValue() + c2.numRemoved.intValue(), eventListener.elementsRemoved.size());
    }

    private static class Producer implements Runnable {
        private final AtomicInteger numPuts = new AtomicInteger(0);
        private final Cache cache;
        private final int startIndex;
        private final int timeToRunMills;
        private final long startTime = System.currentTimeMillis();

        public Producer(Cache cache, int start, int timeToRunMills) {
            this.cache = cache;
            this.startIndex = start;
            this.timeToRunMills = timeToRunMills;
        }

        public void run() {
            int i = startIndex;
            Random rand = new Random();
            while(System.currentTimeMillis() - startTime <= timeToRunMills){
                int batch = rand.nextInt(100);
                Set<Element> elements = new HashSet<Element>();
                for(int j = 0; j < batch; j++){
                    elements.add(new Element("key" + i, "value" + i));
                    i++;
                }
                this.cache.putAll(elements);
                numPuts.addAndGet(batch);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class Consumer implements Runnable{
        private final AtomicInteger numRemoved = new AtomicInteger(0);
        private final Cache cache;
        private final int startIndex;
        private final int timeToRunMills;
        private final long startTime = System.currentTimeMillis();

        public Consumer(Cache cache, int start, int timeToRunMills) {
            this.cache = cache;
            this.startIndex = start;
            this.timeToRunMills = timeToRunMills;
        }

        public void run() {
            int i = startIndex;
            Random rand = new Random();
            while(System.currentTimeMillis() - startTime <= timeToRunMills){
                int batch = rand.nextInt(100);
                Set elements = new HashSet<String>();
                for(int j = 0; j < batch; j++){
                    elements.add("key" + i);
                    i++;
                }
                this.cache.removeAll(elements);
                numRemoved.addAndGet(batch);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class TestCacheEventListener implements CacheEventListener{
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

    }
}
