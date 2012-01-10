/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.ClientConfigurationContext;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.properties.TCPropertiesImpl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.CallableWaiter;
import com.tctest.ActivePassiveTransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

public class L1BMUpdateInvalidatedEntryTest extends ActivePassiveTransparentTestBase {
  {
    TCPropertiesImpl.getProperties().setProperty("seda." + ClientConfigurationContext.RECEIVE_INVALIDATE_OBJECTS_STAGE
                                                     + ".sleepMs", "10000");
  }

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
    private static final CyclicBarrier barrier = new CyclicBarrier(NODE_COUNT);

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }

    @Override
    protected void runTest() throws Throwable {
      final CacheManager cm = getCacheManager();
      final Cache c = cm.getCache("test");
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
          Assert.assertTrue(c.remove("key-" + i));
        }
      }

      barrier.await();
      if (nodeId == 0) {
        info(nodeId, "Accessing elements until invalidations come in.");
        CallableWaiter.waitOnCallable(new Callable<Boolean>() {
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

    private CacheManager getCacheManager() {
      return CacheManager.create(getClass().getResourceAsStream("/l1bm-update-invalidated-entry-test.xml"));
    }
  }

  @Override
  public void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(MultipleServersCrashMode.NO_CRASH);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
  }
}
