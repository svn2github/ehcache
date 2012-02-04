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

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

public class PrimitiveClassTest extends AbstractCacheTestBase {
  private static final int NODE_COUNT = 3;

  public PrimitiveClassTest(TestConfig testConfig) {
    super("primitive-class-test.xml", testConfig, App.class, App.class, App.class);
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

      Set<Class<?>> types = new HashSet<Class<?>>();
      types.add(Void.TYPE);
      types.add(Boolean.TYPE);
      types.add(Byte.TYPE);
      types.add(Character.TYPE);
      types.add(Double.TYPE);
      types.add(Float.TYPE);
      types.add(Integer.TYPE);
      types.add(Long.TYPE);
      types.add(Short.TYPE);

      if (index == 0) {
        for (Class<?> c : types) {
          cache.put(new Element(c, c));
        }
      }

      barrier.await();

      for (Class<?> c : types) {
        assertEquals(c, cache.get(c).getObjectValue());
        assertEquals(c, cache.get(c).getObjectKey());
      }

      Set<Class<?>> copy = new HashSet<Class<?>>(types);
      for (Object o : cache.getKeys()) {
        boolean removed = copy.remove(o);
        if (!removed) { throw new AssertionError("did not remove: " + o); }
      }

      Assert.assertEquals(copy.toString(), 0, copy.size());

    }

  }

}
