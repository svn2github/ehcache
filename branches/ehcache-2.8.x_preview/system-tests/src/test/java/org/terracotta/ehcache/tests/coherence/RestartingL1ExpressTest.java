/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests.coherence;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;

import com.tc.test.config.model.TestConfig;
import com.tc.util.runtime.Os;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RestartingL1ExpressTest extends AbstractCacheTestBase {

  public static final int    CLIENT_COUNT  = 3;
  public static final String SHOULD_CRASH  = "shouldCrash";
  public static final String AFTER_RESTART = "afterRestart";

  public RestartingL1ExpressTest(TestConfig testConfig) {
    super("basic-cache-test.xml", testConfig, RestartingL1ExpressClient.class);

    if (Os.isWindows()) {
      disableTest();
    }
  }

  @Override
  protected void startClients() throws Throwable {
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
        runners[i].addClientArg(SHOULD_CRASH);
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
    runner0.addClientArg(SHOULD_CRASH);
    runner0.addClientArg(AFTER_RESTART);
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

  private class Runner extends Thread {

    private final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
    private final Class                      clientClass;
    private final String                     clientName;
    private final List<String>               extraClientArgs;

    public Runner(Class clientClass, String clientName) {
      this.clientClass = clientClass;
      this.clientName = clientName;
      this.extraClientArgs = new ArrayList<String>();
    }

    @Override
    public void run() {
      try {
        runClient(clientClass, clientName, extraClientArgs);
      } catch (Throwable t) {
        error.set(t);
      }
    }

    public void finish() throws Throwable {
      join();
      Throwable t = error.get();
      if (t != null) throw t;
    }

    public void addClientArg(String arg) {
      extraClientArgs.add(arg);
    }
  }
}
