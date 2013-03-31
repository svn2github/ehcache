package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

public class ServerMapL2EvictionReachesOneL1Test extends AbstractCacheTestBase {

  public ServerMapL2EvictionReachesOneL1Test(TestConfig testConfig) {
    super("/servermap/servermap-l2-eviction-reaches-one-l1-test.xml", testConfig,
          ServerMapL2EvictionReachesOneL1TestClient.class, ServerMapL2EvictionReachesOneL1Verifier.class);

    testConfig.addTcProperty("ehcache.evictor.logging.enabled", "true");

    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.l1.cachemanager.enabled=false");
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.ehcache.evictor.logging.enabled=true");
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.l1.lockmanager.timeout.interval=60000");
  }

}
