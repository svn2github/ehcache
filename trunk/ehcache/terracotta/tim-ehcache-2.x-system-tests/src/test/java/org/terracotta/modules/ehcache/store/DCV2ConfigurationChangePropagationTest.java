package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.concurrent.CyclicBarrier;

import junit.framework.Assert;

/**
 * @author cdennis
 */
public class DCV2ConfigurationChangePropagationTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void doSetUp(final TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier barrier1 = new CyclicBarrier(getParticipantCount());
    private final CyclicBarrier barrier2 = new CyclicBarrier(getParticipantCount() - 1);

    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier1.await();
      CacheManager cm = null;
      if (index != 2) {
        try {
          cm = testTTIChange();
        } finally {
          cm.shutdown();
        }
        try {
          cm = testTTLChange();
        } finally {
          cm.shutdown();
        }
        try {
          cm = testDiskCapacityChange();
        } finally {
          cm.shutdown();
        }
        try {
          cm = testMemoryCapacityChange();
        } finally {
          cm.shutdown();
        }
      }
      barrier1.await();
      if (index == 2) {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-dcv2-cache-test.xml"));
        verifyNewNode(cacheManager.getCache("dcv2Cache"));
      }
      barrier1.await();
    }

    private void verifyNewNode(final Cache cache) {
      Assert.assertEquals(99, cache.getCacheConfiguration().getTimeToIdleSeconds());
      Assert.assertEquals(99, cache.getCacheConfiguration().getTimeToLiveSeconds());
      Assert.assertEquals(99, cache.getCacheConfiguration().getMaxElementsOnDisk());
      Assert.assertEquals(10000, cache.getCacheConfiguration().getMaxEntriesLocalHeap());
    }

    private CacheManager testTTIChange() throws Throwable {
      barrier2.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-dcv2-cache-test.xml"));
      final Cache cache = cacheManager.getCache("dcv2Cache");
      cache.getCacheConfiguration().setEternal(false);

      int index = barrier2.await();

      if (index == 0) {
        System.err.println("Changing TTI on Client " + ManagerUtil.getClientID());
        cache.getCacheConfiguration().setTimeToIdleSeconds(99);
      }

      barrier2.await();

      for (int i = 0; i < 60; i++) {
        Thread.sleep(1000);
        if (99 == cache.getCacheConfiguration().getTimeToIdleSeconds()) {
          System.err.println("Change to TTI took " + (i + 1) + " seconds to propagate to Client "
                             + ManagerUtil.getClientID());
          return cacheManager;
        }
      }

      Assert.fail("Change to TTI failed to propagate inside 1 minute");
      return cacheManager;
    }

    private CacheManager testTTLChange() throws Throwable {
      barrier2.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-dcv2-cache-test.xml"));
      final Cache cache = cacheManager.getCache("dcv2Cache");
      cache.getCacheConfiguration().setEternal(false);

      int index = barrier2.await();

      if (index == 0) {
        System.err.println("Changing TTL on Client " + ManagerUtil.getClientID());
        cache.getCacheConfiguration().setTimeToLiveSeconds(99);
      }

      barrier2.await();

      for (int i = 0; i < 60; i++) {
        Thread.sleep(1000);
        if (99 == cache.getCacheConfiguration().getTimeToLiveSeconds()) {
          System.err.println("Change to TTL took " + (i + 1) + " seconds to propagate to Client "
                             + ManagerUtil.getClientID());
          return cacheManager;
        }
      }

      Assert.fail("Change to TTL failed to propagate inside 1 minute");
      return cacheManager;
    }

    private CacheManager testDiskCapacityChange() throws Throwable {
      barrier2.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-dcv2-cache-test.xml"));
      final Cache cache = cacheManager.getCache("dcv2Cache");

      int index = barrier2.await();

      if (index == 0) {
        System.err.println("Changing Disk Capacity on Client " + ManagerUtil.getClientID());
        cache.getCacheConfiguration().setMaxElementsOnDisk(99);
      }

      barrier2.await();

      for (int i = 0; i < 60; i++) {
        Thread.sleep(1000);
        if (99 == cache.getCacheConfiguration().getMaxElementsOnDisk()) {
          System.err.println("Change to Disk Capacity took " + (i + 1) + " seconds to propagate to Client "
                             + ManagerUtil.getClientID());
          return cacheManager;
        }
      }

      Assert.fail("Change to Disk Capacity failed to propagate inside 1 minute");
      return cacheManager;
    }

    private CacheManager testMemoryCapacityChange() throws Throwable {
      barrier2.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-dcv2-cache-test.xml"));
      final Cache cache = cacheManager.getCache("dcv2Cache");

      int index = barrier2.await();

      if (index == 0) {
        System.err.println("Changing Memory Capacity on Client " + ManagerUtil.getClientID());
        cache.getCacheConfiguration().setMaxEntriesLocalHeap(99);
      }

      barrier2.await();

      if (index == 0) {
        Assert.assertEquals("Failed to change max entries local heap.", 99, cache.getCacheConfiguration()
            .getMaxEntriesLocalHeap());
      } else {
        ThreadUtil.reallySleep(60 * 1000);
        Assert.assertEquals("Max entries local heap change propagated to the other client.", 10000, cache
            .getCacheConfiguration().getMaxEntriesLocalHeap());
      }

      return cacheManager;
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier2", "barrier2");
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier1", "barrier1");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }
}
