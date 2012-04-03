/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.container.hibernate.nontransactional;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.terracotta.ehcache.tests.container.hibernate.BaseClusteredRegionFactoryTestServlet;
import org.terracotta.ehcache.tests.container.hibernate.domain.Item;

import java.util.Map;

import javax.servlet.http.HttpSession;

import junit.framework.Assert;

public class NonEternalSecondLevelCacheTestServlet extends BaseClusteredRegionFactoryTestServlet {

  @Override
  protected void doServer0(HttpSession session, Map<String, String[]> parameters) throws Exception {
    HibernateUtil.dropAndCreateDatabaseSchema();

    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    stats.setStatisticsEnabled(true);
    SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(Item.class.getName());

    Session s = HibernateUtil.getSessionFactory().openSession();
    Transaction t = s.beginTransaction();
    Item i = new Item();
    i.setName("widget");
    i.setDescription("A really top-quality, full-featured widget.");
    s.persist(i);
    long id = i.getId();
    t.commit();
    s.close();

    Assert.assertEquals(1, statistics.getPutCount());
    Assert.assertEquals(0, statistics.getHitCount());

    s = HibernateUtil.getSessionFactory().openSession();
    t = s.beginTransaction();
    s.get(Item.class, id);
    t.commit();
    s.close();

    Assert.assertEquals(1, statistics.getPutCount());
    Assert.assertEquals(1, statistics.getHitCount());

    Thread.sleep(15000);

    s = HibernateUtil.getSessionFactory().openSession();
    t = s.beginTransaction();
    s.get(Item.class, id);
    t.commit();
    s.close();

    Assert.assertEquals(2, statistics.getPutCount());
    Assert.assertEquals(1, statistics.getHitCount());

    s = HibernateUtil.getSessionFactory().openSession();
    t = s.beginTransaction();
    s.get(Item.class, id);
    t.commit();
    s.close();

    Assert.assertEquals(2, statistics.getPutCount());
    Assert.assertEquals(2, statistics.getHitCount());
  }

  @Override
  protected void doServer1(HttpSession session, Map<String, String[]> parameters) throws Exception {
    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    SecondLevelCacheStatistics cacheStats = stats.getSecondLevelCacheStatistics(Item.class.getName());

    HibernateUtil.getSessionFactory().evictEntity(Item.class.getName());

    long size = cacheStats.getElementCountInMemory() + cacheStats.getElementCountOnDisk();
    Assert.assertEquals(0L, size);
  }
}
