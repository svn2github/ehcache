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

    @Override
    protected void setUp() throws Exception {
        cacheManager = new CacheManager(TransactionTest.class.getResourceAsStream("/ehcache-nonxa.xml"));
        TransactionController.setCacheManager(cacheManager);
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
        TransactionController.getInstance().begin();

        cache1.put(new Element(1, "one"));
        cache2.put(new Element(1, "one"));

        Thread tx2 = new Thread() {
            @Override
            public void run() {
                TransactionController.getInstance().begin();

                assertNull(cache1.get(1));
                assertNull(cache2.get(1));

                TransactionController.getInstance().commit();
            }
        };
        tx2.start();
        tx2.join();

        TransactionController.getInstance().commit();

    }


    public void testOneCache() throws Exception {
        TransactionController.getInstance().begin();

        cache1.put(new Element(1, "one"));

        assertEquals(new Element(1, "one"), cache1.get(1));

        Thread tx2 = new Thread() {
            @Override
            public void run() {
                TransactionController.getInstance().begin();

                assertNull(cache1.get(1));

                TransactionController.getInstance().commit();
            }
        };
        tx2.start();
        tx2.join();


        TransactionController.getInstance().commit();

        TransactionController.getInstance().begin();

        assertEquals(new Element(1, "one"), cache1.get(1));

        TransactionController.getInstance().commit();

        Thread tx3 = new Thread() {
            @Override
            public void run() {
                TransactionController.getInstance().begin();

                assertEquals(new Element(1, "one"), cache1.get(1));

                TransactionController.getInstance().commit();
            }
        };
        tx3.start();
        tx3.join();
    }

    public void testRollback() throws Exception {
        TransactionController.getInstance().begin();
        cache1.put(new Element(1, "one"));
        TransactionController.getInstance().rollback();

        TransactionController.getInstance().begin();
        assertNull(cache1.get(1));
        TransactionController.getInstance().rollback();
    }

    public void testRollbackOnly() throws Exception {
        TransactionController.getInstance().begin();

        cache1.put(new Element(1, "one"));

        TransactionController.getInstance().setRollbackOnly();
        try {
            TransactionController.getInstance().commit();
            fail("expected TransactionException");
        } catch (TransactionException e) {
            // expected
        }

        TransactionController.getInstance().begin();

        assertNull(cache1.get(1));

        TransactionController.getInstance().commit();
    }

    public void testTwoConcurrentUpdates() throws Exception {
        final long WAIT_TIME = 1500;
        final long[] times = new long[2];

        TransactionController.getInstance().begin(); //TX 0

        cache1.put(new Element(1, "tx1-one"));

        Thread tx2 = new Thread() {
            @Override
            public void run() {
                TransactionController.getInstance().begin(); //TX 1

                times[0] = System.currentTimeMillis();
                cache1.put(new Element(1, "tx2-one"));
                times[1] = System.currentTimeMillis();

                TransactionController.getInstance().commit(); //TX 1
            }
        };
        tx2.start();
        tx2.join(WAIT_TIME);

        TransactionController.getInstance().commit(); //TX 0


        TransactionController.getInstance().begin(); //TX 2
        assertEquals(new Element(1, "tx1-one"), cache1.get(1));
        TransactionController.getInstance().commit(); //TX 2

        tx2.join();

        assertTrue(times[1] - times[0] >= WAIT_TIME);

        TransactionController.getInstance().begin(); // TX 3
        assertEquals(new Element(1, "tx2-one"), cache1.get(1));
        TransactionController.getInstance().commit(); // TX 3
    }

    public void testDeadlock() throws Exception {
        final String[] losingTx = new String[1];

        TransactionController.getInstance().begin(1);

        cache1.put(new Element(1, "tx1-one"));

        Thread tx2 = new Thread() {
            @Override
            public void run() {
                TransactionController.getInstance().begin(1);

                cache1.put(new Element(2, "tx2-two"));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }

                try {
                    cache1.put(new Element(1, "tx2-one"));

                    cache1.put(new Element("tx2", ""));

                    TransactionController.getInstance().commit();
                } catch (TransactionException e) {
                    losingTx[0] = "tx2";
                    TransactionController.getInstance().rollback();
                }
            }
        };
        tx2.start();
        tx2.join(500);

        try {
            cache1.put(new Element(2, "tx1-two"));

            cache1.put(new Element("tx1", ""));

            TransactionController.getInstance().commit();
        } catch (TransactionException e) {
            losingTx[0] = "tx1";
            TransactionController.getInstance().rollback();
        }

        tx2.join();


        TransactionController.getInstance().begin();
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

        TransactionController.getInstance().commit();
    }

}
