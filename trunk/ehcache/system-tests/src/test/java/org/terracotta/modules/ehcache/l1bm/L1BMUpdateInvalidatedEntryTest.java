/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.terracotta.AbstractTerracottaActivePassiveTestBase;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.object.ClientConfigurationContext;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.TestConfig;
import com.tc.util.CallableWaiter;

import java.util.concurrent.Callable;

import junit.framework.Assert;

public class L1BMUpdateInvalidatedEntryTest extends AbstractTerracottaActivePassiveTestBase {
  {
    TCPropertiesImpl.getProperties().setProperty("seda." + ClientConfigurationContext.RECEIVE_INVALIDATE_OBJECTS_STAGE
                                                     + ".sleepMs", "10000");
  }

  private static final int NODE_COUNT = 2;

  public L1BMUpdateInvalidatedEntryTest(TestConfig testConfig) {
    super("l1bm-update-invalidated-entry-test.xml", testConfig, App.class, App.class);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
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
      final Cache c = cacheManager.getCache("test");
      int nodeId = barrier.await();

      if (nodeId == 0) {
        info(nodeId, "Populating the cache.");
        for (int i = 0; i < 100; i++) {
          c.put(new Element("key-" + i, "value"));
        }
      }

      barrier.await();
      info(nodeId, "Getting the elements once.");
      for (int i = 0; i < 100; i++) {
        Assert.assertNotNull(c.get("key-" + i));
      }

      barrier.await();
      if (nodeId == 1) {
        info(nodeId, "Removing elements.");
        for (int i = 0; i < 100; i++) {
          // On slow machines, it's possible that the elements could have been expired
          // already. That means we can't actually assert true for this remove.
          c.remove("key-" + i);
        }
      }

      barrier.await();
      if (nodeId == 0) {
        Thread.sleep(10 * 1000); // Give the invalidations a bit of time to show up.
        info(nodeId, "Accessing elements until invalidations come in.");
        CallableWaiter.waitOnCallable(new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            for (int i = 0; i < 100; i++) {
              if (c.get("key-" + i) != null) { return false; }
            }
            return true;
          }
        });
      }

      barrier.await();
    }

    private void info(int nodeId, String msg) {
      System.out.println("Node[" + nodeId + "] " + msg);
    }

  }

}
