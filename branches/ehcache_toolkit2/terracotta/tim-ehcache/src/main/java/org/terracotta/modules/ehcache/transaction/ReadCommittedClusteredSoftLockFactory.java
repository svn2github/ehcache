/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.Element;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.SoftLockFactory;
import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.local.LocalTransactionContext;
import org.terracotta.collections.ConcurrentDistributedMap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Ludovic Orban
 */
public class ReadCommittedClusteredSoftLockFactory implements SoftLockFactory {

  private final static Object MARKER = new Object();

  private final String cacheName;
  private final String cacheManagerName;

  // actually all we need would be a ConcurrentSet...
  private final ConcurrentMap<ReadCommittedClusteredSoftLock, Object> newKeyLocks = new ConcurrentDistributedMap<ReadCommittedClusteredSoftLock, Object>();

  // locks must be inserted in a clustered collection b/c they must be managed by the L1 before they are returned
  private final ConcurrentMap<ReadCommittedClusteredSoftLock, Object> allLocks = new ConcurrentDistributedMap<ReadCommittedClusteredSoftLock, Object>();


  public ReadCommittedClusteredSoftLockFactory(String cacheManagerName, String cacheName) {
    this.cacheManagerName = cacheManagerName;
    this.cacheName = cacheName;
  }

  String getCacheName() {
    return cacheName;
  }

  String getCacheManagerName() {
    return cacheManagerName;
  }

  public SoftLock createSoftLock(TransactionID transactionID, Object key, Element newElement, Element oldElement, boolean pinned) {
    ReadCommittedClusteredSoftLock softLock = new ReadCommittedClusteredSoftLock(this, transactionID, key, newElement, oldElement, pinned);

    allLocks.put(softLock, MARKER);

    if (oldElement == null) {
      newKeyLocks.put(softLock, MARKER);
    }
    return softLock;
  }

  ReadCommittedClusteredSoftLock getLock(TransactionID transactionId, Object key) {
    for (ReadCommittedClusteredSoftLock readCommittedSoftLock : allLocks.keySet()) {
      if (readCommittedSoftLock.getTransactionID().equals(transactionId) &&
          readCommittedSoftLock.getKey().equals(key)) {
        return readCommittedSoftLock;
      }
    }
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
    Set<TransactionID> result = new HashSet<TransactionID>();

    for (ReadCommittedClusteredSoftLock softLock : allLocks.keySet()) {
      allLocks.get(softLock); //workaround for DEV-5390
      if (softLock.isExpired()) {
        result.add(softLock.getTransactionID());
      }
    }

    return result;
  }

  /**
   * {@inheritDoc}
   */
  public Set<SoftLock> collectAllSoftLocksForTransactionID(TransactionID transactionID) {
    Set<SoftLock> result = new HashSet<SoftLock>();

    for (ReadCommittedClusteredSoftLock softLock : allLocks.keySet()) {
      allLocks.get(softLock); //workaround for DEV-5390
      if (softLock.getTransactionID().equals(transactionID)) {
        result.add(softLock);
      }
    }

    return result;
  }

  void clearSoftLock(ReadCommittedClusteredSoftLock softLock) {
    newKeyLocks.remove(softLock);

    allLocks.remove(softLock);
  }

  private Set<Object> getNewKeys() {
    Set<Object> result = new HashSet<Object>();

    for (ReadCommittedClusteredSoftLock softLock : newKeyLocks.keySet()) {
      newKeyLocks.get(softLock); //workaround for DEV-5390
      result.add(softLock.getKey());
    }

    return result;
  }
}
