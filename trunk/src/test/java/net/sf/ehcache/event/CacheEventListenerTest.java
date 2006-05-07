/**
 *  Copyright 2003-2006 Greg Luck
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

import junit.framework.TestCase;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Tests the cache listener functionality
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CacheEventListenerTest extends TestCase {

    /**
     * manager
     */
    protected CacheManager manager;
    /**
     * the cache name we wish to test
     */
    protected String cacheName = "sampleCache1";
    /**
     * the cache we wish to test
     */
    protected Ehcache cache;

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        CountingCacheEventListener.resetCounters();
        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-countinglisteners.xml");
        cache = manager.getCache(cacheName);
        cache.removeAll();
    }


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        CountingCacheEventListener.resetCounters();
        manager.shutdown();
    }

    


    /**
     * Tests the put listener.
     */
    public void testPutNotifications() {

        Serializable key = new Date();
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache.put(element);

        List notifications = CountingCacheEventListener.getCacheElementsPut(cache);

        assertTrue(notifications.size() == 1);
        assertEquals(key, ((Element) notifications.get(0)).getObjectKey());
        assertEquals(element.getObjectValue(), ((Element) notifications.get(0)).getObjectValue());

        //A put which updates records as one put, because the second one is an update
        cache.put(element);
        notifications = CountingCacheEventListener.getCacheElementsPut(cache);
        assertTrue(notifications.size() == 1);

    }

    /**
     * Tests the put and update listeners.
     */
    public void testUpdateNotifications() {

        //Put and update
        for (int i = 0; i < 11; i++) {
            cache.put(new Element("" + i, "" + i));
            cache.put(new Element("" + i, "" + i));
        }

        //Put with no update
        cache.put(new Element("20", "20"));


        //Should get 12 puts and 11 updates
        List notifications = CountingCacheEventListener.getCacheElementsPut(cache);
        assertTrue(notifications.size() == 12);
        assertEquals("0", ((Element) notifications.get(0)).getObjectKey());
        assertEquals("0", ((Element) notifications.get(0)).getObjectValue());

        notifications = CountingCacheEventListener.getCacheElementsUpdated(cache);
        assertTrue(notifications.size() == 11);
        assertEquals("0", ((Element) notifications.get(0)).getObjectKey());
        assertEquals("0", ((Element) notifications.get(0)).getObjectValue());


    }

    /**
     * Tests the remove notifier
     */
    public void testRemoveNotifications() {

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache.put(element);

        //Check removal from MemoryStore
        cache.remove(key);


        List notifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(element, notifications.get(0));

        //An unsuccessful remove should not notify
        cache.remove(key);
        notifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(1, notifications.size());

        //check for NPE
        cache.remove(null);

    }

    /**
     * removeAll tests  - with listener
     * 20 is enough to test this from Memory and Disk stores
     */
    public void testRemoveAllNotificationsWithListener() throws IOException {
        for (int i = 0; i < 20; i++) {
            cache.put(new Element("" + i, new Date()));
        }

        cache.removeAll();
        List notifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(20, notifications.size());
    }

    /**
     * removeAll tests  - with listener. Time the elements out first.
     * 20 is enough to test this from Memory and Disk stores
     */
    public void testRemoveAllNotificationsWithListenerExpiry() throws IOException, InterruptedException {
        for (int i = 0; i < 20; i++) {
            cache.put(new Element("" + i, new Date()));
        }
        Thread.sleep(1010);
        cache.removeAll();
        List notifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(20, notifications.size());
        notifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(0, notifications.size());
    }

    /**
     * removeAll tests - no listener. Checks no removal notifications
     * 20 is enough to test this from Memory and Disk stores
     */
    public void testRemoveAllNotificationsNoListener() throws IOException {
        for (int i = 0; i < 20; i++) {
            cache.put(new Element("" + i, new Date()));
        }

        Object[] cacheEventListeners = cache.getCacheEventNotificationService().getCacheEventListeners().toArray();
        for (int i = 0; i < cacheEventListeners.length; i++) {
            CacheEventListener cacheEventListener = (CacheEventListener) cacheEventListeners[i];
            cache.getCacheEventNotificationService().unregisterListener(cacheEventListener);
        }

        cache.removeAll();
        List notifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(0, notifications.size());
    }

    /**
     * removeAll tests - no listener. Checks
     * 20 is enough to test this from Memory and Disk stores
     */
    public void testRemoveAllNotificationsNoListenerExpiry() throws IOException, InterruptedException {
        for (int i = 0; i < 20; i++) {
            cache.put(new Element("" + i, new Date()));
        }

        Object[] cacheEventListeners = cache.getCacheEventNotificationService().getCacheEventListeners().toArray();
        for (int i = 0; i < cacheEventListeners.length; i++) {
            CacheEventListener cacheEventListener = (CacheEventListener) cacheEventListeners[i];
            cache.getCacheEventNotificationService().unregisterListener(cacheEventListener);
        }
        Thread.sleep(1010);
        cache.removeAll();
        List notifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(0, notifications.size());
        notifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(0, notifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically removed. This should
     * trigger a remove notification.
     */
    public void testEvictionFromLRUMemoryStoreNoExpiry() throws IOException, CacheException, InterruptedException {
        String sampleCache2 = "sampleCache2";
        cache = manager.getCache(sampleCache2);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }
        cache.put(new Element(11 + "", new Date()));
        List removalNotifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(1, removalNotifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(0, expiryNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically removed. This should
     * trigger a remove notification.
     */
    public void testEvictionFromLRUMemoryStoreNotSerializable() throws IOException, CacheException, InterruptedException {
        String sampleCache1 = "sampleCache1";
        cache = manager.getCache(sampleCache1);
        cache.removeAll();

        //should trigger a removal notification because it is not Serializable and will be evicted
        cache.put(new Element(12 + "", new Object()));

        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        List removalNotifications2 = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(1, removalNotifications2.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(0, expiryNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically removed. This should
     * trigger a remove notification.
     * <p/>
     * If the element has expired, it should instead trigger an expiry notification.
     */
    public void testEvictionFromLRUMemoryStoreExpiry() throws IOException, CacheException, InterruptedException {
        String sampleCache2 = "sampleCache2";
        cache = manager.getCache(sampleCache2);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        Thread.sleep(1030);
        cache.put(new Element(11 + "", new Date()));

        List removalNotifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(0, removalNotifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(1, expiryNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically removed. This should
     * trigger a notification.
     */
    public void testEvictionFromFIFOMemoryStoreNoExpiry() throws IOException, CacheException {
        String sampleCache3 = "sampleCache3";
        cache = manager.getCache(sampleCache3);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        cache.put(new Element(11 + "", new Date()));

        List removalNotifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(1, removalNotifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(0, expiryNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically removed. This should
     * trigger a notification.
     * <p/>
     * If the element has expired, it should instead trigger an expiry notification.
     */
    public void testEvictionFromFIFOMemoryStoreExpiry() throws IOException, CacheException, InterruptedException {
        String sampleCache3 = "sampleCache3";
        cache = manager.getCache(sampleCache3);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        Thread.sleep(1001);
        cache.put(new Element(11 + "", new Date()));

        List removalNotifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(0, removalNotifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(1, expiryNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically removed. This should
     * trigger a notification.
     */
    public void testEvictionFromLFUMemoryStoreNoExpiry() throws IOException, CacheException {
        String sampleCache4 = "sampleCache4";
        cache = manager.getCache(sampleCache4);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        cache.put(new Element(11 + "", new Date()));

        List removalNotifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(1, removalNotifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(0, expiryNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically removed. This should
     * trigger a notification.
     * <p/>
     * If the element has expired, it should instead trigger an expiry notification.
     */
    public void testEvictionFromLFUMemoryStoreExpiry() throws IOException, CacheException, InterruptedException {
        String sampleCache4 = "sampleCache4";
        cache = manager.getCache(sampleCache4);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        Thread.sleep(1001);
        cache.put(new Element(11 + "", new Date()));

        List removalNotifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(0, removalNotifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(1, expiryNotifications.size());
    }


    /**
     * Tests expiry notification is hooked up to searchInMemoryStore
     *
     * @throws InterruptedException
     * @throws CacheException
     */
    public void testExpiryViaMemoryStoreCheckingOnGet() throws InterruptedException, CacheException, IOException {

        cache.removeAll();
        CountingCacheEventListener.resetCounters();

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Check expiry from memory store in 1 second
        cache.put(element);
        Thread.sleep(1001);

        //Trigger expiry
        cache.get(key);
        List notifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(1, notifications.size());
        assertEquals(element, notifications.get(0));
    }

    /**
     * Tests expiry notification is hooked up to searchInDiskStore.
     * This test is not exact, because the expiry thread may also expire some.
     *
     * @throws InterruptedException
     * @throws CacheException
     */
    public void testExpiryViaDiskStoreCheckingOnGet() throws InterruptedException, CacheException, IOException {
        //Overflow 10 elements to disk store
        for (int i = 0; i < 20; i++) {
            Element element = new Element("" + i, new Date());
            cache.put(element);
        }

        //Wait for expiry
        Thread.sleep(1001);

        //Trigger expiry
        for (int i = 0; i < 20; i++) {
            cache.get("" + i);
        }

        List notifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        for (int i = 0; i < notifications.size(); i++) {
            Element element = (Element) notifications.get(i);
            element.getObjectKey();
        }
        assertTrue(notifications.size() >= 10);
    }


    /**
     * Tests expiry thread expiry
     *
     * @throws InterruptedException
     */
    public void testExpiryViaDiskStoreExpiryThread() throws InterruptedException {
        //Overflow 10 elements to disk store
        for (int i = 0; i < 20; i++) {
            Element element = new Element("" + i, new Date());
            cache.put(element);
        }

        //Wait for expiry and expiry thread
        Thread.sleep(2050);

        List notifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        for (int i = 0; i < notifications.size(); i++) {
            Element element = (Element) notifications.get(i);
            element.getObjectKey();
        }
        assertEquals(10, notifications.size());

    }

}
