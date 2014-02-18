/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.txns;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;

import org.junit.Assert;
import org.terracotta.toolkit.Toolkit;

import java.io.Serializable;

import javax.transaction.TransactionManager;

public class SimpleTx1 extends AbstractTxClient {

  public SimpleTx1(String[] args) {
    super(args);

  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    final TransactionManagerLookup lookup = new DefaultTransactionManagerLookup(getClass().getClassLoader());
    final TransactionManager txnManager = lookup.getTransactionManager();
    int commitCount = 0;
    int rollbackCount = 0;
    System.out.println(txnManager);

    SomeClass instance = new SomeClass(1);
    instance.someOtherInstance = new SomeClass(2);

    Assert.assertTrue(cache.getCacheConfiguration().isCopyOnRead());
    Assert.assertTrue(cache.getCacheConfiguration().isCopyOnWrite());

    try {
      txnManager.begin();
      cache.put(new Element("key1", "value1"));

      cache.put(new Element("someInstance", instance));

      Element element = cache.get("key1");
      System.out.println("key1:" + element.getValue());
      System.out.println("size1: " + cache.getSize());
      Assert.assertTrue("Should NOT be the same instance", instance != cache.get("someInstance").getValue());
      txnManager.commit();
      commitCount++;
    } catch (Exception e) {
      e.printStackTrace();
      txnManager.rollback();
      rollbackCount++;
    }

    instance.someValue = 3;

    try {
      txnManager.begin();
      cache.put(new Element("key1", "value2"));

      Assert.assertTrue("Should NOT be the same instance", instance != cache.get("someInstance").getValue());
      Assert.assertTrue("Should NOT reflect the post commit change!", instance.someValue != ((SomeClass)cache.get("someInstance").getValue()).someValue);

      Element element = cache.get("key1");
      System.out.println("key1:" + element.getValue());
      if (!element.getValue().equals("value2")) { throw new AssertionError("put should be visible in same thread"); }
      System.out.println("size1: " + cache.getSize());
      throw new Exception();
    } catch (AssertionError e) {
      throw new AssertionError(e);
    } catch (Exception e) {
      txnManager.rollback();
      rollbackCount++;
    }

    try {
      txnManager.begin();

      Element element = cache.get("key1");
      System.out.println("key1:" + element.getValue());
      if (!element.getValue().equals("value1")) { throw new AssertionError("should be old value"); }
      System.out.println("size1: " + cache.getSize());

      txnManager.commit();
      commitCount++;
    } catch (AssertionError e) {
      throw new AssertionError(e);
    } catch (Exception e) {
      e.printStackTrace();
      txnManager.rollback();
      rollbackCount++;
    }

    if (commitCount != 2) { throw new AssertionError("expected 2 commits got: " + commitCount); }

    if (rollbackCount != 1) { throw new AssertionError("expected 1 rollback got: " + rollbackCount); }

    try {
      txnManager.begin();

      cache.put(new Element("remove1", "removeValue"));
      System.out.println("size1: " + cache.getSize());

      txnManager.commit();
      commitCount++;
    } catch (AssertionError e) {
      throw new AssertionError(e);
    } catch (Exception e) {
      txnManager.rollback();
      rollbackCount++;
    }

    if (commitCount != 3) { throw new AssertionError("expected 3 commits got: " + commitCount); }

    if (rollbackCount != 1) { throw new AssertionError("expected 1 rollback got: " + rollbackCount); }

    try {
      txnManager.begin();

      int sizeBefore = cache.getSize();
      cache.remove("remove1");
      System.out.println("size1: " + cache.getSize());

      int sizeAfter = cache.getSize();
      if (sizeAfter >= sizeBefore) { throw new AssertionError("remove should reduce the size, expected: "
                                                              + (sizeBefore - 1) + " got: " + sizeAfter); }

      Element removedElement = cache.get("remove1");
      if (removedElement != null) { throw new AssertionError("remove1 key should not exist!"); }

      txnManager.commit();
      commitCount++;
    } catch (AssertionError e) {
      throw new AssertionError(e);
    } catch (Exception e) {
      txnManager.rollback();
      rollbackCount++;
    }

    if (commitCount != 4) { throw new AssertionError("expected 4 commits got: " + commitCount); }

    if (rollbackCount != 1) { throw new AssertionError("expected 1 rollback got: " + rollbackCount); }
    getBarrierForAllClients().await();
  }

  public static void main(String[] args) {
    new SimpleTx1(args).run();
  }

  public static final class SomeClass implements Serializable {

    public int       someValue;
    public SomeClass someOtherInstance;

    public SomeClass(final int someValue) {
      this.someValue = someValue;
    }
  }
}
