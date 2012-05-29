/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction.state;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactoryImpl;
import org.terracotta.modules.ehcache.store.SerializationHelper;
import org.terracotta.modules.ehcache.transaction.ClusteredTransactionID;
import org.terracotta.modules.ehcache.transaction.SoftLockId;
import org.terracotta.modules.ehcache.transaction.SoftLockState;
import org.terracotta.modules.ehcache.transaction.xa.ClusteredXidTransactionID;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

/**
 * @author Abhishek Sanoujam
 */
public class EhcacheTxnsClusteredStateFacadeImpl implements EhcacheTxnsClusteredStateFacade {
  private static final String                              SOFT_LOCKS_MAP_LOCK_SUFFIX     = "SoftLocksMapLock";

  private static final String                              DELIMITER                      = ".";
  private static final String                              SOFT_LOCK_WRITE_LOCK_SUFFIX    = "softLockWriteLock";
  private static final String                              SOFT_LOCK_FREEZE_LOCK_SUFFIX   = "softLockFreezeLock";
  private static final String                              SOFT_LOCK_NOTIFIER_LOCK_SUFFIX = "softLockNotifierLock";
  private final ToolkitMap<String, TransactionCommitState> commitStateToolkitMap;
  private final ToolkitMap<String, XATransactionDecision>  xaTxnDecisionMap;
  private final ToolkitInstanceFactory                     toolkitInstanceFactory;

