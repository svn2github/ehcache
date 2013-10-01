package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

/**
 * @author Alex Snaps
 */
public class ClusterCacheEventsRejoinEnabledTest extends AbstractCacheTestBase {

  public ClusterCacheEventsRejoinEnabledTest(TestConfig testConfig) {
    super("basic-rejoin-cache-test.xml", testConfig, ClusterCacheEventsRejoinEnabledClient.class,
          ClusterCacheEventsRejoinEnabledClient.class);
  }

}
