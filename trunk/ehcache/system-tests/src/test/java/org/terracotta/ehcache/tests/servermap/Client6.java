package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;

public class Client6 extends ServerMapClientBase {

  public Client6(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new Client6(args).run();
  }

  @Override
  protected void runTest(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
    assertClient1Exited(cache);
    BasicServerMapExpressTestHelper.assertValuesInCache(cache);
  }

  private void assertClient1Exited(Cache cache) {
    Element element = cache.get("client1-exited");
    if (element == null) { throw new AssertionError("Element should not be null"); }
    if (!"true".equals(element.getObjectValue())) {
      //
      throw new AssertionError("Client1 should have already exited before this");
    }
  }
}
