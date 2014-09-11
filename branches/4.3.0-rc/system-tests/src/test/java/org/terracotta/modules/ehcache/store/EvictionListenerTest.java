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
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

import java.util.concurrent.Callable;

import static org.terracotta.test.util.WaitUtil.waitUntilCallableReturnsTrue;

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
    private final ToolkitAtomicLong actualEvictionsCount;

    public App(String[] args) {
      super("test2", args);
      this.actualEvictionsCount = getClusteringToolkit().getAtomicLong("testLong");
      this.barrier = getClusteringToolkit().getBarrier("testBarrier", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(final Cache cache, final Toolkit clusteringToolkit) throws Throwable {
      final int numOfElements = 1000;
      final long maxEntriesInCache = cache.getCacheConfiguration().getMaxEntriesInCache();
      final long expectedEvictionsCount = numOfElements - maxEntriesInCache;
      final int index = barrier.await();

      cache.getCacheEventNotificationService().registerListener(this);
      // XXX: assert that the cache is clustered via methods on cache config (when methods exist)
      System.err.println(cache);
      assertEquals(0, cache.getSize());
      assertEquals(1, cache.getCacheConfiguration().getTerracottaConfiguration().getConcurrency());
      barrier.await();

      if (index == 0) {
        for (int i = 0; i < numOfElements; i++) {
          cache.put(new Element(i, "value"));
        }
      }
      barrier.await();

      waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          System.out.println("Client " + index + ", cache size so far: " + cache.getSize());
          return cache.getSize() == maxEntriesInCache; // it must evict exactly 600 elements, because concurrency = 1
        }
      });
      System.out.println("Client " + index + ", final cache size: " + cache.getSize());
      barrier.await();

      waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          System.out.println("Client " + index + ", evicted count so far: " + actualEvictionsCount.get());
          // NOTE! Only one client should ever receive eviction events, and we actually don't know which one
          return actualEvictionsCount.get() == expectedEvictionsCount;
        }
      });
      barrier.await();

      assertEquals(expectedEvictionsCount, this.actualEvictionsCount.get());
    }

    @Override
    public void dispose() {
      // don't care
    }

    @Override
    public void notifyElementEvicted(Ehcache cache, Element element) {
      System.out.println("Element [" + element + "] evicted");
      actualEvictionsCount.incrementAndGet();
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
