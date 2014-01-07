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
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.ThreadKiller;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
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
    public static void enableRmiLogging() throws IOException {
      installRmiLogging("RMICacheReplicatorTest.log");
    }

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
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        assertThat(getActiveReplicationThreads(), IsEmptyCollection.<Thread>empty());
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

    private static CacheConfiguration createSynchronousCache() {
      CacheConfiguration cacheConfig = new CacheConfiguration();
      cacheConfig.maxEntriesLocalHeap(0).eternal(true);
      cacheConfig.addCacheEventListenerFactory(new CacheConfiguration.CacheEventListenerFactoryConfiguration()
              .className("net.sf.ehcache.distribution.RMICacheReplicatorFactory")
              .properties("replicateAsynchronously=false,"
              + "replicatePuts=true,"
              + "replicateUpdates=true,"
              + "replicateUpdatesViaCopy=true,"
              + "replicateRemovals=true"));
      return cacheConfig;
    }
    
    private static CacheConfiguration createDefaultRMICache() {
      CacheConfiguration cacheConfig = new CacheConfiguration();
      cacheConfig.maxEntriesLocalHeap(0).eternal(true);
      cacheConfig.addCacheEventListenerFactory(new CacheConfiguration.CacheEventListenerFactoryConfiguration()
              .className("net.sf.ehcache.distribution.RMICacheReplicatorFactory"));
      return cacheConfig;
    }

    private static CacheConfiguration createNoPutSettingRMICache() {
      CacheConfiguration cacheConfig = new CacheConfiguration();
      cacheConfig.maxEntriesLocalHeap(0).eternal(true);
      cacheConfig.addCacheEventListenerFactory(new CacheConfiguration.CacheEventListenerFactoryConfiguration()
              .className("net.sf.ehcache.distribution.RMICacheReplicatorFactory")
              .properties("replicateAsynchronously=true,"
              + "replicateUpdates=true,"
              + "replicateUpdatesViaCopy=true,"
              + "replicateRemovals=true"));
      return cacheConfig;
    }

    private static List<CacheManager> createCluster(int size, CacheConfiguration ... caches){
        LOG.info("Creating Cluster");
        List<Configuration> configurations = new ArrayList<Configuration>(size);
        for (int i = 0; i < size; i++) {
            Configuration config = createRMICacheManagerConfiguration().name("RMI-Cache-Manager-" + i);
            for (CacheConfiguration cache : caches) {
              config.addCache(cache);
            }
            configurations.add(config);
        }
        LOG.info("Created Configurations");

        List<CacheManager> members = startupManagers(configurations);
        try {
          LOG.info("Created Managers");
          waitForClusterMembership(120, TimeUnit.SECONDS, members);
          LOG.info("Cluster Membership Complete");
          emptyCaches(120, TimeUnit.SECONDS, members);
          LOG.info("Caches Emptied");
          return members;
        } catch (RuntimeException e) {
            destroyCluster(members);
            throw e;
        } catch (Error e) {
            destroyCluster(members);
            throw e;
        }
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
        List<CacheManager> cluster = createCluster(4, createAsynchronousCache().name("testCASOperationsNotSupported"));
        try {
            final Ehcache cache1 = cluster.get(0).getEhcache("testCASOperationsNotSupported");
            final Ehcache cache2 = cluster.get(1).getEhcache("testCASOperationsNotSupported");
            final Ehcache cache3 = cluster.get(2).getEhcache("testCASOperationsNotSupported");
            final Ehcache cache4 = cluster.get(3).getEhcache("testCASOperationsNotSupported");

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
        List<CacheManager> cluster = createCluster(5, createAsynchronousCache().name("testRemoteCachePeersDetectsNewCacheManager"));
        try {
            //Add new CacheManager to cluster
            cluster.add(new CacheManager(createRMICacheManagerConfiguration().name("cm-6").cache(createAsynchronousCache().name("testRemoteCachePeersDetectsNewCacheManager"))));

            //Allow detection to occur
            waitForClusterMembership(10020, TimeUnit.MILLISECONDS, cluster);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    @Test
    public void testRemoteCachePeersDetectsDownCacheManager() throws InterruptedException {
        List<CacheManager> cluster = createCluster(5, createAsynchronousCache().name("testRemoteCachePeersDetectsDownCacheManager"));
        try {
            MulticastKeepaliveHeartbeatSender.setHeartBeatStaleTime(3000);
            //Drop a CacheManager from the cluster
            cluster.remove(4).shutdown();
            assertThat(cluster, hasSize(4));

            //Allow change detection to occur. Heartbeat 1 second and is not stale until 5000
            waitForClusterMembership(10, TimeUnit.SECONDS, cluster);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    @Test
    public void testRemoteCachePeersDetectsDownCacheManagerSlow() throws InterruptedException {
        List<CacheManager> cluster = createCluster(5, createAsynchronousCache().name("testRemoteCachePeersDetectsDownCacheManagerSlow"));
        try {
            CacheManager manager = cluster.get(0);
            CacheManagerPeerProvider provider = manager.getCacheManagerPeerProvider("RMI");
            Cache cache = manager.getCache("testRemoteCachePeersDetectsDownCacheManagerSlow");

            //Drop a CacheManager from the cluster
            cluster.remove(4).shutdown();

            //Insufficient time for it to timeout
            assertThat((List<?>) provider.listRemoteCachePeers(cache), hasSize(4));
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * manager1 adds a replicating cache, then manager2 and so on. Then we remove one. Does everything work as expected?
     */
    @Test
    public void testPutWithNewCacheAddedProgressively() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, createAsynchronousCache().name("dummy"));
        try {
            cluster.get(0).addCache(new Cache(createAsynchronousCache().name("progressiveAddCache")));
            cluster.get(1).addCache(new Cache(createAsynchronousCache().name("progressiveAddCache")));

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
        List<CacheManager> cluster = createCluster(2, createAsynchronousCache().name("testPutWithExplicitReplicationConfig"));
        try {
            putTest(cluster.get(0).getCache("testPutWithExplicitReplicationConfig"), cluster.get(1).getCache("testPutWithExplicitReplicationConfig"), ASYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createAsynchronousCache().name("testPutWithThreadKiller"));
        try {
            putTestWithThreadKiller(cluster.get(0).getCache("testPutWithThreadKiller"), cluster.get(1).getCache("testPutWithThreadKiller"), ASYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createAsynchronousCache().name("testRemotelyReceivedPutNotifiesCountingListener")
                .cacheEventListenerFactory(new CacheEventListenerFactoryConfiguration().className("net.sf.ehcache.event.CountingCacheEventListenerFactory")));
        try {
            Cache cache0 = cluster.get(0).getCache("testRemotelyReceivedPutNotifiesCountingListener");
            Cache cache1 = cluster.get(1).getCache("testRemotelyReceivedPutNotifiesCountingListener");
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
        List<CacheManager> cluster = createCluster(2, createSynchronousCache().name("testPutWithExplicitReplicationSynchronousConfig"));
        try {
            putTest(cluster.get(0).getCache("testPutWithExplicitReplicationSynchronousConfig"), cluster.get(1).getCache("testPutWithExplicitReplicationSynchronousConfig"), SYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createDefaultRMICache().name("testPutWithEmptyReplicationPropertiesConfig"));
        try {
            putTest(cluster.get(0).getCache("testPutWithEmptyReplicationPropertiesConfig"), cluster.get(1).getCache("testPutWithEmptyReplicationPropertiesConfig"), ASYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createNoPutSettingRMICache().name("testPutWithOneMissingReplicationPropertyConfig"));
        try {
            putTest(cluster.get(0).getCache("testPutWithOneMissingReplicationPropertyConfig"), cluster.get(1).getCache("testPutWithOneMissingReplicationPropertyConfig"), ASYNCHRONOUS);
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
            Thread.sleep(1500);
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
        List<CacheManager> cluster = createCluster(2, createAsynchronousCache().name("testRemotePutNotificationGetsToOtherListeners")
                .cacheEventListenerFactory(new CacheEventListenerFactoryConfiguration().className("net.sf.ehcache.event.CountingCacheEventListenerFactory")));
        try {
            Cache cache0 = cluster.get(0).getCache("testRemotePutNotificationGetsToOtherListeners");
            Cache cache1 = cluster.get(1).getCache("testRemotePutNotificationGetsToOtherListeners");

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
        List<CacheManager> cluster = createCluster(2, createAsynchronousCache().name("testRemoveWithExplicitReplicationConfig"));
        try {
            removeTest(cluster.get(0).getCache("testRemoveWithExplicitReplicationConfig"), cluster.get(1).getCache("testRemoveWithExplicitReplicationConfig"), ASYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createSynchronousCache().name("testRemoveWithExplicitReplicationSynchronousConfig"));
        try {
            removeTest(cluster.get(0).getCache("testRemoveWithExplicitReplicationSynchronousConfig"), cluster.get(1).getCache("testRemoveWithExplicitReplicationSynchronousConfig"), SYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createDefaultRMICache().name("testRemoveWithEmptyReplicationPropertiesConfig"));
        try {
            removeTest(cluster.get(0).getCache("testRemoveWithEmptyReplicationPropertiesConfig"), cluster.get(1).getCache("testRemoveWithEmptyReplicationPropertiesConfig"), ASYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createAsynchronousCache().name("testRemoveAllAsynchronous"));
        try {
            removeAllTest(cluster.get(0).getCache("testRemoveAllAsynchronous"), cluster.get(1).getCache("testRemoveAllAsynchronous"), ASYNCHRONOUS);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * test removeAll async
     */
    @Test
    public void testRemoveAllSynchronous() throws Exception {
        List<CacheManager> cluster = createCluster(2, createSynchronousCache().name("testRemoveAllSynchronous"));
        try {
            removeAllTest(cluster.get(0).getCache("testRemoveAllSynchronous"), cluster.get(1).getCache("testRemoveAllSynchronous"), SYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createAsynchronousCache().name("testUpdateWithExplicitReplicationConfig"));
        try {
            updateViaCopyTest(cluster.get(0).getCache("testUpdateWithExplicitReplicationConfig"), cluster.get(1).getCache("testUpdateWithExplicitReplicationConfig"), ASYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createSynchronousCache().name("testUpdateWithExplicitReplicationSynchronousConfig"));
        try {
            updateViaCopyTest(cluster.get(0).getCache("testUpdateWithExplicitReplicationSynchronousConfig"), cluster.get(1).getCache("testUpdateWithExplicitReplicationSynchronousConfig"), SYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createDefaultRMICache().name("testUpdateWithEmptyReplicationPropertiesConfig"));
        try {
            updateViaCopyTest(cluster.get(0).getCache("testUpdateWithEmptyReplicationPropertiesConfig"), cluster.get(1).getCache("testUpdateWithEmptyReplicationPropertiesConfig"), ASYNCHRONOUS);
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
        List<CacheManager> cluster = createCluster(2, createAsynchronousCacheViaInvalidate().name("testPutViaInvalidate"));
        try {
            Cache cache1 = cluster.get(0).getCache("testPutViaInvalidate");
            Cache cache2 = cluster.get(1).getCache("testPutViaInvalidate");

            Element initial = new Element("1", "1");

            cache2.put(initial, true);

            cache1.put(new Element("1", "2"));
            assertAfterPropagation(elementAt(cache2, "1"), nullValue());
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
        List<CacheManager> cluster = createCluster(2, createAsynchronousCacheViaInvalidate().name("testUpdateViaInvalidate"));
        try {
            Cache cache1 = cluster.get(0).getCache("testUpdateViaInvalidate");
            Cache cache2 = cluster.get(1).getCache("testUpdateViaInvalidate");

            Element initial = new Element("1", "1");

            cache1.put(initial, true);
            cache2.put(initial, true);

            cache1.put(new Element("1", "2"));
            assertAfterPropagation(elementAt(cache2, "1"), nullValue());
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
        List<CacheManager> cluster = createCluster(2, createAsynchronousCacheViaInvalidate().name("testUpdateViaInvalidateNonSerializableValue"));
        try {
            /**
            * Non-serializable test class
            */
            class NonSerializable {
                //
            }

            Cache cache1 = cluster.get(0).getCache("testUpdateViaInvalidateNonSerializableValue");
            Cache cache2 = cluster.get(1).getCache("testUpdateViaInvalidateNonSerializableValue");

            Element initial = new Element("1", "1");

            cache1.put(initial, true);
            cache2.put(initial, true);

            cache1.put(new Element("1", new NonSerializable()));
            assertAfterPropagation(elementAt(cache2, "1"), nullValue());
        } finally {
            destroyCluster(cluster);
        }
    }


    /**
     * What happens when two cache instances replicate to each other and a change is initiated
     */
    @Test
    public void testInfiniteNotificationsLoop() throws InterruptedException {
        List<CacheManager> cluster = createCluster(2, createAsynchronousCache().name("testInfiniteNotificationsLoop"));
        try {
            Cache cache0 = cluster.get(0).getCache("testInfiniteNotificationsLoop");
            Cache cache1 = cluster.get(1).getCache("testInfiniteNotificationsLoop");

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
        List<CacheManager> cluster = createCluster(3, createSynchronousCache().name("testCacheOperationsSynchronousMultiThreaded"));
        try {
            // Run a set of threads, that attempt to fetch the elements
            final List executables = new ArrayList();

            executables.add(new ClusterExecutable(cluster.get(0), "testCacheOperationsSynchronousMultiThreaded"));
            executables.add(new ClusterExecutable(cluster.get(1), "testCacheOperationsSynchronousMultiThreaded"));
            executables.add(new ClusterExecutable(cluster.get(2), "testCacheOperationsSynchronousMultiThreaded"));

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
        List<CacheManager> cluster = createCluster(3, createAsynchronousCacheViaInvalidate().name("testCacheOperationsAsynchronousMultiThreaded"));
        try {
            // Run a set of threads, that attempt to fetch the elements
            final List executables = new ArrayList();

            executables.add(new ClusterExecutable(cluster.get(0), "testCacheOperationsAsynchronousMultiThreaded"));
            executables.add(new ClusterExecutable(cluster.get(1), "testCacheOperationsAsynchronousMultiThreaded"));
            executables.add(new ClusterExecutable(cluster.get(2), "testCacheOperationsAsynchronousMultiThreaded"));

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
