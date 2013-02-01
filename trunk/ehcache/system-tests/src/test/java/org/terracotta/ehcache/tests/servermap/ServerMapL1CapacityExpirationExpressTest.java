/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

import java.util.Iterator;

public class ServerMapL1CapacityExpirationExpressTest extends AbstractCacheTestBase {

  public ServerMapL1CapacityExpirationExpressTest(TestConfig testConfig) {
    super("/servermap/servermap-l1-capacity-test.xml", testConfig, ServerMapL1CapacityExpirationExpressTestClient.class);
    testConfig.getL2Config().addExtraServerJvmArg("-Dcom.tc.ehcache.evictor.logging.enabled=true");

    final Iterator<String> iter = testConfig.getClientConfig().getExtraClientJvmArgs().iterator();
    while (iter.hasNext()) {
      final String prop = iter.next();
      if (prop.contains("ehcache.storageStrategy.dcv2.localcache.enabled")) {
        // remove it and always enable local cache for this test
        iter.remove();
      }
    }
    // always enable local cache
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.ehcache.storageStrategy.dcv2.localcache.enabled=true");
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.ehcache.evictor.logging.enabled=true");
  }

}
