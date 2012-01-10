/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.InvalidConfigurationException;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.ArrayList;
import java.util.concurrent.CyclicBarrier;

public class PinnedCacheTest extends TransparentTestBase {
  private static final int NODE_COUNT = 2;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected void setExtraJvmArgs(ArrayList jvmArgs) {
    super.setExtraJvmArgs(jvmArgs);
    // Constantly refresh client object references so that we don't need to wait around for
    // any evictions that may be stuck on client object references
    jvmArgs.add("-Dcom.tc.l2.servermap.eviction.clientObjectReferences.refresh.interval=1");
  }

  @Override
  protected Class getApplicationClass() {
    return PinnedCacheTestApp.class;
  }

  @Override
  protected boolean useExternalProcess() {
    return true;
  }

  public static class PinnedCacheTestApp extends AbstractErrorCatchingTransparentApp {
    private static final CyclicBarrier barrier = new CyclicBarrier(NODE_COUNT);

    public PinnedCacheTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(PinnedCacheTestApp.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }

    @Override
    protected void runTest() throws Throwable {
      int nodeId = barrier.await();
      Thread.currentThread().setName("Node[" + nodeId + "]");
      CacheManager cm = CacheManager.create();

      Cache pinnedInCache = new Cache(new CacheConfiguration().name("pinnedInCache")
          .terracotta(new TerracottaConfiguration()).maxEntriesLocalHeap(100)
          .pinning(new PinningConfiguration().store("inCache")));

      // Do this to establish an order, node0 will add to cache first.
      if (nodeId == 0) {
        cm.addCache(pinnedInCache);
      }
      barrier.await();
      if (nodeId == 1) {
        cm.addCache(pinnedInCache);
      }
      barrier.await();

      if (nodeId == 0) {
        pinnedInCache.getCacheConfiguration().setMaxElementsOnDisk(100);
        for (int i = 0; i < 200; i++) {
          pinnedInCache.put(new Element("key" + i, "value"));
        }
        pinnedInCache.getSize(); // Force transactions to complete first.
      }
      barrier.await();
      assertEquals(pinnedInCache.getSize(), 200);

      if (nodeId == 1) {
        pinnedInCache.getCacheConfiguration().setMaxElementsOnDisk(100);
        for (int i = 200; i < 400; i++) {
          pinnedInCache.put(new Element("key" + i, "value"));
        }
        pinnedInCache.getSize(); // Force transactions to complete first.
      }
      barrier.await();
      assertEquals(pinnedInCache.getSize(), 400);

      Cache pinnedInCacheWithMaxOnDisk = new Cache(new CacheConfiguration().name("pinnedInCacheWithMaxOnDisk")
          .terracotta(new TerracottaConfiguration()).maxEntriesLocalHeap(100).maxElementsOnDisk(123)
          .pinning(new PinningConfiguration().store("inCache")));
      try {
        cm.addCache(pinnedInCacheWithMaxOnDisk);
        fail("Expected cache configuration to fail.");
      } catch (InvalidConfigurationException e) {
        // expected
      }
    }
  }
}
