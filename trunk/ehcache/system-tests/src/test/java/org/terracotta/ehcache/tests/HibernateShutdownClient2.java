package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;

import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.junit.Assert;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.cluster.ClusterInfo;
import org.terracotta.ehcache.tests.container.hibernate.domain.Event;
import org.terracotta.ehcache.tests.container.hibernate.domain.EventManager;
import org.terracotta.ehcache.tests.container.hibernate.nontransactional.HibernateUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class HibernateShutdownClient2 extends ClientBase {

  public HibernateShutdownClient2(String[] args) {
    super("test", args);
  }

  @Override
  public void run() {
    try {
      getBarrierForAllClients().await(TimeUnit.SECONDS.toMillis(3 * 60));

      System.out.println("Current connected clients: " + getConnectedClients());

      runTest(null, getClusteringToolkit());

      getBarrierForAllClients().await(TimeUnit.SECONDS.toMillis(3 * 60));
      System.out.println("Waiting for client1 to shutdown...");
      Thread.sleep(TimeUnit.SECONDS.toMillis(30));

      Assert.assertEquals(1, getConnectedClients());

      pass();
      System.exit(0);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    HibernateUtil.configure("/hibernate-config/shutdowntest/hibernate.cfg.xml");
    EventManager mgr = new EventManager(HibernateUtil.getSessionFactory());
    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    stats.setStatisticsEnabled(true);

    for (Event event : (List<Event>) mgr.listEvents()) {
      mgr.listEmailsOfEvent(event.getId());
    }

    HibernateUtil.getSessionFactory().close();

    System.err.println("Second Level Cache Regions");
    for (String region : stats.getSecondLevelCacheRegionNames()) {
      System.err.println("Region : " + region);
      SecondLevelCacheStatistics l2Stats = stats.getSecondLevelCacheStatistics(region);
      Assert.assertEquals("L2 Cache [Region " + region + "] Cache Miss Count", 0L, l2Stats.getMissCount());

      System.err.println("\tCache Miss Count " + l2Stats.getMissCount());
      System.err.println("\tCache Hit Count " + l2Stats.getHitCount());
      System.err.println("\tCache Put Count " + l2Stats.getPutCount());
    }

    QueryStatistics queryStats = stats.getQueryStatistics("from Event");
    Assert.assertEquals("Cache Miss Count", 0L, queryStats.getCacheMissCount());
    Assert.assertEquals("Cache Hit Count", 1L, queryStats.getCacheHitCount());
    Assert.assertEquals("Cache Put Count", 0L, queryStats.getCachePutCount());
  }

  private int getConnectedClients() {
    ClusteringToolkit clustering = getTerracottaClient().getToolkit();
    ClusterInfo clusterInfo = clustering.getClusterInfo();
    return clusterInfo.getClusterTopology().getNodes().size();
  }
}
