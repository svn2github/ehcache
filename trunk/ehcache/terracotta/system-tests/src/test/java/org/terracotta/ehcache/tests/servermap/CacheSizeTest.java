/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractExpressCacheTest;
import org.terracotta.ehcache.tests.ClientBase;

import junit.framework.Assert;

public class CacheSizeTest extends AbstractExpressCacheTest {
  public CacheSizeTest() {
    super("/servermap/basic-servermap-test.xml", CacheSizeTestClient.class);
  }

  public static class CacheSizeTestClient extends ClientBase {

    private CacheSizeTestClient(String[] args) {
      super("test", args);
    }

    public static void main(String[] args) {
      new CacheSizeTestClient(args).run();
    }

    @Override
    protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
      final int numElems = 5000;
      Assert.assertEquals(Consistency.STRONG, cache.getCacheConfiguration().getTerracottaConfiguration()
          .getConsistency());
      for (int i = 0; i < numElems; i++) {
        cache.put(new Element("key-" + i, "value-" + i));
      }
      System.out
          .println("Populated cache - strong. size: " + cache.getSize() + " keys size: " + cache.getKeys().size());

      Assert.assertEquals(numElems, cache.getSize());
      Assert.assertEquals("STRONG consistency cache: cache.getSize() should be equal to cache.getKeys().size()", cache
          .getSize(), cache.getKeys().size());

      cache = cache.getCacheManager().getCache("eventualConsistencyCache");
      Assert.assertEquals(Consistency.EVENTUAL, cache.getCacheConfiguration().getTerracottaConfiguration()
          .getConsistency());
      for (int i = 0; i < numElems; i++) {
        cache.put(new Element("key-" + i, "value-" + i));
      }
      System.out.println("Populated cache - eventual. size: " + cache.getSize() + " keys size: "
                         + cache.getKeys().size());

      Assert.assertEquals(numElems, cache.getSize());
      Assert.assertEquals("EVENTUAL consistency cache: cache.getSize() should be equal to cache.getKeys().size()",
                          cache.getSize(), cache.getKeys().size());
    }

  }

}
