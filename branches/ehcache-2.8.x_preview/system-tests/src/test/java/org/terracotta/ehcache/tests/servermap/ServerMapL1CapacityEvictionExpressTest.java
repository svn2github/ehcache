/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

import java.util.Iterator;

public class ServerMapL1CapacityEvictionExpressTest extends AbstractCacheTestBase {

  public ServerMapL1CapacityEvictionExpressTest(TestConfig testConfig) {
    super("/servermap/servermap-l1-capacity-test.xml", testConfig, ServerMapL1CapacityEvictionExpressTestClient.class);

    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.l1.lockmanager.timeout.interval=600000");
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.ehcache.evictor.logging.enabled=true");
    final Iterator<String> iter = testConfig.getClientConfig().getExtraClientJvmArgs().iterator();
    while (iter.hasNext()) {
      final String prop = iter.next();
      if (prop.contains("ehcache.storageStrategy.dcv2.localcache.enabled")) {
        // remove it and always enable localcache for this test
        iter.remove();
      }
    }
    // always enable local cache
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.ehcache.storageStrategy.dcv2.localcache.enabled=true");
  }

}
