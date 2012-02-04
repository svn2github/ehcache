/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.coherence;

import net.sf.ehcache.Cache;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.ClientBase;

import junit.framework.Assert;

public class RestartingL1ExpressClient extends ClientBase {

  public static final String PASS_OUTPUT  = "Restarting express client PASS output";
  private Barrier            barrier;
  private boolean            afterRestart = false;
  private boolean            shouldCrash  = false;

  public RestartingL1ExpressClient(String[] args) {
    super("test", args);
    for (String arg : args) {
      if (arg.equals(RestartingL1ExpressTest.SHOULD_CRASH)) {
        this.shouldCrash = true;
      }

      if (arg.equals(RestartingL1ExpressTest.AFTER_RESTART)) {
        this.afterRestart = true;
      }
    }
  }

  // n nodes start in coherent, then setCoherent(false)
  // assert coherent=false in all n nodes
  // n-1 nodes call setCoherent(true)
  // 1 node exits without calling setCoherent(true)
  // n-1 nodes assert coherent
  // 1 node restarts, asserts cache coherent
  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    barrier = toolkit.getBarrier("CacheCoherenceExpressClient", RestartingL1ExpressTest.CLIENT_COUNT);
    Assert.assertEquals(true, cache.isClusterCoherent());
    Assert.assertEquals(true, cache.isNodeCoherent());

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
      Assert.assertEquals(true, cache.isClusterCoherent());
      Assert.assertEquals(true, cache.isNodeCoherent());
      cache.setNodeCoherent(false);
      Assert.assertEquals(false, cache.isClusterCoherent());
      Assert.assertEquals(false, cache.isNodeCoherent());
      cache.setNodeCoherent(true);
      Assert.assertEquals(true, cache.isClusterCoherent());
      Assert.assertEquals(true, cache.isNodeCoherent());
    }
    log(PASS_OUTPUT);
  }

  private void doInitialSteps(Cache cache) throws Exception {
    cache.setNodeCoherent(false);
    Assert.assertEquals(false, cache.isNodeCoherent());
    Assert.assertEquals(false, cache.isClusterCoherent());
    barrier.await();
    if (shouldCrash) {
      log("Running crashing client...");
      // let other nodes make cache coherent
      Assert.assertEquals(false, cache.isClusterCoherent());
      Assert.assertEquals(false, cache.isNodeCoherent());
      log("Crashing client finishing without calling calling setNodeCoherent(true)");
      // barrier Y
      barrier.await();
      // exit without calling setNodeCoherent(true)
    } else {
      log("Running normal client...");
      cache.setNodeCoherent(true);
      Assert.assertEquals(true, cache.isNodeCoherent());
      Assert.assertEquals(false, cache.isClusterCoherent());
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
      Assert.assertEquals(true, cache.isClusterCoherent());
      Assert.assertEquals(true, cache.isNodeCoherent());
      // barrier Z
      barrier.await();
    }

  }

  private static void log(String msg) {
    System.out.println(msg);
  }
}
