/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class BasicBTMXATest extends AbstractStandaloneCacheTest {

  public BasicBTMXATest() {
    super("basic-xa-test.xml", BTMSimpleTx1.class, BTMSimpleTx2.class);
    setParallelClients(true);
  }

  @Override
  protected Class getApplicationClass() {
    return BasicBTMXATest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.BTMApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

  }
}
