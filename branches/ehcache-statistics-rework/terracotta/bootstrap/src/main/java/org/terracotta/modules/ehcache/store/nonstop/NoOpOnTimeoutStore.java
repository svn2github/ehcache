/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package org.terracotta.modules.ehcache.store.nonstop;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.NullResults;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.writer.CacheWriterManager;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link NonstopStore} which returns null for all get operations and does nothing for puts and
 * removes.
 * 
 * @author Abhishek Sanoujam
 */
public final class NoOpOnTimeoutStore implements TerracottaStore {

  /**
   * the singleton instance
   */
  private static final NoOpOnTimeoutStore INSTANCE = new NoOpOnTimeoutStore();

  /**
   * private constructor
   */
  private NoOpOnTimeoutStore() {
    //
  }

  /**
   * Returns the singleton instance
   * 
   * @return the singleton instance
   */
  public static NoOpOnTimeoutStore getInstance() {
    return INSTANCE;
  }

    /**
   * {@inheritDoc}
   */
  @Override
  public void addStoreListener(StoreListener listener) {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean bufferFull() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKey(Object key) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyInMemory(Object key) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyOffHeap(Object key) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean containsKeyOnDisk(Object key) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Results executeQuery(StoreQuery query) {
    return NullResults.INSTANCE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void expireElements() {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void flush() {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element get(Object key) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Policy getInMemoryEvictionPolicy() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getInMemorySize() {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getInMemorySizeInBytes() {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getInternalContext() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List getKeys() {
    return Collections.EMPTY_LIST;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getMBean() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOffHeapSize() {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getOffHeapSizeInBytes() {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getOnDiskSize() {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getOnDiskSizeInBytes() {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasAbortedSizeOf() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element getQuiet(Object key) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Object, Element> getAllQuiet(Collection<?> keys) {
    Map<Object, Element> rv = new HashMap<Object, Element>();
    for (Object key : keys) {
      rv.put(key, null);
    }
    return rv;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Object, Element> getAll(Collection<?> keys) {
    return getAllQuiet(keys);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getSize() {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Status getStatus() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getTerracottaClusteredSize() {
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isCacheCoherent() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isClusterCoherent() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isNodeCoherent() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean put(Element element) throws CacheException {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void putAll(Collection<Element> elements) throws CacheException {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element putIfAbsent(Element element) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element remove(Object key) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAll(Collection<?> keys) {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAll() throws CacheException {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element removeElement(Element element, ElementValueComparator comparator) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeStoreListener(StoreListener listener) {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean replace(Element old, Element element, ElementValueComparator comparator) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element replace(Element element) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setInMemoryEvictionPolicy(Policy policy) {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void waitUntilClusterCoherent() throws UnsupportedOperationException {
    // no-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Attribute<T> getSearchAttribute(String attributeName) {
    return new Attribute(attributeName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set getLocalKeys() {
    return Collections.EMPTY_SET;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CacheConfiguration.TransactionalMode getTransactionalMode() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Element unlockedGet(Object key) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Element unlockedGetQuiet(Object key) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element unsafeGet(Object key) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Element unsafeGetQuiet(Object key) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recalculateSize(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public WriteBehind createWriteBehind() {
    throw new UnsupportedOperationException();
  }

}
