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

package net.sf.ehcache.terracotta;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.terracotta.TerracottaClusteredInstanceHelper.TerracottaRuntimeType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class BasicRejoinTest extends TestCase {

    private static final String ERROR_MSG_REJOIN_CUSTOM = "Rejoin can be enabled only in Terracotta Express mode";
    private static final String ERROR_MSG_REJOIN_NO_NONSTOP = "Terracotta clustered caches must be nonstop when rejoin is enabled";
    private static final CharSequence ERROR_MSG_REJOIN_NO_TC = "Terracotta Rejoin is enabled but can't determine Terracotta Runtime. You are probably missing Terracotta jar(s)";

    private void setupTestMode(ClusteredInstanceFactory mockFactory) throws Exception {
        setupTestMode(mock(TerracottaClusteredInstanceHelper.class), mockFactory);
    }

    private void setupTestMode(TerracottaClusteredInstanceHelper mockHelper, ClusteredInstanceFactory mockFactory) throws Exception {
        setupTestMode(mockHelper, mockFactory, new TestRejoinStore());
    }

    private void setupTestMode(TerracottaClusteredInstanceHelper mockHelper, ClusteredInstanceFactory mockFactory, Store mockStore)
            throws Exception {
        when(mockHelper.getTerracottaRuntimeTypeOrNull()).thenReturn(TerracottaRuntimeType.Express);
        when(mockHelper.newClusteredInstanceFactory((Map<String, CacheConfiguration>) any(), (TerracottaClientConfiguration) any()))
                .thenReturn(mockFactory);
        when(mockFactory.createStore((Ehcache) any())).thenReturn(mockStore);
        when(mockHelper.getDefaultStorageStrategyForCurrentRuntime()).thenReturn(StorageStrategy.CLASSIC);

        Method method = TerracottaClient.class.getDeclaredMethod("setTestMode", TerracottaClusteredInstanceHelper.class);
        method.setAccessible(true);
        method.invoke(null, mockHelper);
    }

    @Test
    public void testInvalidRejoinWithoutNonstop() throws Exception {
        ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        setupTestMode(mockFactory);

        CacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        try {
            new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/invalid-rejoin-no-nonstop-test.xml"));
            fail("Trying to run rejoin without nonstop terracotta caches should fail");
        } catch (InvalidConfigurationException e) {
            System.out.println("Caught expected exception: " + e);
            assertTrue(e.getMessage().contains(ERROR_MSG_REJOIN_NO_NONSTOP));
        }
    }

    @Test
    public void testInvalidRejoinInCustom() throws Exception {
        TerracottaClusteredInstanceHelper mockHelper = mock(TerracottaClusteredInstanceHelper.class);
        ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        setupTestMode(mockHelper, mockFactory);

        CacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        // run in classic mode
        when(mockHelper.getTerracottaRuntimeTypeOrNull()).thenReturn(TerracottaRuntimeType.Custom);

        try {
            new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));
            fail("Running rejoin in custom mode should fail");
        } catch (InvalidConfigurationException e) {
            System.out.println("Caught Expected exception: " + e);
            assertTrue(e.getMessage().contains(ERROR_MSG_REJOIN_CUSTOM));
        }
    }

    @Test
    public void testInvalidRejoinWithoutTerracotta() throws Exception {
        TerracottaClusteredInstanceHelper mockHelper = mock(TerracottaClusteredInstanceHelper.class);
        ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        setupTestMode(mockHelper, mockFactory);

        CacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        // run in classic mode
        when(mockHelper.getTerracottaRuntimeTypeOrNull()).thenReturn(null);

        try {
            new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));
            fail("Running rejoin without Terracotta should fail");
        } catch (InvalidConfigurationException e) {
            System.out.println("Caught Expected exception: " + e);
            assertTrue(e.getMessage().contains(ERROR_MSG_REJOIN_NO_TC));
        }
    }

    @Test
    public void testAddNoNonstopCache() throws Exception {
        TerracottaClusteredInstanceHelper mockHelper = mock(TerracottaClusteredInstanceHelper.class);
        ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        setupTestMode(mockHelper, mockFactory);

        CacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        final String cacheName = "someName";
        try {
            CacheManager cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));

            CacheConfiguration config = new CacheConfiguration(cacheName, 10);
            config.addTerracotta(new TerracottaConfiguration().clustered(true));

            TerracottaConfiguration terracottaConfiguration = config.getTerracottaConfiguration();
            if (terracottaConfiguration.getNonstopConfiguration() != null) {
                terracottaConfiguration.getNonstopConfiguration().enabled(false);
            }

            cacheManager.addCache(new Cache(config));
            fail("Adding Terracotta caches without nonstop should fail");
        } catch (InvalidConfigurationException e) {
            System.out.println("Caught Expected exception: " + e);
            assertTrue(e.getMessage().contains(ERROR_MSG_REJOIN_NO_NONSTOP));
            assertTrue(e.getMessage().contains(cacheName));
        }
    }

    @Test
    public void testBasicRejoin() throws Exception {
        final TerracottaClusteredInstanceHelper mockHelper = mock(TerracottaClusteredInstanceHelper.class);
        final ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        TestRejoinStore testRejoinStore = new TestRejoinStore();
        setupTestMode(mockHelper, mockFactory, testRejoinStore);

        MockCacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        final AtomicInteger factoryCreationCount = new AtomicInteger();
        when(mockHelper.newClusteredInstanceFactory((Map<String, CacheConfiguration>) any(), (TerracottaClientConfiguration) any()))
                .thenAnswer(new Answer<ClusteredInstanceFactory>() {

                    public ClusteredInstanceFactory answer(InvocationOnMock invocation) throws Throwable {
                        factoryCreationCount.incrementAndGet();
                        return mockFactory;
                    }

                });

        CacheManager cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));
        assertEquals(1, factoryCreationCount.get());
        Cache cache = cacheManager.getCache("test");
        assertNotNull(cache);

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
            System.out.println("Caught expected exception on get: " + e);
        }
        try {
            cache.put(new Element("newKey", "newValue"));
            fail("put should have thrown exception after cluster went offline");
        } catch (NonStopCacheException e) {
            System.out.println("Caught expected exception on put: " + e);
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
            System.out.println("Waiting for rejoin to complete.. sleeping 1 sec");
            Thread.sleep(1000);
            if (++count >= 60) {
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
    }

    private static class MockCacheCluster implements CacheCluster {

        private final List<ClusterTopologyListener> listeners = new CopyOnWriteArrayList<ClusterTopologyListener>();
        private final ClusterNode currentNode = new ClusterNode() {

            public String getIp() {
                return "127.0.0.1";
            }

            public String getId() {
                return "1";
            }

            public String getHostname() {
                return "dummyHostName";
            }
        };

        public void fireCurrentNodeLeft() {
            for (ClusterTopologyListener listener : listeners) {
                listener.nodeLeft(currentNode);
            }
        }

        public void fireClusterOffline() {
            for (ClusterTopologyListener listener : listeners) {
                listener.clusterOffline(currentNode);
            }
        }

        public void fireClusterOnline() {
            for (ClusterTopologyListener listener : listeners) {
                listener.clusterOnline(currentNode);
            }
        }

        public boolean addTopologyListener(ClusterTopologyListener listener) {
            return listeners.add(listener);
        }

        public ClusterNode getCurrentNode() {
            return currentNode;
        }

        public Collection<ClusterNode> getNodes() {
            return Collections.singletonList(currentNode);
        }

        public ClusterScheme getScheme() {
            return ClusterScheme.TERRACOTTA;
        }

        public boolean isClusterOnline() {
            return true;
        }

        public boolean removeTopologyListener(ClusterTopologyListener listener) {
            return listeners.remove(listener);
        }

        public ClusterNode waitUntilNodeJoinsCluster() {
            return currentNode;
        }

    }

    private static class ClusterRejoinListener implements ClusterTopologyListener {
        private final AtomicInteger rejoinedCount = new AtomicInteger();

        public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
            System.out.println("Got cluster rejoined event: oldNode=" + printNode(oldNode) + " newNode:" + printNode(newNode));
            rejoinedCount.incrementAndGet();
        }

        private String printNode(ClusterNode node) {
            return "[ClusterNode: id=" + node.getId() + ", hostname=" + node.getHostname() + ", ip=" + node.getIp() + "]";
        }

        public void clusterOffline(ClusterNode node) {
            // TODO Auto-generated method stub

        }

        public void clusterOnline(ClusterNode node) {
            // TODO Auto-generated method stub

        }

        public void nodeJoined(ClusterNode node) {
            // TODO Auto-generated method stub

        }

        public void nodeLeft(ClusterNode node) {
            // TODO Auto-generated method stub

        }

    }

}
