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

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class L1BMOnHeapReadWriteTestApp extends AbstractErrorCatchingTransparentApp {
  public static final String  CONSISTENCY      = "consistency";
  public static final String  VALUE_MODE       = "valueMode";
  public static final String  WITH_STATS       = "withStats";
  public static final String  STORAGE_STRATEGY = "storage strategy";

  public static final int     NODE_COUNT       = 2;
  private static final int    NUM_OF_ELEMENTS  = 10000;
  private static final int    NUM_OF_THREADS   = 10;
  private static final int    WRITE_PERCENTAGE = 5;
  private final CyclicBarrier barrier          = new CyclicBarrier(NODE_COUNT);
  private final String        consitency;
  private final String        valueMode;
  private final boolean       withStats;
  private final String        storageStrategy;

  public L1BMOnHeapReadWriteTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    this.consitency = cfg.getAttribute(CONSISTENCY);
    this.valueMode = cfg.getAttribute(VALUE_MODE);
    this.withStats = Boolean.parseBoolean(cfg.getAttribute(WITH_STATS));
    this.storageStrategy = cfg.getAttribute(STORAGE_STRATEGY);
  }

  @Override
  protected void runTest() throws Throwable {
    CacheManager cacheManager = CacheManager.create();
    Cache testCache = crerateCache("testCache", cacheManager, this.storageStrategy, this.consitency, this.valueMode,
                                   this.withStats);
    boolean shouldWait = false;
    if (this.consitency == Consistency.EVENTUAL.name()) {
      shouldWait = true;
    }
    testL1BigMemorySanity(testCache, shouldWait);
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    config.getOrCreateSpec(L1BMOnHeapReadWriteTestApp.class.getName()).addRoot("barrier", "barrier")
        .addRoot("nodes", "nodes");

    String module_name = "tim-ehcache-2.x";
    TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
    config.addModule(timInfo.artifactId(), timInfo.version());
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
                       + cache.getMemoryStoreSize());
    if (index == 0) {
      Assert.assertTrue(cache.getMemoryStoreSize() > 0);
    } else {
      Assert.assertEquals(0, cache.getMemoryStoreSize());
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

  private Cache crerateCache(String cacheName, CacheManager cacheManager, String strategy, String consistency,
                             String mode, boolean isWithStats) {
    CacheConfiguration cacheConfiguration = new CacheConfiguration();
    cacheConfiguration.setName(cacheName);
    cacheConfiguration.setMaxBytesLocalHeap(10485760L);

    TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
    tcConfiguration.setStorageStrategy(strategy);
    tcConfiguration.setConsistency(consistency);
    tcConfiguration.setValueMode(mode);
    cacheConfiguration.addTerracotta(tcConfiguration);

    Cache cache = new Cache(cacheConfiguration);
    cacheManager.addCache(cache);
    cache.setStatisticsEnabled(isWithStats);
    return cache;
  }

  private static class TestThread implements Runnable {
    private final long  TIME_TO_RUN = 5 * 60 * 1000;
    private final Cache cache;
    private final int   threadIndex;
    private final int   clientIndex;

    public TestThread(Cache cache, int threadIndex, int clientIndex) {
      this.cache = cache;
      this.threadIndex = threadIndex;
      this.clientIndex = clientIndex;
    }

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
