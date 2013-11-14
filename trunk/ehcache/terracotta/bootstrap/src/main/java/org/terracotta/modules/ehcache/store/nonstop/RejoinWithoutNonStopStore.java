/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.modules.ehcache.store.nonstop;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.nonstop.RejoinCacheException;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.writer.CacheWriterManager;
import net.sf.ehcache.writer.writebehind.WriteBehind;

/**
 *
 * @author cdennis
 */
public final class RejoinWithoutNonStopStore implements TerracottaStore {

  /**
   * the singleton instance
   */
  private static final RejoinWithoutNonStopStore INSTANCE = new RejoinWithoutNonStopStore();

  /**
   * private constructor
   */
  private RejoinWithoutNonStopStore() {
    //
  }

  /**
   * returns the singleton instance
   */
  public static RejoinWithoutNonStopStore getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Element get(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during get(Object key)");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element getQuiet(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getQuiet(Object key)");
  }

  /**
   * {@inheritDoc}
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Map<Object, Element> getAllQuiet(Collection<?> keys) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getAllQuiet(Collection<?> keys)");
  }

  /**
   * {@inheritDoc}
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Map<Object, Element> getAll(Collection<?> keys) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getAll(Collection<?> keys)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public List getKeys() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getKeys()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean put(Element element) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during put(Element element)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void putAll(Collection<Element> elements) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during putAll(Collection<Element> elements)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Element remove(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during remove(Object key)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void removeAll(Collection<?> keys) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during removeAll(Collection<?> keys)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void removeAll() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during removeAll()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void flush() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during flush()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Object getInternalContext() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getInternalContext()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public int getSize() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getSize()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Element putIfAbsent(Element element) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during putIfAbsent(Element element)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Element replace(Element element) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during replace(Element element)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void addStoreListener(StoreListener listener) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during addStoreListener(StoreListener listener)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean bufferFull() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during bufferFull()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean containsKey(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during containsKey(Object key)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean containsKeyInMemory(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during containsKeyInMemory(Object key)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean containsKeyOffHeap(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during containsKeyOffHeap(Object key)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean containsKeyOnDisk(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during containsKeyOnDisk(Object key)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void dispose() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during dispose()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Results executeQuery(StoreQuery query) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during executeQuery(StoreQuery query)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void expireElements() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during expireElements()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Policy getInMemoryEvictionPolicy() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getInMemoryEvictionPolicy()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public int getInMemorySize() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getInMemorySize()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public long getInMemorySizeInBytes() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getInMemorySizeInBytes()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Object getMBean() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getMBean()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public int getOffHeapSize() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getOffHeapSize()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public long getOffHeapSizeInBytes() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getOffHeapSizeInBytes()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public int getOnDiskSize() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getOnDiskSize()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public long getOnDiskSizeInBytes() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getOnDiskSizeInBytes()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean hasAbortedSizeOf() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during hasAbortedSizeOf()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Status getStatus() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getStatus()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public int getTerracottaClusteredSize() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getTerracottaClusteredSize()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean isCacheCoherent() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during isCacheCoherent()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean isClusterCoherent() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during isClusterCoherent()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean isNodeCoherent() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during isNodeCoherent()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during putWithWriter(Element element, CacheWriterManager writerManager)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Element removeElement(Element element, ElementValueComparator comparator) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during removeElement(Element element, ElementValueComparator comparator)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void removeStoreListener(StoreListener listener) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during removeStoreListener(StoreListener listener)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during removeWithWriter(Object key, CacheWriterManager writerManager)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
          IllegalArgumentException {
    throw new RejoinCacheException("Client started rejoin during replace(Element old, Element element, ElementValueComparator comparator)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during setAttributeExtractors(Map<String, AttributeExtractor> extractors)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void setInMemoryEvictionPolicy(Policy policy) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during setInMemoryEvictionPolicy(Policy policy)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void setNodeCoherent(boolean coherent) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during setNodeCoherent(boolean coherent)");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public void waitUntilClusterCoherent() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during waitUntilClusterCoherent()");
  }

  @Override
  public Set<Attribute> getSearchAttributes() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getSearchAttributes()");
  }

  /**
   * {@inheritDoc}.
   * <p>
   * Throws {@link RejoinCacheException}
   */
  @Override
  public <T> Attribute<T> getSearchAttribute(String attributeName) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getSearchAttribute(String attributeName)");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set getLocalKeys() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getLocalKeys()");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CacheConfiguration.TransactionalMode getTransactionalMode() throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during getTransactionalMode()");
  }

  /**
   * {@inheritDoc}
   */
  public Element unlockedGet(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during unlockedGet(Object key)");
  }

  /**
   * {@inheritDoc}
   */
  public Element unlockedGetQuiet(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during unlockedGetQuiet(Object key)");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Element unsafeGet(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during unsafeGet(Object key)");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void quickClear() {
    throw new RejoinCacheException("Client started rejoin during quickClear()");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int quickSize() {
    throw new RejoinCacheException("Client started rejoin during quickSize()");
  }

  /**
   * {@inheritDoc}
   */
  public Element unsafeGetQuiet(Object key) throws RejoinCacheException {
    throw new RejoinCacheException("Client started rejoin during unsafeGetQuiet(Object key)");
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

  @Override
  public void notifyCacheEventListenersChanged() {
    throw new RejoinCacheException("Client started rejoin during notifyCacheEventListenersChanged()");
  }
}
