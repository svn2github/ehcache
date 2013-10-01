/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

public class L1BMOnHeapSanityTest extends AbstractCacheTestBase {
  public static final int NODE_COUNT = 2;

  public L1BMOnHeapSanityTest(TestConfig testConfig) {
    super(testConfig, L1BMOnHeapBasicSanityTestApp.class, L1BMOnHeapBasicSanityTestApp.class);
      testConfig.getL2Config().setMaxHeap(1024);
  }

}
