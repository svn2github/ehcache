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
    protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
      if ((exitCode == 0)) { throw new AssertionError("Client " + clientName + " exited with exit code: " + exitCode); }

      FileReader fr = null;
      boolean cacheException = false;
      boolean missingJar = false;
      boolean invalidConfigException = false;
      boolean missingTCJars = false;
      try {
        fr = new FileReader(output);
        BufferedReader reader = new BufferedReader(fr);
        String st = "";
        while ((st = reader.readLine()) != null) {
          if (st.contains("CacheException")) cacheException = true;
          if (st.contains("missing jar")) missingJar = true;
          if (st.contains("InvalidConfigurationException")) invalidConfigException = true;
          if (st.contains("missing Terracotta jar(s)")) missingTCJars = true;
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

      if (!isPassed(cacheException, missingJar, invalidConfigException, missingTCJars)) { throw new AssertionError(); }
    }

    private boolean isPassed(boolean cacheException, boolean missingJar, boolean invalidConfigException,
                             boolean missingTCJars) {
      return (cacheException && missingJar) || (invalidConfigException && missingTCJars);
    }

  }
}
