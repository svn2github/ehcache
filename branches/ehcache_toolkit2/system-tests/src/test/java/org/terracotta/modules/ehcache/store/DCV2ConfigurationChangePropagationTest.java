/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;
import com.tc.util.concurrent.ThreadUtil;

import junit.framework.Assert;

/**
 * @author cdennis
 */
public class DCV2ConfigurationChangePropagationTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 3;

  public DCV2ConfigurationChangePropagationTest(TestConfig testConfig) {
    super("basic-dcv2-cache-test.xml", testConfig, App.class, App.class, App.class);
  }

  public static class App extends ClientBase {

    private final ToolkitBarrier barrier1;
    private final ToolkitBarrier barrier2;
    private int           clientId;

    public App(String[] args) {
      super(args);
      this.barrier1 = getClusteringToolkit().getBarrier("barrier1", NODE_COUNT);
      this.barrier2 = getClusteringToolkit().getBarrier("barrier2", NODE_COUNT - 1);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      clientId = barrier1.await();

      if (clientId != 2) {
        CacheManager cm = cacheManager;
        cacheManager.shutdown();
        try {
          setupCacheManager();
          cm = testTTIChange();
        } finally {
          cm.shutdown();
        }
        try {
          setupCacheManager();
          cm = testTTLChange();
        } finally {
          cm.shutdown();
        }
        try {
          setupCacheManager();
          cm = testDiskCapacityChange();
        } finally {
          cm.shutdown();
        }
        try {
          setupCacheManager();
          cm = testMemoryCapacityChange();
        } finally {
          cm.shutdown();
        }
      }
      barrier1.await();
      if (clientId == 2) {
        verifyNewNode(cacheManager.getCache("dcv2Cache"));
      }
      barrier1.await();
    }

    private void verifyNewNode(final Cache cache) {
      Assert.assertEquals(99, cache.getCacheConfiguration().getTimeToIdleSeconds());
      Assert.assertEquals(99, cache.getCacheConfiguration().getTimeToLiveSeconds());
      Assert.assertEquals(99, cache.getCacheConfiguration().getMaxElementsOnDisk());
      Assert.assertEquals(10000, cache.getCacheConfiguration().getMaxEntriesLocalHeap());
    }

    private CacheManager testTTIChange() throws Throwable {
      barrier2.await();

      cacheManager = getCacheManager();
      final Cache cache = cacheManager.getCache("dcv2Cache");
      cache.getCacheConfiguration().setEternal(false);

      int index = barrier2.await();

      if (index == 0) {
        System.err.println("Changing TTI on Client " + clientId);
        cache.getCacheConfiguration().setTimeToIdleSeconds(99);
      }

      barrier2.await();

      for (int i = 0; i < 60; i++) {
        Thread.sleep(1000);
        if (99 == cache.getCacheConfiguration().getTimeToIdleSeconds()) {
          System.err.println("Change to TTI took " + (i + 1) + " seconds to propagate to Client " + clientId);
          return cacheManager;
        }
      }

      Assert.fail("Change to TTI failed to propagate inside 1 minute");
      return cacheManager;
    }

    private CacheManager testTTLChange() throws Throwable {
      barrier2.await();

      cacheManager = getCacheManager();
      final Cache cache = cacheManager.getCache("dcv2Cache");
      cache.getCacheConfiguration().setEternal(false);

      int index = barrier2.await();

      if (index == 0) {
        System.err.println("Changing TTL on Client " + clientId);
        cache.getCacheConfiguration().setTimeToLiveSeconds(99);
      }

      barrier2.await();

      for (int i = 0; i < 60; i++) {
        Thread.sleep(1000);
        if (99 == cache.getCacheConfiguration().getTimeToLiveSeconds()) {
          System.err.println("Change to TTL took " + (i + 1) + " seconds to propagate to Client " + clientId);
          return cacheManager;
        }
      }

      Assert.fail("Change to TTL failed to propagate inside 1 minute");
      return cacheManager;
    }

    private CacheManager testDiskCapacityChange() throws Throwable {
      barrier2.await();

      cacheManager = getCacheManager();
      final Cache cache = cacheManager.getCache("dcv2Cache");

      int index = barrier2.await();

      if (index == 0) {
        System.err.println("Changing Disk Capacity on Client " + clientId);
        cache.getCacheConfiguration().setMaxElementsOnDisk(99);
      }

      barrier2.await();

      for (int i = 0; i < 60; i++) {
        Thread.sleep(1000);
        if (99 == cache.getCacheConfiguration().getMaxElementsOnDisk()) {
          System.err.println("Change to Disk Capacity took " + (i + 1) + " seconds to propagate to Client " + clientId);
          return cacheManager;
        }
      }

      Assert.fail("Change to Disk Capacity failed to propagate inside 1 minute");
      return cacheManager;
    }

    private CacheManager testMemoryCapacityChange() throws Throwable {
      barrier2.await();

      cacheManager = getCacheManager();
      final Cache cache = cacheManager.getCache("dcv2Cache");

      int index = barrier2.await();

      if (index == 0) {
        System.err.println("Changing Memory Capacity on Client " + clientId);
        cache.getCacheConfiguration().setMaxEntriesLocalHeap(99);
      }

      barrier2.await();

      if (index == 0) {
        Assert.assertEquals("Failed to change max entries local heap.", 99, cache.getCacheConfiguration()
            .getMaxEntriesLocalHeap());
      } else {
        System.out.println("client " + clientId + " is gonna wait for 60 secs...");
        ThreadUtil.reallySleep(60 * 1000);
        Assert.assertEquals("Max entries local heap change propagated to the other client.", 10000, cache
            .getCacheConfiguration().getMaxEntriesLocalHeap());
      }

      return cacheManager;
    }
  }
}
