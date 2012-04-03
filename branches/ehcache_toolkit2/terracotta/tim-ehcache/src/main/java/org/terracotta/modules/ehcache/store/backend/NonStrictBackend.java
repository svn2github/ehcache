/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.backend;

import net.sf.ehcache.Element;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;
import net.sf.ehcache.store.ElementValueComparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.cache.TimestampedValue;
import org.terracotta.cluster.TerracottaProperties;
import org.terracotta.locking.ClusteredLock;
import org.terracotta.locking.LockType;
import org.terracotta.locking.TerracottaLock;
import org.terracotta.meta.MetaData;
import org.terracotta.modules.ehcache.coherence.CacheCoherence;
import org.terracotta.modules.ehcache.store.ClusteredElementEvictionData;
import org.terracotta.modules.ehcache.store.ClusteredStore;
import org.terracotta.modules.ehcache.store.ClusteredStore.SyncLockState;
import org.terracotta.modules.ehcache.store.ClusteredStoreBackend;
import org.terracotta.modules.ehcache.store.UnmodifiableCollectionWrapper;
import org.terracotta.modules.ehcache.store.ValueModeHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Abhishek Sanoujam
 */
public class NonStrictBackend implements BackendStore {

  // undocumented sys-prop name for number of concurrent locks
  private static final String                         EHCACHE_NON_STRICT_CONCURRENT_LOCKS_SIZE_PROPERTY = "ehcache.coherence.nonstrict.concurrentLocksSize";
  private static final String                         EHCAHE_BULKOPS_MAX_KB_SIZE                        = "ehcache.bulkOps.maxKBSize";
  private static final String                         EHCACHE_GETALL_BATCH_SIZE_PROPERTY                = "ehcache.getAll.batchSize";
  private static final int                            DEFAULT_GETALL_BATCH_SIZE                         = 1000;

  private static final int                            DEFAULT_EHCACHE_NON_STRICT_CONCURRENT_LOCKS_SIZE  = 256;
  private static final int                            EHCACHE_NON_STRICT_CONCURRENT_LOCKS_SIZE          = Integer
                                                                                                            .getInteger(EHCACHE_NON_STRICT_CONCURRENT_LOCKS_SIZE_PROPERTY,
                                                                                                                        DEFAULT_EHCACHE_NON_STRICT_CONCURRENT_LOCKS_SIZE);
  private static final int                            KB                                                = 1024;

  private static int                                  BULK_OPS_KB_SIZE                                  = getTerracottaProperty(EHCAHE_BULKOPS_MAX_KB_SIZE,
                                                                                                                                KB)
                                                                                                          * KB;
  private static int                                  GETALL_BATCH_SIZE                                 = getTerracottaProperty(EHCACHE_GETALL_BATCH_SIZE_PROPERTY,
                                                                                                                                DEFAULT_GETALL_BATCH_SIZE);
  private static final Logger                         LOG                                               = LoggerFactory
                                                                                                            .getLogger(NonStrictBackend.class
                                                                                                                .getName());

  private final ClusteredStoreBackend<Object, Object> actualBackend;
  private final ValueModeHandler                      valueModeHandler;
  private final ThreadLocal<SyncLockState>            synclockstate;
  private final ClusteredStore                        clusteredStore;
  private final CacheCoherence                        cacheCoherence;
  private static final TerracottaLock[]               concurrentLocks;
  private final SizeOfEngine                          sizeOfEngine;

  private static int getTerracottaProperty(String propName, int defaultValue) {
    try {
      return new TerracottaProperties().getInteger(propName, defaultValue);
    } catch (UnsupportedOperationException e) {
      // for unit-tests
      return defaultValue;
    }
  }

  static {
    concurrentLocks = new TerracottaLock[EHCACHE_NON_STRICT_CONCURRENT_LOCKS_SIZE];
    for (int i = 0; i < concurrentLocks.length; i++) {
      concurrentLocks[i] = new TerracottaLock("nonstrict-concurrent-lock-" + i, LockType.CONCURRENT);
    }
  }

  public NonStrictBackend(ClusteredStore clusteredStore, final ClusteredStoreBackend<Object, Object> actualBackend,
                          final ValueModeHandler valueModeHandler, final ThreadLocal<SyncLockState> synclockstate,
                          CacheCoherence cacheCoherence, int maxSizeOfDepth, boolean throwExceptionWhenMaxDepthReached) {
    this.clusteredStore = clusteredStore;
    this.actualBackend = actualBackend;
    this.valueModeHandler = valueModeHandler;
    this.synclockstate = synclockstate;
    this.cacheCoherence = cacheCoherence;
    this.sizeOfEngine = new DefaultSizeOfEngine(maxSizeOfDepth, throwExceptionWhenMaxDepthReached);

    if (BULK_OPS_KB_SIZE <= 0) {
      // default is 1 MB
      BULK_OPS_KB_SIZE = KB * KB;
      LOG.info("For bulk operations data will be transferred approximately in a batch of " + BULK_OPS_KB_SIZE
               + " Bytes.");
    }
  }

