/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;

import org.terracotta.api.ClusteringToolkit;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * NOTE: Looks like Atomikos does not support suspend/resume. 
 * 
 * 
 */
public class SuspendResumeClient extends AbstractTxClient {

  public SuspendResumeClient(String[] args) {
    super(args);
  }
  


  @Override
  protected void runTest(Cache cache, ClusteringToolkit toolkit) throws Throwable {
    final TransactionManagerLookup lookup = new DefaultTransactionManagerLookup(); 
    final TransactionManager txnManager = lookup.getTransactionManager();
    int commitCount = 0;
    int rollbackCount = 0;
    System.out.println(txnManager);
    try {
      txnManager.begin();
      cache.put(new Element("key1", "value1"));
       
      Transaction tx = txnManager.suspend();
      
      txnManager.resume(tx);
   
      Element element = cache.get("key1");
      
      if(!element.getValue().equals("value1")) {
        throw new AssertionError("put should be visible in same thread");
      }
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
      txnManager.rollback();
      rollbackCount++;
    }
    
    if(commitCount != 2) {
      throw new AssertionError("expected 2 commits got: " + commitCount);
    }
    
    if(rollbackCount != 1) {
      throw new AssertionError("expected 1 rollback got: " + rollbackCount);
    }
    
    
    txnManager.begin();

    cache.put(new Element("1", "one"));

    Transaction tx1 = txnManager.suspend();

    txnManager.begin();
    
    cache.put(new Element("2", "two"));
    
    txnManager.commit();

    txnManager.resume(tx1);

    if (cache.get("2").getValue().equals("two")) {
      cache.put(new Element("1-2", "one-two"));
    }
    txnManager.commit();
    
    
    
    //check
    txnManager.begin();
    
    Element element = cache.get("1-2");
    
    if(element == null) {
      throw new AssertionError("should contain key 1-2");
    }
    
    txnManager.commit();
    
    //cleanup
    txnManager.begin();
    
    cache.remove("1");
    cache.remove("2");
    cache.remove("1-2");
    
    txnManager.commit();
   
    
    // example 2

    txnManager.begin();

    cache.put(new Element("1", "one"));

    tx1 = txnManager.suspend();

    txnManager.begin();
    cache.put(new Element("2", "two"));
    
    txnManager.rollback();

    txnManager.resume(tx1);

    if (cache.get("2") != null && "two".equals(cache.get("2").getValue())) {
      cache.put(new Element("1-2", "one-two"));
    }
    
    txnManager.commit();

    //validate
    
    txnManager.begin();
    
    Element elementTwo = cache.get("2");
    if(elementTwo != null) {
      throw new AssertionError("shouldn't exist!");
    }
    
    Element elementOneTwo = cache.get("1-2");
    if(elementOneTwo != null) {
      throw new AssertionError("shouldn't exist!");
    }
    
    txnManager.commit();
      
  }
  
  public static void main(String[] args) {
    new SuspendResumeClient(args).run();
  }
}
