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

import org.junit.Assert;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

public class BulkOpsGenericSanityTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 2;

  public BulkOpsGenericSanityTest(TestConfig testConfig) {
    super(testConfig, BulkOpsGenericSanityTestClient.class, BulkOpsGenericSanityTestClient.class);
  }

  public static class BulkOpsGenericSanityTestClient extends ClientBase {
    private ToolkitBarrier barrier;

    public BulkOpsGenericSanityTestClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new BulkOpsGenericSanityTestClient(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      this.barrier = clusteringToolkit.getBarrier("test-barrier", NODE_COUNT);

      Cache dcv2StrongIdentity = createCache("dcv2StrongIdentity", cacheManager, Consistency.STRONG,
                                              "SERIALIZATION");
      testBulkOpsSanity(dcv2StrongIdentity);
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
      Assert.assertEquals(numOfElements, cache.getSize());

      Map<Object, Element> rv = cache.getAll(Arrays.asList("key0", "key1", "key2", "key3", "key4", "key5", "key6",
                                                           "key7", "key8", "key9"));
      Assert.assertEquals(numOfElements, rv.size());

      for (Element element : rv.values()) {
        Assert.assertTrue(elements.contains(element));
      }

      Collection<Element> values = rv.values();
      for (Element element : elements) {
        Assert.assertTrue(values.contains(element));
      }

      rv = cache.getAll(Arrays.asList("key0", "key2", "key4", "key6", "key8"));
      Assert.assertEquals(5, rv.size());

      for (Element element : rv.values()) {
        Assert.assertTrue(elements.contains(element));
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

      Assert.assertEquals(numOfElements - 5, cache.getSize());
      System.out.println("now checking removed <key,value> in " + cache.getName() + " by client");

      for (int i = 0; i < numOfElements; i++) {
        if (i % 2 == 0) {
          Assert.assertNull(cache.get("key" + i));
        } else {
          Assert.assertNotNull("key" + i);
        }
      }
      System.out.println("client, I am done with " + cache.getName());
    }

    private Cache createCache(String cacheName, CacheManager cm, Consistency consistency,
                               String valueMode) {
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setMaxElementsInMemory(100000);
      cacheConfiguration.setEternal(false);
      cacheConfiguration.setTimeToLiveSeconds(100000);
      cacheConfiguration.setTimeToIdleSeconds(200000);

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setConsistency(consistency);
      tcConfiguration.setValueMode(valueMode);
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cm.addCache(cache);
      return cache;
    }
  }
}