  public EhcacheTxnsClusteredStateFacadeImpl(ToolkitInstanceFactory toolkitInstanceFactory) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.commitStateToolkitMap = toolkitInstanceFactory.getOrCreateTransactionCommitStateMap();
    this.xaTxnDecisionMap = toolkitInstanceFactory.getOrCreateXATransactionDecisionMap();
  }

  private static String getStringKeyForTransactionId(TransactionID txnId) {
    if (txnId instanceof ClusteredTransactionID) {
      ClusteredTransactionID clusteredTransactionID = (ClusteredTransactionID) txnId;
      return clusteredTransactionID.getCacheManagerName() + DELIMITER + clusteredTransactionID.getClusterUUID()
             + DELIMITER + clusteredTransactionID.getId();
    } else if (txnId instanceof ClusteredXidTransactionID) {
      ClusteredXidTransactionID clusteredXidTransactionID = (ClusteredXidTransactionID) txnId;
      return clusteredXidTransactionID.getCacheManagerName() + DELIMITER
             + serializeToString(clusteredXidTransactionID.getXid());
    } else {
      throw new AssertionError("Unknown type of transaction id: " + txnId
                               + (txnId != null ? "(" + txnId.getClass().getName() + ")" : ""));
    }
  }

  private static String serializeToString(Object serializable) {
    try {
      return SerializationHelper.serializeToString(serializable);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Object deserializeFromString(String string) {
    try {
      return SerializationHelper.deserializeString(string);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ToolkitMap<String, SoftLockState> getOrCreateAllSoftLocksMap(String cacheManagerName, String cacheName) {
    return toolkitInstanceFactory.getOrCreateAllSoftLockMap(cacheManagerName, cacheName);
  }

  private ToolkitList<SoftLockId> getOrCreateNewSoftLocksSet(String cacheManagerName, String cacheName) {
    return toolkitInstanceFactory.getOrCreateNewSoftLocksSet(cacheManagerName, cacheName);
  }

  @Override
  public TransactionCommitState getTransactionCommitState(ClusteredTransactionID txnId) {
    String clusteredTxnIdKey = getStringKeyForTransactionId(txnId);
    ToolkitLock lock = commitStateToolkitMap.createFinegrainedLock(clusteredTxnIdKey);
    lock.lock();
    try {
      return commitStateToolkitMap.get(clusteredTxnIdKey);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public TransactionCommitState getOrCreateTransactionCommitState(ClusteredTransactionID txnId) {
    String clusteredTxnIdKey = getStringKeyForTransactionId(txnId);
    ToolkitLock lock = commitStateToolkitMap.createFinegrainedLock(clusteredTxnIdKey);
    lock.lock();
    try {
      TransactionCommitState state = commitStateToolkitMap.get(clusteredTxnIdKey);
      if (state == null) {
        state = TransactionCommitState.NOT_COMMITTED;
        commitStateToolkitMap.put(clusteredTxnIdKey, state);
      }
      return state;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void markTransactionForCommit(ClusteredTransactionID txnId) {
    String clusteredTxnIdKey = getStringKeyForTransactionId(txnId);
    ToolkitLock lock = commitStateToolkitMap.createFinegrainedLock(clusteredTxnIdKey);
    lock.lock();
    try {
      commitStateToolkitMap.put(clusteredTxnIdKey, TransactionCommitState.COMMITTED);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public XATransactionDecision getXATransactionDecision(ClusteredXidTransactionID txnId) {
    String key = getStringKeyForTransactionId(txnId);
    ToolkitLock lock = xaTxnDecisionMap.createFinegrainedLock(key);
    lock.lock();
    try {
      XATransactionDecision decision = xaTxnDecisionMap.get(key);
      if (decision == null) { return XATransactionDecision.IN_DOUBT; }
      return decision;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void updateXATransactionDecision(ClusteredXidTransactionID txnId, XATransactionDecision newDecision) {
    String key = getStringKeyForTransactionId(txnId);
    ToolkitLock lock = xaTxnDecisionMap.createFinegrainedLock(key);
    lock.lock();
    try {
      XATransactionDecision existingDecision = xaTxnDecisionMap.get(key);
      validateTransition(txnId, newDecision, existingDecision);
      xaTxnDecisionMap.put(key, newDecision);
    } finally {
      lock.unlock();
    }

  }

  private static void validateTransition(ClusteredXidTransactionID xaTxnId, XATransactionDecision newDecision,
                                         XATransactionDecision existingDecision) {
    if (existingDecision != null) {
      if (existingDecision == XATransactionDecision.ROLLBACK && newDecision == XATransactionDecision.COMMIT) {
        //
        throw new IllegalStateException(xaTxnId + " already marked for rollback, cannot re-mark it for commit");
      }
      if (existingDecision == XATransactionDecision.COMMIT && newDecision == XATransactionDecision.ROLLBACK) {
        //
        throw new IllegalStateException(xaTxnId + " already marked for commit, cannot re-mark it for rollback");
      }
    }
  }

  private void removeTransactionIdState(TransactionID txnId) {
    String key = getStringKeyForTransactionId(txnId);
    if (txnId instanceof ClusteredTransactionID) {
      commitStateToolkitMap.remove(key);
    } else if (txnId instanceof ClusteredXidTransactionID) {
      xaTxnDecisionMap.remove(key);
    } else {
      throw new AssertionError("Unknown type of transaction id: " + txnId
                               + (txnId != null ? "(" + txnId.getClass().getName() + ")" : ""));
    }
  }

  private ToolkitAtomicLong getSoftLockCountForTransaction(TransactionID txnId) {
    String key = getStringKeyForTransactionId(txnId);
    return toolkitInstanceFactory.getToolkit().getAtomicLong(key);
  }

  @Override
  public void createSoftLockState(String cacheManagerName, String cacheName, SoftLockId softLockId,
                                  SoftLockState softLockState) {
    ToolkitMap<String, SoftLockState> allSoftLocksMap = getOrCreateAllSoftLocksMap(cacheManagerName, cacheName);
    ToolkitList<SoftLockId> newSoftLocksSet = getOrCreateNewSoftLocksSet(cacheManagerName, cacheName);
    String key = serializeToString(softLockId);
    ToolkitLock lock = getSoftLocksMapLock(cacheManagerName, cacheName);
    lock.lock();
    try {
      allSoftLocksMap.put(key, softLockState);
      if (softLockState.getOldElement() == null) {
        newSoftLocksSet.add(softLockId);
      }
      getSoftLockCountForTransaction(softLockId.getTransactionId()).incrementAndGet();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public SoftLockState getSoftLockState(String cacheManagerName, String cacheName, SoftLockId softLockId) {
    ToolkitMap<String, SoftLockState> allSoftLocksMap = getOrCreateAllSoftLocksMap(cacheManagerName, cacheName);
    String key = serializeToString(softLockId);
    ToolkitLock lock = getSoftLocksMapLock(cacheManagerName, cacheName);
    lock.lock();
    try {
      return allSoftLocksMap.get(key);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Element updateSoftLockState(String cacheManagerName, String cacheName, SoftLockId softLockId,
                                     Element newElement) {
    ToolkitMap<String, SoftLockState> allSoftLocksMap = getOrCreateAllSoftLocksMap(cacheManagerName, cacheName);
    String key = serializeToString(softLockId);
    ToolkitLock lock = getSoftLocksMapLock(cacheManagerName, cacheName);
    lock.lock();
    try {
      SoftLockState oldSoftLockState = allSoftLocksMap.get(key);
      SoftLockState newSoftLockState = oldSoftLockState.newSoftLockState(newElement);
      allSoftLocksMap.put(key, newSoftLockState);
      return oldSoftLockState.getNewElement();
    } finally {
      lock.unlock();
    }
  }

  private String softLockWriteLockName(String cacheManagerName, String cacheName, SoftLockId softLockId) {
    return ToolkitInstanceFactoryImpl.getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
           + serializeToString(softLockId) + DELIMITER + SOFT_LOCK_WRITE_LOCK_SUFFIX;
  }

  private String softLockFreezeLockName(String cacheManagerName, String cacheName, SoftLockId softLockId) {
    return ToolkitInstanceFactoryImpl.getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
           + serializeToString(softLockId) + DELIMITER + SOFT_LOCK_FREEZE_LOCK_SUFFIX;
  }

  private String softLockNotifierLockName(String cacheManagerName, String cacheName, SoftLockId softLockId) {
    return ToolkitInstanceFactoryImpl.getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
           + serializeToString(softLockId) + DELIMITER + SOFT_LOCK_NOTIFIER_LOCK_SUFFIX;
  }

  @Override
  public ToolkitReadWriteLock getSoftLockWriteLock(String cacheManagerName, String cacheName, SoftLockId softLockId) {
    return this.toolkitInstanceFactory.getToolkit().getReadWriteLock(softLockWriteLockName(cacheManagerName, cacheName,
                                                                                           softLockId));
  }

  @Override
  public ToolkitReadWriteLock getSoftLockFreezeLock(String cacheManagerName, String cacheName, SoftLockId softLockId) {
    return this.toolkitInstanceFactory.getToolkit().getReadWriteLock(softLockFreezeLockName(cacheManagerName,
                                                                                            cacheName, softLockId));
  }

  @Override
  public ToolkitReadWriteLock getSoftLockNotifierLock(String cacheManagerName, String cacheName, SoftLockId softLockId) {
    return this.toolkitInstanceFactory.getToolkit().getReadWriteLock(softLockNotifierLockName(cacheManagerName,
                                                                                              cacheName, softLockId));
  }

  private ToolkitLock getSoftLocksMapLock(String cacheManagerName, String cacheName) {
    return toolkitInstanceFactory.getToolkit()
        .getLock(ToolkitInstanceFactoryImpl.getFullyQualifiedCacheName(cacheManagerName, cacheName) + DELIMITER
                     + SOFT_LOCKS_MAP_LOCK_SUFFIX, ToolkitLockType.WRITE);
  }

  @Override
  public void visitAllSoftLocks(String cacheManagerName, String cacheName, SoftLocksVisitor visitor) {
    ToolkitLock lock = getSoftLocksMapLock(cacheManagerName, cacheName);
    lock.lock();
    try {
      for (String key : getOrCreateAllSoftLocksMap(cacheManagerName, cacheName).keySet()) {
        SoftLockId softLockId = (SoftLockId) deserializeFromString(key);
        visitor.visitSoftLock(softLockId);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void visitAllNewSoftLocks(String cacheManagerName, String cacheName, SoftLocksVisitor visitor) {
    ToolkitLock lock = getSoftLocksMapLock(cacheManagerName, cacheName);
    lock.lock();
    try {
      for (SoftLockId softLockId : getOrCreateNewSoftLocksSet(cacheManagerName, cacheName)) {
        visitor.visitSoftLock(softLockId);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean isSoftLockPresent(String cacheManagerName, String cacheName, SoftLockId softLockId) {
    return getSoftLockState(cacheManagerName, cacheName, softLockId) != null;
  }

  @Override
  public boolean isExpired(String cacheManagerName, String cacheName, SoftLockId softLockId) {
    ToolkitReadWriteLock freezeLock = getSoftLockFreezeLock(cacheManagerName, cacheName, softLockId);
    ToolkitReadWriteLock writeLock = getSoftLockWriteLock(cacheManagerName, cacheName, softLockId);
    boolean alive = isLocked(writeLock) || isLocked(freezeLock);
    return !alive;
  }

  @Override
  public void clearSoftLock(String cacheManagerName, String cacheName, SoftLockId softLockId) {
    ToolkitLock lock = getSoftLocksMapLock(cacheManagerName, cacheName);
    lock.lock();
    try {
      String key = serializeToString(softLockId);
      ToolkitMap<String, SoftLockState> allSoftLocksMap = getOrCreateAllSoftLocksMap(cacheManagerName, cacheName);
      allSoftLocksMap.remove(key);
      getOrCreateNewSoftLocksSet(cacheManagerName, cacheName).remove(softLockId);
      long softLockCount = getSoftLockCountForTransaction(softLockId.getTransactionId()).decrementAndGet();
      if (softLockCount == 0) {
        // no more soft locks remaining for the txnId of this softLock, remove the txn commit state too
        removeTransactionIdState(softLockId.getTransactionId());
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean isLocked(ToolkitReadWriteLock rwLock) {
    ToolkitLock writeLock = rwLock.writeLock();
    if (writeLock.isHeldByCurrentThread()) { return true; }
    // tryLock may return false although the lock is not held but was locked and unlocked by another L1
    // which keeps the lock greedily. That's okay because we're just interested to know if a lock was
    // released prematurely because of a L1 crash in which case tryLock will return true
    boolean gotLock = writeLock.tryLock();
    if (gotLock) {
      writeLock.unlock();
      return false;
    }
    return true;
  }

}
