package org.terracotta.modules.ehcache;

import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.cache.VersionUpdateListener;
import org.terracotta.toolkit.internal.cache.VersionedValue;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A wrapper around {@link ToolkitCacheInternal}
 * which rejects all user actions if the WAN-enabled cache is deactivated.
 *
 * @author Eugene Shelestovich
 */
public class WanAwareToolkitCache<K, V> implements ToolkitCacheInternal<K, V> {

  private static final String CACHE_ACTIVE_KEY = "WAN-CACHE-ACTIVE";
  private static final String NOT_ACTIVE_MSG = "WAN-enabled cache is not active yet";

  private final ToolkitCacheInternal<K, V> delegate;
  private final ToolkitMap<String, Serializable> configMap;

  public WanAwareToolkitCache(final ToolkitCacheInternal<K, V> delegate,
                              final ToolkitMap<String, Serializable> configMap) {
    this.delegate = delegate;
    this.configMap = configMap;

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
  }

  /**
   * Deactivates WAN-enabled cache, so it rejects all user actions.
   */
  public void deactivate() {
    setState(false);
  }

  /**
   * Same as {@link #clear()}, except that it does not generate any server events
   * and completely ignores {@link #isActive()} flag.
   */
  @Override
  public void clearVersioned() {
    //TODO: do not generate server events
    delegate.clearVersioned();
  }