  private TerracottaLock getConcurrentLock(Object key) {
    return concurrentLocks[Math.abs(key.hashCode() % EHCACHE_NON_STRICT_CONCURRENT_LOCKS_SIZE)];
  }

  public void putNoReturn(Object portableKey, TimestampedValue value, MetaData searchMetaData) {
    if (synclockstate.get().isLocked()) {
      actualBackend.putNoReturn(portableKey, value, searchMetaData);
    } else {
      TerracottaLock concurrentLock = getConcurrentLock(portableKey);
      concurrentLock.lock();
      try {
        actualBackend.unlockedPutNoReturn(portableKey, value, searchMetaData);
      } finally {
        concurrentLock.unlock();
      }
    }
  }

  public void putAllNoReturn(Collection<Element> elements) {
    Iterator<Element> iter = elements.iterator();
    while (iter.hasNext()) {
      cacheCoherence.acquireReadLock();
      Set<ClusteredElement> batchedElements = new HashSet<ClusteredElement>();
      try {
        long currentByteSize = 0;
        while (currentByteSize < BULK_OPS_KB_SIZE && iter.hasNext()) {
          Element element = iter.next();
          Object portableKey = this.clusteredStore.generatePortableKeyFor(element.getObjectKey());
          MetaData searchMetaData = this.clusteredStore.createPutSearchMetaData(portableKey, element);
          TimestampedValue value = valueModeHandler.createTimestampedValue(element);
          ClusteredElement clusteredElement = new ClusteredElement(element.getObjectKey(), portableKey, searchMetaData,
                                                                   value);
          batchedElements.add(clusteredElement);
          currentByteSize += getElementSize(clusteredElement, false);
          element.setElementEvictionData(new ClusteredElementEvictionData(this.clusteredStore, value));
        }
        doPutAll(batchedElements);
      } finally {
        cacheCoherence.releaseReadLock();
      }
    }
  }

  private void doPutAll(Set<ClusteredElement> elements) {
    if (synclockstate.get().isLocked()) {
      for (ClusteredElement element : elements) {
        actualBackend.putNoReturn(element.getPortableKey(), element.getTimeStampedValue(), element.getSearchMetaData());
      }
    } else {
      TerracottaLock concurrentLock = getConcurrentLock(elements.iterator().next().getPortableKey());
      concurrentLock.lock();
      try {
        for (ClusteredElement element : elements) {
          actualBackend.unlockedPutNoReturn(element.getPortableKey(), element.getTimeStampedValue(),
                                            element.getSearchMetaData());
        }
      } finally {
        concurrentLock.unlock();
      }
    }

  }

  private long getElementSize(ClusteredElement clusteredElement, boolean computeMetaDataSize) {
    return sizeOfEngine.sizeOf(clusteredElement.getPortableKey(), clusteredElement.getTimeStampedValue(), null)
        .getCalculated();
  }

  public Element get(Object actualKey, Object portableKey, boolean quiet) {
    if (synclockstate.get().isLocked()) {
      if (quiet) {
        return valueModeHandler.createElement(actualKey, actualBackend.getTimestampedValueQuiet(portableKey));
      } else {
        return valueModeHandler.createElement(actualKey, actualBackend.getTimestampedValue(portableKey));
      }
    } else {
      return valueModeHandler.createElement(actualKey, actualBackend.unlockedGetTimestampedValue(portableKey, quiet));
    }
  }

  public Map<Object, Element> getAll(Collection<?> keys, boolean quiet) {
    return new GetAllCustomMap(keys, this, quiet, GETALL_BATCH_SIZE);
  }

