/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class LocalReadsGetKeysTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 2;

  public LocalReadsGetKeysTest(TestConfig testConfig) {
    super("local-reads-get-keys-test.xml", testConfig, App.class, App.class);
    String timeout = "120000";
    testConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "true");
    testConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, timeout);
  }

  public static class App extends ClientBase {

    private static final String VALUE = "value";
    private final ToolkitBarrier barrier;

    public App(String[] args) {
      super("dcv2", args);
      this.barrier = getClusteringToolkit().getBarrier("test", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      if (index == 0) {
        cache.put(new Element(new KeyType(1), VALUE));
      }

      barrier.await();

      if (index != 0) {
        readAllKeys(cache);
      }

      barrier.await();

      if (index == 0) {
        cache.put(new Element(new KeyType(2), VALUE));
      }

      barrier.await();
      waitForAllCurrentTransactionsToComplete(clusteringToolkit);

      if (index != 0) {
        // Sleep for some time, so that other client can receive barrier notification before server dies
        TimeUnit.SECONDS.sleep(5L);

        getTestControlMbean().crashActiveServer(0);
        final ClusterInfo clusterInfo = clusteringToolkit.getClusterInfo();
        WaitUtil.waitUntilCallableReturnsFalse(new Callable<Boolean>() {

          @Override
          public Boolean call() throws Exception {
            return clusterInfo.areOperationsEnabled();
          }
        });

        // Attempt non local read
        Assert.assertEquals(null, cache.get(new KeyType(2)));
        // Attempt local read
        Assert.assertEquals(VALUE, cache.get(new KeyType(1)).getObjectValue());
      }
    }

    private void readAllKeys(Cache cache) {
      // iterating the keys will force them to be deserialized (a get() does not currently do that)
      List keys = cache.getKeys();

      for (Object key : keys) {
        // also call get() to fault the value (ie. make it local)
        cache.get(key);
      }
    }

  }

  private static class KeyType implements Serializable {
    private final int val;

    KeyType(int val) {
      this.val = val;
    }

    @Override
    public int hashCode() {
      return val;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null || obj.getClass() != getClass()) { return false; }
      return val == ((KeyType) obj).val;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName() + "(" + val + ")";
    }
  }

}
