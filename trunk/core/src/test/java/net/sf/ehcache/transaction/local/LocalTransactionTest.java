package net.sf.ehcache.transaction.local;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.store.DefaultElementValueComparator;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.transaction.DeadLockException;
import net.sf.ehcache.transaction.TransactionException;
import net.sf.ehcache.transaction.TransactionInterruptedException;
import net.sf.ehcache.transaction.TransactionTimeoutException;

import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author lorban
 */
public class LocalTransactionTest extends TestCase {

    private final ElementValueComparator elementValueComparator = new DefaultElementValueComparator();
    private CacheManager cacheManager;
    private Ehcache cache1;
    private Ehcache cache2;
    private TransactionController transactionController;

    @Override
    protected void setUp() throws Exception {
        cacheManager = new CacheManager(LocalTransactionTest.class.getResourceAsStream("/ehcache-tx-local.xml"));
        transactionController = cacheManager.getTransactionController();
        transactionController.begin();
        cache1 = cacheManager.getEhcache("txCache1");
        cache1.removeAll();
        cache2 = cacheManager.getEhcache("txCache2");
        cache2.removeAll();
        transactionController.commit();
    }

    @Override
    protected void tearDown() throws Exception {
        if (transactionController.getCurrentTransactionContext() != null) {
            transactionController.rollback();
        }
        cacheManager.shutdown();
    }

    public void testTransactionContextLifeCycle() throws Exception {
        assertNull(transactionController.getCurrentTransactionContext());
        transactionController.begin();
        try {
            transactionController.begin();
            fail("expected TransactionException");
        } catch (TransactionException e) {
            // expected
        }
        assertNotNull(transactionController.getCurrentTransactionContext());
        transactionController.commit();
        try {
            transactionController.commit();
            fail("expected TransactionException");
        } catch (Exception e) {
            // expected
        }
        try {
            transactionController.rollback();
            fail("expected TransactionException");
        } catch (Exception e) {
            // expected
        }
        assertNull(transactionController.getCurrentTransactionContext());
    }

    public void testInterruption() throws Exception {
        transactionController.begin();
        Thread.currentThread().interrupt();
        try {
            cache1.get(1);
            fail("expected TransactionInterruptedException");
        } catch (TransactionInterruptedException e) {
            // expected
        }

        // make sure interrupted status got cleared
        cache1.get(1);

        cache1.put(new Element(1, "one"));

        final CyclicBarrier barrier = new CyclicBarrier(2);
        TxThread tx2 = new TxThread() {
            @Override
            public void exec() throws Exception {
                transactionController.begin();

                // awake tx1
                barrier.await();
                try {
                    cache1.put(new Element(1, "one#tx2"));
                } catch (TransactionInterruptedException e) {
                    // expected
                }

                transactionController.commit();
            }
        };
        tx2.start();

        barrier.await();
        tx2.interrupt();

        tx2.join();
        tx2.assertNotFailed();

        transactionController.commit();
    }

    public void testTimeout() throws Exception {
        transactionController.setDefaultTransactionTimeout(1);
        transactionController.begin();

        cache1.put(new Element(1, "one"));

        Thread.sleep(1500);

        try {
            cache1.get(1);
            fail("expected TransactionTimeoutException");
        } catch (TransactionTimeoutException e) {
            // expected
        }

        try {
            cache1.getQuiet(1);
            fail("expected TransactionTimeoutException");
        } catch (TransactionTimeoutException e) {
            // expected
        }

        try {
            cache1.getKeys();
            fail("expected TransactionTimeoutException");
        } catch (TransactionTimeoutException e) {
            // expected
        }

        try {
            cache1.getSize();
            fail("expected TransactionTimeoutException");
        } catch (TransactionTimeoutException e) {
            // expected
        }

        try {
            cache1.removeAll();
            fail("expected TransactionTimeoutException");
        } catch (TransactionTimeoutException e) {
            // expected
        }

        try {
            cache1.put(new Element(2, "two"));
            fail("expected TransactionTimeoutException");
        } catch (TransactionTimeoutException e) {
            // expected
        }

        try {
            cache1.remove(1);
            fail("expected TransactionTimeoutException");
        } catch (TransactionTimeoutException e) {
            // expected
        }

        transactionController.rollback();
    }

