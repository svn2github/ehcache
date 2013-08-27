/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.terracotta.AbstractTerracottaActivePassiveTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

public class L2SizeDelayTest extends AbstractTerracottaActivePassiveTestBase {
  public L2SizeDelayTest(TestConfig testConfig) {
    super("l2-strong-cache-config.xml", testConfig, L2SizeDelayTestClient.class);
    testConfig.getL2Config().setMinHeap(512);
    testConfig.getL2Config().setMaxHeap(512);
    testConfig.getClientConfig().setMaxHeap(512);
    testConfig.getClientConfig().addExtraClientJvmArg("-XX:+UseParallelGC");
  }

  public static class L2SizeDelayTestClient extends ClientBase {
    public L2SizeDelayTestClient(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new L2SizeDelayTestClient(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      cache = cacheManager.getCache("testEventual");

      int size = 40 * 1024 * 1024;
      byte[] b = new byte[size];

      long time = System.currentTimeMillis();

      cache.put(new Element(1, b));

      while (cache.getStatistics().getLocalHeapSizeInBytes() < size) {
        Thread.sleep(1);
      }

      time = System.currentTimeMillis() - time;

      System.err.println("time taken = " + time + " " + cache.getStatistics().getLocalOffHeapSizeInBytes() + " "
                         + cache.getStatistics().getLocalHeapSizeInBytes());
    }
  }

}
