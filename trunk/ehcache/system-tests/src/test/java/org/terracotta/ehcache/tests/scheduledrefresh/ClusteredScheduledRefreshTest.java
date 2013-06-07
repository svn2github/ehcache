/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.scheduledrefresh;

import com.tc.test.config.model.TestConfig;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.scheduledrefresh.ScheduledRefreshCacheExtension;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import org.junit.Assert;
import org.junit.Ignore;
import org.quartz.impl.StdSchedulerFactory;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.toolkit.Toolkit;

import java.io.IOException;

@Ignore
public class ClusteredScheduledRefreshTest extends AbstractCacheTestBase {

   public ClusteredScheduledRefreshTest(TestConfig testConfig) {
      super("scheduled-refresh-cache-test.xml", testConfig,
          ClusteredScheduledRefreshTestClient.class,
          ClusteredScheduledRefreshTestClient.class,
          ClusteredScheduledRefreshTestClient.class,
          ClusteredScheduledRefreshTestClient.class);
      disableTest();
   }

   @Override
   protected String createClassPath(Class client) throws IOException {
      String s = super.createClassPath(client);
      String sr = TestBaseUtil.jarFor(Cache.class);
      String cp = makeClasspath(s, sr);
      String q = TestBaseUtil.jarFor(StdSchedulerFactory.class);
      cp = makeClasspath(cp, q);

      return cp;
   }

   public static class ClusteredScheduledRefreshTestClient extends ClientBase {

      public ClusteredScheduledRefreshTestClient(String[] args) {
         super("scheduledRefreshCache", args);
      }

      public static void main(String[] args) {
         new ClusteredScheduledRefreshTestClient(args).run();
      }

      @Override
      protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
         Cache dutCache = cache.getCacheManager().getCache("scheduledRefreshCache");
         for (int i = 0; i < 1000; i++) {
            dutCache.put(new Element(new Integer(i), new Integer(i)));
         }
         Thread.sleep(60 * 1000);
         for (int i = 0; i < 1000; i++) {
            Assert.assertTrue(dutCache.get(new Integer(i)).getObjectValue().equals(new Integer(i + 1)));
         }


         ExtendedStatistics.Statistic<Number> refreshStat = ScheduledRefreshCacheExtension.findRefreshStatistic(cache);
         System.out.println("Refresh jobs: " + refreshStat.value().intValue());

         ExtendedStatistics.Statistic<Number> jobStat = ScheduledRefreshCacheExtension.findJobStatistic(cache);
         System.out.println("Batch jobs: " + jobStat.value().intValue());

         ExtendedStatistics.Statistic<Number> procStat = ScheduledRefreshCacheExtension.findKeysProcessedStatistic(cache);
         System.out.println("Keys processed: " + procStat.value().intValue());
         // and then more tests...
         dutCache.dispose();
      }
   }

}
