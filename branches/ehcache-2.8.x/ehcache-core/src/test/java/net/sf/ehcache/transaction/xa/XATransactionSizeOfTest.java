package net.sf.ehcache.transaction.xa;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.CopyStrategyConfiguration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.store.compound.SerializationCopyStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author lorban
 */
public class XATransactionSizeOfTest {

    private BitronixTransactionManager transactionManager;
    private CacheManager cacheManager;
    private Ehcache cache1;

    @Before
    public void setUp() throws Exception {
        CacheConfiguration txCache1Cfg = new CacheConfiguration().name("txCache1")
            .transactionalMode(CacheConfiguration.TransactionalMode.XA_STRICT)
            .sizeOfPolicy(new SizeOfPolicyConfiguration().maxDepth(13)
                .maxDepthExceededBehavior(SizeOfPolicyConfiguration.MaxDepthExceededBehavior.ABORT));
        CopyStrategyConfiguration copyStrategyConfiguration = new CopyStrategyConfiguration();
        copyStrategyConfiguration.setClass(SerializationCopyStrategy.class.getName());
        txCache1Cfg.addCopyStrategy(copyStrategyConfiguration);
        Configuration configuration = new Configuration().maxBytesLocalHeap(10, MemoryUnit.MEGABYTES)
            .cache(txCache1Cfg);

        TransactionManagerServices.getConfiguration().setJournal("null").setServerId(XATransactionSizeOfTest.class.getSimpleName());
        transactionManager = TransactionManagerServices.getTransactionManager();

        cacheManager = new CacheManager(configuration);
        transactionManager.begin();
        cache1 = cacheManager.getEhcache("txCache1");
        cache1.removeAll();
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

    /**
     * Make sure the sizeof engine detects that there are too many objects to be walked.
     */
    @Test
    public void testHasAbortedSizeOf() throws Exception {
        transactionManager.begin();

        List<String> strings = new ArrayList<String>();
        for (int i = 0; i < 100; i++) {
            strings.add("" + i);
        }
        cache1.put(new Element(0, strings));

        transactionManager.commit();
        assertTrue(cache1.hasAbortedSizeOf());
    }

    /**
     * Make sure XA doesn't store stuff that makes the sizeof engine mistakenly walk the whole heap.
     *
     * Here's what is walked when storing elements with integers both as key and value:
     *
     *   40b		net.sf.ehcache.store.chm.SelectableConcurrentHashMap$HashEntry@1205901244
     *   80b		net.sf.ehcache.Element@1942996580
     *   32b		net.sf.ehcache.transaction.SoftLockID@1216216770
     *   80b		net.sf.ehcache.Element@350784291
     *   24b		java.lang.Integer@1618147776
     *   ignored	java.lang.Integer@1897411861
     *   24b		net.sf.ehcache.transaction.xa.XidTransactionIDImpl@738355611
     *   32b		java.lang.String@739090040
     *   32b		[C@840888032
     *   24b		net.sf.ehcache.transaction.xa.SerializableXid@215272917
     *   56b		[B@750131952
     *   56b		[B@1738709374
     *
     * That's 12 objects, so make sure SizeOfPolicyConfiguration's maxDepth is >= 13.
     */
    @Test
    public void testSizeOf() throws Exception {
        transactionManager.begin();

        for (int i = 0; i < 100; i++) {
            cache1.put(new Element(i, i));
        }

        transactionManager.commit();
        assertFalse(cache1.hasAbortedSizeOf());
    }

}
