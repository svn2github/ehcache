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

}
