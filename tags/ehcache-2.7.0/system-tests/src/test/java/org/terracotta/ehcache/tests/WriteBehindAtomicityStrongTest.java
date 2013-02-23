/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class WriteBehindAtomicityStrongTest extends AbstractCacheTestBase {

  public WriteBehindAtomicityStrongTest(TestConfig testConfig) {
    super("synchronous-writebehind-test.xml", testConfig, WriteBehindAtomicityTestClient.class);
  }

}