/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import org.terracotta.cache.TimeSource;
import org.terracotta.cache.TimestampedValue;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.meta.MetaData;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * This interface was created to help narrow the mutation paths on the underlying data store. <br>
 * <br>
 * NOTE: Mutations must be accompanied by search meta data <br>
 * <br>
 */
public interface ClusteredStoreBackend<K, V extends Serializable> {

  /********************************
   * mutators -- require search meta data
   ********************************/
  void clear(MetaData searchMetaData);

  void putNoReturn(K key, V value, MetaData searchMetaData);

  void removeNoReturn(K key, MetaData searchMetaData);

  TimestampedValue<V> removeTimestampedValue(K key, MetaData searchMetaData);

  // void unlockedPutNoReturn(K key, V value, MetaData searchMetaData);

  // void unlockedRemoveNoReturn(K key, MetaData searchMetaData);

  // void unlockedReplaceNoReturn(K key, V currentValue, V newValue, MetaData searchMetaData);

  // void unlockedRemoveNoReturn(K key, V oldValue, MetaData metaData);

  // void unlockedPutIfAbsentNoReturn(K key, V value, MetaData searchMetaData);

  /********************************
   * mutators (end)
   ********************************/

  //

  ToolkitLock createFinegrainedLock(K key);

  Configuration getConfig();

  // void initializeTransients(RegisteredEventListeners cacheEventNotificationService, ClusteredStore clusteredStore);

  boolean containsKey(K key);

  boolean containsLocalKey(K key);

  void unpinAll();

  boolean isPinned(K key);

  void setPinned(K key, boolean pinned);

  TimeSource getTimeSource();

  int size();

  int localSize();

  long localOnHeapSizeInBytes();

  long localOffHeapSizeInBytes();

  int localOnHeapSize();

  int localOffHeapSize();

  Set<K> keySet();

  Set<K> localKeySet();

  TimestampedValue getTimestampedValue(K key);

  TimestampedValue getTimestampedValueQuiet(K key);

  // boolean unlockedContainsKey(K key);

  // boolean unlockedContainsLocalKey(K key);

  // TimestampedValue unlockedGetTimestampedValue(K key, boolean quiet);

  // TimestampedValue unsafeGetTimestampedValue(K key, boolean quiet);

  void shutdown();

  void clearLocalCache();

  // This method returns Object to not expose the underlying TDC and providing a mutation path
  Object getTerracottaDistributedCache();

  Map<K, TimestampedValue<V>> getAllTimeStampedValues(final Set<K> keys, boolean quiet);

  // Map<K, TimestampedValue<V>> unlockedGetAllTimeStampedValues(final Set<K> keys, boolean quiet);

  Map<K, TimestampedValue<V>> getTimestampedValues(final Set<K> keys, boolean quiet);

  boolean containsKeyLocalOnHeap(K key);

  boolean containsKeyLocalOffHeap(K key);

  void setTargetMaxTotalCount(int targetMaxTotalCount);

  void setMaxTTI(int maxTTI);

  void setMaxTTL(int maxTTL);

  void setTargetMaxInMemoryCount(int targetMaxInMemoryCount);

  void setLoggingEnabled(boolean loggingEnabled);

  void setMaxBytesLocalHeap(long maxBytesLocalHeap);

  void setLocalCacheEnabled(boolean enabled);

  void recalculateLocalCacheSize(Object key);
}
