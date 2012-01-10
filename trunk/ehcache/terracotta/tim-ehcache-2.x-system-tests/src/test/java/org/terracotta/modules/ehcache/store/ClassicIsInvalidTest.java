/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;

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

public class ClassicIsInvalidTest extends TransparentTestBase {

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
      CacheManager cacheManager = CacheManager.create();

      try {
        crerateCache("eventualClassicIdentity", cacheManager, "CLASSIC", Consistency.EVENTUAL, "IDENTITY");
        Assert.fail("was able to create a clustered cache with \"classic\" storage strategy");
      } catch (Exception e) {
        // expected exception
      }

      try {
        crerateCache("eventualClassicSerialization", cacheManager, "CLASSIC", Consistency.EVENTUAL, "SERIALIZATION");
        Assert.fail("was able to create a clustered cache with \"classic\" storage strategy");
      } catch (Exception e) {
        // expected exception
      }

      try {
        crerateCache("strongClassicIdentity", cacheManager, "CLASSIC", Consistency.STRONG, "IDENTITY");
        Assert.fail("was able to create a clustered cache with \"classic\" storage strategy");
      } catch (Exception e) {
        // expected exception
      }

      try {
        crerateCache("strongClassicSerialization", cacheManager, "CLASSIC", Consistency.STRONG, "SERIALIZATION");
        Assert.fail("was able to create a clustered cache with \"classic\" storage strategy");
      } catch (Exception e) {
        // expected exception
      }

      crerateCache("eventualDCV2Identity", cacheManager, "DCV2", Consistency.EVENTUAL, "IDENTITY");
      crerateCache("eventualDCV2Serialization", cacheManager, "DCV2", Consistency.EVENTUAL, "SERIALIZATION");
      crerateCache("strongDCV2Identity", cacheManager, "DCV2", Consistency.STRONG, "IDENTITY");
      crerateCache("strongDCV2Serialization", cacheManager, "DCV2", Consistency.STRONG, "SERIALIZATION");

    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }

    private Cache crerateCache(String cacheName, CacheManager cacheManager, String storageStrategy,
                               Consistency consistency, String valueMode) {
      System.out.println("creating " + cacheName);
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(cacheName);
      cacheConfiguration.setOverflowToDisk(false);
      cacheConfiguration.setEternal(false);
      cacheConfiguration.setMaxBytesLocalHeap(10 * 1024 * 1024L);
      cacheConfiguration.setDiskPersistent(false);

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setStorageStrategy(storageStrategy);
      tcConfiguration.setConsistency(consistency);
      tcConfiguration.setValueMode(valueMode);
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cacheManager.addCache(cache);
      return cache;
    }
  }

}
