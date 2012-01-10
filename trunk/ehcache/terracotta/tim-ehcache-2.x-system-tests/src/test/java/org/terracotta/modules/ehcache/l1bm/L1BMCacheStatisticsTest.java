/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.logging.LogLevel;
import com.tc.logging.LogLevelImpl;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.servermap.localcache.impl.L1ServerMapEvictedElementsContext;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalStoreTransactionCompletionListener;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.CallableWaiter;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

public class L1BMCacheStatisticsTest extends TransparentTestBase {
  private static final int NODE_COUNT         = 1;
  private static final int NUMBER_OF_ELEMENTS = 10000;
  private static final int ONHEAP_SIZE        = 10 * 1024 * 1024;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected void setL1ClassLoggingLevels(Map<Class<?>, LogLevel> logLevels) {
    super.setL1ClassLoggingLevels(logLevels);
    logLevels.put(L1ServerMapLocalStoreTransactionCompletionListener.class, LogLevelImpl.DEBUG);
    logLevels.put(L1ServerMapEvictedElementsContext.class, LogLevelImpl.DEBUG);
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {
    private final CacheManager cm = CacheManager.getInstance();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName());

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }

    @Override
    protected void runTest() throws Throwable {
      Cache strongCache = createCache("strong-count-cache", Consistency.STRONG, ValueMode.SERIALIZATION, 1000, 0);
      testCache(strongCache);

      Cache eventualCache = createCache("eventual-count-cache", Consistency.EVENTUAL, ValueMode.SERIALIZATION, 1000, 0);
      testCache(eventualCache);

      Cache strongSizeCache = createCache("strong-size-cache", Consistency.STRONG, ValueMode.SERIALIZATION, 0,
                                          ONHEAP_SIZE);
      testSizeCache(strongSizeCache);

      Cache eventualSizeCache = createCache("eventual-size-cache", Consistency.EVENTUAL, ValueMode.SERIALIZATION, 0,
                                            ONHEAP_SIZE);
      testSizeCache(eventualSizeCache);
    }

    private void loadCache(Cache cache) {
      for (int i = 0; i < NUMBER_OF_ELEMENTS; i++) {
        cache.put(new Element("key-" + i, "value-" + i));
      }
    }

    private void hitCache(Cache cache) {
      Random random = new Random();
      for (int i = 0; i < NUMBER_OF_ELEMENTS * 2; i++) {
        cache.get("key-" + random.nextInt(NUMBER_OF_ELEMENTS * 2));
      }
    }

    protected void testCache(Cache cache) throws Throwable {
      try {
        loadCache(cache);
        assertCacheSize(cache, NUMBER_OF_ELEMENTS, 1000);
        hitCache(cache);
        assertStatistics(cache, NUMBER_OF_ELEMENTS, NUMBER_OF_ELEMENTS, 1000, NUMBER_OF_ELEMENTS * 2 - 1000);
      } finally {
        printCacheStatistics(cache);
      }
    }

    protected void testSizeCache(Cache cache) throws Exception {
      try {
        loadCache(cache);
        hitCache(cache);
        assertStatistics(cache, NUMBER_OF_ELEMENTS, NUMBER_OF_ELEMENTS, cache.getMemoryStoreSize(),
                         NUMBER_OF_ELEMENTS * 2 - cache.getMemoryStoreSize());
        testSizeCacheStability(cache);
      } finally {
        printCacheStatistics(cache);
      }
    }

    private void testSizeCacheStability(Cache cache) throws Exception {
      long key = NUMBER_OF_ELEMENTS;
      // Fill up the cache to a small 3% tolerance.
      while (cache.calculateInMemorySize() < (0.97 * ONHEAP_SIZE)) {
        cache.put(new Element("key-" + key, "value-" + key));
        key++;
      }
      // Put a whole bunch more items to check that the size in bytes remains about the same
      for (long i = key; i < key + NUMBER_OF_ELEMENTS; i++) {
        cache.put(new Element("key-" + i, "value-" + i));
      }
      assertNearEqualsWithinTimeout(ONHEAP_SIZE, memoryStoreSizeInBytes(cache), 0.1);
    }

