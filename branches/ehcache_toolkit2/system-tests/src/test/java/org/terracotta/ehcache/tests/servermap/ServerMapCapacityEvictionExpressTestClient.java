/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

import java.util.Date;

public class ServerMapCapacityEvictionExpressTestClient extends ServerMapClientBase {

  public ServerMapCapacityEvictionExpressTestClient(String[] args) {
    super("testWithEvictionMaxElements", args);
  }

  public static void main(String[] args) {
    new ServerMapCapacityEvictionExpressTestClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
    int size = cache.getSize();
    assertEquals(0, size);
    System.out.println("Client populating cache.");
    for (int i = 0; i < 8000; i++) {
      cache.put(new Element("key-" + i, "value-" + i));
    }

    System.out.println("Cache populated. size: " + cache.getSize());
    System.out.println("Sleeping for 3 mins (now=" + new Date() + ") ... ");
    // Wait a bit for the capacity evictor to do its thing.
    Thread.sleep(3 * 60 * 1000);

    System.out.println("After sleeping for 3 mins. Size: " + cache.getSize());
    // Now size should be the capacity
    assertRange(3000, 6000, cache);

  }
}
