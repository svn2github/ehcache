/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.SoftLockID;
import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @author Ludovic Orban
 */
public class ReadCommittedClusteredSoftLock implements SoftLock {

  private final TransactionID                         transactionID;
  private final Object                                deserializedKey;
  private final ReadCommittedClusteredSoftLockFactory factory;
  private final ToolkitLock                           lock;
  private final ToolkitReadWriteLock                  freezeLock;
  private final ToolkitReadWriteLock                  notificationLock;
  private final Condition                             notifier;
  private boolean                                     expired;

  ReadCommittedClusteredSoftLock(ToolkitInstanceFactory toolkitInstanceFactory,
                                 ReadCommittedClusteredSoftLockFactory factory, TransactionID transactionID, Object key) {
    this.deserializedKey = key;
    this.transactionID = transactionID;
    this.factory = factory;
    String cacheManagerName = factory.getCacheManagerName();
    String cacheName = factory.getCacheName();
    this.lock = toolkitInstanceFactory
        .getSoftLockWriteLock(cacheManagerName, cacheName, transactionID, deserializedKey);
    this.freezeLock = toolkitInstanceFactory.getSoftLockFreezeLock(cacheManagerName, cacheName, transactionID,
                                                                   deserializedKey);
    this.notificationLock = toolkitInstanceFactory.getSoftLockNotifierLock(cacheManagerName, cacheName, transactionID,
                                                                           deserializedKey);
    this.notifier = notificationLock.writeLock().getCondition();
  }

  @Override
  public Object getKey() {
    return deserializedKey;
  }

  @Override
  public Element getElement(TransactionID currentTransactionId, SoftLockID softLockId) {
    freezeLock.readLock().lock();
    try {
      if (transactionID.equals(currentTransactionId)) {
        return softLockId.getNewElement();
      } else {
        return softLockId.getOldElement();
      }
    } finally {
      freezeLock.readLock().unlock();
    }
  }

  public TransactionID getTransactionID() {
    return transactionID;
  }

  @Override
  public void lock() {
    lock.lock();

    if (isExpired()) {
      notificationLock.writeLock().lock();
      try {
        notifier.signalAll();
      } finally {
        notificationLock.writeLock().unlock();
      }
    }
  }

  @Override
  public boolean tryLock(long ms) throws InterruptedException {
    if (isExpired() && factory.getLock(transactionID, getKey()) != null) {
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
    return lock.tryLock(ms, TimeUnit.MILLISECONDS);
  }

  @Override
  public void clearTryLock() {
    lock.unlock();
  }

  @Override
  public void unlock() {
    lock.unlock();
    clear();
  }

  boolean isLocked() {
    return isLocked(lock);
  }

  @Override
  public void freeze() {
    if (!isLocked()) { throw new IllegalStateException("cannot freeze an unlocked soft lock"); }
    freezeLock.writeLock().lock();
  }

  @Override
  public void unfreeze() {
    freezeLock.writeLock().unlock();
  }

  @Override
  public synchronized boolean isExpired() {
    if (!expired) {
      // calling isFrozen or isLocked will un-expire the lock so this state must be remembered
      expired = !isFrozen() && !isLocked();
    }
    return expired;
  }

  public void clear() {
    factory.clearSoftLock(this);
  }

  private boolean isFrozen() {
    return isLocked(freezeLock.writeLock());
  }

  private static boolean isLocked(ToolkitLock lock) {
    if (lock.isHeldByCurrentThread()) { return true; }
    // tryLock may return false although the lock is not held but was locked and unlocked by another L1
    // which keeps the lock greedily. That's okay because we're just interested to know if a lock was
    // released prematurely because of a L1 crash in which case tryLock will return true
    boolean gotLock = lock.tryLock();
    if (gotLock) {
      lock.unlock();
      return false;
    }
    return true;
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof ReadCommittedClusteredSoftLock) {
      ReadCommittedClusteredSoftLock other = (ReadCommittedClusteredSoftLock) object;

      if (!transactionID.equals(other.transactionID)) { return false; }
      if (!getKey().equals(other.getKey())) { return false; }

      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hashCode = 31;

    hashCode *= transactionID.hashCode();
    hashCode *= getKey().hashCode();

    return hashCode;
  }

  @Override
  public String toString() {
    return "Soft Lock [clustered: true, isolation: rc, transactionID: " + transactionID + ", key: " + getKey() + "]";
  }

}
