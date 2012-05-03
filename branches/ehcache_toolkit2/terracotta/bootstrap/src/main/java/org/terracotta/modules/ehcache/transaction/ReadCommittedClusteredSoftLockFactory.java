/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.local.LocalTransactionContext;

import org.terracotta.modules.ehcache.transaction.state.EhcacheTxnsClusteredStateFacade;
import org.terracotta.modules.ehcache.transaction.state.SoftLocksVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Ludovic Orban, Abhishek Sanoujam
 */
public class ReadCommittedClusteredSoftLockFactory implements SoftLockFactory {

  private final String                          cacheName;
  private final String                          cacheManagerName;

  private final EhcacheTxnsClusteredStateFacade facade;

  public ReadCommittedClusteredSoftLockFactory(EhcacheTxnsClusteredStateFacade facade, String cacheManagerName,
                                               String cacheName) {
    this.facade = facade;
    this.cacheManagerName = cacheManagerName;
    this.cacheName = cacheName;
  }

  String getCacheName() {
    return cacheName;
  }

  String getCacheManagerName() {
    return cacheManagerName;
  }

  public SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement,
                                 boolean pinned) {
    SoftLockId softLockId = new SoftLockId(transactionID, key);
    SoftLockState softLockState = new SoftLockState(softLockId, oldElement, newElement, pinned);
    facade.createSoftLockState(cacheManagerName, cacheName, softLockId, softLockState);
    return newSoftLock(softLockId);
  }

  private ReadCommittedClusteredSoftLock newSoftLock(SoftLockId softLockId) {
    ReadCommittedClusteredSoftLock softLock = new ReadCommittedClusteredSoftLock(facade, this, softLockId);
    return softLock;
  }

  ReadCommittedClusteredSoftLock getLock(SoftLockId softLockId) {
    if (facade.isSoftLockPresent(cacheManagerName, cacheName, softLockId)) { return newSoftLock(softLockId); }
    return null;
  }

  public Set<Object> getKeysInvisibleInContext(LocalTransactionContext currentTransactionContext) {
    Set<Object> invisibleKeys = new HashSet<Object>();

    // all new keys added into the store are invisible
    invisibleKeys.addAll(getNewKeys());

    List<SoftLock> currentTransactionContextSoftLocks = currentTransactionContext.getSoftLocksForCache(cacheName);
    for (SoftLock softLock : currentTransactionContextSoftLocks) {
      if (softLock.getElement(currentTransactionContext.getTransactionId()) == null) {
        // if the soft lock's element is null in the current transaction then the key is invisible
        invisibleKeys.add(softLock.getKey());
      } else {
        // if the soft lock's element is not null in the current transaction then the key is visible
        invisibleKeys.remove(softLock.getKey());
      }
    }

    return invisibleKeys;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized Set<TransactionID> collectExpiredTransactionIDs() {
    final Set<TransactionID> result = new HashSet<TransactionID>();
    facade.visitAllSoftLocks(cacheManagerName, cacheName, new SoftLocksVisitor() {
      @Override
      public void visitSoftLock(SoftLockId softLockId) {
        if (facade.isExpired(cacheManagerName, cacheName, softLockId)) {
          result.add(softLockId.getTransactionId());
        }
      }
    });
    return result;
  }

  /**
   * {@inheritDoc}
   */
  public Set<SoftLock> collectAllSoftLocksForTransactionID(final TransactionID transactionID) {
    final Set<SoftLock> result = new HashSet<SoftLock>();
    facade.visitAllSoftLocks(cacheManagerName, cacheName, new SoftLocksVisitor() {
      @Override
      public void visitSoftLock(SoftLockId softLockId) {
        if (softLockId.getTransactionId().equals(transactionID)) {
          result.add(newSoftLock(softLockId));
        }
      }
    });
    return result;
  }

  void clearSoftLock(ReadCommittedClusteredSoftLock softLock) {
    facade.clearSoftLock(cacheManagerName, cacheName, softLock.getSoftLockId());
  }

  private Set<Object> getNewKeys() {
    final Set<Object> result = new HashSet<Object>();
    facade.visitAllNewSoftLocks(cacheManagerName, cacheName, new SoftLocksVisitor() {
      @Override
      public void visitSoftLock(SoftLockId softLockId) {
        result.add(softLockId.getKey());
      }
    });
    return result;
  }
}
