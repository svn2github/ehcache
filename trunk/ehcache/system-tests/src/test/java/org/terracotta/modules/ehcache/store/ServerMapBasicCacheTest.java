/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class ServerMapBasicCacheTest extends AbstractCacheTestBase {

  public ServerMapBasicCacheTest(TestConfig testConfig) {
    super("server-map-cache-test.xml", testConfig, ServerMapBasicCacheApp.class, ServerMapBasicCacheApp.class);
  }

  public static class ServerMapBasicCacheApp extends ClientBase {
    private static final int NODE_COUNT = 2;
    private final Barrier    barrier;

    public ServerMapBasicCacheApp(String[] args) {
      super("defaultStorageStrategyCache", args);
      this.barrier = getClusteringToolkit().getBarrier("test", NODE_COUNT);
    }

    public static void main(String[] args) {
      new ServerMapBasicCacheApp(args).run();
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      final int index = this.barrier.await();

      Assert.assertEquals(StorageStrategy.DCV2, cache.getCacheConfiguration().getTerracottaConfiguration()
          .getStorageStrategy());

      System.out.println("Asserted different default/explicit storage strategys.");

      cache = cacheManager.getCache("test");
      // assert correct storageStrategy is used
      Assert.assertEquals(StorageStrategy.DCV2, cache.getCacheConfiguration().getTerracottaConfiguration()
          .getStorageStrategy());

      // XXX: assert that the cache is clustered via methods on cache
      // config (when methods exist)

      Assert.assertEquals(0, cache.getSize());

      this.barrier.await();

      if (index == 0) {
        cache.put(new Element("key", "value"));
      }

      this.barrier.await();

      Assert.assertEquals(1, cache.getSize());
      Assert.assertEquals("value", cache.get("key").getObjectValue());
      System.err.println("Index : " + index + " : Cache size : " + cache.getSize());

      this.barrier.await();

      if (index == 0) {
        final boolean removed = cache.remove("key");
        Assert.assertTrue(removed);
      }

      this.barrier.await();

      Assert.assertEquals(0, cache.getSize());
    }
  }
}
