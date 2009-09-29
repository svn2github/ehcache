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
public class MissingJarStandaloneCacheTest extends AbstractStandaloneCacheTest {

  public MissingJarStandaloneCacheTest() {
    super("basic-cache-test.xml");
  }

  @Override
  protected Class getApplicationClass() {
    return MissingJarStandaloneCacheTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runClient(Class client) throws Throwable {
      runClient(client, false);
    }

    @Override
    protected void evaluateClientOutput(String clientName, int exitCode, String clientOutput) throws AssertionError {
      if ((exitCode == 0) || !(clientOutput.contains("CacheException") && clientOutput.contains("missing jar"))) {
        throw new AssertionError("Exit Code " + exitCode + "\n" + clientOutput);
      }
    }
  }
}
