/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CountingCacheEventListener;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Unit tests for the RMICacheManagerPeerListener
 * <p/>
 * Note these tests need a live network interface running in multicast mode to work
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class RMICacheManagerPeerListenerTest extends TestCase {

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
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        CountingCacheEventListener.resetCounters();
        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed2.xml");
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed3.xml");
        manager4 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed4.xml");
        manager5 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed5.xml");

        manager1.getCache(cacheName).removeAll();

        cache1 = manager1.getCache(cacheName);
        cache1.removeAll();

        cache2 = manager2.getCache(cacheName);
        cache2.removeAll();

        //allow cluster to be established
        Thread.sleep(100);

    }


    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }


        manager1.shutdown();
        manager2.shutdown();
        manager3.shutdown();
        manager4.shutdown();
        if (manager5 != null) {
            manager5.shutdown();
        }

        if (manager6 != null) {
            manager6.shutdown();
        }
    }


    /**
     * Are all of the replicated caches bound to the RMI listener?
     */
    public void testPeersBound() {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        List cachePeers1 = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).getBoundCachePeers();
        assertEquals(55, cachePeers1.size());
        String[] boundCachePeers1 = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(55, boundCachePeers1.length);
        assertEquals(cachePeers1.size(), boundCachePeers1.length);

        List cachePeers2 = ((RMICacheManagerPeerListener) manager2.getCachePeerListener()).getBoundCachePeers();
        assertEquals(55, cachePeers2.size());
        String[] boundCachePeers2 = ((RMICacheManagerPeerListener) manager2.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(55, boundCachePeers2.length);
        assertEquals(cachePeers2.size(), boundCachePeers2.length);


        List cachePeers3 = ((RMICacheManagerPeerListener) manager3.getCachePeerListener()).getBoundCachePeers();
        assertEquals(55, cachePeers3.size());
        String[] boundCachePeers3 = ((RMICacheManagerPeerListener) manager3.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(55, boundCachePeers3.length);
        assertEquals(cachePeers3.size(), boundCachePeers3.length);


        List cachePeers4 = ((RMICacheManagerPeerListener) manager4.getCachePeerListener()).getBoundCachePeers();
        assertEquals(55, cachePeers4.size());
        String[] boundCachePeers4 = ((RMICacheManagerPeerListener) manager4.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(55, boundCachePeers4.length);
        assertEquals(cachePeers4.size(), boundCachePeers4.length);

        List cachePeers5 = ((RMICacheManagerPeerListener) manager5.getCachePeerListener()).getBoundCachePeers();
        assertEquals(55, cachePeers5.size());
        String[] boundCachePeers5 = ((RMICacheManagerPeerListener) manager5.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(55, boundCachePeers5.length);
        assertEquals(cachePeers5.size(), boundCachePeers5.length);
    }


    /**
     * Are all of the replicated caches bound to the listener and working?
     */
    public void testBoundListenerPeers() throws RemoteException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        String[] boundCachePeers = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).listBoundRMICachePeers();
        validateBoundCachePeer(boundCachePeers);
    }

    /**
     * Are all of the CachePeers for replicated caches bound to the listener and working?
     */
    public void testBoundListenerPeersAfterDefaultCacheAdd() throws RemoteException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        String[] boundCachePeers = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(55, boundCachePeers.length);
        validateBoundCachePeer(boundCachePeers);

        //Add from default which is has a CacheReplicator configured.
        manager1.addCache("fromDefaultCache");
        boundCachePeers = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(56, boundCachePeers.length);
        validateBoundCachePeer(boundCachePeers);
    }

    /**
     * Are all of the CachePeers for replicated caches bound to the listener and working?
     */
    public void testBoundListenerPeersAfterProgrammaticCacheAdd() throws RemoteException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        String[] boundCachePeers = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(55, boundCachePeers.length);
        validateBoundCachePeer(boundCachePeers);

        //Add from default which is has a CacheReplicator configured.


        RMICacheReplicatorFactory factory = new RMICacheReplicatorFactory();
        CacheEventListener replicatingListener = factory.createCacheEventListener(null);
        Cache cache = new Cache("programmaticallyAdded",
                10, null, true, System.getProperty("java.io.tmpdir"), false, 10, 10, false, 60, null);
        cache.getCacheEventNotificationService().registerListener(replicatingListener);

        manager1.addCache(cache);
        boundCachePeers = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(56, boundCachePeers.length);
        validateBoundCachePeer(boundCachePeers);
    }

    /**
     * Are all of the CachePeers for replicated caches bound to the listener and working?
     */
    public void testBoundListenerPeersAfterCacheRemove() throws RemoteException {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        String[] boundCachePeers = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(55, boundCachePeers.length);
        validateBoundCachePeer(boundCachePeers);

        //Remove a replicated cache
        manager1.removeCache("sampleCache1");
        boundCachePeers = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).listBoundRMICachePeers();
        assertEquals(54, boundCachePeers.length);
        validateBoundCachePeer(boundCachePeers);
    }


    private void validateBoundCachePeer(String[] boundCachePeers) {
        for (int i = 0; i < boundCachePeers.length; i++) {
            String boundCacheName = boundCachePeers[i];
            Remote remote = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).lookupPeer(boundCacheName);
            assertNotNull(remote);
        }
    }


    /**
     * Does the RMI listener stop?
     */
    public void testListenerShutsdown() {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        CacheManagerPeerListener cachePeerListener = manager1.getCachePeerListener();
        List cachePeers1 = cachePeerListener.getBoundCachePeers();
        assertEquals(55, cachePeers1.size());
        assertEquals(Status.STATUS_ALIVE, cachePeerListener.getStatus());

        manager1.shutdown();
        assertEquals(Status.STATUS_SHUTDOWN, cachePeerListener.getStatus());

    }

    /**
     * Does the RMI listener stop?
     * <p/>
     * This test does not actually do test the shutdown hook automatically. But you should be able to
     * see "VM shutting down with the RMICacheManagerPeerListener for localhost still active. Calling dispose..."
     * in the log with FINE level when this test is run individually or as the last test in the run. i.e. on VM shutdown.
     */
    public void testListenerShutsdownFromShutdownHook() {

        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        CacheManager manager = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed6.xml");

        CacheManagerPeerListener cachePeerListener = manager.getCachePeerListener();
        List cachePeers1 = cachePeerListener.getBoundCachePeers();
        assertEquals(55, cachePeers1.size());
        assertEquals(Status.STATUS_ALIVE, cachePeerListener.getStatus());

    }
}
