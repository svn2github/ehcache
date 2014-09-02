/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class MixedCacheTest extends AbstractCacheTestBase {

  public MixedCacheTest(TestConfig testConfig) {
    super("mixed-cache-test.xml", testConfig, Client1.class, Client2.class, UnclusteredClient.class,
          UnclusteredClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

}
