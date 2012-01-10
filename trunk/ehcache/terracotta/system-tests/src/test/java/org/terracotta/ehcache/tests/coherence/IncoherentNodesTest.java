/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ehcache.tests.coherence;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.ehcache.tests.AbstractStandaloneCacheTest;
import org.terracotta.ehcache.tests.ClientBase;

import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

import junit.framework.Assert;

public class IncoherentNodesTest extends AbstractStandaloneCacheTest {

  public static final int CLIENT_COUNT = 3;

  public IncoherentNodesTest() {
    super("cache-coherence-test.xml", IncoherentNodesTestClientOne.class, IncoherentNodesTestClientTwo.class);
    setParallelClients(false);
  }

  @Override
  protected Class getApplicationClass() {
    return App.class;
  }

  public static class App extends AbstractStandaloneCacheTest.App {

    public App(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
      super.addClientJvmarg("-Dcom.tc.ehcache.incoherent.logging=true");
    }

  }

  public static class IncoherentNodesTestClientOne extends ClientBase {

    public IncoherentNodesTestClientOne(String[] args) {
      super("test", args);
    }

    public static void main(String[] args) {
      new IncoherentNodesTestClientOne(args).run();
    }

    @Override
    protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
      doTest(cache, toolkit, true);
    }

    @Override
    public void pass() {
      super.pass();
    }

    public void doTest(Cache cache, ClusteringToolkit toolkit, final boolean killInBetween) {
      log("Running test. killInBetween: " + killInBetween);
      cache.setNodeCoherent(false);
      log(" node set to incoherent");

      if (killInBetween) {
        Thread th = new Thread(new Runnable() {

          public void run() {
            log("started the killer thread....");
            try {
              Thread.sleep(5000);
            } catch (InterruptedException e) {
              throw new AssertionError(e);
            }
            log("Exiting vm");
            IncoherentNodesTestClientOne.this.pass();
            Runtime.getRuntime().halt(0);
          }
        });
        th.start();
      }

      try {
        // load data
        int i = 0;
        final int numCycles = 10;
        while (true) {
          log("loading fake data now....");
          cache.put(new Element("key-" + i, "value-" + i));
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            /**/
          }
          if (i++ == numCycles) {
            log(" committing now....");
            break;
          }
        }
      } catch (Exception e) {
        throw new AssertionError(e);
      }

      if (killInBetween) { throw new AssertionError("this node should had been killed by now...."); }

      cache.setNodeCoherent(true);
      cache.waitUntilClusterCoherent();
      Assert.assertTrue(cache.isClusterCoherent());
      log("done....");
    }

    private static void log(String string) {
      System.out.println("__XXX__ " + string);
    }

  }

  public static class IncoherentNodesTestClientTwo extends IncoherentNodesTestClientOne {

    public IncoherentNodesTestClientTwo(String[] args) {
      super(args);
    }

    public static void main(String[] args) {
      new IncoherentNodesTestClientTwo(args).run();
    }

    @Override
    protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
      doTest(cache, toolkit, false);
    }

  }
}
