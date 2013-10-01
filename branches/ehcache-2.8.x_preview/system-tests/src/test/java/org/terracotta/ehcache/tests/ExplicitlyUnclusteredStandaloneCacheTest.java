/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.test.config.model.TestConfig;

/**
 * @author cdennis
 */
public class ExplicitlyUnclusteredStandaloneCacheTest extends AbstractCacheTestBase {

  public ExplicitlyUnclusteredStandaloneCacheTest(TestConfig testConfig) {
    super("explicitly-unclustered-cache-test.xml", testConfig, UnclusteredClient.class, UnclusteredClient.class);
  }

}
