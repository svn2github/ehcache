/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

public class NonStopCacheDisposalTest extends AbstractCacheTestBase {

  public NonStopCacheDisposalTest(TestConfig testConfig) {
    super("/non-stop-cache-disposal-test.xml", testConfig, NonStopCacheTestClient.class);
    testConfig.getClientConfig().getBytemanConfig().setScript("/byteman/nonStopCacheDisposal.btm");
  }

  public static class NonStopCacheTestClient extends ClientBase {

    public NonStopCacheTestClient(String[] args) {
      super("test", args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
      // first make sure NonStop does interrupt cache operations
      try {
        cache.put(new Element(1, 1));
        fail("expected NonStopCacheException");
      } catch (NonStopCacheException nse) {
        // expected
      }

      // then make sure NonStop does not interrupt cache manager shutdown
      cache.getCacheManager().shutdown();
    }

  }
}
