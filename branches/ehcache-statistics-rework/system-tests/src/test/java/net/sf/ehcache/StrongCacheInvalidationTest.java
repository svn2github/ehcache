/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache;

import net.sf.ehcache.store.TerracottaStore;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.modules.ehcache.bulkops.DummyObject;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import junit.framework.Assert;

public class StrongCacheInvalidationTest extends AbstractCacheTestBase {

  public StrongCacheInvalidationTest(TestConfig testConfig) {
    super("strong-cache-invalidation-test.xml", testConfig, StrongCacheInvalidationTestClient.class,
          StrongCacheInvalidationTestClient.class);
  }

  public static class StrongCacheInvalidationTestClient extends ClientBase {

    public static final int     NUM_ELEMENTS     = 2000;
    public static final String  VALUE_SUFFIX_ONE = "prefix1";
    public static final String  VALUE_SUFFIX_TWO = "prefix2";

    public StrongCacheInvalidationTestClient(String[] args) {
      super("strong-cache-invalidation", args);
      // TCLogging.getLogger(L1ServerMapLocalCacheManagerImpl.class).setLevel(LogLevelImpl.DEBUG);
    }

    public static void main(String[] args) {
      new StrongCacheInvalidationTestClient(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      final ToolkitBarrier barrier = getBarrierForAllClients();
      List<Cache> caches = new ArrayList<Cache>();
      Cache myCache = null;

      myCache = cacheManager.getCache("dcv2StrongWithStats");
      myCache.getStatistics().setStatisticsEnabled(true);
      caches.add(myCache);

      myCache = cacheManager.getCache("dcv2StrongWithoutStats");
      myCache.getStatistics().setStatisticsEnabled(false);
      caches.add(myCache);

      testCaches(caches, barrier, false);
    }

    private void testCaches(Collection<Cache> caches, ToolkitBarrier barrier, boolean isLiteral) throws Exception {
      for (Cache cache : caches) {
        testCache(cache, barrier, isLiteral);
      }
    }

    private void testCache(Cache cache, ToolkitBarrier barrier, boolean isLiteral) throws Exception {
      int index = barrier.await();

      if (index == 0) {
        System.out.println(index + " client is doing strong put");
        doPuts(cache, isLiteral, VALUE_SUFFIX_ONE);
      }

      barrier.await();

      if (index == 1) {
        System.out.println(index + " client is doing unlocked get");
        doUnlockedGets(cache, isLiteral, VALUE_SUFFIX_ONE);
      }

      barrier.await();

      if (index == 0) {
        System.out.println(index + " client is doing strong put again");
        doPuts(cache, isLiteral, VALUE_SUFFIX_TWO);
      }

      barrier.await();
      if (index == 1) {
        System.out.println(index + " client should get updated value eventually");
        doEventualGets(cache, isLiteral, VALUE_SUFFIX_TWO);
      }
    }

    private void doUnlockedGets(Cache cache, boolean isLiteral, String suffix) {
      TerracottaStore store = (TerracottaStore) cache.getStore();
      for (int i = 0; i < NUM_ELEMENTS; ++i) {
        Element element = store.get(getKey(i, isLiteral));
        // Element element = store.unlockedGet(getKey(i, isLiteral));
        Assert.assertEquals(getElement(i, isLiteral, suffix), element);
      }
    }

    private void doEventualGets(Cache cache, boolean isLiteral, String suffix) throws Exception {
      final TerracottaStore store = (TerracottaStore) cache.getStore();
      for (int i = 0; i < NUM_ELEMENTS; ++i) {
        final Object key = getKey(i, isLiteral);
        final Element expected = getElement(i, isLiteral, suffix);
        Element element = store.get(key);
        // Element element = store.unlockedGet(key);
        if (!element.equals(expected)) {
          // now keep trying until expected element is available (invalidations will come sometime soon)
          WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
              Element gotFromCache = store.unsafeGet(key);
              return gotFromCache.equals(expected);
            }
          });
        }
        element = store.unsafeGet(key);
        Assert.assertEquals(expected, element);
      }
    }

    private void doPuts(Cache cache, boolean isLiteral, String suffix) {
      for (int i = 0; i < NUM_ELEMENTS; ++i) {
        cache.put(getElement(i, isLiteral, suffix));
      }
    }

    private Element getElement(int i, boolean isLiteral, String valueSuffix) {
      return new Element(getKey(i, isLiteral), getValue(i, isLiteral, valueSuffix));
    }

    private Object getKey(int i, boolean isLiteral) {
      return isLiteral ? "key" + i : new DummyObject("key" + i, i);
    }

    private Object getValue(int i, boolean isLiteral, String valueSuffix) {
      return isLiteral ? "val" + i + "-" + valueSuffix : new DummyObject("val" + i + "-" + valueSuffix, i);
    }
  }
}
