/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.bulkload;

import net.sf.ehcache.store.StoreListener;

import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.cache.VersionUpdateListener;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BulkLoadToolkitCache<K, V> implements ToolkitCacheInternal<K, V> {
  private static final String BULK_LOAD_LOCK_ID = "bulk-load-concurrent-lock";

  private final ToolkitLogger              logger;
  private final ReentrantReadWriteLock     readWriteLock              = new ReentrantReadWriteLock();
  private final ToolkitCacheInternal<K, V> toolkitCache;
  private final BulkLoadEnabledNodesSet    bulkLoadEnabledNodesSet;
  private final ToolkitInternal            toolkitInternal;
  private final Lock                       concurrentLock;
  private boolean                          localCacheEnabledBeforeBulkloadEnabled;

  private final String                     name;
  private final BulkLoadShutdownHook       bulkLoadShutdownHook;
  private final boolean                    loggingEnabled;

  public BulkLoadToolkitCache(ToolkitInternal toolkit, String name, ToolkitCacheInternal<K, V> aggregateServerMap,
                              BulkLoadShutdownHook bulkLoadShutdownHook, StoreListener listener) {
    this.toolkitInternal = toolkit;
    this.name = name;
    this.logger = toolkit.getLogger(BulkLoadToolkitCache.class.getName());
    this.toolkitCache = aggregateServerMap;
    this.bulkLoadEnabledNodesSet = new BulkLoadEnabledNodesSet(toolkit, name, listener);
    this.bulkLoadShutdownHook = bulkLoadShutdownHook;
    this.loggingEnabled = BulkLoadConstants.isLoggingEnabled(toolkit.getProperties());
    this.concurrentLock = toolkit.getLock(BULK_LOAD_LOCK_ID, ToolkitLockTypeInternal.CONCURRENT);
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
        if (!isBulkLoadEnabledInCurrentNode()) {
          enterBulkLoadMode();
        } else {
          if (loggingEnabled) {
            debug("Trying to enable bulk-load mode when already bulk-loading.");
          }
        }
      } else {
        // turning off bulk-load
        if (isBulkLoadEnabledInCurrentNode()) {
          exitBulkLoadMode();
        } else {
          if (loggingEnabled) {
            debug("Trying to disable bulk-load mode when not bulk-loading.");
          }
        }
      }
    } finally {
      releaseLocalWriteLock();
    }
  }

  private void enterBulkLoadMode() {
    if (loggingEnabled) {
      debug("Enabling bulk-load");
    }

    localCacheEnabledBeforeBulkloadEnabled = toolkitCache.getConfiguration()
        .getBoolean(ToolkitConfigFields.LOCAL_CACHE_ENABLED_FIELD_NAME);

    // disable local cache
    if (localCacheEnabledBeforeBulkloadEnabled) {
      setLocalCacheEnabled(false);
    }

    // add current node
    bulkLoadEnabledNodesSet.addCurrentNode();

    bulkLoadShutdownHook.registerCache(this);
  }

  private void exitBulkLoadMode() {
    if (loggingEnabled) {
      debug("Turning off bulk-load");
    }

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
  }

  private void setLocalCacheEnabled(boolean enabled) {
    new ToolkitCacheConfigBuilder().localCacheEnabled(enabled).apply(toolkitCache);
  }

  public void waitUntilBulkLoadCompleteInCluster() throws InterruptedException {
    bulkLoadEnabledNodesSet.waitUntilSetEmpty();
  }

  public boolean isBulkLoadEnabledInCluster() {
    return bulkLoadEnabledNodesSet.isBulkLoadEnabledInCluster();
  }

  public boolean isBulkLoadEnabledInCurrentNode() {
    return bulkLoadEnabledNodesSet.isBulkLoadEnabledInCurrentNode();
  }

  @Override
  public V getQuiet(Object key) {
    return doGet(key, true);
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    concurrentLock.lock();
    try {
      toolkitCache.unlockedPutNoReturn(key, value, (int) createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    } finally {
      concurrentLock.unlock();
    }
  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V unsafeLocalGet(Object key) {
    return toolkitCache.unsafeLocalGet(key);
  }

  @Override
  public void putNoReturn(K key, V value) {
    putNoReturn(key, value, now(), ToolkitConfigFields.NO_MAX_TTI_SECONDS, ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public int localSize() {
    return toolkitCache.localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return toolkitCache.localKeySet();
  }

  @Override
  public boolean containsLocalKey(Object key) {
    return toolkitCache.containsLocalKey(key);
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    return toolkitCache.getAll(keys);
  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    return toolkitCache.getAllQuiet(keys);
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
  public boolean containsKey(Object key) {
    return toolkitCache.containsKey(key);
  }

  @Override
  public boolean containsValue(Object arg0) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return toolkitCache.entrySet();
  }

  @Override
  public V get(Object obj) {
    return doGet(obj, false);
  }

  public V doGet(Object obj, boolean quiet) {
    return toolkitCache.unlockedGet(obj, quiet);
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Set<K> keySet() {
    return toolkitCache.keySet();
  }

  private int now() {
    return (int) System.currentTimeMillis() / 1000;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) {
    for (Entry<? extends K, ? extends V> e : map.entrySet()) {
      putNoReturn(e.getKey(), e.getValue());
    }
  }

  @Override
  public void removeAll(Set<K> keys) {
    for (K key : keys) {
      remove(key);
    }
  }

  @Override
  public void removeVersioned(final Object key, final long version) {
    concurrentLock.lock();
    try {
      toolkitCache.unlockedRemoveNoReturnVersioned(key, version);
    } finally {
      concurrentLock.unlock();
    }
  }

  @Override
  public void registerVersionUpdateListener(final VersionUpdateListener listener) {
    toolkitCache.registerVersionUpdateListener(listener);
  }

  @Override
  public int size() {
    return toolkitCache.size();
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
    // this will unregister the shutdown hook.
    setBulkLoadEnabledInCurrentNode(false);
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
  public void clear() {
    toolkitCache.clear();
  }

  @Override
  public void removeNoReturn(Object key) {
    concurrentLock.lock();
    try {
      toolkitCache.unlockedRemoveNoReturn(key);
    } finally {
      concurrentLock.unlock();
    }
  }

  @Override
  public V remove(Object key) {
    V rv = toolkitCache.unlockedGet(key, true);
    removeNoReturn(key);
    return rv;
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return toolkitCache.createQueryBuilder();
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
    return toolkitCache.localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return toolkitCache.localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(Object key) {
    return toolkitCache.containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(Object key) {
    return toolkitCache.containsKeyLocalOffHeap(key);
  }

  @Override
  public V put(K key, V value) {
    return put(key, value, now(), ToolkitConfigFields.NO_MAX_TTI_SECONDS, ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public V put(K key, V value, int createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    V rv = toolkitCache.unlockedGet(key, true);
    putNoReturn(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    return rv;
  }

  @Override
  public void putVersioned(final K key, final V value, final long version) {
    putVersioned(key, value, version, now(), ToolkitConfigFields.NO_MAX_TTI_SECONDS, ToolkitConfigFields.NO_MAX_TTL_SECONDS);
  }

  @Override
  public void putVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                           final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    concurrentLock.lock();
    try {
      toolkitCache.unlockedPutNoReturnVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    } finally {
      concurrentLock.unlock();
    }
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(Set portableKeys) {
    return toolkitCache.getNodesWithKeys(portableKeys);
  }

  @Override
  public void unlockedPutNoReturn(K k, V v, int createTime, int customTTI, int customTTL) {
    toolkitCache.unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);
  }

  @Override
  public void unlockedPutNoReturnVersioned(final K k, final V v, final long version, final int createTime,
                                           final int customTTI, final int customTTL) {
    toolkitCache.unlockedPutNoReturnVersioned(k, v, version, createTime, customTTI, customTTL);
  }

  @Override
  public void unlockedRemoveNoReturn(Object k) {
    toolkitCache.unlockedRemoveNoReturn(k);
  }

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    toolkitCache.unlockedRemoveNoReturnVersioned(key, version);
  }

  @Override
  public void clearLocalCache() {
    throw new UnsupportedOperationException();
  }

  @Override
  public V unlockedGet(Object k, boolean quiet) {
    return doGet(k, quiet);
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor<K, V> extractor) {
    toolkitCache.setAttributeExtractor(extractor);
  }

  @Override
  public Map<K, V> unlockedGetAll(Collection<K> keys, boolean quiet) {
    return quiet ? getAllQuiet(keys) : getAll(keys);
  }
}
