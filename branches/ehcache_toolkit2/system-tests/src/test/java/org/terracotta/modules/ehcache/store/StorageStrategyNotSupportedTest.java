/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class StorageStrategyNotSupportedTest extends AbstractCacheTestBase {

  public StorageStrategyNotSupportedTest(TestConfig testConfig) {
    super("ehcache-not-supported.xml", testConfig, App.class);
  }

  public static class App extends ClientBase {

    public App(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {

      CacheManager cm = getCacheManager();

      try {
        Assert.assertEquals(1, cm.getCacheNames().length);
        Assert.assertTrue(cm.getCacheNames()[0].equals("test"));
      } catch (CacheException e) {
        fail("Using storageStrategy=dcv2 should work even without ee");
      }

      // test programmatic way
      cm.shutdown();
      setupCacheManager();
      cm = getCacheManager();
      CacheConfiguration cacheConfiguration = new CacheConfiguration("testCache", 100);
      TerracottaConfiguration tc = new TerracottaConfiguration().storageStrategy(StorageStrategy.DCV2).clustered(true);
      cacheConfiguration.addTerracotta(tc);
      cache = new Cache(cacheConfiguration);

      cm.removeCache("test");

      try {
        cm.addCache(cache);
        Assert.assertEquals(1, cm.getCacheNames().length);
        Assert.assertTrue(cm.getCacheNames()[0].equals("testCache"));
      } catch (CacheException e) {
        e.printStackTrace();
        fail("Using storageStrategy=dcv2 should work even without ee");
      }
    }
  }
}
