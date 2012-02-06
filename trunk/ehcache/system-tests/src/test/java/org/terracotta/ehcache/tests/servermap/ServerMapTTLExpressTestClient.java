/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

import java.util.Date;

import junit.framework.Assert;

public class ServerMapTTLExpressTestClient extends ServerMapClientBase {

  public ServerMapTTLExpressTestClient(String[] args) {
    super("testWithEvictionTTL", args);
  }

  public static void main(String[] args) {
    new ServerMapTTLExpressTestClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
    int size = cache.getSize();
    assertEquals(0, size);
    System.out.println("Client populating cache.");
    for (int i = 0; i < 7000; i++) {
      cache.put(new Element("key-" + i, "value-" + i));
    }
    System.out.println("Cache populate. Size: " + cache.getSize());
    // assert range as some may already have got evicted while populating
    // eviction is aggressive with new impl based on "evictUnexpiredEntries" with true by default
    assertRange(5000, 7000, cache);

    System.out.println("Sleeping for 3 mins (now=" + new Date() + ") ... ");
    // Sleep for TTI to kick in:
    Thread.sleep(3 * 60 * 1000);

    System.out.println("After sleeping 3 mins. Size: " + cache);
    // Now size should be equal to capacity
    assertRange(3000, 6000, cache);

    System.out.println("Trying to get on all elements, inline eviction should happen");
    // all others should be evicted inline
    for (int i = 0; i < 7000; i++) {
      Element element = cache.get("key-" + i);
      Assert.assertNull("Element should be null of key-" + i, element);
    }

    Thread.sleep(5 * 1000);

    System.out.println("After inline eviction. Size: " + cache.getSize());
    // Now size should be equal to 0
    assertEquals(0, cache.getSize());
  }

}
