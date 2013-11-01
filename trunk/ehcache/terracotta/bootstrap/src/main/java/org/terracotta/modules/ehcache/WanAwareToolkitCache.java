package org.terracotta.modules.ehcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.cache.VersionUpdateListener;
import org.terracotta.toolkit.internal.cache.VersionedValue;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * A wrapper around {@link ToolkitCacheInternal}
 * which rejects all user actions if the WAN-enabled cache is deactivated.
 *
 * @author Eugene Shelestovich
 */
public class WanAwareToolkitCache<K, V> implements ToolkitCacheInternal<K, V> {

  private static final Logger                       LOGGER           = LoggerFactory.getLogger(WanAwareToolkitCache.class);
  private static final String                       CACHE_ACTIVE_KEY = "WAN-CACHE-ACTIVE";

  private final ToolkitCacheInternal<K, V>          delegate;
  private final ConcurrentMap<String, Serializable> configMap;
  private final NonStopFeature                      nonStop;
  private final ToolkitLock                         configMapLock;

  public WanAwareToolkitCache(final ToolkitCacheInternal<K, V> delegate,
                              final ToolkitMap<String, Serializable> configMap, final NonStopFeature nonStop) {
    this(delegate, configMap, nonStop, configMap.getReadWriteLock().writeLock());
  }

  /**
   * Constructor for Unit Tests only
   */
  WanAwareToolkitCache(final ToolkitCacheInternal<K, V> delegate, final ConcurrentMap<String, Serializable> configMap,
                       final NonStopFeature nonStop, final ToolkitLock configMapLock) {
    this.delegate = delegate;
    this.configMap = configMap;
    this.nonStop = nonStop;
    this.configMapLock = configMapLock;
    configMap.putIfAbsent(CACHE_ACTIVE_KEY, false);
  }

  /**
   * Can the cache handle user actions ?
   *
   * @return {@code true} if the cache is active, {@code false} otherwise
   */
  public boolean isActive() {
    final Serializable active = configMap.get(CACHE_ACTIVE_KEY);
    return (active == null) ? false : (Boolean)active;
  }

  /**
   * Activates WAN-enabled cache, so it can start handling user actions.
   */
  public void activate() {
    setState(true);
    notifyClients();
  }

  /**
   * Deactivates WAN-enabled cache, so it rejects all user actions.
   */
  public void deactivate() {
    setState(false);
  }

  private void setState(boolean active) {
    configMap.replace(CACHE_ACTIVE_KEY, !active, active);
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(final Set portableKeys) {
    waitIfRequired();
    return delegate.getNodesWithKeys(portableKeys);
  }

  @Override
  public void unlockedPutNoReturn(final K k, final V v, final int createTime, final int customTTI, final int customTTL) {
    waitIfRequired();
    delegate.unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);
  }

  @Override
  public void unlockedRemoveNoReturn(final Object k) {
    waitIfRequired();
    delegate.unlockedRemoveNoReturn(k);
  }

  @Override
  public V unlockedGet(final Object k, final boolean quiet) {
    waitIfRequired();
    return delegate.unlockedGet(k, quiet);
  }

  @Override
  public Map<K, V> unlockedGetAll(final Collection<K> keys, final boolean quiet) {
    waitIfRequired();
    return delegate.unlockedGetAll(keys, quiet);
  }

  @Override
  public void removeAll(final Set<K> keys) {
    waitIfRequired();
    delegate.removeAll(keys);
  }


