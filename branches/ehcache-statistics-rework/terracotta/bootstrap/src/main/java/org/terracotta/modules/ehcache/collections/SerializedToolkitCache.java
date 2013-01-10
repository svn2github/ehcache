/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved. Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.modules.ehcache.collections;

import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;
import org.terracotta.toolkit.serialization.ToolkitSerializer;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of {@link ToolkitCache} that supports serializable keys
 */
public class SerializedToolkitCache<K, V extends Serializable> implements ToolkitCache<K, V> {
  private final ToolkitCache<String, V> toolkitCache;
  private final ToolkitSerializer       toolkitSerializer;

  public SerializedToolkitCache(ToolkitCache toolkitMap, ToolkitSerializer toolkitSerializer) {
    this.toolkitCache = toolkitMap;
    this.toolkitSerializer = toolkitSerializer;
  }

  @Override
  public int size() {
    return this.toolkitCache.size();
  }

  @Override
  public boolean isEmpty() {
    return this.toolkitCache.isEmpty();
  }

  private String serializeToString(Object key) {
    return toolkitSerializer.serializeToString(key);
  }

  private Object deserializeFromString(String key) {
    return toolkitSerializer.deserializeFromString(key);
  }

  @Override
  public boolean containsKey(Object key) {
    return this.toolkitCache.containsKey(serializeToString(key));
  }

  @Override
  public V get(Object key) {
    return this.toolkitCache.get(serializeToString(key));
  }

  @Override
  public V put(K key, V value) {
    return this.toolkitCache.put(serializeToString(key), value);
  }

  @Override
  public V remove(Object key) {
    return this.toolkitCache.remove(serializeToString(key));
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    Map<String, V> tempMap = new HashMap<String, V>();
    for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
      tempMap.put(serializeToString(entry.getKey()), entry.getValue());
    }

