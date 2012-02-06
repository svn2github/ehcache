/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.bulkops;

import net.sf.ehcache.Element;
import net.sf.ehcache.store.ElementValueComparator;

import org.junit.Assert;
import org.junit.Test;
import org.terracotta.cache.TimestampedValue;
import org.terracotta.meta.MetaData;
import org.terracotta.modules.ehcache.store.backend.BackendStore;
import org.terracotta.modules.ehcache.store.backend.GetAllCustomMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.TestCase;

public class GetAllCustomMapTest extends TestCase {
  GetAllCustomMap    map;
  int                tenK          = 10000;
  int                numOfElements = tenK + new Random().nextInt(tenK);
  Collection<Object> keys;

  private void initKeys() {
    keys = new HashSet<Object>();
    for (int i = 0; i < numOfElements; i++) {
      Object keyString = "key-" + i;
      keys.add(keyString);
    }
  }

  private void initMap() {
    if (map != null) {
      map.clear();
    }
    map = new GetAllCustomMap(keys, new DummyNonStrictBackend(), false, 1000);
  }

  @Test
  public void test() {
    System.err.println("Running BulkGetAllCustomMapTest with numOfElements " + numOfElements);
    initKeys();
    initMap();
    doTestSize();
    doTestContainsKey();
    doTestContainsValue();
    doTestGet();
    doTestPut();
    doTestRemove();
    doTestPutAll();
    doTestKeySet();
    doTestValues();
    doTestEntrySet();
    doTestAllElements();
    doTestClear();
  }

  private void doTestSize() {
    System.err.println("... testing size");
    Assert.assertEquals(numOfElements, map.size());
  }

  private void doTestContainsKey() {
    System.err.println("... testing containsKey");
    for (Object key : keys) {
      Assert.assertTrue("containsKey for " + key, map.containsKey(key));
    }
  }

  private void doTestContainsValue() {
    System.err.println("... testing containsValue");
    for (Object key : keys) {
      try {
        map.containsValue(prepareValue(key));
      } catch (UnsupportedOperationException e) {
        // ignore only UnsupportedOperationException
      }
    }
  }

  private void doTestGet() {
    System.err.println("... testing get");
    for (Object key : keys) {
      Assert.assertEquals(prepareValue(key), map.get(key).getObjectValue());
    }
  }

  private void doTestPut() {
    System.err.println("... testing put");
    for (Object key : keys) {
      map.put(key, prepareElement(key));
    }
    // testing of put for key which is not present the map, put should throw UnsupportedOperationException
    Object key = "key-" + (numOfElements + 10);
    try {
      map.put(key, prepareElement(key));
    } catch (UnsupportedOperationException e) {
      // ignore only UnsupportedOperationException
    }

  }

  private void doTestRemove() {
    System.err.println("... testing remove");
    for (Object key : keys) {
      try {
        map.remove(key);
      } catch (UnsupportedOperationException e) {
        // ignore only UnsupportedOperationException
      }
    }
  }

  private void doTestPutAll() {
    System.err.println("... testing putAll");
    Map<Object, Element> sampleMap = new HashMap<Object, Element>();
    for (Object key : keys) {
      sampleMap.put(key, prepareElement(key));
    }
    map.putAll(sampleMap);

    // testing of putAll for key which is not present the map, put should throw UnsupportedOperationException
    Object key = "key-" + (numOfElements + 10);
    sampleMap = Collections.singletonMap(key, prepareElement(key));
    try {
      map.putAll(sampleMap);
    } catch (UnsupportedOperationException e) {
      // ignore only UnsupportedOperationException
    }

  }

  private void doTestKeySet() {
    System.err.println("... testing keySet");
    Assert.assertEquals(keys, map.keySet());
  }

  private void doTestValues() {
    try {
      map.values();
    } catch (UnsupportedOperationException e) {
      // ignore only UnsupportedOperationException
    }
  }

  private void doTestClear() {
    map.clear();
    Assert.assertEquals(0, map.size());
    Assert.assertEquals(true, map.isEmpty());
    doTestAllElements();
  }

  private void doTestEntrySet() {
    System.err.println("... testing EntrySet");
    Set<Map.Entry<Object, Element>> entrySet = map.entrySet();
    Assert.assertEquals(keys.size(), entrySet.size());
    Assert.assertEquals(false, entrySet.isEmpty());
  }

  private void doTestAllElements() {
    System.err.println("... testing All Elements");
    Iterator<Entry<Object, Element>> itr = map.entrySet().iterator();
    int counter = 0;
    while (itr.hasNext()) {
      ++counter;
      Entry<Object, Element> entry = itr.next();
      Object key = entry.getKey();
      Element actual = entry.getValue();
      Element expected = prepareElement(key);
      Assert.assertEquals(expected, actual);
    }
    Assert.assertEquals(counter, map.entrySet().size());
  }

  private String prepareValue(Object key) {
    return ((String) key).replace("key", "value");
  }

  private Element prepareElement(Object key) {
    Object value = ((String) key).replace("key", "value");
    return new Element(key, value);
  }

  private class DummyNonStrictBackend implements BackendStore {

    public void getAllInternal(Collection<Object> allKeys, boolean quiet, Map<Object, Element> retMap) {
      for (Object key : allKeys) {
        retMap.put(key, prepareElement(key));
      }
    }

    public void putNoReturn(Object portableKey, TimestampedValue value, MetaData searchMetaData) {
      // no-op
    }

    public void putAllNoReturn(Collection<Element> elements) {
      // no-op
    }

    public Element get(Object actualKey, Object portableKey, boolean quiet) {
      return null;
    }

    public Element unlockedGet(Object actualKey, Object portableKey, boolean quiet) {
      return null;
    }

    public Element unsafeGet(Object actualKey, Object portableKey, boolean quiet) {
      return null;
    }

    public Element remove(Object actualKey, Object portableKey, MetaData searchMetaData) {
      return null;
    }

    public void removeAll(Collection<?> keysCollection, Map keyLookupCache) {
      // no-op
    }

    public boolean containsKey(Object portableKey) {
      return false;
    }

    public boolean containsLocalKey(Object portableKey) {
      return false;
    }

    public int getSize() {
      return 0;
    }

    public int getInMemorySize() {
      return 0;
    }

    public int getTerracottaClusteredSize() {
      return 0;
    }

    public void clear(MetaData searchMetaData) {
      // no-op
    }

    public List getKeys() {
      return null;
    }

    public Set getLocalKeys() {
      return null;
    }

    public Element putIfAbsent(Object portableKey, Element element, MetaData searchMetaData) {
      return null;
    }

    public Element removeElement(Object portableKey, Element element, ElementValueComparator comparator,
                                 MetaData searchMetaData) {
      return null;
    }

    public boolean replace(Object portableKey, Element old, Element element, ElementValueComparator comparator,
                           MetaData searchMetaData) {
      return false;
    }

    public Element replace(Object portableKey, Element element, MetaData searchMetaData) {
      return null;
    }

    public Map<Object, Element> getAll(Collection<?> keysCollection, boolean quiet) {
      return null;
    }

    public long getLocalHeapSizeInBytes() {
      return 0;
    }

    public long getOffHeapSizeInBytse() {
      return 0;
    }

    public int getLocalOnHeapSize() {
      return 0;
    }

    public int getLocalOffHeapSize() {
      return 0;
    }

    public boolean containsKeyLocalOnHeap(Object portableKey) {
      return false;
    }

    public boolean containsKeyLocalOffHeap(Object portableKey) {
      return false;
    }

  }

}
