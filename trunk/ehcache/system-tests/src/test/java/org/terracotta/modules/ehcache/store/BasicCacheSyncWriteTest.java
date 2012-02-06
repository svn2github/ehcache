/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class BasicCacheSyncWriteTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 3;

  public BasicCacheSyncWriteTest(TestConfig testConfig) {
    super("basic-cache-sync-write-test.xml", testConfig, App.class, App.class, App.class);
  }

  public static class App extends ClientBase {
    private final Barrier barrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      Assert.assertTrue(cache.getCacheConfiguration().getTerracottaConfiguration().isSynchronousWrites());

      Assert.assertEquals(0, cache.getSize());

      barrier.await();

      if (index == 0) {
        cache.put(new Element("key", "value"));
      }

      barrier.await();

      Assert.assertEquals(1, cache.getSize());
      Assert.assertEquals("value", cache.get("key").getObjectValue());

      barrier.await();

      if (index == 0) {
        boolean removed = cache.remove("key");
        Assert.assertTrue(removed);
      }

      barrier.await();

      Assert.assertEquals(0, cache.getSize());
    }

  }

}
