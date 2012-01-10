package org.terracotta.modules.ehcache.hibernate.nontransactional;

import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.terracotta.modules.ehcache.hibernate.BaseClusteredRegionFactoryTestServlet;
import org.terracotta.modules.ehcache.hibernate.domain.Event;
import org.terracotta.modules.ehcache.hibernate.domain.EventManager;
import org.terracotta.modules.ehcache.hibernate.domain.Person;
import org.terracotta.modules.ehcache.hibernate.domain.PhoneNumber;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import junit.framework.Assert;

public class EhCacheClusteredHibernateCacheTestServlet extends BaseClusteredRegionFactoryTestServlet {
  public EhCacheClusteredHibernateCacheTestServlet() {
    //
  }

  private static final long serialVersionUID = 1L;

  @Override
  protected void doServer0(HttpSession httpSession, Map<String, String[]> parameters) throws Exception {
    HibernateUtil.dropAndCreateDatabaseSchema();

    EventManager mgr = new EventManager(HibernateUtil.getSessionFactory());
    Statistics stats = HibernateUtil.getSessionFactory().getStatistics();
    stats.setStatisticsEnabled(true);

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

    HibernateUtil.getSessionFactory().close();

    System.err.println("Second Level Cache Regions");
    for (String region : stats.getSecondLevelCacheRegionNames()) {
      System.err.println("Region : " + region);
      SecondLevelCacheStatistics l2Stats = stats.getSecondLevelCacheStatistics(region);
      System.err.println("\tCache Miss Count " + l2Stats.getMissCount());
      System.err.println("\tCache Hit Count " + l2Stats.getHitCount());
      System.err.println("\tCache Put Count " + l2Stats.getPutCount());
    }

    QueryStatistics queryStats = stats.getQueryStatistics("from Event");
    Assert.assertEquals("Cache Miss Count", 1L, queryStats.getCacheMissCount());
    Assert.assertEquals("Cache Hit Count", 0L, queryStats.getCacheHitCount());
    Assert.assertEquals("Cache Put Count", 1L, queryStats.getCachePutCount());
  }

  @Override
  @SuppressWarnings("deprecation")
  protected void doServer1(HttpSession httpSession, Map<String, String[]> parameters) throws Exception {
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
}
