package net.sf.ehcache.constructs.scheduledrefresh;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

public class ScheduledRefreshCacheExtensionTest {


  public static final int TIMED_WAIT_IN_SECS = 70;
  public static final int POLL_WAIT_INTERVAL_SECS = 5;
  private static OddCacheLoader stupidCacheLoaderOdds = new OddCacheLoader();
   private static EvenCacheLoader stupidCacheLoaderEvens = new EvenCacheLoader();
   private static Logger logger = LoggerFactory.getLogger(ScheduledRefreshCacheExtensionTest.class);

   // OK. we want to create an ehcache, then programmatically decorate it with
   // locks.
   //@Test
   public void testIllegalCronExpression() {

      CacheManager manager = new CacheManager(new Configuration().name("illegal-cron"));

       try {
           manager.addCache(new Cache(new CacheConfiguration().name("test").eternal(true).maxEntriesLocalHeap(5000)));
           Ehcache cache = manager.getEhcache("test");
           cache.registerCacheLoader(stupidCacheLoaderEvens);
           cache.registerCacheLoader(stupidCacheLoaderOdds);

           int second = (new GregorianCalendar().get(Calendar.SECOND) + 5) % 60;
           ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration().batchSize(100).quartzThreadCount
              (4).cronExpression("go to your happy place").build();
           ScheduledRefreshCacheExtension cacheExtension = new ScheduledRefreshCacheExtension(config, cache);
           cache.registerCacheExtension(cacheExtension);
           cacheExtension.init();
           // there will havebeen an exception logged.
           Assert.assertEquals(cacheExtension.getStatus(),Status.STATUS_UNINITIALISED);
       } finally {
           manager.shutdown();
       }
   }

   @Test
   public void testSimpleCaseProgrammatic() throws InterruptedException {

      CacheManager manager = new CacheManager(new Configuration().name("simple-programmatic"));

       try {
           manager.addCache(new Cache(new CacheConfiguration().name("test").eternal(true).maxEntriesLocalHeap(5000)));
           Ehcache cache = manager.getEhcache("test");
           cache.registerCacheLoader(stupidCacheLoaderEvens);
           cache.registerCacheLoader(stupidCacheLoaderOdds);

           ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration().batchSize(100).quartzThreadCount
               (4).cronExpression("0/5 * * * * ?").build();
           ScheduledRefreshCacheExtension cacheExtension = new ScheduledRefreshCacheExtension(config, cache);
           cache.registerCacheExtension(cacheExtension);
           cacheExtension.init();
           Assert.assertEquals(cacheExtension.getStatus(), Status.STATUS_ALIVE);

           for (int i = 0; i < 10; i++) {
              cache.put(new Element(i, i + ""));
           }

         waitOnExtensionState(cacheExtension,TIMED_WAIT_IN_SECS,2);

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

           ExtendedStatistics.Statistic<Number> refreshStat=ScheduledRefreshCacheExtension.findRefreshStatistic(cache);
           if(refreshStat != null) {
             Assert.assertTrue(refreshStat.value().intValue()>1);

             ExtendedStatistics.Statistic<Number> jobStat=ScheduledRefreshCacheExtension.findJobStatistic(cache);
             Assert.assertTrue(refreshStat.value().intValue()>1);

             ExtendedStatistics.Statistic<Number> procStat=ScheduledRefreshCacheExtension.findKeysProcessedStatistic(cache);
             Assert.assertTrue(procStat.value().intValue()>10);
           } else {
             Assert.assertNotNull("Unable to find statistic",refreshStat);
           }
       } finally {
           manager.shutdown();
       }
   }

   private void waitOnExtensionState(ScheduledRefreshCacheExtension cacheExtension,
                                    int timeToWaitSecs,
                                    int minRefreshCount) throws InterruptedException {
    for(;timeToWaitSecs>0;timeToWaitSecs=timeToWaitSecs-POLL_WAIT_INTERVAL_SECS) {
      logger.info("----> Awaiting ... " + cacheExtension.getRefreshCount() + " job(s) seen");
      TimeUnit.SECONDS.sleep(POLL_WAIT_INTERVAL_SECS);
      if(cacheExtension.getRefreshCount()>=minRefreshCount) {
        break;
      }
    }
  }

  // OK. we want to create an ehcache, then programmitaclly decorate it with
   // locks.
   @Test
   public void testSimpleCaseXML() throws InterruptedException {

      CacheManager manager = new CacheManager(getClass().getResourceAsStream("/ehcache-scheduled-refresh.xml"));

       try {
           Cache cache = manager.getCache("sr-test");

           for (int i = 0; i < 10; i++) {
              cache.put(new Element(i, i + ""));
           }

           waitOnExtensionState(ScheduledRefreshCacheExtension.findExtensionsFromCache(cache).iterator().next(),
             TIMED_WAIT_IN_SECS,2);

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
       } finally {
           manager.shutdown();
       }
   }



}