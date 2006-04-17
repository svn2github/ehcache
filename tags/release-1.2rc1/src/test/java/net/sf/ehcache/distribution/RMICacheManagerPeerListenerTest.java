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

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.util.List;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.event.CountingCacheEventListener;

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
    protected Cache cache1;

    /**
     * CacheManager 2 of 2s cache being replicated
     */
    protected Cache cache2;

    /**
     * {@inheritDoc}
     * Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
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
        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
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

        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
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

        if (DistributionUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        String[] boundCachePeers1 = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).listBoundRMICachePeers();
        for (int i = 0; i < boundCachePeers1.length; i++) {
            String boundCacheName = boundCachePeers1[i];
            Remote remote = ((RMICacheManagerPeerListener) manager1.getCachePeerListener()).lookupPeer(boundCacheName);
            assertNotNull(remote);
        }

    }


}
