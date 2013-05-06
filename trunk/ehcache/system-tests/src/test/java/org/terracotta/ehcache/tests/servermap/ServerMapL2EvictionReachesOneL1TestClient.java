package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;

import java.util.concurrent.Callable;

public class ServerMapL2EvictionReachesOneL1TestClient extends ServerMapClientBase {
  final static long EXPECTED_EVICTION_COUNT = 5800;

  public ServerMapL2EvictionReachesOneL1TestClient(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ServerMapL2EvictionReachesOneL1TestClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
    System.out.println("Running test with concurrency=1");
    testWith(cache, 3000, EXPECTED_EVICTION_COUNT, clusteringToolkit);

    System.out.println("Testing with higher concurrency value.");
    // 100 maxElementsOnDisk, 50 stripes -> targetMaxTotalCount of 2 for each stripe
    // add 5000 (100 per stripe), at least one per stripe should be evicted
  }

  private void testWith(final Cache cache, final int maxElements, final long expectedEvictionCount,
                        Toolkit clusteringToolkit) throws Exception {
    final EvictionCountingEventListener countingListener = new EvictionCountingEventListener(
        clusteringToolkit.getAtomicLong("EvictionCounter")); // shared counter
    cache.getCacheEventNotificationService().registerListener(countingListener);
    // put elements only after both listeners are registered
    getBarrierForAllClients().await();

    for (int i = 0; i < maxElements; i++) {
      cache.put(new Element("key-" + i, "value-" + i));
    }
    long value = countingListener.getEvictedCount();
    System.out.println("Wating for all evictions, evicted till now: " + value);

    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return countingListener.getEvictedCount() == EXPECTED_EVICTION_COUNT;
      }
    });
    getBarrierForAllClients().await();

    value = countingListener.getEvictedCount();
    System.out.println("Number of evictions = " + value);
    assertTrue("Expected at most " + expectedEvictionCount + " elements to have been evicted, value=" + value,
        (value == expectedEvictionCount));
  }
}