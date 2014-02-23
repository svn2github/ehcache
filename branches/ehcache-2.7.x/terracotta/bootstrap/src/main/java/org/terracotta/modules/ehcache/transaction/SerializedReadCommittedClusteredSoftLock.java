/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;

import java.io.Serializable;

public class SerializedReadCommittedClusteredSoftLock implements Serializable {

  private static final long                                 serialVersionUID = -766870846218858666L;

  private final TransactionID                               transactionID;
  private final Object                                      deserializedKey;
  private transient volatile ReadCommittedClusteredSoftLock softLock;

  public SerializedReadCommittedClusteredSoftLock(TransactionID transactionID, Object deserializedKey) {
    this.transactionID = transactionID;
    this.deserializedKey = deserializedKey;
  }

  public ReadCommittedClusteredSoftLock getSoftLock(ToolkitInstanceFactory toolkitInstanceFactory,
                                                    ReadCommittedClusteredSoftLockFactory factory) {
    ReadCommittedClusteredSoftLock rv = softLock;
    if (rv != null) { return rv; }
    synchronized (this) {
      softLock = new ReadCommittedClusteredSoftLock(toolkitInstanceFactory, factory, transactionID, deserializedKey);
      rv = softLock;
    }
    return rv;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof SerializedReadCommittedClusteredSoftLock) {
      SerializedReadCommittedClusteredSoftLock other = (SerializedReadCommittedClusteredSoftLock) object;

      if (!transactionID.equals(other.transactionID)) { return false; }
      if (!deserializedKey.equals(other.deserializedKey)) { return false; }

      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hashCode = 31;

    hashCode *= transactionID.hashCode();
    hashCode *= deserializedKey.hashCode();

    return hashCode;
  }

}
