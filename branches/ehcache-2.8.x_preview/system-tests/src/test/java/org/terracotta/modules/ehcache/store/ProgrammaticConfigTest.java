/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.event.CacheEventListenerAdapter;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class ProgrammaticConfigTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 2;

  public ProgrammaticConfigTest(TestConfig testConfig) {
    super(testConfig, App.class, App.class);
  }

  public static class App extends ClientBase {
    private final ToolkitBarrier barrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", NODE_COUNT);
    }

    @Override
    protected CacheManager getCacheManager() {

      Configuration configuration = new Configuration().defaultCache(new CacheConfiguration("defaultCache", 100))
          .cache(new CacheConfiguration("example", 100).timeToIdleSeconds(30).timeToLiveSeconds(120)
                     .terracotta(new TerracottaConfiguration())); // defaults are all good

      TerracottaClientConfiguration tcClientConfiguration = new TerracottaClientConfiguration();
      tcClientConfiguration.setUrl(getTerracottaUrl());
      configuration.addTerracottaConfig(tcClientConfiguration);

      return new CacheManager(configuration);
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      final Cache exampleCache = getCacheManager().getCache("example");
      Assert.assertNotNull(exampleCache);

      // Adding eviction listener for testing.
      exampleCache.getCacheEventNotificationService().registerListener(new LoggingEvictionAdapter());

      if (index == 0) {
        exampleCache.put(new Element("abc", "def"));
        waitForAllCurrentTransactionsToComplete(exampleCache);
      }
      barrier.await();
      Element got = exampleCache.get("abc");
      Assert.assertNotNull(got);
      Assert.assertEquals("def", got.getValue());
      Assert.assertEquals(1, exampleCache.getSize());
    }


    /**
     * Eviction listener class for testing purposes.
     */
    private static class LoggingEvictionAdapter extends CacheEventListenerAdapter {
      @Override
      public void notifyElementExpired(Ehcache cache, Element element) {
        System.err.println("Expiring element (" + element.getKey() + ", " + element.getValue() + ")");
      }
    }

  }

}