    public void testDefaultTimeout() throws Exception {
        transactionController.setDefaultTransactionTimeout(1);
        transactionController.begin();

        Thread.sleep(1500);

        try {
            transactionController.commit();
            fail("expected TransactionTimeoutException");
        } catch (TransactionTimeoutException e) {
            // expected
        }
    }

    public void testCopyOnRead() throws Exception {
        transactionController.begin();
        Object putValue = new Object[]{"one#1"};
        cache1.put(new Element(1, putValue));

        Element one = cache1.get(1);
        Object getValue = one.getObjectValue();

        assertFalse(putValue == getValue);
        transactionController.commit();
    }

    public void testCopyOnWrite() throws Exception {
        transactionController.begin();
        Object[] putValue = new Object[]{"one#1"};
        cache1.put(new Element(1, putValue));
        putValue[0] = "one#2";

        Element one = cache1.get(1);
        Object[] getValue = (Object[]) one.getObjectValue();

        assertEquals("one#1", getValue[0]);
        transactionController.commit();
    }

    public void testTwoPuts() throws Exception {
        transactionController.begin();
        cache1.put(new Element(1, new Object[]{"one#1"}));
        transactionController.commit();

        transactionController.begin();
        cache1.put(new Element(1, new Object[]{"one#2"}));
        transactionController.commit();
    }

    public void testTwoCaches() throws Exception {
        transactionController.begin();

        cache1.put(new Element(1, "one"));
        cache2.put(new Element(1, "one"));

        TxThread tx2 = new TxThread() {
            @Override
            public void exec() {
                transactionController.begin();

                assertNull(cache1.get(1));
                assertNull(cache2.get(1));

                transactionController.commit();
            }
        };
        tx2.start();
        tx2.join();
        tx2.assertNotFailed();

        transactionController.commit();
    }


