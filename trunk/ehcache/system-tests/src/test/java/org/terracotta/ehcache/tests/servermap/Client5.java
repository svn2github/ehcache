package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class Client5 extends ServerMapClientBase {

  public Client5(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new Client5(args).run();
  }

  @Override
  protected void runTest(final Cache cache, final ClusteringToolkit clusteringToolkit) throws Throwable {
    BasicServerMapExpressTestHelper.populateCache(cache);
    cache.put(new Element("client1-exited", "true"));

    cache.getCacheManager().getCache("defaultStorageStrategyCache");

    System.out.println("Asserted different/explicit storage strategys");
  }
}
