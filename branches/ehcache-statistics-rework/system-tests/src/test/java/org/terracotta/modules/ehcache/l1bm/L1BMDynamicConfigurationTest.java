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

import org.junit.Assert;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;
import com.tc.util.CallableWaiter;

import java.util.Random;
import java.util.concurrent.Callable;

public class L1BMDynamicConfigurationTest extends AbstractCacheTestBase {

  public L1BMDynamicConfigurationTest(TestConfig testConfig) {
    super(testConfig, App.class, App.class);
  }

  public static class App extends ClientBase {
    private static final int    SIZE_TEST_ELEMENTS  = 10000;
    private static final double SIZE_TEST_TOLERANCE = 0.15;
    private final ToolkitBarrier       barrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", getParticipantCount());

    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      testSizeBasedCache(cacheManager);
      testCountBasedCache(cacheManager);
    }

    private void testSizeBasedCache(CacheManager cm) throws Throwable {
      final int nodeId = barrier.await();
      Cache sizeCache = createCache(cm, "size-cache", 0, MemoryUnit.MEGABYTES.toBytes(4));
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

        @Override
        public Boolean call() throws Exception {
          long cacheSize = cache.getStatistics().getLocalHeapSizeInBytes();
          double minSize = sizeInBytes * (1.0 - SIZE_TEST_TOLERANCE);
          double maxSize = sizeInBytes * (1.0 + SIZE_TEST_TOLERANCE);
          if (cacheSize < minSize || cacheSize > maxSize) {
            System.out.println("Cache size outside of allowable range. Actual=" + cacheSize + " expected range=["
                               + minSize + ", " + maxSize + "]");
            return false;
          }
          return true;
        }
      });
      long cacheSize = cache.getStatistics().getLocalHeapSizeInBytes();
      Assert.assertTrue("Cache size is exceeding the size limit. Size in bytes: " + cacheSize,
                        cacheSize <= sizeInBytes * (1 + SIZE_TEST_TOLERANCE));
      Assert.assertTrue("Cache size below threshold. Size in bytes: " + cacheSize,
                        cacheSize >= (1 - SIZE_TEST_TOLERANCE));
    }

    private void testCountBasedCache(CacheManager cm) throws Throwable {
      final int nodeId = barrier.await();
      final Cache countCache = createCache(cm, "count-cache", 300, 0);

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
      long memoryStoreSize = cache.getStatistics().getLocalHeapSize();

      while (memoryStoreSize > max) {
        System.out.println("Memory Store size: " + memoryStoreSize + " expected 0-" + max);
        Thread.sleep(1000);
        memoryStoreSize = cache.getStatistics().getLocalHeapSize();
      }
      Assert.assertTrue("Cache size differs from expected range. Size: " + memoryStoreSize,
                        (memoryStoreSize > 0 && memoryStoreSize <= max));
    }

    private Cache createCache(CacheManager cm, String cacheName, int maxEntriesOnHeap, long maxBytesOnHeap) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setEternal(true);
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxEntriesLocalHeap(maxEntriesOnHeap);
      if (maxBytesOnHeap > 0) {
        cacheConfiguration.setMaxBytesLocalHeap(maxBytesOnHeap);
      }

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
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
