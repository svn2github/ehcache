package net.sf.ehcache.transaction.local;

import bitronix.tm.TransactionManagerServices;
import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import javax.transaction.RollbackException;
import javax.transaction.TransactionManager;

/**
 * @author lorban
 */
public class JtaLocalRcTest extends TestCase {

    private CacheManager cacheManager;
    private Ehcache cache1;
    private Ehcache cache2;
    private TransactionManager transactionManager;

    @Override
    protected void setUp() throws Exception {
        TransactionManagerServices.getConfiguration().setGracefulShutdownInterval(0);
        TransactionManagerServices.getConfiguration().setJournal("null");
        TransactionManagerServices.getConfiguration().setWarnAboutZeroResourceTransaction(false);
        TransactionManagerServices.getConfiguration().setServerId(JtaLocalRcTest.class.getName());
        transactionManager = TransactionManagerServices.getTransactionManager();
        cacheManager = new CacheManager(JtaLocalRcTest.class.getResourceAsStream("/ehcache-jta_local_rc.xml"));

        transactionManager.begin();
        cache1 = cacheManager.getEhcache("txCache1");
        cache1.removeAll();
        cache2 = cacheManager.getEhcache("txCache2");
        cache2.removeAll();
        transactionManager.commit();
    }

    @Override
    protected void tearDown() throws Exception {
        if (transactionManager.getTransaction() != null) {
            transactionManager.rollback();
        }
        cacheManager.shutdown();
        TransactionManagerServices.getTransactionManager().shutdown();
    }

    public void testTwoCaches() throws Exception {
        transactionManager.begin();

        cache1.put(new Element(1, "one"));
        cache2.put(new Element(1, "one"));

        Thread tx2 = new TxThread() {
            @Override
            public void runTx() throws Exception {
                transactionManager.begin();

                assertNull(cache1.get(1));
                assertNull(cache2.get(1));

                transactionManager.commit();
            }
        };
        tx2.start();
        tx2.join();

        transactionManager.commit();

    }


    public void testPut() throws Exception {
        transactionManager.begin();

        cache1.put(new Element(1, "one"));

        assertEquals(new Element(1, "one"), cache1.get(1));

        Thread tx2 = new TxThread() {
            @Override
            public void runTx() throws Exception {
                transactionManager.begin();

                assertNull(cache1.get(1));

                transactionManager.commit();
            }
        };
        tx2.start();
        tx2.join();


        transactionManager.commit();

        transactionManager.begin();

        assertEquals(new Element(1, "one"), cache1.get(1));

        transactionManager.commit();

        Thread tx3 = new TxThread() {
            @Override
            public void runTx() throws Exception {
                transactionManager.begin();

                assertEquals(new Element(1, "one"), cache1.get(1));

                transactionManager.commit();
            }
        };
        tx3.start();
        tx3.join();
    }

    public void testRemove() throws Exception {
        transactionManager.begin();
        cache1.put(new Element(1, "one"));
        transactionManager.commit();

        transactionManager.begin();

        assertEquals(new Element(1, "one"), cache1.get(1));

        assertTrue(cache1.remove(1));

        assertNull(cache1.get(1));


        Thread tx2 = new TxThread() {
            @Override
            public void runTx() throws Exception {
                transactionManager.setTransactionTimeout(1);
                transactionManager.begin();

                assertEquals(new Element(1, "one"), cache1.get(1));

                try {
                    cache1.remove(1);
                    fail("expected TransactionException");
                } catch (TransactionException e) {
                    // expected
                }

                transactionManager.commit();
            }
        };
        tx2.start();
        tx2.join();


        transactionManager.commit();

        transactionManager.begin();
        assertNull(cache1.get(1));
        transactionManager.commit();
    }

    public void testRollback() throws Exception {
        transactionManager.begin();
        cache1.put(new Element(1, "one"));
        transactionManager.rollback();

        transactionManager.begin();
        assertNull(cache1.get(1));
        transactionManager.rollback();
    }

    public void testRollbackOnly() throws Exception {
        transactionManager.begin();

        cache1.put(new Element(1, "one"));

        transactionManager.setRollbackOnly();
        try {
            transactionManager.commit();
            fail("expected RollbackException");
        } catch (RollbackException e) {
            // expected
        }

        transactionManager.begin();

        assertNull(cache1.get(1));

        transactionManager.commit();
    }

