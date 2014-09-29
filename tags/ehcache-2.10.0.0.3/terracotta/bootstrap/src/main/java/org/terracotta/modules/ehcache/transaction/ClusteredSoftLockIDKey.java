/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.SoftLockID;
import net.sf.ehcache.transaction.TransactionID;

import java.io.Serializable;

/**
 * @author Ludovic Orban
 */
// Dont think We need this class .
public class ClusteredSoftLockIDKey implements Serializable {
  private static final int    PRIME = 31;
  private final TransactionID transactionID;
  private final Object        key;

  public ClusteredSoftLockIDKey(SoftLockID softLockId) {
    this.transactionID = softLockId.getTransactionID();
    this.key = softLockId.getKey();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int hashCode = PRIME;

    hashCode *= transactionID.hashCode();
    hashCode *= key.hashCode();

    return hashCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (object instanceof ClusteredSoftLockIDKey) {
      ClusteredSoftLockIDKey other = (ClusteredSoftLockIDKey) object;

      if (!transactionID.equals(other.transactionID)) { return false; }

      if (!key.equals(other.key)) { return false; }

      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Clustered Soft Lock ID [transactionID: " + transactionID + ", key: " + key + "]";
  }




}
