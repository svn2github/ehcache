package net.sf.ehcache.transaction.xa;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.store.XATransactionalStore;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import junit.framework.AssertionFailedError;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ludovic Orban
 */
public class BareXAResourceTest {

    private Cache cache;
    private TransactionManager tm;

    @Before
    public void setUp() throws Exception {
        cache = new Cache(new CacheConfiguration("test", 10000).transactionalMode(CacheConfiguration.TransactionalMode.XA));
        Configuration configuration = new Configuration();
        configuration.setName("test");
        configuration.addDefaultCache(new CacheConfiguration("default", 1000));
        configuration.addTransactionManagerLookup(new FactoryConfiguration().className("net.sf.ehcache.transaction.xa.DummyTransactionManagerLookup"));
        CacheManager cacheManager = new CacheManager(configuration);
        cacheManager.addCache(cache);
        tm = new DummyTransactionManagerLookup().getTransactionManager();
        tm.begin();

        // clean up cache
        Xid xid = new DummyTransactionManagerLookup.DummyXid(-1, -1);
        getXAResource().start(xid, XAResource.TMNOFLAGS);
        cache.removeAll();
        getXAResource().end(xid, XAResource.TMSUCCESS);
        getXAResource().commit(xid, true);

        // clean up in-doubt TXs
        Xid[] xids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        if (xids != null) {
            for (Xid aXid : xids) {
                getXAResource().rollback(aXid);
            }
        }
    }

    @Test
    public void testTwoPhaseCommit() throws Exception {
        final Element ELEMENT = new Element("1", "one");
        final Xid xid11 = new DummyTransactionManagerLookup.DummyXid(1, 1);
        final Xid xid21 = new DummyTransactionManagerLookup.DummyXid(2, 1);

        getXAResource().start(xid11, XAResource.TMNOFLAGS);
        cache.put(ELEMENT);
        getXAResource().end(xid11, XAResource.TMSUCCESS);

        assertEquals(XAResource.XA_OK, getXAResource().prepare(xid11));
        getXAResource().commit(xid11, false);

        getXAResource().start(xid21, XAResource.TMNOFLAGS);
        assertEquals(ELEMENT, cache.get(ELEMENT.getKey())); //"TX XID21 should see committed data of TX XID11"
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        getXAResource().rollback(xid21);

        Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
    }

    @Test
    public void testOnePhaseCommit() throws Exception {
        final Element ELEMENT = new Element("1", "one");
        final Xid xid11 = new DummyTransactionManagerLookup.DummyXid(1, 1);
        final Xid xid21 = new DummyTransactionManagerLookup.DummyXid(2, 1);

        getXAResource().start(xid11, XAResource.TMNOFLAGS);
        cache.put(ELEMENT);
        getXAResource().end(xid11, XAResource.TMSUCCESS);

        getXAResource().commit(xid11, true);

        getXAResource().start(xid21, XAResource.TMNOFLAGS);
        assertEquals(ELEMENT, cache.get(ELEMENT.getKey())); //"TX XID21 should see committed data of TX XID11"
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        getXAResource().rollback(xid21);

        Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
    }

    @Test
    public void testTMFAILRollback() throws Exception {
        final Element ELEMENT = new Element("1", "one");
        final Xid xid11 = new DummyTransactionManagerLookup.DummyXid(1, 1);
        final Xid xid21 = new DummyTransactionManagerLookup.DummyXid(2, 1);

        getXAResource().start(xid11, XAResource.TMNOFLAGS);
        cache.put(ELEMENT);
        getXAResource().end(xid11, XAResource.TMFAIL);

        getXAResource().rollback(xid11);

        getXAResource().start(xid21, XAResource.TMNOFLAGS);
        assertTrue(cache.get(ELEMENT.getKey()) == null); //"TX XID21 should not see rolled back data of TX XID11"
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        getXAResource().rollback(xid21);

        Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
    }

    @Test
    public void testTMSUCCESSRollback() throws Exception {
        final Element ELEMENT = new Element("1", "one");
        final Xid xid11 = new DummyTransactionManagerLookup.DummyXid(1, 1);
        final Xid xid21 = new DummyTransactionManagerLookup.DummyXid(2, 1);

        getXAResource().start(xid11, XAResource.TMNOFLAGS);
        cache.put(ELEMENT);
        getXAResource().end(xid11, XAResource.TMSUCCESS);

        getXAResource().rollback(xid11);

        getXAResource().start(xid21, XAResource.TMNOFLAGS);
        assertTrue(cache.get(ELEMENT.getKey()) == null); //"TX XID21 should not see rolled back data of TX XID11"
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        getXAResource().rollback(xid21);

        Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
    }

