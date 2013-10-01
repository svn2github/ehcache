/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

/**
 * @author Abhishek Sanoujam
 */
public class GetSizeTest extends AbstractCacheTestBase {

  public GetSizeTest(TestConfig testConfig) {
    super("cache-coherence-test.xml", testConfig, App.class);
  }

  public static class App extends ClientBase {
    private volatile boolean stop = false;

    public App(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      final Cache incoherentCache = cacheManager.getCache("test");
      incoherentCache.setNodeCoherent(false);
      Assert.assertEquals(false, incoherentCache.isNodeCoherent());
      Assert.assertEquals(false, incoherentCache.isClusterCoherent());
      final List<Throwable> throwables = Collections.synchronizedList(new ArrayList<Throwable>());

      Thread putThread = new Thread(new Runnable() {

        @Override
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

        @Override
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
  }

}
