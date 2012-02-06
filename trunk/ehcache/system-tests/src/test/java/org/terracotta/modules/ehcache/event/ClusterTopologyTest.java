/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterScheme;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.util.Collection;

public class ClusterTopologyTest extends AbstractCacheTestBase {

  public ClusterTopologyTest(TestConfig testConfig) {
    super("clustered-events-test.xml", testConfig, App.class, App.class, App.class);
  }

  public static class App extends ClientBase {

    public App(String[] args) {
      super(args);
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      getBarrierForAllClients().await();

      CacheCluster cluster = cacheManager.getCluster(ClusterScheme.TERRACOTTA);
      assertTrue(cluster != null);
      assertTrue(cluster.getScheme().equals(ClusterScheme.TERRACOTTA));
      getBarrierForAllClients().await();
      Collection<ClusterNode> nodes = cluster.getNodes();
      assertEquals(getParticipantCount(), nodes.size());
      getBarrierForAllClients().await();
    }

  }
}
