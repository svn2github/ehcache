/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class ServerMapL1EvictionOffHeapDestroyExpressTestClient extends ServerMapClientBase {

  public ServerMapL1EvictionOffHeapDestroyExpressTestClient(String[] args) {
    super("testWithMaxElementsInMemory", args);
  }

  public static void main(String[] args) {
    new ServerMapL1EvictionOffHeapDestroyExpressTestClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
    System.out.println("Client populating cache.");
    for (int i = 0; i < 10000; i++) {
      cache.put(new Element("key", new byte[10 * 1024]));
      Thread.sleep(10);
    }
  }
}
