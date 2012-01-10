package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.test.MultipleServersCrashMode;
import com.tc.test.MultipleServersPersistenceMode;
import com.tc.test.MultipleServersSharedDataMode;
import com.tc.test.TestConfigObject;
import com.tc.test.activepassive.ActivePassiveServerManager;
import com.tc.test.activepassive.ActivePassiveTestSetupManager;
import com.tctest.ActivePassiveTransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;
import com.tctest.runner.TransparentAppConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class LocalReadsGetKeysTest extends ActivePassiveTransparentTestBase {

  private static final int     NODE_COUNT = 2;
  private TransparentAppConfig transparentAppConfig;

  public LocalReadsGetKeysTest() {
    //
  }

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();

    transparentAppConfig = t.getTransparentAppConfig();
  }

  @Override
  protected Class<?> getApplicationClass() {
    return App.class;
  }

  @Override
  protected boolean canRun() {
    return mode().endsWith(TestConfigObject.TRANSPARENT_TESTS_MODE_ACTIVE_PASSIVE);
  }

  @Override
  protected boolean enableL1Reconnect() {
    return true;
  }

  @Override
  protected void setJvmArgsL1Reconnect(ArrayList jvmArgs) {
    super.setJvmArgsL1Reconnect(jvmArgs);

    TCProperties tcProps = TCPropertiesImpl.getProperties();

    // bump up L1 reconnect time since we're going to bounce server out right
    String timeout = "120000";
    tcProps.setProperty(TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, timeout);
    System.setProperty("com.tc." + TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS, timeout);
    jvmArgs.add("-Dcom.tc." + TCPropertiesConsts.L2_L1RECONNECT_TIMEOUT_MILLS + "=" + timeout);
  }

  @Override
  protected void customizeActivePassiveTest(ActivePassiveServerManager manager) throws Exception {
    manager.startServer(0);

    int index = manager.getAndUpdateActiveIndex();
    assertEquals(0, index);

    transparentAppConfig.setAttribute("manager", manager);
  }

  @Override
  protected void setupActivePassiveTest(ActivePassiveTestSetupManager setupManager) {
    // even though this is an A/P test subclass we're only really going to run just an active
    setupManager.setServerCount(2);

    setupManager.setServerCrashMode(MultipleServersCrashMode.AP_CUSTOMIZED_CRASH);
    setupManager.setServerShareDataMode(MultipleServersSharedDataMode.NETWORK);
    setupManager.setServerPersistenceMode(MultipleServersPersistenceMode.PERMANENT_STORE);
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier               barrier = new CyclicBarrier(getParticipantCount());
    private static ActivePassiveServerManager manager;

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      manager = (ActivePassiveServerManager) cfg.getAttributeObject("manager");
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/local-reads-get-keys-test.xml"));
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
        manager.stopServer(0);

        try {
          attemptNonLocalRead(caches);
          testGetKeysMethods(caches);
        } finally {
          manager.startServer(0);
        }
      }
    }

    private void readAllKeys(Cache[] caches) {
      for (Cache c : caches) {
        // iterating the keys will force them to be deserialized (a get() does not currently do that)

        List keys = c.getKeys();
        assertEquals(1, keys.size());

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
      assertEquals(name, expect, keys.size());
      for (Object key : keys) {
        assertEquals(name, KeyType1.class, key.getClass());
      }
    }

    private void attemptNonLocalRead(Cache[] caches) {
      // should return null since server is down and no local data is present
      for (Cache c : caches) {
        assertEquals(null, c.get(new KeyType1(1)));
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

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      final String module_name = "tim-ehcache-2.x";
      final TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());

      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");
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
