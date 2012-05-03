/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.TransactionException;
import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.transaction.state.EhcacheTxnsClusteredStateFacade;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @author Ludovic Orban, Abhishek Sanoujam
 */
@IgnoreSizeOf
public class ReadCommittedClusteredSoftLock implements SoftLock {

  private final ReadCommittedClusteredSoftLockFactory factory;
  private final String                                cacheManagerName;
  private final String                                cacheName;
  private final ToolkitReadWriteLock                  lock;
  private final ToolkitReadWriteLock                  freezeLock;
  private final ToolkitReadWriteLock                  notificationLock;
  private final Condition                             notifier;
  private final SoftLockId                            softLockId;
  private boolean                                     expired;
  private final EhcacheTxnsClusteredStateFacade       facade;

  ReadCommittedClusteredSoftLock(EhcacheTxnsClusteredStateFacade facade, ReadCommittedClusteredSoftLockFactory factory,
                                 SoftLockId softLockId) {
    this.facade = facade;
    this.factory = factory;
    this.cacheManagerName = factory.getCacheManagerName();
    this.cacheName = factory.getCacheName();
    this.softLockId = softLockId;
    this.lock = facade.getSoftLockWriteLock(cacheManagerName, cacheName, softLockId);
    this.freezeLock = facade.getSoftLockFreezeLock(cacheManagerName, cacheName, softLockId);
    this.notificationLock = facade.getSoftLockNotifierLock(cacheManagerName, cacheName, softLockId);
    this.notifier = notificationLock.writeLock().getCondition();
  }

  public SoftLockId getSoftLockId() {
    return softLockId;
  }

  public Object getKey() {
    return softLockId.getKey();
  }

  public Element getElement(TransactionID currentTransactionId) {
    freezeLock.readLock().lock();
    try {
      SoftLockState state = facade.getSoftLockState(cacheManagerName, cacheName, softLockId);
      if (softLockId.getTransactionId().equals(currentTransactionId)) {
        return state.getNewElement();
      } else {
        return state.getOldElement();
      }
    } finally {
      freezeLock.readLock().unlock();
    }
  }

  public Element updateElement(Element newElement) {
    return facade.updateSoftLockState(cacheManagerName, cacheName, softLockId, newElement);
  }

  public TransactionID getTransactionID() {
    return softLockId.getTransactionId();
  }

  public boolean wasPinned() {
    SoftLockState softLockState = facade.getSoftLockState(cacheManagerName, cacheName, softLockId);
    return softLockState.isPinned();
  }

  public void lock() {
    lock.writeLock().lock();

    if (isExpired()) {
      notificationLock.writeLock().lock();
      try {
        notifier.signalAll();
      } finally {
        notificationLock.writeLock().unlock();
      }
    }
  }

  public synchronized boolean isExpired() {
    if (!expired) {
      // calling isFrozen or isLocked will un-expire the lock so this state must be remembered
      expired = facade.isExpired(cacheManagerName, cacheName, softLockId);
    }
    return expired;
  }

  public boolean tryLock(long ms) throws InterruptedException {
    if (isExpired() && factory.getLock(softLockId) != null) {
      notificationLock.writeLock().lock();
      try {
        while (!isLocked()) {
          boolean canLock = notifier.await(ms, TimeUnit.MILLISECONDS);
          if (!canLock) { return false; }
        }
      } finally {
        notificationLock.writeLock().unlock();
      }
    }
    return lock.writeLock().tryLock(ms, TimeUnit.MILLISECONDS);
  }

  public void clearTryLock() {
    lock.writeLock().unlock();
  }

  public void unlock() {
    lock.writeLock().unlock();
    clear();
  }

  boolean isLocked() {
    return facade.isLocked(lock);
  }

  public void freeze() {
    if (!isLocked()) { throw new IllegalStateException("cannot freeze an unlocked soft lock"); }
    freezeLock.writeLock().lock();
  }

  public void unfreeze() {
    freezeLock.writeLock().unlock();
  }

  public Element getFrozenElement() {
    if (!isFrozen()) { throw new IllegalStateException(
                                                       "cannot get frozen element of a soft lock which hasn't been frozen or hasn't expired"); }

    SoftLockState softLockState = facade.getSoftLockState(cacheManagerName, cacheName, softLockId);
    if (softLockState == null) { throw new AssertionError("Soft lock state not present for : " + this); }
    if (softLockId.getTransactionId().isDecisionCommit()) {
      return softLockState.getNewElement();
    } else {
      return softLockState.getOldElement();
    }
  }

  public void clear() {
    factory.clearSoftLock(this);
  }

  private boolean isFrozen() {
    return facade.isLocked(freezeLock);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof ReadCommittedClusteredSoftLock) {
      ReadCommittedClusteredSoftLock other = (ReadCommittedClusteredSoftLock) object;
      return softLockId.equals(other.softLockId);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return softLockId.hashCode();
  }

  private Object writeReplace() {
    return new ReadCommittedClusteredSoftLockSerializedForm(cacheManagerName, cacheName, softLockId);
  }

  @Override
  public String toString() {
    SoftLockState softLockState = facade.getSoftLockState(cacheManagerName, cacheName, softLockId);
    return "Soft Lock [clustered: true, isolation: rc, transactionID: "
           + softLockId.getTransactionId()
           + ", key: "
           + softLockId.getKey()
           + (softLockState != null ? ", newElement: " + softLockState.getNewElement()
               : " <SoftLockState not present, probably already committed transaction>") + "]";
  }

  /**
   * ReadCommittedClusteredSoftLock serialized form
   */
  private static final class ReadCommittedClusteredSoftLockSerializedForm implements Serializable {

    private final String     cacheManagerName;
    private final String     cacheName;
    private final SoftLockId softLockId;

    private ReadCommittedClusteredSoftLockSerializedForm(String cacheManagerName, String cacheName,
                                                         SoftLockId softLockId) {
      this.cacheManagerName = cacheManagerName;
      this.cacheName = cacheName;
      this.softLockId = softLockId;
    }

    private Object readResolve() {
      for (int i = 0; i < CacheManager.ALL_CACHE_MANAGERS.size(); i++) {
        CacheManager cacheManager = CacheManager.ALL_CACHE_MANAGERS.get(i);
        if (cacheManager.getName().equals(cacheManagerName)) {
          try {
            ReadCommittedClusteredSoftLockFactory softLockFactory = (ReadCommittedClusteredSoftLockFactory) cacheManager
                .getSoftLockFactory(cacheName);
            return softLockFactory.getLock(softLockId);
          } catch (CacheException ce) {
            throw new TransactionException("cannot deserialize SoftLock from cache " + cacheName
                                           + " as the cache cannot be found in cache manager " + cacheManagerName);
          }
        }
      }
      throw new TransactionException("unable to find referent clustered SoftLock in " + cacheManagerName + " "
                                     + cacheName + " for key [" + softLockId.getKey() + "] under transaction "
                                     + softLockId.getTransactionId());
    }

  }

}
