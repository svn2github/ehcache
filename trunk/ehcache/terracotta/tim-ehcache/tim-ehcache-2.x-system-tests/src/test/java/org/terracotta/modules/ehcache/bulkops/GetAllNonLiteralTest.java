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

public class GetAllNonLiteralTest extends TransparentTestBase {
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
      Cache dcv2StrongSerialization = crerateCache("dcv2StrongSerialization", cacheManager, "DCV2", Consistency.STRONG,
                                                   "SERIALIZATION");
      dcv2StrongSerialization.setStatisticsEnabled(false);
      testBulkOpsSanity(dcv2StrongSerialization, false);
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
      Set<DummyObject> vals = new HashSet<DummyObject>();
      for (int i = 0; i < numOfElements; i++) {
        elements.add(new Element(new DummyObject("key" + i, i), new DummyObject("val" + i, i)));
        vals.add(new DummyObject("val" + i, i));
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
      Set getKeySet = new HashSet<DummyObject>();
      for (int i = 0; i < numOfElements; i++) {
        getKeySet.add(new DummyObject("key" + i, i));
      }

      System.out.println("XXXXXX starting getAll");
      Map<Object, Element> rv = cache.getAll(getKeySet);
      assertEquals(numOfElements, rv.size());

      for (Entry<Object, Element> entry : rv.entrySet()) {
        assertNotNull("val for key " + entry.getKey() + " is null", entry.getValue());
        assertTrue(vals.contains(entry.getValue().getObjectValue()));
      }

      barrier.await();
      Set removedKeySet = new HashSet<DummyObject>();
      for (int i = 0; i < numOfElements; i++) {
        if (i % 10 == 0) {
          removedKeySet.add(new DummyObject("key" + i, i));
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

      rv = cache.getAll(getKeySet);
      assertEquals(numOfElements, rv.size());

      for (Entry<Object, Element> entry : rv.entrySet()) {
        DummyObject key = (DummyObject) entry.getKey();
        if (key.getIntPart() % 10 == 0) {
          assertNull("val for " + entry.getKey() + " is not null", entry.getValue());
        } else {
          assertNotNull("val for " + entry.getKey() + " is null", entry.getValue());
          assertTrue(vals.contains(entry.getValue().getObjectValue()));
        }
      }

      System.out.println("XXXXX done with " + cache.getName());
    }

    private Cache crerateCache(String cacheName, CacheManager cacheManager, String storageStrategy,
                               Consistency consistency, String valueMode) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxElementsOnDisk(1000000);
      cacheConfiguration.setMaxElementsInMemory(1000000);
      cacheConfiguration.setOverflowToDisk(false);
      cacheConfiguration.setEternal(false);
      cacheConfiguration.setTimeToLiveSeconds(100000);
      cacheConfiguration.setTimeToIdleSeconds(200000);
      cacheConfiguration.setDiskPersistent(false);
      cacheConfiguration.setDiskExpiryThreadIntervalSeconds(1);

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setStorageStrategy(storageStrategy);
      tcConfiguration.setConsistency(consistency);
      tcConfiguration.setValueMode(valueMode);
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cacheManager.addCache(cache);
      return cache;
    }
  }
}
