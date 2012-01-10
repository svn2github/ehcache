/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheManager;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Map;

import junit.framework.Assert;

public class SampledStatisticTimerTest extends TransparentTestBase {
  private static final int NODE_COUNT = 1;

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

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName());

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }

    @Override
    protected void runTest() throws Throwable {
      CacheManager cm = new CacheManager(getClass().getResourceAsStream("/sampled-statistic-timer-test.xml"));
      for (int i = 0; i < 10; i++) {
        String cacheName = "cache-" + i;
        cm.addCache(cacheName);
      }

      Map<Thread, StackTraceElement[]> liveThreads = Thread.getAllStackTraces();
      int samplerThreadCount = 0;
      for (Thread t : liveThreads.keySet()) {
        String threadName = t.getName();
        if (threadName.contains("SampledStatisticsManager Timer")) {
          samplerThreadCount++;
        }
      }
      Assert.assertEquals("Found statistic sampler threads!.", 0, samplerThreadCount);
    }
  }
}
