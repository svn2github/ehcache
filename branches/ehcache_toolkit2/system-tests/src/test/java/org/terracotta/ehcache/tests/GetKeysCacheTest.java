/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

/**
 * @author Chris Dennis
 */
public class GetKeysCacheTest extends AbstractCacheTestBase {

  public GetKeysCacheTest(TestConfig testConfig) {
    super("basic-cache-test.xml", testConfig, GetKeysClient.class, GetKeysClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

}
