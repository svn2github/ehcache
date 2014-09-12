/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class SerializedArrayCacheTest extends AbstractCacheTestBase {

  public SerializedArrayCacheTest(TestConfig testConfig) {
    super("basic-cache-test.xml", testConfig, ClientArrayValues1.class, ClientArrayValues2.class);
    testConfig.getClientConfig().setParallelClients(false);
  }

}
