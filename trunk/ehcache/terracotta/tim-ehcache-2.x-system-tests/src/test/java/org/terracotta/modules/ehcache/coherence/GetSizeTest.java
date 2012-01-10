/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

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
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Abhishek Sanoujam
 */
public class GetSizeTest extends TransparentTestBase {

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

  public static class App extends AbstractTransparentApp {
    private volatile boolean stop = false;

    public App(final String appId, final ApplicationConfig cfg, final ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
      String testClass = App.class.getName();
      config.addIncludePattern(testClass + "$*", false, false, true);

      String moduleName = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", moduleName);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }

    public void run() {
      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/cache-coherence-test.xml"));
      final Cache incoherentCache = cacheManager.getCache("test");
      incoherentCache.setNodeCoherent(false);
      Assert.assertEquals(false, incoherentCache.isNodeCoherent());
      Assert.assertEquals(false, incoherentCache.isClusterCoherent());
      final List<Throwable> throwables = Collections.synchronizedList(new ArrayList<Throwable>());

      Thread putThread = new Thread(new Runnable() {

        public void run() {
          try {
            for (int i = 0; i < 1000 && !stop; i++) {
              try {
                Thread.sleep(10);
              } catch (InterruptedException e) {
                // ignore
              }
              incoherentCache.put(new Element(new Date() + "-key-" + i, "value-" + i));
              if (i % 50 == 0) debug("Elements added till now: " + (i + 1));
            }
          } catch (Throwable e) {
            throwables.add(e);
          } finally {
            stop = true;
          }
        }
      }, "Put-Thread");

      Thread getSizeThread = new Thread(new Runnable() {

        public void run() {
          try {
            int lastSize = 0;
            while (!stop) {
              int size = incoherentCache.getSize();
              if (size != lastSize && (size % 50) == 0) {
                lastSize = size;
                debug("Size: " + size);
              }
            }
          } catch (Throwable e) {
            throwables.add(e);
          } finally {
            stop = true;
          }
        }
      }, "Get-Size-Thread");

      putThread.start();
      getSizeThread.start();

      try {
        getSizeThread.join();
      } catch (InterruptedException e) {
        // ignore
      }
      if (throwables.size() > 0) {
        debug("There were problems in the test:");
        for (Throwable t : throwables) {
          t.printStackTrace();
        }
        throw new AssertionError("test failed");
      }
      debug("test finished successfully!");
    }

    private static void debug(String string) {
      System.out.println(Thread.currentThread().getName() + ": " + string);
    }
  }

}
