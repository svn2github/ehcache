/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.Decision;
import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.TransactionIDFactory;
import net.sf.ehcache.transaction.TransactionIDSerializedForm;
import net.sf.ehcache.transaction.XidTransactionIDSerializedForm;
import net.sf.ehcache.transaction.xa.XidTransactionID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.transaction.xa.ClusteredXidTransactionID;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public class ClusteredTransactionIDFactory implements TransactionIDFactory {

  private static final Logger LOG = LoggerFactory.getLogger(ClusteredTransactionIDFactory.class.getName());


  private final String clusterUUID;
  private final String cacheManagerName;

  private final ConcurrentMap<TransactionID, Decision> transactionStates;

  public ClusteredTransactionIDFactory(String clusterUUID, String cacheManagerName, ConcurrentMap<TransactionID, Decision> transactionMap) {
    this.clusterUUID = clusterUUID;
    this.cacheManagerName = cacheManagerName;
    this.transactionStates = transactionMap;
    LOG.debug("ClusteredTransactionIDFactory UUID: {}", clusterUUID);
  }

  public TransactionID createTransactionID() {
    TransactionID id = new ClusteredTransactionID(clusterUUID, cacheManagerName);
    transactionStates.putIfAbsent(id, Decision.IN_DOUBT);
    return id;
  }

  public TransactionID restoreTransactionID(TransactionIDSerializedForm serializedForm) {
    return new ClusteredTransactionID(serializedForm);
  }

  public XidTransactionID createXidTransactionID(Xid xid) {
    XidTransactionID id = new ClusteredXidTransactionID(xid, cacheManagerName);
    transactionStates.putIfAbsent(id, Decision.IN_DOUBT);
    return id;
  }

  public XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
    return new ClusteredXidTransactionID(serializedForm);
  }
  
  @Override
  public void markForCommit(TransactionID transactionID) {
    while (true) {
      Decision current = transactionStates.get(transactionID);
      switch (current) {
        case IN_DOUBT:
          if (transactionStates.replace(transactionID, Decision.IN_DOUBT, Decision.COMMIT)) { return; }
          break;
        case ROLLBACK:
          throw new IllegalStateException(this + " already marked for rollback, cannot re-mark it for commit");
        case COMMIT:
          return;
      }
    }
  }

  @Override
  public void markForRollback(XidTransactionID transactionID) {
    while (true) {
      Decision current = transactionStates.get(transactionID);
      switch (current) {
        case IN_DOUBT:
          if (transactionStates.replace(transactionID, Decision.IN_DOUBT, Decision.ROLLBACK)) { return; }
          break;
        case ROLLBACK:
          return;
        case COMMIT:
          throw new IllegalStateException(this + " already marked for commit, cannot re-mark it for rollback");
      }
    }
  }

  @Override
  public boolean isDecisionCommit(TransactionID transactionID) {
    return Decision.COMMIT.equals(transactionStates.get(transactionID));
  }

  @Override
  public Set<TransactionID> getInDoubtTransactionIDs() {
    Set<TransactionID> result = new HashSet<TransactionID>();

    for (Entry<TransactionID, Decision> e : transactionStates.entrySet()) {
      if (Decision.IN_DOUBT.equals(e.getValue())) {
        result.add(e.getKey());
      }
    }

    return result;
  }

  @Override
  public void clear(TransactionID transactionID) {
    transactionStates.remove(transactionID);
  }
}
