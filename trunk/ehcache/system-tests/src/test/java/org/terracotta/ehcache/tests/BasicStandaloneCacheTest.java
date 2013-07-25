/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

import com.tc.test.config.model.TestConfig;

@Category(CheckShorts.class)
public class BasicStandaloneCacheTest extends AbstractCacheTestBase {

  public BasicStandaloneCacheTest(TestConfig testConfig) {
    super("basic-cache-test.xml", testConfig);
  }
}
