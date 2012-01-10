/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.concurrent.CyclicBarrier;

/**
 * Basic cache test for creating clustered caches programmatically
 */
public class ProgrammaticCacheTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      CacheManager cacheManager = CacheManager.getInstance();

        String cacheName = "regionName";
        Cache cache = new Cache(new CacheConfiguration(cacheName, 1000)
                .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
                .timeToLiveSeconds(300)
                .timeToIdleSeconds(300)
                .clearOnFlush(true)
                .terracotta(new TerracottaConfiguration()
                    .clustered(true)
                    .valueMode(TerracottaConfiguration.ValueMode.SERIALIZATION)
                    .coherentReads(true)
                    .orphanEviction(true)
                    .orphanEvictionPeriod(4)
                    .localKeyCache(false)
                    .localKeyCacheSize(0)
                    .copyOnRead(false)));

      cacheManager.addCache(cache);
      barrier.await();
      cache = cacheManager.getCache(cacheName);

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

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

}
