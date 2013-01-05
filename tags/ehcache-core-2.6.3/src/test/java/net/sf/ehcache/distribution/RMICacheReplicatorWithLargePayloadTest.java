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

import static net.sf.ehcache.util.RetryAssert.assertBy;
import static org.hamcrest.collection.IsCollectionWithSize.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.hamcrest.number.OrderingComparison.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.util.RetryAssert;

import org.hamcrest.collection.IsEmptyCollection;
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
public class RMICacheReplicatorWithLargePayloadTest extends AbstractRMITest {

    private static final Logger LOG = Logger.getLogger(RMICacheReplicatorWithLargePayloadTest.class.getName());

    private static int MB = 1024 * 1024;

    /**
     * {@inheritDoc} Sets up two caches: cache1 is local. cache2 is to be receive updates
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        failFastInsufficientMemory();
        assertThat(getActiveReplicationThreads(), IsEmptyCollection.<Thread>empty());
    }

    private void failFastInsufficientMemory() {
        // fail fast if running with insufficient heap
        long totalMemory = Runtime.getRuntime().totalMemory();
        if (totalMemory < 200 * MB) {
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

    @After
    public void noReplicationThreads() throws Exception {
        RetryAssert.assertBy(30, TimeUnit.SECONDS, new Callable<Set<Thread>>() {
            @Override
            public Set<Thread> call() throws Exception {
                return getActiveReplicationThreads();
            }
        }, IsEmptyCollection.<Thread>empty());
    }
    
    private static List<CacheManager> createCluster(int size, String ... caches){
        LOG.info("Creating Cluster");
        Collection<String> required = Arrays.asList(caches);
        List<Configuration> configurations = new ArrayList<Configuration>(size);
        for (int i = 1; i <= size; i++) {
            Configuration config = getConfiguration(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-" + i + ".xml").name("cm" + i);
            if (!required.isEmpty()) {
                for (Iterator<Map.Entry<String, CacheConfiguration>> it = config.getCacheConfigurations().entrySet().iterator(); it.hasNext(); ) {
                    if (!required.contains(it.next().getKey())) {
                        it.remove();
                    }
                }
            }
            configurations.add(config);
        }
        LOG.info("Created Configurations");

        List<CacheManager> members = startupManagers(configurations);
        try {
          LOG.info("Created Managers");
          if (required.isEmpty()) {
              waitForClusterMembership(10, TimeUnit.SECONDS, members);
              LOG.info("Cluster Membership Complete");
              emptyCaches(10, TimeUnit.SECONDS, members);
              LOG.info("Caches Emptied");
          } else {
              waitForClusterMembership(10, TimeUnit.SECONDS, required, members);
              emptyCaches(10, TimeUnit.SECONDS, required, members);
          }
          return members;
        } catch (RuntimeException e) {
          destroyCluster(members);
          throw e;
        } catch (Error e) {
          destroyCluster(members);
          throw e;
        }
    }

    private static void destroyCluster(List<CacheManager> members) {
        for (CacheManager manager : members) {
            if (manager != null) {
                manager.shutdown();
            }
        }
    }

    @Test
    public void testAssertBigPayload() {
        List<CacheManager> cluster = createCluster(3);
        try {
            for (CacheManager manager : cluster) {
                List<CachePeer> localPeers = cluster.get(0).getCachePeerListener("RMI").getBoundCachePeers();
                List<byte[]> payloadList = PayloadUtil.createCompressedPayloadList(localPeers, 150);
                assertThat(manager.getName(), payloadList, hasSize(greaterThan(1)));
            }

            cluster.add(new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-4.xml"));
            
            List<CachePeer> localPeers = cluster.get(3).getCachePeerListener("RMI").getBoundCachePeers();
            List<byte[]> payloadList = PayloadUtil.createCompressedPayloadList(localPeers, 150);
            assertThat(payloadList, hasSize(greaterThan(1)));
        } finally {
          destroyCluster(cluster);
        }
    }

    /**
     * Does a new cache manager in the cluster get detected?
     */
    @Test
    public void testRemoteCachePeersDetectsNewCacheManager() throws InterruptedException {
        List<CacheManager> cluster = createCluster(3);
        try {
            //Add new CacheManager to cluster
            cluster.add(new CacheManager(AbstractCacheTest.TEST_CONFIG_DIR + "distribution/ehcache-distributed-big-payload-4.xml"));

            //Allow detection to occur
            waitForClusterMembership(10020, TimeUnit.MILLISECONDS, cluster);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    @Test
    public void testRemoteCachePeersDetectsDownCacheManager() throws InterruptedException {
        MulticastKeepaliveHeartbeatSender.setHeartBeatStaleTime(3000);
        List<CacheManager> cluster = createCluster(3);
        try {
            //Drop a CacheManager from the cluster
            cluster.remove(2).shutdown();
            assertThat(cluster, hasSize(2));

            //Allow change detection to occur. Heartbeat 1 second and is not stale until 5000
            waitForClusterMembership(11020, TimeUnit.MILLISECONDS, cluster);
        } finally {
            destroyCluster(cluster);
        }
    }

    /**
     * Does a down cache manager in the cluster get removed?
     */
    @Test
    public void testRemoteCachePeersDetectsDownCacheManagerSlow() throws InterruptedException {
        List<CacheManager> cluster = createCluster(3);
        try {
            CacheManager manager = cluster.get(0);

            Thread.sleep(2000);

            //Drop a CacheManager from the cluster
            cluster.remove(2).shutdown();
            
            //Insufficient time for it to timeout
            CacheManagerPeerProvider provider = manager.getCacheManagerPeerProvider("RMI");
            for (String cacheName : manager.getCacheNames()) {
                List remotePeersOfCache1 = provider.listRemoteCachePeers(manager.getCache(cacheName));
                assertThat((List<?>) remotePeersOfCache1, hasSize(2));
            }
        } finally {
            destroyCluster(cluster);
        }        
    }

    /**
     * Tests put and remove initiated from cache1 in a cluster
     * <p/>
     * This test goes into an infinite loop if the chain of notifications is not somehow broken.
     */
    @Test
    public void testPutProgagatesFromAndToEveryCacheManagerAndCache() throws CacheException, InterruptedException {
        final List<CacheManager> cluster = createCluster(3);
        try {
            final CacheManager manager0 = cluster.get(0);
            //Put
            final String[] cacheNames = manager0.getCacheNames();
            Arrays.sort(cacheNames);
            for (int i = 0; i < cacheNames.length; i++) {
                String name = cacheNames[i];
                manager0.getCache(name).put(new Element(Integer.toString(i), Integer.valueOf(i)));
                //Add some non serializable elements that should not get propagated
                manager0.getCache(name).put(new Element("nonSerializable" + i, new Object()));
            }

            assertBy(10, TimeUnit.SECONDS, new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    for (int i = 0; i < cacheNames.length; i++) {
                        String name = cacheNames[i];
                        for (CacheManager manager : cluster.subList(1, cluster.size())) {
                            assertThat("Cache : " + name, manager.getCache(name).get(Integer.toString(i)), notNullValue());
                            assertThat(manager.getCache(name).get("nonSerializable" + i), nullValue());
                        }
                    }
                    return Boolean.TRUE;
                }
            }, is(Boolean.TRUE));
        } finally {
            destroyCluster(cluster);
        }
        
    }
}
