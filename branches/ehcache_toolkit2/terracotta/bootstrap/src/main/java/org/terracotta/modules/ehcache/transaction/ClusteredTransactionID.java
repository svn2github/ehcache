/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.TransactionIDSerializedForm;

import org.terracotta.modules.ehcache.transaction.state.EhcacheTxnsClusteredStateFacade;
import org.terracotta.modules.ehcache.transaction.state.TransactionCommitState;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ludovic Orban, Abhishek Sanoujam
 */
public class ClusteredTransactionID implements TransactionID {

  private static final AtomicInteger            idGenerator = new AtomicInteger();

  // private final fields
  private final String                          clusterUUID;
  private final String                          cacheManagerName;
  private final long                            creationTime;
  private final int                             id;
  private final EhcacheTxnsClusteredStateFacade facade;

  ClusteredTransactionID(EhcacheTxnsClusteredStateFacade facade, String clusterUUID, String cacheManagerName) {
    this(facade, clusterUUID, cacheManagerName, System.currentTimeMillis(), idGenerator.getAndIncrement());
  }

  ClusteredTransactionID(EhcacheTxnsClusteredStateFacade facade, TransactionIDSerializedForm serializedForm) {
    this(facade, serializedForm.getClusterUUID(), serializedForm.getCacheManagerName(), serializedForm
        .getCreationTime(), serializedForm.getId());
  }

  public ClusteredTransactionID(EhcacheTxnsClusteredStateFacade facade, String clusterUUID, String cacheManagerName,
                                long creationTime, int id) {
    this.facade = facade;
    this.clusterUUID = clusterUUID;
    this.cacheManagerName = cacheManagerName;
    this.creationTime = creationTime;
    this.id = id;
    facade.getOrCreateTransactionCommitState(this);
  }

  public boolean isDecisionCommit() {
    TransactionCommitState transactionCommitState = facade.getTransactionCommitState(this);
    if (transactionCommitState == null) { throw new AssertionError("Transaction state not found for: " + this); }
    return transactionCommitState.isCommitted();
  }

  public void markForCommit() {
    facade.markTransactionForCommit(this);
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj instanceof ClusteredTransactionID) {
      ClusteredTransactionID otherId = (ClusteredTransactionID) obj;
      return id == otherId.id && clusterUUID.equals(otherId.clusterUUID) && creationTime == otherId.creationTime;
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return (((id + (int) creationTime) * 31) ^ clusterUUID.hashCode());
  }

  @Override
  public String toString() {
    TransactionCommitState state = facade.getTransactionCommitState(this);
    return id
           + ":"
           + creationTime
           + "@"
           + clusterUUID
           + (state != null ? (state.isCommitted() ? " (marked for commit)" : "")
               : " <txn state not found, probably already committed txn>");
  }

  public String getClusterUUID() {
    return clusterUUID;
  }

  public String getCacheManagerName() {
    return cacheManagerName;
  }

  public int getId() {
    return id;
  }

  private Object writeReplace() {
    // always store constant value for transaction commit state (using false)
    return new TransactionIDSerializedForm(cacheManagerName, clusterUUID, creationTime, id, false);
  }

}
