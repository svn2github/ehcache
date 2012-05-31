/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class CopyOnWriteTest extends AbstractCacheTestBase {

  public CopyOnWriteTest(TestConfig testConfig) {
    super("copy-on-write-test.xml", testConfig, CopyOnWriteClient.class);

    // DEV-7436 -- presumably toolkit2 will allow this test to pass
    timebombTest("2012-07-01");
  }
}
