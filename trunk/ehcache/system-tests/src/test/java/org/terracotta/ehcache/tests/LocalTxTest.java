/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class LocalTxTest extends AbstractCacheTestBase {

  public LocalTxTest(TestConfig testConfig) {
    super("local-tx-test.xml", testConfig, LocalTxClient.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

}
