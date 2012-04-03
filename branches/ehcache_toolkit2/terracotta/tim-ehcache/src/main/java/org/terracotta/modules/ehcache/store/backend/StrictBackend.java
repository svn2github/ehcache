/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.backend;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementValueComparator;

import org.terracotta.cache.TimestampedValue;
import org.terracotta.locking.ClusteredLock;
import org.terracotta.meta.MetaData;
import org.terracotta.modules.ehcache.coherence.CacheCoherence;
import org.terracotta.modules.ehcache.store.ClusteredElementEvictionData;
import org.terracotta.modules.ehcache.store.ClusteredStore;
import org.terracotta.modules.ehcache.store.ClusteredStoreBackend;
import org.terracotta.modules.ehcache.store.UnmodifiableCollectionWrapper;
import org.terracotta.modules.ehcache.store.ValueModeHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Abhishek Sanoujam
 */
public class StrictBackend implements BackendStore {

  private final ClusteredStoreBackend<Object, Object> actualBackend;
  private final ValueModeHandler                      valueModeHandler;
  private final ClusteredStore                        clusteredStore;
  private final CacheCoherence                        cacheCoherence;

  public StrictBackend(ClusteredStore clusteredStore, ClusteredStoreBackend<Object, Object> actualBackend,
                       final ValueModeHandler valueModeHandler, CacheCoherence cacheCoherence) {
    this.clusteredStore = clusteredStore;
    this.actualBackend = actualBackend;
    this.valueModeHandler = valueModeHandler;
    this.cacheCoherence = cacheCoherence;
  }

  public void putNoReturn(Object portableKey, TimestampedValue value, MetaData searchMetaData) {
    actualBackend.putNoReturn(portableKey, value, searchMetaData);
  }

  public void putAllNoReturn(Collection<Element> elements) {
    for (Element element : elements) {
      cacheCoherence.acquireReadLock();
      try {
        Object portableKey = this.clusteredStore.generatePortableKeyFor(element.getObjectKey());
        MetaData searchMetaData = this.clusteredStore.createPutSearchMetaData(portableKey, element);
        TimestampedValue value = valueModeHandler.createTimestampedValue(element);
        putNoReturn(portableKey, value, searchMetaData);
        element.setElementEvictionData(new ClusteredElementEvictionData(clusteredStore, value));
      } finally {
        cacheCoherence.releaseReadLock();
      }
    }
  }

  public Element get(Object actualKey, Object portableKey, final boolean quiet) {
    TimestampedValue timestampedValue;
    if (quiet) {
      timestampedValue = actualBackend.getTimestampedValueQuiet(portableKey);
    } else {
      timestampedValue = actualBackend.getTimestampedValue(portableKey);
    }
    return valueModeHandler.createElement(actualKey, timestampedValue);
  }

  public Map<Object, Element> getAll(Collection<?> keys, boolean quiet) {
    Map<Object, Element> rv = new HashMap<Object, Element>();
    for (Object key : keys) {
      Object pKey = this.clusteredStore.generatePortableKeyFor(key);
      this.cacheCoherence.acquireReadLock();
      try {
        rv.put(key, get(key, pKey, quiet));
      } finally {
        this.cacheCoherence.releaseReadLock();
      }
    }
    return rv;
  }

  public void getAllInternal(Collection<Object> keys, boolean quiet, Map<Object, Element> rv) {
    // no-op
  }

  public Element unlockedGet(Object actualKey, Object portableKey, final boolean quiet) {
    return valueModeHandler.createElement(actualKey, actualBackend.unlockedGetTimestampedValue(portableKey, quiet));
  }

  public Element unsafeGet(Object actualKey, Object portableKey, final boolean quiet) {
    return valueModeHandler.createElement(actualKey, actualBackend.unsafeGetTimestampedValue(portableKey, quiet));
  }

  public Element remove(Object actualKey, Object portableKey, MetaData metaData) {
    return valueModeHandler.createElement(actualKey, actualBackend.removeTimestampedValue(portableKey, metaData));
  }

