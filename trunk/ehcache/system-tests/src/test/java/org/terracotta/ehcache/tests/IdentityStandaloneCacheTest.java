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

/**
 * @author cdennis
 */
public class IdentityStandaloneCacheTest extends AbstractStandaloneCacheTest {

  public IdentityStandaloneCacheTest() {
    super("identity-cache-test.xml");
  }

  @Override
  protected Class getApplicationClass() {
    return IdentityStandaloneCacheTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
      if ((exitCode == 0)) { throw new AssertionError("Client " + clientName + " exited with exit code: " + exitCode); }

      FileReader fr = null;
      boolean cacheException = false;
      boolean identityValMode = false;
      try {
        fr = new FileReader(output);
        BufferedReader reader = new BufferedReader(fr);
        String st = "";
        while ((st = reader.readLine()) != null) {
          if (st.contains("CacheException")) cacheException = true;
          if (st.contains("identity value mode")) identityValMode = true;
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

      if (!cacheException) { throw new AssertionError("Expected Exception"); }

      if (!identityValMode) { throw new AssertionError("Expected identity value mode"); }
    }
  }
}
