/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice. All rights
 * reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class SystemPropTcConfigTest extends AbstractStandaloneCacheTest {

  public SystemPropTcConfigTest() {
    super("basic-cache-test.xml");
  }

  @Override
  protected Class getApplicationClass() {
    return SystemPropTcConfigTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      System.out.println("adding extra -Dtc.config");
      addClientJvmarg("-Dtc.config=tc-config.xml");
      runClient(Client1.class);
    }

    @Override
    protected void evaluateClientOutput(String clientName, int exitCode, String clientOutput) throws AssertionError {
      if (exitCode == 0) throw new AssertionError("Client should fail but didn't");
      if (!clientOutput.contains("net.sf.ehcache.CacheException")) {
        //
        throw new AssertionError("Expecting client to fail with exception: net.sf.ehcache.CacheException");
      }
      String errorMessage = "The Terracotta config file should not be set through -Dtc.config in this usage. It must be embedded in ehcache configuration file.";
      if (!clientOutput.contains(errorMessage)) {
        //
        throw new AssertionError("Expecting client to fail with message: " + errorMessage);
      }
    }
  }
}
