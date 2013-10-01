package org.terracotta.modules.ehcache.coherence;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;

/**
 * CASEventualCacheTest
 */
public class CASEventualCacheTest extends AbstractCacheTestBase {

    public CASEventualCacheTest(TestConfig testConfig) {
        super("cache-coherence-test.xml", testConfig, CASEventualCacheTestClient.class);
    }
}
