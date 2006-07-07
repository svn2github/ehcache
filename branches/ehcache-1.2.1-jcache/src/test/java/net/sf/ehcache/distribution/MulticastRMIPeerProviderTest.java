/**
 *  Copyright 2003-2006 Greg Luck
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

import junit.framework.TestCase;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Multicast tests. These require special machine configuration.
 * <p/>
 * Running on a single machine, as these tests do, you need to add a route command so that two multiCast sockets
 * can be added at the same time.
 * <ol>
 * <li>Mac OSX: <code>route add -net 224.0.0.0 -interface lo0</code>
 * <li>Linux (from JGroups doco, untested): <code>route add -net 224.0.0.0 netmask 224.0.0.0 dev lo</code>
 * </ol>
 *
 * @author Greg Luck
 * @version $Id$
 */
public class MulticastRMIPeerProviderTest extends TestCase {

    private static final Log LOG = LogFactory.getLog(MulticastRMIPeerProviderTest.class.getName());

    /**
     * Cache Manager 1
     */
    protected CacheManager manager1;
    /**
     * Cache Manager 2
     */
    protected CacheManager manager2;
    /**
     * Cache Manager 3
     */
    protected CacheManager manager3;

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }
        MulticastKeepaliveHeartbeatSender.setHeartBeatInterval(1000);
        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed2.xml");
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml");

        //wait for cluster to establish
        Thread.sleep(2000);
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        manager1.shutdown();
        manager2.shutdown();
        manager3.shutdown();
    }

    /**
     * Make sure no exceptions get logged. Manual inspection.
     */
    public void testSolePeer() throws Exception {
        tearDown();

        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR
                + "distribution/ehcache-distributed-no-caches-replicating.xml");
    }

    /**
     * test remote cache peers
     */
    public void testProviderFromCacheManager() throws InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        Ehcache m1sampleCache1 = manager1.getCache("sampleCache1");
        Thread.sleep(2000);

        List peerUrls = manager1.getCachePeerProvider().listRemoteCachePeers(m1sampleCache1);
        assertEquals(expectedPeers(), peerUrls.size());

        Ehcache m2sampleCache1 = manager2.getCache("sampleCache1");
        assertFalse(m1sampleCache1.getGuid().equals(m2sampleCache1.getGuid()));

        List peerUrls2 = manager2.getCachePeerProvider().listRemoteCachePeers(m2sampleCache1);
        assertEquals(expectedPeers(), peerUrls2.size());

        Ehcache m3sampleCache1 = manager3.getCache("sampleCache1");
        assertFalse(m1sampleCache1.getGuid().equals(m3sampleCache1.getGuid()));

        List peerUrls3 = manager3.getCachePeerProvider().listRemoteCachePeers(m3sampleCache1);
        assertEquals(expectedPeers(), peerUrls3.size());
    }

    /**
     * The default caches for ehcache-dsitributed1-6.xml are set to replicate.
     * We create a new cache from the default and expect it to be replicated.
     */
    public void testProviderCreatedFromDefaultCache() throws InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        //manual does not nor should it work this way
        if (this.getClass() != MulticastRMIPeerProviderTest.class) {
            return;
        }

        manager1.addCache("fromDefaultCache");
        RMICacheManagerPeerListener peerListener1 = (RMICacheManagerPeerListener) manager1.getCachePeerListener();
        //peerListener1.notifyCacheAdded("fromDefaultCache");
        manager2.addCache("fromDefaultCache");
        RMICacheManagerPeerListener peerListener2 = (RMICacheManagerPeerListener) manager2.getCachePeerListener();
        //peerListener2.notifyCacheAdded("fromDefaultCache");
        manager3.addCache("fromDefaultCache");
        RMICacheManagerPeerListener peerListener3 = (RMICacheManagerPeerListener) manager3.getCachePeerListener();
        //peerListener3.notifyCacheAdded("fromDefaultCache");
        Thread.sleep(2000);

        CacheManagerPeerProvider cachePeerProvider = manager1.getCachePeerProvider();

        Cache cache = manager1.getCache("fromDefaultCache");
        List peerUrls = cachePeerProvider.listRemoteCachePeers(cache);
        assertEquals(expectedPeers(), peerUrls.size());

    }


    /**
     * The default caches for ehcache-dsitributed1-6.xml are set to replicate.
     * We create a new cache from the default and expect it to be replicated.
     */
    public void testDeleteReplicatedCache() throws InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        //manual does not nor should it work this way
        if (this.getClass() != MulticastRMIPeerProviderTest.class) {
            return;
        }

        manager1.addCache("fromDefaultCache");
        manager2.addCache("fromDefaultCache");
        manager3.addCache("fromDefaultCache");
        Thread.sleep(2200);

        CacheManagerPeerProvider cachePeerProvider = manager1.getCachePeerProvider();
        Cache cache = manager1.getCache("fromDefaultCache");

        //Should be three
        List peerUrls = cachePeerProvider.listRemoteCachePeers(cache);
        assertEquals(expectedPeers(), peerUrls.size());


        manager1.removeCache("fromDefaultCache");
        Thread.sleep(2200);

        peerUrls = cachePeerProvider.listRemoteCachePeers(cache);
        assertEquals(expectedPeers(), peerUrls.size());

    }

    /**
     * There are 3 in the cluster, so there will be two others
     */
    protected int expectedPeers() {
        return 2;
    }


    /**
     * Tests the speed of remotely looking up.
     *
     * @throws RemoteException
     * @throws InterruptedException .19ms
     *                              This seems to imply a maximum of 5000 per second best case. Not bad.
     */
    public void testRemoteGetName() throws RemoteException, InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        Ehcache m1sampleCache1 = manager1.getCache("sampleCache1");
        Thread.sleep(2000);
        List peerUrls = manager1.getCachePeerProvider().listRemoteCachePeers(m1sampleCache1);

        CachePeer m1SampleCach1Peer = (CachePeer) peerUrls.get(0);

        for (int i = 0; i < 100; i++) {
            m1SampleCach1Peer.getName();
        }
        Thread.sleep(2000);

        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 1000; i++) {
            m1SampleCach1Peer.getName();
        }
        long time = stopWatch.getElapsedTime();

        LOG.info("Remote name lookup time in ms: " + time / 1000f);

    }

}
