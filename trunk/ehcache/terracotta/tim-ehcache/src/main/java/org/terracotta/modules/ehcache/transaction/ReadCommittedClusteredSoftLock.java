/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.SoftLockID;
import net.sf.ehcache.transaction.TransactionID;
import org.terracotta.locking.TerracottaReadWriteLock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @author Ludovic Orban
 */
public class ReadCommittedClusteredSoftLock implements SoftLock {

  private final ReadCommittedClusteredSoftLockFactory factory;
  private final TransactionID                         transactionID;
  private final byte[]                                key;
  private volatile transient Object                   deserializedKey;
  private final TerracottaReadWriteLock               lock;
  private final TerracottaReadWriteLock               freezeLock;
  private final TerracottaReadWriteLock               notificationLock;
  private final Condition                             notifier;
  private boolean                                     expired;

  ReadCommittedClusteredSoftLock(ReadCommittedClusteredSoftLockFactory factory, TransactionID transactionID, Object key) {
    this.factory = factory;
    this.transactionID = transactionID;
    this.key = serialize(key);
    this.deserializedKey = key;
    this.lock = new TerracottaReadWriteLock();
    this.freezeLock = new TerracottaReadWriteLock();
    this.notificationLock = new TerracottaReadWriteLock();
    this.notifier = notificationLock.writeLock().newCondition();
  }

  public Object getKey() {
    if (deserializedKey == null) {
      deserializedKey = deserialize(key);
    }
    return deserializedKey;
  }

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
    return isLocked(lock);
  }

  public void freeze() {
    if (!isLocked()) { throw new IllegalStateException("cannot freeze an unlocked soft lock"); }
    freezeLock.writeLock().lock();
  }

  public void unfreeze() {
    freezeLock.writeLock().unlock();
  }

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
    return isLocked(freezeLock);
  }

  private static boolean isLocked(TerracottaReadWriteLock lock) {
    if (lock.isWriteLockedByCurrentThread()) { return true; }
    // tryLock may return false although the lock is not held but was locked and unlocked by another L1
    // which keeps the lock greedily. That's okay because we're just interested to know if a lock was
    // released prematurely because of a L1 crash in which case tryLock will return true
    boolean gotLock = lock.writeLock().tryLock();
    if (gotLock) {
      lock.writeLock().unlock();
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

  private static byte[] serialize(Object obj) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("error serializing " + obj);
    }
  }

  private static Object deserialize(byte[] bytes) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bais);
      Object obj = ois.readObject();
      ois.close();
      return obj;
    } catch (Exception e) {
      throw new RuntimeException("error deserializing " + bytes);
    }
  }

}
