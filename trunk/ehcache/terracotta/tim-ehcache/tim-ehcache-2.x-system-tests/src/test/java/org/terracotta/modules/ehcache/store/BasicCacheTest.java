/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;

import org.terracotta.modules.BasicTimInfo;
import org.terracotta.modules.TimInfo;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.CyclicBarrier;

public class BasicCacheTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractErrorCatchingTransparentApp {

    private final CyclicBarrier barrier = new CyclicBarrier(getParticipantCount());

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      final int index = barrier.await();

      CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-cache-test.xml"));

      Cache cache = cacheManager.getCache("test");

      // assert default storageStrategy OSS runtime is used
      Assert.assertEquals(StorageStrategy.DCV2, cache.getCacheConfiguration().getTerracottaConfiguration()
          .getStorageStrategy());

      // XXX: assert that the cache is clustered via methods on cache config (when methods exist)

      Assert.assertEquals(0, cache.getSize());

      barrier.await();

      if (index == 0) {
        cache.put(new Element("key", "value"));
      }

      barrier.await();

      Assert.assertEquals(1, cache.getSize());
      Assert.assertEquals("value", cache.get("key").getObjectValue());
      Assert.assertEquals(1, cache.getKeys().size());
      Assert.assertEquals("key", cache.getKeys().iterator().next());

      barrier.await();

      testKeysetMutations(cache);

      barrier.await();

      // make sure the cache is still valid after the key set mutations above
      Assert.assertEquals(1, cache.getSize());
      Assert.assertEquals("value", cache.get("key").getObjectValue());
      Assert.assertEquals(1, cache.getKeys().size());
      Assert.assertEquals("key", cache.getKeys().iterator().next());

      barrier.await();

      if (index == 0) {
        boolean removed = cache.remove("key");
        Assert.assertTrue(removed);
      }

      barrier.await();

      Assert.assertEquals(0, cache.getSize());
    }

    private void testKeysetMutations(Cache cache) {
      try {
        cache.getKeys().clear();
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected
      }

      try {
        cache.getKeys().add("sdf");
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected
      }

      try {
        cache.getKeys().add(0, "sdf");
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected
      }

      try {
        cache.getKeys().addAll(Collections.singletonList("sdfsfd"));
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected
      }

      try {
        cache.getKeys().addAll(0, Collections.singletonList("SDfsdf"));
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected
      }

      {
        Iterator iter = cache.getKeys().iterator();
        iter.next();
        try {
          iter.remove();
          fail();
        } catch (UnsupportedOperationException uoe) {
          // expected
        }
      }

      try {
        cache.getKeys().listIterator();
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected for now, but if listIterator() gets implemented this test should make sure you can't mutate the
        // cache through it)
      }

      try {
        cache.getKeys().listIterator(0);
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected for now, but if listIterator() gets implemented this test should make sure you can't mutate the
        // cache through it)
      }

      try {
        cache.getKeys().remove(0);
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected
      }

      assertTrue(cache.getKeys().contains("key"));
      try {
        cache.getKeys().remove("key");
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected
      }

      try {
        cache.getKeys().removeAll(Collections.singletonList("key"));
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected
      }

      assertFalse(cache.getKeys().contains("not in the cache!"));
      try {
        cache.getKeys().retainAll(Collections.singletonList("not in the cache!"));
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected
      }

      try {
        cache.getKeys().set(0, this);
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected
      }

      try {
        cache.getKeys().subList(0, 0);
        fail();
      } catch (UnsupportedOperationException uoe) {
        // expected for now, but if subList() gets implemented this test should make sure you can't mutate the
        // cache through it (or its further iterators!)
      }
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      config.getOrCreateSpec(App.class.getName()).addRoot("barrier", "barrier");

      String module_name = "tim-ehcache-2.x";
      TimInfo timInfo = new BasicTimInfo("org.terracotta.modules", module_name);
      config.addModule(timInfo.artifactId(), timInfo.version());
    }
  }

}
