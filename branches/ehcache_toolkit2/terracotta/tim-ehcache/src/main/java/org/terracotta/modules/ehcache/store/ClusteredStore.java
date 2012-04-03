/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;

import org.terracotta.toolkit.Toolkit;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ClusteredStore implements Store {

  public ClusteredStore(Toolkit toolkit, Ehcache cache, long uniqueId) {
    //
  }

  @Override
  public void unpinAll() {
    //

  }

  @Override
  public boolean isPinned(Object key) {

    return false;
  }

  @Override
  public void setPinned(Object key, boolean pinned) {
    //

  }

  @Override
  public void addStoreListener(StoreListener listener) {

    //
  }

  @Override
  public void removeStoreListener(StoreListener listener) {

    //
  }

  @Override
  public boolean put(Element element) throws CacheException {

    return false;
  }

  @Override
  public void putAll(Collection<Element> elements) throws CacheException {

    //
  }

  @Override
  public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {

    return false;
  }

  @Override
  public Element get(Object key) {

    return null;
  }

  @Override
  public Element getQuiet(Object key) {

    return null;
  }

  @Override
  public List getKeys() {

    return null;
  }

  @Override
  public Element remove(Object key) {

    return null;
  }

  @Override
  public void removeAll(Collection<?> keys) {

    //
  }

  @Override
  public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {

    return null;
  }

  @Override
  public void removeAll() throws CacheException {
    //

  }

  @Override
  public Element putIfAbsent(Element element) throws NullPointerException {

    return null;
  }

  @Override
  public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {

    return null;
  }

  @Override
  public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
      IllegalArgumentException {

    return false;
  }

  @Override
  public Element replace(Element element) throws NullPointerException {

    return null;
  }

  @Override
  public void dispose() {
    //

  }

  @Override
  public int getSize() {

    return 0;
  }

  @Override
  public int getInMemorySize() {

    return 0;
  }

  @Override
  public int getOffHeapSize() {

    return 0;
  }

  @Override
  public int getOnDiskSize() {

    return 0;
  }

  @Override
  public int getTerracottaClusteredSize() {

    return 0;
  }

  @Override
  public long getInMemorySizeInBytes() {

    return 0;
  }

  @Override
  public long getOffHeapSizeInBytes() {

    return 0;
  }

  @Override
  public long getOnDiskSizeInBytes() {

    return 0;
  }

  @Override
  public boolean hasAbortedSizeOf() {

    return false;
  }

  @Override
  public Status getStatus() {

    return null;
  }

  @Override
  public boolean containsKey(Object key) {

    return false;
  }

  @Override
  public boolean containsKeyOnDisk(Object key) {

    return false;
  }

  @Override
  public boolean containsKeyOffHeap(Object key) {

    return false;
  }

  @Override
  public boolean containsKeyInMemory(Object key) {

    return false;
  }

  @Override
  public void expireElements() {
    //

  }

  @Override
  public void flush() throws IOException {
    //

  }

  @Override
  public boolean bufferFull() {

    return false;
  }

  @Override
  public Policy getInMemoryEvictionPolicy() {

    return null;
  }

  @Override
  public void setInMemoryEvictionPolicy(Policy policy) {
    //

  }

  @Override
  public Object getInternalContext() {

    return null;
  }

  @Override
  public boolean isCacheCoherent() {

    return false;
  }

  @Override
  public boolean isClusterCoherent() throws TerracottaNotRunningException {

    return false;
  }

  @Override
  public boolean isNodeCoherent() throws TerracottaNotRunningException {
    //
    return false;
  }

  @Override
  public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException, TerracottaNotRunningException {
    //

  }

  @Override
  public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException,
      InterruptedException {
    //

  }

  @Override
  public Object getMBean() {

    return null;
  }

  @Override
  public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {

    //
  }

  @Override
  public Results executeQuery(StoreQuery query) throws SearchException {

    return null;
  }

  @Override
  public <T> Attribute<T> getSearchAttribute(String attributeName) {

    return null;
  }

  @Override
  public Map<Object, Element> getAllQuiet(Collection<?> keys) {

    return null;
  }

  @Override
  public Map<Object, Element> getAll(Collection<?> keys) {

    return null;
  }

  @Override
  public void recalculateSize(Object key) {

    //
  }

}
