/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class UrlConfigStandaloneCacheTest extends AbstractStandaloneCacheTest {

  public UrlConfigStandaloneCacheTest() {
    super("url-config-cache-test.xml");
  }

  @Override
  protected Class getApplicationClass() {
    return UrlConfigStandaloneCacheTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runClient(Class client) throws Throwable {
      writeXmlFileWithPort("url-config-cache-test-tc-config.xml", "tc-config.xml");
      super.runClient(client);
    }
  }
}
