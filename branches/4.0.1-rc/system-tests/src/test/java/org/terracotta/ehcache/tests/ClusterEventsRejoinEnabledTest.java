/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class ClusterEventsRejoinEnabledTest extends AbstractCacheTestBase {

  public ClusterEventsRejoinEnabledTest(TestConfig testConfig) {
    super("basic-rejoin-cache-test.xml", testConfig, ClusterEventsRejoinEnabledWatcherClient.class,
          ClusterEventsRejoinEnabledWatcherClient.class);
  }

}
