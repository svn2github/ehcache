package net.sf.ehcache.transaction.nonxa;

import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;

/**
 * @author lorban
 */
public class TransactionTest extends TestCase {

    private CacheManager cacheManager;
    private Ehcache cache1;
    private Ehcache cache2;
    private TransactionController transactionController;

    @Override
    protected void setUp() throws Exception {
        cacheManager = new CacheManager(TransactionTest.class.getResourceAsStream("/ehcache-nonxa.xml"));
        transactionController = cacheManager.getTransactionController();
        cache1 = cacheManager.getEhcache("txCache1");
        cache1.removeAll();
        cache2 = cacheManager.getEhcache("txCache2");
        cache2.removeAll();
    }

    @Override
    protected void tearDown() throws Exception {
        cacheManager.shutdown();
    }

    public void testTwoCaches() throws Exception {
        transactionController.begin();

        cache1.put(new Element(1, "one"));
        cache2.put(new Element(1, "one"));

        Thread tx2 = new Thread() {
            @Override
            public void run() {
                transactionController.begin();

                assertNull(cache1.get(1));
                assertNull(cache2.get(1));

                transactionController.commit();
            }
        };
        tx2.start();
        tx2.join();

        transactionController.commit();

    }


    public void testPut() throws Exception {
        transactionController.begin();

        cache1.put(new Element(1, "one"));

        assertEquals(new Element(1, "one"), cache1.get(1));

        Thread tx2 = new Thread() {
            @Override
            public void run() {
                transactionController.begin();

                assertNull(cache1.get(1));

                transactionController.commit();
            }
        };
        tx2.start();
        tx2.join();


        transactionController.commit();

        transactionController.begin();

        assertEquals(new Element(1, "one"), cache1.get(1));

        transactionController.commit();

        Thread tx3 = new Thread() {
            @Override
            public void run() {
                transactionController.begin();

                assertEquals(new Element(1, "one"), cache1.get(1));

                transactionController.commit();
            }
        };
        tx3.start();
        tx3.join();
    }

    public void testRemove() throws Exception {
        transactionController.begin();
        cache1.put(new Element(1, "one"));
        transactionController.commit();

        transactionController.begin();

        assertEquals(new Element(1, "one"), cache1.get(1));

        assertTrue(cache1.remove(1));

        assertNull(cache1.get(1));


        Thread tx2 = new Thread() {
            @Override
            public void run() {
                transactionController.begin(1);

                assertEquals(new Element(1, "one"), cache1.get(1));

                try {
                    cache1.remove(1);
                    fail("expected TransactionException");
                } catch (TransactionException e) {
                    // expected
                }

                transactionController.commit();
            }
        };
        tx2.start();
        tx2.join();


        transactionController.commit();

        transactionController.begin();
        assertNull(cache1.get(1));
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
        final long[] times = new long[2];

        transactionController.begin(); //TX 0

        cache1.put(new Element(1, "tx1-one"));

        Thread tx2 = new Thread() {
            @Override
            public void run() {
                transactionController.begin(); //TX 1

                times[0] = System.currentTimeMillis();
                cache1.put(new Element(1, "tx2-one"));
                times[1] = System.currentTimeMillis();

                transactionController.commit(); //TX 1
            }
        };
        tx2.start();
        tx2.join(WAIT_TIME);

        transactionController.commit(); //TX 0


        transactionController.begin(); //TX 2
        assertEquals(new Element(1, "tx1-one"), cache1.get(1));
        transactionController.commit(); //TX 2

        tx2.join();

        assertTrue(times[1] - times[0] >= WAIT_TIME);

        transactionController.begin(); // TX 3
        assertEquals(new Element(1, "tx2-one"), cache1.get(1));
        transactionController.commit(); // TX 3
    }

    public void testDeadlock() throws Exception {
        final String[] losingTx = new String[1];

        transactionController.begin(1);

        cache1.put(new Element(1, "tx1-one"));

        Thread tx2 = new Thread() {
            @Override
            public void run() {
                transactionController.begin(1);

                cache1.put(new Element(2, "tx2-two"));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }

                try {
                    cache1.put(new Element(1, "tx2-one"));

                    cache1.put(new Element("tx2", ""));

                    transactionController.commit();
                } catch (TransactionException e) {
                    losingTx[0] = "tx2";
                    transactionController.rollback();
                }
            }
        };
        tx2.start();
        tx2.join(500);

        try {
            cache1.put(new Element(2, "tx1-two"));

            cache1.put(new Element("tx1", ""));

            transactionController.commit();
        } catch (TransactionException e) {
            losingTx[0] = "tx1";
            transactionController.rollback();
        }

        tx2.join();


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

}
