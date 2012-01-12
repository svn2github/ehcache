package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.mbean.DSOMBeanController;

public class ServerMapClearExpressTestClient1 extends ServerMapClientBase {

  private final DSOMBeanController dsoMBean;

  public ServerMapClearExpressTestClient1(String[] args) {
    super("test", args);
    dsoMBean = new DSOMBeanController("localhost", getJmxPort());
  }

  public static void main(String[] args) {
    new ServerMapClearExpressTestClient1(args).run();
  }

  @Override
  protected void test(Cache cache, ClusteringToolkit clusteringToolkit) throws Throwable {
    ServerMapClearTestHelper.doTest(cache, clusteringToolkit, dsoMBean);
  }

}
