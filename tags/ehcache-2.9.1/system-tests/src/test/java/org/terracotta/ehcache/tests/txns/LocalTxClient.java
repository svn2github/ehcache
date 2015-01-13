package org.terracotta.ehcache.tests.txns;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.DefaultElementValueComparator;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.transaction.DeadLockException;
import net.sf.ehcache.transaction.TransactionInterruptedException;
import net.sf.ehcache.transaction.TransactionTimeoutException;

import org.terracotta.ehcache.tests.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.util.concurrent.ThreadUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

public class LocalTxClient extends ClientBase {

  private final ElementValueComparator elementValueComparator = new DefaultElementValueComparator(
                                                                                                  new CacheConfiguration()
                                                                                                      .copyOnRead(true)
                                                                                                      .copyOnWrite(false));
  private TransactionController        transactionController;
  private Cache                        cache1;

  public LocalTxClient(String[] args) {
    super("test", args);
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    cache1 = cache;
    transactionController = cache1.getCacheManager().getTransactionController();
    cache1.getCacheManager().getTransactionController().setDefaultTransactionTimeout(120);

    testInterruption();
    clearCache();
    testCopyOnRead();
    clearCache();
    testCopyOnWrite();
    clearCache();
    testTwoPuts();
    clearCache();
    testPut();
    clearCache();
    testRemove();
    clearCache();
    testRemoveAll();
    clearCache();
    testRollback();
    clearCache();
    testTwoConcurrentUpdates();
    clearCache();
    testDeadlock();
    clearCache();
    testGetKeys();
    clearCache();
    testGetSize();
    clearCache();
    testPutIfAbsent();
    clearCache();
    testRemoveElement();
    clearCache();
    testReplace();
    clearCache();
    testReplace2Args();
    clearCache();
    testDecoratedCache();
    clearCache();
  }

  private void clearCache() {
    System.out.println("clearCache()");
    transactionController.begin();
    cache1.removeAll();
    transactionController.commit();
  }

  public void testInterruption() throws Exception {
    System.out.println("testInterruption()");
    transactionController.begin();
    Thread.currentThread().interrupt();
    try {
      cache1.get(1);
      throw new AssertionError("expected TransactionInterruptedException");
    } catch (TransactionInterruptedException e) {
      // expected
    }

    // make sure interrupted status got cleared
    cache1.get(1);

    cache1.put(new Element(1, "one"));

    final CyclicBarrier localBarrier = new CyclicBarrier(2);
    TxThread tx2 = new TxThread() {
      @Override
      public void exec() throws Exception {
        transactionController.begin();

        // awake tx1
        localBarrier.await();
        try {
          cache1.put(new Element(1, "one#tx2"));
        } catch (TransactionInterruptedException e) {
          // expected
        }

        transactionController.commit();
      }
    };
    tx2.start();

    localBarrier.await();
    tx2.interrupt();

    tx2.join();
    tx2.assertNotFailed();

    transactionController.commit();
  }

  public void testCopyOnRead() throws Exception {
    System.out.println("testCopyOnRead()");
    transactionController.begin();
    Object putValue = new Object[] { "one#1" };
    cache1.put(new Element(1, putValue));

    Element one = cache1.get(1);
    Object getValue = one.getObjectValue();

    Assert.assertFalse(putValue == getValue);
    transactionController.commit();
  }

  public void testCopyOnWrite() throws Exception {
    System.out.println("testCopyOnWrite()");
    transactionController.begin();
    Object[] putValue = new Object[] { "one#1" };
    cache1.put(new Element(1, putValue));
    putValue[0] = "one#2";

    Element one = cache1.get(1);
    Object[] getValue = (Object[]) one.getObjectValue();

    Assert.assertEquals("one#1", getValue[0]);
    transactionController.commit();
  }

  public void testTwoPuts() throws Exception {
    System.out.println("testTwoPuts()");
    transactionController.begin();
    cache1.put(new Element(1, new Object[] { "one#1" }));
    transactionController.commit();

    transactionController.begin();
    cache1.put(new Element(1, new Object[] { "one#2" }));
    transactionController.commit();
  }

