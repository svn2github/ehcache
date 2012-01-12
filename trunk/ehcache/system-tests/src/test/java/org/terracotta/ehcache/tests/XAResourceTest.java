/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class XAResourceTest extends AbstractStandaloneCacheTest {

  public XAResourceTest() {
    super("xaresource-test.xml", BareXAResourceTx.class);
    setParallelClients(true);
  }

  @Override
  protected Class getApplicationClass() {
    return XAResourceTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

  }
}
