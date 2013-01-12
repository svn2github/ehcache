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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A {@link NonstopStore} implementation that returns the local value in the VM, if present, for get operations and
 * no-op for put, remove and other operations
 * 
 * @author Abhishek Sanoujam
 */
public class LocalReadsOnTimeoutStore implements TerracottaStore {

  private final TerracottaStore delegate;

  /**
   * Constructor accepting the {@link NonstopActiveDelegateHolder}
   */
  public LocalReadsOnTimeoutStore(TerracottaStore delegate) {
    this.delegate = delegate;
  }

    /**
   * {@inheritDoc}.
   * <p>
   * Uses the underlying store to get the local value present in the VM
   */
  @Override
  public Element get(Object key) throws IllegalStateException, CacheException {
    return getQuiet(key);
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Uses the underlying store to get the local value present in the VM
   */
  @Override
  public List getKeys() throws IllegalStateException, CacheException {
    return Collections.unmodifiableList(new ArrayList(getUnderlyingLocalKeys()));
  }

  private Set getUnderlyingLocalKeys() {
    return delegate.getLocalKeys();
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Uses the underlying store to get the local value present in the VM
   */
  @Override
  public Element getQuiet(Object key) throws IllegalStateException, CacheException {
    return delegate.unsafeGet(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<Object, Element> getAllQuiet(Collection<?> keys) {
    Map<Object, Element> rv = new HashMap<Object, Element>();
    for (Object key : keys) {
      rv.put(key, delegate.unsafeGet(key));
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
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
    // no-op
    return false;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void putAll(Collection<Element> elements) throws CacheException {
    // no-op
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public Element remove(Object key) throws IllegalStateException {
    // no-op
    return null;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void removeAll(final Collection<?> keys) throws IllegalStateException {
    // no-op
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void removeAll() throws IllegalStateException, CacheException {
    // no-op
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void flush() throws IllegalStateException, CacheException {
    // no-op
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op and always returns null
   */
  @Override
  public Object getInternalContext() {
    return null;
  }

  /**
   * {@inheritDoc}.
   * <p>
   */
  @Override
  public int getSize() throws IllegalStateException, CacheException {
    return getUnderlyingLocalKeys().size();
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public Element putIfAbsent(Element element) throws NullPointerException {
    return null;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public Element replace(Element element) throws NullPointerException {
    return null;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void addStoreListener(StoreListener listener) {
    // no-op
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean bufferFull() {
    // no-op
    return false;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean containsKey(Object key) {
    return containsKeyInMemory(key);
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean containsKeyInMemory(Object key) {
    return getUnderlyingLocalKeys().contains(key);
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean containsKeyOffHeap(Object key) {
    // no-op
    return false;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean containsKeyOnDisk(Object key) {
    // no-op
    return false;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void dispose() {
    // no-op

  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public Results executeQuery(StoreQuery query) {
    return NullResults.INSTANCE;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void expireElements() {
    // no-op

  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public Policy getInMemoryEvictionPolicy() {
    // no-op
    return null;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public int getInMemorySize() {
    return getUnderlyingLocalKeys().size();
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public long getInMemorySizeInBytes() {
    return delegate.getInMemorySizeInBytes();
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public Object getMBean() {
    // no-op
    return null;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public int getOffHeapSize() {
    // no-op
    return 0;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public long getOffHeapSizeInBytes() {
    // no-op
    return 0;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public int getOnDiskSize() {
    // no-op
    return 0;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public long getOnDiskSizeInBytes() {
    // no-op
    return 0;
  }

  /**
   * {@inheritDoc}
   * <p>
   * This is a no-op
   */
  @Override
  public boolean hasAbortedSizeOf() {
    return false;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public Status getStatus() {
    // no-op
    return null;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public int getTerracottaClusteredSize() {
    return getUnderlyingLocalKeys().size();
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean isCacheCoherent() {
    // no-op
    return false;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean isClusterCoherent() {
    // no-op
    return false;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean isNodeCoherent() {
    // no-op
    return false;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
    // no-op
    return false;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
    // no-op
    return null;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void removeStoreListener(StoreListener listener) {
    // no-op
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
    // no-op
    return null;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
      IllegalArgumentException {
    // no-op
    return false;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
    // no-op
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void setInMemoryEvictionPolicy(Policy policy) {
    // no-op
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
    // no-op
  }

  /**
   * {@inheritDoc}.
   * <p>
   * This is a no-op
   */
  @Override
  public void waitUntilClusterCoherent() throws UnsupportedOperationException {
    // no-op
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public <T> Attribute<T> getSearchAttribute(String attributeName) {
    return new Attribute<T>(attributeName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set getLocalKeys() {
    return getUnderlyingLocalKeys();
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
    return unlockedGetQuiet(key);
  }

  /**
   * {@inheritDoc}
   */
  public Element unlockedGetQuiet(Object key) {
    return delegate.unsafeGet(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element unsafeGet(Object key) {
    return unsafeGetQuiet(key);
  }

  /**
   * {@inheritDoc}
   */
  public Element unsafeGetQuiet(Object key) {
    return delegate.unsafeGet(key);
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
