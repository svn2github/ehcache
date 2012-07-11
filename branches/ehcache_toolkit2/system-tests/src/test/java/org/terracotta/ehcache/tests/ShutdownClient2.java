package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.junit.Assert;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterInfo;

import java.util.concurrent.TimeUnit;

public class ShutdownClient2 extends ClientBase {

  public ShutdownClient2(String[] args) {
    super("test", args);
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    try {
      getBarrierForAllClients().await(TimeUnit.SECONDS.toMillis(3 * 60), TimeUnit.MILLISECONDS);
      System.out.println("Current connected clients: " + getConnectedClients());

      testCache(cache, toolkit);

      getBarrierForAllClients().await(TimeUnit.SECONDS.toMillis(3 * 60), TimeUnit.MILLISECONDS);
      System.out.println("Waiting for client1 to shutdown...");
      Thread.sleep(TimeUnit.SECONDS.toMillis(30));

      Assert.assertEquals(1, getConnectedClients());

      pass();
      System.exit(0);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  protected void testCache(Cache cache, Toolkit toolkit) throws Throwable {
    Element element = cache.get("key");

    if (element == null) { throw new AssertionError(); }

    Object value = element.getObjectValue();
    if (!"value".equals(value)) { throw new AssertionError("unexpected value: " + value); }
  }

  private int getConnectedClients() {
    Toolkit clustering = getClusteringToolkit();
    ClusterInfo clusterInfo = clustering.getClusterInfo();
    return clusterInfo.getClusterTopology().getNodes().size();
  }
}