    toolkitCache.putAll(tempMap);
  }

  @Override
  public void clear() {
    toolkitCache.clear();
  }

  @Override
  public Set<K> keySet() {
    return new ToolkitKeySet(toolkitCache.keySet(), toolkitSerializer);
  }

  @Override
  public boolean isDestroyed() {
    return toolkitCache.isDestroyed();
  }

  @Override
  public void destroy() {
    toolkitCache.destroy();
  }

  @Override
  public String getName() {
    return toolkitCache.getName();
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(K key) {
    return toolkitCache.createLockForKey(serializeToString(key));
  }

  @Override
  public void removeNoReturn(Object key) {
    toolkitCache.removeNoReturn(serializeToString(key));
  }

  @Override
  public void putNoReturn(K key, V value) {
    toolkitCache.putNoReturn(serializeToString(key), value);
  }

  @Override
  public void unpinAll() {
    toolkitCache.unpinAll();
  }

  @Override
  public boolean isPinned(K key) {
    return toolkitCache.isPinned(serializeToString(key));
  }

  @Override
  public void setPinned(K key, boolean pinned) {
    toolkitCache.setPinned(serializeToString(key), pinned);
  }

  @Override
  public Map<K, V> getAll(Collection<? extends K> keys) {
    HashSet<String> tempSet = new HashSet<String>();
    for (K key : keys) {
      tempSet.add(serializeToString(key));
    }

    Map<String, V> m = toolkitCache.getAll(tempSet);
    Map<K, V> tempMap = m.isEmpty() ? Collections.EMPTY_MAP : new HashMap<K, V>();

    for (Entry<String, V> entry : m.entrySet()) {
      tempMap.put((K) deserializeFromString(entry.getKey()), entry.getValue());
    }

    return tempMap;
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
  public boolean containsValue(Object value) {
    return toolkitCache.containsValue(value);
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return toolkitCache.putIfAbsent(serializeToString(key), value);
  }

  @Override
  public Set<java.util.Map.Entry<K, V>> entrySet() {
    return new ToolkitEntrySet(this.toolkitCache.entrySet(), toolkitSerializer);
  }

  @Override
  public Collection<V> values() {
    return this.toolkitCache.values();
  }

  @Override
  public boolean remove(Object key, Object value) {
    return this.toolkitCache.remove(serializeToString(key), value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return this.toolkitCache.replace(serializeToString(key), oldValue, newValue);
  }

  @Override
  public V replace(K key, V value) {
    return this.toolkitCache.replace(serializeToString(key), value);
  }

  private static class ToolkitEntrySet<K, V> implements Set<java.util.Map.Entry<K, V>> {
    private final Set<java.util.Map.Entry<String, V>> set;
    private final ToolkitSerializer                   serializer;

    public ToolkitEntrySet(Set<java.util.Map.Entry<String, V>> set, ToolkitSerializer serializer) {
      this.set = set;
      this.serializer = serializer;
    }

    @Override
    public int size() {
      return set.size();
    }

    @Override
    public boolean isEmpty() {
      return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      if (!(o instanceof Map.Entry)) { return false; }

      Map.Entry<K, V> entry = (java.util.Map.Entry<K, V>) o;
      ToolkitCacheEntry<String, V> toolkitEntry = null;
      toolkitEntry = new ToolkitCacheEntry<String, V>(serializer.serializeToString(entry.getKey()), entry.getValue());
      return this.set.contains(toolkitEntry);
    }

    @Override
    public Iterator<java.util.Map.Entry<K, V>> iterator() {
      return new ToolkitEntryIterator<K, V>(set.iterator(), serializer);
    }

    @Override
    public Object[] toArray() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(java.util.Map.Entry<K, V> e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends java.util.Map.Entry<K, V>> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }

  private static class ToolkitEntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {

    private final Iterator<Map.Entry<String, V>> iter;
    private final ToolkitSerializer              serializer;

    public ToolkitEntryIterator(Iterator<Map.Entry<String, V>> iter, ToolkitSerializer serializer) {
      this.iter = iter;
      this.serializer = serializer;
    }

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public java.util.Map.Entry<K, V> next() {
      Map.Entry<String, V> entry = iter.next();
      if (entry == null) { return null; }
      return new ToolkitCacheEntry(serializer.deserializeFromString(entry.getKey()), entry.getValue());
    }

    @Override
    public void remove() {
      iter.remove();
    }

  }

  private static class ToolkitCacheEntry<K, V> implements Map.Entry<K, V> {
    private final K k;
    private final V v;

    public ToolkitCacheEntry(K k, V v) {
      this.k = k;
      this.v = v;
    }

    @Override
    public K getKey() {
      return k;
    }

    @Override
    public V getValue() {
      return v;
    }

    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }

  }

  private static class ToolkitKeySet<K> implements Set<K> {

    private final Set<String> set;
    private final ToolkitSerializer serializer;

    public ToolkitKeySet(Set<String> set, ToolkitSerializer serializer) {
      this.set = set;
      this.serializer = serializer;
    }

    @Override
    public int size() {
      return set.size();
    }

    @Override
    public boolean isEmpty() {
      return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return set.contains(serializer.serializeToString(o));
    }

    @Override
    public Iterator<K> iterator() {
      return new ToolkitKeyIterator<K>(set.iterator(), serializer);
    }

    @Override
    public Object[] toArray() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(K e) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends K> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }
  }

  private static class ToolkitKeyIterator<K> implements Iterator<K> {

    private final Iterator<String> iter;
    private final ToolkitSerializer serializer;

    public ToolkitKeyIterator(Iterator<String> iter, ToolkitSerializer serializer) {
      this.iter = iter;
      this.serializer = serializer;
    }

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public K next() {
      String k = iter.next();
      if (k == null) { return null; }
      return (K) serializer.deserializeFromString(k);
    }

    @Override
    public void remove() {
      iter.remove();
    }

  }

  @Override
  public Map<K, V> getAllQuiet(Collection<K> keys) {
    HashSet<String> tempSet = new HashSet<String>();
    for (K key : keys) {
      tempSet.add(serializeToString(key));
    }

    Map<String, V> m = toolkitCache.getAllQuiet(tempSet);
    Map<K, V> tempMap = m.isEmpty() ? Collections.EMPTY_MAP : new HashMap<K, V>();

    for (Entry<String, V> entry : m.entrySet()) {
      tempMap.put((K) deserializeFromString(entry.getKey()), entry.getValue());
    }

    return tempMap;
  }

  @Override
  public V getQuiet(Object key) {
    return this.toolkitCache.get(serializeToString(key));
  }

  @Override
  public void putNoReturn(K key, V value, long createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    this.toolkitCache.putNoReturn(serializeToString(key), value, createTimeInSecs, customMaxTTISeconds,
                                  customMaxTTLSeconds);

  }

  @Override
  public V putIfAbsent(K key, V value, long createTimeInSecs, int customMaxTTISeconds, int customMaxTTLSeconds) {
    return this.toolkitCache.putIfAbsent(serializeToString(key), value, createTimeInSecs, customMaxTTISeconds,
                                         customMaxTTLSeconds);
  }

  @Override
  public void addListener(ToolkitCacheListener<K> listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeListener(ToolkitCacheListener<K> listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAttributeExtractor(ToolkitAttributeExtractor attrExtractor) {
    this.toolkitCache.setAttributeExtractor(attrExtractor);
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return this.toolkitCache.createQueryBuilder();
  }

}
