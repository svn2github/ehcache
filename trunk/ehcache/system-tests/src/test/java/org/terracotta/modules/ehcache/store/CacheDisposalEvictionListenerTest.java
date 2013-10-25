package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import com.tc.objectserver.impl.ServerMapEvictionEngine;
import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

/**
 * @author Eugene Shelestovich
 */
public class CacheDisposalEvictionListenerTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 2;

  public CacheDisposalEvictionListenerTest(TestConfig testConfig) {
    super("evict-cache-test.xml", testConfig, App.class, App.class);
    testConfig.getClientConfig().setParallelClients(true);
    testConfig.getL2Config().setMaxHeap(192);
    testConfig.addTcProperty(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED, "true");
    configureTCLogging(ServerMapEvictionEngine.class.getName(), LogLevel.DEBUG);
  }

  public static class App extends ClientBase implements CacheEventListener {

    private final ToolkitBarrier barrier;
    private final ToolkitAtomicLong sharedEvictionsCount;
    private final AtomicLong localEvictionsCount;

    public App(String[] args) {
      super("disposal-test", args);
      this.sharedEvictionsCount = getClusteringToolkit().getAtomicLong("testLong");
      this.barrier = getClusteringToolkit().getBarrier("testBarrier", NODE_COUNT);
      this.localEvictionsCount = new AtomicLong(0);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(final Cache cache, final Toolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();
      cache.getCacheEventNotificationService().registerListener(this);
      Assert.assertEquals(0, cache.getSize());
      barrier.await();

      // add 1000 elements
      final int numOfElements = 1000;
      if (index == 0) {
        for (int i = 0; i < numOfElements; i++) {
          cache.put(new Element(i, "value"));
        }
      }
      barrier.await();

      // wait for 500 evictions
      waitForAllCurrentTransactionsToComplete(cache);
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          debug("Evictions so far: " + sharedEvictionsCount.get());
          return sharedEvictionsCount.get() >= 500;
        }
      });
      Assert.assertTrue(cache.getSize() <= 500);
      Assert.assertTrue(sharedEvictionsCount.get() >= 500);
      barrier.await();

      // got evictions = holding the lock
      final boolean holdingLock = localEvictionsCount.get() > 0;
      debug("Is holding lock ? " + holdingLock);

      // dispose cache locally, second client should pick up the lead and register eviction listener
      if (holdingLock) {
        Assert.assertTrue(localEvictionsCount.get() >= 500);
        // dispose in a separate thread
        final Thread t = new Thread(new Runnable() {
          @Override
          public void run() {
            cacheManager.removeCache(cache.getName());
          }
        });
        t.start();
        t.join();
      } else {
        // the other client is not supposed to get any evictions
        Assert.assertEquals(0, localEvictionsCount.get());
      }
      barrier.await();

      // reset all counters
      localEvictionsCount.set(0);
      sharedEvictionsCount.set(0);
      barrier.await();

      if (!holdingLock) {
        // add 500 more to trigger post-disposal eviction notifications
        for (int i = numOfElements; i < numOfElements + 500; i++) {
          cache.put(new Element(i, "value"));
        }

        waitForAllCurrentTransactionsToComplete(cache);
        WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            debug("Evictions so far: " + sharedEvictionsCount.get());
            return sharedEvictionsCount.get() >= 500 && localEvictionsCount.get() >= 500;
          }
        });
      }
      barrier.await();


      Assert.assertTrue(sharedEvictionsCount.get() >= 500);
      if (holdingLock) {
        // no new notifications for disposed one
        Assert.assertEquals(0, localEvictionsCount.get());
      } else {
        Assert.assertTrue(cache.getSize() <= 500);
        Assert.assertTrue(localEvictionsCount.get() >= 500);
      }

      debug("Local evictions count: " + localEvictionsCount.get());
    }

    @Override
    public void dispose() {
      // don't care
    }

    @Override
    public void notifyElementEvicted(Ehcache cache, Element element) {
      System.out.println("Element [" + element + "] evicted");
      sharedEvictionsCount.incrementAndGet();
      localEvictionsCount.incrementAndGet();
    }

    @Override
    public void notifyElementExpired(Ehcache cache, Element element) {
      // don't care
    }

    @Override
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
      // don't care
    }

    @Override
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
      // don't care
    }

    @Override
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
      // don't care
    }

    @Override
    public void notifyRemoveAll(Ehcache cache) {
      // don't care
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }
}
