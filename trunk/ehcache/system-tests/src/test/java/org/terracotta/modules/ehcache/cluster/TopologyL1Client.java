/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.cluster;

import net.sf.ehcache.Cache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import junit.framework.Assert;

public class TopologyL1Client extends ClientBase {
  private static final String  MANAGER_UTIL_CLASS_NAME         = "com.tc.object.bytecode.ManagerUtil";
  private static final String  MANAGER_UTIL_GETCLIENTID_METHOD = "getClientID";

  private final ToolkitBarrier barrier;

  public TopologyL1Client(String[] args) {
    super(args);
    this.barrier = getClusteringToolkit().getBarrier("testBarrier", getParticipantCount());
  }

  @Override
  protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
    CacheConfiguration cacheConfig = new CacheConfiguration("cache", 1000);
    cacheConfig.addTerracotta(new TerracottaConfiguration());
    cache = new Cache(cacheConfig);
    cacheManager.addCache(cache);

    TopologyListenerImpl topoListener = new TopologyListenerImpl();
    CacheCluster cacheCluster = cacheManager.getCluster(ClusterScheme.TERRACOTTA);

    Assert.assertTrue(cacheCluster.isClusterOnline());
    Assert.assertNotNull(cacheCluster.getCurrentNode());
    String clusterId = cacheCluster.getCurrentNode().getId();
    int id = Integer.parseInt(getClientID());
    Assert.assertEquals(clusterId, "ClientID[" + id + "]");
    topoListener.setClusterId(clusterId);

    if (!cacheCluster.addTopologyListener(topoListener)) {
      System.out.println("XXXXXX failed to add the topology listener");
    } else {
      System.out.println("XXXXXX successfully added the topology listener");
    }

    int index = barrier.await();
    if (index == 0) {
      Thread serverRestartThread = new Thread(new Runnable() {

        @Override
        public void run() {
          try {
            Thread.sleep(180 * 1000);
            System.out.println("Shutting down server.");
            getTestControlMbean().crashActiveServer(0);
            Thread.sleep(5000);
            System.out.println("Server shutdown complete.");

            getTestControlMbean().restartCrashedServer(0, 0);
            System.out.println("Server restart complete.");
          } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e);
          }
        }
      }, "serverRestartThread");
      serverRestartThread.start();
    }

    System.out.println(clusterId + " client ID: " + id);

    barrier.await();

    /*
     * ClientID[1] client ID: 1 will see Joined 3 (1,2,3) ClientID[2] client ID: 2 will see Joined 2 (2,3) ClientID[3]
     * client ID: 3 will see Joined 1 (3)
     */

    int expectedNodesJoined = getParticipantCount() - id;
    System.out.println(String.format("%s: Nodes Joined - Expected: %d, Actual: %d", clusterId, expectedNodesJoined,
                                     topoListener.getNodesJoined()));
    Assert.assertEquals(clusterId + " Nodes Joined: ", expectedNodesJoined, topoListener.getNodesJoined());

    barrier.await();

    System.out.println(clusterId + " Waiting for 200 secs so that L2 can be restarted.");
    Thread.sleep(200 * 1000);

    barrier.await();
    Thread.sleep(id * 10000);

    /*
     * ClientID[1] client ID: 1 waits 10 secs, Left 0 ClientID[2] client ID: 2 waits 20 secs, Left 1 ClientID[3] client
     * ID: 3 waits 30 secs, Left 2
     */
    int expectedNodesLeft = id;
    System.out.println(String.format("%s Nodes Left - Expected: %d ,  Actual: %d", clusterId, expectedNodesLeft,
                                     topoListener.getNodesLeft()));

    // DEV-4542
    Assert.assertEquals(clusterId + " Nodes Left", expectedNodesLeft, topoListener.getNodesLeft());

    Assert.assertEquals("Cluster Offline times", 1, topoListener.getClusterOffline());
    Assert.assertEquals("Cluster Online times", 2, topoListener.getClusterOnline());

  }

  // work around for ManagerUtil.getClientID
  public String getClientID() {
    if (isStandaloneCfg()) return null;
    try {
      ClassLoader cl = getClusteringToolkit().getMap("testMap", null, null).getClass().getClassLoader();
      Class managerUtil = cl.loadClass(MANAGER_UTIL_CLASS_NAME);
      return (String) managerUtil.getMethod(MANAGER_UTIL_GETCLIENTID_METHOD).invoke(null);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

}
