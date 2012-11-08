/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import java.io.Serializable;

public abstract class BasicServerMapExpressTestHelper {

  private static final int NUM_ELEMENTS = 5000;

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
