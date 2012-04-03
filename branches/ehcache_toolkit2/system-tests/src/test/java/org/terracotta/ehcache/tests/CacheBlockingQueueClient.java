/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.terracotta.toolkit.Toolkit;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

public class CacheBlockingQueueClient extends ClientBase {
  private static final int ITERATIONS = 10;

  public CacheBlockingQueueClient(String[] args) {
    super("test", args);
  }

  public static void main(String[] args) {
    new CacheBlockingQueueClient(args).run();
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {

    BlockingQueue q = toolkit.getBlockingQueue("CacheBlockingQueueClient-queue");

    q.offer("entry #1");
    q.offer("entry #2");
    Assert.assertEquals(2, q.size());

    cache.put(new Element("CacheBlockingQueueClient-queue", q));

    final BlockingQueue queue = (BlockingQueue) cache.get("CacheBlockingQueueClient-queue").getValue();

    Assert.assertEquals(2, queue.size());
    Assert.assertEquals("entry #1", queue.take());
    Assert.assertEquals("entry #2", queue.take());
    Assert.assertEquals(0, queue.size());

    Runnable producer = new Runnable() {
      public void run() {
        for (int i = 0; i < ITERATIONS; i++) {
          queue.offer("a string");
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };

    final AtomicInteger counter = new AtomicInteger();

    Runnable consumer = new Runnable() {
      public void run() {
        while (true) {
          try {
            Object o = queue.poll(1, TimeUnit.SECONDS);
            if (o == null) break;
            counter.incrementAndGet();
            Assert.assertEquals("a string", o);
          } catch (InterruptedException e) {
            // ignore
          }
        }
      }
    };

    Thread t1 = new Thread(producer);
    Thread t2 = new Thread(consumer);

    t1.start();
    t2.start();

    t1.join();
    t2.join();

    Assert.assertEquals(ITERATIONS, counter.get());
  }

}