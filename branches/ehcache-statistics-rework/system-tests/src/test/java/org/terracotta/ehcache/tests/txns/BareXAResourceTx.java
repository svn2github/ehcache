/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.txns;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.xa.DummyTransactionManagerLookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.ehcache.tests.xa.DummyXid;
import org.terracotta.ehcache.tests.xa.EhCacheXAResourceExtractor;
import org.terracotta.toolkit.Toolkit;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;

public class BareXAResourceTx extends AbstractTxClient {

  private static final Logger LOG = LoggerFactory.getLogger(BareXAResourceTx.class);

  public BareXAResourceTx(String[] args) {
    super(args);
  }

  private TransactionManager tm;
  private Cache              cache;

  @Override
  protected void runTest(Cache theCache, Toolkit toolkit) throws Throwable {
    tm = new DummyTransactionManagerLookup().getTransactionManager();
    this.cache = theCache;

    try {
      setUp();
      LOG.info("running testTwoPhaseCommit");
      testTwoPhaseCommit();

      setUp();
      LOG.info("running testOnePhaseCommit");
      testOnePhaseCommit();

      setUp();
      LOG.info("running testTMFAILRollback");
      testTMFAILRollback();

      setUp();
      LOG.info("running testTMSUCCESSRollback");
      testTMSUCCESSRollback();

      setUp();
      LOG.info("running testIsolationWithNewElement");
      testIsolationWithNewElement();

      setUp();
      LOG.info("running testIsolationWithExistingElement");
      testIsolationWithExistingElement();

      setUp();
      LOG.info("running testConflictWithNewElement");
      testConflictWithNewElement();

      setUp();
      LOG.info("running testConflictWithExistingElement");
      testConflictWithExistingElement();

      setUp();
      LOG.info("running testRollbackAfterPrepare");
      testRollbackAfterPrepare();

      setUp();
      LOG.info("running testRollbackWithoutPrepare");
      testRollbackWithoutPrepare();

      setUp();
      LOG.info("running testEnlistment");
      testEnlistment();
    } finally {
      LOG.info("done running tests");
    }
  }

