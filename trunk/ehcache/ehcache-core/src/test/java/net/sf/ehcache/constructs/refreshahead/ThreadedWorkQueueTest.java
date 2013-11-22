package net.sf.ehcache.constructs.refreshahead;

import net.sf.ehcache.constructs.refreshahead.ThreadedWorkQueue.BatchWorker;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadedWorkQueueTest {

  ThreadFactory daemonFactory = new ThreadFactory() {

    @Override
    public Thread newThread(Runnable arg0) {
      Thread t = new Thread(arg0, "test");
      t.setDaemon(true);
      return t;
    }
  };

  @Test
  public void testInitShutdown() {
    ThreadedWorkQueue<Integer> queue = new ThreadedWorkQueue<Integer>(null, 4, daemonFactory, 1000, 10);
    Assert.assertTrue(queue.isAlive());
    queue.shutdown();
    Assert.assertFalse(queue.isAlive());
  }

  @Test
  public void TestSimpleProcessing() {
    final AtomicInteger counter = new AtomicInteger(0);
    final BatchWorker<Integer> nothingWorker = new BatchWorker<Integer>() {
      @Override
      public void process(Collection<? extends Integer> collection) {
        counter.addAndGet(collection.size());
      }
    };
    ThreadedWorkQueue<Integer> queue = new ThreadedWorkQueue<Integer>(nothingWorker, 4, daemonFactory, 1000, 10);

    for (int i = 0; i < 100; i++) {
      queue.offer(new Integer(i));
    }
    for (; queue.getBacklogCount() > 0; ) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
    }
    Assert.assertTrue(counter.get() == 100);
    queue.shutdown();
  }

  @Test
  public void TestOverflowProcessing() {
    final AtomicInteger counter = new AtomicInteger(0);
    // use pause to force a delay, sos ome things fall off.
    final AtomicBoolean pause = new AtomicBoolean(true);

    // create a worker
    final BatchWorker<Integer> counterWorker = new BatchWorker<Integer>() {
      @Override
      public void process(Collection<? extends Integer> collection) {
        counter.addAndGet(collection.size());
        while(pause.get()) {
          Thread.yield();
        }
      }
    };

    // create the queue, add stuff.
    ThreadedWorkQueue<Integer> queue = new ThreadedWorkQueue<Integer>(counterWorker, 1, daemonFactory, 1000, 1);
    final int TESTSIZE = 10000;
    for (int i = 0; i < TESTSIZE; i++) {
      if(i==100) {
        // let the queue process now. Things have fallen off.
        pause.set(false);
      }
      queue.offer(new Integer(i));
    }

    // wait for done.
    for (; queue.getBacklogCount() > 0; ) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
    }

    // should have been overflow. lossage.
    Assert.assertTrue(counter.get() < TESTSIZE);
    Assert.assertTrue(queue.getDroppedCount()>0);
    queue.shutdown();
  }
}
