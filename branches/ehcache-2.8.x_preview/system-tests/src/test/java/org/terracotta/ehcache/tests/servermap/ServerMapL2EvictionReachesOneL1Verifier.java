/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.ehcache.tests.ClientBase;

import junit.framework.Assert;

public class ServerMapL2EvictionReachesOneL1Verifier extends ClientBase {

  public ServerMapL2EvictionReachesOneL1Verifier(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ServerMapL2EvictionReachesOneL1Verifier(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
    System.out.println("in the verifier");

    EvictionCountingEventListener countingListener = new EvictionCountingEventListener(
                                                                                       clusteringToolkit
                                                                                           .getAtomicLong("EvictionCounter"));
    cache.getCacheEventNotificationService().registerListener(countingListener);

    getBarrierForAllClients().await();
    long value = countingListener.getEvictedCount();
    System.out.println("After sleeping 2 mins: value=" + value);
    Assert.assertTrue("Expected at most " + ServerMapL2EvictionReachesOneL1TestClient.EXPECTED_EVICTION_COUNT
                          + " elements to have been evicted, value=" + value,
                      (value <= ServerMapL2EvictionReachesOneL1TestClient.EXPECTED_EVICTION_COUNT));
  }
}
