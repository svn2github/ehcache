/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.hibernate.nontransactional;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.terracotta.modules.ehcache.hibernate.BaseClusteredRegionFactoryTestServlet;
import org.terracotta.modules.ehcache.hibernate.domain.VersionedItem;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import junit.framework.Assert;

public class StaleWritesLeaveCacheConsistentTestServlet extends BaseClusteredRegionFactoryTestServlet {

  @Override
  protected void doServer0(HttpSession session, Map<String, String[]> parameters) throws Exception {
    HibernateUtil.dropAndCreateDatabaseSchema();

    Session s = openSession();
    Transaction txn = s.beginTransaction();
    VersionedItem item = new VersionedItem();
    item.setName("chris");
    item.setDescription("chris' item");
    s.save(item);
    txn.commit();
    s.close();

    Long initialVersion = item.getVersion();

    // manually revert the version property
    item.setVersion(Long.valueOf(initialVersion.longValue() - 1));
    item.setName("tim");
    item.setDescription("tim's item");
    s = openSession();
    try {
      txn = s.beginTransaction();
      try {
        s.update(item);
        txn.commit();
        s.close();
        Assert.fail("expected stale write to fail");
      } catch (Throwable expected) {
        txn.rollback();
      }
    } finally {
      if (s.isOpen()) {
        s.close();
      }
    }

    // check the version value in the cache...
    s = openSession();
    txn = s.beginTransaction();
    VersionedItem check = (VersionedItem) s.get(VersionedItem.class, item.getId());
    Assert.assertEquals(initialVersion, check.getVersion());
    txn.commit();
    s.close();
  }

  @Override
  protected void doServer1(HttpSession session, Map<String, String[]> parameters) throws Exception {
    // check the version value in the cache...
    Session s = openSession();
    Transaction txn = s.beginTransaction();
    List<Long> ids = s.createQuery("select id from VersionedItem").list();
    for (Long id : ids) {
      VersionedItem item = (VersionedItem) s.get(VersionedItem.class, id);
      Assert.assertEquals("chris", item.getName());
    }
    txn.commit();
    s.close();
  }

  private Session openSession() {
    return HibernateUtil.getSessionFactory().openSession();
  }

}
