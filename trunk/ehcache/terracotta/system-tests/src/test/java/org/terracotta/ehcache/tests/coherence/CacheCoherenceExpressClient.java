package org.terracotta.ehcache.tests.coherence;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;
import org.terracotta.ehcache.tests.ClientBase;

import junit.framework.Assert;

public class CacheCoherenceExpressClient extends ClientBase {

  public static final String PASS_OUTPUT = "CacheCoherenceExpressClient PASS output";
  private String             id;
  private Barrier            barrier;

  protected CacheCoherenceExpressClient(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new CacheCoherenceExpressClient(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    barrier = toolkit.getBarrier("CacheCoherenceExpressClient", CacheCoherenceExpressTest.CLIENT_COUNT);
    int index = barrier.await();
    id = "" + index;
    log("Created barrier, index: " + index);

    // coherent="false" now means non-strict
    Assert.assertEquals(Consistency.EVENTUAL, cache.getCacheConfiguration().getTerracottaConfiguration()
        .getConsistency());

    // move to bulk load
    cache.setNodeCoherent(false);

    barrier.await();
    assertEquals(false, cache.isNodeCoherent());
    assertEquals(false, cache.isClusterCoherent());
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

    log(PASS_OUTPUT);
  }

  private void doTestDynamicConfig(int index, Cache cache) throws Exception {
    log("Testing dynamic config change");
    boolean old = cache.isNodeCoherent();
    if (index == 0) {
      cache.setNodeCoherent(true);
      assertEquals(true, cache.isNodeCoherent());
      // barrier 1
      barrier.await();
      // barrier 2
      barrier.await();
      cache.setNodeCoherent(false);
      assertEquals(false, cache.isNodeCoherent());
      // barrier 3
      barrier.await();
      // barrier 4
      barrier.await();
    } else {
      // barrier 1
      barrier.await();
      // validate no change in other node
      assertEquals(old, cache.isNodeCoherent());
      // barrier 2
      barrier.await();
      // barrier 3
      barrier.await();
      assertEquals(old, cache.isNodeCoherent());
      // barrier 4
      barrier.await();
    }
    log("Testing dynamic config change -- done");
  }

  private void basicCacheTest(int index, Cache cache) throws Exception {
    log("Running basicCacheTest");
    assertEquals(0, cache.getSize());

    barrier.await();

    if (index == 0) {
      cache.put(new Element("key", "value"));
    }

    barrier.await();

    assertEquals(1, cache.getSize());
    assertEquals("value", cache.get("key").getObjectValue());

    barrier.await();

    if (index == 0) {
      boolean removed = cache.remove("key");
      assertTrue(removed);
    }

    barrier.await();

    assertEquals(0, cache.getSize());
  }

  private void cacheCoherenceTest(int index, Cache cache, boolean coherentAtEnd) throws Exception {
    barrier.await();
    assertEquals(false, cache.isNodeCoherent());
    assertEquals(false, cache.isClusterCoherent());
    if (index == 0) {
      cache.setNodeCoherent(true);
      assertEquals(true, cache.isNodeCoherent());
      log("Going to wait until coherent");
      barrier.await();
      cache.waitUntilClusterCoherent();
      assertEquals(true, cache.isClusterCoherent());
      log("Cache is now coherent");
      int otherNodes = CacheCoherenceExpressTest.CLIENT_COUNT - 1;
      if (coherentAtEnd) {
        assertEquals(otherNodes, cache.getSize());
      } else {
        log("Asserting other exiting nodes committed");
        // make sure the shutdown hook was executed and 5000 elements were inserted by each node
        assertEquals((5000 * otherNodes) + otherNodes, cache.getSize());
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