    @Test
    public void testIsolationWithNewElement() throws Exception {
        final Element ELEMENT = new Element("1", "one");
        final Xid xid11 = new DummyTransactionManagerLookup.DummyXid(1, 1);
        final Xid xid21 = new DummyTransactionManagerLookup.DummyXid(2, 1);

        getXAResource().start(xid11, XAResource.TMNOFLAGS);
        cache.put(ELEMENT);
        getXAResource().end(xid11, XAResource.TMSUCCESS);
        Transaction tx11 = tm.suspend();

        tm.begin();
        getXAResource().start(xid21, XAResource.TMNOFLAGS);
        assertTrue(cache.get(ELEMENT.getKey()) == null); //"TX XID21 should not be able to see data in progress of TX XID11 "
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        Transaction tx21 = tm.suspend();

        tm.resume(tx11);
        assertEquals(XAResource.XA_OK, getXAResource().prepare(xid11));

        Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertEquals(1, recoveredXids.length); //"there should be 1 TX in-doubt"
        assertEquals(xid11, new DummyTransactionManagerLookup.DummyXid(recoveredXids[0])); //"incorrect in-doubt TX"

        getXAResource().commit(xid11, false);

        tm.resume(tx21);
        getXAResource().start(xid21, XAResource.TMJOIN);
        assertEquals(ELEMENT, cache.get(ELEMENT.getKey())); //"TX XID21 should see committed data of TX XID11"
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        getXAResource().rollback(xid21);

        recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
    }

    @Test
    public void testIsolationWithExistingElement() throws Exception {
        final Element ELEMENT_OLD = new Element("1", "one");
        final Element ELEMENT_NEW = new Element("1", "two-divided-by-two");
        final Xid xid01 = new DummyTransactionManagerLookup.DummyXid(0, 1);
        final Xid xid11 = new DummyTransactionManagerLookup.DummyXid(1, 1);
        final Xid xid21 = new DummyTransactionManagerLookup.DummyXid(2, 1);

        // init cache
        getXAResource().start(xid01, XAResource.TMNOFLAGS);
        cache.put(ELEMENT_OLD);
        getXAResource().end(xid01, XAResource.TMSUCCESS);
        getXAResource().commit(xid01, true);


        getXAResource().start(xid11, XAResource.TMNOFLAGS);
        cache.put(ELEMENT_NEW);
        getXAResource().end(xid11, XAResource.TMSUCCESS);
        Transaction tx11 = tm.suspend();

        tm.begin();
        getXAResource().start(xid21, XAResource.TMNOFLAGS);
        assertEquals(ELEMENT_OLD, cache.get(ELEMENT_OLD.getKey())); //"TX XID21 should see old data"
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        Transaction tx21 = tm.suspend();

        tm.resume(tx11);
        assertEquals(XAResource.XA_OK, getXAResource().prepare(xid11));

        Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertEquals(1, recoveredXids.length); //"there should be 1 TX in-doubt"
        assertEquals(xid11, new DummyTransactionManagerLookup.DummyXid(recoveredXids[0])); //"incorrect in-doubt TX"

        getXAResource().commit(xid11, false);

        tm.resume(tx21);
        getXAResource().start(xid21, XAResource.TMJOIN);
        assertEquals(ELEMENT_NEW, cache.get(ELEMENT_NEW.getKey())); //"TX XID21 should see committed data of TX XID11"
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        getXAResource().rollback(xid21);

        recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
    }

    @Test
    public void testConflictWithNewElement() throws Exception {
        final Element ELEMENT1 = new Element("1", "one");
        final Element ELEMENT2 = new Element("1", "two-divided-by-two");
        final Xid xid01 = new DummyTransactionManagerLookup.DummyXid(0, 1);
        final Xid xid11 = new DummyTransactionManagerLookup.DummyXid(1, 1);
        final Xid xid21 = new DummyTransactionManagerLookup.DummyXid(2, 1);

        getXAResource().start(xid11, XAResource.TMNOFLAGS);
        cache.put(ELEMENT1);
        getXAResource().end(xid11, XAResource.TMSUCCESS);
        Transaction tx11 = tm.suspend();

        tm.begin();
        getXAResource().start(xid21, XAResource.TMNOFLAGS);
        cache.put(ELEMENT2);
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        Transaction tx21 = tm.suspend();

        tm.resume(tx11);
        assertEquals(XAResource.XA_OK, getXAResource().prepare(xid11));
        getXAResource().commit(xid11, false);

        tm.resume(tx21);
        try {
            getXAResource().prepare(xid21);
            throw new AssertionFailedError("expected XAException");
        } catch (XAException ex) {
            assertEquals("XA request failed", ex.getMessage());
            assertEquals(XAException.XA_RBINTEGRITY, ex.errorCode);
            assertEquals("Element for key <1> has changed since it was PUT in the cache and the transaction committed (currentVersion: 0)", ex
                .getCause().getMessage());
        }
        getXAResource().rollback(xid21);

        tm.begin();
        getXAResource().start(xid01, XAResource.TMNOFLAGS);
        assertEquals(ELEMENT2, cache.get(ELEMENT1.getKey())); //"TX XID01 should see committed data of TX XID21"
        getXAResource().end(xid01, XAResource.TMSUCCESS);
        getXAResource().rollback(xid21);

        Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
    }

