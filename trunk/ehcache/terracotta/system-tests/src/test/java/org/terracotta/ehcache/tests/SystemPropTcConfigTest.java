/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

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
      super.runTest();
    }

    @Override
    protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
      if ((exitCode == 0)) { throw new AssertionError("Client " + clientName + " exited with exit code: " + exitCode); }

      FileReader fr = null;
      boolean containsException = false;
      boolean containsErrorMsg = false;
      String errorMessage = "The Terracotta config file should not be set through -Dtc.config in this usage.";
      try {
        fr = new FileReader(output);
        BufferedReader reader = new BufferedReader(fr);
        String st = "";
        while ((st = reader.readLine()) != null) {
          if (st.contains("net.sf.ehcache.CacheException")) containsException = true;
          if (st.contains(errorMessage)) containsErrorMsg = true;
        }
      } catch (Exception e) {
        throw new AssertionError(e);
      } finally {
        try {
          fr.close();
        } catch (Exception e) {
          //
        }
      }

      if (!containsException) { throw new AssertionError(
                                                         "Expecting client to fail with exception: net.sf.ehcache.CacheException"); }

      if (!containsErrorMsg) { throw new AssertionError("Expecting client to fail with message: " + errorMessage); }
    }

  }
}
