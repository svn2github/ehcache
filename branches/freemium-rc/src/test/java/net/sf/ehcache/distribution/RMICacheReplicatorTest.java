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

package net.sf.ehcache.distribution;

import static net.sf.ehcache.util.RetryAssert.assertBy;
import static net.sf.ehcache.util.RetryAssert.elementAt;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
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
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.event.CountingCacheEventListener;
import net.sf.ehcache.event.CountingCacheEventListener.CacheEvent;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.Matcher;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.After;
import org.junit.AfterClass;
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

    private static final String DEFAULT_TEST_CACHE = "sampleCache1";

    /**
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        assertThat(getActiveReplicationThreads(), IsEmptyCollection.<Thread>empty());
        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @After
    public void noReplicationThreads() throws Exception {
        RetryAssert.assertBy(30, TimeUnit.SECONDS, new Callable<Set<Thread>>() {
            @Override
            public Set<Thread> call() throws Exception {
                return getActiveReplicationThreads();
            }
        }, IsEmptyCollection.<Thread>empty());
    }

    private static List<CacheManager> createCluster(int size, String ... caches){
        Collection<String> required = Arrays.asList(caches);
        List<CacheManager> members = new ArrayList<CacheManager>(size);
        for (int i = 1; i <= size; i++) {
            Configuration config = getConfiguration(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed" + i + ".xml").name("cm" + i);
            if (!required.isEmpty()) {
                for (Iterator<Entry<String, CacheConfiguration>> it = config.getCacheConfigurations().entrySet().iterator(); it.hasNext(); ) {
                    if (!required.contains(it.next().getKey())) {
                        it.remove();
                    }
                }
            }
            members.add(new CacheManager(config));
        }

        if (required.isEmpty()) {
            waitForClusterMembership(10, TimeUnit.SECONDS, members);
            emptyCaches(10, TimeUnit.SECONDS, members);
        } else {
            waitForClusterMembership(10, TimeUnit.SECONDS, required, members);
            emptyCaches(10, TimeUnit.SECONDS, required, members);
        }
        
        return members;
    }

    private static void destroyCluster(List<CacheManager> members) {
        for (CacheManager manager : members) {
            if (manager != null) {
                manager.shutdown();
            }
        }
    }
    
    @Test
    public void testCASOperationsNotSupported() throws Exception {
        List<CacheManager> cluster = createCluster(4, DEFAULT_TEST_CACHE);
        try {
            final Ehcache cache1 = cluster.get(0).getEhcache(DEFAULT_TEST_CACHE);
            final Ehcache cache2 = cluster.get(1).getEhcache(DEFAULT_TEST_CACHE);
            final Ehcache cache3 = cluster.get(2).getEhcache(DEFAULT_TEST_CACHE);
            final Ehcache cache4 = cluster.get(3).getEhcache(DEFAULT_TEST_CACHE);

            try {
                cache1.putIfAbsent(new Element("foo", "poo"));
                throw new AssertionError("CAS operation should have failed.");
            } catch (CacheException ce) {
                assertThat(ce.getMessage(), containsString("CAS"));
            }

            try {
                cache2.removeElement(new Element("foo", "poo"));
                throw new AssertionError("CAS operation should have failed.");
            } catch (CacheException ce) {
                assertThat(ce.getMessage(), containsString("CAS"));
            }

            try {
                cache3.replace(new Element("foo", "poo"));
                throw new AssertionError("CAS operation should have failed.");
            } catch (CacheException ce) {
                assertThat(ce.getMessage(), containsString("CAS"));
            }

            try {
                cache4.replace(new Element("foo", "poo"), new Element("foo", "poo2"));
                throw new AssertionError("CAS operation should have failed.");
            } catch (CacheException ce) {
                assertThat(ce.getMessage(), containsString("CAS"));
            }

            try {
                cache1.putIfAbsent(new Element("foo", "poo"), true);
            } catch (CacheException ce) {
                ce.printStackTrace();
                throw new AssertionError("CAS operation should have succeeded.");
            }
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Does a new cache manager in the cluster get detected?
     */
    @Test
    public void testRemoteCachePeersDetectsNewCacheManager() throws InterruptedException {
        List<CacheManager> cluster = createCluster(5, DEFAULT_TEST_CACHE);
        try {
            //Add new CacheManager to cluster
            cluster.add(new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed6.xml"));

            //Allow detection to occur
            waitForClusterMembership(10020, TimeUnit.MILLISECONDS, Collections.singleton(DEFAULT_TEST_CACHE), cluster);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    @Test
    public void testRemoteCachePeersDetectsDownCacheManager() throws InterruptedException {
        List<CacheManager> cluster = createCluster(5, DEFAULT_TEST_CACHE);
        try {
            //Drop a CacheManager from the cluster
            cluster.remove(4).shutdown();
            assertThat(cluster, hasSize(4));
            
            //Allow change detection to occur. Heartbeat 1 second and is not stale until 5000
            waitForClusterMembership(11020, TimeUnit.MILLISECONDS, Collections.singleton(DEFAULT_TEST_CACHE), cluster);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    @Test
    public void testRemoteCachePeersDetectsDownCacheManagerSlow() throws InterruptedException {
        List<CacheManager> cluster = createCluster(5, DEFAULT_TEST_CACHE);
        try {
            CacheManager manager = cluster.get(0);
            Cache cache = manager.getCache(DEFAULT_TEST_CACHE);
            CacheManagerPeerProvider provider = manager.getCacheManagerPeerProvider("RMI");
            assertThat((List<?>) provider.listRemoteCachePeers(cache), hasSize(4));

            MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(2000);
            try {
                Thread.sleep(2000);

                //Drop a CacheManager from the cluster
                cluster.remove(4).shutdown();

                //Insufficient time for it to timeout
                assertThat((List<?>) provider.listRemoteCachePeers(cache), hasSize(4));
            } finally {
                MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);
                Thread.sleep(2000);
            }
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Tests put and remove initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testPutPropagatesFromAndToEveryCacheManagerAndCache() throws CacheException, InterruptedException {
        RMICacheManagerPeerListenerTest.setupLogging();
        final List<CacheManager> cluster = createCluster(5);
        try {
            final CacheManager manager0 = cluster.get(0);
            //Put
            final String[] cacheNames = manager0.getCacheNames();
            Arrays.sort(cacheNames);
            for (int i = 0; i < cacheNames.length; i++) {
                String name = cacheNames[i];
                manager0.getCache(name).put(new Element(Integer.toString(i), Integer.valueOf(i)));
                //Add some non serializable elements that should not get propagated
                manager0.getCache(name).put(new Element("nonSerializable" + i, new Object()));
            }

            assertBy(10, TimeUnit.SECONDS, new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    for (int i = 0; i < cacheNames.length; i++) {
                        String name = cacheNames[i];
                        if (manager0.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(manager0.getCache(name)).isEmpty()) {
                            continue;
                        }
                        if ("sampleCache2".equals(name)) {
                            //sampleCache2 in manager0 replicates puts via invalidate, so the count will be 1 less
                            for (CacheManager manager : cluster.subList(1, cluster.size())) {
                                assertThat(manager.getCache(name).get(Integer.toString(i)), nullValue());
                                assertThat(manager.getCache(name).get("nonSerializable" + i), nullValue());
                            }
                        } else {
                            for (CacheManager manager : cluster.subList(1, cluster.size())) {
                                assertThat("Cache : " + name, manager.getCache(name).get(Integer.toString(i)), notNullValue());
                                assertThat(manager.getCache(name).get("nonSerializable" + i), nullValue());
                            }
                        }
                    }
                    return Boolean.TRUE;
                }
            }, is(Boolean.TRUE));
        } finally {
            destroyCluster(cluster);
            RMICacheManagerPeerListenerTest.revertLogging();
        }
    }

    /**
     * Tests what happens when a CacheManager in the cluster comes and goes. In ehcache-1.2.4 this would cause the new RMI CachePeers in the CacheManager to
     * be permanently corrupt.
     */
    @Test
    public void testPutPropagatesFromAndToEveryCacheManagerAndCacheDirty() throws CacheException, InterruptedException {
        RMICacheManagerPeerListenerTest.setupLogging();
        final List<CacheManager> cluster = createCluster(5);
        try {
            cluster.remove(2).shutdown();
            waitForClusterMembership(10, TimeUnit.SECONDS, cluster);

            cluster.add(2, new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml"));
            waitForClusterMembership(10, TimeUnit.SECONDS, cluster);

            //Put
            final CacheManager manager = cluster.get(0);
            final String[] cacheNames = manager.getCacheNames();
            Arrays.sort(cacheNames);
            for (int i = 0; i < cacheNames.length; i++) {
                String name = cacheNames[i];
                manager.getCache(name).put(new Element(Integer.toString(i), Integer.valueOf(i)));
                //Add some non serializable elements that should not get propagated
                manager.getCache(name).put(new Element("nonSerializable" + i, new Object()));
            }

            assertBy(10, TimeUnit.SECONDS, new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    for (int i = 0; i < cacheNames.length; i++) {
                        String name = cacheNames[i];
                        if (manager.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(manager.getCache(name)).isEmpty()) {
                            continue;
                        }
                        if ("sampleCache2".equals(name)) {
                            //sampleCache2 in manager1 replicates puts via invalidate, so the count will be 1 less
                            for (CacheManager manager : cluster.subList(1, cluster.size())) {
                                assertThat(manager.getCache(name).get(Integer.toString(i)), nullValue());
                                assertThat(manager.getCache(name).get("nonSerializable" + i), nullValue());
                            }
                        } else {
                            for (CacheManager manager : cluster.subList(1, cluster.size())) {
                                assertThat("Cache : " + name, manager.getCache(name).get(Integer.toString(i)), notNullValue());
                                assertThat(manager.getCache(name).get("nonSerializable" + i), nullValue());
                            }
                        }
                    }
                    return Boolean.TRUE;
                }
            }, is(Boolean.TRUE));
        } finally {
            destroyCluster(cluster);
            RMICacheManagerPeerListenerTest.revertLogging();
        }
    }

    /**
     * manager1 adds a replicating cache, then manager2 and so on. Then we remove one. Does everything work as expected?
     */
    @Test
    public void testPutWithNewCacheAddedProgressively() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2);
        try {
            cluster.get(0).addCache("progressiveAddCache");
            cluster.get(1).addCache("progressiveAddCache");

            //The cluster will not have formed yet, so it will fail
            try {
                putTest(cluster.get(0).getCache("progressiveAddCache"), cluster.get(1).getCache("progressiveAddCache"), ASYNCHRONOUS);
                fail();
            } catch (AssertionError e) {
                //expected
            }

            //The cluster will now have formed yet, so it will succeed
            putTest(cluster.get(0).getCache("progressiveAddCache"), cluster.get(1).getCache("progressiveAddCache"), ASYNCHRONOUS);

            Cache secondCache = cluster.get(1).getCache("progressiveAddCache");

            //The second peer disappears. The test will fail.
            cluster.get(1).removeCache("progressiveAddCache");
            try {
                putTest(cluster.get(0).getCache("progressiveAddCache"), secondCache, ASYNCHRONOUS);
                fail();
            } catch (IllegalStateException e) {
                //The second cache will not alive. Expected. But no other exception is caught and this will otherwise fail.

            }
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testPutWithExplicitReplicationConfig() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, "sampleCache1");
        try {
            putTest(cluster.get(0).getCache("sampleCache1"), cluster.get(1).getCache("sampleCache1"), ASYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testPutWithThreadKiller() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, "sampleCache1");
        try {
            putTestWithThreadKiller(cluster.get(0).getCache("sampleCache1"), cluster.get(1).getCache("sampleCache1"), ASYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * CacheEventListeners that are not CacheReplicators should receive cache events originated from receipt
     * of a remote event by a CachePeer.
     */

    @Test
    public void testRemotelyReceivedPutNotifiesCountingListener() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, "sampleCache1");
        try {
            Cache cache0 = cluster.get(0).getCache("sampleCache1");
            Cache cache1 = cluster.get(1).getCache("sampleCache1");
            CountingCacheEventListener.getCountingCacheEventListener(cache0).resetCounters();
            CountingCacheEventListener.getCountingCacheEventListener(cache1).resetCounters();
            putTest(cache0, cache1, ASYNCHRONOUS);
            assertThat(CountingCacheEventListener.getCountingCacheEventListener(cache0).getCacheElementsPut(), hasSize(1));
            assertThat(CountingCacheEventListener.getCountingCacheEventListener(cache1).getCacheElementsPut(), hasSize(1));
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=false, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testPutWithExplicitReplicationSynchronousConfig() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, "sampleCache3");
        try {
            putTest(cluster.get(0).getCache("sampleCache3"), cluster.get(1).getCache("sampleCache3"), SYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testPutWithEmptyReplicationPropertiesConfig() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, "sampleCache4");
        try {
            putTest(cluster.get(0).getCache("sampleCache4"), cluster.get(1).getCache("sampleCache4"), ASYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Test put replicated for cache4 - missing replicatePuts property.
     * replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     * should equal replicateAsynchronously=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testPutWithOneMissingReplicationPropertyConfig() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, "sampleCache5");
        try {
            putTest(cluster.get(0).getCache("sampleCache5"), cluster.get(1).getCache("sampleCache5"), ASYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
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
            assertAfterPropagation(elementAt(toCache, key), equalTo(sourceElement));
        } else {
            assertThat(toCache.get(key), equalTo(sourceElement));
        }
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
            assertAfterPropagation(elementAt(toCache, key), equalTo(sourceElement));
        } else {
            assertThat(toCache.get(key), equalTo(sourceElement));
        }
    }


    /**
     * Checks that a put received from a remote cache notifies any registered listeners.
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testRemotePutNotificationGetsToOtherListeners() throws CacheException, InterruptedException {
        List<CacheManager> cluster = createCluster(2, DEFAULT_TEST_CACHE);
        try {
            Cache cache0 = cluster.get(0).getCache(DEFAULT_TEST_CACHE);
            Cache cache1 = cluster.get(1).getCache(DEFAULT_TEST_CACHE);
            
            final CountingCacheEventListener listener0 = CountingCacheEventListener.getCountingCacheEventListener(cache0);
            final CountingCacheEventListener listener1 = CountingCacheEventListener.getCountingCacheEventListener(cache1);
            listener0.resetCounters();
            listener1.resetCounters();
            
            //Put
            cache0.put(new Element("1", new Date()));
            cache0.put(new Element("2", new Date()));
            cache0.put(new Element("3", new Date()));

            //Nonserializable and non deliverable put
            Object nonSerializableObject = new Object();
            cache0.put(new Element(nonSerializableObject, new Object()));

            //remote receiving caches' counting listener should have been notified
            assertAfterPropagation(new Callable<Collection<CountingCacheEventListener.CacheEvent>>() {

                @Override
                public Collection<CacheEvent> call() throws Exception {
                    return listener1.getCacheElementsPut();
                }

            }, hasSize(3));
            //local initiating cache's counting listener should have been notified
            assertThat(listener0.getCacheElementsPut(), hasSize(4));

            //Update
            cache0.put(new Element("1", new Date()));
            cache0.put(new Element("2", new Date()));
            cache0.put(new Element("3", new Date()));

            //Nonserializable and non deliverable put
            cache0.put(new Element(nonSerializableObject, new Object()));

            //remote receiving caches' counting listener should have been notified
            assertAfterPropagation(new Callable<Collection<CountingCacheEventListener.CacheEvent>>() {

                @Override
                public Collection<CacheEvent> call() throws Exception {
                    return listener1.getCacheElementsUpdated();
                }

            }, hasSize(3));
            //local initiating cache's counting listener should have been notified
            assertThat(listener0.getCacheElementsUpdated(), hasSize(4));

            //Remove
            cache0.remove("1");
            cache0.remove("2");
            cache0.remove("3");
            cache0.remove(nonSerializableObject);

            //remote receiving caches' counting listener should have been notified
            assertAfterPropagation(new Callable<Collection<CountingCacheEventListener.CacheEvent>>() {

                @Override
                public Collection<CacheEvent> call() throws Exception {
                    return listener1.getCacheElementsRemoved();
                }

            }, hasSize(3));
            //local initiating cache's counting listener should have been notified
            assertThat(listener0.getCacheElementsRemoved(), hasSize(4));
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testRemoveWithExplicitReplicationConfig() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, "sampleCache1");
        try {
            removeTest(cluster.get(0).getCache("sampleCache1"), cluster.get(1).getCache("sampleCache1"), ASYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testRemoveWithExplicitReplicationSynchronousConfig() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, "sampleCache3");
        try {
            removeTest(cluster.get(0).getCache("sampleCache3"), cluster.get(1).getCache("sampleCache3"), SYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testRemoveWithEmptyReplicationPropertiesConfig() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, "sampleCache4");
        try {
            removeTest(cluster.get(0).getCache("sampleCache4"), cluster.get(1).getCache("sampleCache4"), ASYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
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
            assertAfterPropagation(elementAt(toCache, key), equalTo(element1));
        } else {
            assertThat(toCache.get(key), equalTo(element1));
        }
        
        //Remove
        fromCache.remove(key);
        if (asynchronous) {
            assertAfterPropagation(elementAt(toCache, key), nullValue());
        } else {
            assertThat(toCache.get(key), nullValue());
        }
    }


    /**
     * test removeAll sync
     */
    @Test
    public void testRemoveAllAsynchronous() throws Exception {
        List<CacheManager> cluster = createCluster(2, "sampleCache1");
        try {
            removeAllTest(cluster.get(0).getCache("sampleCache1"), cluster.get(1).getCache("sampleCache1"), ASYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * test removeAll async
     */
    @Test
    public void testRemoveAllSynchronous() throws Exception {
        List<CacheManager> cluster = createCluster(2, "sampleCache3");
        try {
            removeAllTest(cluster.get(0).getCache("sampleCache3"), cluster.get(1).getCache("sampleCache3"), SYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Tests removeAll initiated from a cache to another cache in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void removeAllTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws Exception {
        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        fromCache.put(element1);


        if (asynchronous) {
            assertAfterPropagation(elementAt(toCache, key), equalTo(element1));
        } else {
            assertThat(toCache.get(key), equalTo(element1));
        }
        
        //Remove
        fromCache.removeAll();
        if (asynchronous) {
            assertAfterPropagation(elementAt(toCache, key), nullValue());
        } else {
            assertThat(toCache.get(key), nullValue());
        }
        assertThat(toCache.getSize(), equalTo(0));
    }


    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testUpdateWithExplicitReplicationConfig() throws Exception {
        List<CacheManager> cluster = createCluster(2, "sampleCache1");
        try {
            updateViaCopyTest(cluster.get(0).getCache("sampleCache1"), cluster.get(1).getCache("sampleCache1"), ASYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Test various cache configurations for cache1 - explicit setting of:
     * properties="replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true "/>
     */
    @Test
    public void testUpdateWithExplicitReplicationSynchronousConfig() throws Exception {
        List<CacheManager> cluster = createCluster(2, "sampleCache3");
        try {
            updateViaCopyTest(cluster.get(0).getCache("sampleCache3"), cluster.get(1).getCache("sampleCache3"), SYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * Test put replicated for cache4 - no properties.
     * Defaults should be replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateUpdatesViaCopy=true, replicateRemovals=true
     */
    @Test
    public void testUpdateWithEmptyReplicationPropertiesConfig() throws Exception {
        List<CacheManager> cluster = createCluster(2, "sampleCache4");
        try {
            updateViaCopyTest(cluster.get(0).getCache("sampleCache4"), cluster.get(1).getCache("sampleCache4"), ASYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Tests put and update through copy initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    public void updateViaCopyTest(Ehcache fromCache, Ehcache toCache, boolean asynchronous) throws Exception {
        Serializable key = new Date();
        Serializable value = new Date();
        Element element1 = new Element(key, value);

        //Put
        fromCache.put(element1);
        if (asynchronous) {
            assertAfterPropagation(elementAt(toCache, key), equalTo(element1));
        } else {
            assertThat(toCache.get(key), equalTo(element1));
        }
        
        //Update
        Element updatedElement1 = new Element(key, new Date());

        fromCache.put(updatedElement1);
        if (asynchronous) {
            assertAfterPropagation(elementAt(toCache, key), equalTo(updatedElement1));
        } else {
            assertThat(toCache.get(key), equalTo(updatedElement1));
        }
    }


    /**
     * Tests put through invalidation initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testPutViaInvalidate() throws CacheException, InterruptedException, IOException {
        List<CacheManager> cluster = createCluster(2, "sampleCache2");
        try {
            Cache cache1 = cluster.get(0).getCache("sampleCache2");
            Cache cache2 = cluster.get(1).getCache("sampleCache2");

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
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * Tests put and update through invalidation initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testUpdateViaInvalidate() throws CacheException, InterruptedException, IOException {
        List<CacheManager> cluster = createCluster(2, "sampleCache2");
        try {
            Cache cache1 = cluster.get(0).getCache("sampleCache2");
            Cache cache2 = cluster.get(1).getCache("sampleCache2");

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
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * Tests put and update through invalidation initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testUpdateViaInvalidateNonSerializableValue() throws CacheException, InterruptedException, IOException {
        List<CacheManager> cluster = createCluster(2, "sampleCache2");
        try {
            Cache cache1 = cluster.get(0).getCache("sampleCache2");
            Cache cache2 = cluster.get(1).getCache("sampleCache2");

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
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * What happens when two cache instances replicate to each other and a change is initiated
     */
    @Test
    public void testInfiniteNotificationsLoop() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, DEFAULT_TEST_CACHE);
        try {
            Cache cache0 = cluster.get(0).getCache(DEFAULT_TEST_CACHE);
            Cache cache1 = cluster.get(1).getCache(DEFAULT_TEST_CACHE);
            
            Serializable key = "1";
            Serializable value = new Date();
            Element element = new Element(key, value);

            //Put
            cache0.put(element);
            assertAfterPropagation(elementAt(cache1, key), equalTo(element));

            //Remove
            cache0.remove(key);
            assertThat(cache0.get(key), nullValue());
            assertAfterPropagation(elementAt(cache1, key), nullValue());

            //Put into 1
            Element element3 = new Element("3", "ddsfds");
            cache1.put(element3);
            assertAfterPropagation(elementAt(cache0, "3"), equalTo(element3));
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Need to wait for async
     *
     * @throws InterruptedException
     */
    private static void waitForPropagate() throws InterruptedException {
        Thread.sleep(1500);
    }
    
    protected static <T> void assertAfterPropagation(Callable<T> callable, Matcher<? super T> matcher) {
        assertBy(1500, TimeUnit.MILLISECONDS, callable, matcher);
    }

    protected static <T> void assertAfterSlowPropagation(Callable<T> callable, Matcher<? super T> matcher) {
        assertBy(6000, TimeUnit.MILLISECONDS, callable, matcher);
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
        List<CacheManager> cluster = createCluster(3, "sampleCache3");
        try {
            // Run a set of threads, that attempt to fetch the elements
            final List executables = new ArrayList();

            executables.add(new ClusterExecutable(cluster.get(0), "sampleCache3"));
            executables.add(new ClusterExecutable(cluster.get(1), "sampleCache3"));
            executables.add(new ClusterExecutable(cluster.get(2), "sampleCache3"));

            assertThat(runTasks(executables), IsEmptyCollection.<Throwable>empty());
        } finally {
            destroyCluster(cluster);
        }
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
    public void testCacheOperationsAsynchronousMultiThreaded() throws Exception, InterruptedException {
        List<CacheManager> cluster = createCluster(3, "sampleCache2");
        try {
            // Run a set of threads, that attempt to fetch the elements
            final List executables = new ArrayList();

            executables.add(new ClusterExecutable(cluster.get(0), "sampleCache2"));
            executables.add(new ClusterExecutable(cluster.get(1), "sampleCache2"));
            executables.add(new ClusterExecutable(cluster.get(2), "sampleCache2"));

            assertThat(runTasks(executables), IsEmptyCollection.<Throwable>empty());
        } finally {
            destroyCluster(cluster);
        }
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
                if (operationSelector == 2) {
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
