/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class OtherClassloaderCacheTest extends AbstractCacheTestBase {

  public OtherClassloaderCacheTest(TestConfig testConfig) {
    super("small-memory-cache-test.xml", testConfig, OtherClassloaderClient.class, ReaderClient.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

}
