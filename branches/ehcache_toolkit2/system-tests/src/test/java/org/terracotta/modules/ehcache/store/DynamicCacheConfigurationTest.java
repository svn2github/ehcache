/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import static java.util.concurrent.TimeUnit.SECONDS;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;

import org.junit.Assert;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;
import com.tc.util.CallableWaiter;

import java.util.Random;
import java.util.concurrent.Callable;

/**
 * @author cdennis
 */
public class DynamicCacheConfigurationTest extends AbstractCacheTestBase {
  public DynamicCacheConfigurationTest(TestConfig testConfig) {
    super(testConfig, App.class);
    testConfig.addTcProperty(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL, "1000");
    testConfig.addTcProperty(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED, "true");
  }

  public static class App extends ClientBase {
    private static final double TOLERANCE = 0.1;

    public App(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      testTTIChange(cacheManager);
      testTTLChange(cacheManager);
      testDiskCapacityChange(cacheManager);
      testMemoryCapacityChange(cacheManager);
      testTTIChangeWithCustomElements(cacheManager);
      testTTLChangeWithCustomElements(cacheManager);
    }

    private Cache createCache(String cacheName, int maxMemory, boolean eternal, long ttl, long tti) {
      return new Cache(new CacheConfiguration(cacheName, maxMemory)
          .eternal(eternal)
          .timeToLiveSeconds(ttl)
          .timeToIdleSeconds(tti)
          .clearOnFlush(true)
          .terracotta(new TerracottaConfiguration().clustered(true).consistency(Consistency.STRONG)
                          .valueMode(TerracottaConfiguration.ValueMode.SERIALIZATION).coherentReads(true)
                          .orphanEviction(true).orphanEvictionPeriod(4).localKeyCache(false).localKeyCacheSize(0)
                          .copyOnRead(false)).logging(true));
    }

    private void testTTIChange(CacheManager manager) throws InterruptedException {
      Cache cache = createCache("testTTIChange", 10, false, 0, 10);
      manager.addCache(cache);

      cache.put(new Element("key1", new byte[0]));
      cache.put(new Element("key2", new byte[0]));

      SECONDS.sleep(6);

      cache.get("key2");

      SECONDS.sleep(6);

      Assert.assertNull(cache.get("key1"));
      Assert.assertNotNull(cache.get("key2"));

      cache.getCacheConfiguration().setTimeToIdleSeconds(20);

      cache.put(new Element("key1", new byte[0]));

      SECONDS.sleep(15);

      Assert.assertNotNull(cache.get("key1"));
      Assert.assertNotNull(cache.get("key2"));

      SECONDS.sleep(25);

      Assert.assertNull(cache.get("key1"));
      Assert.assertNull(cache.get("key2"));

      cache.getCacheConfiguration().setTimeToIdleSeconds(4);

      cache.put(new Element("key1", new byte[0]));
      cache.put(new Element("key2", new byte[0]));

      SECONDS.sleep(8);

      Assert.assertNull(cache.get("key1"));
      Assert.assertNull(cache.get("key2"));
    }

    private void testTTLChange(CacheManager cm) throws InterruptedException {
      Cache cache = createCache("testTTLChange", 10, false, 10, 0);
      cm.addCache(cache);

      cache.put(new Element("key1", new byte[0]));

      SECONDS.sleep(6);

      Assert.assertNotNull(cache.get("key1"));
      cache.put(new Element("key2", new byte[0]));

      SECONDS.sleep(6);

      Assert.assertNull(cache.get("key1"));
      Assert.assertNotNull(cache.get("key2"));

      cache.getCacheConfiguration().setTimeToLiveSeconds(20);

      cache.put(new Element("key1", new byte[0]));

      SECONDS.sleep(8);

      Assert.assertNotNull(cache.get("key1"));
      Assert.assertNotNull(cache.get("key2"));

      SECONDS.sleep(8);

      Assert.assertNotNull(cache.get("key1"));
      Assert.assertNull(cache.get("key2"));

      SECONDS.sleep(10);

      Assert.assertNull(cache.get("key1"));

      cache.getCacheConfiguration().setTimeToLiveSeconds(4);

      cache.put(new Element("key1", new byte[0]));
      cache.put(new Element("key2", new byte[0]));

      SECONDS.sleep(8);

      Assert.assertNull(cache.get("key1"));
      Assert.assertNull(cache.get("key2"));
    }

