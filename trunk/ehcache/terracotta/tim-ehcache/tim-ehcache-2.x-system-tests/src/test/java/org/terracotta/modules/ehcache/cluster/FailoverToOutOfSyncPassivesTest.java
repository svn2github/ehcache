/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.cluster;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.ActivePassiveTransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FailoverToOutOfSyncPassivesTest extends ActivePassiveTransparentTestBase {
  private static final int    NODE_COUNT        = 1;
  private static final String MANAGER_ATTRIBUTE = "MANAGER";

  @Override
  protected boolean canRun() {
    return isMultipleServerTest();
  }

  @Override
  protected void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) throws Exception {
    setupManager.setServerCount(3);
    setupManager.setServerCrashMode(MultipleServersCrashMode.AP_CUSTOMIZED_CRASH);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.TEMPORARY_SWAP_ONLY);
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected void customizeActivePassiveTest(ActivePassiveServerManager manager) throws Exception {
    manager.startServer(0);
    int index = manager.getAndUpdateActiveIndex();
    assertEquals(0, index);

    getTransparentAppConfig().setAttribute(MANAGER_ATTRIBUTE, manager);
  }

  @Override
  protected Class getApplicationClass() {
    return FailoverToOutOfSyncPassivesTestApp.class;
  }

  public static class FailoverToOutOfSyncPassivesTestApp extends AbstractErrorCatchingTransparentApp {
    private final ActivePassiveServerManager manager;

    public FailoverToOutOfSyncPassivesTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      manager = (ActivePassiveServerManager) cfg.getAttributeObject(MANAGER_ATTRIBUTE);
    }

    @Override
    protected void runTest() throws Throwable {
      CacheManager cm = getCacheManager();
      final Ehcache cache = cm.getEhcache("test");

      info("Starting up the passives.");
      manager.startServer(1);
      manager.startServer(2);

      info("Waiting until the passives are synced up.");
      manager.waitServerIsPassiveStandby(1, 600);
      manager.waitServerIsPassiveStandby(2, 600);

      final AtomicInteger keyIndex = new AtomicInteger();
      final AtomicBoolean running = new AtomicBoolean(true);
      Thread putter = new Thread(new Runnable() {
        public void run() {
          while (running.get()) {
            cache.put(new Element("key-" + keyIndex.incrementAndGet(), new byte[1024]));
          }
        }
      });
      putter.start();

      info("Wait for a bit to do a few puts.");
      ThreadUtil.reallySleep(15 * 1000);

      info("Killing the active so a passive can take over.");
      manager.stopServer(0);

      running.set(false);
      putter.join();

      info("Get the current active server.");
      int activeIndex = manager.getAndUpdateActiveIndex();
      info("Killing active server.");
      manager.stopServer(activeIndex);
      manager.getAndUpdateActiveIndex();

      for (int i = 1; i < keyIndex.get(); i++) {
        assertNotNull(cache.get("key-" + i));
      }
    }

    private void info(String msg) {
      System.out.println(msg);
    }

    private CacheManager getCacheManager() {
      return CacheManager.create(FailoverToOutOfSyncPassivesTestApp.class
          .getResourceAsStream("/failover-during-passive-sync-test.xml"));
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(FailoverToOutOfSyncPassivesTestApp.class.getName());

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

}
