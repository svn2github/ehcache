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
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;
import net.sf.ehcache.writer.writebehind.WriteBehind;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LocalReadsAndExceptionOnWritesTimeoutStore
 */
public class LocalReadsAndExceptionOnWritesTimeoutStore implements TerracottaStore {

    private final TerracottaStore reader;
    private final TerracottaStore writer = ExceptionOnTimeoutStore.getInstance();

    /**
     * Constructor accepting the {@link NonstopActiveDelegateHolder}
     */
    public LocalReadsAndExceptionOnWritesTimeoutStore(TerracottaStore delegate) {
        reader = new LocalReadsOnTimeoutStore(delegate);
    }

    public LocalReadsAndExceptionOnWritesTimeoutStore() {
        reader = NoOpOnTimeoutStore.getInstance();
    }

    @Override
    public int getSize() {
        return reader.getSize();
    }

    @Override
    public int getInMemorySize() {
        return reader.getInMemorySize();
    }

    @Override
    public int getOffHeapSize() {
        return reader.getOffHeapSize();
    }

    @Override
    public int getOnDiskSize() {
        return reader.getOnDiskSize();
    }

    @Override
    public int getTerracottaClusteredSize() {
        return reader.getTerracottaClusteredSize();
    }

    @Override
    public long getInMemorySizeInBytes() {
        return reader.getInMemorySizeInBytes();
    }

    @Override
    public long getOffHeapSizeInBytes() {
        return reader.getOffHeapSizeInBytes();
    }

    @Override
    public long getOnDiskSizeInBytes() {
        return reader.getOnDiskSizeInBytes();
    }

    @Override
    public boolean hasAbortedSizeOf() {
        return reader.hasAbortedSizeOf();
    }

    @Override
    public Status getStatus() {
        return reader.getStatus();
    }

    @Override
    public boolean containsKey(Object key) {
        return reader.containsKey(key);
    }

    @Override
    public boolean containsKeyOnDisk(Object key) {
        return reader.containsKeyOnDisk(key);
    }

    @Override
    public boolean containsKeyOffHeap(Object key) {
        return reader.containsKeyOffHeap(key);
    }

    @Override
    public boolean containsKeyInMemory(Object key) {
        return reader.containsKeyInMemory(key);
    }

    @Override
    public Element get(Object key) {
        return reader.get(key);
    }

    @Override
    public Element getQuiet(Object key) {
        return reader.getQuiet(key);
    }

    @Override
    public List getKeys() {
        return reader.getKeys();
    }

    @Override
    public boolean bufferFull() {
        return reader.bufferFull();
    }

    @Override
    public Policy getInMemoryEvictionPolicy() {
        return reader.getInMemoryEvictionPolicy();
    }

    @Override
    public Results executeQuery(StoreQuery query) throws SearchException {
        return reader.executeQuery(query);
    }

    @Override
    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        return reader.getSearchAttribute(attributeName);
    }

    @Override
    public Map<Object, Element> getAllQuiet(Collection<?> keys) {
        return reader.getAllQuiet(keys);
    }

    @Override
    public Map<Object, Element> getAll(Collection<?> keys) {
        return reader.getAll(keys);
    }

    @Override
    public Object getInternalContext() {
        return reader.getInternalContext();
    }

    @Override
    public boolean isCacheCoherent() {
        return reader.isCacheCoherent();
    }

    @Override
    public boolean isClusterCoherent() throws TerracottaNotRunningException {
        return reader.isClusterCoherent();
    }

    @Override
    public boolean isNodeCoherent() throws TerracottaNotRunningException {
        return reader.isNodeCoherent();
    }

    @Override
    public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException, InterruptedException {
        reader.waitUntilClusterCoherent();
    }

    @Override
    public Object getMBean() {
        return reader.getMBean();
    }

    @Override
    public Element unsafeGet(Object key) {
        return reader.unsafeGet(key);
    }

    @Override
    public Set getLocalKeys() {
        return reader.getLocalKeys();
    }

    @Override
    public CacheConfiguration.TransactionalMode getTransactionalMode() {
        return reader.getTransactionalMode();
    }

    @Override
    public boolean put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        return writer.put(element);
    }

    @Override
    public void putAll(Collection<Element> elements) throws CacheException {
        writer.putAll(elements);
    }

    @Override
    public Element remove(Object key) throws IllegalStateException {
        return writer.remove(key);
    }

    @Override
    public void removeAll(Collection<?> keys) throws IllegalStateException {
        writer.removeAll(keys);
    }

    @Override
    public void removeAll() throws IllegalStateException, CacheException {
        writer.removeAll();
    }

    @Override
    public void flush() throws IllegalStateException, CacheException, IOException {
        writer.flush();
    }

    @Override
    public Element putIfAbsent(Element element) throws NullPointerException {
        return writer.putIfAbsent(element);
    }

    @Override
    public Element replace(Element element) throws NullPointerException {
        return writer.replace(element);
    }

    @Override
    public void addStoreListener(StoreListener listener) {
        writer.addStoreListener(listener);
    }

    @Override
    public void dispose() {
        writer.dispose();
    }

    @Override
    public void expireElements() {
        writer.expireElements();
    }

    @Override
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return writer.putWithWriter(element, writerManager);
    }

    @Override
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        return writer.removeElement(element, comparator);
    }

    @Override
    public void removeStoreListener(StoreListener listener) {
        writer.removeStoreListener(listener);
    }

    @Override
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return writer.removeWithWriter(key, writerManager);
    }

    @Override
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
        return writer.replace(old, element, comparator);
    }

    @Override
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        writer.setAttributeExtractors(extractors);
    }

    @Override
    public void setInMemoryEvictionPolicy(Policy policy) {
        writer.setInMemoryEvictionPolicy(policy);
    }

    @Override
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        writer.setNodeCoherent(coherent);
    }

    @Override
    public void recalculateSize(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public WriteBehind createWriteBehind() {
        throw new UnsupportedOperationException();
    }
}
