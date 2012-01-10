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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class BulkOpsGenericSanityTest extends TransparentTestBase {

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

      Cache dcv2StrongIdentity = crerateCache("dcv2StrongIdentity", cacheManager, "DCV2", Consistency.STRONG,
                                              "IDENTITY");
      testBulkOpsSanity(dcv2StrongIdentity);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier").addRoot("nodes", "nodes");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());

      config.getOrCreateSpec(DummyObject.class.getName());
    }

    private void testBulkOpsSanity(Cache cache) throws InterruptedException, BrokenBarrierException {
      int index = barrier.await();
      int numOfElements = 10;
      Set<Element> elements = new HashSet<Element>();
      for (int i = 0; i < numOfElements; i++) {
        elements.add(new Element("key" + i, "val" + i, i));
      }
      if (index == 0) {
        cache.putAll(elements);
      }

      barrier.await();
      while (cache.getSize() != numOfElements) {
        Thread.sleep(1000);
      }
      assertEquals(numOfElements, cache.getSize());

      Map<Object, Element> rv = cache.getAll(Arrays.asList("key0", "key1", "key2", "key3", "key4", "key5", "key6",
                                                           "key7", "key8", "key9"));
      assertEquals(numOfElements, rv.size());

      for (Element element : rv.values()) {
        assertTrue(elements.contains(element));
      }

      Collection<Element> values = rv.values();
      for (Element element : elements) {
        assertTrue(values.contains(element));
      }

      rv = cache.getAll(Arrays.asList("key0", "key2", "key4", "key6", "key8"));
      assertEquals(5, rv.size());

      for (Element element : rv.values()) {
        assertTrue(elements.contains(element));
      }

      System.out.println("verified <key,value> by client now waiting for others...");
      barrier.await();

      if (index != 0) {
        cache.removeAll(Arrays.asList("key0", "key2", "key4", "key6", "key8"));
        System.out.println("removed 5 keys from " + cache.getName() + ". Now waiting for others...");
      }
      barrier.await();
      while (cache.getSize() != numOfElements - 5) {
        Thread.sleep(1000);
      }

      assertEquals(numOfElements - 5, cache.getSize());
      System.out.println("now checking removed <key,value> in " + cache.getName() + " by client");

      for (int i = 0; i < numOfElements; i++) {
        if (i % 2 == 0) {
          assertNull(cache.get("key" + i));
        } else {
          assertNotNull("key" + i);
        }
      }
      System.out.println("client, I am done with " + cache.getName());
    }

    private Cache crerateCache(String cacheName, CacheManager cacheManager, String storageStrategy,
                               Consistency consistency, String valueMode) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxElementsInMemory(100000);
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
