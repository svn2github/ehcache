/**
 *  Copyright 2003-2007 Terracotta, Inc.
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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsSame;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static net.sf.ehcache.distribution.AbstractRMITest.createRMICacheManagerConfiguration;

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
public class MulticastRMIPeerProviderTest extends AbstractRMITest {

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
    @Before
    public void setUp() throws Exception {
        List<Configuration> configurations = new ArrayList<Configuration>();
        configurations.add(createRMICacheManagerConfiguration()
                .defaultCache(createAsynchronousCache())
                .cache(createAsynchronousCache().name("asynchronousCache"))
                .name("MulticastRMIPeerProviderTest-1"));
        configurations.add(createRMICacheManagerConfiguration()
                .defaultCache(createAsynchronousCache())
                .cache(createAsynchronousCache().name("asynchronousCache"))
                .name("MulticastRMIPeerProviderTest-2"));
        configurations.add(createRMICacheManagerConfiguration()
                .defaultCache(createAsynchronousCache())
                .cache(createAsynchronousCache().name("asynchronousCache"))
                .name("MulticastRMIPeerProviderTest-3"));

        List<CacheManager> managers = startupManagers(configurations);
        manager1 = managers.get(0);
        manager2 = managers.get(1);
        manager3 = managers.get(2);

        //wait for cluster to establish
        waitForClusterMembership(10, TimeUnit.SECONDS, manager1, manager2, manager3);
    }

    /**
     * {@inheritDoc}
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

    /**
     * Make sure no exceptions get logged. Manual inspection.
     */
    @Test
    public void testSolePeer() throws Exception {
        tearDown();

        manager1 = new CacheManager(createRMICacheManagerConfiguration()
                .cache(new CacheConfiguration().maxEntriesLocalHeap(0).name("non-replicating"))
                .name("MulticastRMIPeerProviderTest-4"));
    }

    /**
     * test remote cache peers
     */
    @Test
    public void testProviderFromCacheManager() throws InterruptedException {
        MulticastKeepaliveHeartbeatSender.setHeartBeatStaleTime(3000);

        Ehcache m1sampleCache1 = manager1.getCache("asynchronousCache");
        Ehcache m2sampleCache1 = manager2.getCache("asynchronousCache");
        Ehcache m3sampleCache1 = manager3.getCache("asynchronousCache");

        assertThat(m1sampleCache1.getGuid(), not(is(m2sampleCache1.getGuid())));
        assertThat(m1sampleCache1.getGuid(), not(is(m3sampleCache1.getGuid())));

        //Now remove a node, wait for the cluster to self-heal and then test
        manager1.shutdown();

        waitForClusterMembership(10, TimeUnit.SECONDS, manager2, manager3);
    }

    /**
     * The default caches for ehcache-dsitributed1-6.xml are set to replicate.
     * We create a new cache from the default and expect it to be replicated.
     */
    @Test
    public void testProviderCreatedFromDefaultCache() throws InterruptedException {
        //manual does not nor should it work this way
        assumeThat(getClass(), IsSame.<Class<?>>sameInstance(MulticastRMIPeerProviderTest.class));

        manager1.addCache("fromDefaultCache");
        manager2.addCache("fromDefaultCache");
        manager3.addCache("fromDefaultCache");

        waitForClusterMembership(10, TimeUnit.SECONDS, manager1, manager2, manager3);
    }

    @Test
    public void testDeleteReplicatedCache() throws InterruptedException {
        //manual does not nor should it work this way
        assumeThat(getClass(), IsSame.<Class<?>>sameInstance(MulticastRMIPeerProviderTest.class));

        MulticastKeepaliveHeartbeatSender.setHeartBeatStaleTime(3000);

        manager1.addCache("fromDefaultCache");
        manager2.addCache("fromDefaultCache");
        manager3.addCache("fromDefaultCache");

        waitForClusterMembership(10, TimeUnit.SECONDS, manager1, manager2, manager3);

        manager1.removeCache("fromDefaultCache");

        waitForClusterMembership(10, TimeUnit.SECONDS, manager2, manager3);
    }


    /**
     * Determines that the multicast TTL default is 1, which means that packets are restricted to the same subnet.
     * peerDiscovery=automatic, multicastGroupAddress=230.0.0.1, multicastGroupPort=4446, multicastPacketTimeToLive=255
     */
    @Test
    public void testMulticastTTL() throws IOException {
        InetAddress groupAddress = InetAddress.getByName("230.0.0.1");
        MulticastSocket socket = new MulticastSocket();
        socket.joinGroup(groupAddress);
        try {
            assertThat(socket.getTimeToLive(), is(1));
        } finally {
            socket.leaveGroup(groupAddress);
        }
    }

}
