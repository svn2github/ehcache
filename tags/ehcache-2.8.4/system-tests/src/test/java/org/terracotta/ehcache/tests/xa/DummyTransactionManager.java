/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.xa;

import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

public class DummyTransactionManager implements TransactionManager {

  private final AtomicLong txIdGenerator = new AtomicLong();

  private DummyTransaction testTransaction;

  public DummyTransactionManager() {
    //
  }

  public void begin() {
    testTransaction = new DummyTransaction(txIdGenerator.incrementAndGet());
  }

  public void commit() throws IllegalStateException, SecurityException {
    //
  }

  public int getStatus() {
    return 0;
  }

  public Transaction getTransaction() {
    return testTransaction;
  }

  public void resume(Transaction transaction) throws IllegalStateException {
    testTransaction = (DummyTransaction) transaction;
  }

  public void rollback() throws IllegalStateException, SecurityException {
    //
  }

  public void setRollbackOnly() throws IllegalStateException {
    //
  }

  public void setTransactionTimeout(int i) {
    //
  }

  public Transaction suspend() {
    DummyTransaction suspendedTx = testTransaction;
    testTransaction = null;
    return suspendedTx;
  }
}