    public void testTwoConcurrentUpdates() throws Exception {
        final long WAIT_TIME = 500; // this must be shorter than the local ehcache TX timeout
        final long[] times = new long[2];

        transactionManager.begin(); //TX 0

        cache1.put(new Element(1, "tx1-one"));

        Thread tx2 = new TxThread() {
            @Override
            public void runTx() throws Exception {
                transactionManager.begin(); //TX 1

                times[0] = System.currentTimeMillis();
                cache1.put(new Element(1, "tx2-one"));
                times[1] = System.currentTimeMillis();

                transactionManager.commit(); //TX 1
            }
        };
        tx2.start();
        tx2.join(WAIT_TIME);

        transactionManager.commit(); //TX 0


        transactionManager.begin(); //TX 2
        assertEquals(new Element(1, "tx1-one"), cache1.get(1));
        transactionManager.commit(); //TX 2

        tx2.join();

        assertTrue(times[1] - times[0] >= WAIT_TIME);

        transactionManager.begin(); // TX 3
        assertEquals(new Element(1, "tx2-one"), cache1.get(1));
        transactionManager.commit(); // TX 3
    }

    public void testDeadlock() throws Exception {
        final String[] losingTx = new String[1];

        transactionManager.setTransactionTimeout(1);
        transactionManager.begin();

        cache1.put(new Element(1, "tx1-one"));

        Thread tx2 = new TxThread() {
            @Override
            public void runTx() throws Exception {
                transactionManager.setTransactionTimeout(5);
                transactionManager.begin();

                cache1.put(new Element(2, "tx2-two"));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }

                try {
                    cache1.put(new Element(1, "tx2-one"));

                    cache1.put(new Element("tx2", ""));

                    transactionManager.commit();
                } catch (DeadLockException e) {
                    losingTx[0] = "tx2";
                    transactionManager.rollback();
                }
            }
        };
        tx2.start();
        tx2.join(500);

        try {
            cache1.put(new Element(2, "tx1-two"));

            cache1.put(new Element("tx1", ""));

            transactionManager.commit();
        } catch (DeadLockException e) {
            losingTx[0] = "tx1";
            transactionManager.rollback();
        }

        tx2.join();


        transactionManager.begin();
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

        transactionManager.commit();
    }

    public void testGetKeys() throws Exception {
        transactionManager.begin();

        cache1.put(new Element(1, "one"));
        assertEquals(1, cache1.getKeys().size());

        cache1.put(new Element(2, "two"));
        assertEquals(2, cache1.getKeys().size());

        cache1.remove(1);
        assertEquals(1, cache1.getKeys().size());

        transactionManager.commit();


        transactionManager.begin();

        cache1.put(new Element(1, "one"));

        Thread tx2 = new TxThread() {
            @Override
            public void runTx() throws Exception {
                transactionManager.begin();

                assertEquals(1, cache1.getKeys().size());

                transactionManager.commit();
            }
        };
        tx2.start();
        tx2.join();

        transactionManager.commit();
    }

    public void testRemoveAll() throws Exception {
        transactionManager.begin();

        cache1.put(new Element(1, "one"));
        cache1.put(new Element(2, "two"));
        assertEquals(2, cache1.getSize());

        transactionManager.commit();


        transactionManager.begin();

        assertEquals(2, cache1.getSize());
        cache1.removeAll();
        assertEquals(0, cache1.getSize());

        Thread tx2 = new TxThread() {
            @Override
            public void runTx() throws Exception {
                transactionManager.begin();

                assertEquals(2, cache1.getSize());

                transactionManager.commit();
            }
        };
        tx2.start();
        tx2.join();

        transactionManager.commit();
    }

    public void testGetSize() throws Exception {
        transactionManager.begin();
        assertEquals(0, cache1.getSize());
        cache1.put(new Element(1, "one"));
        cache1.put(new Element(2, "two"));
        assertEquals(2, cache1.getSize());

        Thread tx2 = new TxThread() {
            @Override
            public void runTx() throws Exception {
                transactionManager.begin();

                assertEquals(0, cache1.getSize());
                cache1.put(new Element(3, "three"));
                assertEquals(1, cache1.getSize());

                transactionManager.commit();
            }
        };
        tx2.start();
        tx2.join();

        assertEquals(3, cache1.getSize());

        transactionManager.commit();
    }

    private static abstract class TxThread extends Thread {
        @Override
        public void run() {
            try {
                runTx();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public abstract void runTx() throws Exception;
    }

}
