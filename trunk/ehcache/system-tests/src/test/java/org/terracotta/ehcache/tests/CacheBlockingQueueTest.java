/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class CacheBlockingQueueTest extends AbstractCacheTestBase {

  public CacheBlockingQueueTest(TestConfig testConfig) {
    super("cache-locks-test.xml", testConfig, CacheBlockingQueueClient.class);
  }

}