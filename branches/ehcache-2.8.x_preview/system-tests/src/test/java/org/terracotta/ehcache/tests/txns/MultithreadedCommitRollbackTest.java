/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.txns;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

// https://jira.terracotta.org/jira/browse/DEV-8041
public class MultithreadedCommitRollbackTest extends AbstractCacheTestBase {

  public MultithreadedCommitRollbackTest(TestConfig testConfig) {
    super("multithreaded-commit-rollback-test.xml", testConfig, MultithreadedCommitRollbackTestClient.class);
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
    super.evaluateClientOutput(clientName, exitCode, output);

    checkForAnyExceptionsInLog(clientName, exitCode, output);
  }

  private void checkForAnyExceptionsInLog(String clientName, int exitCode, File output) {
    debug(("-----------------------------------------------------------------------------------"));
    debug(("---------- P A R S I N G   L O G S   F O R   A N Y   E X C E P T I O N S ----------"));
    debug(("-----------------------------------------------------------------------------------"));
    debug(("Parsing client log file for any exceptions: " + output.getAbsolutePath()));
    FileReader fr = null;
    try {
      fr = new FileReader(output);
      BufferedReader reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        if (st.endsWith("Exception")) {
          debug(("Found probable exception for line: '" + st + "'"));
          String first = st;
          st = reader.readLine();
          if (st != null) {
            StringBuilder sb = new StringBuilder(st.trim());
            String trimmedLine = new StringBuilder(sb.reverse().toString().trim()).reverse().toString();
            if (trimmedLine.startsWith("at ")) {
              debug(("Concluding its an exception as got second line starting with 'at ': " + trimmedLine));
              if (first.equals("java.lang.NullPointerException")
                  && trimmedLine.startsWith("at net.sf.ehcache.management.ResourceClassLoader")) {
                // TODO: ignore weird error in mgmt for now - remove this
                debug(("Ignoring second line of NPE in management: " + first + "\n" + trimmedLine));
              } else {
                throw new AssertionError("Found exception in log file: " + first + "\n" + st);
              }
            }
          }
        }
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
  }

  private void debug(String string) {
    System.out.println("      ------>> " + string);
  }

  public static class MultithreadedCommitRollbackTestClient extends ClientBase {

    public MultithreadedCommitRollbackTestClient(String[] args) {
      super("testCache", args);
    }

    @Override
    protected void runTest(Cache cache, Toolkit myToolkit) throws Throwable {
      TransactionController transactionController = cache.getCacheManager().getTransactionController();
      final int numThreads = 10;
      final int count = 10;

      AtomicReference<Throwable> error = new AtomicReference<Throwable>();
      List<Thread> threads = new ArrayList<Thread>();
      for (int i = 0; i < numThreads; i++) {
        Thread t = new Thread(new Producer(i, cache, transactionController, count, false, error),
                              "RollbackAtEnd-Thread-" + i);
        threads.add(t);
        t.start();
      }

      for (Thread t : threads) {
        t.join();
      }
      Assert.assertNull("None of the threads should have errors - "
                        + (error.get() == null ? "NULL" : error.get().getMessage()), error.get());
      Assert.assertEquals(0, cache.getSize());

      threads.clear();

      for (int i = 0; i < numThreads; i++) {
        Thread t = new Thread(new Producer(i, cache, transactionController, count, true, error), "CommitAtEnd-Thread-"
                                                                                                 + i);
        threads.add(t);
        t.start();
      }

      for (Thread t : threads) {
        t.join();
      }
      Assert.assertNull("None of the threads should have errors - "
                        + (error.get() == null ? "NULL" : error.get().getMessage()), error.get());
      Assert.assertEquals(numThreads * count, cache.getSize());

    }

    private static class Producer implements Runnable {

      private final TransactionController      controller;
      private final int                        id;
      private final boolean                    shouldCommit;
      private final Cache                      cache;
      private final int                        count;
      private final AtomicReference<Throwable> error;

      public Producer(int id, Cache cache, TransactionController controller, int count, boolean shouldCommit,
                      AtomicReference<Throwable> error) {
        this.cache = cache;
        this.controller = controller;
        this.id = id;
        this.count = count;
        this.shouldCommit = shouldCommit;
        this.error = error;
      }

      @Override
      public void run() {
        debug("Starting thread: shouldCommit: " + shouldCommit);
        try {
          controller.begin();
          for (int i = 0; i < count && error.get() == null; i++) {
            String kv = String.valueOf(id * count + i);
            cache.put(new Element(kv, kv));
            debug("putting: " + kv);
          }
          if (shouldCommit) {
            controller.commit();
          } else {
            controller.rollback();
          }
        } catch (Throwable t) {
          debug("XXX: Caught exception: " + t);
          error.set(t);
        }
      }

    }

  }

}