    public void testTTIChangeWithCustomElements(CacheManager cm) throws InterruptedException {
      Cache cache = createCache("testTTIChangeWithCustomElements", 10, false, 0, 10);
      cm.addCache(cache);

      cache.put(new Element("default", new byte[0]));
      cache.put(new Element("eternal", new byte[0], true, 0, 0));
      cache.put(new Element("short", new byte[0], false, 1, 1));
      cache.put(new Element("long", new byte[0], true, 100, 100));

      SECONDS.sleep(6);

      Assert.assertNull(cache.get("short"));

      SECONDS.sleep(6);

      Assert.assertNull(cache.get("default"));
      Assert.assertNotNull(cache.get("eternal"));
      Assert.assertNull(cache.get("short"));
      Assert.assertNotNull(cache.get("long"));

      cache.getCacheConfiguration().setTimeToIdleSeconds(20);

      cache.put(new Element("default", new byte[0]));
      cache.put(new Element("short", new byte[0], false, 1, 1));

      SECONDS.sleep(15);

      Assert.assertNotNull(cache.get("default"));
      Assert.assertNotNull(cache.get("eternal"));
      Assert.assertNull(cache.get("short"));
      Assert.assertNotNull(cache.get("long"));

      SECONDS.sleep(25);

      Assert.assertNull(cache.get("default"));
      Assert.assertNotNull(cache.get("eternal"));
      Assert.assertNull(cache.get("short"));
      Assert.assertNotNull(cache.get("long"));

      cache.getCacheConfiguration().setTimeToIdleSeconds(4);

      cache.put(new Element("default", new byte[0]));
      cache.put(new Element("short", new byte[0], false, 1, 1));

      SECONDS.sleep(8);

      Assert.assertNull(cache.get("default"));
      Assert.assertNotNull(cache.get("eternal"));
      Assert.assertNull(cache.get("short"));
      Assert.assertNotNull(cache.get("long"));
    }

    public void testTTLChangeWithCustomElements(CacheManager cm) throws InterruptedException {
      Cache cache = createCache("testTTLChangeWithCustomElements", 10, false, 10, 0);
      cm.addCache(cache);

      cache.put(new Element("default", new byte[0]));
      cache.put(new Element("eternal", new byte[0], true, 0, 0));
      cache.put(new Element("short", new byte[0], false, 1, 1));
      cache.put(new Element("long", new byte[0], true, 100, 100));

      SECONDS.sleep(6);

      Assert.assertNotNull(cache.get("default"));
      Assert.assertNotNull(cache.get("eternal"));
      Assert.assertNull(cache.get("short"));
      Assert.assertNotNull(cache.get("long"));

      SECONDS.sleep(6);

      Assert.assertNull(cache.get("default"));
      Assert.assertNotNull(cache.get("eternal"));
      Assert.assertNull(cache.get("short"));
      Assert.assertNotNull(cache.get("long"));

      cache.getCacheConfiguration().setTimeToLiveSeconds(20);

      cache.put(new Element("default", new byte[0]));
      cache.put(new Element("short", new byte[0], false, 1, 1));

      SECONDS.sleep(6);

      Assert.assertNotNull(cache.get("default"));
      Assert.assertNotNull(cache.get("eternal"));
      Assert.assertNull(cache.get("short"));
      Assert.assertNotNull(cache.get("long"));

      SECONDS.sleep(6);

      Assert.assertNotNull(cache.get("default"));
      Assert.assertNotNull(cache.get("eternal"));
      Assert.assertNull(cache.get("short"));
      Assert.assertNotNull(cache.get("long"));

      SECONDS.sleep(10);

      Assert.assertNull(cache.get("default"));
      Assert.assertNotNull(cache.get("eternal"));
      Assert.assertNull(cache.get("short"));
      Assert.assertNotNull(cache.get("long"));

      cache.getCacheConfiguration().setTimeToLiveSeconds(4);

      cache.put(new Element("default", new byte[0]));
      cache.put(new Element("short", new byte[0], false, 1, 1));

      SECONDS.sleep(8);

      Assert.assertNull(cache.get("default"));
      Assert.assertNotNull(cache.get("eternal"));
      Assert.assertNull(cache.get("short"));
      Assert.assertNotNull(cache.get("long"));
    }