  private void setState(boolean active) {
    configMap.replace(CACHE_ACTIVE_KEY, !active, active);
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(final Set portableKeys) {
    if (isActive()) {
      return delegate.getNodesWithKeys(portableKeys);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public void unlockedPutNoReturn(final K k, final V v, final int createTime, final int customTTI, final int customTTL) {
    if (isActive()) {
      delegate.unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public void unlockedPutNoReturnVersioned(final K k, final V v, final long version, final int createTime,
                                           final int customTTI, final int customTTL) {
    delegate.unlockedPutNoReturnVersioned(k, v, version, createTime, customTTI, customTTL);
  }

  @Override
  public void unlockedRemoveNoReturn(final Object k) {delegate.unlockedRemoveNoReturn(k);}

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    delegate.unlockedRemoveNoReturnVersioned(key, version);
  }

  @Override
  public V unlockedGet(final Object k, final boolean quiet) {return delegate.unlockedGet(k, quiet);}

  @Override
  public Map<K, V> unlockedGetAll(final Collection<K> keys, final boolean quiet) {
    return delegate.unlockedGetAll(keys, quiet);
  }

  @Override
  public void clearLocalCache() {delegate.clearLocalCache();}

  @Override
  public V unsafeLocalGet(final Object key) {return delegate.unsafeLocalGet(key);}

  @Override
  public boolean containsLocalKey(final Object key) {return delegate.containsLocalKey(key);}

  @Override
  public int localSize() {return delegate.localSize();}

  @Override
  public Set<K> localKeySet() {return delegate.localKeySet();}

  @Override
  public long localOnHeapSizeInBytes() {return delegate.localOnHeapSizeInBytes();}

  @Override
  public long localOffHeapSizeInBytes() {return delegate.localOffHeapSizeInBytes();}

  @Override
  public int localOnHeapSize() {return delegate.localOnHeapSize();}

  @Override
  public int localOffHeapSize() {return delegate.localOffHeapSize();}

  @Override
  public boolean containsKeyLocalOnHeap(final Object key) {return delegate.containsKeyLocalOnHeap(key);}

  @Override
  public boolean containsKeyLocalOffHeap(final Object key) {return delegate.containsKeyLocalOffHeap(key);}

  @Override
  public V put(final K key, final V value, final int createTimeInSecs, final int customMaxTTISeconds,
               final int customMaxTTLSeconds) {
    if (isActive()) {
      return delegate.put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
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
  public void putIfAbsentVersioned(final K key, final V value, final long version) {
    delegate.putIfAbsentVersioned(key, value, version);
  }

  @Override
  public void putIfAbsentVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                                   final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    delegate.putIfAbsentVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public void disposeLocally() {delegate.disposeLocally();}

  @Override
  public void removeAll(final Set<K> keys) {
    if (isActive()) {
      delegate.removeAll(keys);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public void removeVersioned(final Object key, final long version) {delegate.removeVersioned(key, version);}

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
    if (isActive()) {
      delegate.removeListener(listener);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public void addListener(final ToolkitCacheListener<K> listener) {
    if (isActive()) {
      delegate.addListener(listener);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public V putIfAbsent(final K key, final V value, final long createTimeInSecs, final int maxTTISeconds,
                       final int maxTTLSeconds) {
    if (isActive()) {
      return delegate.putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public void putNoReturn(final K key, final V value, final long createTimeInSecs, final int maxTTISeconds,
                          final int maxTTLSeconds) {
    if (isActive()) {
      delegate.putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public Map<K, V> getAllQuiet(final Collection<K> keys) {
    if (isActive()) {
      return delegate.getAllQuiet(keys);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public V getQuiet(final Object key) {
    if (isActive()) {
      return delegate.getQuiet(key);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(final K key) {return delegate.createLockForKey(key);}

  @Override
  public void setConfigField(final String name, final Serializable value) {delegate.setConfigField(name, value);}

  @Override
  public Configuration getConfiguration() {return delegate.getConfiguration();}

  @Override
  public Map<K, V> getAll(final Collection<? extends K> keys) {
    if (isActive()) {
      return delegate.getAll(keys);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public void putNoReturn(final K key, final V value) {
    if (isActive()) {
      delegate.putNoReturn(key, value);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public void removeNoReturn(final Object key) {
    if (isActive()) {
      delegate.removeNoReturn(key);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public V putIfAbsent(final K key, final V value) {
    if (isActive()) {
      return delegate.putIfAbsent(key, value);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    if (isActive()) {
      return delegate.remove(key, value);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public boolean replace(final K key, final V oldValue, final V newValue) {
    if (isActive()) {
      return delegate.replace(key, oldValue, newValue);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public V replace(final K key, final V value) {
    if (isActive()) {
      return delegate.replace(key, value);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public int size() {return delegate.size();}

  @Override
  public boolean isEmpty() {return delegate.isEmpty();}

  @Override
  public boolean containsKey(final Object key) {
    if (isActive()) {
      return delegate.containsKey(key);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public boolean containsValue(final Object value) {
    if (isActive()) {
      return delegate.containsValue(value);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public V get(final Object key) {
    if (isActive()) {
      return delegate.get(key);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public V put(final K key, final V value) {
    if (isActive()) {
      return delegate.put(key, value);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public V remove(final Object key) {
    if (isActive()) {
      return delegate.remove(key);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> m) {
    if (isActive()) {
      delegate.putAll(m);
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public void clear() {
    if (isActive()) {
      delegate.clear();
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public Set<K> keySet() {
    if (isActive()) {
      return delegate.keySet();
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public Collection<V> values() {
    if (isActive()) {
      return delegate.values();
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    if (isActive()) {
      return delegate.entrySet();
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public boolean equals(final Object o) {return delegate.equals(o);}

  @Override
  public int hashCode() {return delegate.hashCode();}

  @Override
  public boolean isDestroyed() {return delegate.isDestroyed();}

  @Override
  public void destroy() {
    if (isActive()) {
      delegate.destroy();
    } else {
      throw new IllegalStateException(NOT_ACTIVE_MSG);
    }
  }

  @Override
  public void setAttributeExtractor(final ToolkitAttributeExtractor<K, V> attrExtractor) {
    delegate.setAttributeExtractor(attrExtractor);
  }

  @Override
  public QueryBuilder createQueryBuilder() {return delegate.createQueryBuilder();}

  @Override
  public boolean isBulkLoadEnabled() {return delegate.isBulkLoadEnabled();}

  @Override
  public boolean isNodeBulkLoadEnabled() {return delegate.isNodeBulkLoadEnabled();}

  @Override
  public void setNodeBulkLoadEnabled(final boolean enabledBulkLoad) {delegate.setNodeBulkLoadEnabled(enabledBulkLoad);}

  @Override
  public void waitUntilBulkLoadComplete() throws InterruptedException {delegate.waitUntilBulkLoadComplete();}

  @Override
  public String getName() {return delegate.getName();}

  @Override
  public void quickClear() {
    delegate.quickClear();

  }

  @Override
  public int quickSize() {
    return delegate.quickSize();
  }
}
