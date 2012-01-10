/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.hibernate.nontransactional;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.terracotta.modules.ehcache.WaitUtil;
import org.terracotta.modules.ehcache.hibernate.BaseClusteredRegionFactoryTestServlet;
import org.terracotta.modules.ehcache.hibernate.domain.Item;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpSession;

import junit.framework.Assert;

public class EmptySecondLevelCacheEntryTestServlet extends BaseClusteredRegionFactoryTestServlet {

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
    Assert.assertEquals(1, statistics.getElementCountOnDisk());

    // MNK-3167: Due to a race between the ServerMapLocalCache transaction completion callback and getting in the in
    // memory store size. We need to retry getting the memory store size a few times.
    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
      public Boolean call() throws Exception {
        return statistics.getElementCountInMemory() == 1;
      }
    });
  }

  @Override
  protected void doServer1(HttpSession session, Map<String, String[]> parameters) throws Exception {
    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    SecondLevelCacheStatistics cacheStats = stats.getSecondLevelCacheStatistics(Item.class.getName());

    long size = cacheStats.getElementCountInMemory() + cacheStats.getElementCountOnDisk();
    Assert.assertEquals(1L, size);

    HibernateUtil.getSessionFactory().evictEntity(Item.class.getName());

    size = cacheStats.getElementCountInMemory() + cacheStats.getElementCountOnDisk();
    Assert.assertEquals(0L, size);
  }

}
