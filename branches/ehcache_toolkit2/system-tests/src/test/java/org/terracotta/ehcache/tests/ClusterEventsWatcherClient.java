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
        /*
         * Expect 3 clients, the original custom spawning client and then one for each ClusterEventsWatchClient: the
         * toolkit barrier and the Ehcache cache manager will share the L1 client.
         */
        int count = cluster.getNodes().size();
        if (count == 2) return;
        if (count > 2) throw new AssertionError(count + " nodes observed!");
        System.err.println("nodes.size() = " + count);
        Thread.sleep(1000L);
      }

      throw new AssertionError("expected node count never reached");
    } finally {
      getBarrierForAllClients().await();
    }
  }
}