    private void testMemoryCapacityChange(CacheManager cm) throws Exception {
      final Cache cache = createCache("testMemoryCapacityChange", 100, true, 0, 0);
      cache.getCacheConfiguration().getTerracottaConfiguration().storageStrategy(StorageStrategy.DCV2)
          .consistency(TerracottaConfiguration.Consistency.STRONG);
      cm.addCache(cache);

      int i = 0;
      for (; i < 150; i++) {
        cache.put(new Element("key" + i, new byte[0]));
      }

      waitForCacheMemoryStoreSize(cache, 100);

      cache.getCacheConfiguration().setMaxEntriesLocalHeap(200);

      for (; i < 250; i++) {
        cache.put(new Element("key" + i, new byte[0]));
      }

      waitForCacheMemoryStoreSize(cache, 100, 200);

      cache.getCacheConfiguration().setMaxEntriesLocalHeap(50);

      for (; i < 350; i++) {
        cache.put(new Element("key" + i, new byte[0]));
      }

      waitForCacheMemoryStoreSize(cache, 50);
    }

    private void waitForCacheMemoryStoreSize(final Cache cache, final int lowerBound, final int upperBound)
        throws Exception {
      final int min = (int) ((1 - TOLERANCE) * lowerBound);
      final int max = (int) ((1 + TOLERANCE) * upperBound);
      CallableWaiter.waitOnCallable(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          if (cache.getMemoryStoreSize() <= max && cache.getMemoryStoreSize() >= min) { return true; }
          System.out.println("Still waiting for memory store size to fall in bounds [" + lowerBound + ", " + upperBound
                             + "] current=" + cache.getMemoryStoreSize());
          return false;
        }
      }, 30000);
    }

    private void waitForCacheMemoryStoreSize(final Cache cache, final int upperBound) throws Exception {
      waitForCacheMemoryStoreSize(cache, 0, upperBound);
    }

    public void testDiskCapacityChange(CacheManager cm) throws Exception {
      final Cache cache = createCache("testDiskCapacityChange", 10, true, 0, 0);
      cache.getCacheConfiguration().maxElementsOnDisk(100).getTerracottaConfiguration()
          .storageStrategy(StorageStrategy.DCV2).consistency(TerracottaConfiguration.Consistency.STRONG).concurrency(1);
      cm.addCache(cache);

      testCacheDiskCapacity(cache, 100);

      cache.getCacheConfiguration().setMaxElementsOnDisk(200);

      testCacheDiskCapacity(cache, 200);

      cache.getCacheConfiguration().setMaxElementsOnDisk(50);

      testCacheDiskCapacity(cache, 50);
    }

    private void testCacheDiskCapacity(final Cache cache, final int capacity) throws Exception {
      final Random r = new Random();
      for (int i = 0; i < 1000; i++) {
        cache.put(new Element("key" + i, new byte[0]));
      }

      // Wait some time for the remove set to get sent to the server.
      SECONDS.sleep(30);

      CallableWaiter.waitOnCallable(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          // Initiate capacity eviction with a new put
          cache.put(new Element("overflow" + r.nextInt(), new byte[0]));
          return cache.getSize() >= capacity * 0.9 && cache.getSize() <= capacity * 1.1;
        }
      }, 2 * 60 * 1000, 10 * 1000);
    }

  }
}
