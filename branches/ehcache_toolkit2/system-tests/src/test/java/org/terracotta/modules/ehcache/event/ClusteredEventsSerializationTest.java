/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.event;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;
import java.util.Set;

import junit.framework.Assert;

public class ClusteredEventsSerializationTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 5;

  public ClusteredEventsSerializationTest(TestConfig testConfig) {
    super("clustered-events-test.xml", testConfig, App.class, App.class, App.class, App.class, App.class);
    testConfig.addTcProperty("ehcache.clusteredStore.checkContainsKeyOnPut", "true");
  }

  public static class App extends ClientBase {
    private final ToolkitBarrier barrier;

    public App(String[] args) {
      super("testSerialization", args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", NODE_COUNT);
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      Assert.assertEquals(0, cache.getSize());

      barrier.await();

      NonPortable key = new NonPortable("key" + index);
      NonPortable valuePut = new NonPortable("value" + index);
      NonPortable valueUpdate = new NonPortable("valueUpdated" + index);
      cache.put(new Element(key, valuePut));
      cache.put(new Element(key, valueUpdate));
      cache.remove(key);

      barrier.await();

      cache.removeAll();

      barrier.await();

      Thread.sleep(10000);

      EhcacheTerracottaEventListener listener = null;
      Set<CacheEventListener> listeners = cache.getCacheEventNotificationService().getCacheEventListeners();
      for (CacheEventListener l : listeners) {
        if (l instanceof EhcacheTerracottaEventListener) {
          listener = (EhcacheTerracottaEventListener) l;
          break;
        }
      }

      Assert.assertNotNull(listener);

      Assert.assertEquals(NODE_COUNT, listener.getPut().size());
      Assert.assertEquals(NODE_COUNT, listener.getUpdate().size());
      Assert.assertEquals(NODE_COUNT, listener.getRemove().size());
      Assert.assertEquals(NODE_COUNT, listener.getRemoveAll());

      boolean foundPutKey = false;
      for (Element element : listener.getPut()) {
        if (element.getObjectKey().equals(key)) {
          foundPutKey = true;
          Assert.assertEquals(valuePut, element.getObjectValue());
        } else {
          Assert.assertEquals("value" + element.getObjectKey().toString().substring("key".length()), element
              .getObjectValue().toString());
        }
      }
      Assert.assertTrue(foundPutKey);

      boolean foundUpdateKey = false;
      for (Element element : listener.getUpdate()) {
        if (element.getObjectKey().equals(key)) {
          foundUpdateKey = true;
          Assert.assertEquals(valueUpdate, element.getObjectValue());
        } else {
          Assert.assertEquals("valueUpdated" + element.getObjectKey().toString().substring("key".length()), element
              .getObjectValue().toString());
        }
      }
      Assert.assertTrue(foundUpdateKey);

      boolean foundRemoveKey = false;
      for (Element element : listener.getRemove()) {
        if (element.getObjectKey().equals(key)) {
          foundRemoveKey = true;
        }
      }
      Assert.assertTrue(foundRemoveKey);
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
