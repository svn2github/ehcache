/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class NoCacheWithMaxBytesLocalDiskTest extends AbstractCacheTestBase {

  public NoCacheWithMaxBytesLocalDiskTest(TestConfig testConfig) {
    super(testConfig, App.class);
  }

  public static class App extends ClientBase {

    public App(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      try {
        crerateCache("dcv2EventualWithStats", cacheManager, "DCV2", Consistency.EVENTUAL, "IDENTITY");
        Assert.fail("was able to create a clustered cache with \"maxBytesLocalDisk\" set");
      } catch (Exception e) {
        // expected exception
      }
    }

    private Cache crerateCache(String cacheName, CacheManager cm, String storageStrategy, Consistency consistency,
                               String valueMode) {
      System.out.println("creating " + cacheName);
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxBytesLocalDisk(1024 * 1024L);
      cacheConfiguration.setOverflowToDisk(false);
      cacheConfiguration.setEternal(false);
      cacheConfiguration.setMaxBytesLocalHeap(10 * 1024 * 1024L);
      cacheConfiguration.setDiskPersistent(false);
      cacheConfiguration.setOverflowToOffHeap(false);

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setStorageStrategy(storageStrategy);
      tcConfiguration.setConsistency(consistency);
      tcConfiguration.setValueMode(valueMode);
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cm.addCache(cache);
      return cache;
    }
  }

}
