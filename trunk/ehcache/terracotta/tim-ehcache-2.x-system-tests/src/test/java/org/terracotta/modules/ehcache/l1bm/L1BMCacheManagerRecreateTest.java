/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
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

public class L1BMCacheManagerRecreateTest extends TransparentTestBase {
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
    private CacheManager cm;
    private Ehcache      cache;

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
      setup();
      for (int i = 0; i < 100; i++) {
        cache.put(new Element("key" + i, "value" + i));
      }
      cleanup();

      setup();
      for (int i = 0; i < 100; i++) {
        Element e = cache.get("key" + i);
        assertNotNull(e);
        assertEquals("value" + i, e.getObjectValue());
      }
      cleanup();
    }

    private void setup() {
      cm = new CacheManager(getClass().getResourceAsStream("/basic-cache-test.xml"));
      cache = cm.getEhcache("test");
    }

    private void cleanup() {
      cm.shutdown();
    }
  }
}
