/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

import java.util.Date;

import junit.framework.Assert;

public class ServerMapL1CapacityExpirationExpressTestClient extends ServerMapClientBase {

  public ServerMapL1CapacityExpirationExpressTestClient(final String[] args) {
    super("testWithMaxElementsInMemoryExpiration", args);
  }

  public static void main(final String[] args) {
    new ServerMapL1CapacityExpirationExpressTestClient(args).run();
  }

  @Override
  protected void runTest(final Cache cache, final Toolkit clusteringToolkit) throws Throwable {
    assertLocalCache(true);
    final int size = cache.getSize();
    assertEquals(0, size);
    System.out.println("Client populating cache.");
    for (int i = 1; i <= 4999; i++) {
      cache.put(new Element(new Integer(i), "value-" + i));
    }
    Thread.sleep(1000);
    System.out.println("Cache populated. size: " + cache.getSize() + " inMemorySize: " + cache.getStatistics().getLocalHeapSize());
    while (cache.getSize() != 4999) {
      Thread.sleep(100);
    }
    while (cache.getStatistics().getLocalHeapSize() != 4999) {
      Thread.sleep(100);
    }
    Assert.assertEquals(4999, cache.getSize());
    Assert.assertEquals(4999, cache.getStatistics().getLocalHeapSize());

    System.out.println("Sleeping for 35 secs (now=" + new Date() + ")");
    // Sleep for 35 secs to expire the items
    Thread.sleep(35 * 1000);

    // Add more to kick in evictor that expires old entries.
    // exceeding by even 1 element will initiate eviction
    // 4999 elements should be removed, cache may contain 0 to 2 elements in memory max
    // should have 2 elements in cluster
    System.out.println("Adding 100 more elements");
    for (int i = 5000; i <= 5100; i++) {
      cache.put(new Element(new Integer(i), "value-" + i));
    }
    // sleep for some time so that some recalls and removes happen
    Thread.sleep(10000);
    System.out.println("After adding 100 more elements. size: " + cache.getSize() + " inMemorySize: "
                       + cache.getStatistics().getLocalHeapSize());
    // size should decrease as entries are removed
    // assume minimum 100 removed in server already, monkeys may be unhappy here
    // Assert.assertEquals(5100, cache.getSize());
    assertRange((int)cache.getStatistics().getLocalHeapSize(), 5100, cache.getSize());

    // memory store size should decrease by some amount (locks should have been recalled for some)
    // assume minimum 100, monkeys may behave otherwise sometimes
    assertRange(0, 5010, cache.getStatistics().getLocalHeapSize());
    for (int i = 5000; i <= 5100; i++) {
      Element element = cache.get(new Integer(i));
      Assert.assertNotNull(element);
      Assert.assertEquals("value-" + i, element.getValue());
    }

    // Eviction could happen
    assertRange((int)cache.getStatistics().getLocalHeapSize(), 5100, cache.getSize());
    // Assert.assertEquals(5100, cache.getSize());
  }

  private void assertRange(final int min, final int max, final long actual) {
    assertTrue("assert range failed: min: " + min + " max: " + max + " actual: " + actual, min <= actual
                                                                                           && actual <= max);
  }

  private void assertLocalCache(final boolean enabled) throws Exception {
    final String expected = enabled ? "true" : "false";
    final String property = System.getProperty("com.tc.ehcache.storageStrategy.dcv2.localcache.enabled");
    if (property == null || !expected.equalsIgnoreCase(property)) { throw new Exception(
                                                                                        "This client needs to be run with local cache enabled"); }
  }

}
