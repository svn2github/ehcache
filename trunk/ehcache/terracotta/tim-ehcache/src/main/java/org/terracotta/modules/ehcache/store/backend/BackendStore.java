/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store.backend;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementValueComparator;

import org.terracotta.cache.TimestampedValue;
import org.terracotta.meta.MetaData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Abhishek Sanoujam
 */
public interface BackendStore {

  public void putNoReturn(Object portableKey, TimestampedValue value, MetaData searchMetaData);

  public void putAllNoReturn(Collection<Element> elements);

  public Element get(Object actualKey, Object portableKey, final boolean quiet);

  public Element unlockedGet(Object actualKey, Object portableKey, final boolean quiet);

  public Element unsafeGet(Object actualKey, Object portableKey, final boolean quiet);

  public Element remove(Object actualKey, Object portableKey, MetaData searchMetaData);

  public void removeAll(Collection<?> keys, Map keyLookupCache);

  public boolean containsKey(Object portableKey);

  public boolean containsLocalKey(Object portableKey);

  public int getSize();

  public int getInMemorySize();

  public int getTerracottaClusteredSize();

  public long getLocalHeapSizeInBytes();

  public long getOffHeapSizeInBytse();

  public int getLocalOnHeapSize();

  public int getLocalOffHeapSize();

  public void clear(MetaData searchMetaData);

  public List getKeys();

  public Set getLocalKeys();

  public Element putIfAbsent(Object portableKey, Element element, MetaData searchMetaData);

  public Element removeElement(Object portableKey, Element element, ElementValueComparator comparator,
                               MetaData searchMetaData);

  public boolean replace(Object portableKey, Element old, Element element, ElementValueComparator comparator,
                         MetaData searchMetaData);

  public Element replace(Object portableKey, Element element, MetaData searchMetaData);

  public Map<Object, Element> getAll(Collection<?> keys, boolean quiet);

  public void getAllInternal(Collection<Object> keys, boolean quiet, Map<Object, Element> rv);

  public boolean containsKeyLocalOnHeap(Object portableKey);

  public boolean containsKeyLocalOffHeap(Object portableKey);

}
