/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.cluster;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;

import org.terracotta.collections.ConcurrentDistributedSet;
import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class CacheClusterTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier    barrier = new CyclicBarrier(NODE_COUNT);
    private final Set<ClusterNode> nodes   = new ConcurrentDistributedSet<ClusterNode>();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      barrier.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-cache-test.xml"));
      cacheManager.getCache("test");

      CacheCluster clusterInfo = cacheManager.getCluster(ClusterScheme.TERRACOTTA);

      ClusterNode node = clusterInfo.getCurrentNode();
      Assert.assertNotNull(node);
      nodes.add(node);

      barrier.await();

      for (int i = 0; i < 10; i++) {
        int nodeCount = nodes.size();
        if (NODE_COUNT == nodeCount) { return; }
        System.err.println("Node count is for iteration " + i + " is " + nodeCount);
        Thread.sleep(10 * 1000);
      }

      Assert.assertEquals(NODE_COUNT, nodes.size());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier").addRoot("nodes", "nodes");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

}