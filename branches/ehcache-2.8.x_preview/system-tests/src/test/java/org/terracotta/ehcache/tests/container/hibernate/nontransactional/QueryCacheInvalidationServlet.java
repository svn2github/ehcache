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
    Assert.assertEquals(1, statistics.getElementCountInMemory());
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
