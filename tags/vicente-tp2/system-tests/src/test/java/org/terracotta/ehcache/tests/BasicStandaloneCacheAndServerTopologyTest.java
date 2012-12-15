/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class BasicStandaloneCacheAndServerTopologyTest extends AbstractCacheTestBase {

  int dsoPort;

  public BasicStandaloneCacheAndServerTopologyTest(TestConfig testConfig) {
    super("basic-cache-test-different-server-topology.xml", testConfig);
    testConfig.getClientConfig().setParallelClients(false);
  }

  @Override
  protected void startClients() throws Throwable {
    getTestConfig().getClientConfig().addExtraClientJvmArg("-Dmy.tc.server.topology=127.0.0.1:"
                                                               + getGroupData(0).getDsoPort(0));
    getTestConfig().getClientConfig().addExtraClientJvmArg("-Dtc.server.topology=127.0.0.1:"
                                                               + getGroupData(0).getDsoPort(0));

    runClient(Client3.class);
    runClient(Client4.class);
  }
}
