/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class OtherClassloaderCacheTest extends AbstractStandaloneCacheTest {

  public OtherClassloaderCacheTest() {
    super("small-memory-cache-test.xml");
  }
  
  @Override
  protected Class getApplicationClass() {
    return OtherClassloaderCacheTest.App.class;
  }
  
  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected final void runTest() throws Throwable {
      runClient(OtherClassloaderClient.class);
      runClient(ReaderClient.class);
    }
  }
}
