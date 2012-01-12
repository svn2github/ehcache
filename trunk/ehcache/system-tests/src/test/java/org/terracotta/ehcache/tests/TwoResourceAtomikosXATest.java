/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class TwoResourceAtomikosXATest extends AbstractStandaloneCacheTest {

  public TwoResourceAtomikosXATest() {
    super("two-resource-xa-test.xml", TwoResourceTx1.class, TwoResourceTx2.class);
    setParallelClients(true);
  }

  @Override
  protected Class getApplicationClass() {
    return TwoResourceAtomikosXATest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.AtomikosApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

  }
}
