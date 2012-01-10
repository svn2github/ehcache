/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import com.tc.logging.TCLogger;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.util.Assert;

import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;

public class RestartingL1Client {

  private static final TCLogger logger  = ManagerUtil.getLogger(RestartingL1Client.class.getName());
  private CyclicBarrier         barrier = new CyclicBarrier(CacheCoherenceTest.CLIENT_COUNT);

  // private static String id = System.getProperty("test.node-id");

  public static void main(String[] args) {
    log("RestartingL1Client test: args: " + Arrays.asList(args));
    try {
      RestartingL1Client client = new RestartingL1Client();
      if (args.length > 0) {
        boolean afterRestart = args.length > 1;
        client.doTest(true, afterRestart);
      } else {
        client.doTest(false, false);
      }
    } catch (Throwable e) {
      log("Node didn't exit successfully.");
      e.printStackTrace();
      System.exit(1);
    }
  }

  // n nodes start in coherent, then setCoherent(false)
  // assert coherent=false in all n nodes
  // n-1 nodes call setCoherent(true)
  // 1 node exits without calling setCoherent(true)
  // n-1 nodes assert coherent
  // 1 node restarts, asserts cache coherent
  private void doTest(boolean shouldCrash, boolean afterRestart) throws Exception {
    CacheManager cacheManager = new CacheManager(getClass().getResourceAsStream("/basic-cache-test.xml"));
    Cache cache = cacheManager.getCache("test");
    Assert.assertEquals(true, cache.isClusterCoherent());
    Assert.assertEquals(true, cache.isNodeCoherent());

    if (!afterRestart) {
      barrier.await();
      doInitialSteps(cache, shouldCrash);
    } else {
      log("Running crashing client AFTER RESTART...");
      // await X
      barrier.await();
      // await Z
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
  }

  private void doInitialSteps(Cache cache, boolean shouldCrash) throws Exception {
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
      // await Y
      barrier.await();
      // exit without calling setNodeCoherent(true)
    } else {
      log("Running normal client...");
      cache.setNodeCoherent(true);
      Assert.assertEquals(true, cache.isNodeCoherent());
      Assert.assertEquals(false, cache.isClusterCoherent());
      log("Normal client before crasher exiting");
      // await Y
      barrier.await();
      log("Crashing client has probably exited... waiting for it to come back...");
      // by this time 1 node has exited (or in process of exiting)
      // the call below should return quite fast, as soon as the other node exits
      cache.waitUntilClusterCoherent();

      // wait for other node to come back after restart
      // await X
      barrier.await();
      log("Crashing client restarted.");
      // other node has restarted now
      Assert.assertEquals(true, cache.isClusterCoherent());
      Assert.assertEquals(true, cache.isNodeCoherent());
      // await Z
      barrier.await();
    }

  }

  private static void log(String msg) {
    logger.info(msg);
  }
}
