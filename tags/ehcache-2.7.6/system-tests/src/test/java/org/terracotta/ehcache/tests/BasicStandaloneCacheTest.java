/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

public class BasicStandaloneCacheTest extends AbstractCacheTestBase {

  public BasicStandaloneCacheTest(TestConfig testConfig) {
    super("basic-cache-test.xml", testConfig);
  }
}
