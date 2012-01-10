/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.concurrent.CyclicBarrier;

public class TotalCapacityTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

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

    private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());

    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/total-capacity-cache-test.xml"));

      Cache cache1 = cacheManager.getCache("test1");

      if (index == 0) {
        // Fill cache up to the max capacity = 10 elements
        for(int i = 1; i<=10; i++) {
          cache1.put(new Element("key" + i, "value" + i));
          Assert.assertEquals(i, cache1.getSize());
        }
      }

      barrier.await();

      // In another node, add additional elements which should trigger an eviction
      if(index == 1) {
        for(int i = 11; i<=20; i++) {
          cache1.put(new Element("key" + i, "value" + i));
          Assert.assertEquals(10, cache1.getSize());
        }
      }
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

}
