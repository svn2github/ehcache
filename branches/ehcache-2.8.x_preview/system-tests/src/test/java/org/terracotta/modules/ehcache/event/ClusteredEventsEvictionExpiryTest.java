/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
import org.apache.log4j.Logger;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.modules.ehcache.cluster.TopologyListenerImpl;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.Callable;

import junit.framework.Assert;

public class ClusteredEventsEvictionExpiryTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 5;

  public ClusteredEventsEvictionExpiryTest(TestConfig testConfig) {
    super("clustered-events-test.xml", testConfig, App.class, App.class, App.class, App.class, App.class);
    // disable server expiration
    testConfig.addTcProperty(TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED, "false");
  }

  public static class App extends ClientBase {
    private static final Logger LOG = Logger.getLogger(TopologyListenerImpl.class);
    private final ToolkitBarrier barrier;

    public App(String[] args) {
      super("testSerializationExpiry", args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      testCache(cacheManager.getCache("testSerializationExpiry"));
    }

    private void testCache(final Cache cache) throws Throwable {
      LOG.info("Testing cache: " + cache);
      final int index = barrier.await();

      Assert.assertEquals(0, cache.getSize());
      final EhcacheTerracottaEventListener listener = getEhcacheTerracottaEventListener(cache);
      barrier.await();

      LOG.info("Testing element TTL expiry.");
      NonPortable keyTTL = new NonPortable("key_TTL_" + index);
      NonPortable valueTTL = new NonPortable("value_TTL_" + index);
      Element elementToExpireTTL = new Element(keyTTL, valueTTL);
      elementToExpireTTL.setTimeToLive(4);
      cache.put(elementToExpireTTL);
      barrier.await();

      Thread.sleep(5000);
      Assert.assertNull(cache.get(keyTTL));
      barrier.await();

      // since server expiration is disabled, client expiration should notify only one client
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          LOG.info("TTL Expiration. Expired so far: " + listener.getExpired().size() + ", evicted so far: "
                   + listener.getEvicted().size());
          return (listener.getExpired().size() == NODE_COUNT) && (listener.getEvicted().size() == 0);
        }
      });

      boolean foundExpiredKey = false;
      for (Element element : listener.getPut()) {
        if (element.getObjectKey().equals(keyTTL)) {
          foundExpiredKey = true;
          Assert.assertEquals(valueTTL, element.getObjectValue());
        } else {
          Assert.assertEquals("value" + element.getObjectKey().toString().substring("key".length()), element
              .getObjectValue().toString());
        }
      }
      Assert.assertTrue(foundExpiredKey);
      barrier.await();

      LOG.info("Testing element TTI expiry.");
      NonPortable keyTTI = new NonPortable("key_TTI_" + index);
      NonPortable valueTTI = new NonPortable("value_TTI_" + index);
      Element elementToExpireTTI = new Element(keyTTI, valueTTI);
      elementToExpireTTI.setTimeToIdle(4);
      cache.put(elementToExpireTTI);
      barrier.await();

      Thread.sleep(5000);
      Assert.assertNull(cache.get(keyTTI));
      barrier.await();

      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          LOG.info("TTI Expiration. Expired so far: " + listener.getExpired().size() + ", evicted so far: "
                   + listener.getEvicted().size());
          return (listener.getExpired().size() == 2 * NODE_COUNT) && (listener.getEvicted().size() == 0);
        }
      });

      foundExpiredKey = false;
      for (Element element : listener.getPut()) {
        if (element.getObjectKey().equals(keyTTI)) {
          foundExpiredKey = true;
          Assert.assertEquals(valueTTI, element.getObjectValue());
        } else {
          Assert.assertEquals("value" + element.getObjectKey().toString().substring("key".length()), element
              .getObjectValue().toString());
        }
      }
      Assert.assertTrue(foundExpiredKey);
      barrier.await();
      Assert.assertEquals(0, cache.getSize());
    }

    private static EhcacheTerracottaEventListener getEhcacheTerracottaEventListener(final Cache cache) {
      EhcacheTerracottaEventListener tmpListener = null;
      Set<CacheEventListener> listeners = cache.getCacheEventNotificationService().getCacheEventListeners();
      for (CacheEventListener l : listeners) {
        if (l instanceof EhcacheTerracottaEventListener) {
          tmpListener = (EhcacheTerracottaEventListener)l;
          break;
        }
      }

      Assert.assertNotNull(tmpListener);
      return tmpListener;
    }

  }

  public static class NonPortable implements Serializable {
    private final String value;

    public NonPortable(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      NonPortable that = (NonPortable)o;

      if (value != null ? !value.equals(that.value) : that.value != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return value != null ? value.hashCode() : 0;
    }
  }
}
