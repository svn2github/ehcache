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

public class EmptySecondLevelCacheEntryServlet extends BaseClusteredRegionFactoryTestServlet {

  @Override
  protected void doServer0(HttpSession session, Map<String, String[]> parameters) throws Exception {
    HibernateUtil.dropAndCreateDatabaseSchema();

    Session s = HibernateUtil.getSessionFactory().openSession();
    Transaction t = s.beginTransaction();
    Item i = new Item();
    i.setName("widget");
    i.setDescription("A really top-quality, full-featured widget.");
    s.persist(i);
    t.commit();
    s.close();

    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    final SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(Item.class.getName());
    Assert.assertEquals(1, statistics.getElementCountInMemory());
  }

  @Override
  protected void doServer1(HttpSession session, Map<String, String[]> parameters) throws Exception {
    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    SecondLevelCacheStatistics cacheStats = stats.getSecondLevelCacheStatistics(Item.class.getName());

    Assert.assertEquals(1L, cacheStats.getElementCountInMemory());

    HibernateUtil.getSessionFactory().evictEntity(Item.class.getName());

    Assert.assertEquals(0L, cacheStats.getElementCountInMemory());
  }

}
