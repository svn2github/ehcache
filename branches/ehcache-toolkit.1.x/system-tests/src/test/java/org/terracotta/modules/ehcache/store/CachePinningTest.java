/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class CachePinningTest extends AbstractCacheTestBase {

  private static final int ELEMENT_COUNT = 1000;

  public CachePinningTest(TestConfig testConfig) {
    super("cache-pinning-test.xml", testConfig, App.class);
  }

  public static class App extends ClientBase {
    public App(String[] args) {
      super("pinned", args);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {

      for (int i = 0; i < ELEMENT_COUNT; i++) {
        cache.put(new Element(i, i));
      }

      Assert.assertEquals(ELEMENT_COUNT, cache.getSize());

      for (int i = 0; i < ELEMENT_COUNT; i++) {
        Assert.assertNotNull(cache.get(i));
      }

      Assert.assertEquals(ELEMENT_COUNT, cache.getStatistics().getInMemoryHits());
      Assert.assertEquals(0, cache.getStatistics().getInMemoryMisses());
      Assert.assertEquals(0, cache.getStatistics().getOnDiskHits());
      Assert.assertEquals(0, cache.getStatistics().getOnDiskMisses());
      Assert.assertEquals(0, cache.getStatistics().getEvictionCount());
    }

  }
}
