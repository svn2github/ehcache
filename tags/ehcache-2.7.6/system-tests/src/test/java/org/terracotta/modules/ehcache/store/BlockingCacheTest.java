/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.BlockingCache;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;

import junit.framework.Assert;

/**
 * @author Alex Snaps
 */
public class BlockingCacheTest extends AbstractCacheTestBase {

  public BlockingCacheTest(TestConfig testConfig) {
    super("blocking-cache.xml", testConfig, App.class, App.class, App.class);
  }

  public static class App extends ClientBase {
    private static final String       KEY_1   = "funkyKey";
    private static final Serializable VALUE_1 = "A really cool value";
    private static final String       KEY_2   = "otherFunkyKey";
    private static final Serializable VALUE_2 = "Even cooler value";
    private static final String       KEY_3   = "theUeberFunkyKey";
    private static final Serializable VALUE_3 = "can't get any cooler value";
    private final ToolkitBarrier             barrier;

    public App(String[] args) {
      super("test1", args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", getParticipantCount());
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache testcache, Toolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      Ehcache cache = cacheManager.getCache("test1");
      Assert.assertNotNull("There should be a cache test from that manager!", cache);
      BlockingCache blockingCache = new BlockingCache(cache);
      cacheManager.replaceCacheWithDecoratedCache(cache, blockingCache);
      cache = cacheManager.getEhcache("test1");

      if (index == 0) {
        // Node 0 blocks all other read to the key
        Assert.assertNull("Key " + KEY_1 + " should not be present in the cache yet", cache.get(KEY_1));
        cache.put(new Element(KEY_2, VALUE_2));
      }

      barrier.await();

      if (index != 0) {
        // This call should block, until node 0 puts en element for KEY in the cache
        Element element = cache.get(KEY_1);
        Assert.assertNotNull("Node 0 should have put key " + KEY_1 + " in the cache", element);
        Assert.assertEquals("Value for key " + KEY_1 + " should be " + VALUE_1, VALUE_1, element.getValue());
      } else {
        Thread.sleep(2000); // Thinking about the meaning of life for a while
        cache.put(new Element(KEY_1, VALUE_1));
      }
      Element element = cache.get(KEY_2);
      Assert.assertNotNull(element);
      Assert.assertEquals("Value for key " + KEY_2 + " should be " + VALUE_2, VALUE_2, element.getValue());

      barrier.await();

      blockingCache.setTimeoutMillis(3000);
      if (index == 0) {
        // Should block all other get to the same key
        cache.get(KEY_3);
      }

      barrier.await();

      switch (index) {
        case 2:
          Thread.sleep(2000);
          Assert.assertNotNull(cache.get(KEY_3));
          break;
        case 1:
          try {
            cache.get(KEY_3);
            Assert.fail();
          } catch (CacheException e) {
            // We failed aquiring the lock
          }
          break;
        case 0:
          Thread.sleep(3500);
          cache.put(new Element(KEY_3, VALUE_3));
          break;
      }

      barrier.await();

      // This tests inline eviction (EHC-420)
      Thread.sleep(22000);

      switch (index) {
        case 0:
          Assert.assertNull(cache.get(KEY_3));
          break;
      }

      barrier.await();

      switch (index) {
        case 0:
          Thread.sleep(1500);
          cache.put(new Element(KEY_3, VALUE_3));
          break;
        default:
          Assert.assertNotNull(cache.get(KEY_3));
          Assert.assertEquals(VALUE_3, cache.get(KEY_3).getValue());
      }
    }

  }
}
