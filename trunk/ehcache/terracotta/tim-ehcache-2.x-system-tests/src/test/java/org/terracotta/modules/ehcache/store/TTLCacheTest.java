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
import java.util.concurrent.TimeUnit;

public class TTLCacheTest extends TransparentTestBase {

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

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/ttl-cache-test.xml"));

      Cache cache1 = cacheManager.getCache("test1");
      Cache cache2 = cacheManager.getCache("test2");

      barrier.await();

      if (index == 0) {
        cache1.put(new Element("key", "value"));
        cache2.put(new Element("key", "value"));
      }

      barrier.await();
      long expiryOne = cache1.get("key").getExpirationTime();
      long expiryTwo = cache2.get("key").getExpirationTime();

      Assert.assertEquals("value", cache1.get("key").getObjectValue());
      Assert.assertEquals("value", cache2.get("key").getObjectValue());

      while (System.currentTimeMillis() < expiryOne - TimeUnit.SECONDS.toMillis(5)) {
        Thread.sleep(100);
      }

      barrier.await();

      Assert.assertEquals("value", cache1.get("key").getObjectValue());
      Assert.assertEquals("value", cache2.get("key").getObjectValue());

      while (System.currentTimeMillis() < expiryOne + TimeUnit.SECONDS.toMillis(1)) {
        Thread.sleep(100);
      }

      barrier.await();

      Assert.assertNull(cache1.get("key"));
      Assert.assertEquals("value", cache2.get("key").getObjectValue());

      while (System.currentTimeMillis() < expiryTwo - TimeUnit.SECONDS.toMillis(10)) {
        Thread.sleep(100);
      }

      barrier.await();

      Assert.assertNull(cache1.get("key"));
      Assert.assertEquals("value", cache2.get("key").getObjectValue());

      while (System.currentTimeMillis() < expiryTwo + TimeUnit.SECONDS.toMillis(1)) {
        Thread.sleep(100);
      }

      barrier.await();

      Assert.assertNull(cache1.get("key"));
      Assert.assertNull(cache2.get("key"));
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

}
