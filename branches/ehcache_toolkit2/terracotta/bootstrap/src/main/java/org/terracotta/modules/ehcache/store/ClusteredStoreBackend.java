/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import org.terracotta.modules.ehcache.store.bulkload.BulkLoadToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.cache.ToolkitCacheMetaDataCallback;
import org.terracotta.toolkit.internal.meta.MetaData;
import org.terracotta.toolkit.internal.search.SearchBuilder;
import org.terracotta.toolkit.object.ToolkitObjectType;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClusteredStoreBackend<K, V> implements ToolkitCacheInternal<K, V> {
  private final ToolkitCacheInternal<K, V> cache;
  private final BulkLoadToolkitCache<K, V> bulkloadCache;

  private ToolkitCacheInternal<K, V>       activeDelegate;

  private final ReadWriteLock              lock = new ReentrantReadWriteLock();

  public ClusteredStoreBackend(ToolkitInternal toolkit, ToolkitCacheInternal<K, V> cache) {
    this.cache = cache;
    this.bulkloadCache = new BulkLoadToolkitCache<K, V>(toolkit, cache.getName(), cache);
    this.activeDelegate = cache;
  }

  @Override
  public MetaData createMetaData(String category) {
    lock.readLock().lock();
    try {
      return activeDelegate.createMetaData(category);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public org.terracotta.toolkit.internal.cache.ToolkitCacheWithMetadata.EntryWithMetaData<K, V> createEntryWithMetaData(K key,
                                                                                                                              V value,
                                                                                                                              MetaData metaData) {
    lock.readLock().lock();
    try {
      return activeDelegate.createEntryWithMetaData(key, value, metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void setMetaDataCallback(ToolkitCacheMetaDataCallback callback) {
    lock.readLock().lock();
    try {
      activeDelegate.setMetaDataCallback(callback);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void putNoReturnWithMetaData(K key, V value, MetaData metaData) {
    lock.readLock().lock();
    try {
      activeDelegate.putNoReturnWithMetaData(key, value, metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void putNoReturnWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds,
                                      int customMaxTTLSeconds, MetaData metaData) {
    lock.readLock().lock();
    try {
      activeDelegate.putNoReturnWithMetaData(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds,
                                             metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V putIfAbsentWithMetaData(K key, V value, MetaData metaData) {
    lock.readLock().lock();
    try {
      return activeDelegate.putIfAbsentWithMetaData(key, value, metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V putIfAbsentWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds,
                                   int customMaxTTLSeconds, MetaData metaData) {
    lock.readLock().lock();
    try {
      return activeDelegate.putIfAbsentWithMetaData(key, value, createTimeInSecs, customMaxTTISeconds,
                                                    customMaxTTLSeconds, metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void clearWithMetaData(MetaData metaData) {
    lock.readLock().lock();
    try {
      activeDelegate.clearWithMetaData(metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void removeNoReturnWithMetaData(K key, MetaData metaData) {
    lock.readLock().lock();
    try {
      activeDelegate.removeNoReturnWithMetaData(key, metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V removeWithMetaData(K key, MetaData metaData) {
    lock.readLock().lock();
    try {
      return activeDelegate.removeWithMetaData(key, metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean removeWithMetaData(K key, V value, MetaData metaData) {
    lock.readLock().lock();
    try {
      return activeDelegate.removeWithMetaData(key, value, metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public SearchBuilder createSearchBuilder() {
    lock.readLock().lock();
    try {
      return activeDelegate.createSearchBuilder();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V putWithMetaData(K key, V value, MetaData metaData) {
    lock.readLock().lock();
    try {
      return activeDelegate.putWithMetaData(key, value, metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V putWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds,
                           MetaData metaData) {
    lock.readLock().lock();
    try {
      return activeDelegate.putWithMetaData(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds,
                                            metaData);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void putAllWithMetaData(Collection<org.terracotta.toolkit.internal.cache.ToolkitCacheWithMetadata.EntryWithMetaData<K, V>> entries) {
    lock.readLock().lock();
    try {
      activeDelegate.putAllWithMetaData(entries);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void removeAllWithMetaData(Collection<org.terracotta.toolkit.internal.cache.ToolkitCacheWithMetadata.EntryWithMetaData<K, V>> entries) {
    lock.readLock().lock();
    try {
      activeDelegate.removeAllWithMetaData(entries);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V getQuiet(K key) {
    lock.readLock().lock();
    try {
      return activeDelegate.getQuiet(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    lock.readLock().lock();
    try {
      return activeDelegate.getAllQuiet(keys);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void putNoReturn(K key, V value, int createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    lock.readLock().lock();
    try {
      activeDelegate.putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V putIfAbsent(K key, V value, int createTimeInSecs, int maxTTISeconds, int maxTTLSeconds) {
    lock.readLock().lock();
    try {
      return activeDelegate.putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    lock.readLock().lock();
    try {
      activeDelegate.addListener(listener);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    lock.readLock().lock();
    try {
      activeDelegate.removeListener(listener);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void unpinAll() {
    lock.readLock().lock();
    try {
      activeDelegate.unpinAll();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean isPinned(K key) {
    lock.readLock().lock();
    try {
      return activeDelegate.isPinned(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void setPinned(K key, boolean pinned) {
    lock.readLock().lock();
    try {
      activeDelegate.setPinned(key, pinned);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void removeNoReturn(K key) {
    lock.readLock().lock();
    try {
      activeDelegate.removeNoReturn(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V unsafeLocalGet(K key) {
    lock.readLock().lock();
    try {
      return activeDelegate.unsafeLocalGet(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void putNoReturn(K key, V value) {
    lock.readLock().lock();
    try {
      activeDelegate.putNoReturn(key, value);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int localSize() {
    lock.readLock().lock();
    try {
      return activeDelegate.localSize();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Set<K> localKeySet() {
    lock.readLock().lock();
    try {
      return activeDelegate.localKeySet();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean containsLocalKey(K key) {
    lock.readLock().lock();
    try {
      return activeDelegate.containsLocalKey(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    lock.readLock().lock();
    try {
      return activeDelegate.getAll(keys);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Configuration getConfiguration() {
    lock.readLock().lock();
    try {
      return activeDelegate.getConfiguration();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    lock.readLock().lock();
    try {
      activeDelegate.setConfigField(name, value);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean containsValue(Object value) throws UnsupportedOperationException {
    lock.readLock().lock();
    try {
      return activeDelegate.containsValue(value);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V putIfAbsent(K key, V value) {
    lock.readLock().lock();
    try {
      return activeDelegate.putIfAbsent(key, value);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    lock.readLock().lock();
    try {
      return activeDelegate.entrySet();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Collection<V> values() {
    lock.readLock().lock();
    try {
      return activeDelegate.values();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean remove(Object key, Object value) {
    lock.readLock().lock();
    try {
      return activeDelegate.remove(key, value);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    lock.readLock().lock();
    try {
      return activeDelegate.replace(key, oldValue, newValue);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V replace(K key, V value) {
    lock.readLock().lock();
    try {
      return activeDelegate.replace(key, value);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public long localOnHeapSizeInBytes() {
    lock.readLock().lock();
    try {
      return activeDelegate.localOnHeapSizeInBytes();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public long localOffHeapSizeInBytes() {
    lock.readLock().lock();
    try {
      return activeDelegate.localOffHeapSizeInBytes();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int localOnHeapSize() {
    lock.readLock().lock();
    try {
      return activeDelegate.localOnHeapSize();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int localOffHeapSize() {
    lock.readLock().lock();
    try {
      return activeDelegate.localOffHeapSize();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean containsKeyLocalOnHeap(K key) {
    lock.readLock().lock();
    try {
      return activeDelegate.containsKeyLocalOnHeap(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean containsKeyLocalOffHeap(K key) {
    lock.readLock().lock();
    try {
      return activeDelegate.containsKeyLocalOffHeap(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void disposeLocally() {
    lock.readLock().lock();
    try {
      activeDelegate.disposeLocally();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    lock.readLock().lock();
    try {
      return activeDelegate.createLockForKey(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int size() {
    lock.readLock().lock();
    try {
      return activeDelegate.size();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean isEmpty() {
    lock.readLock().lock();
    try {
      return activeDelegate.isEmpty();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean containsKey(Object key) {
    lock.readLock().lock();
    try {
      return activeDelegate.containsKey(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V get(Object key) {
    lock.readLock().lock();
    try {
      return activeDelegate.get(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V put(K key, V value) {
    lock.readLock().lock();
    try {
      return activeDelegate.put(key, value);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V remove(Object key) {
    lock.readLock().lock();
    try {
      return activeDelegate.remove(key);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    lock.readLock().lock();
    try {
      activeDelegate.putAll(m);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void clear() {
    lock.readLock().lock();
    try {
      activeDelegate.clear();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Set<K> keySet() {
    lock.readLock().lock();
    try {
      return activeDelegate.keySet();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean isDestroyed() {
    lock.readLock().lock();
    try {
      return activeDelegate.isDestroyed();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void destroy() {
    lock.readLock().lock();
    try {
      activeDelegate.destroy();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public String getName() {
    lock.readLock().lock();
    try {
      return activeDelegate.getName();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public ToolkitObjectType getType() {
    lock.readLock().lock();
    try {
      return activeDelegate.getType();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    lock.readLock().lock();
    try {
      return activeDelegate.getNodesWithKeys(portableKeys);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL, MetaData metadata) {
    lock.readLock().lock();
    try {
      activeDelegate.unlockedPutNoReturn(k, v, createTime, customTTI, customTTL, metadata);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void unlockedRemoveNoReturn(K k, MetaData metadata) {
    lock.readLock().lock();
    try {
      activeDelegate.unlockedRemoveNoReturn(k, metadata);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    lock.readLock().lock();
    try {
      return activeDelegate.unlockedGet(k, quiet);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void clearLocalCache() {
    lock.readLock().lock();
    try {
      activeDelegate.clearLocalCache();
    } finally {
      lock.readLock().unlock();
    }
  }

  public void setBulkLoadEnabledInCurrentNode(boolean enableBulkLoad) {
    lock.writeLock().lock();
    try {
      if (enableBulkLoad && !isBulkLoadEnabledInCurrentNode()) {
        this.activeDelegate = bulkloadCache;
        this.bulkloadCache.setBulkLoadEnabledInCurrentNode(enableBulkLoad);
      }

      if (!enableBulkLoad && isBulkLoadEnabledInCurrentNode()) {
        this.activeDelegate = cache;
        this.bulkloadCache.setBulkLoadEnabledInCurrentNode(enableBulkLoad);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void waitUntilBulkLoadCompleteInCluster() throws InterruptedException {
    lock.readLock().lock();
    try {
      bulkloadCache.waitUntilBulkLoadCompleteInCluster();
    } finally {
      lock.readLock().unlock();
    }
  }

  public boolean isBulkLoadEnabledInCluster() {
    lock.readLock().lock();
    try {
      return bulkloadCache.isBulkLoadEnabledInCluster();
    } finally {
      lock.readLock().unlock();
    }
  }

  public boolean isBulkLoadEnabledInCurrentNode() {
    lock.readLock().lock();
    try {
      return bulkloadCache.isBulkLoadEnabledInCurrentNode();
    } finally {
      lock.readLock().unlock();
    }
  }
}
