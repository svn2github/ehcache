package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.junit.Assert;
import org.terracotta.api.ClusteringToolkit;
import org.terracotta.cluster.ClusterInfo;
import org.terracotta.coordination.Barrier;

import java.util.concurrent.TimeUnit;

public class ShutdownClient2 extends ClientBase {

  private Barrier barrier;

  public ShutdownClient2(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new ShutdownClient2(args).doTest();
  }

  public void doTest() {
    try {
      barrier = getClusteringToolkit().getBarrier("shutdownBarrier", 2);
      barrier.await(TimeUnit.SECONDS.toMillis(3 * 60));
      System.out.println("Current connected clients: " + getConnectedClients());

      test(setupCache(), getClusteringToolkit());

      barrier.await(TimeUnit.SECONDS.toMillis(3 * 60));
      System.out.println("Waiting for client1 to shutdown...");
      Thread.sleep(TimeUnit.SECONDS.toMillis(30));

      Assert.assertEquals(1, getConnectedClients());

      pass();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    Element element = cache.get("key");

    if (element == null) { throw new AssertionError(); }

    Object value = element.getObjectValue();
    if (!"value".equals(value)) { throw new AssertionError("unexpected value: " + value); }
  }

  private int getConnectedClients() {
    ClusteringToolkit clustering = getTerracottaClient().getToolkit();
    ClusterInfo clusterInfo = clustering.getClusterInfo();
    return clusterInfo.getClusterTopology().getNodes().size();
  }
}
