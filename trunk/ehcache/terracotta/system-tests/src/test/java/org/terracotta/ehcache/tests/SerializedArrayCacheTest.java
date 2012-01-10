/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class SerializedArrayCacheTest extends AbstractStandaloneCacheTest {

  public SerializedArrayCacheTest() {
    super("basic-cache-test.xml");
  }
  
  @Override
  protected Class getApplicationClass() {
    return SerializedArrayCacheTest.App.class;
  }
  
  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected final void runTest() throws Throwable {
      runClient(ClientArrayValues1.class);
      runClient(ClientArrayValues2.class);
    }
  }
}
