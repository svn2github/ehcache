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

import java.io.Serializable;
import java.util.concurrent.CyclicBarrier;

public class SerializedCacheCopyOnReadTest extends TransparentTestBase {

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

    private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());

    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-copy-on-read-cache-test.xml"));

      Cache cache = cacheManager.getCache("test");

      Assert.assertEquals(0, cache.getSize());

      barrier.await();

      if (index == 0) {
        ValueHolder value = new ValueHolder("value");
        cache.put(new Element("key", value));
      }

      barrier.await();

      ValueHolder value = (ValueHolder) cache.get("key").getObjectValue();
      Assert.assertEquals(1, cache.getSize());
      Assert.assertEquals("value", value.getData());

      // test that copyOnRead works
      ValueHolder value2 = (ValueHolder) cache.get("key").getObjectValue();
      Assert.assertTrue(value != value2);
      Assert.assertEquals("value", value2.getData());
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

  public static class ValueHolder implements Serializable {
    private String data;

    public ValueHolder(final String data) {
      setData(data);
    }

    public synchronized String getData() {
      return data;
    }

    public synchronized void setData(final String data) {
      this.data = data;
    }
  }

}
