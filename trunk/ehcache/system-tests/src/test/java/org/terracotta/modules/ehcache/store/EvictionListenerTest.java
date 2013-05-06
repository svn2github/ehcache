/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
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

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

public class EvictionListenerTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 2;

  public EvictionListenerTest(TestConfig testConfig) {
    super("evict-cache-test.xml", testConfig, App.class, App.class);
//    testConfig.setDgcEnabled(true);
//    testConfig.setDgcIntervalInSec(10);
//    testConfig.getL2Config().setMaxHeap(1024);
    testConfig.addTcProperty(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED, "true");
  }

  public static class App extends ClientBase implements CacheEventListener {

    private final ToolkitBarrier barrier;
    private final AtomicLong localEvictedCount = new AtomicLong();

    public App(String[] args) {
      super("test2", args);
      this.barrier = getClusteringToolkit().getBarrier("testBarrier", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(final Cache cache, final Toolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      cache.getCacheEventNotificationService().registerListener(this);
      // XXX: assert that the cache is clustered via methods on cache config (when methods exist)
      System.err.println(cache);
      Assert.assertEquals(0, cache.getSize());
      barrier.await();

      int numOfElements = 1000;
      if (index == 0) {
        for (int i = 0; i < numOfElements; i++) {
          cache.put(new Element(i, "value"));
        }
      }
      barrier.await();

      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return cache.getSize() == 500;
        }
      });

      System.out.println("XXXX client" + index + " final size: " + cache.getSize());
      long evictedElements = numOfElements - cache.getSize();
      System.out.println("XXXX client" + index + ": " + localEvictedCount.get());
      Assert.assertEquals("XXXX client " + index + " failed.", evictedElements, localEvictedCount.get());
    }

    @Override
    public void dispose() {
      // don't care
    }

    @Override
    public void notifyElementEvicted(Ehcache cache, Element element) {
      System.out.println("Element [" + element + "] evicted");
      localEvictedCount.incrementAndGet();
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
