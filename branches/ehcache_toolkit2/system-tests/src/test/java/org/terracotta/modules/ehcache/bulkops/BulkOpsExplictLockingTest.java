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

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class BulkOpsExplictLockingTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 2;

  public BulkOpsExplictLockingTest(TestConfig testConfig) {
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
      Cache dcv2Strong = crerateCache("dcv2Strong", cacheManager, "DCV2", Consistency.STRONG);
      testBulkOpsWithExplictLocking(dcv2Strong);
      Cache dcv2Eventual = crerateCache("dcv2Eventual", cacheManager, "DCV2", Consistency.EVENTUAL);
      testBulkOpsWithExplictLocking(dcv2Eventual);

    }

    private void testBulkOpsWithExplictLocking(Cache cache) throws InterruptedException, BrokenBarrierException {
      int index = barrier.await();
      int numOfElements = 100;
      Set<Element> elements = new HashSet<Element>();
      for (int i = 0; i < numOfElements; i++) {
        elements.add(new Element(new Key("key" + i, i), new Value("val" + i, i)));
      }
      Key key = new Key("key0", 0);
      if (index == 0) {
        cache.acquireWriteLockOnKey(key);
        cache.putAll(elements);
      }

      barrier.await();
      if (index == 0) {
        Assert.assertTrue(cache.isWriteLockedByCurrentThread(key));
      } else {
        Assert.assertFalse(cache.isWriteLockedByCurrentThread(key));
      }

      if (index == 0) {
        cache.releaseWriteLockOnKey(key);
      }
      Assert.assertFalse(cache.isWriteLockedByCurrentThread(key));
      barrier.await();

      while (cache.getSize() != numOfElements) {
        Thread.sleep(1000);
      }
      Assert.assertEquals(numOfElements, cache.getSize());

      Set keySet1 = new HashSet<Key>();
      for (int i = 0; i < numOfElements; i++) {
        keySet1.add(new Key("key" + i, i));
      }

      cache.acquireReadLockOnKey(key);
      Map<Object, Element> rv = cache.getAll(keySet1);
      cache.releaseReadLockOnKey(key);
      Assert.assertEquals(numOfElements, rv.size());

      for (Element element : rv.values()) {
        Assert.assertTrue(elements.contains(element));
      }

      Collection<Element> values = rv.values();
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

      for (Element element : rv.values()) {
        Assert.assertTrue(elements.contains(element));
      }

      Assert.assertEquals(keySet2, rv.keySet());
      System.out.println("verified <key,value> by client now waiting for others...");
      barrier.await();

      if (index != 0) {
        cache.acquireWriteLockOnKey(key);
        cache.removeAll(keySet2);
        cache.releaseWriteLockOnKey(key);
        System.out.println("removed " + keySet2.size() + " keys from " + cache.getName()
                           + ". Now waiting for others...");
      }
      barrier.await();
      while (cache.getSize() != numOfElements - keySet2.size()) {
        Thread.sleep(1000);
      }

      Assert.assertEquals(numOfElements - keySet2.size(), cache.getSize());
      System.out.println("now checking removed <key,value> in " + cache.getName() + " by client");
      for (Object k : keySet2) {
        Assert.assertNull(cache.get(k));
      }
      System.out.println("client, I am done with " + cache.getName());
    }

    private Cache crerateCache(String cacheName, CacheManager cm, String storageStrategy, Consistency consistency) {
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
      tcConfiguration.setConsistency(Consistency.STRONG);
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cm.addCache(cache);
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

  }
}
