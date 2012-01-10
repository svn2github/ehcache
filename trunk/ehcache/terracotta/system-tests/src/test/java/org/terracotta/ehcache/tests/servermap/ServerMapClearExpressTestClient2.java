package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.mbean.DSOMBean;
import org.terracotta.ehcache.tests.mbean.DSOMBeanController;

public class ServerMapClearExpressTestClient2 extends ServerMapClientBase {

  private final DSOMBean dsoMBean;

  public ServerMapClearExpressTestClient2(String[] args) {
    super("test", args);
    dsoMBean = new DSOMBeanController("localhost", getJmxPort());
  }

  public static void main(String[] args) {
    new ServerMapClearExpressTestClient2(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
    ServerMapClearTestHelper.doTest(cache, clusteringToolkit, dsoMBean);
  }
}
