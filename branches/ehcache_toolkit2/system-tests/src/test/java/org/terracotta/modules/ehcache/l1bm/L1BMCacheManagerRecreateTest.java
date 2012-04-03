/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.junit.Assert;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

public class L1BMCacheManagerRecreateTest extends AbstractCacheTestBase {

  public L1BMCacheManagerRecreateTest(TestConfig testConfig) {
    super(testConfig, App.class);
  }

  public static class App extends ClientBase {
    private Cache cache;

    public App(String[] args) {
      super(args);
    }

    @Override
    protected void runTest(Cache testcache, Toolkit clusteringToolkit) throws Throwable {
      cache = testcache;
      for (int i = 0; i < 100; i++) {
        cache.put(new Element("key" + i, "value" + i));
      }
      cleanup();

      setup();
      for (int i = 0; i < 100; i++) {
        Element e = cache.get("key" + i);
        Assert.assertNotNull(e);
        Assert.assertEquals("value" + i, e.getObjectValue());
      }
      cleanup();
    }

    private void setup() {
      setupCacheManager();
      cache = getCache();
    }

    private void cleanup() {
      getCacheManager().shutdown();
    }
  }
}
