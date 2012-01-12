/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.xa;

import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

public class DummyTransaction implements Transaction {

  private final long id;

  public DummyTransaction(long id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DummyTransaction) {
      DummyTransaction otherTx = (DummyTransaction) o;
      return otherTx.id == id;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (int) id;
  }

  public void commit() throws SecurityException {
    //
  }

  public boolean delistResource(XAResource xaResource, int i) throws IllegalStateException {
    return true;
  }

  public boolean enlistResource(XAResource xaResource) throws IllegalStateException {
    return true;
  }

  public int getStatus() {
    return 0;
  }

  public void registerSynchronization(Synchronization synchronization) throws IllegalStateException {
    //
  }

  public void rollback() throws IllegalStateException {
    //
  }

  public void setRollbackOnly() throws IllegalStateException {
    //
  }
}
