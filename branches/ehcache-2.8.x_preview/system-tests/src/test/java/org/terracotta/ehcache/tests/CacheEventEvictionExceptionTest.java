/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListenerAdapter;

import org.terracotta.toolkit.Toolkit;
import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;

/*
 * Test to verify that a CacheEventListener that misbehaves by throwing a {@link RuntimeException} on notification of an
 * evicted element does not bring down the L1. (See DEV-7620).
 */
public class CacheEventEvictionExceptionTest extends AbstractCacheTestBase {

  public CacheEventEvictionExceptionTest(TestConfig testConfig) {
    super("/cache-event-eviction-exception-test.xml", testConfig, CacheEventEvictionExceptionTestClient.class);
    testConfig.addTcProperty(TCPropertiesConsts.L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL, "1000");
  }

  public static class CacheEventEvictionExceptionTestClient extends ClientBase {

    public static void main(String[] args) {
      new CacheEventEvictionExceptionTestClient(args).run();
    }

    public CacheEventEvictionExceptionTestClient(String[] args) {
      super("test", args);
    }


    @Override
    protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
      cache.getCacheEventNotificationService().registerListener(new ThrowingListener());
      for (int i = 0; i < 5000; i++) {
        cache.put(new Element(i, i));
        if (i % 1000 == 0) {
          // Force flush the RemoteObjectManager by faulting some stuff in.
          for (int j = 0; j < 200 && j < i; j++) {
            cache.get(j);
          }
        }
      }

      for (int i = 0; i < 10000; i++) {
        Element e = cache.get(i);
        if (e != null) {
          assertEquals(Integer.valueOf(i), e.getObjectValue());
        }
      }

    }

    private static class ThrowingListener extends CacheEventListenerAdapter {
      @Override
      public void notifyElementEvicted(Ehcache cache, Element element) {
        throw new RuntimeException("Expected exception! Nothing to worry about.");
      }
    }

  }
}
