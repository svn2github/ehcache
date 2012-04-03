/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.TransactionIDSerializedForm;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ludovic Orban
 */
public class ClusteredTransactionID implements TransactionID {

  private static final AtomicInteger idGenerator = new AtomicInteger();

  private final String clusterUUID;
  private final String cacheManagerName;
  private final long creationTime;
  private final int id;
  private volatile boolean commit;

  ClusteredTransactionID(String clusterUUID, String cacheManagerName) {
    this.clusterUUID = clusterUUID;
    this.cacheManagerName = cacheManagerName;
    this.creationTime = System.currentTimeMillis();
    this.id = idGenerator.getAndIncrement();
  }

  ClusteredTransactionID(TransactionIDSerializedForm serializedForm) {
    this.clusterUUID = serializedForm.getClusterUUID();
    this.cacheManagerName = serializedForm.getCacheManagerName();
    this.creationTime = serializedForm.getCreationTime();
    this.id = serializedForm.getId();
    this.commit = serializedForm.isCommit();
  }

  // autolocked in config
  public synchronized boolean isDecisionCommit() {
    return commit;
  }

  // autolocked in config
  public synchronized void markForCommit() {
    this.commit = true;
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj instanceof ClusteredTransactionID) {
      ClusteredTransactionID otherId = (ClusteredTransactionID) obj;
      return id == otherId.id &&
              clusterUUID.equals(otherId.clusterUUID) &&
              creationTime == otherId.creationTime;
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return (((id + (int) creationTime) * 31) ^ clusterUUID.hashCode());
  }

  @Override
  public String toString() {
    return id + ":" + creationTime + "@" + clusterUUID + (commit ? " (marked for commit)" : "");
  }

  private Object writeReplace() {
    return new TransactionIDSerializedForm(cacheManagerName, clusterUUID, creationTime, id, commit);
  }

}
