package org.terracotta.ehcache.tests;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.event.TerracottaCacheEventReplicationFactory;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.test.util.WaitUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

/**
 * @author Alex Snaps
 */
public class ClusterCacheEventsRejoinEnabledClient extends ClientBase {

  private static final int    ELEMENTS = 10;
  private final AtomicInteger counter  = new AtomicInteger();

  public ClusterCacheEventsRejoinEnabledClient(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ClusterCacheEventsRejoinEnabledClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    final int nodeId = getBarrierForAllClients().await();

    final CacheCluster cluster = cache.getCacheManager().getCluster(ClusterScheme.TERRACOTTA);
    Assert.assertTrue(cluster != null);
    Assert.assertTrue(cluster.getScheme().equals(ClusterScheme.TERRACOTTA));
    System.out.println("WELCOME TO " + cluster.getCurrentNode().getId());

    try {
      cache.getCacheEventNotificationService().registerListener(new TerracottaCacheEventReplicationFactory()
                                                                    .createCacheEventListener(null));
      cache.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter() {
        @Override
        public void notifyElementPut(final Ehcache ehcache, final Element element) throws CacheException {
          System.out.println(cluster.getCurrentNode().getId() + " GOT A VALUE FOR " + element.getKey());
          counter.getAndIncrement();
        }
      });
      getBarrierForAllClients().await();
      if (nodeId == 0) {
        for (int i = 0; i < ELEMENTS; i++) {
          final Element element = new Element(Integer.toString(i) + " FROM " + cluster.getCurrentNode().getId(),
                                              "Value for " + Integer.toString(i));
          cache.put(element);
          System.out.println(cluster.getCurrentNode().getId() + " PUTS A VALUE FOR " + element.getKey());
        }
      }
      getBarrierForAllClients().await();
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        public Boolean call() throws Exception {
          int counterValue = counter.get();
          System.out.println("Waiting until counter hits expected: " + ELEMENTS + ", Actual: " + counterValue);
          if (counterValue == ELEMENTS) { return true; }
          return false;
        }
      });
      assertThat(counter.get(), equalTo(ELEMENTS));
    } finally {
      getBarrierForAllClients().await();
    }
  }
}