  public void getAllInternal(Collection<Object> keys, boolean quiet, Map<Object, Element> rv) {
    Map<Object, Object> portableKeyActualKeyMap = new HashMap<Object, Object>();
    for (Object actualKey : keys) {
      portableKeyActualKeyMap.put(this.clusteredStore.generatePortableKeyFor(actualKey), actualKey);
    }
    this.cacheCoherence.acquireReadLock();
    try {
      Map<Object, TimestampedValue<Object>> valueMap;
      if (synclockstate.get().isLocked()) {
        valueMap = actualBackend.getTimestampedValues(portableKeyActualKeyMap.keySet(), quiet);
      } else {
        valueMap = actualBackend.unlockedGetAllTimeStampedValues(portableKeyActualKeyMap.keySet(), quiet);
      }
      for (Entry<Object, TimestampedValue<Object>> entry : valueMap.entrySet()) {
        Object actualKey = portableKeyActualKeyMap.get(entry.getKey());
        rv.put(actualKey, valueModeHandler.createElement(actualKey, entry.getValue()));
      }
    } finally {
      this.cacheCoherence.releaseReadLock();
    }
  }

  public Element unlockedGet(Object actualKey, Object portableKey, boolean quiet) {
    return valueModeHandler.createElement(actualKey, actualBackend.unlockedGetTimestampedValue(portableKey, quiet));
  }

  public Element unsafeGet(Object actualKey, Object portableKey, boolean quiet) {
    return valueModeHandler.createElement(actualKey, actualBackend.unsafeGetTimestampedValue(portableKey, quiet));
  }

  public Element remove(Object actualKey, Object portableKey, MetaData metaData) {
    final boolean externalLock = synclockstate.get().isLocked();
    TerracottaLock concurrentLock = null;
    if (!externalLock) {
      concurrentLock = getConcurrentLock(portableKey);
      concurrentLock.lock();
    }
    try {
      TimestampedValue timestampedValue = actualBackend.unlockedGetTimestampedValue(portableKey, true);
      actualBackend.unlockedRemoveNoReturn(portableKey, metaData);
      return valueModeHandler.createElement(actualKey, timestampedValue);
    } finally {
      if (!externalLock) {
        concurrentLock.unlock();
      }
    }
  }

  public void removeAll(Collection<?> keys, Map keyLookupCache) {
    final boolean externalLock = synclockstate.get().isLocked();
    TerracottaLock concurrentLock = null;
    Iterator iter = keys.iterator();
    while (iter.hasNext()) {
      cacheCoherence.acquireReadLock();
      try {
        long currentByteSize = 0;
        while (currentByteSize < BULK_OPS_KB_SIZE && iter.hasNext()) {
          Object key = iter.next();
          Object portableKey = this.clusteredStore.generatePortableKeyFor(key);
          currentByteSize += sizeOfEngine.sizeOf(portableKey, null, null).getCalculated();

          if (keyLookupCache != null) {
            keyLookupCache.remove(key);
          }

          if (!externalLock && concurrentLock == null) {
            concurrentLock = getConcurrentLock(portableKey);
            concurrentLock.lock();
          }
          MetaData metaData = clusteredStore.createRemoveSearchMetaData(portableKey);
          actualBackend.unlockedRemoveNoReturn(portableKey, metaData);
        }
      } finally {
        if (concurrentLock != null) {
          concurrentLock.unlock();
        }
        concurrentLock = null;
        cacheCoherence.releaseReadLock();
      }
    }
  }

  public void clear(MetaData metaData) {
    actualBackend.clear(metaData);
  }

  public boolean containsKey(Object portableKey) {
    if (synclockstate.get().isLocked()) {
      return actualBackend.containsKey(portableKey);
    } else {
      return actualBackend.unlockedContainsKey(portableKey);
    }
  }

  public boolean containsLocalKey(Object portableKey) {
    if (synclockstate.get().isLocked()) {
      return actualBackend.containsLocalKey(portableKey);
    } else {
      return actualBackend.unlockedContainsLocalKey(portableKey);
    }
  }

  public int getSize() {
    return actualBackend.size();
  }

  public int getInMemorySize() {
    return actualBackend.localSize();
  }

  public int getTerracottaClusteredSize() {
    return actualBackend.size();
  }

  public List getKeys() {
    return new UnmodifiableCollectionWrapper(new RealObjectKeySet(valueModeHandler, this.actualBackend.keySet(), false));
  }

  public Set getLocalKeys() {
    return Collections.unmodifiableSet(new RealObjectKeySet(valueModeHandler, this.actualBackend.localKeySet(), true));
  }

