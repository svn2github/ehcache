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

package net.sf.ehcache.terracotta;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.ClusterTopologyListener;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.constructs.nonstop.ThreadDump;
import net.sf.ehcache.terracotta.TerracottaClusteredInstanceHelper.TerracottaRuntimeType;
import net.sf.ehcache.terracotta.TestRejoinStore.StoreAction;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class BasicRejoinTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(BasicRejoinTest.class);

    private static final String ERROR_MSG_REJOIN_CUSTOM = "Rejoin cannot be used in Terracotta DSO mode";
    private static final String ERROR_MSG_REJOIN_NO_NONSTOP = "Terracotta clustered caches must be nonstop when rejoin is enabled";
    private static final CharSequence ERROR_MSG_REJOIN_NO_TC = "Terracotta Rejoin is enabled but can't determine Terracotta Runtime. "
            + "You are probably missing Terracotta jar(s)";

    @Test
    public void testInvalidRejoinWithoutNonstop() throws Exception {
        ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory);

        CacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        try {
            new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/invalid-rejoin-no-nonstop-test.xml"));
            fail("Trying to run rejoin without nonstop terracotta caches should fail");
        } catch (InvalidConfigurationException e) {
            LOG.info("Caught expected exception: " + e);
            assertTrue(e.getMessage().contains(ERROR_MSG_REJOIN_NO_NONSTOP));
        }
    }

    @Test
    public void testInvalidRejoinInCustom() throws Exception {
        ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory, TerracottaRuntimeType.Custom);

        CacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        try {
            new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));
            fail("Running rejoin in custom mode should fail");
        } catch (InvalidConfigurationException e) {
            LOG.info("Caught Expected exception: " + e);
            assertTrue(e.getMessage().contains(ERROR_MSG_REJOIN_CUSTOM));
        }
    }

    @Test
    public void testInvalidRejoinWithoutTerracotta() throws Exception {
        ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory, (TerracottaRuntimeType) null);

        CacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        try {
            new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));
            fail("Running rejoin without Terracotta should fail");
        } catch (InvalidConfigurationException e) {
            LOG.info("Caught Expected exception: " + e);
            assertTrue(e.getMessage().contains(ERROR_MSG_REJOIN_NO_TC));
        }
    }

    @Test
    public void testAddNoNonstopCache() throws Exception {
        ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory);

        CacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        final String cacheName = "someName";
        CacheManager cacheManager = null;
        try {
            cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));

            CacheConfiguration config = new CacheConfiguration(cacheName, 10);
            config.addTerracotta(new TerracottaConfiguration().clustered(true));

            TerracottaConfiguration terracottaConfiguration = config.getTerracottaConfiguration();
            if (terracottaConfiguration.getNonstopConfiguration() != null) {
                terracottaConfiguration.getNonstopConfiguration().enabled(false);
            }

            cacheManager.addCache(new Cache(config));
            fail("Adding Terracotta caches without nonstop should fail");
        } catch (InvalidConfigurationException e) {
            LOG.info("Caught Expected exception: " + e);
            assertTrue(e.getMessage().contains(ERROR_MSG_REJOIN_NO_NONSTOP));
            assertTrue(e.getMessage().contains(cacheName));
        } finally {
            if (cacheManager != null) {
                cacheManager.shutdown();
            }
        }
    }

    @Test
    public void testAddUnclusteredCache() throws Exception {
        ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory);

        CacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        final String cacheName = "someUnclusteredCacheName";
        CacheManager cacheManager = null;
        try {
            cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));

            CacheConfiguration config = new CacheConfiguration(cacheName, 1000);
            cacheManager.addCache(new Cache(config));
            List<String> cacheNames = Arrays.asList(cacheManager.getCacheNames());
            Assert.assertTrue("Adding unclustered cache should not fail", cacheNames.contains(cacheName));

            Cache cache = cacheManager.getCache(cacheName);
            Assert.assertFalse("Unclustered cache should have terracottaClustered = false", cache.getCacheConfiguration().isTerracottaClustered());
            Assert.assertNull("Unclustered cache should have null terracotta config", cache.getCacheConfiguration().getTerracottaConfiguration());
            for (int i = 0; i < 100; i++) {
                cache.put(new Element("key-" + i, "value-" + i));
            }

            for (int i = 0; i < 100; i++) {
                String key = "key-" + i;
                Element element = cache.get(key);
                Assert.assertNotNull("Element should not be null for key: " + key, element);
                Assert.assertEquals(key, element.getKey());
                Assert.assertEquals("value-"  + i, element.getValue());
            }

        } finally {
            if (cacheManager != null) {
                cacheManager.shutdown();
            }
        }
    }

    @Test
    public void testBasicRejoin() throws Exception {
        final ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        final AtomicInteger factoryCreationCount = new AtomicInteger();
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory, new Runnable() {
            public void run() {
                factoryCreationCount.incrementAndGet();
            }
        });
        TestRejoinStore testRejoinStore = new TestRejoinStore();
        when(mockFactory.createStore((Ehcache) any())).thenReturn(testRejoinStore);

        MockCacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        CacheManager cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));
        assertEquals(1, factoryCreationCount.get());
        Cache cache = cacheManager.getCache("test");
        assertNotNull(cache);

        cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration().timeoutMillis(2000);

        cache.put(new Element("key", "value"));

        Element element = cache.get("key");
        assertNotNull(element);
        assertEquals("value", element.getValue());

        ClusterRejoinListener rejoinListener = new ClusterRejoinListener();
        cacheManager.getCluster(ClusterScheme.TERRACOTTA).addTopologyListener(rejoinListener);

        // lets simulate cluster offline with blocking behavior on test store
        testRejoinStore.setBlocking(true);

        // now gets/puts should throw exception as nonstop is configured the default behavior - "exception"
        try {
            cache.get("key");
            fail("Get should have thrown exception after cluster went offline");
        } catch (NonStopCacheException e) {
            LOG.info("Caught expected exception on get: " + e);
        }
        try {
            cache.put(new Element("newKey", "newValue"));
            fail("put should have thrown exception after cluster went offline");
        } catch (NonStopCacheException e) {
            LOG.info("Caught expected exception on put: " + e);
        }

        // lets make the cluster rejoin
        // don't forget to unblock the test store
        testRejoinStore.setBlocking(false);
        mockCacheCluster.fireCurrentNodeLeft();

        int count = 0;
        while (true) {
            if (rejoinListener.rejoinedCount.get() > 0) {
                break;
            }
            LOG.info("Waiting for rejoin to complete.. sleeping 1 sec, count=" + count);
            Thread.sleep(1000);
            if (++count >= 60) {
                LOG.info(ThreadDump.takeThreadDump());
                fail("Rejoin did not happen even after 60 seconds. Something wrong.");
            }
        }
        // assert rejoin event fired
        assertEquals(1, rejoinListener.rejoinedCount.get());
        // assert new factory created
        assertEquals(2, factoryCreationCount.get());

        // now gets/puts should go through
        element = cache.get("key");
        assertNotNull(element);
        assertEquals("value", element.getValue());

        cache.put(new Element("newKey", "newValue"));
        // assert new key-value
        element = cache.get("newKey");
        assertNotNull(element);
        assertEquals("newValue", element.getValue());

        cacheManager.shutdown();
    }

    @Test
    public void testDisposeCalledOnRejoin() throws Exception {
        final ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        final AtomicInteger factoryCreationCount = new AtomicInteger();
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory, new Runnable() {
            public void run() {
                factoryCreationCount.incrementAndGet();
            }
        });
        TestRejoinStore testRejoinStore = new TestRejoinStore();
        when(mockFactory.createStore((Ehcache) any())).thenReturn(testRejoinStore);

        MockCacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        CacheManager cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));
        assertEquals(1, factoryCreationCount.get());
        Cache cache = cacheManager.getCache("test");
        assertNotNull(cache);

        cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration().timeoutMillis(2000);

        cache.put(new Element("key", "value"));

        Element element = cache.get("key");
        assertNotNull(element);
        assertEquals("value", element.getValue());

        ClusterRejoinListener rejoinListener = new ClusterRejoinListener();
        cacheManager.getCluster(ClusterScheme.TERRACOTTA).addTopologyListener(rejoinListener);

        // clear all methods called prior to here
        testRejoinStore.getCalledMethods().clear();

        // lets make the cluster rejoin
        mockCacheCluster.fireCurrentNodeLeft();

        int count = 0;
        while (true) {
            if (rejoinListener.rejoinedCount.get() > 0) {
                break;
            }
            LOG.info("Waiting for rejoin to complete.. sleeping 1 sec, count=" + count);
            Thread.sleep(1000);
            if (++count >= 60) {
                LOG.info(ThreadDump.takeThreadDump());
                fail("Rejoin did not happen even after 60 seconds. Something wrong.");
            }
        }
        // assert rejoin event fired
        assertEquals(1, rejoinListener.rejoinedCount.get());
        // assert new factory created
        assertEquals(2, factoryCreationCount.get());

        LOG.info("Methods called during rejoin: " + testRejoinStore.getCalledMethods());
        Assert.assertTrue("dispose should have been called on rejoin", testRejoinStore.getCalledMethods().contains("dispose"));
        cacheManager.shutdown();
    }

    @Test
    public void testRejoinKeepsTryingOnException() throws Exception {
        final ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        final AtomicInteger factoryCreationCount = new AtomicInteger();
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory, new Runnable() {
            public void run() {
                factoryCreationCount.incrementAndGet();
            }
        });
        TestRejoinStore testRejoinStore = new TestRejoinStore();
        when(mockFactory.createStore((Ehcache) any())).thenReturn(testRejoinStore);

        MockCacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        CacheManager cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));
        assertEquals(1, factoryCreationCount.get());
        Cache cache = cacheManager.getCache("test");
        assertNotNull(cache);

        cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration().timeoutMillis(2000);

        ClusterRejoinListener rejoinListener = new ClusterRejoinListener();
        cacheManager.getCluster(ClusterScheme.TERRACOTTA).addTopologyListener(rejoinListener);

        // make the store keep throwing exception to fail the rejoin
        testRejoinStore.setStoreAction(StoreAction.EXCEPTION);

        // lets make the cluster rejoin
        mockCacheCluster.fireCurrentNodeLeft();

        int initialCalledMethodsSize = testRejoinStore.getCalledMethods().size();
        int calledMethodsSize = 0;
        int count = 0;
        while (true) {
            calledMethodsSize += testRejoinStore.getCalledMethods().size();
            testRejoinStore.clearCalledMethods();
            if (calledMethodsSize - initialCalledMethodsSize > 15) {
                // rejoin has been retrying, so number of called methods increasing
                break;
            }
            if (rejoinListener.rejoinedCount.get() > 0) {
                break;
            }
            LOG.info("Waiting for rejoin to complete.. sleeping 3 sec, count=" + count);
            Thread.sleep(3000);
            if (++count >= 20) {
                LOG.info(ThreadDump.takeThreadDump());
                fail("Shouldn't take 60 seconds for multiple rejoin tries");
            }
        }
        LOG.info("calledMethodSize: " + calledMethodsSize + " initial:" + initialCalledMethodsSize);
        assertTrue("Rejoin should have been retrying on getting exception ", calledMethodsSize > initialCalledMethodsSize);

        // now lets make the rejoin happen
        testRejoinStore.setStoreAction(StoreAction.NONE);
        count = 0;
        while (true) {
            if (rejoinListener.rejoinedCount.get() > 0) {
                break;
            }
            LOG.info("Waiting for rejoin to complete.. sleeping 1 sec, count=" + count);
            Thread.sleep(1000);
            if (++count >= 60) {
                LOG.info(ThreadDump.takeThreadDump());
                fail("Rejoin should have happened withing 60 seconds. Something wrong");
            }
        }

        // assert rejoin event fired
        assertEquals(1, rejoinListener.rejoinedCount.get());

        LOG.info("Methods called during rejoin: " + testRejoinStore.getCalledMethods());
        Assert.assertTrue("dispose should have been called on rejoin", testRejoinStore.getCalledMethods().contains("dispose"));
        cacheManager.shutdown();
    }

    public static class ClusterRejoinListener implements ClusterTopologyListener {
        private final AtomicInteger rejoinedCount = new AtomicInteger();

        public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
            LOG.info("========= Got cluster rejoined event: oldNode=" + printNode(oldNode) + " newNode:" + printNode(newNode));
            rejoinedCount.incrementAndGet();
        }

        public AtomicInteger getRejoinedCount() {
            return rejoinedCount;
        }

        private String printNode(ClusterNode node) {
            return "[ClusterNode: id=" + node.getId() + ", hostname=" + node.getHostname() + ", ip=" + node.getIp() + "]";
        }

        public void clusterOffline(ClusterNode node) {
            LOG.info("========= Got OFFLINE event: node=" + printNode(node));
        }

        public void clusterOnline(ClusterNode node) {
            LOG.info("========= Got ONLINE event: node=" + printNode(node));
        }

        public void nodeJoined(ClusterNode node) {
            LOG.info("========= Got NODE_JOINED event: node=" + printNode(node));
        }

        public void nodeLeft(ClusterNode node) {
            LOG.info("========= Got NODE_LEFT event: node=" + printNode(node));
        }

    }

}
