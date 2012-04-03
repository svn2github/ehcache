/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class TTICacheTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 3;

  public TTICacheTest(TestConfig testConfig) {
    super("tti-cache-test.xml", testConfig, App.class, App.class, App.class);
  }

  public static class App extends ClientBase {

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

      Cache cache1 = cacheManager.getCache("test1"); // TTI = 10s
      Cache cache2 = cacheManager.getCache("test2"); // TTI = 20s

      if (index == 0) {
        cache1.put(new Element("key", "value"));
        cache2.put(new Element("key", "value"));
      }

      barrier.await();

      Assert.assertEquals("value", cache1.get("key").getObjectValue());
      Assert.assertEquals("value", cache2.get("key").getObjectValue());

      barrier.await();
      long expiryOne = cache1.getQuiet("key").getExpirationTime();
      while (System.currentTimeMillis() < expiryOne - TimeUnit.SECONDS.toMillis(8)) {
        Thread.sleep(100);
      }

      Assert.assertEquals("value", cache1.get("key").getObjectValue());
      Assert.assertEquals("value", cache2.get("key").getObjectValue());
      Assert.assertEquals("value", cache1.get("key").getObjectValue());
      Assert.assertEquals("value", cache2.get("key").getObjectValue());
      Assert.assertEquals("value", cache1.get("key").getObjectValue());
      Assert.assertEquals("value", cache2.get("key").getObjectValue());

      barrier.await();
      expiryOne = cache1.getQuiet("key").getExpirationTime();
      while (System.currentTimeMillis() < expiryOne + TimeUnit.SECONDS.toMillis(3)) {
        Thread.sleep(100);
      }

      // (key, value) in cache1 should expire
      Assert.assertNull(cache1.get("key"));

      // triggering last access time only on one node
      if (index == 0) {
        Assert.assertEquals("value", cache2.get("key").getObjectValue());
      }

      Thread.sleep(2000);

      barrier.await();

      Assert.assertNull(cache1.get("key"));

      // checking that other nodes received the last access time update and don't expire the element
      if (index != 1) {
        Assert.assertNotNull(cache2.get("key"));
      }

      barrier.await();
      long expiryTwo = cache2.getQuiet("key").getExpirationTime();
      while (System.currentTimeMillis() < expiryTwo + TimeUnit.SECONDS.toMillis(3)) {
        Thread.sleep(100);
      }

      // (key, value) in cache2 should expire
      Assert.assertNull(cache1.get("key"));
      Assert.assertNull(cache2.get("key"));
    }

  }

}
