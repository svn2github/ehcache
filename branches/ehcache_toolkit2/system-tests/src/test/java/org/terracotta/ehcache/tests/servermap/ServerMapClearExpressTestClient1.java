package org.terracotta.ehcache.tests.servermap;

import net.sf.ehcache.Cache;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.ehcache.tests.mbean.DSOMBeanController;

public class ServerMapClearExpressTestClient1 extends ServerMapClientBase {

  private final DSOMBeanController dsoMBean;

  public ServerMapClearExpressTestClient1(String[] args) {
    super("test", args);
    dsoMBean = new DSOMBeanController("localhost", getTestControlMbean().getGroupsData()[0].getJmxPort(0));
  }

  public static void main(String[] args) {
    new ServerMapClearExpressTestClient1(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
    ServerMapClearTestHelper.doTest(cache, clusteringToolkit, dsoMBean);
  }

}
