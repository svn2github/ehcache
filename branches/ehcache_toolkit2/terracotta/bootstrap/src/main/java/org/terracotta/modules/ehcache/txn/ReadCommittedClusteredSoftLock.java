/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.txn;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.SoftLockID;
import net.sf.ehcache.transaction.TransactionException;
import net.sf.ehcache.transaction.TransactionID;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @author Ludovic Orban
 */
public class ReadCommittedClusteredSoftLock implements SoftLock, Serializable {

  private final ReadCommittedClusteredSoftLockFactory factory;
  private final TransactionID                         transactionID;
  private volatile transient Object                   deserializedKey;
  private final ToolkitLock                           lock;
  private final ToolkitReadWriteLock                  freezeLock;
  private final ToolkitReadWriteLock                  notificationLock;
  private final Condition                             notifier;
  private boolean                                     expired;
  private final String                                cacheManagerName;
  private final String                                cacheName;


  ReadCommittedClusteredSoftLock(ToolkitInstanceFactory toolkitInstanceFactory,
                                 ReadCommittedClusteredSoftLockFactory factory,
                                 TransactionID transactionID, Object key) {
    this.factory = factory;
    this.cacheManagerName = factory.getCacheManagerName();
    this.deserializedKey = key;
    this.cacheName = factory.getCacheName();
    this.transactionID = transactionID;
    this.lock = toolkitInstanceFactory.getSoftLockWriteLock(cacheManagerName, cacheName, transactionID);
    this.freezeLock = toolkitInstanceFactory.getSoftLockFreezeLock(cacheManagerName, cacheName, transactionID);
    this.notificationLock = toolkitInstanceFactory.getSoftLockNotifierLock(cacheManagerName, cacheName, transactionID);
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

  private Object writeReplace() {
    return new ReadCommittedClusteredSoftLockSerializedForm(cacheManagerName, cacheName, transactionID, deserializedKey);
  }

  /**
   * ReadCommittedClusteredSoftLock serialized form
   */
  private static final class ReadCommittedClusteredSoftLockSerializedForm implements Serializable {

    private final String     cacheManagerName;
    private final String     cacheName;
    private final TransactionID transactionID;
    private final Object        key;

    private ReadCommittedClusteredSoftLockSerializedForm(String cacheManagerName, String cacheName,
                                                         TransactionID transactionID, Object key) {
      this.cacheManagerName = cacheManagerName;
      this.cacheName = cacheName;
      this.transactionID = transactionID;
      this.key = key;
    }

    private Object readResolve() {
      for (int i = 0; i < CacheManager.ALL_CACHE_MANAGERS.size(); i++) {
        CacheManager cacheManager = CacheManager.ALL_CACHE_MANAGERS.get(i);
        if (cacheManager.getName().equals(cacheManagerName)) {
          try {

            ReadCommittedClusteredSoftLockFactory softLockFactory = (ReadCommittedClusteredSoftLockFactory) cacheManager
                .createSoftLockManager(cacheManager
                .getCache(cacheName));
            return softLockFactory.resolveLock(transactionID, key);
          } catch (CacheException ce) {
            throw new TransactionException("cannot deserialize SoftLock from cache " + cacheName
                                           + " as the cache cannot be found in cache manager " + cacheManagerName);
          }
        }
      }
      throw new TransactionException("unable to find referent clustered SoftLock in " + cacheManagerName + " "
                                     + cacheName + " for key [" + key + "] under transaction " + transactionID);
    }

  }

}
