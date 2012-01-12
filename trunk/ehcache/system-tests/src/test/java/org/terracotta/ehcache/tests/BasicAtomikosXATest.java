/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class BasicAtomikosXATest extends AbstractStandaloneCacheTest {

  public BasicAtomikosXATest() {
    super("basic-xa-test.xml", SimpleTx1.class, SimpleTx2.class);
    setParallelClients(true);
  }

  @Override
  protected Class getApplicationClass() {
    return BasicAtomikosXATest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.AtomikosApp {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

  }
}
