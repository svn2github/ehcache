/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.ClientBase;

import junit.framework.Assert;

public class CacheCoherenceTestL1Client extends ClientBase {

  private final Barrier barrier;
  private String        id;

  public CacheCoherenceTestL1Client(String[] args) {
    super(args);
    this.barrier = getClusteringToolkit().getBarrier("barrier", CacheCoherenceTest.CLIENT_COUNT);
  }

  public static void main(String[] args) {
    try {
      new CacheCoherenceTestL1Client(args).run();
    } catch (Throwable e) {
      e.printStackTrace();
      System.err.println("Test FAILED");
      System.exit(1);
    }
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
    final int index = barrier.await();
    id = "" + index;

    // coherent="false" now means eventual
    Assert.assertEquals(Consistency.EVENTUAL, cache.getCacheConfiguration().getTerracottaConfiguration()
        .getConsistency());

    // move to bulk load
    cache.setNodeCoherent(false);

    barrier.await();
    Assert.assertEquals(false, cache.isNodeCoherent());
    Assert.assertEquals(false, cache.isClusterCoherent());
    barrier.await();
    cache.setNodeCoherent(true);
    barrier.await();
    basicCacheTest(index, cache);
    barrier.await();
    cache.setNodeCoherent(false);
    barrier.await();

    boolean old = cache.isNodeCoherent();
    barrier.await();
    doTestDynamicConfig(index, cache);
    barrier.await();
    cache.setNodeCoherent(old);
    barrier.await();

    log("####### running cache coherence test, waiting node should get notified");
    cacheCoherenceTest(index, cache, true);

    barrier.await();
    log("####### setting cache to incoherent again in all nodes.");
    cache.setNodeCoherent(false);
    barrier.await();

    log("####### running cache coherence test, some nodes will disconnect without calling setCoherent(true)");
    // run this test last
    cacheCoherenceTest(index, cache, false);

  }

  private void doTestDynamicConfig(int index, Cache cache) throws Exception {
    log("Testing dynamic config change");
    boolean old = cache.isNodeCoherent();
    if (index == 0) {
      cache.setNodeCoherent(true);
      Assert.assertEquals(true, cache.isNodeCoherent());
      // await 1
      barrier.await();
      // await 2
      barrier.await();
      cache.setNodeCoherent(false);
      Assert.assertEquals(false, cache.isNodeCoherent());
      // await 3
      barrier.await();
      // await 4
      barrier.await();
    } else {
      // await 1
      barrier.await();
      // validate no change in other node
      Assert.assertEquals(old, cache.isNodeCoherent());
      // await 2
      barrier.await();
      // await 3
      barrier.await();
      Assert.assertEquals(old, cache.isNodeCoherent());
      // await 4
      barrier.await();
    }
    log("Testing dynamic config change -- done");
  }

  private void basicCacheTest(int index, Cache cache) throws Exception {
    Assert.assertEquals(0, cache.getSize());

    barrier.await();

    if (index == 0) {
      cache.put(new Element("key", "value"));
    }

    barrier.await();

    Assert.assertEquals(1, cache.getSize());
    Assert.assertEquals("value", cache.get("key").getObjectValue());

    barrier.await();

    if (index == 0) {
      boolean removed = cache.remove("key");
      Assert.assertTrue(removed);
    }

    barrier.await();

    Assert.assertEquals(0, cache.getSize());
  }

  private void cacheCoherenceTest(int index, Cache cache, boolean coherentAtEnd) throws Exception {
    barrier.await();
    Assert.assertEquals(false, cache.isNodeCoherent());
    Assert.assertEquals(false, cache.isClusterCoherent());
    if (index == 0) {
      cache.setNodeCoherent(true);
      Assert.assertEquals(true, cache.isNodeCoherent());
      log("Going to wait until coherent");
      barrier.await();
      cache.waitUntilClusterCoherent();
      Assert.assertEquals(true, cache.isClusterCoherent());
      log("Cache is now coherent");
      int otherNodes = CacheCoherenceTest.CLIENT_COUNT - 1;
      if (coherentAtEnd) {
        Assert.assertEquals(otherNodes, cache.getSize());
      } else {
        log("Asserting other exiting nodes committed");
        // make sure the shutdown hook was executed and 5000 elements were inserted by each node
        Assert.assertEquals((5000 * otherNodes) + otherNodes, cache.getSize());
        log("Done");
      }
    } else {
      barrier.await();
      if (coherentAtEnd) {
        Element element = new Element("key-" + index, "value");
        cache.put(element);
        log("added element and sleeping for 10 secs: " + element);
        // 10 seconds is enough for 1 element to be flushed from the local buffer
        Thread.sleep(10 * 1000);
        log("setting cache coherent");
        cache.setNodeCoherent(true);
      } else {
        // put 5000 elements, from each node
        for (int i = 0; i < 5000; i++) {
          cache.put(new Element("node-" + index + "-key-" + i, "node-" + index + "-value-" + i));
        }
        log("Node exiting without setting back cache in coherent.");
      }
    }
  }

  private void log(String string) {
    System.out.println("Node-" + id + ": " + string);
  }

}
