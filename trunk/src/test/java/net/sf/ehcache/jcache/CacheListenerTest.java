/**
 *  Copyright 2003-2007 Greg Luck
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

package net.sf.ehcache.jcache;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheException;
import net.sf.jsr107cache.Cache;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests cache listeners, using the test package CountingCacheListener
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CacheListenerTest extends AbstractCacheTest {

    /**
     * setup test
     */
    protected void setUp() throws Exception {
        super.setUp();

    }

    /**
     * teardown
     * limits to what we can do here under jsr107
     */
    protected void tearDown() throws Exception {
        getTest1Cache().clear();
    }


    /**
     * Gets the sample cache 1
     * <cache name="sampleCache1"
     * maxElementsInMemory="10000"
     * maxElementsOnDisk="1000"
     * eternal="false"
     * timeToIdleSeconds="360"
     * timeToLiveSeconds="1000"
     * overflowToDisk="true"
     * memoryStoreEvictionPolicy="LRU">
     * <cacheEventListenerFactory class="net.sf.ehcache.event.NullCacheEventListenerFactory"/>
     * </cache>
     */
    protected Cache getTest1Cache() throws net.sf.jsr107cache.CacheException {
        Cache cache = net.sf.jsr107cache.CacheManager.getInstance().getCache("listenerTest1");
        if (cache == null) {
            //sampleCache1
            Map env = new HashMap();
            env.put("name", "test1");
            env.put("maxElementsInMemory", "10000");
            env.put("maxElementsOnDisk", "1000");
            env.put("memoryStoreEvictionPolicy", "LRU");
            env.put("overflowToDisk", "true");
            env.put("eternal", "false");
            env.put("timeToLiveSeconds", "1");
            env.put("timeToIdleSeconds", "1");
            env.put("diskPersistent", "false");
            env.put("diskExpiryThreadIntervalSeconds", "120");
            env.put("cacheLoaderFactoryClassName", "net.sf.ehcache.jcache.CountingCacheLoaderFactory");
            cache = net.sf.jsr107cache.CacheManager.getInstance().getCacheFactory().createCache(env);
            net.sf.jsr107cache.CacheManager.getInstance().registerCache("listenerTest1", cache);
        }
        return net.sf.jsr107cache.CacheManager.getInstance().getCache("listenerTest1");
    }

    /**
     * Tests the put listener.
     */
    public void testPutNotifications() throws net.sf.jsr107cache.CacheException {


        Cache cache = getTest1Cache();


        cache.put("1", new Date());


        CountingCacheListener countingCacheListener = new CountingCacheListener();
        cache.addListener(countingCacheListener);

        cache.put("2", new Date());

        List notifications = countingCacheListener.getCacheElementsPut();

        //The one put before we registered the listener should not have been received
        assertTrue(notifications.size() == 1);
        assertEquals("2", notifications.get(0));

        //A put which updates records as two puts, because JCache does not have an update notification
        cache.put("2", new Date());
        notifications = countingCacheListener.getCacheElementsPut();
        assertTrue(notifications.size() == 2);

        cache.removeListener(countingCacheListener);

        //Now put another value. It should not be received
        cache.put("3", new Date());
        notifications = countingCacheListener.getCacheElementsPut();
        assertTrue(notifications.size() == 2);


    }


    /**
     * Tests the remove notifier
     */
    public void testRemoveNotifications() throws net.sf.jsr107cache.CacheException {

        Serializable key = "1";
        Serializable value = new Date();

        Cache cache = getTest1Cache();
        CountingCacheListener countingCacheListener = new CountingCacheListener();
        cache.addListener(countingCacheListener);

        //Put
        cache.put(key, value);

        //Check removal from MemoryStore
        cache.remove(key);


        List notifications = countingCacheListener.getCacheElementsRemoved();
        assertEquals(key, notifications.get(0));

        //An unsuccessful remove should notify
        cache.remove(key);
        notifications = countingCacheListener.getCacheElementsRemoved();
        assertEquals(2, notifications.size());

        //check for NPE
        cache.remove(null);

    }

    /**
     * Tests the expiry notifier. These are mapped to evictions in the JCache adaptor.
     */
    public void testExpiryNotifications() throws InterruptedException, net.sf.jsr107cache.CacheException {

        Serializable key = "1";
        Serializable value = new Date();

        Cache cache = getTest1Cache();
        CountingCacheListener countingCacheListener = new CountingCacheListener();
        cache.addListener(countingCacheListener);

        //Put
        cache.put(key, value);

        //expire
        Thread.sleep(1020);

        //force expiry
        Object expired = cache.get(key);
        assertEquals(null, expired);

        //Check counting listener
        List notifications = countingCacheListener.getCacheElementsEvicted();
        assertEquals(1, notifications.size());

        //check for NPE
        cache.remove(null);

    }


    /**
     * Tests the eviction notifier.
     * sampleCache2 does not overflow, so an evict should trigger a notification
     */
    public void testEvictNotificationsWhereNoOverflow() {

        JCache cache2 = new JCache(manager.getCache("sampleCache2"), null);
        CountingCacheListener countingCacheListener = new CountingCacheListener();
        cache2.addListener(countingCacheListener);

        //1 should be evicted
        for (int i = 0; i < 1001; i++) {
            cache2.put("" + i, new Date());
        }

        List notifications = countingCacheListener.getCacheElementsEvicted();
        assertEquals(1, notifications.size());
    }

    /**
     * Tests the eviction notifier.
     * sampleCache1 overflows, so the evict should overflow to disk and not trigger a notification
     */
    public void testEvictNotificationsWhereOverflow() {


        JCache cache1 = new JCache(manager.getCache("sampleCache1"), null);
        CountingCacheListener countingCacheListener = new CountingCacheListener();
        cache1.addListener(countingCacheListener);

        //1 should be evicted
        for (int i = 0; i < 10001; i++) {
            cache1.put("" + i, new Date());
        }

        List notifications = countingCacheListener.getCacheElementsEvicted();
        assertEquals(0, notifications.size());
    }

    /**
     * Tests the removeAll notifier.
     */
    public void testClearNotification() {

        JCache cache2 = new JCache(manager.getCache("sampleCache2"), null);
        CountingCacheListener countingCacheListener = new CountingCacheListener();
        cache2.addListener(countingCacheListener);

        //Put 11.
        for (int i = 0; i < 11; i++) {
            cache2.put("" + i, new Date());
        }

        List notifications = countingCacheListener.getCacheRemoveAlls();
        assertEquals(0, notifications.size());

        //Remove all
        cache2.clear();
        notifications = countingCacheListener.getCacheRemoveAlls();
        assertEquals(1, notifications.size());
    }


    /**
     * Tests the remove notifier where the element does not exist in the local cache.
     * Listener notification is required for correct operation of cluster invalidation.
     */
    public void testRemoveNotificationWhereElementDidNotExist() throws net.sf.jsr107cache.CacheException {

        Serializable key = "1";

        Cache cache = getTest1Cache();
        CountingCacheListener countingCacheListener = new CountingCacheListener();
        cache.addListener(countingCacheListener);

        //Don't Put
        //cache.put(element);

        //Check removal from MemoryStore
        cache.remove(key);


        List notifications = countingCacheListener.getCacheElementsRemoved();
        assertEquals(key, notifications.get(0));

        //An unsuccessful remove should notify
        cache.remove(key);
        notifications = countingCacheListener.getCacheElementsRemoved();
        assertEquals(2, notifications.size());

        //check for NPE
        cache.remove(null);

    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically evicted. This should
     * trigger an eviction notification.
     */
    public void testEvictionFromLRUMemoryStoreNotSerializable() throws IOException, CacheException, InterruptedException {
        String sampleCache1 = "sampleCache1";
        Cache cache = new JCache(manager.getCache(sampleCache1), null);
        cache.clear();
        CountingCacheListener countingCacheListener = new CountingCacheListener();
        cache.addListener(countingCacheListener);

        //should trigger a removal notification because it is not Serializable and will be evicted
        cache.put("non-serializable", new Object());

        for (int i = 0; i < 10000; i++) {
            cache.put(i + "", new Date());
        }

        List evictionNotifications = countingCacheListener.getCacheElementsEvicted();
        assertEquals(1, evictionNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically removed. This should
     * trigger a remove notification.
     * <p/>
     * If the element has expired, it should not trigger an eviction notification.
     */
    public void testEvictionFromLRUMemoryStoreExpiry() throws IOException, CacheException, InterruptedException {
        String sampleCache2 = "sampleCache1";
        Cache cache = new JCache(manager.getCache(sampleCache2), null);
        cache.clear();
        CountingCacheListener countingCacheListener = new CountingCacheListener();
        cache.addListener(countingCacheListener);
        for (int i = 0; i < 10000; i++) {
            cache.put(i + "", new Date());
        }

        Thread.sleep(1030);
        cache.put("new", new Date());

        List removalNotifications = countingCacheListener.getCacheElementsEvicted();
        assertEquals(0, removalNotifications.size());

    }

}
