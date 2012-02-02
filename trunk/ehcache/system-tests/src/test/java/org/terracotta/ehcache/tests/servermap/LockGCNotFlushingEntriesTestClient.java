/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class LockGCNotFlushingEntriesTestClient extends ServerMapClientBase {

  public LockGCNotFlushingEntriesTestClient(String[] args) {
    super("testLockGC", args);
  }

  public static void main(String[] args) {
    new LockGCNotFlushingEntriesTestClient(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
    int size = cache.getSize();
    assertEquals(0, size);
    System.out.println("Client populating cache.");
    for (int i = 0; i < 1000; i++) {
      cache.put(new Element("key-" + i, "value-" + i));
    }

    System.out.println("Cache populated. Sleeping for 120 secs. size: " + cache.getSize() + " inMemorySize: "
                       + cache.getMemoryStoreSize());
    Thread.sleep(120 * 1000);

    System.out.println("After sleeping 120 secs. size: " + cache.getSize() + " inMemorySize: "
                       + cache.getMemoryStoreSize());
    // assert range as some may have got evicted while populating cache
    assertTrue(1000 == cache.getSize());
    assertTrue(1000 == cache.getMemoryStoreSize());
  }
}
