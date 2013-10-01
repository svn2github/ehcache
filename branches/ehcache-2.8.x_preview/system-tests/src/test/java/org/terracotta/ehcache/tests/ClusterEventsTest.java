/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class ClusterEventsTest extends AbstractCacheTestBase {

  public ClusterEventsTest(TestConfig testConfig) {
    super("basic-cache-test.xml", testConfig, ClusterEventsWatcherClient.class, ClusterEventsWatcherClient.class);
  }

}
