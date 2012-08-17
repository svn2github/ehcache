/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;

import java.io.Serializable;

public class SerializedReadCommittedClusteredSoftLock implements Serializable {

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

}
