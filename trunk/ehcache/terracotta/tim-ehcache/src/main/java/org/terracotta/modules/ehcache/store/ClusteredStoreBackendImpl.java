/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.Element;
import net.sf.ehcache.event.RegisteredEventListeners;

import org.terracotta.bytecode.NotClearable;
import org.terracotta.cache.CacheConfig;
import org.terracotta.cache.LocallyCacheable;
import org.terracotta.cache.TerracottaDistributedCache;
import org.terracotta.cache.TimeSource;
import org.terracotta.cache.TimestampedValue;
import org.terracotta.collections.ClusteredMap;
import org.terracotta.collections.MutatedObjectHandle;
import org.terracotta.collections.MutationCallback;
import org.terracotta.config.Configuration;
import org.terracotta.locking.ClusteredLock;
import org.terracotta.meta.MetaData;

import java.util.Map;
import java.util.Set;

public class ClusteredStoreBackendImpl<K, V> implements ClusteredStoreBackend<K, V>, NotClearable {

  static {
    for (Class i : ClusteredStoreBackendImpl.class.getInterfaces()) {
      System.err.println("XXX: " + i);
    }
  }

  private final TerracottaDistributedCache<K, V> tdc;
  private final ValueModeHandler                 valueModeHandler;
  private final String                           cacheName;

  private transient RegisteredEventListeners     registeredEventListeners;
  private transient volatile ClusteredStore      clusteredStore;

  public ClusteredStoreBackendImpl(final Configuration config, final ClusteredMap cdm,
                                   final ValueModeHandler valueModeHandler, final RegisteredEventListeners listeners,
                                   final String cacheName, ClusteredStore clusteredStore) {
    this.cacheName = cacheName;
    this.tdc = new TDCWithEvents(config, cdm);
    this.valueModeHandler = valueModeHandler;

    initializeTransients(listeners, clusteredStore);
  }

  public void loadReferences() {
    tdc.initializeLocalCache();
    valueModeHandler.getClass();
  }

  public TerracottaDistributedCache<K, V> getTerracottaDistributedCache() {
    return tdc;
  }

  /**
   * See {@link ClusteredStore#initalizeTransients}
   */
  final public void initializeTransients(RegisteredEventListeners listeners, ClusteredStore store) {
    this.registeredEventListeners = listeners;
    this.clusteredStore = store;
  }

  public void clearLocalCache() {
    ((LocallyCacheable) tdc).clearLocalCache();
  }

  public void unpinAll() {
    tdc.unpinAll();
  }

  public boolean isPinned(Object key) {
    return tdc.isPinned(key);
  }

  public void setPinned(Object key, boolean pinned) {
    tdc.setPinned(key, pinned);
  }

  public boolean containsKey(Object key) {
    return tdc.containsKey(key);
  }

  public boolean containsLocalKey(K key) {
    return tdc.containsLocalKey(key);
  }

  public boolean unlockedContainsLocalKey(K key) {
    return containsLocalKey(key);
  }

  public TimestampedValue<V> getTimestampedValue(K key) {
    return tdc.getTimestampedValue(key);
  }

  public TimestampedValue<V> getTimestampedValueQuiet(K key) {
    return tdc.getTimestampedValueQuiet(key);
  }

  public Map<K, TimestampedValue<V>> getTimestampedValues(final Set<K> keys, boolean quiet) {
    return tdc.getTimestampedValues(keys, quiet);
  }

