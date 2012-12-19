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

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

public class L1BMOnHeapWithTTLSanityTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 2;

  public L1BMOnHeapWithTTLSanityTest(TestConfig testConfig) {
    super(testConfig, App.class, App.class);
  }

  public static class App extends ClientBase {
    private final ToolkitBarrier barrier;

    private int                  index;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {

      Cache dcv2EventualSerializationWithStats = createCache("dcv2EventualSerializationWithStats", cacheManager,
                                                              Consistency.EVENTUAL, ValueMode.SERIALIZATION);
      dcv2EventualSerializationWithStats.getStatistics().setStatisticsEnabled(true);
      testL1BigMemorySanity(dcv2EventualSerializationWithStats, true);
      dcv2EventualSerializationWithStats.removeAll();

      Cache dcv2EventualSerializationWithoutStats = createCache("dcv2EventualSerializationWithoutStats", cacheManager,
                                                                 Consistency.EVENTUAL, ValueMode.SERIALIZATION);
      dcv2EventualSerializationWithoutStats.getStatistics().setStatisticsEnabled(false);
      testL1BigMemorySanity(dcv2EventualSerializationWithoutStats, true);
      dcv2EventualSerializationWithoutStats.removeAll();

      Cache dcv2StrongSerializationWithStats = createCache("dcv2StrongSerializationWithStats", cacheManager,
                                                            Consistency.STRONG, ValueMode.SERIALIZATION);
      dcv2StrongSerializationWithStats.getStatistics().setStatisticsEnabled(true);
      testL1BigMemorySanity(dcv2StrongSerializationWithStats, false);
      dcv2StrongSerializationWithStats.removeAll();

      Cache dcv2StrongWithoutStats = createCache("dcv2StrongWithoutStats", cacheManager, Consistency.STRONG,
                                                  ValueMode.SERIALIZATION);
      dcv2StrongWithoutStats.getStatistics().setStatisticsEnabled(false);
      testL1BigMemorySanity(dcv2StrongWithoutStats, false);
      dcv2StrongWithoutStats.removeAll();
    }

    private void testL1BigMemorySanity(Cache cache, boolean shouldWait) throws Exception {
      index = barrier.await();
      int numOfElements = 500;
      if (index == 0) {
        for (int i = 0; i < numOfElements; i++) {
          cache.put(new Element("key" + i, "val" + i));
        }
        println("done with putting " + cache.getSize() + " entries");
      }
      barrier.await();
      if (shouldWait) {
        while (cache.getSize() != numOfElements) {
          Thread.sleep(1000);
        }
      }
      Assert.assertEquals(numOfElements, cache.getSize());
      println("client " + index + " cache size: " + cache.getSize() + " local: "
                         + cache.getStatistics().getLocalHeapSize());
      if (index == 0) {
        Assert.assertTrue(cache.getStatistics().getLocalHeapSize() > 0);
      } else {
        Assert.assertEquals(0, cache.getStatistics().getLocalHeapSize());
      }

      barrier.await();

      println("testing get");
      for (int i = 0; i < numOfElements; i++) {
        Assert.assertNotNull("value for key" + i + " is null", cache.get("key" + i));
      }
      Assert.assertTrue(cache.getStatistics().getLocalHeapSize() > 0);

      barrier.await();
      println("done with basic get, now removing random entries...");
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
        waitForAllCurrentTransactionsToComplete(cache);
      }
      barrier.await();

      println("removed " + removedKeySet.size() + " elemets. Cache size: " + cache.getSize());

      println("testing get after remove.");
      for (int i = 0; i < numOfElements; i++) {
        String key = "key" + i;
        if (removedKeySet.contains(key)) {
          Assert.assertNull(nodeId() + " value for " + key + " is not null", cache.get(key));
        } else {
          Assert.assertNotNull(nodeId() + " value for " + key + " is null", cache.get(key));
        }
      }

      // wait for 30 secs to get element expired
      println("waiting fo elements to get expired...");
      Thread.sleep(30000);
      println("All elements should be null after expiration...");
      for (int i = 0; i < numOfElements; i++) {
        Assert.assertNull(nodeId() + " value for key" + i + " is not null", cache.get("key" + i));
      }
      println("done with " + cache.getName());
    }

    private void println(String msg) {
      System.out.println(nodeId() + " " + msg);
    }

    private String nodeId() {
      return "Node[" + index + "]";
    }

    private Cache createCache(String cacheName, CacheManager cm, Consistency consistency,
                               ValueMode valueMode) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setTimeToLiveSeconds(30);
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxBytesLocalHeap(409600L);

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
