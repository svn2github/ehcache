/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class TwoResourceBTMXATest extends AbstractStandaloneCacheTest {

  public TwoResourceBTMXATest() {
    super("two-resource-xa-test.xml", BTMTwoResourceTx1.class, BTMTwoResourceTx2.class);
    setParallelClients(true);

    // DEV-3930
    // disableAllUntil("2010-03-31");
  }

  @Override
  protected Class getApplicationClass() {
    return TwoResourceBTMXATest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.BTMApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

  }
}