    @Test
    public void testConflictWithExistingElement() throws Exception {
        final Element ELEMENT0 = new Element("1", "unknown-yet");
        final Element ELEMENT1 = new Element("1", "one");
        final Element ELEMENT2 = new Element("1", "two-divided-by-two");
        final Xid xid01 = new DummyTransactionManagerLookup.DummyXid(0, 1);
        final Xid xid11 = new DummyTransactionManagerLookup.DummyXid(1, 1);
        final Xid xid21 = new DummyTransactionManagerLookup.DummyXid(2, 1);

        // init cache
        getXAResource().start(xid01, XAResource.TMNOFLAGS);
        cache.put(ELEMENT0);
        getXAResource().end(xid01, XAResource.TMSUCCESS);
        getXAResource().commit(xid01, true);

        tm.begin();
        getXAResource().start(xid11, XAResource.TMNOFLAGS);
        cache.put(ELEMENT1);
        getXAResource().end(xid11, XAResource.TMSUCCESS);
        Transaction tx11 = tm.suspend();

        tm.begin();
        getXAResource().start(xid21, XAResource.TMNOFLAGS);
        cache.put(ELEMENT2);
        getXAResource().end(xid21, XAResource.TMSUCCESS);

        assertEquals(XAResource.XA_OK, getXAResource().prepare(xid21));
        getXAResource().commit(xid21, false);

        tm.resume(tx11);
        try {
            getXAResource().prepare(xid11);
            throw new AssertionFailedError("expected XAException");
        } catch (XAException ex) {
            assertEquals("XA request failed", ex.getMessage());
            assertEquals(XAException.XA_RBINTEGRITY, ex.errorCode);
            assertEquals("Element for key <1> has changed since it was PUT in the cache and the transaction committed (currentVersion: 0)", ex
                .getCause().getMessage());
        }
        getXAResource().rollback(xid11);

        tm.begin();
        getXAResource().start(xid01, XAResource.TMNOFLAGS);
        assertEquals(ELEMENT1, cache.get(ELEMENT0.getKey())); //"TX XID01 should see committed data of TX XID11"
        getXAResource().end(xid01, XAResource.TMSUCCESS);
        getXAResource().rollback(xid21);

        Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
    }

    @Test
    public void testRollbackAfterPrepare() throws Exception {
        final Element ELEMENT = new Element("1", "one");
        final Xid xid11 = new DummyTransactionManagerLookup.DummyXid(1, 1);
        final Xid xid21 = new DummyTransactionManagerLookup.DummyXid(2, 1);

        getXAResource().start(xid11, XAResource.TMNOFLAGS);
        cache.put(ELEMENT);
        getXAResource().end(xid11, XAResource.TMSUCCESS);

        assertEquals(XAResource.XA_OK, getXAResource().prepare(xid11));
        getXAResource().rollback(xid11);

        tm.begin();
        getXAResource().start(xid21, XAResource.TMNOFLAGS);
        assertTrue(cache.get(ELEMENT.getKey()) == null); //"TX XID21 should see no data"
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        getXAResource().rollback(xid21);

        Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
    }

    @Test
    public void testRollbackWithoutPrepare() throws Exception {
        final Element ELEMENT = new Element("1", "one");
        final Xid xid11 = new DummyTransactionManagerLookup.DummyXid(1, 1);
        final Xid xid21 = new DummyTransactionManagerLookup.DummyXid(2, 1);

        getXAResource().start(xid11, XAResource.TMNOFLAGS);
        cache.put(ELEMENT);
        getXAResource().end(xid11, XAResource.TMSUCCESS);

        getXAResource().rollback(xid11);

        tm.begin();
        getXAResource().start(xid21, XAResource.TMNOFLAGS);
        assertTrue(cache.get(ELEMENT.getKey()) == null); //"TX XID21 should see no data"
        getXAResource().end(xid21, XAResource.TMSUCCESS);
        getXAResource().rollback(xid21);

        Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
        assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
    }

    @Test
    public void testEnlistment() throws Exception {
        try {
            final Element ELEMENT = new Element("1", "one");
            cache.put(ELEMENT);
            throw new AssertionError("expected CacheException");
        } catch (CacheException e) {
            assertEquals("enlistment of XAResource of cache named 'test@test.cacheManager' did not end up calling XAResource.start()", e.getMessage());
        }
    }

    private XAResource getXAResource() {
        try {
            Field field = cache.getClass().getDeclaredField("compoundStore");
            field.setAccessible(true);
            XATransactionalStore store = (XATransactionalStore)field.get(cache);
            return store.getOrCreateXAResource();
        } catch (Exception e) {
            throw new RuntimeException("cannot extract XAResource out of cache " + cache, e);
        }
    }
}
