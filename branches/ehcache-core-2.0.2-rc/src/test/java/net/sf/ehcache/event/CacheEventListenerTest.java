/**
 *  Copyright 2003-2009 Terracotta, Inc.
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
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the cache listener functionality
 *
 * @author Greg Luck
 * @version $Id$
 */
public class CacheEventListenerTest extends AbstractCacheTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheEventListenerTest.class.getName());


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
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        CountingCacheEventListener.resetCounters();
        manager.shutdown();
        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-countinglisteners.xml");
        cache = manager.getCache(cacheName);
        cache.removeAll();
    }


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        CountingCacheEventListener.resetCounters();
        super.tearDown();
    }


    /**
     * Tests the registration and unregistration of listeners
     */
    @Test
    public void testRegistration() {
        TestCacheEventListener listener = new TestCacheEventListener();

        int count = cache.getCacheEventNotificationService().getCacheEventListeners().size();
        cache.getCacheEventNotificationService().registerListener(listener);

        assertEquals(count + 1, cache.getCacheEventNotificationService().getCacheEventListeners().size());
        cache.getCacheEventNotificationService().unregisterListener(listener);

        assertEquals(count, cache.getCacheEventNotificationService().getCacheEventListeners().size());
    }


    /**
     * Tests the put listener.
     */
    @Test
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
    @Test
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
    @Test
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

        //An unsuccessful remove should notify
        cache.remove(key);
        notifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(2, notifications.size());

        //check for NPE
        cache.remove(null);

    }


    /**
     * Tests the eviction notifier.
     * sampleCache2 does not overflow, so an evict should trigger a notification
     */
    @Test
    public void testEvictNotificationsWhereNoOverflow() {

        Ehcache cache2 = manager.getCache("sampleCache2");

        //Put 11. 1 should be evicted
        Element element = null;
        for (int i = 0; i < 11; i++) {
            element = new Element("" + i, new Date());
            cache2.put(element);
        }

        List notifications = CountingCacheEventListener.getCacheElementsEvicted(cache2);
        assertEquals(1, notifications.size());
    }

    /**
     * Tests the eviction notifier.
     * sampleCache1 overflows, so the evict should overflow to disk and not trigger a notification
     */
    @Test
    public void testEvictNotificationsWhereOverflow() {

        Ehcache cache1 = manager.getCache("sampleCache1");

        //Put 11. 1 should be evicted
        Element element = null;
        for (int i = 0; i < 11; i++) {
            element = new Element("" + i, new Date());
            cache1.put(element);
        }

        List notifications = CountingCacheEventListener.getCacheElementsEvicted(cache1);
        assertEquals(0, notifications.size());
    }

    /**
     * Tests the removeAll notifier.
     */
    @Test
    public void testRemoveAllNotification() {

        Ehcache cache2 = manager.getCache("sampleCache2");

        //Put 11.
        Element element = null;
        for (int i = 0; i < 11; i++) {
            element = new Element("" + i, new Date());
            cache2.put(element);
        }

        List notifications = CountingCacheEventListener.getCacheRemoveAlls(cache2);
        assertEquals(0, notifications.size());

        //Remove all
        cache2.removeAll();
        notifications = CountingCacheEventListener.getCacheRemoveAlls(cache2);
        assertEquals(1, notifications.size());
    }


    /**
     * Tests the remove notifier where the element does not exist in the local cache.
     * Listener notification is required for correct operation of cluster invalidation.
     */
    @Test
    public void testRemoveNotificationWhereElementDidNotExist() {

        Serializable key = "1";

        //Don't Put
        //cache.put(element);

        //Check removal from MemoryStore
        cache.remove(key);


        List notifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(key, ((Element) notifications.get(0)).getKey());

        //An unsuccessful remove should notify
        cache.remove(key);
        notifications = CountingCacheEventListener.getCacheElementsRemoved(cache);
        assertEquals(2, notifications.size());

        //check for NPE
        cache.remove(null);

    }


    /**
     * Tests the expiry notifier. Check a reported scenario
     */
    @Test
    public void testExpiryNotifications() throws InterruptedException {

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        cache.getCacheEventNotificationService().registerListener(new TestCacheEventListener());

        //Put
        cache.put(element);

        //expire
        Thread.sleep(1999);

        //force expiry
        Element expiredElement = cache.get(key);
        assertEquals(null, expiredElement);

        //the TestCacheEventListener does a put of a new Element with the same key on expiry
        Element newElement = cache.get(key);
        assertEquals("set on notify", newElement.getValue());
        assertNotNull(newElement);

        //Check counting listener
        List notifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(element, notifications.get(0));
        assertEquals(1, notifications.size());

        //check for NPE
        cache.remove(null);

    }

    /**
     * Used to do work on notifyRemoved for the above test.
     */
    class TestCacheEventListener implements CacheEventListener {

        /**
         * {@inheritDoc}
         */
        public void notifyElementRemoved(final Ehcache cache, final Element element) throws CacheException {
            //
        }

        /**
         * Called immediately after an element has been put into the cache. The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
         * will block until this method returns.
         * <p/>
         * Implementers may wish to have access to the Element's fields, including value, so the element is provided.
         * Implementers should be careful not to modify the element. The effect of any modifications is undefined.
         *
         * @param cache   the cache emitting the notification
         * @param element the element which was just put into the cache.
         */
        public void notifyElementPut(final Ehcache cache, final Element element) throws CacheException {
            //
        }

        /**
         * Called immediately after an element has been put into the cache and the element already
         * existed in the cache. This is thus an update.
         * <p/>
         * The {@link net.sf.ehcache.Cache#put(net.sf.ehcache.Element)} method
         * will block until this method returns.
         * <p/>
         * Implementers may wish to have access to the Element's fields, including value, so the element is provided.
         * Implementers should be careful not to modify the element. The effect of any modifications is undefined.
         *
         * @param cache   the cache emitting the notification
         * @param element the element which was just put into the cache.
         */
        public void notifyElementUpdated(final Ehcache cache, final Element element) throws CacheException {
            //
        }

        /**
         * Called immediately after an element is <i>found</i> to be expired. The
         * {@link net.sf.ehcache.Cache#remove(Object)} method will block until this method returns.
         * <p/>
         * As the {@link net.sf.ehcache.Element} has been expired, only what was the key of the element is known.
         * <p/>
         * Elements are checked for expiry in ehcache at the following times:
         * <ul>
         * <li>When a get request is made
         * <li>When an element is spooled to the diskStore in accordance with a MemoryStore eviction policy
         * <li>In the DiskStore when the expiry thread runs, which by default is
         * {@link net.sf.ehcache.Cache#DEFAULT_EXPIRY_THREAD_INTERVAL_SECONDS}
         * </ul>
         * If an element is found to be expired, it is deleted and this method is notified.
         *
         * @param cache   the cache emitting the notification
         * @param element the element that has just expired
         *                <p/>
         *                Deadlock Warning: expiry will often come from the <code>DiskStore</code> expiry thread. It holds a lock to the
         *                DiskStorea the time the notification is sent. If the implementation of this method calls into a
         *                synchronized <code>Cache</code> method and that subsequently calls into DiskStore a deadlock will result.
         *                Accordingly implementers of this method should not call back into Cache.
         */
        public void notifyElementExpired(final Ehcache cache, final Element element) {
            cache.put(new Element(element.getKey(), "set on notify"));
        }

        /**
         * {@inheritDoc}
         */
        public void notifyElementEvicted(final Ehcache cache, final Element element) {
            //
        }

        /**
         * {@inheritDoc}
         */
        public void notifyRemoveAll(final Ehcache cache) {
            //
        }

        /**
         * Give the replicator a chance to cleanup and free resources when no longer needed
         */
        public void dispose() {
            //
        }

        /**
         * Creates and returns a copy of this object.  The precise meaning
         * of "copy" may depend on the class of the object. The general
         * intent is that, for any object <tt>x</tt>, the expression:
         * <blockquote>
         * <pre>
         * x.clone() != x</pre></blockquote>
         * will be true, and that the expression:
         * <blockquote>
         * <pre>
         * x.clone().getClass() == x.getClass()</pre></blockquote>
         * will be <tt>true</tt>, but these are not absolute requirements.
         * While it is typically the case that:
         * <blockquote>
         * <pre>
         * x.clone().equals(x)</pre></blockquote>
         * will be <tt>true</tt>, this is not an absolute requirement.
         * <p/>
         * By convention, the returned object should be obtained by calling
         * <tt>super.clone</tt>.  If a class and all of its superclasses (except
         * <tt>Object</tt>) obey this convention, it will be the case that
         * <tt>x.clone().getClass() == x.getClass()</tt>.
         * <p/>
         * By convention, the object returned by this method should be independent
         * of this object (which is being cloned).  To achieve this independence,
         * it may be necessary to modify one or more fields of the object returned
         * by <tt>super.clone</tt> before returning it.  Typically, this means
         * copying any mutable objects that comprise the internal "deep structure"
         * of the object being cloned and replacing the references to these
         * objects with references to the copies.  If a class contains only
         * primitive fields or references to immutable objects, then it is usually
         * the case that no fields in the object returned by <tt>super.clone</tt>
         * need to be modified.
         * <p/>
         * The method <tt>clone</tt> for class <tt>Object</tt> performs a
         * specific cloning operation. First, if the class of this object does
         * not implement the interface <tt>Cloneable</tt>, then a
         * <tt>CloneNotSupportedException</tt> is thrown. Note that all arrays
         * are considered to implement the interface <tt>Cloneable</tt>.
         * Otherwise, this method creates a new instance of the class of this
         * object and initializes all its fields with exactly the contents of
         * the corresponding fields of this object, as if by assignment; the
         * contents of the fields are not themselves cloned. Thus, this method
         * performs a "shallow copy" of this object, not a "deep copy" operation.
         * <p/>
         * The class <tt>Object</tt> does not itself implement the interface
         * <tt>Cloneable</tt>, so calling the <tt>clone</tt> method on an object
         * whose class is <tt>Object</tt> will result in throwing an
         * exception at run time.
         *
         * @return a clone of this instance.
         * @throws CloneNotSupportedException if the object's class does not
         *                                    support the <code>Cloneable</code> interface. Subclasses
         *                                    that override the <code>clone</code> method can also
         *                                    throw this exception to indicate that an instance cannot
         *                                    be cloned.
         * @see Cloneable
         */
        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically removed. This should
     * trigger an eviction notification.
     */
    @Test
    public void testEvictionFromLRUMemoryStoreNoExpiry() throws IOException, CacheException, InterruptedException {
        String sampleCache2 = "sampleCache2";
        cache = manager.getCache(sampleCache2);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }
        cache.put(new Element(11 + "", new Date()));
        List evictionNotifications = CountingCacheEventListener.getCacheElementsEvicted(cache);
        assertEquals(1, evictionNotifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(0, expiryNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically evicted. This should
     * trigger an eviction notification.
     */
    @Test
    public void testEvictionFromLRUMemoryStoreNotSerializable() throws IOException, CacheException, InterruptedException {
        String sampleCache1 = "sampleCache1";
        cache = manager.getCache(sampleCache1);
        cache.removeAll();

        //should trigger a removal notification because it is not Serializable when it is evicted
        cache.put(new Element(12 + "", new Object()));

        for (int i = 0; i < 10; i++) {
            // use non-serializable object for all values
            cache.put(new Element(i + "", new Object()));
            cache.get(i + "");
        }

        List evictionNotifications = CountingCacheEventListener.getCacheElementsEvicted(cache);
        assertEquals(1, evictionNotifications.size());

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
    @Test
    public void testEvictionFromLRUMemoryStoreExpiry() throws IOException, CacheException, InterruptedException {
        String sampleCache2 = "sampleCache2";
        cache = manager.getCache(sampleCache2);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        Thread.sleep(1999);
        cache.put(new Element(11 + "", new Date()));

        List removalNotifications = CountingCacheEventListener.getCacheElementsEvicted(cache);
        assertEquals(0, removalNotifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(1, expiryNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically evicted. This should
     * trigger a notification.
     */
    @Test
    public void testEvictionFromFIFOMemoryStoreNoExpiry() throws IOException, CacheException {
        String sampleCache3 = "sampleCache3";
        cache = manager.getCache(sampleCache3);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        cache.put(new Element(11 + "", new Date()));

        List removalNotifications = CountingCacheEventListener.getCacheElementsEvicted(cache);
        assertEquals(1, removalNotifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(0, expiryNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically evicted. This should
     * trigger a notification.
     * <p/>
     * If the element has expired, it should instead trigger an expiry notification.
     */
    @Test
    public void testEvictionFromFIFOMemoryStoreExpiry() throws IOException, CacheException, InterruptedException {
        String sampleCache3 = "sampleCache3";
        cache = manager.getCache(sampleCache3);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        Thread.sleep(1999);
        cache.put(new Element(11 + "", new Date()));

        List notifications = CountingCacheEventListener.getCacheElementsEvicted(cache);
        assertEquals(0, notifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(1, expiryNotifications.size());
    }

    /**
     * When the <code>MemoryStore</code> overflows, and there is no disk
     * store, then the element gets automatically evicted. This should
     * trigger a notification.
     */
    @Test
    public void testEvictionFromLFUMemoryStoreNoExpiry() throws IOException, CacheException {
        String sampleCache4 = "sampleCache4";
        cache = manager.getCache(sampleCache4);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        cache.put(new Element(11 + "", new Date()));

        List notifications = CountingCacheEventListener.getCacheElementsEvicted(cache);
        assertEquals(1, notifications.size());

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
    @Test
    public void testEvictionFromLFUMemoryStoreExpiry() throws IOException, CacheException, InterruptedException {
        String sampleCache4 = "sampleCache4";
        cache = manager.getCache(sampleCache4);
        cache.removeAll();
        for (int i = 0; i < 10; i++) {
            cache.put(new Element(i + "", new Date()));
        }

        Thread.sleep(1999);
        cache.put(new Element(11 + "", new Date()));

        List notifications = CountingCacheEventListener.getCacheElementsEvicted(cache);
        assertEquals(0, notifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(1, expiryNotifications.size());
    }


    /**
     * Tests expiry notification is hooked up to searchInMemoryStore
     *
     * @throws InterruptedException
     * @throws CacheException
     */
    @Test
    public void testExpiryViaMemoryStoreCheckingOnGet() throws InterruptedException, CacheException, IOException {

        cache.removeAll();
        CountingCacheEventListener.resetCounters();

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Check expiry from memory store in 1 second
        cache.put(element);
        Thread.sleep(1999);

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
    @Test
    public void testExpiryViaDiskStoreCheckingOnGet() throws InterruptedException, CacheException, IOException {
        //Overflow 10 elements to disk store
        for (int i = 0; i < 20; i++) {
            Element element = new Element("" + i, new Date());
            cache.put(element);
        }

        //Wait for expiry
        Thread.sleep(1999);

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
    @Test
    public void testExpiryViaDiskStoreExpiryThread() throws InterruptedException {
        //Overflow 10 elements to disk store
        for (int i = 0; i < 20; i++) {
            Element element = new Element("" + i, new Date());
            cache.put(element);
        }

        // Wait for expiry and expiry thread
        Thread.sleep(2999);

        List notifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        for (int i = 0; i < notifications.size(); i++) {
            Element element = (Element) notifications.get(i);
            element.getObjectKey();
        }
        assertEquals(10, notifications.size());

    }


    /**
     * Tests that elements evicted from disk are notified to any listeners.
     * fails from full ant builds?
     */
    public void xTestEvictionFromDiskStoreWithExpiry() throws IOException, CacheException, InterruptedException {

        cache.removeAll();
        //Overflow 10 elements to disk store which is maximum
        for (int i = 0; i < 20; i++) {
            Element element = new Element("" + i, new Date());
            cache.put(element);
        }
        cache.put(new Element(21 + "", new Date()));
        Thread.sleep(2999);

        List notifications = CountingCacheEventListener.getCacheElementsEvicted(cache);
        assertEquals(1, notifications.size());

        List expiryNotifications = CountingCacheEventListener.getCacheElementsExpired(cache);
        assertEquals(10, expiryNotifications.size());
    }


    /**
     * Test adding and removing of listeners while events are being notified
     */
    @Test
    public void testAddAndRemoveListenerConcurrency() throws Exception {

        final List executables = new ArrayList();

        for (int i = 0; i < 1; i++) {
            final Executable executable = new Executable() {
                public void execute() throws Exception {
                    try {
                        CacheEventListener listener = new TestCacheEventListener();
                        cache.getCacheEventNotificationService().registerListener(listener);
                        assertTrue(cache.getCacheEventNotificationService().unregisterListener(listener));

                    } catch (Throwable t) {
                        LOG.error("", t);
                        fail();
                    }
                }
            };
            executables.add(executable);
        }

        runThreads(executables);
    }


}
