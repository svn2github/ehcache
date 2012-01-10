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
import com.tc.objectserver.core.api.ServerConfigurationContext;
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

import java.util.ArrayList;

public class FailoverDuringPassiveSyncTest extends ActivePassiveTransparentTestBase {
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
  protected void setExtraJvmArgs(ArrayList jvmArgs) {
    super.setExtraJvmArgs(jvmArgs);
    // Slow down the object sync to create a sufficiently large window for crashing the active
    jvmArgs.add("-Dcom.tc.seda." + ServerConfigurationContext.OBJECTS_SYNC_STAGE + ".sleepMs=2000");
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
    return FailoverDuringPassiveSyncTestApp.class;
  }

  public static class FailoverDuringPassiveSyncTestApp extends AbstractErrorCatchingTransparentApp {
    private final ActivePassiveServerManager manager;

    public FailoverDuringPassiveSyncTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      manager = (ActivePassiveServerManager) cfg.getAttributeObject(MANAGER_ATTRIBUTE);
    }

    @Override
    protected void runTest() throws Throwable {
      CacheManager cm = getCacheManager();
      Ehcache cache = cm.getEhcache("test");
      for (int i = 0; i < 20000; i++) {
        cache.put(new Element("key-" + i, new byte[1024]));
      }

      info("Starting up the first passive.");
      manager.startServer(1);

      info("Waiting until the passive is synced up.");
      manager.waitServerIsPassiveStandby(1, 600);

      info("Starting up the second passive.");
      manager.startServer(2);

      info("Sleeping for a short time to wait for the passive syncup to start.");
      ThreadUtil.reallySleep(15 * 1000);

      info("Killing the active so passive[1] can take over.");
      manager.stopServer(0);

      info("Waiting for passive[2] to fully sync up.");
      manager.waitServerIsPassiveStandby(2, 600);

      info("Stopping passive[1].");
      manager.stopServer(1);

      for (int i = 0; i < 20000; i++) {
        assertNotNull(cache.get("key-" + i));
      }
    }

    private void info(String msg) {
      System.out.println(msg);
    }

    private CacheManager getCacheManager() {
      return CacheManager.create(FailoverDuringPassiveSyncTestApp.class
          .getResourceAsStream("/failover-during-passive-sync-test.xml"));
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(FailoverDuringPassiveSyncTestApp.class.getName());

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

}