  private void setUp() throws Exception {
    tm.begin();

    // clean up cache
    Xid xid = new DummyXid(-1, -1);
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

  public void testTwoPhaseCommit() throws Exception {
    final Element ELEMENT = new Element("1", "one");
    final Xid xid11 = new DummyXid(1, 1);
    final Xid xid21 = new DummyXid(2, 1);

    getXAResource().start(xid11, XAResource.TMNOFLAGS);
    cache.put(ELEMENT);
    getXAResource().end(xid11, XAResource.TMSUCCESS);

    Assert.assertEquals(XAResource.XA_OK, getXAResource().prepare(xid11));
    getXAResource().commit(xid11, false);

    getXAResource().start(xid21, XAResource.TMNOFLAGS);
    Assert.assertEquals(ELEMENT, cache.get(ELEMENT.getKey())); // "TX XID21 should see committed data of TX XID11"
    getXAResource().end(xid21, XAResource.TMSUCCESS);
    getXAResource().rollback(xid21);

    Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    Assert.assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
  }

  public void testOnePhaseCommit() throws Exception {
    final Element ELEMENT = new Element("1", "one");
    final Xid xid11 = new DummyXid(1, 1);
    final Xid xid21 = new DummyXid(2, 1);

    getXAResource().start(xid11, XAResource.TMNOFLAGS);
    cache.put(ELEMENT);
    getXAResource().end(xid11, XAResource.TMSUCCESS);

    getXAResource().commit(xid11, true);

    getXAResource().start(xid21, XAResource.TMNOFLAGS);
    Assert.assertEquals(ELEMENT, cache.get(ELEMENT.getKey())); // "TX XID21 should see committed data of TX XID11"
    getXAResource().end(xid21, XAResource.TMSUCCESS);
    getXAResource().rollback(xid21);

    Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    Assert.assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
  }

  public void testTMFAILRollback() throws Exception {
    final Element ELEMENT = new Element("1", "one");
    final Xid xid11 = new DummyXid(1, 1);
    final Xid xid21 = new DummyXid(2, 1);

    getXAResource().start(xid11, XAResource.TMNOFLAGS);
    cache.put(ELEMENT);
    getXAResource().end(xid11, XAResource.TMFAIL);

    getXAResource().rollback(xid11);

    getXAResource().start(xid21, XAResource.TMNOFLAGS);
    Assert.assertTrue(cache.get(ELEMENT.getKey()) == null); // "TX XID21 should not see rolled back data of TX XID11"
    getXAResource().end(xid21, XAResource.TMSUCCESS);
    getXAResource().rollback(xid21);

    Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    Assert.assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
  }

  public void testTMSUCCESSRollback() throws Exception {
    final Element ELEMENT = new Element("1", "one");
    final Xid xid11 = new DummyXid(1, 1);
    final Xid xid21 = new DummyXid(2, 1);

    getXAResource().start(xid11, XAResource.TMNOFLAGS);
    cache.put(ELEMENT);
    getXAResource().end(xid11, XAResource.TMSUCCESS);

    getXAResource().rollback(xid11);

    getXAResource().start(xid21, XAResource.TMNOFLAGS);
    Assert.assertTrue(cache.get(ELEMENT.getKey()) == null); // "TX XID21 should not see rolled back data of TX XID11"
    getXAResource().end(xid21, XAResource.TMSUCCESS);
    getXAResource().rollback(xid21);

    Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    Assert.assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
  }

  public void testIsolationWithNewElement() throws Exception {
    final Element ELEMENT = new Element("1", "one");
    final Xid xid11 = new DummyXid(1, 1);
    final Xid xid21 = new DummyXid(2, 1);

    getXAResource().start(xid11, XAResource.TMNOFLAGS);
    cache.put(ELEMENT);
    getXAResource().end(xid11, XAResource.TMSUCCESS);
    Transaction tx11 = tm.suspend();

    tm.begin();
    getXAResource().start(xid21, XAResource.TMNOFLAGS);
    Assert.assertTrue(cache.get(ELEMENT.getKey()) == null); // "TX XID21 should not be able to see data in progress of TX XID11 "
    getXAResource().end(xid21, XAResource.TMSUCCESS);
    Transaction tx21 = tm.suspend();

    tm.resume(tx11);
    Assert.assertEquals(XAResource.XA_OK, getXAResource().prepare(xid11));

    getXAResource().commit(xid11, false);

    tm.resume(tx21);
    getXAResource().start(xid21, XAResource.TMJOIN);
    Assert.assertEquals(ELEMENT, cache.get(ELEMENT.getKey())); // "TX XID21 should see committed data of TX XID11"
    getXAResource().end(xid21, XAResource.TMSUCCESS);
    getXAResource().rollback(xid21);

    Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    Assert.assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
  }

  public void testIsolationWithExistingElement() throws Exception {
    final Element ELEMENT_OLD = new Element("1", "one");
    final Element ELEMENT_NEW = new Element("1", "two-divided-by-two");
    final Xid xid01 = new DummyXid(0, 1);
    final Xid xid11 = new DummyXid(1, 1);
    final Xid xid21 = new DummyXid(2, 1);

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
    Assert.assertEquals(ELEMENT_OLD, cache.get(ELEMENT_OLD.getKey())); // "TX XID21 should see old data"
    getXAResource().end(xid21, XAResource.TMSUCCESS);
    Transaction tx21 = tm.suspend();

    tm.resume(tx11);
    Assert.assertEquals(XAResource.XA_OK, getXAResource().prepare(xid11));

    getXAResource().commit(xid11, false);

    tm.resume(tx21);
    getXAResource().start(xid21, XAResource.TMJOIN);
    Assert.assertEquals(ELEMENT_NEW, cache.get(ELEMENT_NEW.getKey())); // "TX XID21 should see committed data of TX XID11"
    getXAResource().end(xid21, XAResource.TMSUCCESS);
    getXAResource().rollback(xid21);

    Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    Assert.assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
  }

  public void testConflictWithNewElement() throws Exception {
    final Element ELEMENT1 = new Element("1", "one");
    final Element ELEMENT2 = new Element("1", "two-divided-by-two");
    final Xid xid01 = new DummyXid(0, 1);
    final Xid xid11 = new DummyXid(1, 1);
    final Xid xid21 = new DummyXid(2, 1);

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
    Assert.assertEquals(XAResource.XA_OK, getXAResource().prepare(xid11));
    getXAResource().commit(xid11, false);

    tm.resume(tx21);
    try {
      getXAResource().prepare(xid21);
      throw new AssertionFailedError("expected XAException");
    } catch (XAException ex) {
      Assert.assertEquals(XAException.XA_RBINTEGRITY, ex.errorCode);
    }
    getXAResource().rollback(xid21);

    tm.begin();
    getXAResource().start(xid01, XAResource.TMNOFLAGS);
    Assert.assertEquals(ELEMENT2, cache.get(ELEMENT1.getKey())); // "TX XID01 should see committed data of TX XID21"
    getXAResource().end(xid01, XAResource.TMSUCCESS);
    getXAResource().rollback(xid21);

    Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    Assert.assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
  }

  public void testConflictWithExistingElement() throws Exception {
    final Element ELEMENT0 = new Element("1", "unknown-yet");
    final Element ELEMENT1 = new Element("1", "one");
    final Element ELEMENT2 = new Element("1", "two-divided-by-two");
    final Xid xid01 = new DummyXid(0, 1);
    final Xid xid11 = new DummyXid(1, 1);
    final Xid xid21 = new DummyXid(2, 1);

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

    Assert.assertEquals(XAResource.XA_OK, getXAResource().prepare(xid21));
    getXAResource().commit(xid21, false);

    tm.resume(tx11);
    try {
      getXAResource().prepare(xid11);
      throw new AssertionFailedError("expected XAException");
    } catch (XAException ex) {
      Assert.assertEquals(XAException.XA_RBINTEGRITY, ex.errorCode);
    }
    getXAResource().rollback(xid11);

    tm.begin();
    getXAResource().start(xid01, XAResource.TMNOFLAGS);
    Assert.assertEquals(ELEMENT1, cache.get(ELEMENT0.getKey())); // "TX XID01 should see committed data of TX XID11"
    getXAResource().end(xid01, XAResource.TMSUCCESS);
    getXAResource().rollback(xid21);

    Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    Assert.assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
  }

  public void testRollbackAfterPrepare() throws Exception {
    final Element ELEMENT = new Element("1", "one");
    final Xid xid11 = new DummyXid(1, 1);
    final Xid xid21 = new DummyXid(2, 1);

    getXAResource().start(xid11, XAResource.TMNOFLAGS);
    cache.put(ELEMENT);
    getXAResource().end(xid11, XAResource.TMSUCCESS);

    Assert.assertEquals(XAResource.XA_OK, getXAResource().prepare(xid11));
    getXAResource().rollback(xid11);

    tm.begin();
    getXAResource().start(xid21, XAResource.TMNOFLAGS);
    Assert.assertNull(cache.get(ELEMENT.getKey())); // "TX XID21 should see no data"
    getXAResource().end(xid21, XAResource.TMSUCCESS);
    getXAResource().rollback(xid21);

    Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    Assert.assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
  }

  public void testRollbackWithoutPrepare() throws Exception {
    final Element ELEMENT = new Element("1", "one");
    final Xid xid11 = new DummyXid(1, 1);
    final Xid xid21 = new DummyXid(2, 1);

    getXAResource().start(xid11, XAResource.TMNOFLAGS);
    cache.put(ELEMENT);
    getXAResource().end(xid11, XAResource.TMSUCCESS);

    getXAResource().rollback(xid11);

    tm.begin();
    getXAResource().start(xid21, XAResource.TMNOFLAGS);
    Assert.assertTrue(cache.get(ELEMENT.getKey()) == null); // "TX XID21 should see no data"
    getXAResource().end(xid21, XAResource.TMSUCCESS);
    getXAResource().rollback(xid21);

    Xid[] recoveredXids = getXAResource().recover(XAResource.TMSTARTRSCAN | XAResource.TMENDRSCAN);
    Assert.assertTrue("there should be 0 in-doubt TX", recoveredXids == null || 0 == recoveredXids.length);
  }

  public void testEnlistment() throws Exception {
    try {
      final Element ELEMENT = new Element("1", "one");
      cache.put(ELEMENT);
      throw new AssertionError("expected CacheException");
    } catch (CacheException e) {
      // expected
    }
  }

  private XAResource getXAResource() {
    return EhCacheXAResourceExtractor.extractXAResource(cache);
  }

  public static void main(String[] args) {
    new BareXAResourceTx(args).run();
  }

}
