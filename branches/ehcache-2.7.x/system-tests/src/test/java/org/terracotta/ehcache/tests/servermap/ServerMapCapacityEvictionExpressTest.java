/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

import java.util.Iterator;

public class ServerMapCapacityEvictionExpressTest extends AbstractCacheTestBase {

  public ServerMapCapacityEvictionExpressTest(TestConfig testConfig) {
    super("/servermap/basic-servermap-cache-test.xml", testConfig, ServerMapCapacityEvictionExpressTestClient.class);
    testConfig.setDgcEnabled(true);
    testConfig.setDgcIntervalInSec(60);
    testConfig.addTcProperty("ehcache.evictor.logging.enabled", "true");
    testConfig.addTcProperty("l2.servermap.eviction.clientObjectReferences.refresh.interval","500");
    
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
