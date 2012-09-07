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

package net.sf.ehcache.distribution;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheManager;

import org.junit.Before;

/**
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class ManualRMIPeerProviderTest extends MulticastRMIPeerProviderTest {

    /**
     * {@inheritDoc}
     */
    @Override
    @Before
    public void setUp() throws Exception {
        manager1 = new CacheManager(getConfiguration(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed1.xml").name("cm1"));
        manager2 = new CacheManager(getConfiguration(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed2.xml").name("cm2"));
        manager3 = new CacheManager(getConfiguration(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed3.xml").name("cm3"));

        /* manager3 has an empty manual configuration, which is topped up by adding manual entries.
         * The sampleCache1 from manager3 is added to the rmiUrls list for manager1 and manager2
         */
        CacheManagerPeerProvider peerProvider = manager3.getCacheManagerPeerProvider("RMI");
        peerProvider.registerPeer("//localhost:40001/sampleCache1");
        peerProvider.registerPeer("//localhost:40002/sampleCache1");

        //Allow cluster setup
        waitForClusterMembership(10, TimeUnit.SECONDS, Collections.singleton("sampleCache1"), manager1, manager2, manager3);
    }
}
