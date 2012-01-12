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

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;

import java.util.concurrent.atomic.AtomicInteger;

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
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    final Barrier barrier = toolkit.getBarrier("ClusterCacheEventsRejoinEnabledClient", 2);
    final int nodeId = barrier.await();

    final CacheCluster cluster = cache.getCacheManager().getCluster(ClusterScheme.TERRACOTTA);
    assertTrue(cluster != null);
    assertTrue(cluster.getScheme().equals(ClusterScheme.TERRACOTTA));
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
      if (nodeId == 0) {
        for (int i = 0; i < ELEMENTS; i++) {
          final Element element = new Element(Integer.toString(i) + " FROM " + cluster.getCurrentNode().getId(),
                                              "Value for " + Integer.toString(i));
          cache.put(element);
          System.out.println(cluster.getCurrentNode().getId() + " PUTS A VALUE FOR " + element.getKey());
        }
      }
      barrier.await();
      Thread.sleep(2000L);
      assertThat(counter.get(), equalTo(ELEMENTS));
    } finally {
      barrier.await();
    }
  }
}
