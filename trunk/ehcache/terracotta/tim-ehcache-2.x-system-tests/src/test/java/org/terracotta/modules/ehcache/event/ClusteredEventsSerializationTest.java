/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

public class ClusteredEventsSerializationTest extends TransparentTestBase {

  private static final int NODE_COUNT = 5;

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

    private final CyclicBarrier barrier = new CyclicBarrier(NODE_COUNT);

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      TCProperties tcProps = TCPropertiesImpl.getProperties();
      tcProps.setProperty("ehcache.clusteredStore.checkContainsKeyOnPut", "true");
      final int index = barrier.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/clustered-events-test.xml"));

      Cache cache = cacheManager.getCache("testSerialization");

      Assert.assertEquals(0, cache.getSize());

      barrier.await();

      NonPortable key = new NonPortable("key" + index);
      NonPortable valuePut = new NonPortable("value" + index);
      NonPortable valueUpdate = new NonPortable("valueUpdated" + index);
      cache.put(new Element(key, valuePut));
      cache.put(new Element(key, valueUpdate));
      cache.remove(key);

      barrier.await();

      cache.removeAll();

      barrier.await();

      Thread.sleep(10000);

      EhcacheTerracottaEventListener listener = null;
      Set<CacheEventListener> listeners = cache.getCacheEventNotificationService().getCacheEventListeners();
      for (CacheEventListener l : listeners) {
        if (l instanceof EhcacheTerracottaEventListener) {
          listener = (EhcacheTerracottaEventListener) l;
          break;
        }
      }

      Assert.assertNotNull(listener);

      Assert.assertEquals(ManagerUtil.getClientID(), NODE_COUNT, listener.getPut().size());
      Assert.assertEquals(ManagerUtil.getClientID(), NODE_COUNT, listener.getUpdate().size());
      Assert.assertEquals(ManagerUtil.getClientID(), NODE_COUNT, listener.getRemove().size());
      Assert.assertEquals(ManagerUtil.getClientID(), NODE_COUNT, listener.getRemoveAll());

      boolean foundPutKey = false;
      for (Element element : listener.getPut()) {
        if (element.getObjectKey().equals(key)) {
          foundPutKey = true;
          Assert.assertEquals(valuePut, element.getObjectValue());
        } else {
          Assert.assertEquals("value" + element.getObjectKey().toString().substring("key".length()), element
              .getObjectValue().toString());
        }
      }
      Assert.assertTrue(foundPutKey);

      boolean foundUpdateKey = false;
      for (Element element : listener.getUpdate()) {
        if (element.getObjectKey().equals(key)) {
          foundUpdateKey = true;
          Assert.assertEquals(valueUpdate, element.getObjectValue());
        } else {
          Assert.assertEquals("valueUpdated" + element.getObjectKey().toString().substring("key".length()), element
              .getObjectValue().toString());
        }
      }
      Assert.assertTrue(foundUpdateKey);

      boolean foundRemoveKey = false;
      for (Element element : listener.getRemove()) {
        if (element.getObjectKey().equals(key)) {
          foundRemoveKey = true;
        }
      }
      Assert.assertTrue(foundRemoveKey);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

  public static class NonPortable implements Serializable {
    private final String value;

    public NonPortable(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      NonPortable that = (NonPortable) o;

      if (value != null ? !value.equals(that.value) : that.value != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return value != null ? value.hashCode() : 0;
    }
  }
}