  /**
   * The server is the ultimate authority in deciding which put will go thru if it sees two putIfAbsent calls. Still
   * this method is locked even in nonstrict mode to avoid two concurrent putIfAbsent method calls return back nulls and
   * confusing the app as to which one got thru. The locking is debatable and can be removed if we want to relax the
   * constraints and make it faster.
   */
  public Element putIfAbsent(Object portableKey, Element element, MetaData searchMetaData) {
    ClusteredLock lock = actualBackend.createFinegrainedLock(portableKey);
    lock.lock();
    TimestampedValue value = null;
    try {
      TimestampedValue oldValue = actualBackend.unlockedGetTimestampedValue(portableKey, true);
      if (oldValue == null) {
        value = valueModeHandler.createTimestampedValue(element);
        actualBackend.unlockedPutIfAbsentNoReturn(portableKey, value, searchMetaData);
        // TODO::Why do we do this only for putIfAbsent and replace methods ?
        element.setElementEvictionData(new ClusteredElementEvictionData(clusteredStore, value));
        return null;
      }
      return valueModeHandler.createElement(element.getObjectKey(), oldValue);
    } finally {
      lock.unlock();
    }
  }

  public Element removeElement(Object portableKey, Element element, ElementValueComparator comparator, MetaData metaData) {
    final boolean externalLock = synclockstate.get().isLocked();
    TerracottaLock concurrentLock = null;
    if (!externalLock) {
      concurrentLock = getConcurrentLock(portableKey);
      concurrentLock.lock();
    }
    try {
      TimestampedValue oldValue = actualBackend.unlockedGetTimestampedValue(portableKey, true);
      Element oldElement = valueModeHandler.createElement(element.getObjectKey(), oldValue);
      if (comparator.equals(element, oldElement)) {
        actualBackend.unlockedRemoveNoReturn(portableKey, oldValue, metaData);
        return oldElement;
      } else {
        return null;
      }
    } finally {
      if (!externalLock) {
        concurrentLock.unlock();
      }
    }
  }

  public boolean replace(Object portableKey, Element old, Element element, ElementValueComparator comparator,
                         MetaData searchMetaData) {
    final boolean externalLock = synclockstate.get().isLocked();
    TerracottaLock concurrentLock = null;
    if (!externalLock) {
      concurrentLock = getConcurrentLock(portableKey);
      concurrentLock.lock();
    }
    TimestampedValue newValue = null;
    try {
      TimestampedValue currentValue = actualBackend.unlockedGetTimestampedValue(portableKey, true);
      Element currentElement = valueModeHandler.createElement(element.getObjectKey(), currentValue);
      if (comparator.equals(old, currentElement)) {
        newValue = valueModeHandler.createTimestampedValue(element);
        actualBackend.unlockedReplaceNoReturn(portableKey, currentValue, newValue, searchMetaData);
        element.setElementEvictionData(new ClusteredElementEvictionData(clusteredStore, newValue));
        return true;
      } else {
        return false;
      }
    } finally {
      if (!externalLock) {
        concurrentLock.unlock();
      }
    }
  }

  /**
   * This method is unlocked as most other nonstrict methods. Once it faults in the current mapping from the server it
   * replaces the value only when its the exact same object, else the server will skip the replace if the value is
   * changed in the meantime. This is ok as the ordering of the operation cant be guaranteed anyways.
   */
  public Element replace(Object portableKey, Element element, MetaData searchMetaData) {
    final boolean externalLock = synclockstate.get().isLocked();
    TerracottaLock concurrentLock = null;
    if (!externalLock) {
      concurrentLock = getConcurrentLock(portableKey);
      concurrentLock.lock();
    }
    TimestampedValue newValue = null;
    try {
      TimestampedValue currentValue = actualBackend.unlockedGetTimestampedValue(portableKey, true);
      Element currentElement = valueModeHandler.createElement(element.getObjectKey(), currentValue);
      if (currentElement != null) {
        newValue = valueModeHandler.createTimestampedValue(element);
        actualBackend.unlockedReplaceNoReturn(portableKey, currentValue, newValue, searchMetaData);
        element.setElementEvictionData(new ClusteredElementEvictionData(clusteredStore, newValue));
      }
      return currentElement;
    } finally {
      if (!externalLock) {
        concurrentLock.unlock();
      }
    }
  }

  public long getLocalHeapSizeInBytes() {
    return actualBackend.localOnHeapSizeInBytes();
  }

  public long getOffHeapSizeInBytse() {
    return actualBackend.localOffHeapSizeInBytes();
  }

  public int getLocalOnHeapSize() {
    return actualBackend.localOnHeapSize();
  }

  public int getLocalOffHeapSize() {
    return actualBackend.localOffHeapSize();
  }

  public boolean containsKeyLocalOnHeap(Object portableKey) {
    return actualBackend.containsKeyLocalOnHeap(portableKey);
  }

  public boolean containsKeyLocalOffHeap(Object portableKey) {
    return actualBackend.containsKeyLocalOffHeap(portableKey);
  }
}
