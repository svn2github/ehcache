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

import org.terracotta.locking.TerracottaReadWriteLock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @author Ludovic Orban
 */
@IgnoreSizeOf
public class ReadCommittedClusteredSoftLock implements SoftLock {

  private final ReadCommittedClusteredSoftLockFactory factory;
  private final String                                cacheManagerName;
  private final String                                cacheName;
  private final TransactionID                         transactionID;
  private final boolean                               pinned;
  private final byte[]                                key;
  private byte[]                                      newElement;
  private final byte[]                                oldElement;
  private final TerracottaReadWriteLock               lock;
  private final TerracottaReadWriteLock               freezeLock;
  private final TerracottaReadWriteLock               notificationLock;
  private final Condition                             notifier;
  private boolean                                     expired;

  ReadCommittedClusteredSoftLock(ReadCommittedClusteredSoftLockFactory factory, TransactionID transactionID,
                                 Object key, Element newElement, Element oldElement, boolean pinned) {
    this.factory = factory;
    this.cacheManagerName = factory.getCacheManagerName();
    this.cacheName = factory.getCacheName();
    this.transactionID = transactionID;
    this.pinned = pinned;
    this.key = serialize(key);
    this.newElement = serialize(newElement);
    this.oldElement = serialize(oldElement);
    this.lock = new TerracottaReadWriteLock();
    this.freezeLock = new TerracottaReadWriteLock();
    this.notificationLock = new TerracottaReadWriteLock();
    this.notifier = notificationLock.writeLock().newCondition();
  }

  public Object getKey() {
    return deserialize(key);
  }

  public Element getElement(TransactionID currentTransactionId) {
    freezeLock.readLock().lock();
    try {
      if (transactionID.equals(currentTransactionId)) {
        return (Element) deserialize(newElement);
      } else {
        return (Element) deserialize(oldElement);
      }
    } finally {
      freezeLock.readLock().unlock();
    }
  }

  public Element updateElement(Element e) {
    Element prev = (Element) deserialize(this.newElement);
    this.newElement = serialize(e);
    return prev;
  }

  public TransactionID getTransactionID() {
    return transactionID;
  }

  public boolean wasPinned() {
    return pinned;
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
    if (isExpired() && factory.getLock(transactionID, deserialize(key)) != null) {
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

  @Override
  public Element getOldElement() {
    if (!isFrozen()) { throw new IllegalStateException("cannot get frozen element of a soft lock which hasn't been frozen or hasn't expired"); }
    return (Element) deserialize(oldElement);
  }

  @Override
  public Element getNewElement() {
    if (!isFrozen()) { throw new IllegalStateException("cannot get frozen element of a soft lock which hasn't been frozen or hasn't expired"); }
    return (Element) deserialize(newElement);
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
      if (!Arrays.equals(key, other.key)) { return false; }

      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hashCode = 31;

    hashCode *= transactionID.hashCode();
    hashCode *= Arrays.hashCode(key);

    return hashCode;
  }

  private Object writeReplace() {
    return new ReadCommittedClusteredSoftLockSerializedForm(cacheManagerName, cacheName, transactionID, key);
  }

  @Override
  public String toString() {
    return "Soft Lock [clustered: true, isolation: rc, transactionID: " + transactionID + ", key: " + deserialize(key)
           + ", newElement: " + deserialize(newElement) + "]";
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

  /**
   * ReadCommittedClusteredSoftLock serialized form
   */
  private static final class ReadCommittedClusteredSoftLockSerializedForm implements Serializable {

    private final String        cacheManagerName;
    private final String        cacheName;
    private final TransactionID transactionID;
    private final byte[]        key;

    private ReadCommittedClusteredSoftLockSerializedForm(String cacheManagerName, String cacheName,
                                                         TransactionID transactionID, byte[] key) {
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
            ReadCommittedClusteredSoftLockFactory softLockFactory = (ReadCommittedClusteredSoftLockFactory)cacheManager
                .getSoftLockFactory(cacheName);
            return softLockFactory.getLock(transactionID, deserialize(key));
          } catch (CacheException ce) {
            throw new TransactionException("cannot deserialize SoftLock from cache " + cacheName +
                                           " as the cache cannot be found in cache manager " + cacheManagerName);
          }
        }
      }
      throw new TransactionException("unable to find referent clustered SoftLock in " + cacheManagerName + " "
                                     + cacheName + " for key [" + deserialize(key) + "] under transaction "
                                     + transactionID);
    }

  }

}
