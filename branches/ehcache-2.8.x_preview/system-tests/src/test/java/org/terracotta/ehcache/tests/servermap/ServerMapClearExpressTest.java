package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

public class ServerMapClearExpressTest extends AbstractCacheTestBase {

  public ServerMapClearExpressTest(TestConfig testConfig) {
    super("/servermap/servermap-clear-test.xml", testConfig, ServerMapClearExpressTestClient1.class,
          ServerMapClearExpressTestClient2.class);
  }

}
