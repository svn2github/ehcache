/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class TwoResourceSuspendResumeBTMXATest extends AbstractStandaloneCacheTest {

  public TwoResourceSuspendResumeBTMXATest() {
    super("two-resource-xa-test.xml", TwoResourceSuspendResumeBTMClient.class);
    setParallelClients(false);
  }

  @Override
  protected Class getApplicationClass() {
    return TwoResourceSuspendResumeBTMXATest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.BTMApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

  }
}
