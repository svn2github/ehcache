package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.TerracottaConfiguration.StorageStrategy;

import org.terracotta.api.ClusteringToolkit;

import junit.framework.Assert;

public class Client5 extends ServerMapClientBase {

  public Client5(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new Client5(args).run();
  }

  @Override
  protected void test(final Cache cache, final ClusteringToolkit clusteringToolkit) throws Throwable {
    BasicServerMapExpressTestHelper.populateCache(cache);
    cache.put(new Element("client1-exited", "true"));

    Cache storageCache = cache.getCacheManager().getCache("defaultStorageStrategyCache");
    Assert.assertEquals(StorageStrategy.DCV2, storageCache.getCacheConfiguration().getTerracottaConfiguration()
        .getStorageStrategy());

    System.out.println("Asserted different/explicit storage strategys");
  }
}
