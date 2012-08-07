/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemovingCacheWriteBehindTest extends AbstractCacheTestBase {

  public RemovingCacheWriteBehindTest(TestConfig testConfig) {
    super("basic-writebehind-test.xml", testConfig, App.class);
  }

  public static class App extends ClientBase {

    public App(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(final Cache cache, Toolkit clusteringToolkit) throws Throwable {

      final CyclicBarrier localBarrier = new CyclicBarrier(2);

      WriteBehindCacheWriter writer;

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

      writer = new WriteBehindCacheWriter("WriteBehindCacheWriter for Cache " + cache.getName(), 0, 20L, true);
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
            // Thrown when we only got to shutdown the AsyncWriteBehind
          e.printStackTrace();
          cause = e.getCause();
          break;
        } catch (IllegalStateException e) {
            // Thrown when the 'entire' Cache is shutdown already
          e.printStackTrace();
          cause = e;
          break;
        }
      }
      assertThat("Cause for putWithWriter shouldn't be null!", cause, notNullValue());
      assertThat("Cause for putWithWriter shouldn't be that?!", cause.getClass().getName(),
                 equalTo(IllegalStateException.class.getName()));
    }

  }
}
