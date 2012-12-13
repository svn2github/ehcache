/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.terracotta.AbstractTerracottaActivePassiveTestBase;

import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

import java.io.Serializable;
import java.util.List;

import junit.framework.Assert;

public class LocalReadsGetKeysTest extends AbstractTerracottaActivePassiveTestBase {

  private static final int NODE_COUNT = 2;

  public LocalReadsGetKeysTest(TestConfig testConfig) {
    super("local-reads-get-keys-test.xml", testConfig, App.class, App.class);
    String timeout = "120000";
    testConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    testConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, timeout);
    timebombTest("2012-12-30");
  }

  public static class App extends ClientBase {

    private final ToolkitBarrier barrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      Cache[] caches = allCaches(cacheManager);

      if (index == 0) {
        loadKeyType1(caches, 0);
      }

      barrier.await();

      if (index != 0) {
        readAllKeys(caches);
      }

      barrier.await();

      if (index == 0) {
        loadKeyType1(caches, 1);
        loadKeyType2(caches);
      }

      barrier.await();

      if (index != 0) {
        getTestControlMbean().crashActiveServer(0);

        try {
          attemptNonLocalRead(caches);
          testGetKeysMethods(caches);
        } finally {
          getTestControlMbean().restartCrashedServer(0, 0);
        }
      }
    }

    private void readAllKeys(Cache[] caches) {
      for (Cache c : caches) {
        // iterating the keys will force them to be deserialized (a get() does not currently do that)

        List keys = c.getKeys();
        Assert.assertEquals(1, keys.size());

        for (Object key : keys) {
          // also call get() to fault the value (ie. make it local)
          c.get(key);
        }
      }
    }

    //
    private static Cache[] allCaches(CacheManager cacheManager) {
      int i = 0;
      Cache[] caches = new Cache[cacheManager.getCacheNames().length];
      for (String name : cacheManager.getCacheNames()) {
        caches[i++] = cacheManager.getCache(name);
      }

      return caches;
    }

    private void testGetKeysMethods(Cache[] caches) {
      for (Cache c : caches) {
        String name = c.getName();
        if (!name.equals("dcv2") && !name.equals("classic")) { throw new AssertionError(name); }

        verifyKeys(name, c.getKeys(), "classic".equals(name) ? 2 : 1);
        verifyKeys(name, c.getKeysWithExpiryCheck(), 1);
      }
    }

    private void verifyKeys(String name, List keys, int expect) {
      Assert.assertEquals(name, expect, keys.size());
      for (Object key : keys) {
        Assert.assertEquals(name, KeyType1.class, key.getClass());
      }
    }

    private void attemptNonLocalRead(Cache[] caches) {
      // should return null since server is down and no local data is present
      for (Cache c : caches) {
        Assert.assertEquals(null, c.get(new KeyType1(1)));
      }
    }

    private void loadKeyType1(Cache[] caches, int val) {
      for (Cache c : caches) {
        c.put(new Element(new KeyType1(val), "value"));
      }
    }

    private void loadKeyType2(Cache[] caches) {
      for (Cache c : caches) {
        c.put(new Element(new KeyType2(0), "value"));
      }
    }

  }

  private static class KeyBase implements Serializable {
    private final int val;

    KeyBase(int val) {
      this.val = val;
    }

    @Override
    public int hashCode() {
      return val;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || obj.getClass() != getClass()) { return false; }
      return val == ((KeyBase) obj).val;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "(" + val + ")";
    }
  }

  private static class KeyType1 extends KeyBase {
    KeyType1(int val) {
      super(val);
    }
  }

  private static class KeyType2 extends KeyBase {
    KeyType2(int val) {
      super(val);
    }
  }
}
