/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.backend;

import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GetAllCustomMap extends AbstractMap<Object, Element> {
  private static final Logger          LOGGER = LoggerFactory.getLogger(GetAllCustomMap.class.getName());
  private final Collection<?>          keys;
  private final Map<Object, Element>[] internalMaps;
  private final BackendStore           backendStore;
  private final boolean[]              fetchCompleted;
  private final boolean                quiet;
  private final int                    getAllBatchSize;

  public GetAllCustomMap(Collection<?> keys, BackendStore backendStore, boolean quiet, int getAllBatchSize) {
    this.keys = keys;
    this.internalMaps = new Map[(int) Math.ceil((double) keys.size() / (double) getAllBatchSize)];
    this.fetchCompleted = new boolean[internalMaps.length];
    this.backendStore = backendStore;
    this.quiet = quiet;
    this.getAllBatchSize = getAllBatchSize;
    initMaps();
    fetchValuesForIndex(0);
  }

  private void initMaps() {
    for (int index = 0; index < internalMaps.length; ++index) {
      internalMaps[index] = new HashMap<Object, Element>();
    }
    int index = 0;
    int counter = 0;
    // initialize all internal maps with <key,null>
    for (Object key : keys) {
      if (counter == getAllBatchSize) {
        ++index;
        counter = 0;
      }
      internalMaps[index].put(key, null);
      ++counter;
    }
  }

  @Override
  public boolean containsKey(Object key) {
    return keys.contains(key);
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException("containsValue is not supported on this map implementation");
  }

  @Override
  public Element get(Object key) {
    int index = getMapIndexForKey(key);
    if (index == -1) { return null; }
    if (fetchCompleted[index]) { return internalMaps[index].get(key); }
    fetchValuesForIndex(index);
    return internalMaps[index].get(key);
  }

  private void fetchValuesForIndex(int index) {
    if (!fetchCompleted[index]) {
      backendStore.getAllInternal(internalMaps[index].keySet(), quiet, internalMaps[index]);
      fetchCompleted[index] = true;
    }
  }

  private int getMapIndexForKey(Object key) {
    for (int index = 0; index < internalMaps.length; ++index) {
      if (internalMaps[index].containsKey(key)) { return index; }
    }
    return -1;
  }

  @Override
  public Element put(Object key, Element value) {
    if (!containsKey(key)) { throw new UnsupportedOperationException("putInternal is supported only for existing keys"); }
    Map<Object, Element> mapForKey = internalMaps[getMapIndexForKey(key)];
    return mapForKey.put(key, value);
  }

  @Override
  public Element remove(Object key) {
    throw new UnsupportedOperationException("remove is not supported on this map implementation");
  }

  @Override
  public void putAll(Map<? extends Object, ? extends Element> m) {
    for (Map.Entry<? extends Object, ? extends Element> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public Set<Object> keySet() {
    Set<Object> keySet = new HashSet<Object>();
    for (Map<Object, Element> internalMap : internalMaps) {
      keySet.addAll(internalMap.keySet());
    }
    return keySet;
  }

  @Override
  public Collection<Element> values() {
    throw new UnsupportedOperationException("values is not supported on this map implementation");
  }

  @Override
  public Set<Map.Entry<Object, Element>> entrySet() {
    return new EntrySet();
  }

  private final class EntrySet extends AbstractSet<Map.Entry<Object, Element>> {

    @Override
    public Iterator<java.util.Map.Entry<Object, Element>> iterator() {
      return new EntryIterator();
    }

    @Override
    public int size() {
      return keys.size();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException("removeAll is not supported on this Set implementation");
    }

    @Override
    public boolean remove(Object o) {
      throw new UnsupportedOperationException("remove is not supported on this Set implementation");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException("retainAll is not supported on this Set implementation");
    }

    @Override
    public void clear() {
      keys.clear();
      for (int index = 0; index < internalMaps.length; ++index) {
        internalMaps[index] = new HashMap<Object, Element>();
        LOGGER.info("Map " + index + " cleared...");
      }
    }
  }

  private final class EntryIterator implements Iterator<Entry<Object, Element>> {
    Iterator<Entry<Object, Element>> currentIterator;
    int                              index   = 0;
    Map<Object, Element>[]           intMaps = GetAllCustomMap.this.internalMaps;
    boolean[]                        fetched = GetAllCustomMap.this.fetchCompleted;

    EntryIterator() {
      advance();
    }

    public boolean hasNext() {
      return currentIterator.hasNext();
    }

    public Map.Entry<Object, Element> next() {
      Map.Entry<Object, Element> entry = currentIterator.next();
      advance();
      return entry;
    }

    // this method essentially advance iterator from internal maps if needed
    private final void advance() {
      if (currentIterator == null) {
        currentIterator = intMaps[index].entrySet().iterator();
      } else {
        if (!currentIterator.hasNext()) {
          // we want advance to next iterator only when this iterator is exhausted
          if (index < intMaps.length - 1) {
            // we can advance to next iterator only if currentIterator is not the last iterator
            ++index;
            if (!fetched[index]) {
              GetAllCustomMap.this.fetchValuesForIndex(index);
            }
            currentIterator = intMaps[index].entrySet().iterator();
          } else {
            // we can not advance to next iterator because this iterator is the last iterator
          }
        } else {
          // we do not want to advance to next iterator because this iterator is not fully exhausted
        }
      }
    }

    public void remove() {
      throw new UnsupportedOperationException("remove not supported");
    }

  }

}
