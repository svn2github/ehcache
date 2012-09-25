/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

import java.util.Iterator;

public class ServerMapTTLExpressTest extends AbstractCacheTestBase {

  public ServerMapTTLExpressTest(TestConfig testConfig) {
    super("/servermap/basic-servermap-cache-test.xml", testConfig, ServerMapTTLExpressTestClient.class);
    testConfig.getL2Config().setDgcEnabled(true);
    testConfig.getL2Config().setDgcIntervalInSec(60);
    testConfig.addTcProperty("ehcache.evictor.logging.enabled", "true");

    // L1 properties
    final Iterator<String> iter = testConfig.getClientConfig().getExtraClientJvmArgs().iterator();
    while (iter.hasNext()) {
      final String prop = iter.next();
      if (prop.contains("ehcache.storageStrategy.dcv2.localcache.enabled")) {
        // remove it and always disable localcache for this test
        iter.remove();
      }
    }
    // always disable local cache
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.ehcache.storageStrategy.dcv2.localcache.enabled=false");
  }

}