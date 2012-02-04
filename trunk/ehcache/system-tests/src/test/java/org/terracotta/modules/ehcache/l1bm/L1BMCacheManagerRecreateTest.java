/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.l1bm;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.test.config.model.TestConfig;

public class L1BMCacheManagerRecreateTest extends AbstractCacheTestBase {

  public L1BMCacheManagerRecreateTest(TestConfig testConfig) {
    super(testConfig, App.class);
  }

  public static class App extends ClientBase {
    private CacheManager cm;
    private Ehcache      cache;

    public App(String[] args) {
      super(args);
    }

    @Override
    protected void runTest(Cache testcache, ClusteringToolkit clusteringToolkit) throws Throwable {
      cacheManager.shutdown();

      setup();
      for (int i = 0; i < 100; i++) {
        cache.put(new Element("key" + i, "value" + i));
      }
      cleanup();

      setup();
      for (int i = 0; i < 100; i++) {
        Element e = cache.get("key" + i);
        assertNotNull(e);
        assertEquals("value" + i, e.getObjectValue());
      }
      cleanup();
    }

    private void setup() {
      cm = getCacheManager();
      cache = cm.getEhcache("test");
    }

    private void cleanup() {
      cm.shutdown();
    }
  }
}
