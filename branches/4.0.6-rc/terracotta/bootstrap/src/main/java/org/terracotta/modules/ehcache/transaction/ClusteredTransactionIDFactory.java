/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.transaction.AbstractTransactionIDFactory;
import net.sf.ehcache.transaction.Decision;
import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.TransactionIDSerializedForm;
import net.sf.ehcache.transaction.XidTransactionIDSerializedForm;
import net.sf.ehcache.transaction.xa.XidTransactionID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.collections.SerializedToolkitCache;
import org.terracotta.modules.ehcache.transaction.xa.ClusteredXidTransactionID;

import java.util.concurrent.ConcurrentMap;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public class ClusteredTransactionIDFactory extends AbstractTransactionIDFactory {

  private static final Logger                                   LOG = LoggerFactory
                                                                        .getLogger(ClusteredTransactionIDFactory.class
                                                                            .getName());

  private final String                                          clusterUUID;
  private final String                                          cacheManagerName;

  private final SerializedToolkitCache<TransactionID, Decision> transactionStates;
  private final CacheCluster                                    clusterTopology;

  public ClusteredTransactionIDFactory(String clusterUUID, String cacheManagerName,
                                       ToolkitInstanceFactory toolkitInstanceFactory, CacheCluster topology) {
    this.clusterUUID = clusterUUID;
    this.cacheManagerName = cacheManagerName;
    this.transactionStates = toolkitInstanceFactory.getOrCreateTransactionCommitStateMap(cacheManagerName);
    clusterTopology = topology;
    LOG.debug("ClusteredTransactionIDFactory UUID: {}", clusterUUID);
  }

  @Override
  public TransactionID createTransactionID() {
    TransactionID id = new ClusteredTransactionID(clusterTopology.getCurrentNode().getId(), clusterUUID,
                                                  cacheManagerName);
    getTransactionStates().putIfAbsent(id, Decision.IN_DOUBT);
    return id;
  }

  @Override
  public boolean isExpired(TransactionID id) {
    if (id instanceof ClusteredID) {
      String ownerClientId = ((ClusteredID) id).getOwnerID();
      for (ClusterNode node : clusterTopology.getNodes()) {
        if (node.getId().equals(ownerClientId)) { return false; }
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public TransactionID restoreTransactionID(TransactionIDSerializedForm serializedForm) {
    return new ClusteredTransactionID(serializedForm);
  }

  @Override
  public XidTransactionID createXidTransactionID(Xid xid, Ehcache cache) {
    XidTransactionID id = new ClusteredXidTransactionID(xid, cacheManagerName, cache.getName(), clusterTopology
        .getCurrentNode().getId());
    getTransactionStates().putIfAbsent(id, Decision.IN_DOUBT);
    return id;
  }

  @Override
  public XidTransactionID restoreXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
    return new ClusteredXidTransactionID(serializedForm);
  }

  @Override
  protected ConcurrentMap<TransactionID, Decision> getTransactionStates() {
    return transactionStates;
  }

  @Override
  public Boolean isPersistent() {
    return Boolean.TRUE;
  }
}
