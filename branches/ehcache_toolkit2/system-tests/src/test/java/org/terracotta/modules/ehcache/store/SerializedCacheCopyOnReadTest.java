/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;

import junit.framework.Assert;

public class SerializedCacheCopyOnReadTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 3;

  public SerializedCacheCopyOnReadTest(TestConfig testConfig) {
    super("basic-copy-on-read-cache-test.xml", testConfig, App.class, App.class, App.class);
  }

  public static class App extends ClientBase {
    private final Barrier barrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("test-barrier", NODE_COUNT);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      final int index = barrier.await();

      Assert.assertEquals(0, cache.getSize());

      barrier.await();

      if (index == 0) {
        ValueHolder value = new ValueHolder("value");
        cache.put(new Element("key", value));
      }

      barrier.await();

      ValueHolder value = (ValueHolder) cache.get("key").getObjectValue();
      Assert.assertEquals(1, cache.getSize());
      Assert.assertEquals("value", value.getData());

      // test that copyOnRead works
      ValueHolder value2 = (ValueHolder) cache.get("key").getObjectValue();
      Assert.assertTrue(value != value2);
      Assert.assertEquals("value", value2.getData());
    }

  }

  public static class ValueHolder implements Serializable {
    private String data;

    public ValueHolder(final String data) {
      setData(data);
    }

    public synchronized String getData() {
      return data;
    }

    public synchronized void setData(final String data) {
      this.data = data;
    }
  }

}
