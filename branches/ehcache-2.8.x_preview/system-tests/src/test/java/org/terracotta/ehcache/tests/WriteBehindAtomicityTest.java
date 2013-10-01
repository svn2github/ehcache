/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class WriteBehindAtomicityTest extends AbstractWriteBehindAtomicityTestBase {

  public WriteBehindAtomicityTest(TestConfig testConfig) {
    super("basic-writebehind-test.xml", testConfig, WriteBehindAtomicityTestClient.class);
  }

}