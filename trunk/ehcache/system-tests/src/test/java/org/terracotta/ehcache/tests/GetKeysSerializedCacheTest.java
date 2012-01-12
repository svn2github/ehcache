/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

/**
 * @author Chris Dennis
 */
public class GetKeysSerializedCacheTest extends AbstractStandaloneCacheTest {

  public GetKeysSerializedCacheTest() {
    super("basic-cache-test.xml");
  }

  @Override
  protected Class getApplicationClass() {
    return GetKeysSerializedCacheTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected final void runTest() throws Throwable {
      runClient(GetKeysClient.class);
      runClient(GetKeysClient.class);
    }
  }

}
