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

  ClusteredTransactionID(String ownerId, String clusterUUID, String cacheManagerName) {
    this.clusterUUID = clusterUUID;
    this.ownerID = ownerId;
    this.cacheManagerName = cacheManagerName;
    this.creationTime = System.currentTimeMillis();
    this.id = idGenerator.getAndIncrement();
  }

  ClusteredTransactionID(TransactionIDSerializedForm serializedForm) {
    this.clusterUUID = serializedForm.getClusterUUID();
    this.ownerID = serializedForm.getOwnerID();
    this.cacheManagerName = serializedForm.getCacheManagerName();
    this.creationTime = serializedForm.getCreationTime();
    this.id = serializedForm.getId();
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
