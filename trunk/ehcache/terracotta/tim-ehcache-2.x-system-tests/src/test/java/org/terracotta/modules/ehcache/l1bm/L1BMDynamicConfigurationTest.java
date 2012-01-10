/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.CallableWaiter;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;

public class L1BMDynamicConfigurationTest extends TransparentTestBase {
  private static final int NODE_COUNT = 2;

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
    private static final int           SIZE_TEST_ELEMENTS  = 10000;
    private static final double        SIZE_TEST_TOLERANCE = 0.07;
    private static final CacheManager  cm                  = CacheManager.getInstance();
    private static final CyclicBarrier barrier             = new CyclicBarrier(NODE_COUNT);

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }

    @Override
    protected void runTest() throws Throwable {
      testSizeBasedCache();
      testCountBasedCache();
    }

    private void testSizeBasedCache() throws Throwable {
      final int nodeId = barrier.await();
      Cache sizeCache = createCache("size-cache", 0, MemoryUnit.MEGABYTES.toBytes(4));
      barrier.await();
      testCacheSizeLimit(sizeCache, MemoryUnit.MEGABYTES.toBytes(4));
      barrier.await();
      if (nodeId == 0) {
        sizeCache.getCacheConfiguration().setMaxBytesLocalHeap(MemoryUnit.MEGABYTES.toBytes(8));
      } else {
        sizeCache.getCacheConfiguration().setMaxBytesLocalHeap(MemoryUnit.MEGABYTES.toBytes(2));
      }
      barrier.await();
      if (nodeId == 0) {
        testCacheSizeLimit(sizeCache, MemoryUnit.MEGABYTES.toBytes(8));
      } else {
        testCacheSizeLimit(sizeCache, MemoryUnit.MEGABYTES.toBytes(2));
      }
      barrier.await();
      if (nodeId == 0) {
        sizeCache.getCacheConfiguration().setMaxBytesLocalHeap(MemoryUnit.MEGABYTES.toBytes(4));
      }
      barrier.await();
      if (nodeId == 0) {
        testCacheSizeLimit(sizeCache, MemoryUnit.MEGABYTES.toBytes(4));
      } else {
        testCacheSizeLimit(sizeCache, MemoryUnit.MEGABYTES.toBytes(2));
      }
    }

    private void testCacheSizeLimit(final Cache cache, final long sizeInBytes) throws Exception {
      final Random r = new Random();
      for (int i = 0; i < SIZE_TEST_ELEMENTS; i++) {
        cache.put(new Element("key" + r.nextInt(), "value" + r.nextInt()));
      }
      CallableWaiter.waitOnCallable(new Callable<Boolean>() {

        public Boolean call() throws Exception {
          long cacheSize = cache.calculateInMemorySize();
          if (cacheSize < sizeInBytes * (1 - SIZE_TEST_TOLERANCE)) {
            System.out.println("Cache size below threshold. Size in bytes: " + cacheSize);
            return false;
          }
          return true;
        }
      });

      CallableWaiter.waitOnCallable(new Callable<Boolean>() {

        public Boolean call() throws Exception {
          long cacheSize = cache.calculateInMemorySize();
          if (cacheSize > sizeInBytes * (1 + SIZE_TEST_TOLERANCE)) {
            System.out.println("Cache size is exceeding the size limit. Size in bytes: " + cacheSize);
            return false;
          }
          return true;
        }
      });
      long cacheSize = cache.calculateInMemorySize();
      assertTrue("Cache size is exceeding the size limit. Size in bytes: " + cacheSize,
                 cacheSize <= sizeInBytes * (1 + SIZE_TEST_TOLERANCE));
      assertTrue("Cache size below threshold. Size in bytes: " + cacheSize, cacheSize >= (1 - SIZE_TEST_TOLERANCE));
    }

    private void testCountBasedCache() throws Throwable {
      final int nodeId = barrier.await();
      final Cache countCache = createCache("count-cache", 300, 0);

      barrier.await();

      testCacheCountLimit(countCache, 300);

      barrier.await();

      if (nodeId == 0) {
        countCache.getCacheConfiguration().setMaxEntriesLocalHeap(200);
      } else {
        countCache.getCacheConfiguration().setMaxEntriesLocalHeap(100);
      }

      barrier.await();

      if (nodeId == 0) {
        testCacheCountLimit(countCache, 200);
      } else {
        testCacheCountLimit(countCache, 100);
      }

      barrier.await();

      if (nodeId == 0) {
        countCache.getCacheConfiguration().setMaxEntriesLocalHeap(50);
      }

      barrier.await();

      if (nodeId == 0) {
        testCacheCountLimit(countCache, 50);
      } else {
        testCacheCountLimit(countCache, 100);
      }
    }

    private void testCacheCountLimit(final Cache cache, int maxEntriesOnLocalHeap) throws InterruptedException {
      final Random r = new Random();
      for (int i = 0; i < maxEntriesOnLocalHeap * 3; i++) {
        cache.put(new Element("key" + r.nextInt(), "value" + r.nextInt()));
      }

      int tenPercent = (maxEntriesOnLocalHeap * 10) / 100;
      int max = maxEntriesOnLocalHeap + tenPercent;
      long memoryStoreSize = cache.getMemoryStoreSize();

      while (memoryStoreSize > max) {
        System.out.println("Memory Store size: " + memoryStoreSize + " expected 0-" + max);
        Thread.sleep(1000);
        memoryStoreSize = cache.getMemoryStoreSize();
      }
      assertTrue("Cache size differs from expected range. Size: " + memoryStoreSize,
                 (memoryStoreSize > 0 && memoryStoreSize <= max));
    }

    private Cache createCache(String cacheName, int maxEntriesOnHeap, long maxBytesOnHeap) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setEternal(true);
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxEntriesLocalHeap(maxEntriesOnHeap);
      if (maxBytesOnHeap > 0) {
        cacheConfiguration.setMaxBytesLocalHeap(maxBytesOnHeap);
      }

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setStorageStrategy("DCV2");
      tcConfiguration.setConsistency(Consistency.EVENTUAL);
      tcConfiguration.setValueMode(ValueMode.SERIALIZATION.toString());
      cacheConfiguration.addTerracotta(tcConfiguration);

      cacheConfiguration.setStatistics(true);

      Cache cache = new Cache(cacheConfiguration);
      cm.addCache(cache);
      return cache;
    }
  }
}
