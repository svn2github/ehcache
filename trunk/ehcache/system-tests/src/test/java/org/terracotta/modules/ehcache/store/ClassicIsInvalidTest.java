/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;
import com.tc.util.Assert;

public class ClassicIsInvalidTest extends AbstractCacheTestBase {

  public ClassicIsInvalidTest(TestConfig testConfig) {
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

      Cache c = crerateCache("eventualClassicIdentity", cacheManager, "CLASSIC", Consistency.EVENTUAL, "IDENTITY");
      Assert.assertEquals(c.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy(),
                          StorageStrategy.DCV2);

      c = crerateCache("eventualClassicSerialization", cacheManager, "CLASSIC", Consistency.EVENTUAL, "SERIALIZATION");
      Assert.assertEquals(c.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy(),
                          StorageStrategy.DCV2);

      c = crerateCache("strongClassicIdentity", cacheManager, "CLASSIC", Consistency.STRONG, "IDENTITY");
      Assert.assertEquals(c.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy(),
                          StorageStrategy.DCV2);

      c = crerateCache("strongClassicSerialization", cacheManager, "CLASSIC", Consistency.STRONG, "SERIALIZATION");
      Assert.assertEquals(c.getCacheConfiguration().getTerracottaConfiguration().getStorageStrategy(),
                          StorageStrategy.DCV2);

      crerateCache("eventualDCV2Identity", cacheManager, "DCV2", Consistency.EVENTUAL, "IDENTITY");
      crerateCache("eventualDCV2Serialization", cacheManager, "DCV2", Consistency.EVENTUAL, "SERIALIZATION");
      crerateCache("strongDCV2Identity", cacheManager, "DCV2", Consistency.STRONG, "IDENTITY");
      crerateCache("strongDCV2Serialization", cacheManager, "DCV2", Consistency.STRONG, "SERIALIZATION");

    }

    private Cache crerateCache(String cacheName, CacheManager cm, String storageStrategy, Consistency consistency,
                               String valueMode) {
      System.out.println("creating " + cacheName);
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setOverflowToDisk(false);
      cacheConfiguration.setEternal(false);
      cacheConfiguration.setMaxBytesLocalHeap(10 * 1024 * 1024L);
      cacheConfiguration.setDiskPersistent(false);

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
