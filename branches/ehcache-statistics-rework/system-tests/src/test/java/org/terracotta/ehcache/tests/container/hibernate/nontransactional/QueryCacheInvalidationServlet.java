/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.container.hibernate.nontransactional;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.junit.Assert;
import org.terracotta.ehcache.tests.container.hibernate.BaseClusteredRegionFactoryTestServlet;
import org.terracotta.ehcache.tests.container.hibernate.domain.Item;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpSession;

public class QueryCacheInvalidationServlet extends BaseClusteredRegionFactoryTestServlet {

  @Override
  protected void doServer0(HttpSession session, Map<String, String[]> parameters) throws Exception {
    HibernateUtil.dropAndCreateDatabaseSchema();

    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    stats.setStatisticsEnabled(true);

    Session s = HibernateUtil.getSessionFactory().openSession();
    Transaction t = s.beginTransaction();
    Item i = new Item();
    i.setName("widget");
    i.setDescription("A really top-quality, full-featured widget.");
    s.persist(i);
    t.commit();
    s.close();

    final SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(Item.class.getName());
    Assert.assertEquals(1, statistics.getPutCount());
//    Assert.assertEquals(1, statistics.getElementCountOnDisk());
//    boolean foundInMemory = waitUntilTrue(new Callable<Boolean>() {
//      public Boolean call() throws Exception {
//        long elementCountInMemory = statistics.getElementCountInMemory();
//        System.out.println("Checking stats, elementCountInMemory: " + elementCountInMemory);
//        return elementCountInMemory == 1;
//      }
//    });
//    Assert.assertEquals(true, foundInMemory);
  }

  private boolean waitUntilTrue(final Callable<Boolean> callable) throws Exception {

    for (int i = 0; i < 120; i++) { // wait up to two minutes, which is a long time, but far less than the overal test
                                    // time-out
      Boolean rv = callable.call();
      System.out.println("Waiting until callable returns true, returned: " + rv);
      if (rv == true) { return true; }
      Thread.sleep(1000);
    }
    return false;
  }

  @Override
  protected void doServer1(HttpSession session, Map<String, String[]> parameters) throws Exception {
    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    stats.setStatisticsEnabled(true);

    Session s = HibernateUtil.getSessionFactory().openSession();
    Transaction t = s.beginTransaction();
    Item i = (Item) s.get(Item.class, Long.valueOf(1L));

    SecondLevelCacheStatistics slcs = stats.getSecondLevelCacheStatistics(Item.class.getName());

    Assert.assertEquals(1, slcs.getHitCount());
    Assert.assertEquals(0, slcs.getMissCount());

    i.setDescription("A bog standard item");

    t.commit();
    s.close();

    Assert.assertEquals(1, slcs.getPutCount());

    // cleanup
    s = HibernateUtil.getSessionFactory().openSession();
    t = s.beginTransaction();
    s.delete(i);
    t.commit();
    s.close();
  }
}
