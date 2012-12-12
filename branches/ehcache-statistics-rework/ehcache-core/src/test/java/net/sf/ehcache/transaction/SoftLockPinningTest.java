package net.sf.ehcache.transaction;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.TransactionStatusChangeListener;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.util.RetryAssert;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.transaction.Status;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * @author lorban
 */
public class SoftLockPinningTest {

    private CacheManager cacheManager;
    private Ehcache cache1;
    private Ehcache cache2;
    private Ehcache xaCache1;
    private Ehcache xaCache2;
    private TransactionController transactionController;
    private BitronixTransactionManager transactionManager;

    @Before
    public void setUp() throws Exception {
        TransactionManagerServices.getConfiguration().setJournal("null").setServerId("SoftLockPinningTest");
        transactionManager = TransactionManagerServices.getTransactionManager();

        cacheManager = new CacheManager(new Configuration().diskStore(new DiskStoreConfiguration().path(System.getProperty("java.io.tmpdir")))
            .cache(new CacheConfiguration().name("localTxCache1")
                .maxEntriesLocalHeap(1)
                .overflowToDisk(true)
                .transactionalMode(CacheConfiguration.TransactionalMode.LOCAL)
            )
            .cache(new CacheConfiguration().name("localTxCache2")
                .maxEntriesLocalHeap(1)
                .overflowToDisk(true)
                .transactionalMode(CacheConfiguration.TransactionalMode.LOCAL)
            )
            .cache(new CacheConfiguration().name("xaCache1")
                .maxEntriesLocalHeap(1)
                .overflowToDisk(true)
                .transactionalMode(CacheConfiguration.TransactionalMode.XA_STRICT)
            )
            .cache(new CacheConfiguration().name("xaCache2")
                .maxEntriesLocalHeap(1)
                .overflowToDisk(true)
                .transactionalMode(CacheConfiguration.TransactionalMode.XA_STRICT)
            )
        );

        transactionController = cacheManager.getTransactionController();
        transactionController.begin();
        cache1 = cacheManager.getEhcache("localTxCache1");
        cache1.removeAll();
        cache2 = cacheManager.getEhcache("localTxCache2");
        cache2.removeAll();
        transactionController.commit();

        transactionManager.begin();
        xaCache1 = cacheManager.getEhcache("xaCache1");
        xaCache1.removeAll();
        xaCache2 = cacheManager.getEhcache("xaCache2");
        xaCache2.removeAll();
        transactionManager.commit();
    }

    @After
    public void tearDown() throws Exception {
        if (transactionController.getCurrentTransactionContext() != null) {
            transactionController.rollback();
        }
        if (transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
            transactionManager.rollback();
        }
        transactionManager.shutdown();
        cacheManager.shutdown();
    }

    @Test
    public void testDiskBackedCacheLocalTx() throws Exception {
        transactionController.begin();

        for (int i = 0; i < 100; i++) {
            Element element1 = new Element(i, i);
            element1.setTimeToIdle(1);
            element1.setTimeToLive(1);
            cache1.put(element1);

            Element element2 = new Element(i, i);
            element2.setTimeToIdle(1);
            element2.setTimeToLive(1);
            cache2.put(element2);
        }

        assertEquals(100, cache1.getStatistics().getMemoryStoreSize());
        assertEquals(100, cache2.getStatistics().getMemoryStoreSize());
        RetryAssert.assertBy(5, TimeUnit.SECONDS, new Callable<Integer>() {
            public Integer call() throws Exception {
                return (int) cache1.getStatistics().getDiskStoreSize();
            }
        }, Is.is(100));
        RetryAssert.assertBy(5, TimeUnit.SECONDS, new Callable<Integer>() {
            public Integer call() throws Exception {
                return (int) cache2.getStatistics().getDiskStoreSize();
            }
        }, Is.is(100));

        // wait more than TTI/TTL, soft locked elements are both pinned and eternal, they must not be evicted
        Thread.sleep(1999);

        transactionController.commit();

        transactionController.begin();
        assertEquals(100, cache1.getSize());
        assertEquals(100, cache2.getSize());
        transactionController.commit();

        // wait more than TTI/TTL, elements must have been evicted
        Thread.sleep(1999);

        transactionController.begin();
        for (int i = 0; i < 100; i++) {
            assertNull("cache1 key " + i, cache1.get(i));
            assertNull("cache2 key " + i, cache2.get(i));
        }
        transactionController.commit();
    }

    @Test
    public void testDiskBackedCacheXaStrictTx() throws Exception {
        transactionManager.begin();

        for (int i = 0; i < 100; i++) {
            Element element1 = new Element(i, i);
            element1.setTimeToIdle(1);
            element1.setTimeToLive(1);
            xaCache1.put(element1);

            Element element2 = new Element(i, i);
            element2.setTimeToIdle(1);
            element2.setTimeToLive(1);
            xaCache2.put(element2);
        }

        // The soft locked elements are put into the underlying store during prepare -> assertions have to occur between phase 1 and 2 of 2PC
        // Fortunately, there are some good JTA implementations out there (*wink*, *wink*) that do have useful non-standard extensions which
        // make this check easy to implement. We must also make sure to use at least 2 XA resources to prevent the 1PC optimization to kick in.
        transactionManager.getCurrentTransaction().addTransactionStatusChangeListener(new TransactionStatusChangeListener() {
            public void statusChanged(final int oldStatus, final int newStatus) {
                if (oldStatus == Status.STATUS_PREPARED) {

                    assertEquals(100, xaCache1.getStatistics().getMemoryStoreSize());
                    assertEquals(100, xaCache2.getStatistics().getMemoryStoreSize());
                    RetryAssert.assertBy(5, TimeUnit.SECONDS, new Callable<Integer>() {
                        public Integer call() throws Exception {
                            return (int) xaCache1.getStatistics().getDiskStoreSize();
                        }
                    }, Is.is(100));
                    RetryAssert.assertBy(5, TimeUnit.SECONDS, new Callable<Integer>() {
                        public Integer call() throws Exception {
                            return (int) xaCache2.getStatistics().getDiskStoreSize();
                        }
                    }, Is.is(100));

                }
            }
        });

        // wait more than TTI/TTL, soft locked elements are both pinned and eternal, they must not be evicted
        Thread.sleep(1999);

        transactionManager.commit();

        transactionManager.begin();
        assertEquals(100, xaCache1.getSize());
        assertEquals(100, xaCache2.getSize());
        transactionManager.commit();

        // wait more than TTI/TTL, elements must have been evicted
        Thread.sleep(1999);

        transactionManager.begin();
        for (int i = 0; i < 100; i++) {
            assertNull("xaCache1 key " + i, xaCache1.get(i));
            assertNull("xaCache2 key " + i, xaCache2.get(i));
        }
        transactionManager.commit();
    }

}
