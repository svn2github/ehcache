/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;

import org.terracotta.context.annotations.ContextChild;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copies elements, either on read, write or both before using the underlying store to actually store things
 * When copying both ways, the store might not see the same types being stored
 * @param <T> the store type it wraps
 *
 * @author Alex Snaps
 */
abstract class AbstractCopyingCacheStore<T extends Store> implements Store {

    @ContextChild
    private final T store;
    private final CopyStrategyHandler copyStrategyHandler;

    /**
     * Creates a copying instance of store, that wraps the actual storage
     * @param store the real store
     * @param copyOnRead whether to copy on reads
     * @param copyOnWrite whether to copy on writes
     * @param copyStrategyInstance the copy strategy to use on every copy operation
     */
    public AbstractCopyingCacheStore(final T store, final boolean copyOnRead, final boolean copyOnWrite,
                                     final ReadWriteCopyStrategy<Element> copyStrategyInstance) {

        this.store = store;
        copyStrategyHandler = new CopyStrategyHandler(copyOnRead, copyOnWrite, copyStrategyInstance);
    }

    @Override
    public void addStoreListener(final StoreListener listener) {
        store.addStoreListener(listener);
    }

    @Override
    public void removeStoreListener(final StoreListener listener) {
        store.removeStoreListener(listener);
    }

    @Override
    public boolean put(final Element e) throws CacheException {
        return e == null || store.put(copyStrategyHandler.copyElementForWriteIfNeeded(e));
    }

    @Override
    public void putAll(final Collection<Element> elements) throws CacheException {
        for (Element element : elements) {
            put(element);
        }
    }

