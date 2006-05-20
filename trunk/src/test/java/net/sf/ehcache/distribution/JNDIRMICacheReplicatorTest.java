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

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.event.CountingCacheEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.Context;
import javax.naming.spi.InitialContextFactory;
import java.util.List;

/**
 * Tests replication of Cache events with
 * JNDIRMICacheManagerPeerListener and JNDIManualRMICacheManagerPeerProvider.
 * <p/>
 * MockContextFactory and MockContext are used to mock JNDI.
 *
 * @author Greg Luck
 * @author Andy McNutt
 * @version $Id$
 * @see RMICacheReplicatorTest
 */
public class JNDIRMICacheReplicatorTest extends RMICacheReplicatorTest {

    private static final Log LOG = LogFactory.getLog(JNDIRMICacheReplicatorTest.class.getName());

    /**
     * Constructor, for suites.
     * @param name
     */
    public JNDIRMICacheReplicatorTest(String name) {
        super(name);
    }

    /**
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MockContextFactory.class.getName());
        new MockContextFactory().clear();
        CountingCacheEventListener.resetCounters();
        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi2.xml");
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi3.xml");
        manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi4.xml");
        manager5 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi5.xml");

//        manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR
//                + "distribution/ehcache-distributed-jndi6.xml");

        cache1 = manager1.getCache(cacheName);

        cache1.removeAll();

        cache2 = manager2.getCache(cacheName);
        cache2.removeAll();

        //allow cluster to be established
        Thread.sleep(6000);
    }

    /**
     * todo The JNDI replicator uses more memory than the RMI one. Investigate.
     * @throws CacheException
     * @throws InterruptedException
     */
    public void testBigPutsProgagatesAsynchronous() throws CacheException, InterruptedException {
        forceVMGrowth();
        super.testBigPutsProgagatesAsynchronous();
    }

    /**
     * todo The JNDI replicator uses more memory than the RMI one. Investigate.
     * @throws CacheException
     * @throws InterruptedException
     */
    public void testBigRemovesProgagatesAsynchronous() throws CacheException, InterruptedException {
        forceVMGrowth();
        super.testBigPutsProgagatesAsynchronous();
    }


    /**
     * Does a new cache manager in the cluster get detected?
     */
    public void testRemoteCachePeersDetectsNewCacheManager() throws InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        CacheManagerPeerProvider provider = manager1.getCachePeerProvider();
        List remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
        assertEquals(4, remotePeersOfCache1.size());

        // Add new CacheManager to cluster
        manager6 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-jndi6.xml");

        // Allow detection to occur
        Thread.sleep(1010);

        remotePeersOfCache1 = provider.listRemoteCachePeers(cache1);
        assertEquals(5, remotePeersOfCache1.size());
    }

    /**
     * The number of caches there should be.
     */
    protected int getNumberOfReplicatingCachesInCacheManager() {
        return 7;
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    public void testRemoteCachePeersDetectsDownCacheManager() throws InterruptedException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        // manager1 has stashContexts=false, stashRemoteCachePeers=false
        // so it looks up the peers each call of listRemoteCachePeers
        CacheManagerPeerProvider providerManager1 = manager1.getCachePeerProvider();
        List remotePeersOfCache1 = providerManager1.listRemoteCachePeers(cache1);
        int targetNumberPeers = 4;
        assertEquals(remotePeersOfCache1.size()
                + " remotePeersOfCache1 should be " + targetNumberPeers
                + " for manager1, remotePeersOfCache1=" + remotePeersOfCache1
                + "  " + getContextFactory(),
                targetNumberPeers, remotePeersOfCache1.size());

        CacheManagerPeerProvider providerManager2 = manager2.getCachePeerProvider();
        remotePeersOfCache1 = providerManager2.listRemoteCachePeers(cache1);
        assertEquals(remotePeersOfCache1.size()
                + " remotePeersOfCache1 should be " + targetNumberPeers
                + " for manager2, remotePeersOfCache1=" + remotePeersOfCache1
                + "  " + getContextFactory(),
                targetNumberPeers, remotePeersOfCache1.size());

        // Drop a CacheManager from the cluster
        manager5.shutdown();

        remotePeersOfCache1 = providerManager1.listRemoteCachePeers(cache1);
        targetNumberPeers = 3;
        assertEquals(remotePeersOfCache1.size()
                + " remotePeersOfCache1 should be " + targetNumberPeers
                + " for manager1, remotePeersOfCache1=" + remotePeersOfCache1
                + "  " + getContextFactory(),
                targetNumberPeers, remotePeersOfCache1.size());

        // manager2 defaults to stashContexts=true, stashRemoteCachePeers=true
        // so it finds stashed peers each call of listRemoteCachePeers
        targetNumberPeers = 4;
        remotePeersOfCache1 = providerManager2.listRemoteCachePeers(cache1);
        assertEquals(remotePeersOfCache1.size()
                + " remotePeersOfCache1 should be " + targetNumberPeers
                + " for manager2, remotePeersOfCache1=" + remotePeersOfCache1
                + "  " + getContextFactory(),
                targetNumberPeers, remotePeersOfCache1.size());

        // Drop a CacheManager from the cluster
        manager4.shutdown();

        remotePeersOfCache1 = providerManager1.listRemoteCachePeers(cache1);
        // didn't stash this peer either
        targetNumberPeers = 2;
        assertEquals(remotePeersOfCache1.size()
                + " remotePeersOfCache1 should be " + targetNumberPeers
                + " for manager1, remotePeersOfCache1=" + remotePeersOfCache1
                + "  " + getContextFactory(),
                targetNumberPeers, remotePeersOfCache1.size());

        remotePeersOfCache1 = providerManager2.listRemoteCachePeers(cache1);
        // still 4 because stash peers
        targetNumberPeers = 4;
        assertEquals(remotePeersOfCache1.size()
                + " remotePeersOfCache1 should be " + targetNumberPeers
                + " for manager2, remotePeersOfCache1=" + remotePeersOfCache1
                + "  " + getContextFactory(),
                targetNumberPeers, remotePeersOfCache1.size());
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    public void testRemoteCachePeersDetectsDownCacheManagerSlow() throws InterruptedException {
        //Does not work because JNDI knows straightaway when an RMICachePeer is uncontactable
    }


    /**
     * manager1 adds a replicating cache, then manager2 and so on. Then we remove one. Does everything work as expected?
     */
    public void testPutWithNewCacheAddedProgressively() throws InterruptedException {
        //don't run because JNDI is manual and will not change its config dynamically.
    }


    private InitialContextFactory getContextFactory() {
        return new MockContextFactory();
    }

}