    public void testPut() throws Exception {
        transactionController.begin();

        cache1.put(new Element(1, "one"));

        assertEquals(new Element(1, "one"), cache1.get(1));

        TxThread tx2 = new TxThread() {
            @Override
            public void exec() {
                transactionController.begin();

                assertNull(cache1.get(1));

                transactionController.commit();
            }
        };
        tx2.start();
        tx2.join();
        tx2.assertNotFailed();

        transactionController.commit();

        transactionController.begin();
        assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));
        transactionController.commit();

        TxThread tx3 = new TxThread() {
            @Override
            public void exec() {
                transactionController.begin();

                assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));

                transactionController.commit();
            }
        };
        tx3.start();
        tx3.join();
        tx3.assertNotFailed();
    }

    public void testRemove() throws Exception {
        transactionController.begin();
        cache1.put(new Element(1, "one"));
        transactionController.commit();

        transactionController.begin();

        assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));
        cache1.put(new Element(2, "two"));
        assertTrue(cache1.remove(1));
        assertNull(cache1.get(1));
        assertTrue(elementValueComparator.equals(new Element(2, "two"), cache1.get(2)));


        TxThread tx2 = new TxThread() {
            @Override
            public void exec() {
                transactionController.begin(1);

                assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));

                try {
                    cache1.remove(1);
                    fail("expected DeadLockException");
                } catch (DeadLockException e) {
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
        assertNull(cache1.get(1));
        assertTrue(elementValueComparator.equals(new Element(2, "two"), cache1.get(2)));
        transactionController.commit();
    }

    public void testRollback() throws Exception {
        transactionController.begin();
        cache1.put(new Element(1, "one"));
        transactionController.rollback();

        transactionController.begin();
        assertNull(cache1.get(1));
        transactionController.rollback();
    }

    public void testRollbackOnly() throws Exception {
        transactionController.begin();

        cache1.put(new Element(1, "one"));

        transactionController.setRollbackOnly();
        try {
            transactionController.commit();
            fail("expected TransactionException");
        } catch (TransactionException e) {
            // expected
        }

        transactionController.begin();

        assertNull(cache1.get(1));

        transactionController.commit();
    }

    public void testTwoConcurrentUpdates() throws Exception {
        final long WAIT_TIME = 1500;
        final long ERROR_MARGIN = 200;
        final long[] times = new long[2];

        //TX 0
        transactionController.begin();

        cache1.put(new Element(1, "tx1-one"));

        TxThread tx2 = new TxThread() {
            @Override
            public void exec() throws InterruptedException {
                //TX 1
                transactionController.begin(4);

                times[0] = System.currentTimeMillis();
                cache1.put(new Element(1, "tx2-one"));
                times[1] = System.currentTimeMillis();

                Thread.sleep(WAIT_TIME);

                //TX 1
                transactionController.commit();
            }
        };
        tx2.start();
        tx2.join(WAIT_TIME);

        //TX 0
        transactionController.commit();

        //TX 2
        transactionController.begin();
        assertTrue(elementValueComparator.equals(new Element(1, "tx1-one"), cache1.get(1)));
        //TX 2
        transactionController.commit();

        tx2.join();
        tx2.assertNotFailed();

        assertTrue("expected TX1 to be on hold for more than " + WAIT_TIME + "ms, waited: " + (times[1] - times[0]), times[1] - times[0]
                >= (WAIT_TIME - ERROR_MARGIN));

        // TX 3
        transactionController.begin();
        assertTrue(elementValueComparator.equals(new Element(1, "tx2-one"), cache1.get(1)));
        // TX 3
        transactionController.commit();
    }

    public void testDeadlock() throws Exception {
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
        assertNotNull(losingTx[0]);

        // make sure both elements are from the same TX
        String el1TxName = ((String) el1.getValue()).substring(0, 3);
        String el2TxName = ((String) el2.getValue()).substring(0, 3);
        assertEquals(el1TxName, el2TxName);

        // make sure the winning TX could insert its unique element
        String winningTx = losingTx[0].equals("tx1") ? "tx2" : "tx1";
        assertNotNull(cache1.get(winningTx));

        // make sure the losing TX could NOT insert its unique element
        assertNull(cache1.get(losingTx[0]));

        transactionController.commit();
    }

    public void testGetKeys() throws Exception {
        transactionController.begin();

        cache1.put(new Element(1, "one"));
        assertEquals(1, cache1.getKeys().size());
        assertTrue(cache1.getKeys().containsAll(Arrays.asList(1)));

        cache1.put(new Element(2, "two"));
        assertEquals(2, cache1.getKeys().size());
        assertTrue(cache1.getKeys().containsAll(Arrays.asList(1, 2)));

        cache1.remove(1);
        assertEquals(1, cache1.getKeys().size());
        assertTrue(cache1.getKeys().containsAll(Arrays.asList(2)));

        transactionController.commit();


        transactionController.begin();

        cache1.put(new Element(1, "one"));

        final AtomicBoolean tx2Success = new AtomicBoolean(false);
        TxThread tx2 = new TxThread() {
            @Override
            public void exec() {
                transactionController.begin();

                assertEquals(1, cache1.getKeys().size());
                assertTrue(cache1.getKeys().containsAll(Arrays.asList(2)));

                transactionController.commit();
                tx2Success.set(true);
            }
        };
        tx2.start();
        tx2.join();
        tx2.assertNotFailed();

        transactionController.commit();
        assertTrue(tx2Success.get());
    }

    public void testRemoveAll() throws Exception {
        transactionController.begin();

        cache1.put(new Element(1, "one"));
        cache1.put(new Element(2, "two"));
        assertEquals(2, cache1.getSize());

        transactionController.commit();


        transactionController.begin();

        assertEquals(2, cache1.getSize());
        cache1.removeAll();
        assertEquals(0, cache1.getSize());

        final CyclicBarrier barrier = new CyclicBarrier(2);
        TxThread tx2 = new TxThread() {
            @Override
            public void exec() throws Exception {
                transactionController.begin();

                assertEquals(2, cache1.getSize());

                cache1.put(new Element(3, "three"));

                //wake up tx1
                barrier.await();
                barrier.await();

                transactionController.commit();
            }
        };
        tx2.start();
        barrier.await();
        assertEquals(0, cache1.getSize());

        // wake up tx2
        barrier.await();

        tx2.join();
        tx2.assertNotFailed();

        transactionController.commit();
    }

    public void testGetSize() throws Exception {
        transactionController.begin();
        assertEquals(0, cache1.getSize());
        cache1.put(new Element(1, "one"));
        cache1.put(new Element(2, "two"));
        assertEquals(2, cache1.getSize());

        TxThread tx2 = new TxThread() {
            @Override
            public void exec() {
                transactionController.begin();

                assertEquals(0, cache1.getSize());
                cache1.put(new Element(3, "three"));
                assertEquals(1, cache1.getSize());

                transactionController.commit();
            }
        };
        tx2.start();
        tx2.join();
        tx2.assertNotFailed();

        assertEquals(3, cache1.getSize());

        transactionController.commit();
    }

    public void testPutIfAbsent() throws Exception {
        transactionController.begin();
        assertNull(cache1.putIfAbsent(new Element(0, "zero")));
        transactionController.commit();

        transactionController.begin();
        assertNull(cache1.putIfAbsent(new Element(1, "one")));
        assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.putIfAbsent(new Element(1, "one#2"))));
        assertTrue(cache1.remove(1));
        assertNull(cache1.putIfAbsent(new Element(1, "one")));

        final CyclicBarrier barrier = new CyclicBarrier(2);
        TxThread tx2 = new TxThread() {
            @Override
            public void exec() throws Exception {
                transactionController.begin();

                assertTrue(elementValueComparator.equals(new Element(0, "zero"), cache1.putIfAbsent(new Element(0, "zero#2"))));

                assertEquals(1, cache1.getSize());
                cache1.put(new Element(2, "two"));
                assertEquals(2, cache1.getSize());

                // awake tx1
                barrier.await();
                assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.putIfAbsent(new Element(1, "one#tx2"))));

                transactionController.commit();
            }
        };
        tx2.start();
        barrier.await();

        assertEquals(2, cache1.getSize());
        assertNull(cache1.get(2));

        transactionController.commit();
        tx2.join();
        tx2.assertNotFailed();


        transactionController.begin();

        assertEquals(3, cache1.getSize());
        assertTrue(elementValueComparator.equals(new Element(0, "zero"), cache1.get(0)));
        assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));
        assertTrue(elementValueComparator.equals(new Element(2, "two"), cache1.get(2)));

        transactionController.commit();
    }

    public void testRemoveElement() throws Exception {
        transactionController.begin();
        assertEquals(0, cache1.getSize());

        cache1.put(new Element(1, "one"));
        assertEquals(1, cache1.getSize());

        final CyclicBarrier barrier = new CyclicBarrier(2);
        TxThread tx2 = new TxThread() {
            @Override
            public void exec() throws Exception {
                transactionController.begin();

                // awake tx1
                barrier.await();
                assertTrue(cache1.removeElement(new Element(1, "one")));

                barrier.await();
                assertEquals(0, cache1.getSize());
                transactionController.commit();
            }
        };
        tx2.start();

        barrier.await();
        transactionController.commit();

        transactionController.begin();
        assertEquals(1, cache1.getSize());
        transactionController.commit();
        barrier.await(); // awake tx2

        tx2.join();
        tx2.assertNotFailed();

        transactionController.begin();
        assertEquals(0, cache1.getSize());
        assertFalse(cache1.removeElement(new Element(1, "one")));
        cache1.put(new Element(1, "one"));
        assertTrue(cache1.removeElement(new Element(1, "one")));
        transactionController.commit();
    }

    public void testReplace() throws Exception {
        transactionController.begin();

        assertNull(cache1.replace(new Element(1, "one")));
        cache1.put(new Element(1, "one"));
        assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.replace(new Element(1, "one#2"))));
        assertTrue(elementValueComparator.equals(new Element(1, "one#2"), cache1.replace(new Element(1, "one#3"))));

        assertTrue(elementValueComparator.equals(new Element(1, "one#3"), cache1.get(1)));

        transactionController.commit();
    }

    public void testReplace2Args() throws Exception {
        transactionController.begin();

        assertFalse(cache1.replace(new Element(1, "one"), new Element(1, "one#2")));
        cache1.put(new Element(1, "one"));
        assertTrue(cache1.replace(new Element(1, "one"), new Element(1, "one#2")));
        assertFalse(cache1.replace(new Element(1, "one"), new Element(1, "one#3")));
        assertTrue(cache1.replace(new Element(1, "one#2"), new Element(1, "one")));

        assertTrue(elementValueComparator.equals(new Element(1, "one"), cache1.get(1)));

        transactionController.commit();
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
        }

        public void assertNotFailed() {
            if (failed) {
                throw new AssertionFailedError("TxThread failed");
            }
        }
    }

}
