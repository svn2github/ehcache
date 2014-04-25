/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class EmbeddedEhcacheJarTest extends AbstractCacheTestBase {
  public EmbeddedEhcacheJarTest(TestConfig testConfig) {
    super("basic-cache-test.xml", testConfig, EmbeddedEhcacheJarTestClient.class);
  }
}
