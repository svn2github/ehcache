/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.Callable;

import junit.framework.Assert;

public class CacheElementPinningTest extends AbstractCacheTestBase {

  private static final int ELEMENT_COUNT = 1000;
  private static final int EXTRA_COUNT   = 10;

  public CacheElementPinningTest(TestConfig testConfig) {
    super("cache-pinning-test.xml", testConfig, App.class, App.class);
  }

  public static class App extends ClientBase {
    public App(String[] args) {
      super("pinned", args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      System.out.println("Testing element pinning with Strong cache");
      runBasicElementPinningTest(cacheManager.getCache("pinnedElementStrong"));

      System.out.println("Testing element with Eventual cache");
      runBasicElementPinningTest(cacheManager.getCache("pinnedElementEventual"));
    }

    private void runBasicElementPinningTest(final Cache cache) throws Exception {
      final ToolkitBarrier barrier = getBarrierForAllClients();
      int index = barrier.await();
      debug("Client Index = " + index + " for " + cache.getName());
      if (index == 0) {
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          cache.setPinned(i, true);
          cache.put(new Element(i, i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());

        for (int i = 0; i < ELEMENT_COUNT; i++) {
          assertNotNull(cache.get(i));
        }

        Assert.assertEquals(ELEMENT_COUNT, cache.getMemoryStoreSize());
        Assert.assertEquals(0, cache.getStatistics().getInMemoryMisses());
        Assert.assertEquals(0, cache.getStatistics().getOnDiskHits());
        Assert.assertEquals(0, cache.getStatistics().getOnDiskMisses());
        Assert.assertEquals(0, cache.getStatistics().getEvictionCount());

        for (int i = 0; i < ELEMENT_COUNT; i++) {
          cache.setPinned(i, false);
        }
        // Elements will be evicted from cache. doing gets now will go to L2.
        for (int i = ELEMENT_COUNT; i < ELEMENT_COUNT + EXTRA_COUNT; i++) {
          cache.put(new Element(i, i));
        }
        waitForAllCurrentTransactionsToComplete(cache);
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          cache.get(i);
        }

        Assert.assertTrue(cache.getMemoryStoreSize() < ELEMENT_COUNT);
        Assert.assertTrue(0 < cache.getStatistics().getInMemoryMisses());
        Assert.assertTrue(0 < cache.getStatistics().getOnDiskHits());
        cache.unpinAll();
        cache.removeAll();
        waitForAllCurrentTransactionsToComplete(cache);
        debug("done testing pining with client " + index + " size " + cache.getSize());
      }

      barrier.await();

      if (index == 1) {
        debug("pinning elemnts on client " + index);
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          cache.setPinned(i, true);
          cache.put(new Element(i, i + 1));
        }
        waitForAllCurrentTransactionsToComplete(cache);
        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());
      }

      barrier.await();
      debug("asserting elemnts on client " + index);
      for (int i = 0; i < ELEMENT_COUNT; i++) {
        assertNotNull(cache.get(i));
      }

      barrier.await();
      if (index == 0) {
        debug("pinning and updating elemnts on client " + index);
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          cache.setPinned(i, true);
          cache.put(new Element(i, i + 2));
        }
        waitForAllCurrentTransactionsToComplete(cache);
      }

      Assert.assertEquals(ELEMENT_COUNT, cache.getSize());
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          System.out.println("memory store count " + cache.getMemoryStoreSize());
          return cache.getMemoryStoreSize() >= ELEMENT_COUNT;
        }
      });
      barrier.await();

      debug("asserting elemnts again on client " + index);
      for (int i = 0; i < ELEMENT_COUNT; i++) {
        Assert.assertEquals(new Element(i, i + 2), cache.get(i));
      }
    }
  }
}
