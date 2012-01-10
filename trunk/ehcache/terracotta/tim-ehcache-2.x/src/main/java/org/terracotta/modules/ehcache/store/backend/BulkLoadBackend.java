/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.backend;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementValueComparator;

import org.terracotta.cache.TimestampedValue;
import org.terracotta.meta.MetaData;
import org.terracotta.modules.ehcache.coherence.CacheCoherence;
import org.terracotta.modules.ehcache.store.ClusteredElementEvictionData;
import org.terracotta.modules.ehcache.store.ClusteredStore;
import org.terracotta.modules.ehcache.store.ClusteredStoreBackend;
import org.terracotta.modules.ehcache.store.LocalBufferedMap;
import org.terracotta.modules.ehcache.store.UnmodifiableMultiCollectionSetWrapper;
import org.terracotta.modules.ehcache.store.UnmodifiableMultiCollectionWrapper;
import org.terracotta.modules.ehcache.store.ValueModeHandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Abhishek Sanoujam
 */
public class BulkLoadBackend implements BackendStore {

  private final ClusteredStoreBackend<Object, Object>              actualBackend;
  private final ValueModeHandler                                   valueModeHandler;
  private final LocalBufferedMap<Object, TimestampedValue<Object>> localBufferedMap;
  private final ClusteredStore                                     clusteredStore;
  private final CacheCoherence                                     cacheCoherence;

  public BulkLoadBackend(ClusteredStore clusteredStore, ClusteredStoreBackend<Object, Object> actualBackend,
                         final ValueModeHandler valueModeHandler,
                         final LocalBufferedMap<Object, TimestampedValue<Object>> localBufferedMap,
                         CacheCoherence cacheCoherence) {
    this.clusteredStore = clusteredStore;
    this.actualBackend = actualBackend;
    this.valueModeHandler = valueModeHandler;
    this.localBufferedMap = localBufferedMap;
    this.cacheCoherence = cacheCoherence;
  }

  public void putNoReturn(Object portableKey, TimestampedValue value, MetaData searchMetaData) {
    localBufferedMap.put(portableKey, value, searchMetaData);
    // valueModeHandler.processStoreValue called in localBufferedMap.flush() after flushing to server
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

  public Element get(Object actualKey, Object portableKey, boolean quiet) {
    TimestampedValue value = null;
    value = localBufferedMap.get(portableKey);
    if (value == null) {
      value = actualBackend.unlockedGetTimestampedValue(portableKey, quiet);
    }
    return valueModeHandler.createElement(actualKey, value);
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

  public Element unlockedGet(Object actualKey, Object portableKey, boolean quiet) {
    TimestampedValue<Object> value = localBufferedMap.get(portableKey);
    if (value == null) {
      value = actualBackend.unlockedGetTimestampedValue(portableKey, quiet);
    }
    return valueModeHandler.createElement(actualKey, value);
  }

  public Element unsafeGet(Object actualKey, Object portableKey, boolean quiet) {
    TimestampedValue<Object> value = localBufferedMap.get(portableKey);
    if (value == null) {
      value = actualBackend.unsafeGetTimestampedValue(portableKey, quiet);
    }
    return valueModeHandler.createElement(actualKey, value);
  }

  public Element remove(Object actualKey, Object portableKey, MetaData metaData) {
    return valueModeHandler.createElement(actualKey, localBufferedMap.remove(portableKey, metaData));
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
    boolean rv = localBufferedMap.containsKey(portableKey);
    if (rv) { return true; }
    return actualBackend.unlockedContainsKey(portableKey);
  }

  public boolean containsLocalKey(Object portableKey) {
    boolean rv = localBufferedMap.containsKey(portableKey);
    if (rv) { return true; }
    return actualBackend.unlockedContainsLocalKey(portableKey);
  }

  public int getSize() {
    return localBufferedMap.getSize() + actualBackend.size();
  }

  public int getTerracottaClusteredSize() {
    // no need to add localBufferedSize.getSize() here in incoherent mode, as its yet to go in cluster
    return actualBackend.size();
  }

  public int getInMemorySize() {
    return localBufferedMap.getSize() + actualBackend.localSize();
  }

  public void clear(MetaData metaData) {
    localBufferedMap.clear(metaData);
    actualBackend.clear(metaData);
  }

  public List getKeys() {
    return new UnmodifiableMultiCollectionWrapper(new RealObjectKeySet(valueModeHandler, this.actualBackend.keySet(),
                                                                       false), new RealObjectKeySet(valueModeHandler,
                                                                                                    localBufferedMap
                                                                                                        .getKeys(),
                                                                                                    false));
  }

  public Set getLocalKeys() {
    return new UnmodifiableMultiCollectionSetWrapper(new RealObjectKeySet(valueModeHandler,
                                                                          this.actualBackend.localKeySet(), true),
                                                     new RealObjectKeySet(valueModeHandler, localBufferedMap.getKeys(),
                                                                          false));
  }

  public Element putIfAbsent(Object portableKey, Element element, MetaData searchMetaData) {
    throw new UnsupportedOperationException();
  }

  public Element removeElement(Object portableKey, Element element, ElementValueComparator comparator, MetaData metaData) {
    throw new UnsupportedOperationException();
  }

  public boolean replace(Object portableKey, Element old, Element element, ElementValueComparator comparator,
                         MetaData searchMetaData) {
    throw new UnsupportedOperationException();
  }

  public Element replace(Object portableKey, Element element, MetaData searchMetaData) {
    throw new UnsupportedOperationException();
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
