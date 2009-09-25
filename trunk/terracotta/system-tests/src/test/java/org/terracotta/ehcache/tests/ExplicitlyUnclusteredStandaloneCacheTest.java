/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

/**
 *
 * @author cdennis
 */
public class ExplicitlyUnclusteredStandaloneCacheTest extends AbstractStandaloneCacheTest {

  public ExplicitlyUnclusteredStandaloneCacheTest() {
    super("explicitly-unclustered-cache-test.xml");
  }

  @Override
  protected Class getApplicationClass() {
    return ExplicitlyUnclusteredStandaloneCacheTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected final void runTest() throws Throwable {
      runClient(UnclusteredClient.class);
      runClient(UnclusteredClient.class);
    }
  }
}
