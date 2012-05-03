/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction.state;

import net.sf.ehcache.Element;

import org.terracotta.modules.ehcache.transaction.ClusteredTransactionID;
import org.terracotta.modules.ehcache.transaction.SoftLockId;
import org.terracotta.modules.ehcache.transaction.SoftLockState;
import org.terracotta.modules.ehcache.transaction.xa.ClusteredXidTransactionID;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

public interface EhcacheTxnsClusteredStateFacade {

  /**
   * Gets commit state for the txn id. Returns null if state not present for the txn id
   */
  TransactionCommitState getTransactionCommitState(ClusteredTransactionID clusteredTransactionID);

  /**
   * Gets commit state for the txn if already created. Otherwise creates new state with
   * {@link TransactionCommitState#NOT_COMMITTED} and returns it
   */
  TransactionCommitState getOrCreateTransactionCommitState(ClusteredTransactionID clusteredTransactionID);

  /**
   * Returns the transaction commit state for the txn id
   */
  void markTransactionForCommit(ClusteredTransactionID clusteredTransactionID);

  /**
   * Gets the decision for the xa txn id
   */
  XATransactionDecision getXATransactionDecision(ClusteredXidTransactionID xaTxnId);

  /**
   * Updates the decision for the xa txn id
   */
  void updateXATransactionDecision(ClusteredXidTransactionID xaTxnId, XATransactionDecision decision);

  /**
   * Creates a new soft lock state for associated soft lock id
   */
  void createSoftLockState(String cacheManagerName, String cacheName, SoftLockId softLockId, SoftLockState softLockState);

  /**
   * Returns the state for the soft lock id, can return null
   */
  SoftLockState getSoftLockState(String cacheManagerName, String cacheName, SoftLockId softLockId);

  /**
   * Updates the state for the soft lock id, can throw exception if the state is not created already
   */
  Element updateSoftLockState(String cacheManagerName, String cacheName, SoftLockId softLockId, Element newElement);

  /**
   * Gets read-write lock for the soft lock the cache in the cache manager
   */
  ToolkitReadWriteLock getSoftLockWriteLock(String cacheManagerName, String cacheName, SoftLockId softLockId);

  /**
   * Gets read-write "freeze" lock for the soft lock the cache in the cache manager
   */
  ToolkitReadWriteLock getSoftLockFreezeLock(String cacheManagerName, String cacheName, SoftLockId softLockId);

  /**
   * Gets read-write "notifier" lock for the soft lock the cache in the cache manager
   */
  ToolkitReadWriteLock getSoftLockNotifierLock(String cacheManagerName, String cacheName, SoftLockId softLockId);

  /**
   * Visit "all" soft locks for the cache in the cache manager
   */
  void visitAllSoftLocks(String cacheManagerName, String cacheName, SoftLocksVisitor visitor);

  /**
   * Visit over all "new" soft locks for the cache in the cache manager
   */
  void visitAllNewSoftLocks(String cacheManagerName, String cacheName, SoftLocksVisitor visitor);

  /**
   * Returns true if a soft lock exists for the corresponding id
   */
  boolean isSoftLockPresent(String cacheManagerName, String cacheName, SoftLockId softLockId);

  /**
   * Returns whether the soft lock is expired or not
   */
  boolean isExpired(String cacheManagerName, String cacheName, SoftLockId softLockId);

  /**
   * Clears the soft lock and associated state
   */
  void clearSoftLock(String cacheManagerName, String cacheName, SoftLockId softLockId);

  /**
   * Returns whether the given lock is locked by the current thread or not
   */
  boolean isLocked(ToolkitReadWriteLock lock);

}
