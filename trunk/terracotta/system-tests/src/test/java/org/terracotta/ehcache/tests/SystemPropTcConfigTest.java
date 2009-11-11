/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class SystemPropTcConfigTest extends AbstractStandaloneCacheTest {

  public SystemPropTcConfigTest() {
    super("basic-cache-test.xml");
  }
  
  @Override
  protected Class getApplicationClass() {
    return SystemPropTcConfigTest.App.class;
  }
  
  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      System.out.println("adding extra -Dtc.config");
      addClientJvmarg("-Dtc.config=tc-config.xml");
      runClient(Client1.class);
    }
  }
}
