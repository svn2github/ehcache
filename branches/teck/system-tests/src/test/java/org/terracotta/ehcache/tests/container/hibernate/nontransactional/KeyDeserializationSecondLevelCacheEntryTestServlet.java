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

public class KeyDeserializationSecondLevelCacheEntryTestServlet extends BaseClusteredRegionFactoryTestServlet {

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
    SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(Item.class.getName());
    Map<?, ?> entries = statistics.getEntries();
    Assert.assertEquals(1, entries.size());
  }

  @Override
  protected void doServer1(HttpSession session, Map<String, String[]> parameters) throws Exception {
    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    SecondLevelCacheStatistics cacheStats = stats.getSecondLevelCacheStatistics(Item.class.getName());

    Map<?, ?> entries = cacheStats.getEntries();
    Assert.assertEquals(1, entries.size());

    HibernateUtil.getSessionFactory().evictEntity(Item.class.getName());

    entries = cacheStats.getEntries();
    Assert.assertEquals(0, entries.size());
  }
}
