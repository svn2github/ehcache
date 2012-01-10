/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class LocalTxTest extends AbstractStandaloneCacheTest {

  public LocalTxTest() {
    super("local-tx-test.xml", LocalTxClient.class);
    setParallelClients(true);
  }

  @Override
  protected Class getApplicationClass() {
    return LocalTxTest.App.class;
  }

 public static class App extends AbstractStandaloneCacheTest.App {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      //addClientJvmarg("-Xdebug");
      //addClientJvmarg("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005");
      super.runTest();
    }
  }

}
