/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

/**
 * @author Chris Dennis
 */
public class GetKeysSerializedCacheTest extends AbstractCacheTestBase {

  public GetKeysSerializedCacheTest(TestConfig testConfig) {
    super("basic-cache-test.xml", testConfig, GetKeysClient.class, GetKeysClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

}
