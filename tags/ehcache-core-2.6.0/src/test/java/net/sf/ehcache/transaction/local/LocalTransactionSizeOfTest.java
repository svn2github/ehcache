package net.sf.ehcache.transaction.local;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.TransactionController;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author lorban
 */
public class LocalTransactionSizeOfTest {

    private CacheManager cacheManager;
    private Ehcache cache1;
    private TransactionController transactionController;

    @Before
    public void setUp() throws Exception {
        CacheConfiguration txCache1Cfg = new CacheConfiguration().name("txCache1")
            .transactionalMode(CacheConfiguration.TransactionalMode.LOCAL)
            .sizeOfPolicy(new SizeOfPolicyConfiguration().maxDepth(10)
                .maxDepthExceededBehavior(SizeOfPolicyConfiguration.MaxDepthExceededBehavior.ABORT));
        CopyStrategyConfiguration copyStrategyConfiguration = new CopyStrategyConfiguration();
        copyStrategyConfiguration.setClass(SerializationCopyStrategy.class.getName());
        txCache1Cfg.addCopyStrategy(copyStrategyConfiguration);
        Configuration configuration = new Configuration().maxBytesLocalHeap(10, MemoryUnit.MEGABYTES)
            .cache(txCache1Cfg);

        cacheManager = new CacheManager(configuration);
        transactionController = cacheManager.getTransactionController();
        transactionController.begin();
        cache1 = cacheManager.getEhcache("txCache1");
        cache1.removeAll();
        transactionController.commit();
    }

    @After
    public void tearDown() throws Exception {
        if (transactionController.getCurrentTransactionContext() != null) {
            transactionController.rollback();
        }
        cacheManager.shutdown();
    }

    @Test
    public void testHasAbortedSizeOf() {
        transactionController.begin();

        List<String> strings = new ArrayList<String>();
        for (int i = 0; i < 100; i++) {
            strings.add("" + i);
        }
        cache1.put(new Element(0, strings));

        transactionController.commit();
        assertTrue(cache1.hasAbortedSizeOf());
    }

    @Test
    public void testSizeOf() throws Exception {
        transactionController.begin();

        for (int i = 0; i < 100; i++) {
            cache1.put(new Element(i, i));
        }

        transactionController.commit();
        assertFalse(cache1.hasAbortedSizeOf());
    }

}
