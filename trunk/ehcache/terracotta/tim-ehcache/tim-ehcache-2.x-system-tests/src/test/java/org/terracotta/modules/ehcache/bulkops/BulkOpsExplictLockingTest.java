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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class BulkOpsExplictLockingTest extends TransparentTestBase {
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

      Cache dcv2Strong = crerateCache("dcv2Strong", cacheManager, "DCV2", Consistency.STRONG);
      testBulkOpsWithExplictLocking(dcv2Strong);
      Cache dcv2Eventual = crerateCache("dcv2Eventual", cacheManager, "DCV2", Consistency.EVENTUAL);
      testBulkOpsWithExplictLocking(dcv2Eventual);

    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier").addRoot("nodes", "nodes");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
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
      assertEquals(numOfElements, cache.getSize());

      Set keySet1 = new HashSet<Key>();
      for (int i = 0; i < numOfElements; i++) {
        keySet1.add(new Key("key" + i, i));
      }

      cache.acquireReadLockOnKey(key);
      Map<Object, Element> rv = cache.getAll(keySet1);
      cache.releaseReadLockOnKey(key);
      assertEquals(numOfElements, rv.size());

      for (Element element : rv.values()) {
        assertTrue(elements.contains(element));
      }

      Collection<Element> values = rv.values();
      for (Element element : elements) {
        assertTrue(values.contains(element));
      }

      Set keySet2 = new HashSet<Key>();
      for (int i = 0; i < numOfElements; i++) {
        if (i % 2 == 0) {
          keySet2.add(new Key("key" + i, i));
        }
      }

      rv = cache.getAll(keySet2);
      assertEquals(keySet2.size(), rv.size());

      for (Element element : rv.values()) {
        assertTrue(elements.contains(element));
      }

      assertEquals(keySet2, rv.keySet());
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

      assertEquals(numOfElements - keySet2.size(), cache.getSize());
      System.out.println("now checking removed <key,value> in " + cache.getName() + " by client");
      for (Object k : keySet2) {
        assertNull(cache.get(k));
      }
      System.out.println("client, I am done with " + cache.getName());
    }

    private Cache crerateCache(String cacheName, CacheManager cacheManager, String storageStrategy,
                               Consistency consistency) {
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
      cacheManager.addCache(cache);
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
