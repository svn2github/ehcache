/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.txn;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.transaction.SoftLock;
import net.sf.ehcache.transaction.SoftLockID;
import net.sf.ehcache.transaction.SoftLockManager;
import net.sf.ehcache.transaction.TransactionID;
import net.sf.ehcache.transaction.local.LocalTransactionContext;

import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class ReadCommittedClusteredSoftLockFactory implements SoftLockManager {
  private final String                                                             cacheName;
  private final String                                                             cacheManagerName;

  private final ToolkitInstanceFactory                                             toolkitInstanceFactory;

  // actually all we need would be a ConcurrentSet...
  private final ToolkitList<ReadCommittedClusteredSoftLock>                        newKeyLocks;

  // locks must be inserted in a clustered collection b/c they must be managed by the L1 before they are returned
  private final ToolkitMap<ClusteredSoftLockIDKey, ReadCommittedClusteredSoftLock> allLocks;

  public ReadCommittedClusteredSoftLockFactory(ToolkitInstanceFactory toolkitInstanceFactory, String cacheManagerName,
                                               String cacheName) {
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.cacheManagerName = cacheManagerName;
    this.cacheName = cacheName;
    allLocks = toolkitInstanceFactory.getOrCreateAllSoftLockMap(cacheManagerName, cacheName);
    newKeyLocks = toolkitInstanceFactory.getOrCreateNewSoftLocksSet(cacheManagerName, cacheName);
  }

  @Override
  public SoftLock createSoftLock(SoftLockID softLockId) {
    ReadCommittedClusteredSoftLock softLock = new ReadCommittedClusteredSoftLock(toolkitInstanceFactory, this,
                                                                                 softLockId.getTransactionID(),
                                                                                 softLockId.getKey());

    allLocks.put(new ClusteredSoftLockIDKey(softLockId), softLock);

    if (softLockId.getOldElement() == null) {
      newKeyLocks.add(softLock);
    }
    return softLock;
  }

  @Override
  public SoftLockID createSoftLockID(TransactionID transactionID, Object key, Element newElement, Element oldElement,
                                     boolean pinned) {
    if (newElement != null && newElement.getObjectValue() instanceof SoftLockID) { throw new AssertionError(
                                                                                                            "newElement must not contain a soft lock ID"); }
    if (oldElement != null && oldElement.getObjectValue() instanceof SoftLockID) { throw new AssertionError(
                                                                                                            "oldElement must not contain a soft lock ID"); }

    return new SoftLockID(transactionID, key, newElement, oldElement, pinned);
  }

  @Override
  public SoftLock findSoftLockById(SoftLockID softLockId) {
    ReadCommittedClusteredSoftLock softLock = allLocks.get(new ClusteredSoftLockIDKey(softLockId));
    softLock.initializeTransients(toolkitInstanceFactory, this);
    return softLock;
  }

  ReadCommittedClusteredSoftLock getLock(TransactionID transactionId, Object key) {

    for (Map.Entry<ClusteredSoftLockIDKey, ReadCommittedClusteredSoftLock> entry : allLocks.entrySet()) {
      ReadCommittedClusteredSoftLock readCommittedSoftLock = allLocks.get(entry.getKey()); // workaround for DEV-5390
      readCommittedSoftLock.initializeTransients(toolkitInstanceFactory, this);
      if (readCommittedSoftLock.getTransactionID().equals(transactionId) && readCommittedSoftLock.getKey().equals(key)) {
        readCommittedSoftLock.initializeTransients(toolkitInstanceFactory, this);
        return readCommittedSoftLock;
      }
    }
    return null;
  }

  @Override
  public Set<Object> getKeysInvisibleInContext(LocalTransactionContext currentTransactionContext, Store underlyingStore) {
    Set<Object> invisibleKeys = new HashSet<Object>();

    // all new keys added into the store are invisible
    invisibleKeys.addAll(getNewKeys());

    List<SoftLock> currentTransactionContextSoftLocks = currentTransactionContext.getSoftLocksForCache(cacheName);
    for (SoftLock softLock : currentTransactionContextSoftLocks) {
      SoftLockID softLockId = (SoftLockID) underlyingStore.getQuiet(softLock.getKey()).getObjectValue();

      if (softLock.getElement(currentTransactionContext.getTransactionId(), softLockId) == null) {
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
  @Override
  public synchronized Set<TransactionID> collectAllLiveTransactionIDs() {
    Set<TransactionID> result = new HashSet<TransactionID>();

    for (Map.Entry<ClusteredSoftLockIDKey, ReadCommittedClusteredSoftLock> entry : allLocks.entrySet()) {
      ReadCommittedClusteredSoftLock softLock = allLocks.get(entry.getKey()); // workaround for DEV-5390
      if (!softLock.isExpired()) {
        result.add(softLock.getTransactionID());
      }
    }

    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<SoftLock> collectAllSoftLocksForTransactionID(TransactionID transactionID) {
    Set<SoftLock> result = new HashSet<SoftLock>();

    for (Map.Entry<ClusteredSoftLockIDKey, ReadCommittedClusteredSoftLock> entry : allLocks.entrySet()) {
      ReadCommittedClusteredSoftLock softLock = allLocks.get(entry.getKey()); // workaround for DEV-5390
      if (softLock.getTransactionID().equals(transactionID)) {
        softLock.initializeTransients(toolkitInstanceFactory, this);
        result.add(softLock);
      }
    }

    return result;
  }

  @Override
  public void clearSoftLock(SoftLock softLock) {
    newKeyLocks.remove(softLock);

    for (Map.Entry<ClusteredSoftLockIDKey, ReadCommittedClusteredSoftLock> entry : allLocks.entrySet()) {
      if (entry.getValue().equals(softLock)) {
        allLocks.remove(entry.getKey());
        break;
      }
    }
  }

  private Set<Object> getNewKeys() {
    Set<Object> result = new HashSet<Object>();
    int i = 0;
    for (ReadCommittedClusteredSoftLock softLock : newKeyLocks) {
      newKeyLocks.get(i); // workaround for DEV-5390
      result.add(softLock.getKey());
    }

    return result;
  }

  String getCacheName() {
    return cacheName;
  }

  String getCacheManagerName() {
    return cacheManagerName;
  }

}
