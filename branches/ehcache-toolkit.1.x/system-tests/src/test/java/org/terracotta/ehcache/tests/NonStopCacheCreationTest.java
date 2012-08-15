/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

import com.tc.test.config.model.TestConfig;

public class NonStopCacheCreationTest extends AbstractCacheTestBase {

  private static final String XML = "/ehcache-config.xml";

  public NonStopCacheCreationTest(TestConfig testConfig) {
    super("/non-stop-cache-creation-test.xml", testConfig, NonStopCacheTestClient.class);
  }

  public static class NonStopCacheTestClient extends ClientBase {

    public NonStopCacheTestClient(String[] args) {
      super("test", args);
    }

    @Override
    protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
      cache.getCacheManager().shutdown();
      
      CacheManager cm = new CacheManager(ClientBase.class.getResourceAsStream(XML));
      testCacheManager(cm);
      cm.shutdown();

      cm = CacheManager.create(ClientBase.class.getResourceAsStream(XML));
      testCacheManager(cm);
      cm.shutdown();
    }

    private void testCacheManager(CacheManager cm) {
      Cache c = cm.getCache("test");
      c.put(new Element("foo", "bar"));
      assertEquals("bar", c.get("foo").getObjectValue());
      c.remove("foo");
      assertNull(c.get("foo"));
    }

  }
}
