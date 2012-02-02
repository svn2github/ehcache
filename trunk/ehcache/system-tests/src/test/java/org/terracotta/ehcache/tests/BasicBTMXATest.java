/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class BasicBTMXATest extends AbstractBTMCacheTest {

  public BasicBTMXATest(TestConfig testConfig) {
    super("basic-xa-test.xml", testConfig, BTMSimpleTx1.class, BTMSimpleTx2.class);
  }

}
