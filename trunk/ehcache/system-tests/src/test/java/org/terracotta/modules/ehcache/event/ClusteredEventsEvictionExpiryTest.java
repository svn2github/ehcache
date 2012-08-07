/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.apache.log4j.Logger;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.modules.ehcache.cluster.TopologyListenerImpl;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;
import java.util.Set;

import junit.framework.Assert;

public class ClusteredEventsEvictionExpiryTest extends AbstractCacheTestBase {

  private static Logger    LOG        = Logger.getLogger(TopologyListenerImpl.class);
  private static final int NODE_COUNT = 5;

  public ClusteredEventsEvictionExpiryTest(TestConfig testConfig) {
    super("clustered-events-test.xml", testConfig, App.class, App.class, App.class, App.class, App.class);
    // FORGE-582
    timebombTest("2013-01-15");
  }

  public static class App extends ClientBase {
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
      testCache(cacheManager.getCache("testIdentityExpiry"));

    }

    private void testCache(Cache cache) throws Throwable {

      LOG.info("Testing cache: " + cache);

      final int index = barrier.await();

      EhcacheTerracottaEventListener listener = null;
      Set<CacheEventListener> listeners = cache.getCacheEventNotificationService().getCacheEventListeners();
      for (CacheEventListener l : listeners) {
        if (l instanceof EhcacheTerracottaEventListener) {
          listener = (EhcacheTerracottaEventListener) l;
          break;
        }
      }

      Assert.assertNotNull(listener);
      Assert.assertEquals(0, cache.getSize());

      barrier.await();
      LOG.info("Testing element TTL expiry.");

      NonPortable keyTTL = new NonPortable("key_TTL_" + index);
      NonPortable valueTTL = new NonPortable("value_TTL_" + index);
      Element elementToExpireTTL = new Element(keyTTL, valueTTL);
      elementToExpireTTL.setTimeToLive(4);
      cache.put(elementToExpireTTL);

      barrier.await();
      Thread.sleep(6000);
      Assert.assertNull(cache.get(keyTTL));

      barrier.await();
      Assert.assertEquals(NODE_COUNT, listener.getExpired().size());

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

      Thread.sleep(6000);
      Assert.assertNull(cache.get(keyTTI));
      barrier.await();

      Assert.assertEquals(NODE_COUNT * 2, listener.getExpired().size());

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

      LOG.info("Testing element in-memory eviction.");
      for (int i = 0; i < 10; i++)
        cache.put(new Element(new NonPortable("key_" + index), new NonPortable("value_" + index)));

      for (int i = 0; i < 10; i++)
        cache.get(new NonPortable("key_" + index));

      barrier.await();
      Assert.assertEquals(NODE_COUNT, listener.getEvicted().size());
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

      NonPortable that = (NonPortable) o;

      if (value != null ? !value.equals(that.value) : that.value != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return value != null ? value.hashCode() : 0;
    }
  }
}
