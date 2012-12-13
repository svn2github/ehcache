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

import org.junit.Assert;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;
import com.tc.util.CallableWaiter;

import java.util.concurrent.Callable;

public class L1BMCacheStatisticsTest extends AbstractCacheTestBase {

  public L1BMCacheStatisticsTest(TestConfig testConfig) {
    super(testConfig, App.class);
    testConfig.getL2Config().setMaxHeap(512);
  }

  public static class App extends ClientBase {
    private static final int NUMBER_OF_ELEMENTS = 5000;
    private static final int ONHEAP_SIZE        = 10 * 1024 * 1024;
    private static final int THROTTLE           = 10;
    private static final int MEMORY_STORE_COUNT = 1000;

    public App(String[] args) {
      super(args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      Cache strongCache = createCache(cacheManager, "strong-count-cache", Consistency.STRONG, ValueMode.SERIALIZATION,
                                      1000, 0);
      testCache(strongCache);

      Cache eventualCache = createCache(cacheManager, "eventual-count-cache", Consistency.EVENTUAL,
                                        ValueMode.SERIALIZATION, 1000, 0);
      testCache(eventualCache);

      Cache strongSizeCache = createCache(cacheManager, "strong-size-cache", Consistency.STRONG,
                                          ValueMode.SERIALIZATION, 0, ONHEAP_SIZE);
      testSizeCache(strongSizeCache);

      Cache eventualSizeCache = createCache(cacheManager, "eventual-size-cache", Consistency.EVENTUAL,
                                            ValueMode.SERIALIZATION, 0, ONHEAP_SIZE);
      testSizeCache(eventualSizeCache);
    }

    private void loadCache(Cache cache, boolean slowMode) throws Exception {
      for (int i = 0; i < NUMBER_OF_ELEMENTS; i++) {
        cache.put(new Element("key-" + i, "value-" + i));
        if (slowMode) {
          // MNK-3263: If we put too fast on a slow machine it winds up building up too many eviction contexts. That
          // inevitably brings down the memory store size below acceptable thresholds. To avoid this we just put slower,
          // that should give the rest of the system a bit more time to catch up with a slow machine.
          Thread.sleep(THROTTLE);
        }
      }
    }

    private void hitCache(Cache cache) throws Exception {
      for (int i = NUMBER_OF_ELEMENTS * 2 - 1; i >= 0; i--) {
        cache.get("key-" + i);
      }
    }

    protected void testCache(Cache cache) throws Throwable {
      try {
        loadCache(cache, true);
        assertCacheSize(cache, NUMBER_OF_ELEMENTS, MEMORY_STORE_COUNT);
        hitCache(cache);
        assertStatistics(cache, NUMBER_OF_ELEMENTS, NUMBER_OF_ELEMENTS, MEMORY_STORE_COUNT, NUMBER_OF_ELEMENTS * 2
                                                                                            - MEMORY_STORE_COUNT);
      } finally {
        printCacheStatistics(cache);
        cache.removeAll();
      }
    }

    protected void testSizeCache(Cache cache) throws Exception {
      try {
        loadCache(cache, true);
        hitCache(cache);
        assertStatistics(cache, NUMBER_OF_ELEMENTS, NUMBER_OF_ELEMENTS, cache.getMemoryStoreSize(),
                         NUMBER_OF_ELEMENTS * 2 - cache.getMemoryStoreSize());
        testSizeCacheStability(cache);
      } finally {
        printCacheStatistics(cache);
        cache.removeAll();
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
      Assert.assertEquals(cacheSize, cache.getSize());
      assertNearEqualsWithinTimeout(inMemoryCacheSize, memoryStoreCount(cache), 0.175);
    }

    protected void assertStatistics(Cache cache, long cacheHits, long cacheMisses, long inMemoryHits,
                                    long inMemoryMisses) throws Exception {
      assertNearEquals(cacheHits, cache.getStatistics().cacheHitCount(), 0.05);
      assertNearEquals(cacheMisses, cache.getStatistics().cacheMissCount(), 0.05);
      assertNearEquals(inMemoryHits, cache.getStatistics().localHeapHitCount(), 0.2);
      assertNearEquals(inMemoryMisses, cache.getStatistics().localHeapMissCount(), 0.2);
      Assert.assertEquals(0, cache.getStatistics().localOffHeapHitCount());
      Assert.assertEquals(0, cache.getStatistics().localOffHeapMissCount());
    }

    private void assertNearEquals(final long expected, final long actual, final double tolerance) {
      final long lowerBound = (long) ((1.0 - tolerance) * expected);
      final long upperBound = (long) ((1.0 + tolerance) * expected);
      Assert.assertTrue("Number out of expected range [" + lowerBound + ", " + upperBound + "] actual=" + actual,
                        lowerBound <= actual && upperBound >= actual);
    }

    private void assertNearEqualsWithinTimeout(final long expected, final Callable<Long> getActual,
                                               final double tolerance) throws Exception {
      final long lowerBound = (long) ((1.0 - tolerance) * expected);
      final long upperBound = (long) ((1.0 + tolerance) * expected);
      CallableWaiter.waitOnCallable(new Callable<Boolean>() {
        @Override
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
        @Override
        public Long call() throws Exception {
          return cache.getMemoryStoreSize();
        }
      };
    }

    private Callable<Long> memoryStoreSizeInBytes(final Cache cache) {
      return new Callable<Long>() {
        @Override
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
      info("Cache hits: " + cache.getStatistics().cacheHitCount() + " misses: "
           + cache.getStatistics().cacheMissCount());
      info("Cache in memory hits: " + cache.getStatistics().localHeapHitCount() + " misses: "
           + cache.getStatistics().localHeapMissCount());
      info("Cache off heap hits: " + cache.getStatistics().localOffHeapHitCount() + " misses: "
           + cache.getStatistics().localOffHeapMissCount());
      info("Cache expirations: " + cache.getStatistics().cacheExpiredCount());
    }

    private Cache createCache(CacheManager cm, String cacheName, Consistency consistency, ValueMode valueMode,
                              int maxEntriesOnHeap, long maxBytesOnHeap) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setEternal(true);
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxEntriesLocalHeap(maxEntriesOnHeap);
      if (maxBytesOnHeap > 0) {
        cacheConfiguration.setMaxBytesLocalHeap(maxBytesOnHeap);
      }

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
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
