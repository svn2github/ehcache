/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Cache;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

import java.util.Map;

import junit.framework.Assert;

public class SampledStatisticTimerTest extends AbstractCacheTestBase {

  public SampledStatisticTimerTest(TestConfig testConfig) {
    super("sampled-statistic-timer-test.xml", testConfig, App.class);
  }

  public static class App extends ClientBase {

    public App(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
      for (int i = 0; i < 10; i++) {
        String cacheName = "cache-" + i;
        cacheManager.addCache(cacheName);
      }

      Map<Thread, StackTraceElement[]> liveThreads = Thread.getAllStackTraces();
      int samplerThreadCount = 0;
      for (Thread t : liveThreads.keySet()) {
        String threadName = t.getName();
        if (threadName.contains("SampledStatisticsManager Timer")) {
          samplerThreadCount++;
        }
      }
      Assert.assertEquals("Found statistic sampler threads!.", 0, samplerThreadCount);
    }
  }
}
