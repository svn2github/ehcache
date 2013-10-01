/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;

public class BasicServerMapExpressCacheTest extends AbstractCacheTestBase {

  public BasicServerMapExpressCacheTest(TestConfig testConfig) {
    super("/servermap/basic-servermap-test.xml", testConfig, BasicServerMapExpressCacheTestClient.class,
          BasicServerMapExpressCacheTestClient.class);
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
    protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
      int index = getBarrierForAllClients().await();
      if (index == 0) {
        System.out.println("Client-" + index + ": populating cache");
        populateCache(cache);
      } else {
        System.out.println("Client-" + index + ": waiting for other node to populate cache");
      }
      getBarrierForAllClients().await();
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
