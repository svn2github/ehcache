/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.LockType;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

import junit.framework.Assert;

public class ServerMapLocalSizeTest extends AbstractCacheTestBase {

  public ServerMapLocalSizeTest(TestConfig testConfig) {
    super("/servermap/basic-servermap-test.xml", testConfig, ServerMapLocalSizeTestClient.class);
  }

  public static class ServerMapLocalSizeTestClient extends ClientBase {

    public ServerMapLocalSizeTestClient(String[] args) {
      super("testLocalSizeCache", args);
    }

    public static void main(String[] args) {
      new ServerMapLocalSizeTestClient(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {

      doTestLocalSize(cache);

      doTestReplace(cache);
    }

    private void doTestLocalSize(Cache cache) throws Throwable {
      final int maxElementsInMemory = 1000;
      cache.getStatistics().setSampledStatisticsEnabled(true);
      for (int i = 0; i < maxElementsInMemory; i++) {
        cache.put(new Element("key-" + i, "value-" + i));
      }

      System.out.println("Size: " + cache.getSize());
      System.out.println("In Memory size: " + cache.getStatistics().getCore().getLocalHeapSize());

      // eventual - can't assert size
      // Assert.assertEquals(maxElementsInMemory, cache.getSize());
      Assert.assertEquals(maxElementsInMemory, cache.getStatistics().getCore().getLocalHeapSize());
      Assert.assertEquals(maxElementsInMemory, cache.getMemoryStoreSize());

      for (int i = 1; i <= 100; i++) {
        cache.put(new Element("new-key-" + i, "new-value-" + i));

        // eventual - can't assert size
        // Assert.assertEquals(maxElementsInMemory + i, cache.getSize());
        // assert range as though eviction will happen, new puts can happen earlier before space becomes available (by
        // freeing meta-info mapping) in which case more key-value mapping are evicted
        long actual = cache.getStatistics().getCore().getLocalHeapSize();
        final int delta = 100;
        Assert.assertTrue("Failed at i=" + i + ", actual: " + actual, (maxElementsInMemory - delta) < actual
                                                                      && (actual - delta) <= maxElementsInMemory);
        actual = cache.getMemoryStoreSize();
        Assert.assertTrue("Failed at i=" + i + ", actual: " + actual, maxElementsInMemory - delta < actual
                                                                      && (actual - delta) <= maxElementsInMemory);
        Thread.sleep(100);
      }
    }

    private void doTestReplace(Cache cache) throws Throwable {
      cache.removeAll();
      System.out.println("Running replace test, size: " + cache.getSize());

      CacheLockProvider clp = (CacheLockProvider) cache.getInternalContext();
      String key = "replace-key";
      String value = "replace-value";
      clp.getSyncForKey(key).lock(LockType.WRITE);
      try {
        Element element = cache.get(key);
        Assert.assertNull("Element should be null", element);
        cache.put(new Element(key, value));
      } finally {
        clp.getSyncForKey(key).unlock(LockType.WRITE);
      }
      // MNK-3020, calling getSize() to make sure that no entries are SMLCImpl.pendingTransactionEntries
      cache.getSize();
      long newSize = cache.getMemoryStoreSize();
      System.out.println("After adding first time, size: " + newSize);
      Assert.assertEquals(1, newSize);
      Element element = null;
      clp.getSyncForKey(key).lock(LockType.WRITE);
      try {
        element = cache.get(key);
      } finally {
        clp.getSyncForKey(key).unlock(LockType.WRITE);
      }
      Assert.assertNotNull("Element cannot be null", element);
      Assert.assertEquals(key, element.getKey());
      Assert.assertEquals(value, element.getValue());

      String newValue = "new-replace-value";
      clp.getSyncForKey(key).lock(LockType.WRITE);
      try {
        cache.put(new Element(key, newValue));
      } finally {
        clp.getSyncForKey(key).unlock(LockType.WRITE);
      }

      newSize = cache.getMemoryStoreSize();
      System.out.println("After replace, size: " + newSize);
      Assert.assertEquals(1, newSize);
      element = cache.get(key);
      System.out.println("Element: " + element);
      Assert.assertNotNull("Element cannot be null", element);
      Assert.assertEquals(key, element.getKey());
      Assert.assertEquals(newValue, element.getValue());

    }
  }

}
