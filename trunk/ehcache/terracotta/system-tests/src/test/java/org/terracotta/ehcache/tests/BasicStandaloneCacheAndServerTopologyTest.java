/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All rights
 * reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class BasicStandaloneCacheAndServerTopologyTest extends AbstractStandaloneCacheTest {

  int dsoPort;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    dsoPort = getDsoPort();
  }

  public BasicStandaloneCacheAndServerTopologyTest() {
    super("basic-cache-test-different-server-topology.xml", "${my.tc.server.topology}", Client3.class, Client4.class);
  }

  @Override
  protected Class getApplicationClass() {
    return BasicStandaloneCacheAndServerTopologyTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      addClientJvmarg("-Dmy.tc.server.topology=127.0.0.1:" + getPort());
      addClientJvmarg("-Dtc.server.topology=127.0.0.1:" + getPort());
      super.runTest();
    }
  }

}
