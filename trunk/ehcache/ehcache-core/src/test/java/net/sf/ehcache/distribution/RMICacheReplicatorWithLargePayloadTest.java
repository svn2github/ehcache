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
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests replication of Cache events with large payloads exceeding MTU
 * <p/>
 * Note these tests need a live network interface running in multicast mode to work
 * <p/>
 *
 * @author Abhishek Sanoujam
 */
public class RMICacheReplicatorWithLargePayloadTest extends AbstractRMITest {

    private static final Logger LOG = Logger.getLogger(RMICacheReplicatorWithLargePayloadTest.class.getName());

    private static int MB = 1024 * 1024;

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
     * {@inheritDoc} Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        failFastInsufficientMemory();

        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);

        List<Configuration> configurations = new ArrayList<Configuration>();
        configurations.add(ConfigurationFactory.parseConfiguration(new File(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-1.xml")).name("cm1"));
        configurations.add(ConfigurationFactory.parseConfiguration(new File(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-2.xml")).name("cm2"));
        configurations.add(ConfigurationFactory.parseConfiguration(new File(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-3.xml")).name("cm2"));
        List<CacheManager> managers = startupManagers(configurations);
        manager1 = managers.get(0);
        manager2 = managers.get(1);
        manager3 = managers.get(2);
        // allow cluster to be established
        waitForClusterMembership(10, TimeUnit.SECONDS, Arrays.asList(manager1.getCacheNames()), manager1, manager2, manager3);
    }

    private void failFastInsufficientMemory() {
        // fail fast if running with insufficient heap
        long totalMemory = Runtime.getRuntime().totalMemory();
        if (totalMemory < 200 * MB) {
            String msg = "Insufficient heap (approx. " + (totalMemory / MB) + " MB detected), this test requires at least 256 MB to run.\n";
            msg += "Steps to take:\n";
            msg += "   1) If you are running with eclipse: specify \"-Xms256m -Xmx256m\" as VM arguments in the \"Run Confuguration\" for this test\n";
            msg += "   2) If you are running using mvn with \"mvn test -Dtest=" + this.getClass().getSimpleName()
                    + "\", add this in the command line: -DargLine=\"-Xms256m -Xmx256m\"\n";
            msg += "      Run the test like: mvn test -Dtest=" + this.getClass().getSimpleName() + " -DargLine=\"-Xms256m -Xmx256m\"";
            LOG.log(Level.WARNING, msg);
            fail(msg);
        }
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

        RetryAssert.assertBy(30, TimeUnit.SECONDS, new Callable<Set<Thread>>() {
            public Set<Thread> call() throws Exception {
                return getActiveReplicationThreads();
            }
        }, IsEmptyCollection.<Thread>empty());
    }

    @Test
    public void testAssertBigPayload() {
        List<CachePeer> localPeers = manager1.getCachePeerListener("RMI").getBoundCachePeers();
        List<byte[]> payloadList = PayloadUtil.createCompressedPayloadList(localPeers, 150);
        Assert.assertTrue("Payload is not big enough for cacheManager-1", payloadList.size() > 1);

        localPeers = manager2.getCachePeerListener("RMI").getBoundCachePeers();
        payloadList = PayloadUtil.createCompressedPayloadList(localPeers, 150);
        Assert.assertTrue("Payload is not big enough for cacheManager-2", payloadList.size() > 1);

        localPeers = manager3.getCachePeerListener("RMI").getBoundCachePeers();
        payloadList = PayloadUtil.createCompressedPayloadList(localPeers, 150);
        Assert.assertTrue("Payload is not big enough for cacheManager-3", payloadList.size() > 1);

        CacheManager manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-4.xml");
        try {
            localPeers = manager4.getCachePeerListener("RMI").getBoundCachePeers();
            payloadList = PayloadUtil.createCompressedPayloadList(localPeers, 150);
            Assert.assertTrue("Payload is not big enough for cacheManager-4", payloadList.size() > 1);
        } finally {
            manager4.shutdown();
        }
    }

    /**
     * Does a new cache manager in the cluster get detected?
     */
    @Test
    public void testRemoteCachePeersDetectsNewCacheManager() throws InterruptedException {
        // Add new CacheManager to cluster
        CacheManager manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-4.xml");
        try {
            // Allow detection to occur
            waitForClusterMembership(10020, TimeUnit.MILLISECONDS, Arrays.asList(manager1.getCacheNames()), manager1, manager2, manager3, manager4);
        } finally {
            manager4.shutdown();
        }
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    @Test
    public void testRemoteCachePeersDetectsDownCacheManager() throws InterruptedException {
        // Drop a CacheManager from the cluster
        manager3.shutdown();
        // Allow change detection to occur. Heartbeat 1 second and is not stale until 5000
        waitForClusterMembership(11020, TimeUnit.MILLISECONDS, Arrays.asList(manager1.getCacheNames()), manager1, manager2);
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    @Test
    public void testRemoteCachePeersDetectsDownCacheManagerSlow() throws InterruptedException {
        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(2000);
        try {
            Thread.sleep(2000);
            // Drop a CacheManager from the cluster
            manager3.shutdown();

            // Insufficient time, should be alive till now
            CacheManagerPeerProvider provider = manager1.getCacheManagerPeerProvider("RMI");
            for (String cacheName : manager1.getCacheNames()) {
                List remotePeersOfCache1 = provider.listRemoteCachePeers(manager1.getCache(cacheName));
                assertEquals(2, remotePeersOfCache1.size());
            }
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
    public void testPutProgagatesFromAndToEveryCacheManagerAndCache() throws CacheException, InterruptedException {

        // Put
        final String[] cacheNames = manager1.getCacheNames();
        Arrays.sort(cacheNames);
        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            manager1.getCache(name).put(new Element(Integer.toString(i), Integer.valueOf(i)));
            // Add some non serializable elements that should not get propagated
            manager1.getCache(name).put(new Element("nonSerializable" + i, new Object()));
        }

        assertBy(10, TimeUnit.SECONDS, new Callable<Boolean>() {

            public Boolean call() throws Exception {
                for (int i = 0; i < cacheNames.length; i++) {
                    String name = cacheNames[i];
                    for (CacheManager manager : new CacheManager[] {manager2, manager3}) {
                        Element element = manager.getCache(name).get(Integer.toString(i));
                        assertNotNull("Cache : " + name, element);
                        assertEquals(Integer.toString(i), element.getKey());
                        assertEquals(Integer.valueOf(i), element.getValue());

                        assertNull(manager.getCache(name).get("nonSerializable" + i));
                    }
                }
                return Boolean.TRUE;
            }
        }, is(Boolean.TRUE));
    }
}
