/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.cluster;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.apache.log4j.Logger;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.util.Assert;

import java.util.concurrent.CyclicBarrier;

public class TopologyL1Client {

  private static final Logger LOG     = Logger.getLogger(TopologyL1Client.class.getName());
  private final CyclicBarrier barrier = new CyclicBarrier(ClusterTopologyListenerTest.CLIENT_COUNT);

  public static void main(String[] args) {
    TopologyL1Client client = new TopologyL1Client();
    try {
      client.doTest();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void doTest() throws Exception {
    Configuration config = new Configuration();
    config.setDefaultCacheConfiguration(new CacheConfiguration("default", 1000));
    config.setName("cluster-topology-listener-test");
    
    CacheManager cacheManager = new CacheManager(config);

    CacheConfiguration cacheConfig = new CacheConfiguration("cache", 1000);
    cacheConfig.addTerracotta(new TerracottaConfiguration());
    Cache cache = new Cache(cacheConfig);
    cacheManager.addCache(cache);

    TopologyListenerImpl topoListener = new TopologyListenerImpl();
    CacheCluster cacheCluster = cacheManager.getCluster(ClusterScheme.TERRACOTTA);
    
    Assert.assertTrue(cacheCluster.isClusterOnline());
    Assert.assertNotNull(cacheCluster.getCurrentNode());
    String clusterId = cacheCluster.getCurrentNode().getId();
    int id = Integer.parseInt(ManagerUtil.getClientID());
    Assert.assertEquals(clusterId, "ClientID[" + id + "]");
    topoListener.setClusterId(clusterId);

    if (!cacheCluster.addTopologyListener(topoListener)) {
      System.out.println("XXXXXX failed to add the topology listener");
    } else {
      System.out.println("XXXXXX successfully added the topology listener");
    }

    barrier.await();
    Thread.sleep(10000);

    LOG.info(clusterId + " client ID: " + id);

    barrier.await();

    /*
     * ClientID[1] client ID: 1 will see Joined 3 (1,2,3) 
     * ClientID[2] client ID: 2 will see Joined 2 (2,3)
     * ClientID[3] client ID: 3 will see Joined 1 (3)
     */

    int expectedNodesJoined = ClusterTopologyListenerTest.CLIENT_COUNT - id + 1;
	LOG.info(String.format("%s: Nodes Joined - Expected: %d, Actual: %d",
			clusterId, expectedNodesJoined, topoListener.getNodesJoined()));
	Assert.assertEquals(clusterId + " Nodes Joined: " , expectedNodesJoined, topoListener.getNodesJoined());

    barrier.await();
    
    LOG.info(clusterId + " Waiting for 180 secs so that L2 can be restarted.");
    Thread.sleep(3 * 60000);

    barrier.await();
    Thread.sleep(id * 10000);

    /*
     * ClientID[1] client ID: 1 waits 10 secs, Left 0 
     * ClientID[2] client ID: 2 waits 20 secs, Left 1
     * ClientID[3] client ID: 3 waits 30 secs, Left 2
     */
    int expectedNodesLeft = id - 1;
	LOG.info(String.format("%s Nodes Left - Expected: %d ,  Actual: %d",
			clusterId, expectedNodesLeft, topoListener.getNodesLeft()));

    // DEV-4542
    Assert.assertEquals(clusterId + " Nodes Left", expectedNodesLeft, topoListener.getNodesLeft());

    Assert.assertEquals("Cluster Offline times", 1, topoListener.getClusterOffline());
    Assert.assertEquals("Cluster Online times", 2, topoListener.getClusterOnline());

  }

}
