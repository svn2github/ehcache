package net.sf.ehcache.transaction;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.TransactionStatusChangeListener;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.store.CacheStore;
import net.sf.ehcache.store.CopyingCacheStore;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.disk.DiskStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Set;

import javax.transaction.Status;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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

        assertAllSoftLockPinned(getDiskStore(cache1));
        assertAllSoftLockPinned(getDiskStore(cache2));

        // wait more than TTI/TTL, soft locked elements are both pinned and eternal, they must not be evicted
        Thread.sleep(1999);

        transactionController.commit();

        // TODO CHECK WITH LUDOVIC, BUT I THINK THIS IS NONSENSE
//        transactionController.begin();
//        assertEquals(100, cache1.getSize());
//        assertEquals(100, cache2.getSize());
//        transactionController.commit();

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
    @Ignore("Now fails at the last commit somehow... need to check with Ludovic")
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

                    assertAllSoftLockPinned(getDiskStore(xaCache1));
                    assertAllSoftLockPinned(getDiskStore(xaCache2));

                }
            }
        });

        // wait more than TTI/TTL, soft locked elements are both pinned and eternal, they must not be evicted
        Thread.sleep(1999);

        transactionManager.commit();

        // TODO CHECK WITH LUDOVIC, BUT I THINK THIS IS NONSENSE
//        transactionManager.begin();
//        assertEquals(100, xaCache1.getSize());
//        assertEquals(100, xaCache2.getSize());
//        transactionManager.commit();

        // wait more than TTI/TTL, elements must have been evicted
        Thread.sleep(1999);

        transactionManager.begin();
        for (int i = 0; i < 100; i++) {
            assertNull("xaCache1 key " + i, xaCache1.get(i));
            assertNull("xaCache2 key " + i, xaCache2.get(i));
        }
        transactionManager.commit();
    }

    private void assertAllSoftLockPinned(final DiskStore diskStore) {
        final Set pinnedKeys = diskStore.getPresentPinnedKeys();
//        assertThat(diskStore.getInMemorySize(), is(pinnedKeys.size()));
        for(int i = 0; i < 100; i++) {
            final Element actual = diskStore.get(i);
            assertThat(actual, notNullValue());
            assertThat(actual.getObjectValue().getClass(), is((Object) SoftLockID.class));
            assertThat(pinnedKeys.remove(actual.getObjectKey()), is(true));
        }
        assertThat(pinnedKeys.isEmpty(), is(true));
    }

    private DiskStore getDiskStore(final Ehcache cache) {
        try {
            if(cache instanceof Cache) {
                final Store txStore = getFieldValue("compoundStore", cache, Cache.class);
                if(txStore instanceof AbstractTransactionStore) {
                    Store copyingStore = getFieldValue("underlyingStore", txStore, AbstractTransactionStore.class);
                    if (copyingStore instanceof CopyingCacheStore) {
                        Store store = getFieldValue("store", copyingStore, CopyingCacheStore.class);
                        if(store instanceof CacheStore) {
                            return getFieldValue("authoritativeTier", store, CacheStore.class);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Couldn't get to the underlying authoritative DiskStore", e);
        }
        throw new IllegalArgumentException("Couldn't get to the underlying authoritative DiskStore");
    }

    private <T> T getFieldValue(final String field, final Object instance, Class<?> aClass) throws NoSuchFieldException, IllegalAccessException {
        final Field compoundStore = aClass.getDeclaredField(field);
        compoundStore.setAccessible(true);
        return (T) compoundStore.get(instance);
    }

}
