package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.concurrent.CyclicBarrier;

public class ServerMapBasicCacheApp extends AbstractErrorCatchingTransparentApp {

  private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());

  public ServerMapBasicCacheApp(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
  }

  @Override
  protected void runTest() throws Throwable {
    final int index = this.barrier.await();

    final CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/server-map-cache-test.xml"));

    Cache cache = cacheManager.getCache("defaultStorageStrategyCache");
    Assert.assertEquals(StorageStrategy.DCV2, cache.getCacheConfiguration().getTerracottaConfiguration()
        .getStorageStrategy());

    System.out.println("Asserted different default/explicit storage strategys.");

    cache = cacheManager.getCache("test");
    // assert correct storageStrategy is used
    Assert.assertEquals(StorageStrategy.DCV2, cache.getCacheConfiguration().getTerracottaConfiguration()
        .getStorageStrategy());

    // XXX: assert that the cache is clustered via methods on cache
    // config (when methods exist)

    Assert.assertEquals(0, cache.getSize());

    this.barrier.await();

    if (index == 0) {
      cache.put(new Element("key", "value"));
    }

    this.barrier.await();

    Assert.assertEquals(1, cache.getSize());
    Assert.assertEquals("value", cache.get("key").getObjectValue());
    System.err.println("Index : " + index + " : Cache size : " + cache.getSize());

    this.barrier.await();

    if (index == 0) {
      final boolean removed = cache.remove("key");
      Assert.assertTrue(removed);
    }

    this.barrier.await();

    Assert.assertEquals(0, cache.getSize());
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    config.getOrCreateSpec(ServerMapBasicCacheApp.class.getName()).addRoot("barrier", "barrier");

    final String module_name = "tim-ehcache-2.x";
    final TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
    config.addModule(timInfo.artifactId(), timInfo.version());
  }
}
