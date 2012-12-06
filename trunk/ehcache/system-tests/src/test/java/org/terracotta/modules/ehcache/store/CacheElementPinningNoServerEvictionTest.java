/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class CacheElementPinningNoServerEvictionTest extends AbstractCacheTestBase {

  private static final int ELEMENT_COUNT = 1000;

  public CacheElementPinningNoServerEvictionTest(TestConfig testConfig) {
    super("cache-pinning-test.xml", testConfig, App.class, App.class);
    testConfig.getClientConfig()
        .addExtraClientJvmArg("-Dcom.tc." + TCPropertiesConsts.L1_SERVERMAPMANAGER_FAULT_INVALIDATED_PINNED_ENTRIES
                                  + "=true");
  }

  public static class App extends ClientBase {
    public App(String[] args) {
      super("pinned", args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      System.out.println("Testing element pinning with Strong cache");
      runBasicElementPinningTest(cacheManager.getCache("pinnedElementStrongNoServerEviction"));

      System.out.println("Testing element with Eventual cache");
      runBasicElementPinningTest(cacheManager.getCache("pinnedElementEventualNoServerEviction"));
    }

    private void runBasicElementPinningTest(Cache cache) throws InterruptedException, BrokenBarrierException {
      final ToolkitBarrier barrier = getBarrierForAllClients();
      int index = barrier.await();
      debug(" Client Index = " + index);
      if (index == 0) {
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          cache.setPinned(i, true);
        }
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          cache.put(new Element(i, i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());

        for (int i = 0; i < ELEMENT_COUNT; i++) {
          assertNotNull(cache.get(i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getStatistics().getInMemoryHits());
        Assert.assertEquals(0, cache.getStatistics().getInMemoryMisses());
        Assert.assertEquals(0, cache.getStatistics().getOnDiskHits());
        Assert.assertEquals(0, cache.getStatistics().getOnDiskMisses());
        Assert.assertEquals(0, cache.getStatistics().getEvictionCount());

        for (int i = 0; i < ELEMENT_COUNT; i++) {
          cache.setPinned(i, false);
        }
        // Elements will be evicted from cache. doing gets now will go to L2.
        for (int i = 1001; i < 1010; i++) {
          cache.put(new Element(i, i));
        }
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          assertNotNull(cache.get(i));
        }
        Assert.assertTrue(2 * ELEMENT_COUNT > cache.getStatistics().getInMemoryHits());
        Assert.assertTrue(0 < cache.getStatistics().getInMemoryMisses());
        Assert.assertTrue(0 < cache.getStatistics().getOnDiskHits());
        cache.removeAll();
      }

      barrier.await();
      // This keys are first put then pinned in order to test races on L1 when an element is pinned and eviction can
      // occur on same time.
      if (index == 1) {
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          cache.put(new Element(i, i + 1));
          cache.setPinned(i, true);
        }
        waitForAllCurrentTransactionsToComplete(cache);
        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());
      }

      barrier.await();

      for (int i = 0; i < ELEMENT_COUNT; i++) {
        assertNotNull(cache.get(i));
      }

      barrier.await();
      // This keys are first put then pinned in order to test races on L1 when an element is pinned and eviction can
      // occur on same time.
      if (index == 0) {
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          cache.put(new Element(i, i + 2));
          cache.setPinned(i, true);
        }
        waitForAllCurrentTransactionsToComplete(cache);
      }

      barrier.await();
      for (int i = 0; i < ELEMENT_COUNT; i++) {
        Assert.assertEquals(new Element(i, i + 2), cache.get(i));
      }
    }
  }
}
