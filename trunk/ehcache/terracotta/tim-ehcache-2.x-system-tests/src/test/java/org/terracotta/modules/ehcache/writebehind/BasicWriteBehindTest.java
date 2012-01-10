/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

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
import java.util.concurrent.atomic.AtomicLong;

public class BasicWriteBehindTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

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

    final AtomicLong totalWriteCount = new AtomicLong();
    final AtomicLong totalDeleteCount = new AtomicLong();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      WriteBehindCacheWriter writer;

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-writebehind-test.xml"));
      Cache cache = cacheManager.getCache("test");

      if (0 == index) {
        writer = new WriteBehindCacheWriter("WriteBehindCacheWriter", this, 20L);
        cache.registerCacheWriter(writer);

        for (int i = 0; i < 1000; i++) {
          cache.putWithWriter(new Element("key" + i % 200, "value" + i));
          if (0 == i % 10) {
            cache.removeWithWriter("key" + i % 200 / 10);
          }
        }
      } else {
        writer = new WriteBehindCacheWriter("WriteBehindCacheWriter", this, 10L);
        cache.registerCacheWriter(writer);

        cache.putWithWriter(new Element("key", "value"));
        cache.removeWithWriter("key");
      }

      Thread.sleep(60000);
      barrier.await();

      System.out.println("[Client " + getApplicationId() + " processed " + writer.getWriteCount() + " writes for writer 1]");
      System.out.println("[Client " + getApplicationId() + " processed " + writer.getDeleteCount() + " deletes for writer 1]");

      totalWriteCount.addAndGet(writer.getWriteCount());
      totalDeleteCount.addAndGet(writer.getDeleteCount());

      barrier.await();

      if (0 == index) {
        System.out.println("[Clients processed a total of " + totalWriteCount.get() + " writes]");
        System.out.println("[Clients processed a total of " + totalDeleteCount.get() + " deletes]");

        Assert.assertEquals(1001, totalWriteCount.get());
        Assert.assertEquals(101, totalDeleteCount.get());
      }

      barrier.await();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName())
        .addRoot("barrier", "barrier")
        .addRoot("totalWriteCount", "totalWriteCount")
        .addRoot("totalDeleteCount", "totalDeleteCount");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }
}