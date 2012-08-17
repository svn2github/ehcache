/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction.xa;

import net.sf.ehcache.transaction.XidTransactionIDSerializedForm;
import net.sf.ehcache.transaction.xa.XidTransactionID;

import org.terracotta.modules.ehcache.transaction.ClusteredID;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public class ClusteredXidTransactionID implements XidTransactionID, ClusteredID {

  private final Xid    xid;
  private final String ownerID;
  private final String cacheName;
  private final String cacheManagerName;

  public ClusteredXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
    this.xid = new XidClustered(serializedForm.getXid());
    this.ownerID = serializedForm.getOwnerID();
    this.cacheManagerName = serializedForm.getCacheManagerName();
    this.cacheName = serializedForm.getCacheName();
  }

  public ClusteredXidTransactionID(Xid xid, String cacheManagerName, String cacheName, String ownerID) {
    this.cacheManagerName = cacheManagerName;
    this.cacheName = cacheName;
    this.ownerID = ownerID;
    this.xid = new XidClustered(xid);
  }

  @Override
  public Xid getXid() {
    return xid;
  }

  @Override
  public String getCacheName() {
    return cacheName;
  }

  @Override
  public String getOwnerID() {
    return ownerID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean equals(Object obj) {
    if (obj instanceof ClusteredXidTransactionID) {
      ClusteredXidTransactionID otherId = (ClusteredXidTransactionID) obj;
      return xid.equals(otherId.xid);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int hashCode() {
    return xid.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Clustered [" + xid + "]";
  }

  private Object writeReplace() {
    return new XidTransactionIDSerializedForm(cacheManagerName, cacheName, ownerID, xid);
  }
}
