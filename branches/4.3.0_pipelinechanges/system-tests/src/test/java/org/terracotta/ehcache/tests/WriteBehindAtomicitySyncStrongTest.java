/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class WriteBehindAtomicitySyncStrongTest extends AbstractWriteBehindAtomicityTestBase {

  public WriteBehindAtomicitySyncStrongTest(TestConfig testConfig) {
    super("strong-writebehind-test.xml", testConfig, WriteBehindAtomicityTestClient.class);
  }

}