/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

import java.util.Iterator;

public class ServerMapL1EvictionOffHeapDestroyExpressTest extends AbstractCacheTestBase {

  public ServerMapL1EvictionOffHeapDestroyExpressTest(TestConfig testConfig) {
    super("/servermap/servermap-l1-eviction-destroy-off-heap-test.xml", testConfig,
          ServerMapL1EvictionOffHeapDestroyExpressTestClient.class);
    testConfig.getL2Config().setDgcEnabled(true);
    // shut off inline dgc
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.l2.objectmanager.dgc.inline.enabled=false");

    // set low threshold for eviction from on-heap to cause more faulting
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.l2.cachemanager.criticalThreshold = 10");

    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.l2.cachemanager.threshold = 5");

    // enable fault & flush logging
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.l2.objectmanager.flush.logging.enabled=true");
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.l2.objectmanager.fault.logging.enabled=true");

    // add L1 jvm arguments
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.l1.lockmanager.timeout.interval=600000");
    // ehcache evictor logging not needed for this test
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.ehcache.evictor.logging.enabled=false");
    testConfig.getClientConfig().addExtraClientJvmArg("-XX:+HeapDumpOnOutOfMemoryError");
    final Iterator<String> iter = testConfig.getClientConfig().getExtraClientJvmArgs().iterator();
    while (iter.hasNext()) {
      final String prop = iter.next();
      if (prop.contains("ehcache.storageStrategy.dcv2.localcache.enabled")) {
        // remove it and disable local cache for this test
        iter.remove();
      }
    }
    // disable local cache to cause reads/writes to go to server
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.ehcache.storageStrategy.dcv2.localcache.enabled=false");
  }

}
