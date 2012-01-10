/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class SuspendResumeBTMXATest extends AbstractStandaloneCacheTest {

  public SuspendResumeBTMXATest() {
    super("basic-xa-test.xml", SuspendResumeBTMClient.class);
    setParallelClients(false);
  }

  @Override
  protected Class getApplicationClass() {
    return SuspendResumeBTMXATest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.BTMApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

  }
}
