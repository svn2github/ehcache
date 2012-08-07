/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.txns;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;

import org.terracotta.toolkit.Toolkit;

import javax.transaction.TransactionManager;

public class TwoResourceTx2 extends AbstractTxClient {

  public TwoResourceTx2(String[] args) {
    super(args);
  }

  @Override
  protected void runTest(Cache cache, Toolkit toolkit) throws Throwable {
    Cache cache2 = getCacheManager().getCache("test2");
    final TransactionManagerLookup lookup = new DefaultTransactionManagerLookup();

    final TransactionManager txnManager = lookup.getTransactionManager();

    getBarrierForAllClients().await();

    try {
      txnManager.begin();
      Element oldElement = cache.get("key1");
      if (!"value1".equals(oldElement.getValue())) { throw new AssertionError("Should have been put by Client 1"); }
      Element removedElement = cache.get("remove1");

      if (removedElement != null) { throw new AssertionError("remove1 key should not exist!"); }

      cache.put(new Element("key1", "value2"));
      cache2.put(new Element("key1", "value1"));

      System.out.println("\nReading entry");

      txnManager.commit();

      txnManager.begin();
      cache2.put(new Element("key1", "value1"));

      System.out.println("Value is: " + cache.get("key1").getValue());
      System.out.println("Size of is: " + cache.getSize());
      txnManager.commit();

    } catch (Exception e) {
      e.printStackTrace();
      txnManager.rollback();
    }
  }

  public static void main(String[] args) {
    new TwoResourceTx2(args).run();
  }
}
