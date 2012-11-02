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

public class GetAllTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 2;

  public GetAllTest(TestConfig testConfig) {
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
      Cache dcv2EventualWithStats = createCache("dcv2EventualWithStats", cacheManager, Consistency.EVENTUAL,
                                                 ValueMode.SERIALIZATION);
      dcv2EventualWithStats.setStatisticsEnabled(true);
      testBulkOpsSanity(dcv2EventualWithStats, true);

      Cache dcv2EventualWithoutStats = createCache("dcv2EventualWithoutStats", cacheManager,
                                                    Consistency.EVENTUAL, ValueMode.SERIALIZATION);
      dcv2EventualWithoutStats.setStatisticsEnabled(false);
      testBulkOpsSanity(dcv2EventualWithoutStats, true);

      Cache dcv2StrongWithStats = createCache("dcv2StrongWithStats", cacheManager, Consistency.STRONG,
                                               ValueMode.SERIALIZATION);
      dcv2StrongWithStats.setStatisticsEnabled(true);
      testBulkOpsSanity(dcv2StrongWithStats, false);

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
      Assert.assertEquals(numOfElements, cache.getSize());

      barrier.await();
      Set getKeySet = new HashSet<String>();
      for (int i = 0; i < numOfElements; i++) {
        getKeySet.add("key" + i);
      }

      System.out.println("XXXXXX starting getAll");
      Map<Object, Element> rv = cache.getAll(getKeySet);
      Assert.assertEquals(numOfElements, rv.size());

      for (Entry<Object, Element> entry : rv.entrySet()) {
        Assert.assertNotNull("val for key " + entry.getKey() + " is null", entry.getValue());
        Assert.assertTrue(vals.contains(entry.getValue().getObjectValue()));
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

      Assert.assertEquals(numOfElements - removedKeySet.size(), cache.getSize());

      rv = cache.getAll(getKeySet);
      Assert.assertEquals(numOfElements, rv.size());

      for (Entry<Object, Element> entry : rv.entrySet()) {
        String key = (String) entry.getKey();
        int i = Integer.parseInt(key.substring(3));
        if (i % 10 == 0) {
          Assert.assertNull("val for " + entry.getKey() + " is not null", entry.getValue());
        } else {
          Assert.assertNotNull("val for " + entry.getKey() + " is null", entry.getValue());
          Assert.assertTrue(vals.contains(entry.getValue().getObjectValue()));
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

    private Cache createCache(String cacheName, CacheManager cm, Consistency consistency,
                               ValueMode valueMode) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxEntriesInCache(10000);
      cacheConfiguration.setMaxElementsInMemory(10000);
      cacheConfiguration.setEternal(false);
      cacheConfiguration.setTimeToLiveSeconds(100000);
      cacheConfiguration.setTimeToIdleSeconds(200000);

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setConsistency(consistency);
      tcConfiguration.setValueMode(valueMode.name());
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cm.addCache(cache);
      return cache;
    }
  }
}
