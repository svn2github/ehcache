/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction.xa;

import net.sf.ehcache.transaction.XidTransactionIDSerializedForm;
import net.sf.ehcache.transaction.xa.XidTransactionID;

import org.terracotta.modules.ehcache.transaction.state.EhcacheTxnsClusteredStateFacade;
import org.terracotta.modules.ehcache.transaction.state.XATransactionDecision;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban, Abhishek Sanoujam
 */
public class ClusteredXidTransactionID implements XidTransactionID {

  private final Xid                             xid;
  private final String                          cacheManagerName;
  private final EhcacheTxnsClusteredStateFacade facade;

  public ClusteredXidTransactionID(EhcacheTxnsClusteredStateFacade facade, XidTransactionIDSerializedForm serializedForm) {
    this(facade, serializedForm.getXid(), serializedForm.getCacheManagerName());
  }

  public ClusteredXidTransactionID(EhcacheTxnsClusteredStateFacade facade, Xid xid, String cacheManagerName) {
    this.facade = facade;
    this.cacheManagerName = cacheManagerName;
    this.xid = new XidClustered(xid);
  }

  public String getCacheManagerName() {
    return cacheManagerName;
  }

  public boolean isDecisionCommit() {
    return facade.getXATransactionDecision(this) == XATransactionDecision.COMMIT;
  }

  public synchronized void markForCommit() {
    facade.updateXATransactionDecision(this, XATransactionDecision.COMMIT);
  }

  public boolean isDecisionRollback() {
    return facade.getXATransactionDecision(this) == XATransactionDecision.ROLLBACK;
  }

  public void markForRollback() {
    facade.updateXATransactionDecision(this, XATransactionDecision.COMMIT);
  }

  public Xid getXid() {
    return xid;
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
    return "Clustered [" + xid + "] (decision: " + facade.getXATransactionDecision(this) + ")";
  }

  private Object writeReplace() {
    return new XidTransactionIDSerializedForm(cacheManagerName, xid, facade.getXATransactionDecision(this).toString());
  }

}
