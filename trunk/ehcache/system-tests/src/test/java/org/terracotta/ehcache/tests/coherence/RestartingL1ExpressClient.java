/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.coherence;

import net.sf.ehcache.Cache;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.ClientBase;

public class RestartingL1ExpressClient extends ClientBase {

  public static final String PASS_OUTPUT = "Restarting express client PASS output";
  private Barrier            barrier;
  private final boolean      afterRestart;
  private final boolean      shouldCrash;

  protected RestartingL1ExpressClient(String[] args) {
    super("test", args);
    if (args.length > 1) {
      afterRestart = args.length > 2;
      shouldCrash = true;
    } else {
      afterRestart = false;
      shouldCrash = false;
    }
  }

  // n nodes start in coherent, then setCoherent(false)
  // assert coherent=false in all n nodes
  // n-1 nodes call setCoherent(true)
  // 1 node exits without calling setCoherent(true)
  // n-1 nodes assert coherent
  // 1 node restarts, asserts cache coherent
  public static void main(String[] args) {
    new RestartingL1ExpressClient(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    barrier = toolkit.getBarrier("CacheCoherenceExpressClient", RestartingL1ExpressTest.CLIENT_COUNT);
    assertEquals(true, cache.isClusterCoherent());
    assertEquals(true, cache.isNodeCoherent());

    if (!afterRestart) {
      barrier.await();
      doInitialSteps(cache);
    } else {
      log("Running crashing client AFTER RESTART...");
      // barrier X
      barrier.await();
      // barrier Z
      barrier.await();
      // cache is coherent when it restarts
      assertEquals(true, cache.isClusterCoherent());
      assertEquals(true, cache.isNodeCoherent());
      cache.setNodeCoherent(false);
      assertEquals(false, cache.isClusterCoherent());
      assertEquals(false, cache.isNodeCoherent());
      cache.setNodeCoherent(true);
      assertEquals(true, cache.isClusterCoherent());
      assertEquals(true, cache.isNodeCoherent());
    }
    log(PASS_OUTPUT);
  }

  private void doInitialSteps(Cache cache) throws Exception {
    cache.setNodeCoherent(false);
    assertEquals(false, cache.isNodeCoherent());
    assertEquals(false, cache.isClusterCoherent());
    barrier.await();
    if (shouldCrash) {
      log("Running crashing client...");
      // let other nodes make cache coherent
      assertEquals(false, cache.isClusterCoherent());
      assertEquals(false, cache.isNodeCoherent());
      log("Crashing client finishing without calling calling setNodeCoherent(true)");
      // barrier Y
      barrier.await();
      // exit without calling setNodeCoherent(true)
    } else {
      log("Running normal client...");
      cache.setNodeCoherent(true);
      assertEquals(true, cache.isNodeCoherent());
      assertEquals(false, cache.isClusterCoherent());
      log("Normal client before crasher exiting");
      // barrier Y
      barrier.await();
      log("Crashing client has probably exited... waiting for it to come back...");
      // by this time 1 node has exited (or in process of exiting)
      // the call below should return quite fast, as soon as the other node exits
      cache.waitUntilClusterCoherent();

      // wait for other node to come back after restart
      // barrier X
      barrier.await();
      log("Crashing client restarted.");
      // other node has restarted now
      assertEquals(true, cache.isClusterCoherent());
      assertEquals(true, cache.isNodeCoherent());
      // barrier Z
      barrier.await();
    }

  }

  private static void log(String msg) {
    System.out.println(msg);
  }
}
