/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests.coherence;

import org.terracotta.ehcache.tests.AbstractStandaloneCacheTest;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.runtime.Os;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class RestartingL1ExpressTest extends AbstractStandaloneCacheTest {

  public static final int CLIENT_COUNT = 3;

  public RestartingL1ExpressTest() {
    super("basic-cache-test.xml", RestartingL1ExpressClient.class);

    if (Os.isWindows()) {
      disableTest();
    }
  }

  @Override
  protected Class getApplicationClass() {
    return RestartingL1ExpressTest.App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {
    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    @Override
    protected void runTest() throws Throwable {
      // n nodes start in coherent, then setCoherent(false)
      // assert coherent=false in all n nodes
      // n-1 nodes call setCoherent(true)
      // 1 node exits without calling setCoherent(true)
      // n-1 nodes assert coherent
      // 1 node restarts, asserts cache coherent
      // java RestartingL1ExpressClient -> (no-args), n-1 nodes, does not crash
      // java RestartingL1ExpressClient shouldCrash -> (one arg), 1 node, exits this node without calling
      // setCoherent(true)
      // java RestartingL1ExpressClient shouldCrash afterRestart -> (two args), 1 node, restarts and asserts cache
      // coherent
      Runner[] runners = new Runner[CLIENT_COUNT];
      for (int i = 0; i < runners.length; i++) {
        if (i == 0) {
          // start the restarting client
          runners[i] = new Runner(RestartingL1ExpressClient.class, "restart-crasher-client");
          runners[i].addClientArg("shouldCrash");
        } else {
          runners[i] = new Runner(RestartingL1ExpressClient.class, "restart-normal-client-" + (i - 1));
        }
      }
      for (Runner runner : runners) {
        runner.start();
      }
      System.out.println("Waiting for first client to finish");
      // wait for first client to exit
      runners[0].finish();

      // restart this client in restart
      Runner runner0 = runners[0] = new Runner(RestartingL1ExpressClient.class, "restart-afterRestart-client");
      runner0.addClientArg("shouldCrash");
      runner0.addClientArg("afterRestart");
      runner0.start();

      for (Runner runner : runners) {
        runner.finish();
      }
    }

    @Override
    protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
      super.evaluateClientOutput(clientName, exitCode, output);

      FileReader fr = null;
      try {
        fr = new FileReader(output);
        BufferedReader reader = new BufferedReader(fr);
        String st = "";
        while ((st = reader.readLine()) != null) {
          if (st.contains(RestartingL1ExpressClient.PASS_OUTPUT)) return;
        }
        throw new AssertionError("Client exited without pass output string: " + RestartingL1ExpressClient.PASS_OUTPUT);
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
