/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.TransactionIDSerializedForm;
import net.sf.ehcache.transaction.XidTransactionIDSerializedForm;
import net.sf.ehcache.transaction.xa.XidTransactionID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.transaction.state.EhcacheTxnsClusteredStateFacade;
import org.terracotta.modules.ehcache.transaction.xa.ClusteredXidTransactionID;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban, Abhishek Sanoujam
 */
public class ClusteredTransactionIDFactory implements TransactionIDFactory {

  private static final Logger                   LOG = LoggerFactory.getLogger(ClusteredTransactionIDFactory.class
                                                        .getName());

  private final String                          clusterUUID;
  private final String                          cacheManagerName;

  private final EhcacheTxnsClusteredStateFacade facade;

  public ClusteredTransactionIDFactory(EhcacheTxnsClusteredStateFacade facade, String clusterUUID,
                                       String cacheManagerName) {
    this.facade = facade;
    this.clusterUUID = clusterUUID;
    this.cacheManagerName = cacheManagerName;
    LOG.debug("ClusteredTransactionIDFactory UUID: {}", clusterUUID);
  }

  public TransactionID createTransactionID() {
    return new ClusteredTransactionID(facade, clusterUUID, cacheManagerName);
  }

  public TransactionID restoreTransactionID(TransactionIDSerializedForm serializedForm) {
    return new ClusteredTransactionID(facade, serializedForm);
  }

  public XidTransactionID createXidTransactionID(Xid xid) {
    return new ClusteredXidTransactionID(facade, xid, cacheManagerName);
  }

  public XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
    return new ClusteredXidTransactionID(facade, serializedForm);
  }
}
