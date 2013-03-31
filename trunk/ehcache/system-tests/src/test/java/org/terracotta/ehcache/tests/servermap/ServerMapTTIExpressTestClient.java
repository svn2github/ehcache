/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

import java.util.Date;

import junit.framework.Assert;

public class ServerMapTTIExpressTestClient extends ServerMapClientBase {

  public ServerMapTTIExpressTestClient(String[] args) {
    super("testWithEvictionTTI", args);
  }

  public static void main(String[] args) {
    new ServerMapTTIExpressTestClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
    int size = cache.getSize();
    assertEquals(0, size);
    System.out.println("Client populating cache.");
    for (int i = 0; i < 7000; i++) {
      cache.put(new Element("key-" + i, "value-" + i));
    }
    System.out.println("Cache populate. Size: " + cache.getSize());
    // assert range as some may already have got evicted while populating
    assertRange(5000, 7000, cache);

    System.out.println("Sleeping for 1 mins (now=" + new Date() + ") ... ");
    Thread.sleep(60 * 1000);
    // Sleep for TTI to kick in:
    // Wait up to 30 sec. for the capacity evictor to do its thing.
    int count = 0;
    while ( cache.getSize() > 6000 && count++ < 60) {
        Thread.sleep(5000);
        System.out.println("Cache populated. size: " + cache.getSize());
    }
    
    System.out.println("After sleeping 3 mins. Size: " + cache.getSize());
    // Now size should be <= capacity
    assertRange(3000, 6000, cache);

    System.out.println("Trying to get on all elements, inline eviction should happen");
    // all others should be evicted inline
    for (int i = 0; i < 7000; i++) {
      Element element = cache.get("key-" + i);
      Assert.assertNull("Element should be null of key-" + i, element);
    }

    Thread.sleep(5000);

    System.out.println("After inline eviction. Size: " + cache.getSize());
    // Now size should be equal to 0
    assertEquals(0, cache.getSize());
  }

}
