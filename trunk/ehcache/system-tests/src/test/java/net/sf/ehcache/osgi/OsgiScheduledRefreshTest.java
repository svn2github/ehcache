/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.osgi;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.scheduledrefresh.ScheduledRefreshCacheExtension;
import net.sf.ehcache.constructs.scheduledrefresh.ScheduledRefreshConfiguration;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.terracotta.test.OsgiUtil;

import java.util.Calendar;
import java.util.GregorianCalendar;

import junit.framework.Assert;

/**
 * Adapt ScheduledRefreshCacheExtensionTest to run with OSGi.
 * <p>
 * This test duplicates a few test classes in ehcache-scheduled-refresh to simplify bundle loading for osgi deployment
 * </p>
 * 
 * @author cschanck
 * @author hhuynh
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OsgiScheduledRefreshTest {

  private static OddCacheLoader  stupidCacheLoaderOdds  = new OddCacheLoader();
  private static EvenCacheLoader stupidCacheLoaderEvens = new EvenCacheLoader();

  public OsgiScheduledRefreshTest() {
    Thread.currentThread().setContextClassLoader(OsgiScheduledRefreshTest.class.getClassLoader());
  }

  @Configuration
  public Option[] config() {
    return options(OsgiUtil.getMavenBundle("net.sf.ehcache", "ehcache-ee", "ehcache"),
                   OsgiUtil.getMavenBundle("org.quartz-scheduler", "quartz"), wrappedBundle(maven("c3p0", "c3p0")
                       .versionAsInProject()), OsgiUtil.commonOptions());
  }

  private static void sleepySeconds(int secs) {
    sleepy(secs * 1000);
  }

  private static void sleepy(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      //
    }
  }

  // OK. we want to create an ehcache, then programmitically decorate it with
  // locks.
  @Test
  public void testIllegalCronExpression() {

    CacheManager manager = new CacheManager();
    manager.removeAllCaches();

    manager.addCache(new Cache(new CacheConfiguration().name("test").eternal(true).maxEntriesLocalHeap(5000)));
    Ehcache cache = manager.getEhcache("test");
    cache.registerCacheLoader(stupidCacheLoaderEvens);
    cache.registerCacheLoader(stupidCacheLoaderOdds);

    ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration().batchSize(100).quartzThreadCount(4)
        .cronExpression("go to your happy place").build();
    ScheduledRefreshCacheExtension cacheExtension = new ScheduledRefreshCacheExtension(config, cache);
    cache.registerCacheExtension(cacheExtension);
    cacheExtension.init();
    // there will havebeen an exception logged.
    Assert.assertEquals(cacheExtension.getStatus(), Status.STATUS_UNINITIALISED);

    manager.removeAllCaches();
    manager.shutdown();
  }

  @Test
  public void testSimpleCaseProgrammatic() {

    CacheManager manager = new CacheManager();
    manager.removeAllCaches();

    manager.addCache(new Cache(new CacheConfiguration().name("test").eternal(true).maxEntriesLocalHeap(5000)));
    Ehcache cache = manager.getEhcache("test");
    cache.registerCacheLoader(stupidCacheLoaderEvens);
    cache.registerCacheLoader(stupidCacheLoaderOdds);

    int second = (new GregorianCalendar().get(Calendar.SECOND) + 5) % 60;
    ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration().batchSize(100).quartzThreadCount(4)
        .cronExpression(second + "/5 * * * * ?").build();
    ScheduledRefreshCacheExtension cacheExtension = new ScheduledRefreshCacheExtension(config, cache);
    cache.registerCacheExtension(cacheExtension);
    cacheExtension.init();
    Assert.assertEquals(cacheExtension.getStatus(), Status.STATUS_ALIVE);

    for (int i = 0; i < 10; i++) {
      cache.put(new Element(new Integer(i), i + ""));
    }

    sleepySeconds(8);

    for (Object key : cache.getKeys()) {
      Element val = cache.get(key);
      // System.out.println("["+key+", "+cache.get(key).getObjectValue()+"]");
      int iVal = ((Number) key).intValue();
      if ((iVal & 0x01) == 0) {
        // even
        Assert.assertEquals(iVal + 20000, Long.parseLong((String) val.getObjectValue()));
      } else {
        Assert.assertEquals(iVal + 10000, Long.parseLong((String) val.getObjectValue()));
        // odd
      }

    }

    ExtendedStatistics.Statistic<Number> refreshStat = ScheduledRefreshCacheExtension.findRefreshStatistic(cache);
    Assert.assertEquals(1, refreshStat.value().intValue());

    ExtendedStatistics.Statistic<Number> jobStat = ScheduledRefreshCacheExtension.findJobStatistic(cache);
    Assert.assertEquals(1, jobStat.value().intValue());

    ExtendedStatistics.Statistic<Number> procStat = ScheduledRefreshCacheExtension.findKeysProcessedStatistic(cache);
    Assert.assertEquals(10, procStat.value().intValue());

    // cacheExtension.dispose();
    manager.removeAllCaches();
    manager.shutdown();
  }

  // OK. we want to create an ehcache, then programmitaclly decorate it with
  // locks.
  @Test
  public void testSimpleCaseXML() throws Exception {
    CacheManager manager = new CacheManager(
                                            OsgiScheduledRefreshTest.class
                                                .getResource("/net/sf/ehcache/osgi/ehcache-scheduled-refresh.xml"));

    Cache cache = manager.getCache("sr-test");

    int second = (new GregorianCalendar().get(Calendar.SECOND) + 5) % 60;

    for (int i = 0; i < 10; i++) {
      cache.put(new Element(new Integer(i), i + ""));
    }

    second = Math.max(8, 60 - second + 3);
    System.out.println("Scheduled delay is :: " + second);
    sleepySeconds(second);

    for (Object key : cache.getKeys()) {
      Element val = cache.get(key);
      // System.out.println("["+key+", "+cache.get(key).getObjectValue()+"]");
      int iVal = ((Number) key).intValue();
      if ((iVal & 0x01) == 0) {
        // even
        Assert.assertEquals(iVal + 20000, Long.parseLong((String) val.getObjectValue()));
      } else {
        Assert.assertEquals(iVal + 10000, Long.parseLong((String) val.getObjectValue()));
        // odd
      }

    }
    manager.removeAllCaches();

    manager.shutdown();
  }

  // OK. we want to create an ehcache, then programmitically decorate it with
  // locks.
  @Test
  public void testPolling() {

    CacheManager manager = new CacheManager();
    manager.removeAllCaches();

    manager.addCache(new Cache(new CacheConfiguration().name("tt").eternal(true).maxEntriesLocalHeap(5000)
        .overflowToDisk(false)));
    Ehcache cache = manager.getEhcache("tt");
    stupidCacheLoaderEvens.setMsDelay(100);
    cache.registerCacheLoader(stupidCacheLoaderEvens);
    cache.registerCacheLoader(stupidCacheLoaderOdds);

    int second = (new GregorianCalendar().get(Calendar.SECOND) + 5) % 60;
    ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration().batchSize(2).quartzThreadCount(2)
        .pollTimeMs(100).cronExpression(second + "/1 * * * * ?").build();
    ScheduledRefreshCacheExtension cacheExtension = new ScheduledRefreshCacheExtension(config, cache);
    cache.registerCacheExtension(cacheExtension);
    cacheExtension.init();
    Assert.assertEquals(cacheExtension.getStatus(), Status.STATUS_ALIVE);

    final int ELEMENT_COUNT = 50;
    long[] orig = new long[ELEMENT_COUNT];
    for (int i = 0; i < ELEMENT_COUNT; i++) {
      Element elem = new Element(new Integer(i), i + "");
      orig[i] = elem.getCreationTime();
      cache.put(elem);
    }

    sleepySeconds(20);

    // cacheExtension.dispose();
    manager.removeAllCaches();
    manager.shutdown();
  }
}
