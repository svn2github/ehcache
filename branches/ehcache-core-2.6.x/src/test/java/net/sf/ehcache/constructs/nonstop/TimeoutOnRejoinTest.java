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

package net.sf.ehcache.constructs.nonstop;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.terracotta.BasicRejoinTest.ClusterRejoinListener;
import net.sf.ehcache.terracotta.ClusteredInstanceFactory;
import net.sf.ehcache.terracotta.MockCacheCluster;
import net.sf.ehcache.terracotta.TerracottaUnitTesting;
import net.sf.ehcache.terracotta.TestRejoinStore;
import net.sf.ehcache.terracotta.TestRejoinStore.StoreAction;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeoutOnRejoinTest extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutOnRejoinTest.class);
    private static final long NON_STOP_TIMEOUT_MILLIS = 3000;
    private static final long DELTA_MILLIS = 500;

    @Test
    public void testTimeoutOnRejoin() throws Exception {
        final ClusteredInstanceFactory mockFactory = mock(ClusteredInstanceFactory.class);
        TerracottaUnitTesting.setupTerracottaTesting(mockFactory);
        TestRejoinStore testRejoinStore = new TestRejoinStore();
        when(mockFactory.createStore((Ehcache) any())).thenReturn(testRejoinStore);

        MockCacheCluster mockCacheCluster = new MockCacheCluster();
        when(mockFactory.getTopology()).thenReturn(mockCacheCluster);

        CacheManager cacheManager = new CacheManager(CacheManager.class.getResourceAsStream("/rejoin/basic-rejoin-test.xml"));
        Cache cache = cacheManager.getCache("test");
        assertNotNull(cache);

        cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration().timeoutMillis(NON_STOP_TIMEOUT_MILLIS);

        cache.put(new Element("key", "value"));

        Element element = cache.get("key");
        assertNotNull(element);
        assertEquals("value", element.getValue());

        ClusterRejoinListener rejoinListener = new ClusterRejoinListener();
        cacheManager.getCluster(ClusterScheme.TERRACOTTA).addTopologyListener(rejoinListener);

        // lets simulate cluster offline
        mockCacheCluster.fireClusterOffline();
        // assert time out happens when offline, immediateTimeout=false
        cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration().immediateTimeout(false);
        assertOperationsTimeout(cache, NON_STOP_TIMEOUT_MILLIS, true);
        // assert time out happens when offline, immediateTimeout=false
        cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration().immediateTimeout(true);
        assertOperationsTimeout(cache, 0, true);

        // lets make the cluster rejoin, but let rejoin not succeed
        testRejoinStore.setStoreAction(StoreAction.EXCEPTION);
        mockCacheCluster.fireCurrentNodeLeft();

        long start = System.currentTimeMillis();
        cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration().immediateTimeout(false);
        while (true) {
            // keep doing for 60 seconds
            if (System.currentTimeMillis() - start > 60000) {
                break;
            }
            LOGGER.info("Asserting operations times out with set timeoutMillis (immediateTimeout=false)");
            assertOperationsTimeout(cache, NON_STOP_TIMEOUT_MILLIS, true);
        }

        start = System.currentTimeMillis();
        cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration().immediateTimeout(true);
        LOGGER.info("Asserting operations times out with set timeoutMillis (immediateTimeout=true)");
        while (true) {
            // keep doing for 4 seconds
            if (System.currentTimeMillis() - start > 4000) {
                break;
            }
            assertOperationsTimeout(cache, 0, false);
        }

        // now let the rejoin go through
        testRejoinStore.setStoreAction(StoreAction.NONE);

        // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // ///////////////////////// READ ME /////////////////////////////////////////////////////////////////////////////////
        // If this test fails, then probably methods in Store interface that are used in Cache.initialize() should be using
        // forceExecuteWithExecutor() method in ExecutorServiceStore
        // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        int count = 0;
        while (true) {
            if (rejoinListener.getRejoinedCount().get() > 0) {
                break;
            }
            LOGGER.info("Waiting for rejoin to complete.. sleeping 1 sec, count=" + count);
            Thread.sleep(1000);
            if (++count >= 60) {
                LOGGER.info(ThreadDump.takeThreadDump());
                fail("Rejoin did not happen even after 60 seconds. Something wrong.");
            }
        }
        // assert rejoin event fired
        assertEquals(1, rejoinListener.getRejoinedCount().get());
        cache.getCacheConfiguration().getTerracottaConfiguration().getNonstopConfiguration().immediateTimeout(false);

        System.out.println(new Date() + ": Asserting operations go through");
        assertOperationsGoThrough(cache);
        System.out.println(new Date() + ": Test passed successfully");
    }

    private void assertOperationsGoThrough(Cache cache) throws Exception {
        try {
            Element element;
            // now gets/puts should go through
            System.out.println(new Date() + ": Doing get");
            element = cache.get("key");
            assertNotNull(element);
            assertEquals("value", element.getValue());

            System.out.println(new Date() + ": Doing put");
            cache.put(new Element("newKey", "newValue"));
            // assert new key-value
            element = cache.get("newKey");
            assertNotNull(element);
            assertEquals("newValue", element.getValue());
            System.out.println(new Date() + ": Test Done");
        } catch (Exception e) {
            System.out.println(new Date() + ": Test failed");
            throw e;
        }
    }

    private void assertOperationsTimeout(Cache cache, long expectedTimeoutMillis, boolean log) {
        // now gets/puts should throw exception as nonstop is configured the default behavior - "exception"
        long start = System.currentTimeMillis();
        try {
            cache.get("key");
            fail("Get should have thrown exception after cluster went offline");
        } catch (NonStopCacheException e) {
            long elapsed = System.currentTimeMillis() - start;
            if (log) {
                LOGGER.info("+++++++ Caught expected exception on get: " + e);
                LOGGER.info("+++++++ Elapsed time before getting nonstop cache exception: " + elapsed);
            }
            Assert.assertTrue("expected timeout: " + expectedTimeoutMillis + " actual: " + elapsed,
                    elapsed + DELTA_MILLIS >= expectedTimeoutMillis);
        }
        start = System.currentTimeMillis();
        try {
            cache.put(new Element("newKey", "newValue"));
            fail("put should have thrown exception after cluster went offline");
        } catch (NonStopCacheException e) {
            long elapsed = System.currentTimeMillis() - start;
            if (log) {
                LOGGER.info("+++++++ Caught expected exception on put: " + e);
                LOGGER.info("+++++++ Elapsed time before getting nonstop cache exception: " + elapsed);
            }
            Assert.assertTrue("expected timeout: " + expectedTimeoutMillis + " actual: " + elapsed,
                    elapsed + DELTA_MILLIS >= expectedTimeoutMillis);
        }
    }

}
