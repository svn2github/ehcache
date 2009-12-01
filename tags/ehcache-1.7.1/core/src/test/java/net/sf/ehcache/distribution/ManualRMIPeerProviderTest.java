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

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class ManualRMIPeerProviderTest extends MulticastRMIPeerProviderTest {


    /**
     * {@inheritDoc}
     */
    @Before
    public void setUp() throws Exception {
        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed2.xml");
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed3.xml");

        /* manager3 has an empty manual configuration, which is topped up by adding manual entries.
         * The sampleCache1 from manager3 is added to the rmiUrls list for manager1 and manager2
         */
        CacheManagerPeerProvider peerProvider = manager3.getCacheManagerPeerProvider("RMI");
        peerProvider.registerPeer("//localhost:40001/sampleCache1");
        peerProvider.registerPeer("//localhost:40002/sampleCache1");

        //Allow cluster setup
        Thread.sleep(2000);
    }


    /**
     * test remote cache peers
     */
    @Test
    public void testProviderFromCacheManager() throws InterruptedException {
        Ehcache m1sampleCache1 = manager1.getCache("sampleCache1");
        Thread.sleep(2000);

        List peerUrls = manager1.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(m1sampleCache1);
        assertEquals(expectedPeers(), peerUrls.size());

        Ehcache m2sampleCache1 = manager2.getCache("sampleCache1");
        assertFalse(m1sampleCache1.getGuid().equals(m2sampleCache1.getGuid()));

        List peerUrls2 = manager2.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(m2sampleCache1);
        assertEquals(expectedPeers(), peerUrls2.size());

        Ehcache m3sampleCache1 = manager3.getCache("sampleCache1");
        assertFalse(m1sampleCache1.getGuid().equals(m3sampleCache1.getGuid()));

        List peerUrls3 = manager3.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(m3sampleCache1);
        assertEquals(expectedPeers(), peerUrls3.size());

        //Now remove a node, wait for the cluster to self-heal and then test
        manager1.shutdown();
        Thread.sleep(1000);
        peerUrls3 = manager3.getCacheManagerPeerProvider("RMI").listRemoteCachePeers(m3sampleCache1);
        //The manual provider removes the cache peer that was not reachable
        assertEquals(1, peerUrls3.size());

    }


}
