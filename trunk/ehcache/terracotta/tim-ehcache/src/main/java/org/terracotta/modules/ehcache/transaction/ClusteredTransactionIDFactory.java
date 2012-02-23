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
import org.terracotta.modules.ehcache.transaction.xa.ClusteredXidTransactionID;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public class ClusteredTransactionIDFactory implements TransactionIDFactory {

  private static final Logger LOG = LoggerFactory.getLogger(ClusteredTransactionIDFactory.class.getName());


  private final String clusterUUID;
  private final String cacheManagerName;

  public ClusteredTransactionIDFactory(String clusterUUID, String cacheManagerName) {
    this.clusterUUID = clusterUUID;
    this.cacheManagerName = cacheManagerName;
    LOG.debug("ClusteredTransactionIDFactory UUID: {}", clusterUUID);
  }

  public TransactionID createTransactionID() {
    return new ClusteredTransactionID(clusterUUID, cacheManagerName);
  }

  public TransactionID restoreTransactionID(TransactionIDSerializedForm serializedForm) {
    return new ClusteredTransactionID(serializedForm);
  }

  public XidTransactionID createXidTransactionID(Xid xid) {
    return new ClusteredXidTransactionID(xid, cacheManagerName);
  }

  public XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
    return new ClusteredXidTransactionID(serializedForm);
  }
}
