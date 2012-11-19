package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

public class ServerMapL2EvictionReachesL1Test extends AbstractCacheTestBase {

  public ServerMapL2EvictionReachesL1Test(TestConfig testConfig) {
    super("/servermap/servermap-l2-eviction-reaches-l1-test.xml", testConfig,
          ServerMapL2EvictionReachesL1TestClient.class);
    testConfig.getL2Config().setDgcEnabled(true);
    testConfig.getL2Config().setDgcIntervalInSec(60);
    testConfig.addTcProperty("ehcache.evictor.logging.enabled", "true");

    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.l1.cachemanager.enabled=false");
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.ehcache.evictor.logging.enabled=true");
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.l1.lockmanager.timeout.interval=60000");
  }

}