  public void removeAll(Collection<?> keys, Map keyLookupCache) {
    for (Object key : keys) {
      Object portableKey = clusteredStore.generatePortableKeyFor(key);
      remove(key, portableKey, clusteredStore.createRemoveSearchMetaData(portableKey));
      if (keyLookupCache != null) {
        keyLookupCache.remove(key);
      }
    }
  }

  public boolean containsKey(Object portableKey) {
    return actualBackend.containsKey(portableKey);
  }

  public boolean containsLocalKey(Object portableKey) {
    return actualBackend.containsLocalKey(portableKey);
  }

  public void clear(MetaData metaData) {
    actualBackend.clear(metaData);
  }

  public int getInMemorySize() {
    return actualBackend.localSize();
  }

  public List getKeys() {
    return new UnmodifiableCollectionWrapper(new RealObjectKeySet(valueModeHandler, this.actualBackend.keySet(), false));
  }

  public Set getLocalKeys() {
    return Collections.unmodifiableSet(new RealObjectKeySet(valueModeHandler, this.actualBackend.localKeySet(), true));
  }

  public int getSize() {
    return actualBackend.size();
  }

  public int getTerracottaClusteredSize() {
    return actualBackend.size();
  }

  public Element putIfAbsent(Object portableKey, Element element, MetaData searchMetaData) {
    ClusteredLock lock = actualBackend.createFinegrainedLock(portableKey);
    lock.lock();
    try {
      TimestampedValue oldValue = actualBackend.getTimestampedValueQuiet(portableKey);
      if (oldValue == null) {
        TimestampedValue value = valueModeHandler.createTimestampedValue(element);
        actualBackend.putNoReturn(portableKey, value, searchMetaData);
        element.setElementEvictionData(new ClusteredElementEvictionData(clusteredStore, value));
        return null;
      }
      return valueModeHandler.createElement(element.getObjectKey(), oldValue);
    } finally {
      lock.unlock();
    }
  }

  public Element removeElement(Object portableKey, Element element, ElementValueComparator comparator, MetaData metaData) {
    ClusteredLock lock = actualBackend.createFinegrainedLock(portableKey);
    lock.lock();
    try {
      TimestampedValue oldValue = actualBackend.getTimestampedValueQuiet(portableKey);
      Element oldElement = valueModeHandler.createElement(element.getObjectKey(), oldValue);
      if (comparator.equals(element, oldElement)) {
        actualBackend.removeNoReturn(portableKey, metaData);
        return oldElement;
      } else {
        return null;
      }
    } finally {
      lock.unlock();
    }
  }

  public boolean replace(Object portableKey, Element old, Element element, ElementValueComparator comparator,
                         MetaData searchMetaData) {
    ClusteredLock lock = actualBackend.createFinegrainedLock(portableKey);
    lock.lock();
    try {
      Element currentElement = valueModeHandler.createElement(element.getObjectKey(),
                                                              actualBackend.getTimestampedValueQuiet(portableKey));
      if (comparator.equals(old, currentElement)) {
        TimestampedValue value = valueModeHandler.createTimestampedValue(element);
        actualBackend.putNoReturn(portableKey, value, searchMetaData);
        element.setElementEvictionData(new ClusteredElementEvictionData(clusteredStore, value));
        return true;
      } else {
        return false;
      }
    } finally {
      lock.unlock();
    }
  }

  public Element replace(Object portableKey, Element element, MetaData searchMetaData) {
    ClusteredLock lock = actualBackend.createFinegrainedLock(portableKey);
    lock.lock();
    try {
      TimestampedValue currentValue = actualBackend.getTimestampedValueQuiet(portableKey);
      Element currentElement = valueModeHandler.createElement(element.getObjectKey(), currentValue);
      if (currentElement != null) {
        TimestampedValue value = valueModeHandler.createTimestampedValue(element);
        actualBackend.putNoReturn(portableKey, value, searchMetaData);
        element.setElementEvictionData(new ClusteredElementEvictionData(clusteredStore, value));
      }
      return currentElement;
    } finally {
      lock.unlock();
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
