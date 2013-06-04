/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.scheduledrefresh;

import net.sf.ehcache.Cache;

import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.scheduledrefresh.ScheduledRefreshCacheExtension;
import net.sf.ehcache.constructs.scheduledrefresh.ScheduledRefreshConfiguration;
import org.junit.Assert;
import org.junit.Ignore;
import org.quartz.impl.StdSchedulerFactory;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Ignore
public class ClusteredScheduledRefreshTest extends AbstractCacheTestBase {

  public ClusteredScheduledRefreshTest(TestConfig testConfig) {
    super("scheduled-refresh-cache-test.xml", testConfig, ClusteredScheduledRefreshTestClient.class);
    disableTest();
  }

  @Override
  protected String createClassPath(Class client) throws IOException {
    String s=super.createClassPath(client);
    String sr = TestBaseUtil.jarFor(Cache.class);
    System.out.println("--------------------> "+sr);
    String cp=makeClasspath(s, sr);
    String q = TestBaseUtil.jarFor(StdSchedulerFactory.class);
    System.out.println("--------------------> "+q);
    cp=makeClasspath(cp, q);

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
      dutCache.put(new Element(new Integer(1), new Integer(0)));
      Thread.sleep(60 * 1000);
      Assert.assertTrue(dutCache.get(new Integer(1)).getObjectValue().equals(new Integer(2)));

       // and then more tests...
    }
  }

}
