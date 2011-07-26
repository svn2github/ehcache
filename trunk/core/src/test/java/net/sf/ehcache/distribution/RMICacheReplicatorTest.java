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

package net.sf.ehcache.distribution;

import static net.sf.ehcache.util.RetryAssert.assertBy;
import static net.sf.ehcache.util.RetryAssert.elementAt;
import static net.sf.ehcache.util.RetryAssert.sizeOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.ThreadKiller;
import net.sf.ehcache.event.CountingCacheEventListener;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests replication of Cache events
 * <p/>
 * Note these tests need a live network interface running in multicast mode to work
 * <p/>
 * If running involving RMIAsynchronousCacheReplicator individually the test will fail because
 * the VM will gobble up the SoftReferences rather than allocating more memory. Uncomment the
 * forceVMGrowth() method usage in setup.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class RMICacheReplicatorTest extends AbstractRMITest {

    @BeforeClass
    public static void enableHeapDump() {
        setHeapDumpOnOutOfMemoryError(true);
    }

    @AfterClass
    public static void disableHeapDump() {
        setHeapDumpOnOutOfMemoryError(false);
    }

    /**
     * A value to represent replicate asynchronously
     */
    protected static final boolean ASYNCHRONOUS = true;

    /**
     * A value to represent replicate synchronously
     */
    protected static final boolean SYNCHRONOUS = false;

    private static final Logger LOG = LoggerFactory.getLogger(RMICacheReplicatorTest.class.getName());


    /**
     * CacheManager 1 in the cluster
     */
    protected CacheManager manager1;
    /**
     * CacheManager 2 in the cluster
     */
    protected CacheManager manager2;
    /**
     * CacheManager 3 in the cluster
     */
    protected CacheManager manager3;
    /**
     * CacheManager 4 in the cluster
     */
    protected CacheManager manager4;
    /**
     * CacheManager 5 in the cluster
     */
    protected CacheManager manager5;
    /**
     * CacheManager 6 in the cluster
     */
    protected CacheManager manager6;

    /**
     * The name of the cache under test
     */
    protected String cacheName = "sampleCache1";
    /**
     * CacheManager 1 of 2s cache being replicated
     */
    protected Ehcache cache1;

    /**
     * CacheManager 2 of 2s cache being replicated
     */
    protected Ehcache cache2;

    /**
     * Allows setup to be the same
     */
    protected String cacheNameBase = "ehcache-distributed";

    /**
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        Assume.assumeThat(getActiveReplicationThreads(), IsEmptyCollection.<Thread>empty());

        //Required to get SoftReference tests to pass. The VM clean up SoftReferences rather than allocating
        // memory to -Xmx!
//        forceVMGrowth();
//        System.gc();
        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);

        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed2.xml");
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml");
        manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed4.xml");
        manager5 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed5.xml");

        //manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi6.xml");

        //allow cluster to be established
        waitForClusterMembership(10, TimeUnit.SECONDS, Collections.singleton(cacheName), manager1, manager2, manager3, manager4, manager5);

        manager1.getCache(cacheName).put(new Element("setup", "setup"));
        for (CacheManager manager : new CacheManager[] {manager1, manager2, manager3, manager4, manager5}) {
            assertBy(10, TimeUnit.SECONDS, elementAt(manager.getCache(cacheName), "setup"), notNullValue());
        }

        manager1.getCache(cacheName).removeAll();
        for (CacheManager manager : new CacheManager[] {manager1, manager2, manager3, manager4, manager5}) {
            assertBy(10, TimeUnit.SECONDS, sizeOf(manager.getCache(cacheName)), is(0));
        }

        CountingCacheEventListener.resetCounters();
        cache1 = manager1.getCache(cacheName);
        cache2 = manager2.getCache(cacheName);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {

        if (manager1 != null) {
            manager1.shutdown();
        }
        if (manager2 != null) {
            manager2.shutdown();
        }
        if (manager3 != null) {
            manager3.shutdown();
        }
        if (manager4 != null) {
            manager4.shutdown();
        }
        if (manager5 != null) {
            manager5.shutdown();
        }
        if (manager6 != null) {
            manager6.shutdown();
        }

        RetryAssert.assertBy(30, TimeUnit.SECONDS, new Callable<Set<Thread>>() {
            public Set<Thread> call() throws Exception {
                return getActiveReplicationThreads();
            }
        }, IsEmptyCollection.<Thread>empty());
    }

    @Test
    public void testCASOperationsNotSupported() throws Exception {
        LOG.info("START TEST");

        final Ehcache cache1 = manager1.getEhcache(cacheName);
        final Ehcache cache2 = manager2.getEhcache(cacheName);
        final Ehcache cache3 = manager3.getEhcache(cacheName);
        final Ehcache cache4 = manager4.getEhcache(cacheName);

        try {
            cache1.putIfAbsent(new Element("foo", "poo"));
            throw new AssertionError("CAS operation should have failed.");
        } catch (CacheException ce) {
            assertEquals(true, ce.getMessage().contains("CAS"));
        }

        try {
            cache2.removeElement(new Element("foo", "poo"));
            throw new AssertionError("CAS operation should have failed.");
        } catch (CacheException ce) {
            assertEquals(true, ce.getMessage().contains("CAS"));
        }

        try {
            cache3.replace(new Element("foo", "poo"));
            throw new AssertionError("CAS operation should have failed.");
        } catch (CacheException ce) {
            assertEquals(true, ce.getMessage().contains("CAS"));
        }

        try {
            cache4.replace(new Element("foo", "poo"), new Element("foo", "poo2"));
            throw new AssertionError("CAS operation should have failed.");
        } catch (CacheException ce) {
            assertEquals(true, ce.getMessage().contains("CAS"));
        }

        LOG.info("END TEST");
    }

    /**
     * Does a new cache manager in the cluster get detected?
     */
    @Test
    public void testRemoteCachePeersDetectsNewCacheManager() throws InterruptedException {
        //Add new CacheManager to cluster
        manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed6.xml");

        //Allow detection to occur
        waitForClusterMembership(10020, TimeUnit.MILLISECONDS, Collections.singleton(cache1.getName()), manager1, manager2, manager3, manager4, manager5, manager6);
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    @Test
    public void testRemoteCachePeersDetectsDownCacheManager() throws InterruptedException {
        //Drop a CacheManager from the cluster
        manager5.shutdown();

        //Allow change detection to occur. Heartbeat 1 second and is not stale until 5000
        waitForClusterMembership(11020, TimeUnit.MILLISECONDS, Collections.singleton(cache1.getName()), manager1, manager2, manager3, manager4);
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    @Test
    public void testRemoteCachePeersDetectsDownCacheManagerSlow() throws InterruptedException {
        try {
            CacheManagerPeerProvider provider = manager1.getCacheManagerPeerProvider("RMI");
            List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
            assertEquals(4, remotePeersOfCache1.size());

            MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(2000);
            Thread.sleep(2000);

            //Drop a CacheManager from the cluster
            manager5.shutdown();

            //Insufficient time for it to timeout
            remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
            assertEquals(4, remotePeersOfCache1.size());
        } finally {
            MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);
            Thread.sleep(2000);
        }
    }

    /**
     * Tests put and remove initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testPutPropagatesFromAndToEveryCacheManagerAndCache() throws CacheException, InterruptedException {

        //Put
        final String[] cacheNames = manager1.getCacheNames();
        Arrays.sort(cacheNames);
        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            manager1.getCache(name).put(new Element(Integer.toString(i), Integer.valueOf(i)));
            //Add some non serializable elements that should not get propagated
            manager1.getCache(name).put(new Element("nonSerializable" + i, new Object()));
        }

        assertBy(10, TimeUnit.SECONDS, new Callable<Boolean>() {

            public Boolean call() throws Exception {
                for (int i = 0; i < cacheNames.length; i++) {
                    String name = cacheNames[i];
                    if (manager1.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(manager1.getCache(name)).isEmpty()) {
                        continue;
                    }
                    if ("sampleCache2".equals(name)) {
                        //sampleCache2 in manager1 replicates puts via invalidate, so the count will be 1 less
                        for (CacheManager manager : new CacheManager[] {manager2, manager3, manager4, manager5}) {
                            assertNull(manager.getCache(name).get(Integer.toString(i)));
                            assertNull(manager.getCache(name).get("nonSerializable" + i));
                        }
                    } else {
                        for (CacheManager manager : new CacheManager[] {manager2, manager3, manager4, manager5}) {
                            assertNotNull("Cache : " + name, manager.getCache(name).get(Integer.toString(i)));
                            assertNull(manager.getCache(name).get("nonSerializable" + i));
                        }
                    }
                }
                return Boolean.TRUE;
            }
        }, is(Boolean.TRUE));
    }

    /**
     * Tests what happens when a CacheManager in the cluster comes and goes. In ehcache-1.2.4 this would cause the new RMI CachePeers in the CacheManager to
     * be permanently corrupt.
     */
    @Test
    public void testPutPropagatesFromAndToEveryCacheManagerAndCacheDirty() throws CacheException, InterruptedException {

        manager3.shutdown();
        waitForClusterMembership(11020, TimeUnit.MILLISECONDS, Collections.singleton(cacheName), manager1, manager2, manager4, manager5);

        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml");
        waitForClusterMembership(11020, TimeUnit.MILLISECONDS, Collections.singleton(cacheName), manager1, manager2, manager3, manager4, manager5);

        //Put
        final String[] cacheNames = manager1.getCacheNames();
        Arrays.sort(cacheNames);
        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            manager1.getCache(name).put(new Element(Integer.toString(i), Integer.valueOf(i)));
            //Add some non serializable elements that should not get propagated
            manager1.getCache(name).put(new Element("nonSerializable" + i, new Object()));
        }

        assertBy(10, TimeUnit.SECONDS, new Callable<Boolean>() {

            public Boolean call() throws Exception {
                for (int i = 0; i < cacheNames.length; i++) {
                    String name = cacheNames[i];
                    if (manager1.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(manager1.getCache(name)).isEmpty()) {
                        continue;
                    }
                    if ("sampleCache2".equals(name)) {
                        //sampleCache2 in manager1 replicates puts via invalidate, so the count will be 1 less
                        for (CacheManager manager : new CacheManager[] {manager2, manager3, manager4, manager5}) {
                            assertNull(manager2.getCache(name).get(Integer.toString(i)));
                            assertNull(manager2.getCache(name).get("nonSerializable" + i));
                        }
                    } else {
                        for (CacheManager manager : new CacheManager[] {manager2, manager3, manager4, manager5}) {
                            assertNotNull(manager2.getCache(name).get(Integer.toString(i)));
                            assertNull(manager2.getCache(name).get("nonSerializable" + i));
                        }
                    }
                }
                return Boolean.TRUE;
            }
        }, is(Boolean.TRUE));
    }

    /**
     * manager1 adds a replicating cache, then manager2 and so on. Then we remove one. Does everything work as expected?
     */
    @Test
    public void testPutWithNewCacheAddedProgressively() throws InterruptedException {

        manager1.addCache("progressiveAddCache");
        manager2.addCache("progressiveAddCache");

        //The cluster will not have formed yet, so it will fail
        try {
            putTest(manager1.getCache("progressiveAddCache"), manager2.getCache("progressiveAddCache"), ASYNCHRONOUS);
            fail();
        } catch (AssertionError e) {
            //expected
        }

        //The cluster will now have formed yet, so it will succeed
        putTest(manager1.getCache("progressiveAddCache"), manager2.getCache("progressiveAddCache"), ASYNCHRONOUS);

        Cache secondCache = manager2.getCache("progressiveAddCache");

        //The second peer disappears. The test will fail.
        manager2.removeCache("progressiveAddCache");
        try {
            putTest(manager1.getCache("progressiveAddCache"), secondCache, ASYNCHRONOUS);
            fail();
        } catch (IllegalStateException e) {
            //The second cache will not alive. Expected. But no other exception is caught and this will otherwise fail.

        }


    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testPutWithExplicitReplicationConfig() throws InterruptedException {

        putTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testPutWithThreadKiller() throws InterruptedException {

        putTestWithThreadKiller(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * CacheEventListeners that are not CacheReplicators should receive cache events originated from receipt
     * of a remote event by a CachePeer.
     */

    @Test
    public void testRemotelyReceivedPutNotifiesCountingListener() throws InterruptedException {

        putTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
        assertEquals(1, CountingCacheEventListener.getCacheElementsPut(manager1.getCache("sampleCache1")).size());
        assertEquals(1, CountingCacheEventListener.getCacheElementsPut(manager2.getCache("sampleCache1")).size());

    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=false, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testPutWithExplicitReplicationSynchronousConfig() throws InterruptedException {
        putTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testPutWithEmptyReplicationPropertiesConfig() throws InterruptedException {
        putTest(manager1.getCache("sampleCache4"), manager2.getCache("sampleCache4"), ASYNCHRONOUS);
    }

    /**
     * Test put replicated for cache4 - missing replicatePuts property.
     * replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     * should equal replicateAsynchronously=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testPutWithOneMissingReplicationPropertyConfig() throws InterruptedException {
        putTest(manager1.getCache("sampleCache5"), manager2.getCache("sampleCache5"), ASYNCHRONOUS);
    }


    /**
     * Tests put and remove initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void putTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws CacheException, InterruptedException {

        Serializable key = new Date();
        Serializable value = new Date();
        Element sourceElement = new Element(key, value);

        //Put
        fromCache.put(sourceElement);
        int i = 0;

        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to toCache.
        Element deliveredElement = toCache.get(key);
        assertEquals(sourceElement, deliveredElement);

    }


    /**
     * Tests put and remove initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void putTestWithThreadKiller(Ehcache fromCache, Ehcache toCache, boolean asynchronous)
            throws CacheException, InterruptedException {

        fromCache.put(new Element("thread killer", new ThreadKiller()));
        if (asynchronous) {
            waitForPropagate();
        }

        Serializable key = new Date();
        Serializable value = new Date();
        Element sourceElement = new Element(key, value);

        //Put
        fromCache.put(sourceElement);

        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to toCache.
        Element deliveredElement = toCache.get(key);
        assertEquals(sourceElement, deliveredElement);

    }


    /**
     * Checks that a put received from a remote cache notifies any registered listeners.
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testRemotePutNotificationGetsToOtherListeners() throws CacheException, InterruptedException {

        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        cache1.put(new Element("1", new Date()));
        cache1.put(new Element("2", new Date()));
        cache1.put(new Element("3", new Date()));

        //Nonserializable and non deliverable put
        Object nonSerializableObject = new Object();
        cache1.put(new Element(nonSerializableObject, new Object()));


        waitForPropagate();

        //local initiating cache's counting listener should have been notified
        assertEquals(4, CountingCacheEventListener.getCacheElementsPut(cache1).size());
        //remote receiving caches' counting listener should have been notified
        assertEquals(3, CountingCacheEventListener.getCacheElementsPut(cache2).size());

        //Update
        cache1.put(new Element("1", new Date()));
        cache1.put(new Element("2", new Date()));
        cache1.put(new Element("3", new Date()));

        //Nonserializable and non deliverable put
        cache1.put(new Element(nonSerializableObject, new Object()));

        waitForPropagate();

        //local initiating cache's counting listener should have been notified
        assertEquals(3, CountingCacheEventListener.getCacheElementsUpdated(cache1).size());
        //remote receiving caches' counting listener should have been notified
        assertEquals(3, CountingCacheEventListener.getCacheElementsUpdated(cache2).size());

        //Remove
        cache1.remove("1");
        cache1.remove("2");
        cache1.remove("3");
        cache1.remove(nonSerializableObject);

        waitForPropagate();

        //local initiating cache's counting listener should have been notified
        assertEquals(4, CountingCacheEventListener.getCacheElementsRemoved(cache1).size());
        //remote receiving caches' counting listener should have been notified
        assertEquals(3, CountingCacheEventListener.getCacheElementsRemoved(cache2).size());

    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testRemoveWithExplicitReplicationConfig() throws InterruptedException {
        removeTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testRemoveWithExplicitReplicationSynchronousConfig() throws InterruptedException {
        removeTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testRemoveWithEmptyReplicationPropertiesConfig() throws InterruptedException {
        removeTest(manager1.getCache("sampleCache4"), manager2.getCache("sampleCache4"), ASYNCHRONOUS);
    }

    /**
     * Tests put and remove initiated from a cache to another cache in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void removeTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws CacheException, InterruptedException {

        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        fromCache.put(element1);

        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        Element element2 = toCache.get(key);
        assertEquals(element1, element2);

        //Remove
        fromCache.remove(key);
        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        element2 = toCache.get(key);
        assertNull(element2);

    }


    /**
     * test removeAll sync
     */
    @Test
    public void testRemoveAllAsynchronous() throws Exception {
        removeAllTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * test removeAll async
     */
    @Test
    public void testRemoveAllSynchronous() throws Exception {
        removeAllTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }

    /**
     * Tests removeAll initiated from a cache to another cache in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void removeAllTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws Exception {

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForPropagate();


        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        fromCache.put(element1);


        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        Element element2 = toCache.get(key);
        assertEquals(element1, element2);

        //Remove
        fromCache.removeAll();
        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        element2 = toCache.get(key);
        assertNull(element2);
        assertEquals(0, toCache.getSize());

    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testUpdateWithExplicitReplicationConfig() throws Exception {
        updateViaCopyTest(manager1.getCache("sampleCache1"), manager2.getCache("sampleCache1"), ASYNCHRONOUS);
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testUpdateWithExplicitReplicationSynchronousConfig() throws Exception {
        updateViaCopyTest(manager1.getCache("sampleCache3"), manager2.getCache("sampleCache3"), SYNCHRONOUS);
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testUpdateWithEmptyReplicationPropertiesConfig() throws Exception {
        updateViaCopyTest(manager1.getCache("sampleCache4"), manager2.getCache("sampleCache4"), ASYNCHRONOUS);
    }

    /**
     * Tests put and update through copy initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void updateViaCopyTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws Exception {

        fromCache.removeAll();
        toCache.removeAll();

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForPropagate();

        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        fromCache.put(element1);
        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        Element element2 = toCache.get(key);
        assertEquals(element1, element2);

        //Update
        Element updatedElement1 = new Element(key, new Date());

        fromCache.put(updatedElement1);
        if (asynchronous) {
            waitForPropagate();
        }

        //Should have been replicated to cache2.
        Element receivedUpdatedElement2 = toCache.get(key);
        assertEquals(updatedElement1, receivedUpdatedElement2);

    }


    /**
     * Tests put through invalidation initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testPutViaInvalidate() throws CacheException, InterruptedException, IOException {

        cache1 = manager1.getCache("sampleCache2");
        cache1.removeAll();

        cache2 = manager2.getCache("sampleCache2");
        cache2.removeAll();

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForPropagate();

        String key = "1";
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        Element element3 = new Element("key2", "two");

        //Put into 2. 2 is configured to replicate puts via copy
        cache2.put(element1);
        assertNotNull(cache2.get(key));
        waitForPropagate();

        //Should have been replicated to cache1.
        Element element2 = cache1.get(key);
        assertEquals(element1, element2);

        //Put
        cache1.put(element3);
        waitForPropagate();

        //Invalidate should have been replicated to cache2.
        assertNull(cache2.get("key2"));

        //Update
        cache1.put(element3);
        waitForPropagate();

        //Should have been removed in cache2.
        element2 = cache2.get("key2");
        assertNull(element2);

    }


    /**
     * Tests put and update through invalidation initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testUpdateViaInvalidate() throws CacheException, InterruptedException, IOException {

        cache1 = manager1.getCache("sampleCache2");
        cache1.removeAll();

        cache2 = manager2.getCache("sampleCache2");
        cache2.removeAll();

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForPropagate();

        String key = "1";
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        cache2.put(element1);
        Element element2 = cache2.get(key);
        assertEquals(element1, element2);

        //Update
        cache1.put(element1);
        waitForPropagate();

        //Should have been removed in cache2.
        element2 = cache2.get(key);
        assertNull(element2);

    }


    /**
     * Tests put and update through invalidation initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testUpdateViaInvalidateNonSerializableValue() throws CacheException, InterruptedException, IOException {

        cache1 = manager1.getCache("sampleCache2");
        cache1.removeAll();

        cache2 = manager2.getCache("sampleCache2");
        cache2.removeAll();

        //removeAll is distributed. Stop it colliding with the rest of the test
        waitForPropagate();

        String key = "1";
        Serializable value = new Date();

        /**
         * Non-serializable test class
         */
        class NonSerializable {
            //
        }

        NonSerializable value1 = new NonSerializable();
        Element element1 = new Element(key, value1);

        //Put
        cache2.put(element1);
        Element element2 = cache2.get(key);
        assertEquals(element1, element2);

        //Update
        cache1.put(element1);
        waitForPropagate();

        //Should have been removed in cache2.
        element2 = cache2.get(key);
        assertNull(element2);

    }


    /**
     * What happens when two cache instances replicate to each other and a change is initiated
     */
    @Test
    public void testInfiniteNotificationsLoop() throws InterruptedException {

        Serializable key = "1";
        Serializable value = new Date();
        Element element = new Element(key, value);

        //Put
        cache1.put(element);
        waitForPropagate();

        //Should have been replicated to cache2.
        Element element2 = cache2.get(key);
        assertEquals(element, element2);

        //Remove
        cache1.remove(key);
        assertNull(cache1.get(key));

        //Should have been replicated to cache2.
        waitForPropagate();
        element2 = cache2.get(key);
        assertNull(element2);

        //Put into 2
        Element element3 = new Element("3", "ddsfds");
        cache2.put(element3);
        waitForPropagate();
        Element element4 = cache2.get("3");
        assertEquals(element3, element4);

    }


    /**
     * Shows result of perf problem and fix in flushReplicationQueue
     * <p/>
     * Behaviour before change:
     * <p/>
     * INFO: Items written: 10381
     * Oct 29, 2007 11:40:04 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 29712
     * Oct 29, 2007 11:40:57 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 1
     * Oct 29, 2007 11:40:58 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 32354
     * Oct 29, 2007 11:42:34 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 322
     * Oct 29, 2007 11:42:35 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 41909
     * <p/>
     * Behaviour after change:
     * INFO: Items written: 26356
     * Oct 29, 2007 11:44:39 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 33656
     * Oct 29, 2007 11:44:40 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 32234
     * Oct 29, 2007 11:44:42 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 38677
     * Oct 29, 2007 11:44:43 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 43418
     * Oct 29, 2007 11:44:44 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 31277
     * Oct 29, 2007 11:44:45 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 27769
     * Oct 29, 2007 11:44:46 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 29596
     * Oct 29, 2007 11:44:47 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 17142
     * Oct 29, 2007 11:44:48 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 14775
     * Oct 29, 2007 11:44:49 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 4088
     * Oct 29, 2007 11:44:51 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 5492
     * Oct 29, 2007 11:44:52 AM net.sf.ehcache.distribution.RMICacheReplicatorTest testReplicatePerf
     * INFO: Items written: 10188
     * <p/>
     * Also no pauses noted.
     */
    @Test
    public void testReplicatePerf() throws InterruptedException {

        if (manager2 != null) {
            manager2.shutdown();
        }
        if (manager3 != null) {
            manager3.shutdown();
        }
        if (manager4 != null) {
            manager4.shutdown();
        }
        if (manager5 != null) {
            manager5.shutdown();
        }
        if (manager6 != null) {
            manager6.shutdown();
        }

        //wait for cluster to drop back to just one: manager1
        waitForPropagate();


        long start = System.currentTimeMillis();
        final String keyBase = Long.toString(start);
        int count = 0;

        for (int i = 0; i < 100000; i++) {
            final String key = keyBase + ':' + Integer.toString((int) (Math.random() * 1000.0));
            cache1.put(new Element(key, "My Test"));
            cache1.get(key);
            cache1.remove(key);
            count++;

            final long end = System.currentTimeMillis();
            if (end - start >= 1000) {
                start = end;
                LOG.info("Items written: " + count);
                //make sure it does not choke
                assertTrue(count > 1000);
                count = 0;
            }
        }
    }


    /**
     * Need to wait for async
     *
     * @throws InterruptedException
     */
    protected void waitForPropagate() throws InterruptedException {
        Thread.sleep(1500);
    }

    /**
     * Need to wait for async
     *
     * @throws InterruptedException
     */
    protected void waitForSlowPropagate() throws InterruptedException {
        Thread.sleep(6000);
    }


    /**
     * Distributed operations create extra scope for deadlock.
     * This test checks whether a distributed deadlock scenario exists for synchronous replication
     * of each distributed operation all at once.
     * It shows that no distributed deadlock exists for asynchronous replication. It is multi thread
     * and multi process safe.
     * <p/>
     * Carefully tailored to exercise:
     * <ol>
     * <li>overflow to disk. We put in 20 things and the memory size is 10
     * <li>each peer is working on the same set of keys thus maximising contention
     * <li>we do puts, gets and removes to explore all the execution paths
     * </ol>
     * If a deadlock occurs, processing will stop until a SocketTimeout exception is thrown and
     * the deadlock will be released.
     */
    @Test
    public void testCacheOperationsSynchronousMultiThreaded() throws Exception, InterruptedException {

        // Run a set of threads, that attempt to fetch the elements
        final List executables = new ArrayList();

        executables.add(new ClusterExecutable(manager1, "sampleCache3"));
        executables.add(new ClusterExecutable(manager2, "sampleCache3"));
        executables.add(new ClusterExecutable(manager3, "sampleCache3"));

        assertThat(runTasks(executables), IsEmptyCollection.<Throwable>empty());
    }


    /**
     * Distributed operations create extra scope for deadlock.
     * This test checks whether a distributed deadlock scenario exists for asynchronous replication
     * of each distributed operation all at once.
     * It shows that no distributed deadlock exists for asynchronous replication. It is multi thread
     * and multi process safe.
     * It uses sampleCache2, which is configured to be asynchronous
     * <p/>
     * Carefully tailored to exercise:
     * <ol>
     * <li>overflow to disk. We put in 20 things and the memory size is 10
     * <li>each peer is working on the same set of keys thus maximising contention
     * <li>we do puts, gets and removes to explore all the execution paths
     * </ol>
     */
    @Test
    public void testCacheOperationsAynchronousMultiThreaded() throws Exception, InterruptedException {

        // Run a set of threads, that attempt to fetch the elements
        final List executables = new ArrayList();

        executables.add(new ClusterExecutable(manager1, "sampleCache2"));
        executables.add(new ClusterExecutable(manager2, "sampleCache2"));
        executables.add(new ClusterExecutable(manager3, "sampleCache2"));

        assertThat(runTasks(executables), IsEmptyCollection.<Throwable>empty());
    }

    /**
     * An Exececutable which allows the CacheManager to be set
     */
    class ClusterExecutable implements Callable<Void> {

        private final CacheManager manager;
        private final String cacheName;

        /**
         * Construct with CacheManager
         *
         * @param manager
         */
        public ClusterExecutable(CacheManager manager, String cacheName) {
            this.manager = manager;
            this.cacheName = cacheName;
        }

        /**
         * Execute
         *
         * @throws Exception
         */
        public Void call() throws Exception {
            Random random = new Random();

            for (int i = 0; i < 20; i++) {
                Integer key = Integer.valueOf((i));
                int operationSelector = random.nextInt(4);
                Cache cache = manager.getCache(cacheName);
                if (operationSelector == 100) {
                    cache.get(key);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(cache.getGuid() + ": get " + key);
                    }
                } else if (operationSelector == 100) {
                    cache.remove(key);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(cache.getGuid() + ": remove " + key);
                    }
                } else if (operationSelector == 2) {
                    cache.put(new Element(key,
                            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                                    + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(cache.getGuid() + ": put " + key);
                    }
                } else {
                    //every twelfth time 1/4 * 1/3 = 1/12
                    if (random.nextInt(3) == 1) {
                        LOG.debug("cache.removeAll()");
                        cache.removeAll();
                    }
                }
            }
            return null;
        }
    }
}
