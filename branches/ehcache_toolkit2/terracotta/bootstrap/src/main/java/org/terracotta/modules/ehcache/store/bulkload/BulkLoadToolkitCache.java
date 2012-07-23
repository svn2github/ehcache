/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.bulkload;

import org.terracotta.toolkit.cache.ToolkitCacheConfigFields;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.cache.ToolkitCacheMetaDataCallback;
import org.terracotta.toolkit.internal.meta.MetaData;
import org.terracotta.toolkit.internal.search.SearchBuilder;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BulkLoadToolkitCache<K, V> implements ToolkitCacheInternal<K, V> {

  private final ToolkitLogger              logger;
  private final static boolean             LOGGING_ENABLED            = BulkLoadConstants.isLoggingEnabled();

  private final ReentrantReadWriteLock     readWriteLock              = new ReentrantReadWriteLock();
  private final ToolkitCacheInternal<K, V> toolkitCache;
  private final BulkLoadEnabledNodesSet    bulkLoadEnabledNodesSet;
  private final AtomicBoolean              currentNodeBulkLoadEnabled = new AtomicBoolean(false);
  private final ToolkitInternal            toolkitInternal;
  private final LocalBufferedMap<K, V>     localBufferedMap;
  private boolean                          localCacheEnabledBeforeBulkloadEnabled;

  private final String                     name;
  private final BulkLoadShutdownHook       bulkLoadShutdownHook;

  public BulkLoadToolkitCache(ToolkitInternal toolkit, String name, ToolkitCacheInternal<K, V> aggregateServerMap) {
    this.toolkitInternal = toolkit;
    this.name = name;
    this.logger = toolkit.getLogger(BulkLoadToolkitCache.class.getName());
    this.toolkitCache = aggregateServerMap;
    this.bulkLoadEnabledNodesSet = new BulkLoadEnabledNodesSet(toolkit, name);
    this.localBufferedMap = new LocalBufferedMap(name, this, aggregateServerMap, toolkit);
    this.bulkLoadShutdownHook = BulkLoadShutdownHook.getInstance(toolkit);
  }

  public void debug(String msg) {
    logger.info("['" + name + "'] " + msg);
  }

  public void acquireLocalReadLock() {
    readWriteLock.readLock().lock();
  }

  public void acquireLocalWriteLock() {
    readWriteLock.writeLock().lock();
  }

  public void releaseLocalReadLock() {
    readWriteLock.readLock().unlock();
  }

  public void releaseLocalWriteLock() {
    readWriteLock.writeLock().unlock();
  }

  public void setBulkLoadEnabledInCurrentNode(boolean enableBulkLoad) {
    acquireLocalWriteLock();
    try {
      if (enableBulkLoad) {
        // turning on bulk-load
        if (currentNodeBulkLoadEnabled.compareAndSet(false, true)) {
          if (LOGGING_ENABLED) {
            debug("Enabling bulk-load");
          }
          localCacheEnabledBeforeBulkloadEnabled = toolkitCache.getConfiguration()
              .getBoolean(ToolkitStoreConfigFields.LOCAL_CACHE_ENABLED_FIELD_NAME);

          // add current node
          bulkLoadEnabledNodesSet.addCurrentNode();
          // start local buffering
          localBufferedMap.startBuffering();

          // disable local cache
          if (localCacheEnabledBeforeBulkloadEnabled) {
            setLocalCacheEnabled(false);
          }

          bulkLoadShutdownHook.registerCache(this);
        } else {
          if (LOGGING_ENABLED) {
            debug("Trying to enable bulk-load mode when already bulk-loading.");
          }
        }
      } else {
        // turning off bulk-load
        if (currentNodeBulkLoadEnabled.compareAndSet(true, false)) {
          if (LOGGING_ENABLED) {
            debug("Turning off bulk-load");
          }
          // flush and stop local buffering
          localBufferedMap.flushAndStopBuffering();
          // wait until all txns finished
          toolkitInternal.waitUntilAllTransactionsComplete();
          // clear local cache
          toolkitCache.clearLocalCache();

          // enable local cache
          if (localCacheEnabledBeforeBulkloadEnabled) {
            setLocalCacheEnabled(true);
          }

          // remove current node from list of bulk-loading nodes
          bulkLoadEnabledNodesSet.removeCurrentNode();

          bulkLoadShutdownHook.unregisterCache(this);
        } else {
          if (LOGGING_ENABLED) {
            debug("Trying to disable bulk-load mode when not bulk-loading.");
          }
        }
      }
    } finally {
      releaseLocalWriteLock();
    }
  }

  private void setLocalCacheEnabled(boolean enabled) {
    toolkitInternal.getConfigBuilderFactory().newToolkitCacheConfigBuilder().localCacheEnabled(enabled)
        .apply(toolkitCache);
  }

  public void waitUntilBulkLoadCompleteInCluster() throws InterruptedException {
    bulkLoadEnabledNodesSet.waitUntilSetEmpty();
  }

  public boolean isBulkLoadEnabledInCluster() {
    return bulkLoadEnabledNodesSet.isBulkLoadEnabledInCluster();
  }

  public boolean isBulkLoadEnabledInCurrentNode() {
    // use local atomicBoolean instead of querying which would take clustered lock
    return currentNodeBulkLoadEnabled.get();
  }

  @Override
  public V getQuiet(Object key) {
    return doGet(key, true);
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    // custom tti ttl not supported
    put(key, value);
  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeNoReturn(Object key) {
    removeWithMetaData(key, null);
  }

  @Override
  public V unsafeLocalGet(Object key) {
    V value = localBufferedMap.get(key);
    if (value == null) {
      value = toolkitCache.unsafeLocalGet(key);
    }
    return value;
  }

  @Override
  public void putNoReturn(K key, V value) {
    put(key, value);
  }

  @Override
  public int localSize() {
    return localBufferedMap.getSize() + toolkitCache.localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return new UnmodifiableMultiSetWrapper<K>(localBufferedMap.getKeys(), toolkitCache.localKeySet());
  }

  @Override
  public void unpinAll() {
    toolkitCache.unpinAll();
  }

  @Override
  public boolean isPinned(K key) {
    return toolkitCache.isPinned(key);
  }

  @Override
  public void setPinned(K key, boolean pinned) {
    toolkitCache.setPinned(key, pinned);
  }

  @Override
  public boolean containsLocalKey(Object key) {
    return localBufferedMap.containsKey(key) || toolkitCache.containsLocalKey(key);
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    Map<K, V> rv = new HashMap<K, V>();
    for (K key : keys) {
      rv.put(key, doGet(key, false));
    }
    return rv;
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    Map<K, V> rv = new HashMap<K, V>();
    for (K key : keys) {
      rv.put(key, doGet(key, true));
    }
    return rv;
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    toolkitCache.addListener(listener);
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    toolkitCache.removeListener(listener);
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    return toolkitCache.createLockForKey(key);
  }

  @Override
  public Configuration getConfiguration() {
    return toolkitCache.getConfiguration();
  }

  @Override
  public void setConfigField(String name, Serializable value) {
    toolkitCache.setConfigField(name, value);
  }

  @Override
  public void clear() {
    clearWithMetaData(null);
  }

  @Override
  public boolean containsKey(Object keyObj) {
    K key = (K) keyObj;
    return localBufferedMap.containsKey(key) || toolkitCache.containsKey(key);
  }

  @Override
  public boolean containsValue(Object arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return new UnmodifiableMultiSetWrapper<Map.Entry<K, V>>(localBufferedMap.entrySet(), toolkitCache.entrySet());
  }

  @Override
  public V get(Object obj) {
    return doGet(obj, false);
  }

  public V doGet(Object obj, boolean quiet) {
    K key = (K) obj;
    if (localBufferedMap.isKeyBeingRemoved(obj)) { return null; }

    V value = localBufferedMap.get(key);
    if (value == null) {
      value = toolkitCache.unlockedGet(obj, quiet);
    }
    return value;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Set<K> keySet() {
    return new UnmodifiableMultiSetWrapper<K>(localBufferedMap.getKeys(), toolkitCache.keySet());
  }

  @Override
  public V put(K key, V value) {
    return putWithMetaData(key, value, now(), ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS,
                           ToolkitCacheConfigFields.NO_MAX_TTL_SECONDS, null);
  }

  private int now() {
    return (int) System.currentTimeMillis() / 1000;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @Override
  public void putAllWithMetaData(Collection<EntryWithMetaData<K, V>> entries) {
    for (EntryWithMetaData<K, V> e : entries) {
      putWithMetaData(e.getKey(), e.getValue(), e.getMetaData());
    }
  }

  @Override
  public void removeAllWithMetaData(Collection<EntryWithMetaData<K, V>> entries) {
    for (EntryWithMetaData<K, V> e : entries) {
      removeWithMetaData(e.getKey(), e.getMetaData());
    }
  }

  @Override
  public V remove(Object obj) {
    K key = (K) obj;
    return removeWithMetaData(key, null);
  }

  @Override
  public int size() {
    return localBufferedMap.getSize() + toolkitCache.size();
  }

  @Override
  public Collection<V> values() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return toolkitCache.getName();
  }

  @Override
  public boolean isDestroyed() {
    return toolkitCache.isDestroyed();
  }

  @Override
  public void destroy() {
    bulkLoadEnabledNodesSet.disposeLocally();
    toolkitCache.destroy();
  }

  @Override
  public void disposeLocally() {
    bulkLoadEnabledNodesSet.disposeLocally();
    toolkitCache.disposeLocally();
  }

  @Override
  public V putIfAbsent(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V replace(K key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetaData createMetaData(String category) {
    return toolkitCache.createMetaData(category);
  }

  @Override
  public EntryWithMetaData<K, V> createEntryWithMetaData(K key, V value, MetaData metaData) {
    return toolkitCache.createEntryWithMetaData(key, value, metaData);
  }

  @Override
  public void setMetaDataCallback(ToolkitCacheMetaDataCallback callback) {
    toolkitCache.setMetaDataCallback(callback);
  }

  @Override
  public void putNoReturnWithMetaData(K key, V value, MetaData metaData) {
    putWithMetaData(key, value, now(), ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS,
                    ToolkitCacheConfigFields.NO_MAX_TTL_SECONDS, metaData);
  }

  @Override
  public void putNoReturnWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds,
                                      int customMaxTTLSeconds, MetaData metaData) {
    // custom tti/ttl not supported
    putNoReturnWithMetaData(key, value, metaData);
  }

  @Override
  public V putIfAbsentWithMetaData(K key, V value, MetaData metaData) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V putIfAbsentWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds,
                                   int customMaxTTLSeconds, MetaData metaData) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearWithMetaData(MetaData metaData) {
    localBufferedMap.clear(metaData);
    toolkitCache.clearWithMetaData(metaData);
  }

  @Override
  public void removeNoReturnWithMetaData(Object key, MetaData metaData) {
    removeWithMetaData(key, metaData);
  }

  @Override
  public V removeWithMetaData(Object key, MetaData metaData) {
    V rv = localBufferedMap.remove((K) key, metaData);
    if (rv == null) {
      rv = toolkitCache.get(key);
    }
    return rv;
  }

  @Override
  public boolean removeWithMetaData(Object key, Object value, MetaData metaData) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SearchBuilder createSearchBuilder() {
    return toolkitCache.createSearchBuilder();
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return toolkitCache.localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return toolkitCache.localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return localBufferedMap.getSize() + toolkitCache.localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return toolkitCache.localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return localBufferedMap.containsKey(key) || toolkitCache.containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return toolkitCache.containsKeyLocalOffHeap(key);
  }

  @Override
  public V putWithMetaData(K key, V value, MetaData metaData) {
    return putWithMetaData(key, value, now(), ToolkitCacheConfigFields.NO_MAX_TTI_SECONDS,
                           ToolkitCacheConfigFields.NO_MAX_TTL_SECONDS, metaData);
  }

  @Override
  public V putWithMetaData(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds,
                           MetaData metaData) {
    return localBufferedMap.put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds, metaData);
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    return toolkitCache.getNodesWithKeys(portableKeys);
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL, MetaData metadata) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unlockedRemoveNoReturn(Object k, MetaData metadata) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearLocalCache() {
    throw new UnsupportedOperationException();
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    throw new UnsupportedOperationException();
  }

}
