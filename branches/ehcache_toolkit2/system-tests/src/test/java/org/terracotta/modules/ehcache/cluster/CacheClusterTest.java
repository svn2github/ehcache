/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.cluster;

import net.sf.ehcache.Cache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.collections.ClusteredMap;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.test.util.TestBaseUtil;

import com.tc.test.config.model.TestConfig;

import java.io.IOException;

import junit.framework.Assert;

public class CacheClusterTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 3;

  public CacheClusterTest(TestConfig testConfig) {
    super("basic-cache-test.xml", testConfig, App.class, App.class, App.class);
  }

  @Override
  protected String createClassPath(Class client) throws IOException {
    String classPath = super.createClassPath(client);
    classPath = addToClasspath(classPath, TestBaseUtil.jarFor(ClusterNode.class));
    return classPath;
  }

  public static class App extends ClientBase {
    private final Barrier                barrier;
    private ClusteredMap<String, String> nodes;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", NODE_COUNT);
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      barrier.await();
      this.nodes = getClusteringToolkit().getMap("testMap");

      cacheManager.getCache("test");

      CacheCluster clusterInfo = cacheManager.getCluster(ClusterScheme.TERRACOTTA);

      ClusterNode node = clusterInfo.getCurrentNode();
      Assert.assertNotNull(node);
      nodes.put(node.getId(), node.getId());

      barrier.await();

      for (int i = 0; i < 10; i++) {
        int nodeCount = nodes.size();
        if (NODE_COUNT == nodeCount) { return; }
        System.err.println("Node count is for iteration " + i + " is " + nodeCount);
        Thread.sleep(10 * 1000);
      }

      Assert.assertEquals(NODE_COUNT, nodes.size());
    }

  }

}
