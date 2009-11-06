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

package net.sf.ehcache.distribution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CountingCacheEventListener;

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

public class RMICacheReplicatorWithLargePayloadTest extends AbstractCacheTest {

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
     * CacheManager 4 in the cluster
     */
    protected CacheManager manager4;

    /**
     * {@inheritDoc} Sets up two caches: cache1 is local. cache2 is to be receive updates
     * 
     * @throws Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        failFastInsufficientMemory();

        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);

        CountingCacheEventListener.resetCounters();
        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-2.xml");
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-3.xml");
        // allow cluster to be established
        Thread.sleep(1020);
        // enable distributed removeAlls to finish
        waitForPropagate();
    }

    private void failFastInsufficientMemory() {
        // fail fast if running with insufficient heap
        long totalMemory = Runtime.getRuntime().totalMemory();
        if (totalMemory < 250 * MB) {
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
    @Override
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
        Thread.sleep(2000);

        List threads = JVMUtil.enumerateThreads();
        for (int i = 0; i < threads.size(); i++) {
            Thread thread = (Thread) threads.get(i);
            if (thread.getName().equals("Replication Thread")) {
                fail("There should not be any replication threads running after shutdown");
            }
        }

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

        manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-4.xml");
        localPeers = manager4.getCachePeerListener("RMI").getBoundCachePeers();
        payloadList = PayloadUtil.createCompressedPayloadList(localPeers, 150);
        Assert.assertTrue("Payload is not big enough for cacheManager-4", payloadList.size() > 1);

        manager4.shutdown();
    }

    /**
     * 3 cache managers should means that each cache has two remote peers
     */

    @Test
    public void testRemoteCachePeersEqualsNumberOfCacheManagersInCluster() {

        doTestRemoteCachePeers(2);
    }

    private void doTestRemoteCachePeers(int expectedRemotePeerCount) {
        CacheManagerPeerProvider provider = manager1.getCacheManagerPeerProvider("RMI");
        for (String cacheName : manager1.getCacheNames()) {
            List remotePeersOfCache1 = provider.listRemoteCachePeers(manager1.getCache(cacheName));
            assertEquals(expectedRemotePeerCount, remotePeersOfCache1.size());
        }
    }

    /**
     * Does a new cache manager in the cluster get detected?
     */

    @Test
    public void testRemoteCachePeersDetectsNewCacheManager() throws InterruptedException {
        doTestRemoteCachePeers(2);
        // Add new CacheManager to cluster
        manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-4.xml");
        // Allow detection to occur
        Thread.sleep(10020);
        doTestRemoteCachePeers(3);
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */

    @Test
    public void testRemoteCachePeersDetectsDownCacheManager() throws InterruptedException {
        doTestRemoteCachePeers(2);
        // Drop a CacheManager from the cluster
        manager3.shutdown();
        // Allow change detection to occur. Heartbeat 1 second and is not stale until 5000
        Thread.sleep(11020);
        doTestRemoteCachePeers(1);
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */

    @Test
    public void testRemoteCachePeersDetectsDownCacheManagerSlow() throws InterruptedException {

        try {
            doTestRemoteCachePeers(2);

            MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(2000);
            Thread.sleep(2000);

            // Drop a CacheManager from the cluster
            manager3.shutdown();

            // Insuffiecient time, should be alive till now
            doTestRemoteCachePeers(2);
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
        String[] cacheNames = manager1.getCacheNames();
        int numberOfCaches = 3;// getNumberOfReplicatingCachesInCacheManager();
        Arrays.sort(cacheNames);
        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            manager1.getCache(name).put(new Element("" + i, new Integer(i)));
            // Add some non serializable elements that should not get propagated
            manager1.getCache(name).put(new Element("nonSerializable" + i, new Object()));
        }

        waitForPropagate();
        
        for (int i = 0; i < cacheNames.length; i++) {
            String name = cacheNames[i];
            Element element = manager2.getCache(name).get("" + i);
            assertNotNull(element);
            assertEquals("" + i, element.getKey());
            assertEquals(new Integer(i), element.getValue());

            element = manager2.getCache(name).get("nonSerializable" + i);
            assertNull(element);

            element = manager3.getCache(name).get("" + i);
            assertNotNull(element);
            assertEquals("" + i, element.getKey());
            assertEquals(new Integer(i), element.getValue());

            element = manager3.getCache(name).get("nonSerializable" + i);
            assertNull(element);
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

}
