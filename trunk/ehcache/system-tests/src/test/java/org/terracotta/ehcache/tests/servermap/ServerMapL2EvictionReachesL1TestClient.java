package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.terracotta.api.ClusteringToolkit;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerMapL2EvictionReachesL1TestClient extends ServerMapClientBase {

  public ServerMapL2EvictionReachesL1TestClient(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ServerMapL2EvictionReachesL1TestClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
    System.out.println("Running test with concurrency=1");
    testWith(cache, 3000, 100);

    System.out.println("Testing with higher concurrency value.");
    // 100 maxElementsOnDisk, 50 stripes -> targetMaxTotalCount of 2 for each stripe
    // add 5000 (100 per stripe), at least one per stripe should be evicted
    // use 25 just in case
    testWith(cache.getCacheManager().getCache("testWithConcurrency"), 5000, 50);
  }

  private void testWith(final Cache cache, final int maxElements, final int expectedEvictionCount)
      throws InterruptedException {
    EvictionCountingEventListener countingListener = new EvictionCountingEventListener();
    cache.getCacheEventNotificationService().registerListener(countingListener);

    for (int i = 0; i < maxElements; i++) {
      cache.put(new Element("key-" + i, "value-" + i));
    }

    Calendar timeoutTime = Calendar.getInstance();
    timeoutTime.add(Calendar.MINUTE, 5);
    System.out.println("Waiting 5 minutes for evictions to reach the expected count of " + expectedEvictionCount);
    assertRange(expectedEvictionCount, maxElements, cache);
  }

  public class EvictionCountingEventListener implements CacheEventListener {
    private final AtomicInteger count = new AtomicInteger();

    public void notifyElementEvicted(Ehcache cache, Element element) {
      int val = count.incrementAndGet();
      if (val % 100 == 0) {
        System.out.println("EvictionListener: number of elements evicted till now: " + val);
      }
    }

    public void dispose() {
      //
    }

    public void notifyElementExpired(Ehcache cache, Element element) {
      //
    }

    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
      //
    }

    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
      //
    }

    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
      //
    }

    public void notifyRemoveAll(Ehcache cache) {
      //
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }

  }
}
