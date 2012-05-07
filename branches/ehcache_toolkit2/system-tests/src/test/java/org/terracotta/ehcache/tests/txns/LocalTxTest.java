/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.txns;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.ClientConfig;
import com.tc.test.config.model.TestConfig;

public class LocalTxTest extends AbstractCacheTestBase {

  public LocalTxTest(TestConfig testConfig) {
    super("local-tx-test.xml", testConfig, LocalTxClient.class);
    ClientConfig clientConfig = testConfig.getClientConfig();
    clientConfig.setParallelClients(true);
    clientConfig.getBytemanConfig().setScript("/byteman/LocalTxTestDebug.btm");
  }

}
