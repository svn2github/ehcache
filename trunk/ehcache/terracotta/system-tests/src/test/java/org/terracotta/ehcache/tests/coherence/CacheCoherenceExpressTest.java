/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests.coherence;

import org.terracotta.ehcache.tests.AbstractStandaloneCacheTest;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class CacheCoherenceExpressTest extends AbstractStandaloneCacheTest {

  public static final int CLIENT_COUNT = 3;

  public CacheCoherenceExpressTest() {
    super("cache-coherence-test.xml", CacheCoherenceExpressClient.class);
    setParallelClients(true);
  }

  @Override
  protected Class getApplicationClass() {
    return CacheCoherenceExpressTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      Runner[] runners = new Runner[CLIENT_COUNT];
      for (int i = 0; i < runners.length; i++) {
        runners[i] = new Runner(CacheCoherenceExpressClient.class, "cache-coherence-client-" + i);
        runners[i].start();
      }

      for (Runner runner : runners) {
        runner.finish();
      }
    }

    @Override
    protected void evaluateClientOutput(String clientName, int exitCode, File clientOutput) throws Throwable {
      super.evaluateClientOutput(clientName, exitCode, clientOutput);

      FileReader fr = null;
      try {
        fr = new FileReader(clientOutput);
        BufferedReader reader = new BufferedReader(fr);
        String st = "";
        while ((st = reader.readLine()) != null) {
          if (st.contains(CacheCoherenceExpressClient.PASS_OUTPUT)) return;
        }
        throw new AssertionError("Client exited without pass output string: " + CacheCoherenceExpressClient.PASS_OUTPUT);
      } catch (Exception e) {
        throw new AssertionError(e);
      } finally {
        try {
          fr.close();
        } catch (Exception e) {
          //
        }
      }
    }
  }
}
