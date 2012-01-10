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
import org.terracotta.modules.ehcache.bulkops.DummyObject;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class L1BMOnHeapWithTTLSanityTest extends TransparentTestBase {
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
    private final CyclicBarrier barrier = new CyclicBarrier(NODE_COUNT);

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      CacheManager cacheManager = CacheManager.create();

      Cache dcv2EventualSerializationWithStats = crerateCache("dcv2EventualSerializationWithStats", cacheManager,
                                                              "DCV2", Consistency.EVENTUAL, ValueMode.SERIALIZATION);
      dcv2EventualSerializationWithStats.setStatisticsEnabled(true);
      testL1BigMemorySanity(dcv2EventualSerializationWithStats, true);

      Cache dcv2EventualIdentityWithStats = crerateCache("dcv2EventualIdentityWithStats", cacheManager, "DCV2",
                                                         Consistency.EVENTUAL, ValueMode.IDENTITY);
      dcv2EventualIdentityWithStats.setStatisticsEnabled(true);
      testL1BigMemorySanity(dcv2EventualIdentityWithStats, true);

      Cache dcv2EventualSerializationWithoutStats = crerateCache("dcv2EventualSerializationWithoutStats", cacheManager,
                                                                 "DCV2", Consistency.EVENTUAL, ValueMode.SERIALIZATION);
      dcv2EventualSerializationWithoutStats.setStatisticsEnabled(false);
      testL1BigMemorySanity(dcv2EventualSerializationWithoutStats, true);

      Cache dcv2EventualIdentityWithoutStats = crerateCache("dcv2EventualIdentityWithoutStats", cacheManager, "DCV2",
                                                            Consistency.EVENTUAL, ValueMode.IDENTITY);
      dcv2EventualIdentityWithoutStats.setStatisticsEnabled(false);
      testL1BigMemorySanity(dcv2EventualIdentityWithoutStats, true);

      Cache dcv2StrongSerializationWithStats = crerateCache("dcv2StrongSerializationWithStats", cacheManager, "DCV2",
                                                            Consistency.STRONG, ValueMode.SERIALIZATION);
      dcv2StrongSerializationWithStats.setStatisticsEnabled(true);
      testL1BigMemorySanity(dcv2StrongSerializationWithStats, false);

      Cache dcv2StrongIdentityWithStats = crerateCache("dcv2StrongIdentityWithStats", cacheManager, "DCV2",
                                                       Consistency.STRONG, ValueMode.IDENTITY);
      dcv2StrongIdentityWithStats.setStatisticsEnabled(true);
      testL1BigMemorySanity(dcv2StrongIdentityWithStats, false);

      Cache dcv2StrongWithoutStats = crerateCache("dcv2StrongWithoutStats", cacheManager, "DCV2", Consistency.STRONG,
                                                  ValueMode.SERIALIZATION);
      dcv2StrongWithoutStats.setStatisticsEnabled(false);
      testL1BigMemorySanity(dcv2StrongWithoutStats, false);

      Cache dcv2StrongIdentityWithoutStats = crerateCache("dcv2StrongIdentityWithoutStats", cacheManager, "DCV2",
                                                          Consistency.STRONG, ValueMode.IDENTITY);
      dcv2StrongIdentityWithoutStats.setStatisticsEnabled(false);
      testL1BigMemorySanity(dcv2StrongIdentityWithoutStats, false);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier").addRoot("nodes", "nodes");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());

      config.getOrCreateSpec(DummyObject.class.getName());
    }

    private void testL1BigMemorySanity(Cache cache, boolean shouldWait) throws InterruptedException,
        BrokenBarrierException {
      int index = barrier.await();
      int numOfElements = 500;
      if (index == 0) {
        for (int i = 0; i < numOfElements; i++) {
          cache.put(new Element("key" + i, "val" + i));
        }
        System.out.println("XXXXX done with putting " + cache.getSize() + " entries");
      }
      barrier.await();
      if (shouldWait) {
        while (cache.getSize() != numOfElements) {
          Thread.sleep(1000);
        }
      }
      assertEquals(numOfElements, cache.getSize());
      System.out.println("XXXXXX client " + index + " cache size: " + cache.getSize() + " local: "
                         + cache.getMemoryStoreSize());
      if (index == 0) {
        assertTrue(cache.getMemoryStoreSize() > 0);
      } else {
        assertEquals(0, cache.getMemoryStoreSize());
      }

      barrier.await();

      System.out.println("XXXXXX testing get");
      for (int i = 0; i < numOfElements; i++) {
        assertNotNull("value for key" + i + " is null", cache.get("key" + i));
      }
      assertTrue(cache.getMemoryStoreSize() > 0);

      barrier.await();
      System.out.println("XXXX done with basic get, now removing random entries...");
      Set removedKeySet = new HashSet<String>();
      for (int i = 0; i < numOfElements; i++) {
        if (i % 10 == 0) {
          removedKeySet.add("key" + i);
        }
      }

      if (index == 0) {
        cache.removeAll(removedKeySet);
      }

      barrier.await();
      if (shouldWait) {
        while (cache.getSize() != numOfElements - removedKeySet.size()) {
          Thread.sleep(1000);
        }
      }
      System.out.println("XXXXX removed " + removedKeySet.size() + " elemets. Cache size: " + cache.getSize());

      System.out.println("XXXX testing get after remove.");
      for (int i = 0; i < numOfElements; i++) {
        String key = "key" + i;
        if (removedKeySet.contains(key)) {
          assertNull("value for " + key + " is not null", cache.get(key));
        } else {
          assertNotNull("value for " + key + " is null", cache.get(key));
        }
      }

      // wait for 90 secs to get element expired
      System.out.println("waiting fo elements to get expired...");
      Thread.sleep(30000);
      System.out.println("All elements should be null after expiration...");
      for (int i = 0; i < numOfElements; i++) {
        assertNull("value for key" + i + " is not null", cache.get("key" + i));
      }
      System.out.println("XXXXXX done with " + cache.getName());
    }

    private Cache crerateCache(String cacheName, CacheManager cacheManager, String storageStrategy,
                               Consistency consistency, ValueMode valueMode) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setTimeToLiveSeconds(30);
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxBytesLocalHeap(409600L);

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setStorageStrategy(storageStrategy);
      tcConfiguration.setConsistency(consistency);
      tcConfiguration.setValueMode(valueMode.name());
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cacheManager.addCache(cache);
      return cache;
    }
  }
}
