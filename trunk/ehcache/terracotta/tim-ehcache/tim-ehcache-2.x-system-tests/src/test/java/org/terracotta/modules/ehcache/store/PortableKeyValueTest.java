/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.Store;

import org.terracotta.cache.TimestampedValue;
import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.Serializable;

import junit.framework.Assert;

public class PortableKeyValueTest extends TransparentTestBase {

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
      CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName("cache");
      cacheConfiguration.setMaxElementsInMemory(1000);
      cacheConfiguration.setOverflowToDisk(false);
      cacheConfiguration.setEternal(false);
      cacheConfiguration.setTimeToLiveSeconds(100000);
      cacheConfiguration.setTimeToIdleSeconds(200000);
      cacheConfiguration.setDiskPersistent(false);
      cacheConfiguration.setDiskExpiryThreadIntervalSeconds(1);

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      cacheConfiguration.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfiguration);
      cacheManager.addCache(cache);

      Store store = cacheManager.createTerracottaStore(cache);
      Assert.assertTrue("store: " + store.getClass().getName(), store instanceof ClusteredSafeStore);

      ClusteredStore clusteredStore = ((ClusteredSafeStore) store).getClusteredStore();
      DummyObject dummyObject = new DummyObject("kjhdckhkshc", 387268623);
      Object portableKey = clusteredStore.generatePortableKeyFor(dummyObject);
      Assert.assertTrue(portableKey instanceof String);

      final ValueModeHandler valueModeHandler = ValueModeHandlerFactory.createValueModeHandler(clusteredStore,
                                                                                               cacheConfiguration);

      TimestampedValue value = valueModeHandler.createTimestampedValue(new Element(new DummyObject("iuidi", 387),
                                                                                   new DummyObject("auefiuwef", 363)));

      Assert.assertTrue(value.getValue() instanceof byte[]);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier").addRoot("nodes", "nodes");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

  private static class DummyObject implements Serializable {
    private final String stringKey;
    private final int    intKey;

    public DummyObject(String stringKey, int intKey) {
      super();
      this.stringKey = stringKey;
      this.intKey = intKey;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + intKey;
      result = prime * result + ((stringKey == null) ? 0 : stringKey.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      DummyObject other = (DummyObject) obj;
      if (intKey != other.intKey) return false;
      if (stringKey == null) {
        if (other.stringKey != null) return false;
      } else if (!stringKey.equals(other.stringKey)) return false;
      return true;
    }

  }
}