  public TimestampedValue<V> removeTimestampedValue(K key, MetaData searchMetaData) {
    if (searchMetaData != null) {
      return tdc.removeTimestampedValueWithCallback(key, createCallback(searchMetaData));
    } else {
      return tdc.removeTimestampedValue(key);
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
      tdc.clearWithCallback(createCallback(searchMetaData));
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
    tdc.shutdown();
  }

  public TimeSource getTimeSource() {
    return tdc.getTimeSource();
  }

  public void unlockedReplaceNoReturn(K key, V currentValue, V newValue, MetaData searchMetaData) {
    if (searchMetaData != null) {
      tdc.unlockedReplaceNoReturnWithCallback(key, currentValue, newValue, createCallback(searchMetaData));
    } else {
      tdc.unlockedReplaceNoReturn(key, currentValue, newValue);
    }
  }

  public void unlockedRemoveNoReturn(K key, V oldValue, MetaData searchMetaData) {
    if (searchMetaData != null) {
      tdc.unlockedRemoveNoReturnWithCallback(key, oldValue, createCallback(searchMetaData));
    } else {
      tdc.unlockedRemoveNoReturn(key, oldValue);
    }
  }

  public void unlockedPutIfAbsentNoReturn(K key, V value, MetaData searchMetaData) {
    if (searchMetaData != null) {
      tdc.unlockedPutIfAbsentNoReturnWithCallback(key, value, createCallback(searchMetaData));
    } else {
      tdc.unlockedPutIfAbsentNoReturn(key, value);
    }
  }

  public void putNoReturn(K key, V value, MetaData searchMetaData) {
    if (searchMetaData != null) {
      tdc.putNoReturnWithCallback(key, value, createCallback(searchMetaData));
    } else {
      tdc.putNoReturn(key, value);
    }
  }

  public void unlockedPutNoReturn(K key, V value, MetaData searchMetaData) {
    if (searchMetaData != null) {
      tdc.unlockedPutNoReturnWithCallback(key, value, createCallback(searchMetaData));
    } else {
      tdc.unlockedPutNoReturn(key, value);
    }
  }

  public TimestampedValue<V> unsafeGetTimestampedValue(K key, boolean quiet) {
    return tdc.unsafeGetTimestampedValue(key, quiet);
  }

  public TimestampedValue<V> unlockedGetTimestampedValue(K key, boolean quiet) {
    return tdc.unlockedGetTimestampedValue(key, quiet);
  }

  public Map<K, TimestampedValue<V>> unlockedGetAllTimeStampedValues(final Set<K> keys, final boolean quiet) {
    return tdc.unlockedGetAllTimestampedValue(keys, quiet);
  }

  public boolean unlockedContainsKey(Object key) {
    return tdc.unlockedContainsKey(key);
  }

  public void removeNoReturn(K key, MetaData searchMetaData) {
    if (searchMetaData != null) {
      tdc.removeNoReturnWithCallback(key, createCallback(searchMetaData));
    } else {
      tdc.removeNoReturn(key);
    }
  }

  public void unlockedRemoveNoReturn(Object key, MetaData searchMetaData) {
    if (searchMetaData != null) {
      tdc.unlockedRemoveNoReturnWithCallback((K) key, createCallback(searchMetaData));
    } else {
      tdc.unlockedRemoveNoReturn(key);
    }
  }

  public CacheConfig getConfig() {
    return tdc.getConfig();
  }

  public ClusteredLock createFinegrainedLock(K key) {
    return tdc.createFinegrainedLock(key);
  }

  private MutationCallback<K, TimestampedValue<V>> createCallback(MetaData metaData) {
    if (metaData == null) { throw new AssertionError(); }
    return new MetaDataCallback(metaData);
  }

  public void recalculateLocalCacheSize(Object key) {
    tdc.recalculateLocalCacheSize(key);
  }

  private class TDCWithEvents extends TerracottaDistributedCache<K, V> {

    public TDCWithEvents(Configuration config, ClusteredMap cdm) {
      super(config, cdm);
    }

    @Override
    protected void onEvict(final K key, final TimestampedValue<V> value) {
      Element element = null;
      if (value == null) {
        element = new Element(valueModeHandler.getRealKeyObject(key), null);
      } else {
        element = valueModeHandler.createElement(valueModeHandler.getRealKeyObject(key), value);
      }
      registeredEventListeners.notifyElementEvicted(element, false);
    }

    @Override
    protected void onExpiry(final K key, final TimestampedValue<V> value) {
      Element element = null;
      if (value == null) {
        element = new Element(valueModeHandler.getRealKeyObject(key), null);
      } else {
        element = valueModeHandler.createElement(valueModeHandler.getRealKeyObject(key), value);
      }
      registeredEventListeners.notifyElementExpiry(element, false);
    }

    @Override
    protected MutationCallback<K, TimestampedValue<V>> getEvictRemoveCallback() {
      if (!clusteredStore.isSearchable()) return null;

      MetaData metaData = MetaData.create("SEARCH");
      metaData.add("CACHENAME@", cacheName);
      metaData.add("COMMAND@", "NOT-SET");
      return new MetaDataCallback(metaData);
    }
  }

  private class MetaDataCallback implements MutationCallback<K, TimestampedValue<V>> {

    private final MetaData metaData;

    MetaDataCallback(MetaData metaData) {
      this.metaData = metaData;
    }

    public void putEvent(K key, TimestampedValue<V> value, MutatedObjectHandle handle) {
      metaData.set("COMMAND@", SearchConstants.PUT_COMMAND);
      metaData.addOidFor("VALUE@", value);
      handle.addMetaData(metaData);
    }

    public void clearEvent(MutatedObjectHandle handle) {
      metaData.set("COMMAND@", SearchConstants.CLEAR_COMMAND);
      handle.addMetaData(metaData);
    }

    public void removeEvent(K key, MutatedObjectHandle handle) {
      metaData.set("COMMAND@", SearchConstants.REMOVE_COMMAND);
      metaData.add("KEY@", key.toString());
      handle.addMetaData(metaData);
    }

    public void replaceEvent(K key, TimestampedValue<V> currentValue, TimestampedValue<V> newValue,
                             MutatedObjectHandle handle) {
      metaData.set("COMMAND@", SearchConstants.REPLACE_COMMAND);
      metaData.addOidFor("VALUE@", newValue);
      metaData.addOidFor("PREV_VALUE@", currentValue);
      handle.addMetaData(metaData);
    }

    public void putIfAbsentEvent(K key, TimestampedValue<V> value, MutatedObjectHandle handle) {
      metaData.set("COMMAND@", SearchConstants.PUT_IF_ABSENT_COMMAND);
      metaData.addOidFor("VALUE@", value);
      handle.addMetaData(metaData);
    }

    public void removeIfValueEqualEvent(K key, TimestampedValue<V> value, MutatedObjectHandle handle) {
      metaData.set("COMMAND@", SearchConstants.REMOVE_IF_VALUE_EQUAL_COMMAND);
      metaData.add("", 1);
      metaData.add("", key);
      metaData.addOidFor("", value);
      handle.addMetaData(metaData);
    }
  }

  public long localOnHeapSizeInBytes() {
    return tdc.localOnHeapSizeInBytes();
  }

  public long localOffHeapSizeInBytes() {
    return tdc.localOffHeapSizeInBytes();
  }

  public int localOnHeapSize() {
    return tdc.localOnHeapSize();
  }

  public int localOffHeapSize() {
    return tdc.localOffHeapSize();
  }

  public boolean containsKeyLocalOnHeap(K key) {
    return tdc.containsKeyLocalOnHeap(key);
  }

  public boolean containsKeyLocalOffHeap(K key) {
    return tdc.containsKeyLocalOffHeap(key);
  }

  public void setTargetMaxTotalCount(int targetMaxTotalCount) {
    tdc.setTargetMaxTotalCount(targetMaxTotalCount);
    getConfig().setTargetMaxTotalCount(targetMaxTotalCount);
  }

  public void setMaxTTI(int maxTTI) {
    tdc.setMaxTTI(maxTTI);
    getConfig().setMaxTTISeconds(maxTTI);
  }

  public void setMaxTTL(int maxTTL) {
    tdc.setMaxTTL(maxTTL);
    getConfig().setMaxTTLSeconds(maxTTL);
  }

  public void setTargetMaxInMemoryCount(int targetMaxInMemoryCount) {
    // Don't set in MutableConfig as this is a local property
    tdc.setMaxEntriesLocalHeap(targetMaxInMemoryCount);
  }

  public void setLoggingEnabled(boolean loggingEnabled) {
    getConfig().setLoggingEnabled(loggingEnabled);
  }

  public void setMaxBytesLocalHeap(long maxBytesLocalHeap) {
    // Don't set in MutableConfig as this is a local property
    tdc.setMaxBytesLocalHeap(maxBytesLocalHeap);
  }

  public void setLocalCacheEnabled(boolean enabled) {
    tdc.setLocalCacheEnabled(enabled);
  }
}
