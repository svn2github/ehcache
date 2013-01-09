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

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

import java.util.concurrent.Callable;

import junit.framework.Assert;

public class CachePinningFaultInvalidatedEntriesTest extends AbstractCacheTestBase {

  private static final int ELEMENT_COUNT = 1000;

  public CachePinningFaultInvalidatedEntriesTest(TestConfig testConfig) {
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

      System.out.println("Testing with Eventual tier pinned cache");
      basicTestInvalidatedEntriesFaulted(cacheManager.getCache("pinnedEventual"), true);
      System.out.println("Testing with Strong tier pinned cache");
      basicTestInvalidatedEntriesFaulted(cacheManager.getCache("pinned"), true);

      System.out.println("Testing with Eventual element pinned cache");
      basicTestInvalidatedEntriesFaulted(cacheManager.getCache("pinnedElementEventual"), false);
      System.out.println("Testing with Strong element pinned cache");
      basicTestInvalidatedEntriesFaulted(cacheManager.getCache("pinnedElementStrong"), false);

    }

    private void basicTestInvalidatedEntriesFaulted(final Cache cache, boolean tierPinned) throws Exception {
      int index = getBarrierForAllClients().await();
      if (index == 0) {
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          if (!tierPinned) {
            cache.setPinned(getKey(i), true);
          }
          cache.put(new Element(getKey(i), getValue(i)));
          if (!tierPinned) {
            Assert.assertTrue(cache.isPinned(getKey(i)));
          }
        }
        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          assertNotNull(cache.get(getKey(i)));
          assertEquals(cache.get(getKey(i)).getValue(), getValue(i));
        }
        // All the gets on both the clients should be local as the pinned entries would have been faulted.
//        Assert.assertEquals(0, cache.getStatistics().remoteHitCount());
//        Assert.assertEquals(0, cache.getStatistics().remoteMissCount());
        Assert.assertEquals(0, cache.getStatistics().cacheEvictedCount());
      }
      getBarrierForAllClients().await();
      if (index == 1) {
        // Update elements, invalidate entries on other client
        for (int i = 0; i < ELEMENT_COUNT; i++) {
          if (!tierPinned) {
            cache.setPinned(getKey(i), true);
          }
          cache.put(new Element(getKey(i), getValue(i + 1)));
          if (!tierPinned) {
            Assert.assertTrue(cache.isPinned(getKey(i)));
          }
        }
        Assert.assertEquals(ELEMENT_COUNT, cache.getSize());
      }

      getBarrierForAllClients().await();
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          long localsize = cache.getStatistics().getLocalHeapSize();
          System.out.println("Local cache Size = " + localsize);
          return localsize == ELEMENT_COUNT;
        }
      });
      getBarrierForAllClients().await();
      for (int i = 0; i < ELEMENT_COUNT; i++) {
        assertNotNull(cache.get(getKey(i)));
        assertEquals(cache.get(getKey(i)).getValue(), getValue(i + 1));
      }
      // All the gets on both the clients should be local as the pinned entries would have been faulted.
      if (index == 0) {
//        Assert.assertEquals(0, cache.getStatistics().remoteHitCount());
//        Assert.assertEquals(0, cache.getStatistics().remoteMissCount());
        Assert.assertEquals(0, cache.getStatistics().cacheEvictedCount());
      }
      getBarrierForAllClients().await();
    }

    private Object getKey(int i) {
      return String.valueOf(i);
    }

    private Object getValue(int i) {
      return i;
    }

  }
}
