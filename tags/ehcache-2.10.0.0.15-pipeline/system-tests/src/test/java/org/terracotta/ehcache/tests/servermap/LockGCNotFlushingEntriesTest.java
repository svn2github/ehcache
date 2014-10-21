/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

public class LockGCNotFlushingEntriesTest extends AbstractCacheTestBase {

  public LockGCNotFlushingEntriesTest(TestConfig testConfig) {
    super("/servermap/lock-gc-test.xml", testConfig, LockGCNotFlushingEntriesTestClient.class);
    testConfig.getClientConfig().addExtraClientJvmArg("-Dcom.tc.l1.lockmanager.timeout.interval=5000");
  }

}