  @Override
  public V put(final K key, final V value, final int createTimeInSecs, final int customMaxTTISeconds,
               final int customMaxTTLSeconds) {
    waitIfRequired();
    return delegate.put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public V putIfAbsent(final K key, final V value, final long createTimeInSecs, final int maxTTISeconds,
                       final int maxTTLSeconds) {
    waitIfRequired();
    return delegate.putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
  }

  @Override
  public void putNoReturn(final K key, final V value, final long createTimeInSecs, final int maxTTISeconds,
                          final int maxTTLSeconds) {
    waitIfRequired();
    delegate.putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
  }

  @Override
  public Map<K, V> getAllQuiet(final Collection<K> keys) {
    waitIfRequired();
    return delegate.getAllQuiet(keys);
  }

  @Override
  public V getQuiet(final Object key) {
    waitIfRequired();
    return delegate.getQuiet(key);
  }


  @Override
  public Map<K, V> getAll(final Collection<? extends K> keys) {
    waitIfRequired();
    return delegate.getAll(keys);
  }

  @Override
  public void putNoReturn(final K key, final V value) {
    waitIfRequired();
    delegate.putNoReturn(key, value);
  }

  @Override
  public void removeNoReturn(final Object key) {
    waitIfRequired();
    delegate.removeNoReturn(key);
  }

  @Override
  public V putIfAbsent(final K key, final V value) {
    waitIfRequired();
    return delegate.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    waitIfRequired();
    return delegate.remove(key, value);
  }

  @Override
  public boolean replace(final K key, final V oldValue, final V newValue) {
    waitIfRequired();
    return delegate.replace(key, oldValue, newValue);
  }

  @Override
  public V replace(final K key, final V value) {
    waitIfRequired();
    return delegate.replace(key, value);
  }

  @Override
  public int size() {
    waitIfRequired();
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    waitIfRequired();
    return delegate.isEmpty();
  }

  @Override
  public boolean containsKey(final Object key) {
    waitIfRequired();
    return delegate.containsKey(key);
  }

  @Override
  public boolean containsValue(final Object value) {
    waitIfRequired();
    return delegate.containsValue(value);
  }

  @Override
  public V get(final Object key) {
    waitIfRequired();
    return delegate.get(key);
  }

  @Override
  public V put(final K key, final V value) {
    waitIfRequired();
    return delegate.put(key, value);
  }

  @Override
  public V remove(final Object key) {
    waitIfRequired();
    return delegate.remove(key);
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> m) {
    waitIfRequired();
    delegate.putAll(m);
  }

  @Override
  public void clear() {
    waitIfRequired();
    delegate.clear();
  }

  @Override
  public Set<K> keySet() {
    waitIfRequired();
    return delegate.keySet();
  }

  @Override
  public Collection<V> values() {
    waitIfRequired();
    return delegate.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    waitIfRequired();
    return delegate.entrySet();
  }

  @Override
  public void destroy() {
    waitIfRequired();
    delegate.destroy();
  }

  @Override
  public boolean equals(final Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean isDestroyed() {
    return delegate.isDestroyed();
  }


  @Override
  public void setAttributeExtractor(final ToolkitAttributeExtractor<K, V> attrExtractor) {
    delegate.setAttributeExtractor(attrExtractor);
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return delegate.createQueryBuilder();
  }

  @Override
  public boolean isBulkLoadEnabled() {
    return delegate.isBulkLoadEnabled();
  }

  @Override
  public boolean isNodeBulkLoadEnabled() {
    return delegate.isNodeBulkLoadEnabled();
  }

  @Override
  public void setNodeBulkLoadEnabled(final boolean enabledBulkLoad) {
    delegate.setNodeBulkLoadEnabled(enabledBulkLoad);
  }

  @Override
  public void waitUntilBulkLoadComplete() throws InterruptedException {
    delegate.waitUntilBulkLoadComplete();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public void quickClear() {
    delegate.quickClear();
  }

  @Override
  public int quickSize() {
    return delegate.quickSize();
  }

  @Override
  public void clearLocalCache() {
    delegate.clearLocalCache();
  }

  @Override
  public V unsafeLocalGet(final Object key) {
    return delegate.unsafeLocalGet(key);
  }

  @Override
  public boolean containsLocalKey(final Object key) {
    return delegate.containsLocalKey(key);
  }

  @Override
  public int localSize() {
    return delegate.localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return delegate.localKeySet();
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return delegate.localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return delegate.localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return delegate.localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return delegate.localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(final Object key) {
    return delegate.containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(final Object key) {
    return delegate.containsKeyLocalOffHeap(key);
  }

  @Override
  public void disposeLocally() {
    delegate.disposeLocally();
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(final K key) {
    return delegate.createLockForKey(key);
  }

  @Override
  public void setConfigField(final String name, final Serializable value) {
    delegate.setConfigField(name, value);
  }

  @Override
  public Configuration getConfiguration() {
    return delegate.getConfiguration();
  }

  @Override
  public void addListener(final ToolkitCacheListener<K> listener) {
    delegate.addListener(listener);
  }


  // **************** Methods called from Orchestrator - START **********************************
  // These methods should not wait for cache to become active
  // *********************************************************************************************

  @Override
  public void putIfAbsentVersioned(final K key, final V value, final long version) {
    delegate.putIfAbsentVersioned(key, value, version);
  }

  @Override
  public void putVersioned(final K key, final V value, final long version) {
    delegate.putVersioned(key, value, version);
  }

  @Override
  public void putVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                           final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    delegate.putVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public void putIfAbsentVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                                   final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    delegate.putIfAbsentVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public void unlockedPutNoReturnVersioned(final K k, final V v, final long version, final int createTime,
                                           final int customTTI, final int customTTL) {
    delegate.unlockedPutNoReturnVersioned(k, v, version, createTime, customTTI, customTTL);
  }

  @Override
  public void removeVersioned(final Object key, final long version) {
    delegate.removeVersioned(key, version);
  }

  @Override
  public void registerVersionUpdateListener(final VersionUpdateListener listener) {
    delegate.registerVersionUpdateListener(listener);
  }

  @Override
  public Set<K> keySetForSegment(final int segmentIndex) {
    return delegate.keySetForSegment(segmentIndex);
  }

  @Override
  public VersionedValue<V> getVersionedValue(final Object key) {
    return delegate.getVersionedValue(key);
  }

  @Override
  public void removeListener(final ToolkitCacheListener<K> listener) {
    delegate.removeListener(listener);
  }

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    delegate.unlockedRemoveNoReturnVersioned(key, version);
  }

  /**
   * Same as {@link #clear()}, except that it does not generate any server events and completely ignores
   * {@link #isActive()} flag.
   */
  @Override
  public void clearVersioned() {
    delegate.clearVersioned();
  }


  // **************** Methods called from Orchestrator - END **********************************
  // ********************************************************************************************


  /**
   * This method makes the current thread wait until the Cache becomes active or the NonStop timeout breaches.
   */
  private void waitIfRequired() {
    if (!isActive()) {
      LOGGER.info("Cache '{}' not active. Waiting for the Orchestrator to mark it active", delegate.getName());
      waitUntilActive();
      LOGGER.info("Cache '{}' is now active", delegate.getName());
    }
  }

  void waitUntilActive() {
    configMapLock.lock();
    try {
      while (!isActive()) {
        try {
          configMapLock.getCondition().await();
        } catch (InterruptedException e) {
          if (nonStop.isTimedOut()) {
            LOGGER.error("Operation timed-out while waitng for the cache '{}' to become active",
                         delegate.getName());
            throw new NonStopException("Cache '" + delegate.getName() + "' not active currently.");
          }
        }
      }
    } finally {
      configMapLock.unlock();
    }
  }

  /**
   * Notifies the waiting client.
   */
  void notifyClients() {
    configMapLock.lock();
    try {
      configMapLock.getCondition().signalAll();
    } finally {
      configMapLock.unlock();
    }
  }

}
