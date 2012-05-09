/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.ehcache.tests.mbean.DSOMBean;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import junit.framework.Assert;

/**
 * This class is maintained in both tim-ehcache-2.0-ee-system-tests and ehcache-terracotta-ee-system-tests projects
 * (READ: any changes to this class should also be made to same class in other project)
 */
public abstract class ServerMapClearTestHelper {

  public static void doTest(Cache cache, Toolkit clusteringToolkit, DSOMBean dsoMBean) throws Throwable {
    // node 1 populates cache
    // both nodes asserts cache populated
    // node 2 clears cache
    // cache + localCache on both nodes are cleared

    int numElements = 10;
    ToolkitBarrier barrier = clusteringToolkit.getBarrier("clearTestBarrier", 2);
    int index = barrier.await();
    if (index == 0) {
      debug(index, "Populating cache");
      for (int i = 0; i < numElements; i++) {
        cache.put(new Element(getKey(i), getValue(i)));
      }
    } else {
      debug(index, "waiting for other node to populate cache");
    }
    barrier.await();

    debug(index, "Asserting cache is populated");
    if (index == 0) {
      long initialGetValueReqCount = getGlobalServerMapGetValueRequestsCount(dsoMBean);
      debug(index, "should hit local cache for this node");
      checkElements(index, dsoMBean, cache, numElements, initialGetValueReqCount, 0);

    }
    barrier.await();

    if (index != 0) {
      long initialGetValueReqCount = getGlobalServerMapGetValueRequestsCount(dsoMBean);
      debug(index, "should NOT hit local cache for this node");
      checkElements(index, dsoMBean, cache, numElements, initialGetValueReqCount, numElements);
    }

    barrier.await();
    if (index != 0) {
      debug(index, "Clearing cache");
      cache.removeAll();
      debug(index, "Cache cleared");
    } else {
      debug(index, "Waiting for other node to clear cache");
    }
    barrier.await();

    debug(index, "Asserting size 0 after cache clear");
    Assert.assertEquals("Cache size should be zero after clear", 0, cache.getSize());
    debug(index, "Size=0 asserted");

    barrier.await();
    debug(index, "Asserting both cache and local cache is cleared");
    for (int i = 0; i < numElements; i++) {
      String key = getKey(i);
      boolean isElementInMemory = cache.isElementInMemory(key);
      boolean isElementOnDisk = cache.isElementOnDisk(key);
      Element element = cache.get(key);

      // debug(index, "isElementInMemory: " + isElementInMemory + " isElementOnDisk: " + isElementOnDisk
      // + " elementIsNull:" + (element == null));

      Assert.assertFalse("Element for key: " + key + " should not be present in memory", isElementInMemory);
      Assert.assertFalse("Element for key: " + key + " should not be present on disk", isElementOnDisk);
      Assert.assertNull("Element for key: " + key + " should be null", element);
    }

  }

  private static void checkElements(int index, DSOMBean dsoMBean, Cache cache, int numElements,
                                    long initialGetValueReqCount, int requestsMade) {
    for (int i = 0; i < numElements; i++) {
      Element element = cache.get(getKey(i));
      Assert.assertNotNull(element);
      Assert.assertEquals(getValue(i), element.getObjectValue());
    }

    long actual = getGlobalServerMapGetValueRequestsCount(dsoMBean);
    String msg = "expecting (" + initialGetValueReqCount + "+" + requestsMade + ") "
                 + (initialGetValueReqCount + requestsMade) + " Actual: " + actual;
    debug(index, msg);
    Assert
        .assertEquals("GET_VALUE requests count should match: " + msg, initialGetValueReqCount + requestsMade, actual);
  }

  private static long getGlobalServerMapGetValueRequestsCount(DSOMBean dsoMBean) {
    return dsoMBean.getGlobalServerMapGetValueRequestsCount();
  }

  private static void debug(int index, String string) {
    System.out.println("Node-" + index + ": " + string);
  }

  private static String getValue(int i) {
    return "value-" + i;
  }

  private static String getKey(int i) {
    return "key-" + i;
  }

}
