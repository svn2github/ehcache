/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
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

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class RemovingCacheWriteBehindTest extends TransparentTestBase {

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

      final CyclicBarrier localBarrier = new CyclicBarrier(2);

      WriteBehindCacheWriter writer;

      final CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-writebehind-test.xml"));
      final Cache cache = cacheManager.getCache("test");
      ExecutorService executorService = Executors.newFixedThreadPool(1);
      executorService.execute(new Runnable() {
        public void run() {
          try {
            localBarrier.await();
            System.err.println("Fine...");
            Thread.sleep(100);
            cacheManager.removeCache(cache.getName());
            System.err.println("Removed cache");
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } catch (BrokenBarrierException e) {
            e.printStackTrace();
          }
        }
      });

      writer = new WriteBehindCacheWriter("WriteBehindCacheWriter for Cache " +cache.getName(), this, 20L, true);
      cache.registerCacheWriter(writer);

      Throwable cause = null;
      System.err.println("Putting with writer...");
      for (int i = 0; i < 100000; i++) {
        if (i == 100) {
          localBarrier.await();
          System.err.println("Let's break things!");
        }
        final String key = "key" + i;
        System.err.println("Scheduling for key " + key);
        try {
          cache.putWithWriter(new Element(key, "value" + i));
        } catch (CacheException e) {
          e.printStackTrace();
          cause = e.getCause();
          break;
        }
      }
      assertThat("Cause for putWithWriter shouldn't be null!", cause, notNullValue());
      assertThat("Cause for putWithWriter shouldn't be that?!", cause.getClass().getName(), equalTo(IllegalStateException.class.getName()));
      assertThat(cause.getMessage(), equalTo("AsyncWriteBehind is stopped!"));
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