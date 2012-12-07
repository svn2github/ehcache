/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class CacheTierPinningTest extends AbstractCacheTestBase {

  private static final int ELEMENT_COUNT = 1000;

  public CacheTierPinningTest(TestConfig testConfig) {
    super("cache-pinning-test.xml", testConfig, App.class);
  }

  public static class App extends ClientBase {
    public App(String[] args) {
      super("pinned", args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {

      System.out.println("Testing with Strong tier pinned cache");
      runBasicPinningTest(cacheManager.getCache("pinned"));
      System.out.println("Testing with Eventual tier pinned cache");
      runBasicPinningTest(cacheManager.getCache("pinnedEventual"));
    }

    private void runBasicPinningTest(Cache cache) {
      for (int i = 0; i < ELEMENT_COUNT; i++) {
        cache.put(new Element(i, i));
      }

      Assert.assertEquals(ELEMENT_COUNT, cache.getSize());

      for (int i = 0; i < ELEMENT_COUNT; i++) {
        Assert.assertNotNull(cache.get(i));
      }

      Assert.assertEquals(ELEMENT_COUNT, cache.getStatistics().getCore().getInMemoryHits());
      Assert.assertEquals(0, cache.getStatistics().getCore().getInMemoryMisses());
      Assert.assertEquals(0, cache.getStatistics().getCore().getOnDiskHits());
      Assert.assertEquals(0, cache.getStatistics().getCore().getOnDiskMisses());
      Assert.assertEquals(0, cache.getStatistics().getCore().getEvictionCount());
    }

  }
}
