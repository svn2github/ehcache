/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.backend;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;

import com.tc.test.config.model.TestConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

public class BulkLoadInternalKeyRepresentationExposedTest extends AbstractCacheTestBase {

  private static final int NODE_COUNT = 3;

  public BulkLoadInternalKeyRepresentationExposedTest(TestConfig testConfig) {
    super(testConfig, App.class, App.class, App.class);
  }

  public static class App extends ClientBase {

    private static final int NUM_OF_WRITERS = 1;
    private static final int NUM_OF_READERS = 1;

    private final ToolkitBarrier    barrier;
    ToolkitBarrier                  tBarrier;

    public App(String[] args) {
      super(args);
      this.barrier = getClusteringToolkit().getBarrier("barrier1", NODE_COUNT);
      this.tBarrier = getClusteringToolkit().getBarrier("barrier2", NODE_COUNT * (NUM_OF_READERS + NUM_OF_WRITERS));
    }

    public static void main(String[] args) {
      new App(args).run();
    }

    @Override
    protected void runTest(Cache cache, Toolkit clusteringToolkit) throws Throwable {
      final int index = this.barrier.await();

      cache.setNodeBulkLoadEnabled(true);

      Assert.assertEquals(0, cache.getSize());

      this.barrier.await();

      List<WriterThread> writers = new ArrayList<WriterThread>();

      System.out.println("start " + NUM_OF_WRITERS + " writer threads... ");

      for (int i = 1; i <= NUM_OF_WRITERS; i++) {
        WriterThread thread = new WriterThread(i, index, cache, tBarrier);
        writers.add(thread);
        thread.start();
      }

      System.out.println("start " + NUM_OF_READERS + " reader threads... ");

      List<ReaderThread> readers = new ArrayList<ReaderThread>();

      for (int i = 1; i <= NUM_OF_READERS; i++) {
        ReaderThread thread = new ReaderThread(i, index, cache, tBarrier);
        readers.add(thread);
        thread.start();
      }

      System.out.println("complete reading.");
      // removes
      this.barrier.await();

      for (ReaderThread thread : readers) {
        thread.join();
        for (Exception e : thread.getExceptions()) {
          Assert.fail(e.getMessage());
        }
      }

      for (WriterThread thread : writers) {
        thread.join();
        for (Exception e : thread.getExceptions()) {
          Assert.fail(e.getMessage());
        }
      }

      this.barrier.await();
      waitForAllCurrentTransactionsToComplete(cache);
      Assert.assertEquals(0, cache.getSize());
    }

  }

  public static class MyKey implements Serializable {
    private final String keyVal;

    public MyKey(final String keyVal) {
      this.keyVal = keyVal;
    }

    public synchronized String getKeyVal() {
      return keyVal;
    }
  }

  private static class WriterThread extends Thread {

    private static final int     NUM_OF_ELEMENTS = 1000;
    private final Cache          cache;
    private final int            threadNo;
    private final int            participantIndex;
    private final Set<Exception> exceptions      = new HashSet<Exception>();
    private final ToolkitBarrier        tBarrier;

    public WriterThread(int num, int participantIndex, Cache cache, ToolkitBarrier tBarrier) {
      super("WriterThread-" + num);
      this.threadNo = num;
      this.participantIndex = participantIndex;
      this.cache = cache;
      this.tBarrier = tBarrier;
    }

    @Override
    public void run() {

      try {
        System.out.println(WriterThread.class.getName() + " - " + participantIndex + ", " + threadNo + " starting...");
        int startIndex = participantIndex * NUM_OF_ELEMENTS;
        for (int i = startIndex; i < startIndex + NUM_OF_ELEMENTS; i++) {
          cache.put(new Element(new MyKey(threadNo + "key" + i), "value" + i));
        }

        System.out.println(WriterThread.class.getName() + " - " + participantIndex + ", " + threadNo + " Wrote "
                           + NUM_OF_ELEMENTS + " elements.");

        tBarrier.await(); // wait for all writers and iterators

        System.out.println(WriterThread.class.getName() + " - " + participantIndex + ", " + threadNo
                           + " Awaiting iterators to complete.");

        tBarrier.await(); // wait for all writers and iterators

        tBarrier.await(); // wait for all writers and iterators

      } catch (Exception e) {
        exceptions.add(e);
      }
    }

    public Set<Exception> getExceptions() {
      return exceptions;
    }

  }

  private static class ReaderThread extends Thread {

    private final Cache          cache;
    private final int            threadNo;
    private final int            participantIndex;
    private final Set<Exception> exceptions = new HashSet<Exception>();
    private final ToolkitBarrier        tBarrier;

    public ReaderThread(int num, int participantIndex, Cache cache, ToolkitBarrier tBarrier) {
      super("ReaderThread-" + num);
      this.threadNo = num;
      this.participantIndex = participantIndex;
      this.cache = cache;
      this.tBarrier = tBarrier;
    }

    @Override
    public void run() {

      try {

        System.out.println(ReaderThread.class.getName() + " - " + participantIndex + ", " + threadNo
                           + " waiting for writers to complete.");

        tBarrier.await(); // wait for all writers and iterators

        System.out.println(ReaderThread.class.getName() + " - " + participantIndex + ", " + threadNo
                           + " getting keys and iterating...");

        List keys = cache.getKeys();

        for (Object o : keys) {
          if (!(o instanceof MyKey)) throw new IllegalStateException("Key is not a MyKey!, it is a: "
                                                                     + o.getClass().getCanonicalName());
          MyKey skey = (MyKey) o;
          // caSystem.out.println("Iterated to key: " + skey);
          if (!skey.getKeyVal().contains("key")) throw new IllegalStateException("Key is garbled!");
        }

        System.out.println(ReaderThread.class.getName() + " - " + participantIndex + ", " + threadNo
                           + " iteration complete...");

        tBarrier.await(); // wait for all writers and iterators

        System.out.println(ReaderThread.class.getName() + " - " + participantIndex + ", " + threadNo + " done.");

        cache.removeAll();

        tBarrier.await(); // wait for all writers and iterators
      } catch (Exception e) {
        exceptions.add(e);
      }

    }

    public Set<Exception> getExceptions() {
      return exceptions;
    }

  }

}
