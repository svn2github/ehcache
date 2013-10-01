/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheWriterConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;

import org.junit.Assert;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.modules.ehcache.async.AsyncCoordinatorImpl;
import org.terracotta.toolkit.Toolkit;

import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.test.config.model.TestConfig;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemovingCacheWriteBehindTest extends AbstractCacheTestBase {

  public RemovingCacheWriteBehindTest(TestConfig testConfig) {
    super("basic-writebehind-test.xml", testConfig, App.class);
    configureTCLogging(AsyncCoordinatorImpl.class.getName(), LogLevel.DEBUG);
  }

  public static class App extends ClientBase {
    private static int NUM_ELEMENTS = 10000;

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
        @Override
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
      for (int i = 0; i < NUM_ELEMENTS; i++) {
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
      Assert.assertNotNull("Cause for putWithWriter shouldn't be null!", cause);
      Assert.assertEquals("Cause for putWithWriter shouldn't be that?!", cause.getClass().getName(), IllegalStateException.class.getName());
      testAddSameNameCache(cache.getName());
    }

    private void testAddSameNameCache(String cacheName) {
      Cache newCache = createCache(cacheName, cacheManager, Consistency.EVENTUAL);
      WriteBehindCacheWriter writer = new WriteBehindCacheWriter("WriteBehindCacheWriter for Cache " + cacheName,
                                                                 0, 20L, true);
      newCache.registerCacheWriter(writer);
      for (int i = 0; i < NUM_ELEMENTS / 100; i++) {
        newCache.putWithWriter(new Element("key" + i, "value" + i));
      }
    }

    private Cache createCache(String cacheName, CacheManager cm, Consistency consistency) {
      CacheConfiguration cacheConfig = new CacheConfiguration();
      cacheConfig.setName(cacheName);
      cacheConfig.setMaxEntriesLocalHeap(NUM_ELEMENTS);
      cacheConfig.cacheWriter(new CacheWriterConfiguration()
          .writeMode(CacheWriterConfiguration.WriteMode.WRITE_BEHIND));

      TerracottaConfiguration tcConfiguration = new TerracottaConfiguration();
      tcConfiguration.setConsistency(consistency);
      cacheConfig.addTerracotta(tcConfiguration);

      Cache cache = new Cache(cacheConfig);
      cm.addCache(cache);
      return cache;
    }

  }
}
