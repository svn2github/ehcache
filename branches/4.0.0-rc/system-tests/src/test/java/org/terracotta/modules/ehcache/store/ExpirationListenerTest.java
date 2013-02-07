/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;
import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.atomic.AtomicLong;
import junit.framework.Assert;

public class ExpirationListenerTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 1;

  public ExpirationListenerTest(TestConfig testConfig) {
    super("expire-cache-test.xml", testConfig, App.class);
  }

  public static class App extends ClientBase implements CacheEventListener {

    private final AtomicLong localExpiredCount = new AtomicLong();
    private final ToolkitBarrier barrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      cache.getCacheEventNotificationService().registerListener(this);

      // XXX: assert that the cache is clustered via methods on cache config (when methods exist)

      Assert.assertEquals(0, cache.getSize());

      barrier.await();

      if (index == 0) {
        cache.put(new Element("key", "value"));
      }

      barrier.await();

      // make sure the item has been evicted
      int tries = 0;
      while (cache.getSize() > 0 && tries < 3) {
        ThreadUtil.reallySleep(2 * 1000);
        tries++;
      }

      barrier.await();

      // To make sure L2 evicts the entry
      Assert.assertNull(cache.get("key"));
      // only assert local listener would notice eviction events
      if (index == 0) {
        Assert.assertEquals(1, localExpiredCount.get());
      }

      Assert.assertEquals(0, cache.getSize());
    }

    public void dispose() {
      // don't care
    }

    public void notifyElementEvicted(Ehcache cache, Element element) {
      System.out.println("Element [" + element + "] evicted");
    }

    public void notifyElementExpired(Ehcache cache, Element element) {
      localExpiredCount.incrementAndGet();
    }

    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
      // don't care
    }

    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
      // don't care
    }

    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
      // don't care
    }

    public void notifyRemoveAll(Ehcache cache) {
      // don't care
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }
  }

}
