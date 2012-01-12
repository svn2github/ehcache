/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.AbstractStandaloneCacheTest;
import org.terracotta.ehcache.tests.ClientBase;

import java.io.Serializable;

public class BasicServerMapExpressCacheTest extends AbstractStandaloneCacheTest {

  public BasicServerMapExpressCacheTest() {
    super("/servermap/basic-servermap-test.xml", BasicServerMapExpressCacheTestClient.class,
          BasicServerMapExpressCacheTestClient.class);
    setParallelClients(true);
  }

  public static class BasicServerMapExpressCacheTestClient extends ClientBase {

    private static final int NUM_ELEMENTS = 5000;

    public BasicServerMapExpressCacheTestClient(String[] args) {
      super("test", args);
    }

    public static void main(String[] args) {
      new BasicServerMapExpressCacheTestClient(args).run();
    }

    @Override
    protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
      Barrier barrier = toolkit.getBarrier("test-barrier", 2);
      int index = barrier.await();
      if (index == 0) {
        System.out.println("Client-" + index + ": populating cache");
        populateCache(cache);
      } else {
        System.out.println("Client-" + index + ": waiting for other node to populate cache");
      }
      barrier.await();
      System.out.println("Client-" + index + ": verifying cache");
      assertValuesInCache(cache);
    }

    public static void populateCache(Cache cache) {
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        cache.put(new Element(getKey(i), getValue(i)));
      }

    }

    private static Serializable getKey(int i) {
      return "key-" + i;
    }

    private static Serializable getValue(int i) {
      return "value-" + i;
    }

    public static void assertValuesInCache(Cache cache) {
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Element element = cache.get(getKey(i));

        if (element == null) { throw new AssertionError("element for key=" + getKey(i) + " should not be null"); }

        Object value = element.getObjectValue();
        Serializable expectedValue = getValue(i);
        if (!expectedValue.equals(value)) { throw new AssertionError("Expected value: " + expectedValue + " Actual: "
                                                                     + value); }
      }
    }

  }

}
