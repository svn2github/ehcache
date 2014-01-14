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
public class ClusteredTransactionID implements TransactionID, ClusteredID {

  private static final AtomicInteger idGenerator = new AtomicInteger();

  private final String               clusterUUID;
  private final String               ownerID;
  private final String               cacheManagerName;
  private final long                 creationTime;
  private final int                  id;

  public ClusteredTransactionID(String ownerId, String clusterUUID, String cacheManagerName) {
    this(ownerId, clusterUUID, cacheManagerName, System.currentTimeMillis(), idGenerator.getAndIncrement());
  }

  public ClusteredTransactionID(TransactionIDSerializedForm serializedForm) {
    this(serializedForm.getOwnerID(), serializedForm.getClusterUUID(), serializedForm.getCacheManagerName(),
            serializedForm.getCreationTime(), serializedForm.getId());
  }

  public ClusteredTransactionID(String ownerId, String clusterUUID, String cacheManagerName, long creationTime, int id) {
    this.clusterUUID = clusterUUID;
    this.ownerID = ownerId;
    this.cacheManagerName = cacheManagerName;
    this.creationTime = creationTime;
    this.id = id;
  }

  @Override
  public String getOwnerID() {
    return ownerID;
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
    return id + ":" + ownerID + ":" + creationTime + "@" + clusterUUID;
  }

  private Object writeReplace() {
    return new TransactionIDSerializedForm(cacheManagerName, clusterUUID, ownerID, creationTime, id);
  }

}
