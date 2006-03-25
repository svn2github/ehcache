/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.event;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.AbstractCacheTest;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Tests the cache listener functionality
 *
 * @author Greg Luck
 * @version $Id: CacheEventListenerTest.java,v 1.2 2006/03/25 04:06:11 gregluck Exp $
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
    protected Cache cache;

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
        assertEquals(key, ((Element) notifications.get(0)).getKey());
        assertEquals(element.getValue(), ((Element) notifications.get(0)).getValue());

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
        assertEquals("0", ((Element) notifications.get(0)).getKey());
        assertEquals("0", ((Element) notifications.get(0)).getValue());

        notifications = CountingCacheEventListener.getCacheElementsUpdated(cache);
        assertTrue(notifications.size() == 11);
        assertEquals("0", ((Element) notifications.get(0)).getKey());
        assertEquals("0", ((Element) notifications.get(0)).getValue());


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
            element.getKey();
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
            element.getKey();
        }
        assertEquals(10, notifications.size());

    }

}
