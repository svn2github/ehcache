/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class ProgrammaticConfigTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 2;

  public ProgrammaticConfigTest(TestConfig testConfig) {
    super(testConfig, App.class, App.class);
  }

  public static class App extends ClientBase {
    private final Barrier barrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", NODE_COUNT);
    }

    @Override
    protected CacheManager getCacheManager() {

      Configuration configuration = new Configuration().defaultCache(new CacheConfiguration("defaultCache", 100))
          .cache(new CacheConfiguration("example", 100).timeToIdleSeconds(5).timeToLiveSeconds(120)
                     .terracotta(new TerracottaConfiguration())); // defaults are all good

      TerracottaClientConfiguration tcClientConfiguration = new TerracottaClientConfiguration();
      tcClientConfiguration.setUrl(getTerracottaUrl());
      configuration.addTerracottaConfig(tcClientConfiguration);

      return new CacheManager(configuration);
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      Cache exampleCache = getCacheManager().getCache("example");
      assertNotNull(exampleCache);

      if (index == 0) {
        exampleCache.put(new Element("abc", "def"));
      }

      barrier.await();

      Element got = exampleCache.get("abc");
      assertNotNull(got);
      assertEquals("def", got.getValue());
      Assert.assertEquals(1, exampleCache.getSize());
    }

  }

}
