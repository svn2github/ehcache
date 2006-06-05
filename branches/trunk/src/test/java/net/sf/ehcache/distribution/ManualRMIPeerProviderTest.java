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

/**
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class ManualRMIPeerProviderTest extends MulticastRMIPeerProviderTest {


    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        if (JVMUtil.isSingleRMIRegistryPerVM()) {
            return;
        }

        manager1 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed1.xml");
        manager2 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed2.xml");
        manager3 = new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-manual-distributed3.xml");

        /* manager3 has an empty manual configuration, which is topped up by adding manual entries.
         * The sampleCache1 from manager3 is added to the rmiUrls list for manager1 and manager2
         */
        CacheManagerPeerProvider peerProvider = manager3.getCacheManagerPeerProvider();
        peerProvider.registerPeer("//localhost:40001/sampleCache1");
        peerProvider.registerPeer("//localhost:40002/sampleCache1");

        //Allow cluster setup
        Thread.sleep(100);
    }



}
