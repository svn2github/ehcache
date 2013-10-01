/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.coherence;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

public class CacheCoherenceTest extends AbstractCacheTestBase {

  public static final int CLIENT_COUNT = 3;

  public CacheCoherenceTest(TestConfig testConfig) {
    super("cache-coherence-test.xml", testConfig, CacheCoherenceTestL1Client.class, CacheCoherenceTestL1Client.class,
          CacheCoherenceTestL1Client.class);
  }

}
