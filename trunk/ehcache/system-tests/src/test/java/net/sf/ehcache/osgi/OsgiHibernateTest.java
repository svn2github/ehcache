/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.osgi;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.terracotta.ehcache.tests.container.hibernate.domain.Event;
import org.terracotta.ehcache.tests.container.hibernate.domain.EventManager;
import org.terracotta.ehcache.tests.container.hibernate.domain.Item;
import org.terracotta.ehcache.tests.container.hibernate.domain.Person;
import org.terracotta.ehcache.tests.container.hibernate.domain.PhoneNumber;
import org.terracotta.ehcache.tests.container.hibernate.domain.VersionedItem;
import org.terracotta.test.OsgiUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * @author Chris Dennis
 * @author hhuynh
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OsgiHibernateTest {

  private SessionFactory  sessionFactory;
  private Configuration   config;

  /**
   * Injected BundleContext
   */
  @Inject
  protected BundleContext bc;

  public OsgiHibernateTest() {
    Thread.currentThread().setContextClassLoader(OsgiHibernateTest.class.getClassLoader());
  }

  public void testBundle() {
    assertThat(bc, is(notNullValue()));
    System.out.println(Arrays.asList(bc.getBundles()));
  }

  @org.ops4j.pax.exam.Configuration
  public Option[] config() {
    return options(bootDelegationPackages("sun.*,javax.naming,javax.naming.spi,javax.naming.event,javax.management"),
                   OsgiUtil.commonOptions(), wrappedBundle(maven("javax.transaction", "jta").versionAsInProject())
                       .exports("javax.transaction;version=1.1"), OsgiUtil.getMavenBundle("net.sf.ehcache", "ehcache"),
                   mavenBundle("net.sf.ehcache.test", "hibernate-ehcache-bundle").versionAsInProject().noStart(),
                   wrappedBundle(maven("org.apache.derby", "derby").versionAsInProject()),
                   systemProperty("derby.system.home").value("derby"));
  }

  @ProbeBuilder
  public TestProbeBuilder extendProbe(TestProbeBuilder builder) {
    builder.setHeader(Constants.IMPORT_PACKAGE,
                      "javax.transaction;version=1.1,org.hibernate,org.osgi.framework,org.slf4j");
    builder.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*");
    return builder;
  }

  public synchronized SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      try {
        sessionFactory = config.buildSessionFactory();
      } catch (HibernateException ex) {
        System.err.println("Initial SessionFactory creation failed." + ex);
        throw new ExceptionInInitializerError(ex);
      }
    }
    return sessionFactory;
  }

  private void printBundles() {
    for (Bundle b : bc.getBundles()) {
      System.out.println("XXX Bundle " + b.getSymbolicName() + ", " + b.getVersion() + ", state:  " + b.getState());
    }
  }

  @Before
  public void setUp() throws Exception {
    printBundles();
    config = new Configuration().configure(OsgiHibernateTest.class
        .getResource("/net/sf/ehcache/osgi/hibernate.cfg.xml"));
    config.setProperty("hibernate.hbm2ddl.auto", "create-drop");
    getSessionFactory().getStatistics().setStatisticsEnabled(true);
    removeCaches();
  }

  @After
  public void tearDown() {
    getSessionFactory().close();
  }

  private void removeCaches() {
    for (CacheManager manager : CacheManager.ALL_CACHE_MANAGERS) {
      for (String s : manager.getCacheNames()) {
        final Cache cache = manager.getCache(s);
        if (cache.getStatus() == Status.STATUS_ALIVE) {
          cache.removeAll();
        }
      }
    }
  }

  @Test
  public void testQueryCacheInvalidation() throws Exception {
    Session s = getSessionFactory().openSession();
    Transaction t = s.beginTransaction();
    Item i = new Item();
    i.setName("widget");
    i.setDescription("A really top-quality, full-featured widget.");
    s.persist(i);
    t.commit();
    s.close();

    SecondLevelCacheStatistics slcs = s.getSessionFactory().getStatistics()
        .getSecondLevelCacheStatistics(Item.class.getName());

    assertEquals(1, slcs.getPutCount());
    assertEquals(1, slcs.getElementCountInMemory());
    assertEquals(1, slcs.getEntries().size());

    s = getSessionFactory().openSession();
    t = s.beginTransaction();
    i = (Item) s.get(Item.class, i.getId());

    assertEquals(1, slcs.getHitCount());
    assertEquals(0, slcs.getMissCount());

    i.setDescription("A bog standard item");

    t.commit();
    s.close();

    assertEquals(2, slcs.getPutCount());

    Object entry = slcs.getEntries().get(i.getId());
    Map map;
    if (entry instanceof Map) {
      map = (Map) entry;
    } else {
      Method valueMethod = entry.getClass().getDeclaredMethod("getValue", (Class[]) null);
      valueMethod.setAccessible(true);
      map = (Map) valueMethod.invoke(entry, (Object[]) null);
    }
    assertTrue(map.get("description").equals("A bog standard item"));
    assertTrue(map.get("name").equals("widget"));

    // cleanup
    s = getSessionFactory().openSession();
    t = s.beginTransaction();
    s.delete(i);
    t.commit();
    s.close();
  }

  @Test
  public void testEmptySecondLevelCacheEntry() throws Exception {
    getSessionFactory().evictEntity(Item.class.getName());
    Statistics stats = getSessionFactory().getStatistics();
    stats.clear();
    SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(Item.class.getName());
    Map cacheEntries = statistics.getEntries();
    assertEquals(0, cacheEntries.size());
  }

  @Test
  public void testStaleWritesLeaveCacheConsistent() {
    Session s = getSessionFactory().openSession();
    Transaction txn = s.beginTransaction();
    VersionedItem item = new VersionedItem();
    item.setName("steve");
    item.setDescription("steve's item");
    s.save(item);
    txn.commit();
    s.close();

    Long initialVersion = item.getVersion();

    // manually revert the version property
    item.setVersion(new Long(item.getVersion().longValue() - 1));

    try {
      s = getSessionFactory().openSession();
      txn = s.beginTransaction();
      s.update(item);
      txn.commit();
      s.close();
      fail("expected stale write to fail");
    } catch (Throwable expected) {
      // expected behavior here
      if (txn != null) {
        try {
          txn.rollback();
        } catch (Throwable ignore) {
          //
        }
      }
    } finally {
      if (s != null && s.isOpen()) {
        try {
          s.close();
        } catch (Throwable ignore) {
          //
        }
      }
    }

    // check the version value in the cache...
    SecondLevelCacheStatistics slcs = getSessionFactory().getStatistics()
        .getSecondLevelCacheStatistics(VersionedItem.class.getName());

    Object entry = slcs.getEntries().get(item.getId());
    Long cachedVersionValue;
    if (entry instanceof SoftLock) {
      // FIXME don't know what to test here
      // cachedVersionValue = new Long( ( (ReadWriteCache.Lock)
      // entry).getUnlockTimestamp() );
    } else {
      cachedVersionValue = (Long) ((Map) entry).get("_version");
      assertEquals(initialVersion.longValue(), cachedVersionValue.longValue());
    }

    // cleanup
    s = getSessionFactory().openSession();
    txn = s.beginTransaction();
    item = (VersionedItem) s.load(VersionedItem.class, item.getId());
    s.delete(item);
    txn.commit();
    s.close();

  }

  @Test
  public void testGeneralUsage() throws Exception {
    EventManager mgr = new EventManager(getSessionFactory());
    Statistics stats = getSessionFactory().getStatistics();

    // create 3 persons Steve, Orion, Tim
    Person stevePerson = new Person();
    stevePerson.setFirstname("Steve");
    stevePerson.setLastname("Harris");
    Long steveId = mgr.createAndStorePerson(stevePerson);
    mgr.addEmailToPerson(steveId, "steve@tc.com");
    mgr.addEmailToPerson(steveId, "sharrif@tc.com");
    mgr.addTalismanToPerson(steveId, "rabbit foot");
    mgr.addTalismanToPerson(steveId, "john de conqueroo");

    PhoneNumber p1 = new PhoneNumber();
    p1.setNumberType("Office");
    p1.setPhone(111111);
    mgr.addPhoneNumberToPerson(steveId, p1);

    PhoneNumber p2 = new PhoneNumber();
    p2.setNumberType("Home");
    p2.setPhone(222222);
    mgr.addPhoneNumberToPerson(steveId, p2);

    Person orionPerson = new Person();
    orionPerson.setFirstname("Orion");
    orionPerson.setLastname("Letizi");
    Long orionId = mgr.createAndStorePerson(orionPerson);
    mgr.addEmailToPerson(orionId, "orion@tc.com");
    mgr.addTalismanToPerson(orionId, "voodoo doll");

    Long timId = mgr.createAndStorePerson("Tim", "Teck");
    mgr.addEmailToPerson(timId, "teck@tc.com");
    mgr.addTalismanToPerson(timId, "magic decoder ring");

    Long engMeetingId = mgr.createAndStoreEvent("Eng Meeting", stevePerson, new Date());
    mgr.addPersonToEvent(steveId, engMeetingId);
    mgr.addPersonToEvent(orionId, engMeetingId);
    mgr.addPersonToEvent(timId, engMeetingId);

    Long docMeetingId = mgr.createAndStoreEvent("Doc Meeting", orionPerson, new Date());
    mgr.addPersonToEvent(steveId, docMeetingId);
    mgr.addPersonToEvent(orionId, docMeetingId);

    for (Event event : (List<Event>) mgr.listEvents()) {
      mgr.listEmailsOfEvent(event.getId());
    }

    QueryStatistics queryStats = stats.getQueryStatistics("from Event");
    assertEquals("Cache Miss Count", 1L, queryStats.getCacheMissCount());
    assertEquals("Cache Hit Count", 0L, queryStats.getCacheHitCount());
    assertEquals("Cache Put Count", 1L, queryStats.getCachePutCount());
  }
}
