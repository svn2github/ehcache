/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class CompressedCacheTest extends AbstractCacheTestBase {

  public CompressedCacheTest(TestConfig testConfig) {
    super("compressed-cache-test.xml", testConfig);
    testConfig.getClientConfig().setParallelClients(false);
  }
}
