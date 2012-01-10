/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.hibernate.nontransactional;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.TerracottaConfiguration.ValueMode;
import net.sf.ehcache.hibernate.regions.EhcacheEntityRegion;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cache.Region;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.terracotta.modules.ehcache.hibernate.BaseClusteredRegionFactoryTestServlet;
import org.terracotta.modules.ehcache.hibernate.domain.Item;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import junit.framework.Assert;

import static net.sf.ehcache.util.RetryAssert.assertBy;
import static org.hamcrest.CoreMatchers.equalTo;

public class IdentityRemappingCacheTestServlet extends BaseClusteredRegionFactoryTestServlet {

  @Override
  protected void doServer0(HttpSession session, Map<String, String[]> parameters) throws Exception {
    HibernateUtilIdentity.dropAndCreateDatabaseSchema();

    checkCacheSettings();

    Session s = HibernateUtilIdentity.getSessionFactory().openSession();
    Transaction t = s.beginTransaction();
    Item i = new Item();
    i.setName("widget");
    i.setDescription("A really top-quality, full-featured widget.");
    s.persist(i);
    t.commit();
    s.close();

    Statistics stats = HibernateUtilIdentity.getSessionFactory().getStatistics();
    final SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(Item.class.getName());

    assertBy(2, TimeUnit.SECONDS, new Callable<Long>() {
      public Long call() throws Exception {
        return statistics.getElementCountOnDisk();
      }
    }, equalTo(1L));

    assertBy(2, TimeUnit.SECONDS, new Callable<Long>() {
      public Long call() throws Exception {
        return statistics.getElementCountInMemory();
      }
    }, equalTo(1L));
  }

  @Override
  protected void doServer1(HttpSession session, Map<String, String[]> parameters) throws Exception {
    checkCacheSettings();

    Statistics stats = HibernateUtilIdentity.getSessionFactory().getStatistics();
    SecondLevelCacheStatistics cacheStats = stats.getSecondLevelCacheStatistics(Item.class.getName());

    long size = cacheStats.getElementCountInMemory() + cacheStats.getElementCountOnDisk();
    Assert.assertEquals(1L, size);

    HibernateUtilIdentity.getSessionFactory().evictEntity(Item.class.getName());

    size = cacheStats.getElementCountInMemory() + cacheStats.getElementCountOnDisk();
    Assert.assertEquals(0L, size);
  }

  private void checkCacheSettings() {
    SessionFactory sessionFactory = HibernateUtilIdentity.getSessionFactory();

    if (sessionFactory instanceof SessionFactoryImplementor) {
      EntityPersister persister = ((SessionFactoryImplementor) sessionFactory).getEntityPersister(Item.class.getName());
      Region cacheRegion = persister.getCacheAccessStrategy().getRegion();
      Assert.assertTrue(cacheRegion instanceof EhcacheEntityRegion);

      Ehcache cache = ((EhcacheEntityRegion) cacheRegion).getEhcache();

      Assert.assertTrue(cache.getCacheConfiguration().isTerracottaClustered());
      Assert.assertEquals(ValueMode.SERIALIZATION, cache.getCacheConfiguration().getTerracottaConfiguration()
          .getValueMode());

      String moddedConfig = cache.getCacheManager().getActiveConfigurationText();
      String originalConfig = cache.getCacheManager().getOriginalConfigurationText();
      Assert.assertFalse(originalConfig.equals(moddedConfig));
    } else {
      System.err.println("Expected SessionFactory instance to also implement SessionFactoryImplementor - it doesn't!");
    }
  }
}
