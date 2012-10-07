/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

public class DaemonThreadsWriteBehindTest extends AbstractCacheTestBase {

  public DaemonThreadsWriteBehindTest(TestConfig testConfig) {
    super("basic-writebehind-test.xml", testConfig, DaemonThreadsWriteBehindTestClient.class);
  }


}
