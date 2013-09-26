package net.sf.ehcache.transaction.xa;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.TransactionStatusChangeListener;
import bitronix.tm.recovery.Recoverer;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.XAResourceProducer;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.CacheStoreHelper;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.TxCopyingCacheStore;
import net.sf.ehcache.transaction.TransactionTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * @author Ludovic Orban
 */
public class XATransactionTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(XATransactionTest.class);

    private TransactionManager tm;
    private CacheManager cacheManager;
    private Ehcache cache1;
    private Ehcache cache2;

    @Override
    protected void setUp() throws Exception {
        TransactionManagerServices.getConfiguration().setJournal("null").setGracefulShutdownInterval(0).setBackgroundRecoveryIntervalSeconds(1);

        tm = TransactionManagerServices.getTransactionManager();
        cacheManager = new CacheManager(XATransactionTest.class.getResourceAsStream("/ehcache-tx-twopc.xml"));

        cache1 = cacheManager.getEhcache("txCache1");
        cache2 = cacheManager.getEhcache("txCache2");
        tm.begin();

        cache1.removeAll();
        cache2.removeAll();

        tm.commit();
    }

    @Override
    protected void tearDown() throws Exception {
        if (tm.getTransaction() != null) {
            tm.rollback();
        }
        cacheManager.shutdown();
        TransactionManagerServices.getTransactionManager().shutdown();
    }

    public void testSimple() throws Exception {
        LOG.info("******* START");

        tm.begin();
        cache1.get(1);
        cache1.put(new Element(1, "one"));
        tm.commit();

        tm.begin();
        Element e = cache1.get(1);
        assertEquals("one", e.getObjectValue());
        cache1.remove(1);
        e = cache1.get(1);
        assertNull(e);
        int size = cache1.getSize();
        assertEquals(0, size);
        tm.rollback();

        tm.begin();
        e = cache1.get(1);
        assertEquals("one", e.getObjectValue());

        tm.rollback();

        LOG.info("******* END");
    }

    public void testRecoveryWhileTransactionsAreLive() throws Exception {
        tm.begin();

        BitronixTransaction transaction = (BitronixTransaction)tm.getTransaction();
        transaction.addTransactionStatusChangeListener(new TransactionStatusChangeListener() {
            @Override
            public void statusChanged(int oldStatus, int newStatus) {
                if (oldStatus == Status.STATUS_PREPARED) {
                    try {
                        // <MNK-4250>
                        // the BTM recoverer must not run while we mess with the internal BTM structures
                        // so cancel it and wait until it doesn't run anymore
                        Recoverer recoverer = TransactionManagerServices.getRecoverer();
                        TransactionManagerServices.getTaskScheduler().cancelRecovery(recoverer);
                        while (recoverer.isRunning()) {
                            try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
                        }

                        XAResourceProducer txCache1Producer = ResourceRegistrar.get("txCache1");
                        XAResource xaResource1 = txCache1Producer.startRecovery().getXAResource();
                        Xid[] recoveredXids1 = xaResource1.recover(XAResource.TMSTARTRSCAN);
                        txCache1Producer.endRecovery();

                        XAResourceProducer txCache2Producer = ResourceRegistrar.get("txCache2");
                        XAResource xaResource2 = txCache2Producer.startRecovery().getXAResource();
                        Xid[] recoveredXids2 = xaResource2.recover(XAResource.TMSTARTRSCAN);
                        txCache1Producer.endRecovery();

                        // recover should not return XIDs of active transactions
                        assertEquals(0, recoveredXids1.length);
                        assertEquals(0, recoveredXids2.length);

                        // reschedule the BTM recoverer
                        // </MNK-4250>
                        TransactionManagerServices.getTaskScheduler().scheduleRecovery(recoverer, new java.util.Date());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        cache1.put(new Element(1, "one"));
        cache2.put(new Element(1, "one"));
        tm.commit();
    }

    public void testPutDuring2PC() throws Exception {
        tm.begin();

        cache1.put(new Element(1, "one"));
        // 1PC bypasses 1st phase -> enlist a 2nd resource to prevent it
        cache2.put(new Element(1, "one"));

        BitronixTransaction tx = (BitronixTransaction) tm.getTransaction();

        tx.addTransactionStatusChangeListener(new TransactionStatusChangeListener() {
            public void statusChanged(int oldStatus, int newStatus) {
                if (oldStatus == Status.STATUS_PREPARED) {

                    TxThread t = new TxThread() {
                        @Override
                        public void exec() throws Exception {
                            tm.setTransactionTimeout(1);
                            tm.begin();

                            try {
                                cache1.put(new Element(1, "one#2"));
                                fail("expected TransactionTimeoutException");
                            } catch (TransactionTimeoutException e) {
                                // expected
                            }
                            tm.rollback();
                        }
                    };
                    t.start();
                    t.joinAndAssertNotFailed();
                }
            }
        });

        tm.commit();
    }

    public void testGetOldElementFromStore() throws Exception {
        Cache txCache = (Cache)cache1;

        CacheStoreHelper cacheStoreHelper = new CacheStoreHelper(txCache);
        TxCopyingCacheStore store = (TxCopyingCacheStore)cacheStoreHelper.getStore();

        Element one = new Element(1, "one");
        tm.begin();
        txCache.put(one);
        tm.commit();

        Element oneUp = new Element(1, "oneUp");
        tm.begin();
        txCache.put(oneUp);
        assertEquals(one, store.getOldElement(1));
        tm.commit();

        assertEquals(oneUp, store.getOldElement(1));
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

        public void joinAndAssertNotFailed() {
            try {
                join();
            } catch (InterruptedException e) {
                // ignore
            }
            assertNotFailed();
        }

        public void assertNotFailed() {
            if (failed) {
                throw new AssertionFailedError("TxThread failed");
            }
        }
    }
}
