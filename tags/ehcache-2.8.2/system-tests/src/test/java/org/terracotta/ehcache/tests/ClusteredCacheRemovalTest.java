package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

/**
 * @author Alex Snaps
 */
public class ClusteredCacheRemovalTest extends AbstractCacheTestBase {
  public ClusteredCacheRemovalTest(TestConfig testConfig) {
    super("one-cache-test.xml", testConfig, CacheRemovalClient.class);
  }
}
