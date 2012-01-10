package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;

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

public class StorageStrategyNotSupportedTest extends TransparentTestBase {

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

    @Override
    protected void runTest() throws Throwable {

      try {
        CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/ehcache-not-supported.xml"));
        Assert.assertEquals(1, cacheManager.getCacheNames().length);
        Assert.assertTrue(cacheManager.getCacheNames()[0].equals("test"));
      } catch (CacheException e) {
        fail("Using storageStrategy=dcv2 should work even without ee");
      }

      // test programmatic way
      CacheManager cm = CacheManager.getInstance();
      CacheConfiguration cacheConfiguration = new CacheConfiguration("testCache", 100);
      TerracottaConfiguration tc = new TerracottaConfiguration().storageStrategy(StorageStrategy.DCV2).clustered(true);
      cacheConfiguration.addTerracotta(tc);
      Cache cache = new Cache(cacheConfiguration);

      try {
        cm.addCache(cache);
        Assert.assertEquals(1, cm.getCacheNames().length);
        Assert.assertTrue(cm.getCacheNames()[0].equals("testCache"));
      } catch (CacheException e) {
        fail("Using storageStrategy=dcv2 should work even without ee");
      }

    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

}
