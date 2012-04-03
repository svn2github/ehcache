/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

public class MemoryStoreEvictionPolicyTest extends AbstractCacheTestBase {
  public MemoryStoreEvictionPolicyTest(TestConfig testConfig) {
    super("memory-store-eviction-policy-test.xml", testConfig, MemoryStoreEvictionPolicyTestApp.class);
  }

  public static class MemoryStoreEvictionPolicyTestApp extends ClientBase {
    public static void main(String[] args) {
      new MemoryStoreEvictionPolicyTestApp(args).run();
    }

    public MemoryStoreEvictionPolicyTestApp(String[] args) {
      super("test", args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
      Cache fifo = new Cache(new CacheConfiguration("fifo", 1000)
          .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.FIFO).terracotta(new TerracottaConfiguration()));
      try {
        cacheManager.addCache(fifo);
        fail();
      } catch (IllegalArgumentException e) {
        // expected exception
      }

      Cache lfu = new Cache(new CacheConfiguration("lfu", 1000)
          .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU).terracotta(new TerracottaConfiguration()));
      try {
        cacheManager.addCache(lfu);
        fail();
      } catch (IllegalArgumentException e) {
        // expected exception
      }

      Cache lru = new Cache(new CacheConfiguration("lru", 1000)
          .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU).terracotta(new TerracottaConfiguration()));
      cacheManager.addCache(lru);

      Cache clock = new Cache(new CacheConfiguration("clock", 1000)
          .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.CLOCK).terracotta(new TerracottaConfiguration()));
      cacheManager.addCache(clock);

      Cache none = new Cache(new CacheConfiguration("none", 1000).terracotta(new TerracottaConfiguration()));
      cacheManager.addCache(none);

      none.getSize();
    }
  }
}
