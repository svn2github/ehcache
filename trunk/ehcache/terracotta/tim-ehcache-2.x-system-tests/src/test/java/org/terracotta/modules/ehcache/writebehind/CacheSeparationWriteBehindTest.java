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

public class CacheSeparationWriteBehindTest extends TransparentTestBase {

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

    final AtomicLong totalWriteCount1 = new AtomicLong();
    final AtomicLong totalDeleteCount1 = new AtomicLong();
    final AtomicLong totalWriteCount2 = new AtomicLong();
    final AtomicLong totalDeleteCount2 = new AtomicLong();

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/cache-separation-writebehind-test.xml"));
      Cache cache1 = cacheManager.getCache("test1");
      Cache cache2 = cacheManager.getCache("test2");

      WriteBehindCacheWriter writer1;
      WriteBehindCacheWriter writer2;

      if (0 == index) {
        writer1 = new WriteBehindCacheWriter("WriteBehindCacheWriter1", this, 20L);
        cache1.registerCacheWriter(writer1);
        writer2 = new WriteBehindCacheWriter("WriteBehindCacheWriter2", this, 20L);
        cache2.registerCacheWriter(writer2);

        for (int i = 0; i < 1000; i++) {
          cache1.putWithWriter(new Element("key" + i % 200, "value" + i));
          if (0 == i % 10) {
            cache1.removeWithWriter("key" + i % 200 / 10);
          }
        }

        for (int i = 0; i < 100; i++) {
          cache2.putWithWriter(new Element("key" + i % 200, "value" + i));
          if (0 == i % 10) {
            cache2.removeWithWriter("key" + i % 200 / 10);
          }
        }
      } else {
        writer1 = new WriteBehindCacheWriter("WriteBehindCacheWriter1", this, 10L);
        cache1.registerCacheWriter(writer1);
        writer2 = new WriteBehindCacheWriter("WriteBehindCacheWriter2", this, 10L);
        cache2.registerCacheWriter(writer2);

        cache1.putWithWriter(new Element("key", "value"));
        cache1.removeWithWriter("key");
        cache2.putWithWriter(new Element("key", "value"));
        cache2.removeWithWriter("key");
      }

      Thread.sleep(60000);
      barrier.await();

      System.out.println("[Client " + getApplicationId() + " processed " + writer1.getWriteCount() + " writes for writer 1]");
      System.out.println("[Client " + getApplicationId() + " processed " + writer2.getWriteCount() + " writes for writer 2]");
      System.out.println("[Client " + getApplicationId() + " processed " + writer1.getDeleteCount() + " deletes for writer 1]");
      System.out.println("[Client " + getApplicationId() + " processed " + writer2.getDeleteCount() + " deletes for writer 2]");

      totalWriteCount1.addAndGet(writer1.getWriteCount());
      totalDeleteCount1.addAndGet(writer1.getDeleteCount());
      totalWriteCount2.addAndGet(writer2.getWriteCount());
      totalDeleteCount2.addAndGet(writer2.getDeleteCount());

      barrier.await();

      if (0 == index) {
        System.out.println("[Clients processed a total of " + totalWriteCount1.get() + " writes for writer 1]");
        System.out.println("[Clients processed a total of " + totalWriteCount2.get() + " writes for writer 2]");
        System.out.println("[Clients processed a total of " + totalDeleteCount1.get() + " deletes for writer 1]");
        System.out.println("[Clients processed a total of " + totalDeleteCount2.get() + " deletes for writer 2]");

        Assert.assertEquals(1001, totalWriteCount1.get());
        Assert.assertEquals(101, totalWriteCount2.get());
        Assert.assertEquals(101, totalDeleteCount1.get());
        Assert.assertEquals(11, totalDeleteCount2.get());
      }

      barrier.await();
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName())
        .addRoot("barrier", "barrier")
        .addRoot("totalWriteCount1", "totalWriteCount1")
        .addRoot("totalDeleteCount1", "totalDeleteCount1")
        .addRoot("totalWriteCount2", "totalWriteCount2")
        .addRoot("totalDeleteCount2", "totalDeleteCount2");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }
}