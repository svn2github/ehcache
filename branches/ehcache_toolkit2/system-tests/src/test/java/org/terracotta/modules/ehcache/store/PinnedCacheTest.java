/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.junit.Assert;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

public class PinnedCacheTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 2;

  public PinnedCacheTest(TestConfig testConfig) {
    super(testConfig, PinnedCacheTestApp.class, PinnedCacheTestApp.class);
    testConfig.addTcProperty("l2.servermap.eviction.clientObjectReferences.refresh.interval", "1");
  }

  public static class PinnedCacheTestApp extends ClientBase {
    private final Barrier barrier;

    public PinnedCacheTestApp(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test", NODE_COUNT);
    }

    public static void main(String[] args) {
      new PinnedCacheTestApp(args).run();
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      int nodeId = barrier.await();
      Thread.currentThread().setName("Node[" + nodeId + "]");
      Cache pinnedInCache = new Cache(new CacheConfiguration().name("pinnedInCache")
          .terracotta(new TerracottaConfiguration()).maxEntriesLocalHeap(100)
          .pinning(new PinningConfiguration().store("inCache")));

      // Do this to establish an order, node0 will add to cache first.
      if (nodeId == 0) {
        cacheManager.addCache(pinnedInCache);
      }
      barrier.await();
      if (nodeId == 1) {
        cacheManager.addCache(pinnedInCache);
      }
      barrier.await();

      if (nodeId == 0) {
        pinnedInCache.getCacheConfiguration().setMaxElementsOnDisk(100);
        for (int i = 0; i < 200; i++) {
          pinnedInCache.put(new Element("key" + i, "value"));
        }
        pinnedInCache.getSize(); // Force transactions to complete first.
      }
      barrier.await();
      Assert.assertEquals(pinnedInCache.getSize(), 200);

      if (nodeId == 1) {
        pinnedInCache.getCacheConfiguration().setMaxElementsOnDisk(100);
        for (int i = 200; i < 400; i++) {
          pinnedInCache.put(new Element("key" + i, "value"));
        }
        pinnedInCache.getSize(); // Force transactions to complete first.
      }
      barrier.await();
      Assert.assertEquals(pinnedInCache.getSize(), 400);

      Cache pinnedInCacheWithMaxOnDisk = new Cache(new CacheConfiguration().name("pinnedInCacheWithMaxOnDisk")
          .terracotta(new TerracottaConfiguration()).maxEntriesLocalHeap(100).maxElementsOnDisk(123)
          .pinning(new PinningConfiguration().store("inCache")));
      try {
        cacheManager.addCache(pinnedInCacheWithMaxOnDisk);
        Assert.fail("Expected cache configuration to fail.");
      } catch (InvalidConfigurationException e) {
        // expected
      }
    }
  }
}
