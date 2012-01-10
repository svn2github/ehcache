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
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import junit.framework.Assert;

public class CachePinningTest extends TransparentTestBase {

  private static final int NODE_COUNT    = 1;
  private static final int ELEMENT_COUNT = 1000;

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

    @Override
    protected void runTest() throws Throwable {
      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/cache-pinning-test.xml"));

      Cache cache = cacheManager.getCache("pinned");

      for (int i = 0; i < ELEMENT_COUNT; i++) {
        cache.put(new Element(i, i));
      }

      Assert.assertEquals(ELEMENT_COUNT, cache.getSize());

      for (int i = 0; i < ELEMENT_COUNT; i++) {
        assertNotNull(cache.get(i));
      }

      Assert.assertEquals(ELEMENT_COUNT, cache.getStatistics().getInMemoryHits());
      Assert.assertEquals(0, cache.getStatistics().getInMemoryMisses());
      Assert.assertEquals(0, cache.getStatistics().getOnDiskHits());
      Assert.assertEquals(0, cache.getStatistics().getOnDiskMisses());
      Assert.assertEquals(0, cache.getStatistics().getEvictionCount());
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }
}
