/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import org.terracotta.cache.TimeSource;
import org.terracotta.cache.TimestampedValue;
import org.terracotta.toolkit.collections.ToolkitCache;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.config.ToolkitCacheConfigFields;
import org.terracotta.toolkit.config.ToolkitMapConfigFields;
import org.terracotta.toolkit.internal.collections.ToolkitCacheWithMetadata;
import org.terracotta.toolkit.internal.meta.MetaData;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class ClusteredStoreBackendImpl<K, V extends Serializable> implements ClusteredStoreBackend<K, V> {

  static {
    for (Class i : ClusteredStoreBackendImpl.class.getInterfaces()) {
      System.err.println("XXX: " + i);
    }
  }

  private final ToolkitCacheWithMetadata<K, TimestampedValue<V>> tdc;

  public ClusteredStoreBackendImpl(final ToolkitCache<K, TimestampedValue<V>> cache) {
    this.tdc = (ToolkitCacheWithMetadata<K, TimestampedValue<V>>) cache;
  }

  public ToolkitCache<K, TimestampedValue<V>> getTerracottaDistributedCache() {
    return tdc;
  }

  public void clearLocalCache() {
    // tdc.clearLocalCache();
  }

  public void unpinAll() {
    tdc.unpinAll();
  }

  public boolean isPinned(K key) {
    return tdc.isPinned(key);
  }

  public void setPinned(K key, boolean pinned) {
    tdc.setPinned(key, pinned);
  }

  public boolean containsLocalKey(K key) {
    return tdc.containsLocalKey(key);
  }

  public boolean unlockedContainsLocalKey(K key) {
    return containsLocalKey(key);
  }

  public TimestampedValue<V> getTimestampedValue(K key) {
    return tdc.get(key);
  }

  public TimestampedValue<V> getTimestampedValueQuiet(K key) {
    return tdc.getQuiet(key);
  }

  public Map<K, TimestampedValue<V>> getTimestampedValues(final Set<K> keys, boolean quiet) {
    // TODO: Support for quiet getAll ??
    return tdc.getAll(keys);
  }

  public TimestampedValue<V> removeTimestampedValue(K key, MetaData searchMetaData) {
    if (searchMetaData != null) {
      return (tdc).removeWithMetaData(key, searchMetaData);
    } else {
      return tdc.remove(key);
    }
  }

  public Set<K> keySet() {
    return tdc.keySet();
  }

  public Set<K> localKeySet() {
    return tdc.localKeySet();
  }

  public void clear(MetaData searchMetaData) {
    if (searchMetaData != null) {
      tdc.clearWithMetaData(searchMetaData);
    } else {
      tdc.clear();
    }
  }

  public int size() {
    return tdc.size();
  }

  public int localSize() {
    return tdc.localSize();
  }

  public void shutdown() {
    tdc.destroy();
  }

  public void putNoReturn(K key, V value, MetaData searchMetaData) {
    if (searchMetaData != null) {
      tdc.putNoReturnWithMetaData(key, wrapValue(value), searchMetaData);
    } else {
      tdc.putNoReturn(key, wrapValue(value));
    }
  }

  public TimestampedValue<V> unsafeGetTimestampedValue(K key, boolean quiet) {
    // TODO: Unsafe get quiet ??
    return tdc.unsafeGet(key);
  }

  public void removeNoReturn(K key, MetaData searchMetaData) {
    if (searchMetaData != null) {
      tdc.removeNoReturnWithMetaData(key, searchMetaData);
    } else {
      tdc.removeNoReturn(key);
    }
  }

  public Configuration getConfig() {
    return tdc.getConfiguration();
  }

  public ToolkitLock createFinegrainedLock(K key) {
    return tdc.createFinegrainedLock(key);
  }

  public void recalculateLocalCacheSize(Object key) {
    // tdc.recalculateLocalCacheSize(key);
  }

  public long localOnHeapSizeInBytes() {
    // return tdc.localOnHeapSizeInBytes();
    return -1;
  }

  public long localOffHeapSizeInBytes() {
    // return tdc.localOffHeapSizeInBytes();
    return -1;
  }

  public int localOnHeapSize() {
    // return tdc.localOnHeapSize();
    return -1;
  }

  public int localOffHeapSize() {
    // return tdc.localOffHeapSize();
    return -1;
  }

  public boolean containsKeyLocalOnHeap(K key) {
    return tdc.containsLocalKey(key);
  }

  public boolean containsKeyLocalOffHeap(K key) {
    // return tdc.containsKeyLocalOffHeap(key);
    return false;
  }

  public void setTargetMaxTotalCount(int targetMaxTotalCount) {
    tdc.setConfigField(ToolkitCacheConfigFields.MAX_TOTAL_COUNT_FIELD_NAME, targetMaxTotalCount);
  }

  public void setMaxTTI(int maxTTI) {
    tdc.setConfigField(ToolkitCacheConfigFields.MAX_TTI_SECONDS_FIELD_NAME, maxTTI);
  }

  public void setMaxTTL(int maxTTL) {
    tdc.setConfigField(ToolkitCacheConfigFields.MAX_TTL_SECONDS_FIELD_NAME, maxTTL);
  }

  public void setTargetMaxInMemoryCount(int targetMaxInMemoryCount) {
    // Don't set in MutableConfig as this is a local property
    tdc.setConfigField(ToolkitMapConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME, targetMaxInMemoryCount);
  }

  public void setLoggingEnabled(boolean loggingEnabled) {
    // getConfig().setLoggingEnabled(loggingEnabled);
  }

  public void setMaxBytesLocalHeap(long maxBytesLocalHeap) {
    tdc.setConfigField(ToolkitMapConfigFields.MAX_BYTES_LOCAL_HEAP_FIELD_NAME, maxBytesLocalHeap);
  }

  public void setLocalCacheEnabled(boolean enabled) {
    tdc.setConfigField(ToolkitMapConfigFields.LOCAL_CACHE_ENABLED_FIELD_NAME, enabled);
  }

  private TimestampedValue<V> wrapValue(final V value) {
    return null;
  }

  public boolean containsKey(K key) {
    return tdc.containsKey(key);
  }

  @Override
  public TimeSource getTimeSource() {
    return null;
  }

  @Override
  public Map<K, TimestampedValue<V>> getAllTimeStampedValues(Set<K> keys, boolean quiet) {
    return tdc.getAll(keys);
  }

}
