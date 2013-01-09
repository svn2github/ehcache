/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class L1BMOnHeapReadWriteTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT       = 2;
  private static final int NUM_OF_ELEMENTS  = 1000;
  private static final int NUM_OF_THREADS   = 10;
  private static final int WRITE_PERCENTAGE = 5;

  public L1BMOnHeapReadWriteTest(TestConfig testConfig) {
    super(testConfig, L1BMOnHeapReadWriteTestApp.class, L1BMOnHeapReadWriteTestApp.class);
  }

  public static class L1BMOnHeapReadWriteTestApp extends ClientBase {
    private final ToolkitBarrier barrier;

    public L1BMOnHeapReadWriteTestApp(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", NODE_COUNT);
    }

    public static void main(String[] args) {
      new L1BMOnHeapReadWriteTestApp(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      boolean shouldWait = true;
      Cache eventualWithStatsCache = createCache("eventualWithStatsCache", cacheManager, "EVENTUAL", true);
      testL1BigMemorySanity(eventualWithStatsCache, shouldWait);
      eventualWithStatsCache.removeAll();

      Cache eventualWithoutStatsCache = createCache("eventualWithoutStatsCache", cacheManager, "EVENTUAL", false);
      testL1BigMemorySanity(eventualWithoutStatsCache, shouldWait);
      eventualWithoutStatsCache.removeAll();

      shouldWait = false;
      Cache strongWithStatsCache = createCache("strongWithStatsCache", cacheManager, "STRONG", true);
      testL1BigMemorySanity(strongWithStatsCache, shouldWait);
      strongWithStatsCache.removeAll();

      shouldWait = false;
      Cache strongWithoutStatsCache = createCache("strongWithoutStatsCache", cacheManager, "STRONG", false);
      testL1BigMemorySanity(strongWithoutStatsCache, shouldWait);
      strongWithoutStatsCache.removeAll();
    }

    private void testL1BigMemorySanity(Cache cache, boolean shouldWait) throws InterruptedException,
        BrokenBarrierException {
      int index = barrier.await();
      if (index == 0) {
        System.out.println("XXXXXX putting " + NUM_OF_ELEMENTS + " in the cache");
        for (int i = 0; i < NUM_OF_ELEMENTS; i++) {
          cache.put(new Element("key" + i, "val" + i));
        }
        System.out.println("XXXXX done with putting " + cache.getSize() + " entries");
      }
      barrier.await();
      if (shouldWait) {
        while (cache.getSize() != NUM_OF_ELEMENTS) {
          Thread.sleep(1000);
        }
      }
      Assert.assertEquals(NUM_OF_ELEMENTS, cache.getSize());
      System.out.println("XXXXXX client " + index + " cache size: " + cache.getSize() + " local: "
                         + cache.getStatistics().getLocalHeapSize());
      if (index == 0) {
        Assert.assertTrue(cache.getStatistics().getLocalHeapSize() > 0);
      } else {
        Assert.assertEquals(0, cache.getStatistics().getLocalHeapSize());
      }

      System.out.println("XXXXX starting test threads....");
      Thread ths[] = new Thread[NUM_OF_THREADS];
      for (int i = 0; i < NUM_OF_THREADS; i++) {
        ths[i] = new Thread(new TestThread(cache, i, index), "testThread" + i);
        ths[i].start();
      }

      for (Thread th : ths) {
        th.join();
      }
      barrier.await();
      System.out.println("XXXXXX done with " + cache.getName());
    }

    private Cache createCache(String cacheName, CacheManager cm, String consistency, boolean isWithStats) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxBytesLocalHeap(10485760L);

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setConsistency(consistency);
      tcConfiguration.setValueMode("SERIALIZATION");
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cm.addCache(cache);
      return cache;
    }

    private static class TestThread implements Runnable {
      private final long  TIME_TO_RUN = 1 * 60 * 1000;
      private final Cache cache;
      private final int   threadIndex;
      private final int   clientIndex;

      public TestThread(Cache cache, int threadIndex, int clientIndex) {
        this.cache = cache;
        this.threadIndex = threadIndex;
        this.clientIndex = clientIndex;
      }

      @Override
      public void run() {
        System.out.println("XXXXX client[" + clientIndex + "] started thread " + threadIndex);
        long start = System.currentTimeMillis();
        Random rand = new Random(start);
        while (System.currentTimeMillis() - start < TIME_TO_RUN) {
          if (rand.nextInt(100) < WRITE_PERCENTAGE) {
            this.cache.put(new Element("key" + rand.nextInt(NUM_OF_ELEMENTS), "val" + rand.nextInt(NUM_OF_ELEMENTS)));
          } else {
            String key = "key" + rand.nextInt(NUM_OF_ELEMENTS);
            Assert.assertNotNull("value for " + key + " is null", this.cache.get(key));
          }
        }
      }
    }
  }
}
