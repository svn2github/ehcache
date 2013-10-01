/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

/**
 * @author cdennis
 */
public class NamelessCacheManagerStandaloneCacheTest extends AbstractCacheTestBase {

  public NamelessCacheManagerStandaloneCacheTest(TestConfig testConfig) {
    super("nameless-cachemanager-cache-test.xml", testConfig);
  }
}
