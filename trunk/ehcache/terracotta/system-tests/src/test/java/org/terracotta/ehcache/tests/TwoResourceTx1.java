/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;

import org.terracotta.api.ClusteringToolkit;
import org.terracotta.coordination.Barrier;

import javax.transaction.TransactionManager;

public class TwoResourceTx1 extends AbstractTxClient {
  
  public TwoResourceTx1(String[] args) {
    super(args);
    
  }
  

  @Override
  protected void test(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    Cache cache2 = getCacheManager().getCache("test2");
    final TransactionManagerLookup lookup = new DefaultTransactionManagerLookup(); 
    final TransactionManager txnManager = lookup.getTransactionManager();
    final Barrier barrier = toolkit.getBarrier(BARRIER_NAME, 2);
    int commitCount = 0;
    int rollbackCount = 0;
    System.out.println(txnManager);
        
    try {
      txnManager.begin();
      cache.put(new Element("key1", "value1"));
       
      cache2.put(new Element("key1", "value1"));

      Element element = cache.get("key1");
      System.out.println("key1:" + element.getValue());
      System.out.println("size1: " + cache.getSize());
      txnManager.commit();
      commitCount++;
    } catch (Exception e) {
      e.printStackTrace();
      txnManager.rollback();
      rollbackCount++;
    }
    
    try {
      txnManager.begin();
      cache.put(new Element("key1", "value2"));
       
      cache2.put(new Element("key1", "value1"));
   
      Element element = cache.get("key1");
      System.out.println("key1:" + element.getValue());
      if(!element.getValue().equals("value2")) {
        throw new AssertionError("put should be visible in same thread");
      }
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

      cache2.put(new Element("key1", "value1"));

      Element element = cache.get("key1");
      System.out.println("key1:" + element.getValue());
      if(!element.getValue().equals("value1")) {
        throw new AssertionError("should be old value");
      }
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
    
    if(commitCount != 2) {
      throw new AssertionError("expected 2 commits got: " + commitCount);
    }
    
    if(rollbackCount != 1) {
      throw new AssertionError("expected 1 rollback got: " + rollbackCount);
    }
    
    
    try {
      txnManager.begin();
      
      cache.put(new Element("remove1", "removeValue"));
      System.out.println("size1: " + cache.getSize());
      cache2.put(new Element("key1", "value1"));
      
      txnManager.commit();
      commitCount++;
    } catch (AssertionError e) {
        throw new AssertionError(e);
    } catch (Exception e) {
      txnManager.rollback();
      rollbackCount++;
    }
    
    if(commitCount != 3) {
      throw new AssertionError("expected 3 commits got: " + commitCount);
    }
    
    if(rollbackCount != 1) {
      throw new AssertionError("expected 1 rollback got: " + rollbackCount);
    }

    try {
      txnManager.begin();
      
      int sizeBefore = cache.getSize();
      cache.remove("remove1");
      System.out.println("size1: " + cache.getSize());
      cache2.put(new Element("key1", "value1"));
      
      int sizeAfter = cache.getSize();
      if(sizeAfter >= sizeBefore) {
        throw new AssertionError("remove should reduce the size, expected: " + (sizeBefore - 1) + " got: " + sizeAfter);
      }
      
      Element removedElement =  cache.get("remove1");
      if(removedElement !=  null) {
        throw new AssertionError("remove1 key should not exist!");
      }
      
      
      txnManager.commit();
      commitCount++;
    } catch (AssertionError e) {
        throw new AssertionError(e);
    } catch (Exception e) {
      txnManager.rollback();
      rollbackCount++;
    }
    
    if(commitCount != 4) {
      throw new AssertionError("expected 4 commits got: " + commitCount);
    }
    
    if(rollbackCount != 1) {
      throw new AssertionError("expected 1 rollback got: " + rollbackCount);
    }
    barrier.await();
  }
  
  public static void main(String[] args) {
    new TwoResourceTx1(args).run();
  }
}
