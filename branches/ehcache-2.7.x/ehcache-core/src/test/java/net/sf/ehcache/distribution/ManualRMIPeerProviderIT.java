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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.FactoryConfiguration;

import org.junit.Before;

import static net.sf.ehcache.distribution.AbstractRMITest.createAsynchronousCache;

/**
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class ManualRMIPeerProviderIT extends MulticastRMIPeerProviderIT {

    /**
     * {@inheritDoc}
     */
    @Override
    @Before
    public void setUp() throws Exception {
        List<Configuration> configurations = new ArrayList<Configuration>();
        configurations.add(new Configuration()
                .cacheManagerPeerListenerFactory(new FactoryConfiguration()
                .className("net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory")
                .properties("hostName=localhost, port=5011, socketTimeoutMillis=2000"))
                .cacheManagerPeerProviderFactory(new FactoryConfiguration()
                .className("net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory")
                .properties("peerDiscovery=manual,rmiUrls=//localhost:5012/asynchronousCache|//localhost:5013/asynchronousCache"))
                .cache(createAsynchronousCache().name("asynchronousCache"))
                .name("ManualRMIPeerProviderTest-1"));

        configurations.add(new Configuration()
                .cacheManagerPeerListenerFactory(new FactoryConfiguration()
                .className("net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory")
                .properties("hostName=localhost, port=5012, socketTimeoutMillis=2000"))
                .cacheManagerPeerProviderFactory(new FactoryConfiguration()
                .className("net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory")
                .properties("peerDiscovery=manual,rmiUrls=//localhost:5011/asynchronousCache|//localhost:5013/asynchronousCache"))
                .cache(createAsynchronousCache().name("asynchronousCache"))
                .name("ManualRMIPeerProviderTest-2"));

        configurations.add(new Configuration()
                .cacheManagerPeerListenerFactory(new FactoryConfiguration()
                .className("net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory")
                .properties("hostName=localhost, port=5013, socketTimeoutMillis=2000"))
                .cacheManagerPeerProviderFactory(new FactoryConfiguration()
                .className("net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory")
                .properties("peerDiscovery=manual"))
                .cache(createAsynchronousCache().name("asynchronousCache"))
                .name("ManualRMIPeerProviderTest-3"));

        List<CacheManager> managers = startupManagers(configurations);
        manager1 = managers.get(0);
        manager2 = managers.get(1);
        manager3 = managers.get(2);

        /* manager3 has an empty manual configuration, which is topped up by adding manual entries.
         * The sampleCache1 from manager3 is added to the rmiUrls list for manager1 and manager2
         */
        CacheManagerPeerProvider peerProvider = manager3.getCacheManagerPeerProvider("RMI");
        peerProvider.registerPeer("//localhost:5011/asynchronousCache");
        peerProvider.registerPeer("//localhost:5012/asynchronousCache");

        //Allow cluster setup
        waitForClusterMembership(10, TimeUnit.SECONDS, manager1, manager2, manager3);
    }
}
