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

import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class SimpleVersionTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 2;

  public SimpleVersionTest(TestConfig testConfig) {
    super("simple-version-test.xml", testConfig, App.class, App.class);
  }

  public static class App extends ClientBase {

    private final Barrier barrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test", NODE_COUNT);
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      testVersion(index, cacheManager.getCache("serialized"));
    }

    private void testVersion(final int index, Cache cache) throws InterruptedException, BrokenBarrierException {
      if (index == 0) {
        Element e1 = new Element("key", "value");
        e1.setVersion(12345);

        cache.put(e1);
      }

      barrier.await();

      Assert.assertEquals(12345, cache.get("key").getVersion());
    }

  }
}