    @Override
    public boolean putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        return store.putWithWriter(copyStrategyHandler.copyElementForWriteIfNeeded(element), writerManager);
    }

    @Override
    public Element get(final Object key) {
        return copyStrategyHandler.copyElementForReadIfNeeded(store.get(key));
    }

    @Override
    public Element getQuiet(final Object key) {
        return copyStrategyHandler.copyElementForReadIfNeeded(store.getQuiet(key));
    }

    @Override
    public List getKeys() {
        return store.getKeys();
    }

    @Override
    public Element remove(final Object key) {
        return copyStrategyHandler.copyElementForReadIfNeeded(store.remove(key));
    }

    @Override
    public void removeAll(final Collection<?> keys) {
        for (Object key : keys) {
            remove(key);
        }
    }

    @Override
    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        return copyStrategyHandler.copyElementForReadIfNeeded(store.removeWithWriter(key, writerManager));
    }

    @Override
    public void removeAll() throws CacheException {
        store.removeAll();
    }

    @Override
    public Element putIfAbsent(final Element element) throws NullPointerException {
        return copyStrategyHandler.copyElementForReadIfNeeded(store.putIfAbsent(copyStrategyHandler.copyElementForWriteIfNeeded(element)));
    }

    @Override
    public Element removeElement(final Element element, final ElementValueComparator comparator) throws NullPointerException {
        Element removed = store.removeElement(copyStrategyHandler.copyElementForRemovalIfNeeded(element), comparator);
        return copyStrategyHandler.copyElementForReadIfNeeded(removed);
    }

    @Override
    public boolean replace(final Element old, final Element element,
                           final ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
        Element oldElement = copyStrategyHandler.copyElementForRemovalIfNeeded(old);
        Element newElement = copyStrategyHandler.copyElementForWriteIfNeeded(element);
        return store.replace(oldElement, newElement, comparator);
    }

    @Override
    public Element replace(final Element element) throws NullPointerException {
        return copyStrategyHandler.copyElementForReadIfNeeded(store.replace(copyStrategyHandler.copyElementForWriteIfNeeded(element)));
    }

    @Override
    public void dispose() {
        store.dispose();
    }

    @Override
    public int getSize() {
        return store.getSize();
    }

    @Override
    public int getInMemorySize() {
        return store.getInMemorySize();
    }

    @Override
    public int getOffHeapSize() {
        return store.getOffHeapSize();
    }

    @Override
    public int getOnDiskSize() {
        return store.getOnDiskSize();
    }

    @Override
    public int getTerracottaClusteredSize() {
        return store.getTerracottaClusteredSize();
    }

    @Override
    public long getInMemorySizeInBytes() {
        return store.getInMemorySizeInBytes();
    }

    @Override
    public long getOffHeapSizeInBytes() {
        return store.getOffHeapSizeInBytes();
    }

    @Override
    public long getOnDiskSizeInBytes() {
        return store.getOnDiskSizeInBytes();
    }

    @Override
    public boolean hasAbortedSizeOf() {
        return store.hasAbortedSizeOf();
    }

    @Override
    public Status getStatus() {
        return store.getStatus();
    }

    @Override
    public boolean containsKey(final Object key) {
        return store.containsKey(key);
    }

    @Override
    public boolean containsKeyOnDisk(final Object key) {
        return store.containsKeyOnDisk(key);
    }

    @Override
    public boolean containsKeyOffHeap(final Object key) {
        return store.containsKeyOffHeap(key);
    }

    @Override
    public boolean containsKeyInMemory(final Object key) {
        return store.containsKeyInMemory(key);
    }

    @Override
    public void expireElements() {
        store.expireElements();
    }

    @Override
    public void flush() throws IOException {
        store.flush();
    }

    @Override
    public boolean bufferFull() {
        return store.bufferFull();
    }

    @Override
    public Policy getInMemoryEvictionPolicy() {
        return store.getInMemoryEvictionPolicy();
    }

    @Override
    public void setInMemoryEvictionPolicy(final Policy policy) {
        store.setInMemoryEvictionPolicy(policy);
    }

    @Override
    public Object getInternalContext() {
        return store.getInternalContext();
    }

    @Override
    public boolean isCacheCoherent() {
        return store.isCacheCoherent();
    }

    @Override
    public boolean isClusterCoherent() throws TerracottaNotRunningException {
        return store.isClusterCoherent();
    }

    @Override
    public boolean isNodeCoherent() throws TerracottaNotRunningException {
        return store.isNodeCoherent();
    }

    @Override
    public void setNodeCoherent(final boolean coherent) throws UnsupportedOperationException, TerracottaNotRunningException {
        store.setNodeCoherent(coherent);
    }

    @Override
    public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException, InterruptedException {
        store.waitUntilClusterCoherent();
    }

    @Override
    public Object getMBean() {
        return store.getMBean();
    }

    @Override
    public void setAttributeExtractors(final Map<String, AttributeExtractor> extractors) {
        store.setAttributeExtractors(extractors);
    }

    @Override
    public Results executeQuery(final StoreQuery query) throws SearchException {
        return store.executeQuery(query);
    }

    @Override
    public <S> Attribute<S> getSearchAttribute(final String attributeName) {
        return store.getSearchAttribute(attributeName);
    }

    @Override
    public Set<Attribute> getSearchAttributes() {
        return store.getSearchAttributes();
    }

    @Override
    public Map<Object, Element> getAllQuiet(final Collection<?> keys) {
        Map<Object, Element> elements = new HashMap<Object, Element>();
        for (Object key : keys) {
            elements.put(key, getQuiet(key));
        }
        return elements;
    }

    @Override
    public Map<Object, Element> getAll(final Collection<?> keys) {
        Map<Object, Element> elements = new HashMap<Object, Element>();
        for (Object key : keys) {
            elements.put(key, get(key));
        }
        return elements;
    }

    @Override
    public void recalculateSize(final Object key) {
        store.recalculateSize(key);
    }

    /**
     * Accessor to the underlying store
     * @return the underlying store
     */
    public T getUnderlyingStore() {
        return store;
    }

    /**
     * Accessor to the {@link CopyStrategyHandler}
     *
     * @return the copy strategy handler
     */
    protected CopyStrategyHandler getCopyStrategyHandler() {
        return copyStrategyHandler;
    }
}