  public void testPut() throws Exception {
    System.out.println("testPut()");
    transactionController.begin();

    cache1.put(new Element(1, "one"));

    Assert.assertEquals(new Element(1, "one"), cache1.get(1));

    TxThread tx2 = new TxThread() {
      @Override
      public void exec() {
        transactionController.begin();

        Assert.assertNull(cache1.get(1));

        transactionController.commit();
      }
    };
    tx2.start();
    tx2.join();
    tx2.assertNotFailed();

    transactionController.commit();

    transactionController.begin();
    System.out.println("cache1.get(1): " + cache1.get(1));
    Assert.assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));
    transactionController.commit();

    TxThread tx3 = new TxThread() {
      @Override
      public void exec() {
        transactionController.begin();

        Assert.assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));

        transactionController.commit();
      }
    };
    tx3.start();
    tx3.join();
    tx3.assertNotFailed();
  }

  public void testRemove() throws Exception {
    System.out.println("testRemove()");
    transactionController.begin();
    cache1.put(new Element(1, "one"));
    transactionController.commit();

    transactionController.begin();

    Assert.assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));
    cache1.put(new Element(2, "two"));
    Assert.assertTrue(cache1.remove(1));
    Assert.assertNull(cache1.get(1));
    Assert.assertTrue(elementValueComparator.equals(new Element(2, "two"), cache1.get(2)));

    TxThread tx2 = new TxThread() {
      @Override
      public void exec() {
        transactionController.begin(1);

        Assert.assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));

        try {
          cache1.remove(1);
          Assert.fail("expected TransactionTimeoutException");
        } catch (TransactionTimeoutException e) {
          // expected
        }

        transactionController.rollback();
      }
    };
    tx2.start();
    tx2.join();
    tx2.assertNotFailed();

    transactionController.commit();

    transactionController.begin();
    Assert.assertNull(cache1.get(1));
    Assert.assertTrue(elementValueComparator.equals(new Element(2, "two"), cache1.get(2)));
    transactionController.commit();
  }

  public void testRollback() throws Exception {
    System.out.println("testRollback()");
    transactionController.begin();
    cache1.put(new Element(1, "one"));
    transactionController.rollback();

    transactionController.begin();
    Assert.assertNull(cache1.get(1));
    transactionController.rollback();
  }

  public void testTwoConcurrentUpdates() throws Exception {
    System.out.println("testTwoConcurrentUpdates()");
    final CyclicBarrier localBarrier = new CyclicBarrier(2);
    final long WAIT_TIME = 1500;
    final long ERROR_MARGIN = 200;
    final long[] times = new long[2];

    // TX 0
    transactionController.begin();

    cache1.put(new Element(1, "tx1-one"));

    TxThread tx1 = new TxThread() {
      @Override
      public void exec() throws Exception {
        // TX 1
        transactionController.begin(4);

        times[0] = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
        cache1.put(new Element(1, "tx2-one"));
        times[1] = TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);

        // TX 1 must not commit for as long as TX 2 did not commit
        localBarrier.await(60, TimeUnit.SECONDS);

        // TX 1
        transactionController.commit();
      }
    };
    tx1.start();
    ThreadUtil.reallySleep(WAIT_TIME);

    // TX 0
    transactionController.commit();

    // TX 2
    transactionController.begin();
    // asserting data committed by TX 0 and modified but not yet committed by TX 1
    Assert.assertTrue(elementValueComparator.equals(new Element(1, "tx1-one"), cache1.get(1)));
    // TX 2
    transactionController.commit();

    // TX 2 committed, let TX 1 commit
    localBarrier.await(60, TimeUnit.SECONDS);
    tx1.join();
    tx1.assertNotFailed();

    // asserting that TX 1 blocked for a certain time on a lock held by TX 0
    Assert.assertTrue("expected TX1 to be on hold for more than " + WAIT_TIME + "ms, waited: " + (times[1] - times[0]),
                      times[1] - times[0] >= (WAIT_TIME - ERROR_MARGIN));

    // TX 3
    transactionController.begin();
    // asserting data committed by TX 1
    Assert.assertTrue(elementValueComparator.equals(new Element(1, "tx2-one"), cache1.get(1)));
    // TX 3
    transactionController.commit();
  }

  public void testDeadlock() throws Exception {
    System.out.println("testDeadlock()");
    final String[] losingTx = new String[1];

    transactionController.begin(2);

    cache1.put(new Element(1, "tx1-one"));

    final CyclicBarrier barrier1 = new CyclicBarrier(2);
    final CyclicBarrier barrier2 = new CyclicBarrier(2);
    TxThread tx2 = new TxThread() {
      @Override
      public void exec() throws Exception {
        transactionController.begin();

        cache1.put(new Element(2, "tx2-two"));

        // awake tx1
        barrier1.await();
        barrier2.await();

        try {
          cache1.put(new Element(1, "tx2-one"));

          cache1.put(new Element("tx2", ""));

          transactionController.commit();
        } catch (DeadLockException e) {
          losingTx[0] = "tx2";
          transactionController.rollback();
        }
      }
    };
    tx2.start();
    barrier1.await();

    try {
      cache1.put(new Element(2, "tx1-two"));

      cache1.put(new Element("tx1", ""));

      transactionController.commit();
    } catch (DeadLockException e) {
      losingTx[0] = "tx1";
      transactionController.rollback();
    }

    // awake tx2
    barrier2.await();
    tx2.join();
    tx2.assertNotFailed();

    transactionController.begin();
    Element el1 = cache1.get(1);
    Element el2 = cache1.get(2);

    // make sure one TX lost
    Assert.assertNotNull(losingTx[0]);

    // make sure both elements are from the same TX
    String el1TxName = ((String) el1.getValue()).substring(0, 3);
    String el2TxName = ((String) el2.getValue()).substring(0, 3);
    Assert.assertEquals(el1TxName, el2TxName);

    // make sure the winning TX could insert its unique element
    String winningTx = losingTx[0].equals("tx1") ? "tx2" : "tx1";
    Assert.assertNotNull(cache1.get(winningTx));

    // make sure the losing TX could NOT insert its unique element
    Assert.assertNull(cache1.get(losingTx[0]));

    transactionController.commit();
  }

  public void testGetKeys() throws Exception {
    System.out.println("testGetKeys()");
    transactionController.begin(600000);

    cache1.put(new Element(1, "one"));
    Assert.assertEquals(1, cache1.getKeys().size());
    Assert.assertTrue(cache1.getKeys().containsAll(Arrays.asList(1)));

    cache1.put(new Element(2, "two"));
    Assert.assertEquals(2, cache1.getKeys().size());
    Assert.assertTrue(cache1.getKeys().containsAll(Arrays.asList(1, 2)));

    cache1.remove(1);
    Assert.assertEquals(1, cache1.getKeys().size());
    Assert.assertTrue("keys: " + keysToString(), cache1.getKeys().containsAll(Arrays.asList(2)));

    transactionController.commit();

    transactionController.begin();

    cache1.put(new Element(1, "one"));

    final AtomicBoolean tx2Success = new AtomicBoolean(false);
    TxThread tx2 = new TxThread() {
      @Override
      public void exec() {
        transactionController.begin();

        Assert.assertEquals(1, cache1.getKeys().size());
        Assert.assertTrue(cache1.getKeys().containsAll(Arrays.asList(2)));

        transactionController.commit();
        tx2Success.set(true);
      }
    };
    tx2.start();
    tx2.join();
    tx2.assertNotFailed();

    transactionController.commit();
    Assert.assertTrue(tx2Success.get());
  }

  public void testRemoveAll() throws Exception {
    System.out.println("testRemoveAll()");
    transactionController.begin();

    cache1.put(new Element(1, "one"));
    cache1.put(new Element(2, "two"));
    Assert.assertEquals(2, cache1.getSize());

    transactionController.commit();

    transactionController.begin(600000);

    Assert.assertEquals(2, cache1.getSize());
    cache1.removeAll();
    Assert.assertEquals(0, cache1.getSize());

    final CyclicBarrier localBarrier = new CyclicBarrier(2);
    TxThread tx2 = new TxThread() {
      @Override
      public void exec() throws Exception {
        transactionController.begin();

        Assert.assertEquals(2, cache1.getSize());

        cache1.put(new Element(3, "three"));

        // wake up tx1
        localBarrier.await();
        localBarrier.await();

        transactionController.commit();
      }
    };
    tx2.start();
    localBarrier.await();
    Assert.assertEquals(0, cache1.getSize());

    // wake up tx2
    localBarrier.await();

    tx2.join();
    tx2.assertNotFailed();

    transactionController.commit();
  }

  public void testGetSize() throws Exception {
    System.out.println("testGetSize()");
    transactionController.begin();
    Assert.assertEquals(0, cache1.getSize());
    cache1.put(new Element(1, "one"));
    cache1.put(new Element(2, "two"));
    Assert.assertEquals(2, cache1.getSize());

    TxThread tx2 = new TxThread() {
      @Override
      public void exec() {
        transactionController.begin();

        Assert.assertEquals(0, cache1.getSize());
        cache1.put(new Element(3, "three"));
        Assert.assertEquals(1, cache1.getSize());

        transactionController.commit();
      }
    };
    tx2.start();
    tx2.join();
    tx2.assertNotFailed();

    Assert.assertEquals(3, cache1.getSize());

    transactionController.commit();
  }

  public void testPutIfAbsent() throws Exception {
    System.out.println("testPutIfAbsent()");
    transactionController.begin();
    Assert.assertNull(cache1.putIfAbsent(new Element(0, "zero")));
    transactionController.commit();

    transactionController.begin();
    Assert.assertNull(cache1.putIfAbsent(new Element(1, "one")));
    Assert
        .assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.putIfAbsent(new Element(1, "one#2"))));
    Assert.assertTrue(cache1.remove(1));
    Assert.assertNull(cache1.putIfAbsent(new Element(1, "one")));

    final CyclicBarrier localBarrier = new CyclicBarrier(2);
    TxThread tx2 = new TxThread() {
      @Override
      public void exec() throws Exception {
        transactionController.begin();

        Assert.assertTrue(elementValueComparator.equals(new Element(0, "zero"),
                                                        cache1.putIfAbsent(new Element(0, "zero#2"))));

        Assert.assertEquals(1, cache1.getSize());
        cache1.put(new Element(2, "two"));
        Assert.assertEquals(2, cache1.getSize());

        // awake tx1
        localBarrier.await();
        Assert.assertTrue(elementValueComparator.equals(new Element(1, "one"),
                                                        cache1.putIfAbsent(new Element(1, "one#tx2"))));

        transactionController.commit();
      }
    };
    tx2.start();
    localBarrier.await();

    Assert.assertEquals(2, cache1.getSize());
    Assert.assertNull(cache1.get(2));

    transactionController.commit();
    tx2.join();
    tx2.assertNotFailed();

    transactionController.begin();

    Assert.assertEquals(3, cache1.getSize());
    Assert.assertTrue(elementValueComparator.equals(new Element(0, "zero"), cache1.get(0)));
    Assert.assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));
    Assert.assertTrue(elementValueComparator.equals(new Element(2, "two"), cache1.get(2)));

    transactionController.commit();
  }

  public void testRemoveElement() throws Exception {
    System.out.println("testRemoveElement()");
    transactionController.begin();
    Assert.assertEquals(0, cache1.getSize());

    cache1.put(new Element(1, "one"));
    Assert.assertEquals(1, cache1.getSize());

    final CyclicBarrier localBarrier = new CyclicBarrier(2);
    TxThread tx2 = new TxThread() {
      @Override
      public void exec() throws Exception {
        transactionController.begin();

        // awake tx1
        localBarrier.await();
        Assert.assertTrue(cache1.removeElement(new Element(1, "one")));

        localBarrier.await();
        Assert.assertEquals(0, cache1.getSize());
        transactionController.commit();
      }
    };
    tx2.start();

    localBarrier.await();
    transactionController.commit();

    transactionController.begin();
    Assert.assertEquals(1, cache1.getSize());
    transactionController.commit();
    localBarrier.await(); // awake tx2

    tx2.join();
    tx2.assertNotFailed();

    transactionController.begin();
    Assert.assertEquals(0, cache1.getSize());
    Assert.assertFalse(cache1.removeElement(new Element(1, "one")));
    cache1.put(new Element(1, "one"));
    Assert.assertTrue(cache1.removeElement(new Element(1, "one")));
    transactionController.commit();
  }

  public void testReplace() throws Exception {
    System.out.println("testReplace()");
    transactionController.begin();

    Assert.assertNull(cache1.replace(new Element(1, "one")));
    cache1.put(new Element(1, "one"));
    Assert.assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.replace(new Element(1, "one#2"))));
    Assert.assertTrue(elementValueComparator.equals(new Element(1, "one#2"), cache1.replace(new Element(1, "one#3"))));

    Assert.assertTrue(elementValueComparator.equals(new Element(1, "one#3"), cache1.get(1)));

    transactionController.commit();
  }

  public void testReplace2Args() throws Exception {
    System.out.println("testReplace2Args()");
    transactionController.begin();

    Assert.assertFalse(cache1.replace(new Element(1, "one"), new Element(1, "one#2")));
    cache1.put(new Element(1, "one"));
    Assert.assertTrue(cache1.replace(new Element(1, "one"), new Element(1, "one#2")));
    Assert.assertFalse(cache1.replace(new Element(1, "one"), new Element(1, "one#3")));
    Assert.assertTrue(cache1.replace(new Element(1, "one#2"), new Element(1, "one")));

    Assert.assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));

    transactionController.commit();
  }

  public void testDecoratedCache() throws Exception {
    System.out.println("testDecoratedCache()");
    transactionController.begin();

    Ehcache decoratedCache = getCacheManager().getEhcache("testDecorated");
    decoratedCache.put(new Element(1, 2));

    transactionController.commit();
  }

  private String keysToString() {
    List keys = cache1.getKeys();

    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (Object key : keys) {
      sb.append(key).append(", ");
    }
    sb.append("]");

    return sb.toString();
  }

  private static class TxThread extends Thread {
    private volatile boolean failed;

    @Override
    public final void run() {
      try {
        exec();
      } catch (Throwable t) {
        t.printStackTrace();
        failed = true;
      }
    }

    public void exec() throws Exception {
      //
    }

    public void assertNotFailed() {
      if (failed) { throw new AssertionFailedError("TxThread failed"); }
    }
  }

  public static void main(String[] args) {
    System.setProperty("net.sf.ehcache.sizeof.verboseDebugLogging", "true");
    new LocalTxClient(args).run();
  }

}
