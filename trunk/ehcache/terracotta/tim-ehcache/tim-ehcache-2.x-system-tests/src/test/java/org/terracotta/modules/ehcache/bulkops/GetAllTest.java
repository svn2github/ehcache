/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.bulkops;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class GetAllTest extends TransparentTestBase {
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

      Cache dcv2EventualWithStats = crerateCache("dcv2EventualWithStats", cacheManager, "DCV2", Consistency.EVENTUAL,
                                                 ValueMode.SERIALIZATION);
      dcv2EventualWithStats.setStatisticsEnabled(true);
      testBulkOpsSanity(dcv2EventualWithStats, true);

      Cache dcv2EventualWithoutStats = crerateCache("dcv2EventualWithoutStats", cacheManager, "DCV2",
                                                    Consistency.EVENTUAL, ValueMode.SERIALIZATION);
      dcv2EventualWithoutStats.setStatisticsEnabled(false);
      testBulkOpsSanity(dcv2EventualWithoutStats, true);

      Cache dcv2StrongWithStats = crerateCache("dcv2StrongWithStats", cacheManager, "DCV2", Consistency.STRONG,
                                               ValueMode.SERIALIZATION);
      dcv2StrongWithStats.setStatisticsEnabled(true);
      testBulkOpsSanity(dcv2StrongWithStats, false);

      Cache dcv2StrongIdentityWithStats = crerateCache("dcv2StrongIdentityWithStats", cacheManager, "DCV2",
                                                       Consistency.STRONG, ValueMode.IDENTITY);
      dcv2StrongIdentityWithStats.setStatisticsEnabled(true);
      testBulkOpsSanity(dcv2StrongIdentityWithStats, false);

      Cache dcv2StrongWithoutStats = crerateCache("dcv2StrongWithoutStats", cacheManager, "DCV2", Consistency.STRONG,
                                                  ValueMode.SERIALIZATION);
      dcv2StrongWithoutStats.setStatisticsEnabled(false);
      testBulkOpsSanity(dcv2StrongWithoutStats, false);

      Cache dcv2StrongIdentityWithoutStats = crerateCache("dcv2StrongIdentityWithoutStats", cacheManager, "DCV2",
                                                          Consistency.STRONG, ValueMode.IDENTITY);
      dcv2StrongIdentityWithoutStats.setStatisticsEnabled(false);
      testBulkOpsSanity(dcv2StrongIdentityWithoutStats, false);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier").addRoot("nodes", "nodes");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());

      config.getOrCreateSpec(DummyObject.class.getName());
    }

    private void testBulkOpsSanity(Cache cache, boolean shouldWait) throws InterruptedException, BrokenBarrierException {
      int index = barrier.await();
      int numOfElements = 2000;
      Set<Element> elements = new HashSet<Element>();
      Set<String> vals = new HashSet<String>();
      for (int i = 0; i < numOfElements; i++) {
        elements.add(new Element("key" + i, "val" + i));
        vals.add("val" + i);
      }
      if (index == 0) {
        cache.putAll(elements);
        System.out.println("XXXXX done with putting " + elements.size() + " entries");
      }

      barrier.await();
      if (shouldWait) {
        while (cache.getSize() != numOfElements) {
          Thread.sleep(1000);
        }
      }
      assertEquals(numOfElements, cache.getSize());

      barrier.await();
      Set getKeySet = new HashSet<String>();
      for (int i = 0; i < numOfElements; i++) {
        getKeySet.add("key" + i);
      }

      System.out.println("XXXXXX starting getAll");
      Map<Object, Element> rv = cache.getAll(getKeySet);
      assertEquals(numOfElements, rv.size());

      for (Entry<Object, Element> entry : rv.entrySet()) {
        assertNotNull("val for key " + entry.getKey() + " is null", entry.getValue());
        assertTrue(vals.contains(entry.getValue().getObjectValue()));
      }

      barrier.await();
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
        waitTillRemoveFinished(cache, numOfElements);
      }

      assertEquals(numOfElements - removedKeySet.size(), cache.getSize());

      rv = cache.getAll(getKeySet);
      assertEquals(numOfElements, rv.size());

      for (Entry<Object, Element> entry : rv.entrySet()) {
        String key = (String) entry.getKey();
        int i = Integer.parseInt(key.substring(3));
        if (i % 10 == 0) {
          assertNull("val for " + entry.getKey() + " is not null", entry.getValue());
        } else {
          assertNotNull("val for " + entry.getKey() + " is null", entry.getValue());
          assertTrue(vals.contains(entry.getValue().getObjectValue()));
        }
      }

      System.out.println("XXXXXX done with " + cache.getName());
    }

    private void waitTillRemoveFinished(Cache cache, int numOfElements) throws InterruptedException {
      for (int i = 0; i < numOfElements; i++) {
        if (i % 10 == 0) {
          while (cache.get("key" + i) != null) {
            Thread.sleep(100);
            System.out.println("XXXX val for key" + i + " is not null yet");
          }
        }
      }
    }

    private Cache crerateCache(String cacheName, CacheManager cacheManager, String storageStrategy,
                               Consistency consistency, ValueMode valueMode) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxElementsOnDisk(10000);
      cacheConfiguration.setMaxElementsInMemory(10000);
      cacheConfiguration.setOverflowToDisk(false);
      cacheConfiguration.setEternal(false);
      cacheConfiguration.setTimeToLiveSeconds(100000);
      cacheConfiguration.setTimeToIdleSeconds(200000);
      cacheConfiguration.setDiskPersistent(false);
      cacheConfiguration.setDiskExpiryThreadIntervalSeconds(1);

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
