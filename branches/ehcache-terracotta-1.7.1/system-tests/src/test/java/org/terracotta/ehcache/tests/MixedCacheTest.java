/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class MixedCacheTest extends AbstractStandaloneCacheTest {

  public MixedCacheTest() {
    super("mixed-cache-test.xml");
  }
  
  @Override
  protected Class getApplicationClass() {
    return MixedCacheTest.App.class;
  }
  
  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected final void runTest() throws Throwable {
      runClient(Client1.class);
      runClient(Client2.class);
      runClient(UnclusteredClient.class);
      runClient(UnclusteredClient.class);
    }
  }
}
