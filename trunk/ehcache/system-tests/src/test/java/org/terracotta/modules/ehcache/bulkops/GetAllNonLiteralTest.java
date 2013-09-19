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

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class GetAllNonLiteralTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 2;

  public GetAllNonLiteralTest(TestConfig testConfig) {
    super(testConfig, App.class, App.class);
  }

  public static class App extends ClientBase {
    private final ToolkitBarrier barrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      Cache dcv2StrongSerialization = createCache("dcv2StrongSerialization", cacheManager, Consistency.STRONG);
      testBulkOpsSanity(dcv2StrongSerialization, false);
    }

    private void testBulkOpsSanity(Cache cache, boolean shouldWait) throws InterruptedException, BrokenBarrierException {
      int index = barrier.await();
      final int numOfElements = 2000;
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
      Set getKeySet = new HashSet<DummyObject>();
      for (int i = 0; i < numOfElements; i++) {
        getKeySet.add(new DummyObject("key" + i, i));
      }

      System.out.println("XXXXXX starting getAll");
      Map<Object, Element> rv = cache.getAll(getKeySet);
      Assert.assertEquals(numOfElements, rv.size());

      for (Entry<Object, Element> entry : rv.entrySet()) {
        Assert.assertNotNull("val for key " + entry.getKey() + " is null", entry.getValue());
        Assert.assertTrue(vals.contains(entry.getValue().getObjectValue()));
      }

      barrier.await();
      if (shouldWait) {
        while (cache.getSize() != numOfElements) {
          Thread.sleep(1000);
        }
      }
      Assert.assertEquals(numOfElements, cache.getSize());

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
      Assert.assertEquals(numOfElements, rv.size());

      for (Entry<Object, Element> entry : rv.entrySet()) {
        DummyObject key = (DummyObject) entry.getKey();
        if (key.getIntPart() % 10 == 0) {
          Assert.assertNull("val for " + entry.getKey() + " is not null", entry.getValue());
        } else {
          Assert.assertNotNull("val for " + entry.getKey() + " is null", entry.getValue());
          Assert.assertTrue(vals.contains(entry.getValue().getObjectValue()));
        }
      }

      System.out.println("XXXXX done with " + cache.getName());
    }

    private Cache createCache(String cacheName, CacheManager cm, Consistency consistency) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxEntriesInCache(1000000);
      cacheConfiguration.setMaxElementsInMemory(1000000);
      cacheConfiguration.setEternal(false);
      cacheConfiguration.setTimeToLiveSeconds(100000);
      cacheConfiguration.setTimeToIdleSeconds(200000);

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setConsistency(consistency);
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cm.addCache(cache);
      return cache;
    }
  }
}
