package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterScheme;

import org.terracotta.toolkit.Toolkit;

import junit.framework.Assert;

public class ClusterEventsWatcherClient extends ClientBase {

  public ClusterEventsWatcherClient(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ClusterEventsWatcherClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    getBarrierForAllClients().await();

    CacheCluster cluster = cache.getCacheManager().getCluster(ClusterScheme.TERRACOTTA);
    Assert.assertTrue(cluster != null);
    Assert.assertTrue(cluster.getScheme().equals(ClusterScheme.TERRACOTTA));

    try {
      final long end = System.currentTimeMillis() + 20000L;
      while (System.currentTimeMillis() < end) {
        int count = cluster.getNodes().size();
        if (count == 4) return;
        if (count > 4) throw new AssertionError(count + " nodes observed!");
        System.err.println("nodes.size() = " + count);
        Thread.sleep(1000L);
      }

      throw new AssertionError("expected node count never reached");
    } finally {
      getBarrierForAllClients().await();
    }
  }
}
