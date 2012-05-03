/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.store.SerializationHelper;

import java.io.Serializable;

public class SoftLockId implements Serializable {

  private final TransactionID transactionId;
  private final Object        key;

  public SoftLockId(TransactionID transactionId, Object key) {
    this.transactionId = transactionId;
    this.key = key;
  }

  public TransactionID getTransactionId() {
    return transactionId;
  }

  public Object getKey() {
    return key;
  }

  private Object writeReplace() {
    return new SoftLockSerializedState(transactionId, SerializationHelper.serialize(key));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((transactionId == null) ? 0 : transactionId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SoftLockId other = (SoftLockId) obj;
    if (key == null) {
      if (other.key != null) return false;
    } else if (!key.equals(other.key)) return false;
    if (transactionId == null) {
      if (other.transactionId != null) return false;
    } else if (!transactionId.equals(other.transactionId)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "SoftLockId [transactionId=" + transactionId + ", key=" + key + "]";
  }

  private static class SoftLockSerializedState implements Serializable {

    private final TransactionID txnId;
    private final byte[]        serializedKey;

    public SoftLockSerializedState(TransactionID transactionId, byte[] serializedKey) {
      txnId = transactionId;
      this.serializedKey = serializedKey;
    }

    private Object readResolve() {
      return new SoftLockId(txnId, SerializationHelper.deserialize(serializedKey));
    }

  }

}
