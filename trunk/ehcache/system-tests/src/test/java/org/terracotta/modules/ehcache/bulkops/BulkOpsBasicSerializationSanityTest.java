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
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;

public class BulkOpsBasicSerializationSanityTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 2;

  public BulkOpsBasicSerializationSanityTest(TestConfig testConfig) {
    super(testConfig, App.class, App.class);
  }

  public static class App extends ClientBase {
    private final Barrier barrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      Cache dcv2StrongSerialization = crerateCache("dcv2StrongSerialization", cacheManager, Consistency.STRONG,
                                                   "SERIALIZATION");
      testBulkOpsSanity(dcv2StrongSerialization);
      barrier.await();

      Cache dcv2EventualSerialization = crerateCache("dcv2EventualSerialization", cacheManager,
                                                     Consistency.EVENTUAL, "SERIALIZATION");
      testBulkOpsSanity(dcv2EventualSerialization);
      barrier.await();

      barrier.await();

    }

    private void testBulkOpsSanity(Cache cache) throws InterruptedException, BrokenBarrierException {
      int index = barrier.await();
      int numOfElements = 100;
      Set<Element> elements = new HashSet<Element>();
      for (int i = 0; i < numOfElements; i++) {
        elements.add(new Element(new Key("key" + i, i), new Value("val" + i, i)));
      }
      if (index == 0) {
        cache.putAll(elements);
      }

      barrier.await();
      while (cache.getSize() != numOfElements) {
        Thread.sleep(1000);
      }
      Assert.assertEquals(numOfElements, cache.getSize());

      Set keySet1 = new HashSet<Key>();
      for (int i = 0; i < numOfElements; i++) {
        keySet1.add(new Key("key" + i, i));
      }

      Map<Object, Element> rv = cache.getAll(keySet1);
      Assert.assertEquals(numOfElements, rv.size());

      Collection<Element> values = new HashSet<Element>();
      for (Entry<Object, Element> entry : rv.entrySet()) {
        Assert.assertTrue(elements.contains(entry.getValue()));
        values.add(entry.getValue());
      }

      for (Element element : elements) {
        Assert.assertTrue(values.contains(element));
      }

      Set keySet2 = new HashSet<Key>();
      for (int i = 0; i < numOfElements; i++) {
        if (i % 2 == 0) {
          keySet2.add(new Key("key" + i, i));
        }
      }

      rv = cache.getAll(keySet2);
      Assert.assertEquals(keySet2.size(), rv.size());

      for (Entry<Object, Element> entry : rv.entrySet()) {
        Assert.assertTrue(elements.contains(entry.getValue()));
      }

      Assert.assertEquals(keySet2, rv.keySet());
      System.out.println("verified <key,value> by client now waiting for others...");
      barrier.await();

      if (index != 0) {
        cache.removeAll(keySet2);
        System.out.println("client " + index + " removed " + keySet2.size() + " keys from " + cache.getName()
                           + ". Now waiting for others...");
        // sleep for 60 seconds for eventual caches
        if (cache.getCacheConfiguration().getTerracottaConfiguration().getConsistency() == Consistency.EVENTUAL) {
          Thread.sleep(TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS));
        }
      }
      index = barrier.await();
      while (cache.getSize() != numOfElements - keySet2.size()) {
        Thread.sleep(1000);
      }

      Assert.assertEquals(numOfElements - keySet2.size(), cache.getSize());
      System.out.println("client " + index + "now checking removed <key,value> in " + cache.getName() + " by client");
      for (Object key : keySet2) {
        Assert.assertNull(cache.get(key));
      }
      System.out.println("client " + index + " done with " + cache.getName());
    }

    private Cache crerateCache(String cacheName, CacheManager cm, Consistency consistency,
                               String valueMode) {
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
      tcConfiguration.setConsistency(consistency);
      tcConfiguration.setValueMode(valueMode);
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cm.addCache(cache);
      System.out.println("\n\ncache " + cacheName + " created with consistency " + consistency);
      return cache;
    }
  }

  private static class Key implements Serializable {
    private final String stringKey;
    private final int    intKey;

    public Key(String stringKey, int intKey) {
      super();
      this.stringKey = stringKey;
      this.intKey = intKey;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + intKey;
      result = prime * result + ((stringKey == null) ? 0 : stringKey.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Key other = (Key) obj;
      if (intKey != other.intKey) return false;
      if (stringKey == null) {
        if (other.stringKey != null) return false;
      } else if (!stringKey.equals(other.stringKey)) return false;
      return true;
    }

    @Override
    public String toString() {
      return stringKey;
    }

  }

  private static class Value implements Serializable {
    private final String keyVal;
    private final int    intVal;

    public Value(String keyVal, int intVal) {
      this.keyVal = keyVal;
      this.intVal = intVal;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + intVal;
      result = prime * result + ((keyVal == null) ? 0 : keyVal.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Value other = (Value) obj;
      if (intVal != other.intVal) return false;
      if (keyVal == null) {
        if (other.keyVal != null) return false;
      } else if (!keyVal.equals(other.keyVal)) return false;
      return true;
    }

    @Override
    public String toString() {
      return keyVal;
    }

  }
}
