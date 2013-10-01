package net.sf.ehcache.constructs.scheduledrefresh;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created with IntelliJ IDEA.
 * User: cschanck
 * Date: 6/17/13
 * Time: 12:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScheduledRefreshConfigurationTest {

   @Test
   public void testFreezing() {

      ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration()
         .batchSize(10)
         .quartzThreadCount(4)
         .parallelJobCount(5)
         .pollTimeMs(100)
         .cronExpression("0/5 * * * * ?");

      config.setParallelJobCount(10);
      config.setJobStoreFactoryClassName("foo");
      config.setTerracottaConfigUrl("bar");
      config.setBatchSize(10);
      config.setCronExpression("ffff");
      config.setEvictOnLoadMiss(true);
      config.setKeyGeneratorClass("foo bar");
      config.setPollTimeMs(1000);
      config.setQuartzThreadCount(111);
      config.setUseBulkload(true);

      config.build();

      try {
         config.setParallelJobCount(10);
         Assert.fail();
      } catch(IllegalStateException e) {
      }

      try {
         config.setJobStoreFactoryClassName("foo");
         Assert.fail();
      } catch(IllegalStateException e) {
      }

      try {
         config.setTerracottaConfigUrl("bar");
         Assert.fail();
      } catch(IllegalStateException e) {
      }

      try {
         config.setBatchSize(10);
         Assert.fail();
      } catch(IllegalStateException e) {
      }

      try {
         config.setCronExpression("ffff");
         Assert.fail();
      } catch(IllegalStateException e) {
      }

      try {
         config.setEvictOnLoadMiss(true);
         Assert.fail();
      } catch(IllegalStateException e) {
      }

      try {
         config.setKeyGeneratorClass("foo bar");
         Assert.fail();
      } catch(IllegalStateException e) {
      }

      try {
         config.setPollTimeMs(1000);
         Assert.fail();
      } catch(IllegalStateException e) {
      }

      try {
         config.setQuartzThreadCount(111);
         Assert.fail();
      } catch(IllegalStateException e) {
      }

      try {
         config.setUseBulkload(true);
         Assert.fail();
      } catch(IllegalStateException e) {
      }


   }

   // OK. we want to create an ehcache, then programmitically decorate it with
   // locks.
   @Test
   public void testMisconfiguration() {

      try {
         ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration()
            .batchSize(-10)
            .quartzThreadCount(4)
            .parallelJobCount(5)
            .pollTimeMs(100)
            .cronExpression("0/5 * * * * ?").build();
         Assert.fail();
      } catch(IllegalArgumentException e) {
      }

      try {
         ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration()
            .batchSize(10)
            .quartzThreadCount(0)
            .parallelJobCount(5)
            .pollTimeMs(100)
            .cronExpression("0/5 * * * * ?").build();
         Assert.fail();
      } catch(IllegalArgumentException e) {
      }

      try {
         ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration()
            .batchSize(10)
            .quartzThreadCount(4)
            .parallelJobCount(1)
            .pollTimeMs(100)
            .cronExpression("0/5 * * * * ?").build();
         Assert.fail();
      } catch(IllegalArgumentException e) {
      }

      try {
         ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration()
            .batchSize(10)
            .quartzThreadCount(4)
            .parallelJobCount(5)
            .pollTimeMs(10000000)
            .cronExpression("0/5 * * * * ?").build();
         Assert.fail();
      } catch(IllegalArgumentException e) {
      }

      try {
         ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration()
            .batchSize(10)
            .quartzThreadCount(4)
            .parallelJobCount(5)
            .pollTimeMs(-100)
            .cronExpression("0/5 * * * * ?").build();
         Assert.fail();
      } catch(IllegalArgumentException e) {
      }

      try {
         ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration()
            .batchSize(10)
            .quartzThreadCount(4)
            .parallelJobCount(5)
            .pollTimeMs(100)
            .cronExpression(null).build();
         Assert.fail();
      } catch(IllegalArgumentException e) {
      }

   }

   public void testEng10MathildeFail() {
      ScheduledRefreshConfiguration config = new ScheduledRefreshConfiguration()
         .cronExpression( (new GregorianCalendar().get(Calendar.SECOND) + 5) % 60 + "/5 * * * * ?")
         .jobStoreFactory("toto");


   }
}