    protected void assertCacheSize(Cache cache, int cacheSize, int inMemoryCacheSize) throws Exception {
      assertEquals(cacheSize, cache.getSize());
      assertNearEqualsWithinTimeout(inMemoryCacheSize, memoryStoreCount(cache), 0.175);
    }

    protected void assertStatistics(Cache cache, long cacheHits, long cacheMisses, long inMemoryHits,
                                    long inMemoryMisses) throws Exception {
      assertNearEquals(cacheHits, cache.getLiveCacheStatistics().getCacheHitCount(), 0.05);
      assertNearEquals(cacheMisses, cache.getLiveCacheStatistics().getCacheMissCount(), 0.05);
      assertNearEquals(inMemoryHits, cache.getLiveCacheStatistics().getInMemoryHitCount(), 0.2);
      assertNearEquals(inMemoryMisses, cache.getLiveCacheStatistics().getInMemoryMissCount(), 0.2);
      assertEquals(0, cache.getLiveCacheStatistics().getOffHeapHitCount());
      assertEquals(0, cache.getLiveCacheStatistics().getOffHeapMissCount());
    }

    private void assertNearEquals(final long expected, final long actual, final double tolerance) {
      final long lowerBound = (long) ((1.0 - tolerance) * expected);
      final long upperBound = (long) ((1.0 + tolerance) * expected);
      assertTrue("Number out of expected range [" + lowerBound + ", " + upperBound + "] actual=" + actual,
                 lowerBound <= actual && upperBound >= actual);
    }

    private void assertNearEqualsWithinTimeout(final long expected, final Callable<Long> getActual,
                                               final double tolerance) throws Exception {
      final long lowerBound = (long) ((1.0 - tolerance) * expected);
      final long upperBound = (long) ((1.0 + tolerance) * expected);
      CallableWaiter.waitOnCallable(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          final long actual = getActual.call();
          if (lowerBound <= actual && upperBound >= actual) { return true; }
          info("Still waiting, Number out of expected range [" + lowerBound + ", " + upperBound + "] actual=" + actual);
          return false;
        }
      });
    }

    private Callable<Long> memoryStoreCount(final Cache cache) {
      return new Callable<Long>() {
        public Long call() throws Exception {
          return cache.getMemoryStoreSize();
        }
      };
    }

    private Callable<Long> memoryStoreSizeInBytes(final Cache cache) {
      return new Callable<Long>() {
        public Long call() throws Exception {
          return cache.calculateInMemorySize();
        }
      };
    }

    private void info(String s) {
      System.out.println(s);
    }

    private void printCacheStatistics(Cache cache) {
      info("Cache name: " + cache.getName());
      info("Cache size: " + cache.getSize());
      info("Cache in memory size: " + cache.getMemoryStoreSize());
      info("Cache off heap size: " + cache.getOffHeapStoreSize());
      info("Cache hits: " + cache.getLiveCacheStatistics().getCacheHitCount() + " misses: "
           + cache.getLiveCacheStatistics().getCacheMissCount());
      info("Cache in memory hits: " + cache.getLiveCacheStatistics().getInMemoryHitCount() + " misses: "
           + cache.getLiveCacheStatistics().getInMemoryMissCount());
      info("Cache off heap hits: " + cache.getLiveCacheStatistics().getOffHeapHitCount() + " misses: "
           + cache.getLiveCacheStatistics().getOffHeapMissCount());
      info("Cache expirations: " + cache.getLiveCacheStatistics().getExpiredCount());
    }

    private Cache createCache(String cacheName, Consistency consistency, ValueMode valueMode, int maxEntriesOnHeap,
                              long maxBytesOnHeap) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setEternal(true);
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxEntriesLocalHeap(maxEntriesOnHeap);
      if (maxBytesOnHeap > 0) {
        cacheConfiguration.setMaxBytesLocalHeap(maxBytesOnHeap);
      }

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setStorageStrategy("DCV2");
      tcConfiguration.setConsistency(consistency);
      tcConfiguration.setValueMode(valueMode.name());
      cacheConfiguration.addTerracotta(tcConfiguration);

      cacheConfiguration.setStatistics(true);

      Cache cache = new Cache(cacheConfiguration);
      cm.addCache(cache);
      return cache;
    }
  }
}
