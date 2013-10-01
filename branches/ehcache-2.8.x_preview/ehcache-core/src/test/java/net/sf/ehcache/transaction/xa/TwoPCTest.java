package net.sf.ehcache.transaction.xa;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.TransactionStatusChangeListener;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.transaction.TransactionTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import javax.transaction.RollbackException;
import javax.transaction.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author lorban
 */
public class TwoPCTest {

    private CacheManager cacheManager;
    private Ehcache xaCache1;
    private Ehcache xaCache2;
    private BitronixTransactionManager transactionManager;

    @Before
    public void setUp() throws Exception {
        TransactionManagerServices.getConfiguration().setJournal("null").setServerId("SoftLockPinningTest");
        transactionManager = TransactionManagerServices.getTransactionManager();

        cacheManager = new CacheManager(new Configuration());

        cacheManager.addCache(new Cache(new CacheConfiguration().name("xaCache1")
                .maxEntriesLocalHeap(10)
                .transactionalMode(CacheConfiguration.TransactionalMode.XA_STRICT)));
        cacheManager.addCache(new Cache(new CacheConfiguration().name("xaCache2")
                .maxEntriesLocalHeap(10)
                .transactionalMode(CacheConfiguration.TransactionalMode.XA_STRICT)));

        xaCache1 = cacheManager.getEhcache("xaCache1");
        xaCache2 = cacheManager.getEhcache("xaCache2");
    }

    private void clearAll() throws Exception {
        transactionManager.begin();
        xaCache1.removeAll();
        xaCache2.removeAll();
        transactionManager.commit();
    }

    @After
    public void tearDown() throws Exception {
        if (transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION) {
            transactionManager.rollback();
        }
        transactionManager.shutdown();
        cacheManager.shutdown();
    }

    @Test
    public void testRemoveCachesAfterPhase1() throws Exception {
        clearAll();
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

        // Remove the caches between the 1st and 2nd phase of 2PC to make sure the soft locks are in the cache but won't be deserializable.
        // We must also make sure to use at least 2 XA resources to prevent the 1PC optimization to kick in.
        transactionManager.getCurrentTransaction().addTransactionStatusChangeListener(new TransactionStatusChangeListener() {
            public void statusChanged(final int oldStatus, final int newStatus) {
                if (oldStatus == Status.STATUS_PREPARED) {
                    cacheManager.removeCache("xaCache1");
                    cacheManager.removeCache("xaCache2");
                }
            }
        });

        // the TM should be able to commit without problem, the test should just pass.
        transactionManager.commit();
    }

    @Test
    public void testTimeout() throws Exception {
        xaCache1.getCacheManager().getTransactionController().setDefaultTransactionTimeout(5);

        transactionManager.setTransactionTimeout(2);
        transactionManager.begin();

        // get doesn't enlist -> XATransactionStore will set this cache's XA timeout for this TX to be the default local TX timeout
        xaCache1.get(1);

        Thread.sleep(3000);
        // XA tx timed out but the resource doesn't know about it -> call succeeds
        xaCache1.get(1);

        Thread.sleep(3000);
        // cache's XA timeout for this TX reached -> fail
        try {
            xaCache1.get(1);
            fail("expected TransactionTimeoutException");
        } catch (TransactionTimeoutException e) {
            // expected
        }

        try {
            // this XA tx timed out
            transactionManager.commit();
            fail("expected RollbackException");
        } catch (RollbackException e) {
            // expected
        }

        // check that there is no internal leak (EHC-937)
        assertEquals(0, ((Map)getStoreField(xaCache1, "transactionToTimeoutMap")).size());
    }

    private static Object getStoreField(Ehcache cache, String storeFieldName) throws IllegalAccessException, NoSuchFieldException {
        Field storeField = cache.getClass().getDeclaredField("compoundStore");
        storeField.setAccessible(true);
        XATransactionStore store = (XATransactionStore)storeField.get(cache);

        Field field = store.getClass().getDeclaredField(storeFieldName);
        field.setAccessible(true);
        return field.get(store);
    }

}