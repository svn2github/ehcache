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
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.compound.ReadWriteCopyStrategy;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.terracotta.context.annotations.ContextChild;

/**
 * Copies elements, either on read, write or both before using the underlying store to actually store things
 * When copying both ways, the store might not see the same types being stored
 * @param <T> the store type it wraps
 * @author Alex Snaps
 */
public final class CopyingCacheStore<T extends Store> implements Store {

    @ContextChild
    private final T store;
    private final boolean copyOnRead;
    private final boolean copyOnWrite;
    private final ReadWriteCopyStrategy<Element> copyStrategy;

    /**
     * Creates a copying instance of store, that wraps the actual storage
     * @param store the real store
     * @param copyOnRead whether to copy on reads
     * @param copyOnWrite whether to copy on writes
     * @param copyStrategyInstance the copy strategy to use on every copy operation
     */
    public CopyingCacheStore(final T store, final boolean copyOnRead, final boolean copyOnWrite,
                             final ReadWriteCopyStrategy<Element> copyStrategyInstance) {

        this.store = store;
        this.copyOnRead = copyOnRead;
        this.copyOnWrite = copyOnWrite;
        this.copyStrategy = copyStrategyInstance;
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
        return e == null || store.put(copyElementForWriteIfNeeded(e));
    }

    @Override
    public void putAll(final Collection<Element> elements) throws CacheException {
        for (Element element : elements) {
            put(element);
        }
    }

    @Override
    public boolean putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        return store.putWithWriter(copyElementForWriteIfNeeded(element), writerManager);
    }

    @Override
    public Element get(final Object key) {
        return copyElementForReadIfNeeded(store.get(key));
    }

    @Override
    public Element getQuiet(final Object key) {
        return copyElementForReadIfNeeded(store.getQuiet(key));
    }

    @Override
    public List getKeys() {
        return store.getKeys();
    }

    @Override
    public Element remove(final Object key) {
        return copyElementForReadIfNeeded(store.remove(key));
    }

    @Override
    public void removeAll(final Collection<?> keys) {
        for (Object key : keys) {
            remove(key);
        }
    }

    @Override
    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        return copyElementForReadIfNeeded(store.removeWithWriter(key, writerManager));
    }

    @Override
    public void removeAll() throws CacheException {
        store.removeAll();
    }

    @Override
    public Element putIfAbsent(final Element element) throws NullPointerException {
        return copyElementForReadIfNeeded(store.putIfAbsent(copyElementForWriteIfNeeded(element)));
    }

    @Override
    public Element removeElement(final Element element, final ElementValueComparator comparator) throws NullPointerException {
        return copyElementForReadIfNeeded(store.removeElement(copyElementForRemovalIfNeeded(element), comparator));
    }

    @Override
    public boolean replace(final Element old, final Element element,
                           final ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
        return store.replace(copyElementForRemovalIfNeeded(old), copyElementForWriteIfNeeded(element), comparator);
    }

    @Override
    public Element replace(final Element element) throws NullPointerException {
        return store.replace(copyElementForWriteIfNeeded(element));
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
    public <T> Attribute<T> getSearchAttribute(final String attributeName) {
        return store.getSearchAttribute(attributeName);
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
     * Perform copy on read on an element if configured
     *
     * @param element the element to copy for read
     * @return a copy of the element with the reconstructed original value
     */
    protected Element copyElementForReadIfNeeded(Element element) {
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForRead(element);
        } else if (copyOnRead) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element));
        } else {
            return element;
        }
    }

    /**
     * Perform copy on write on an element if configured
     *
     * @param element the element to copy for write
     * @return a copy of the element with a storage-ready value
     */
    protected Element copyElementForWriteIfNeeded(Element element) {
        if (element == null) {
            return null;
        }
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForWrite(element);
        } else if (copyOnWrite) {
            return copyStrategy.copyForRead(copyStrategy.copyForWrite(element));
        } else {
            return element;
        }
    }

    /**
     * Perform copy for the element If both copy on read and copy on write are set to true
     *
     * @param element the element to copy for removal
     * @return a copy of the element with a storage-ready value
     */
    private Element copyElementForRemovalIfNeeded(Element element) {
        if (element == null) {
            return null;
        }
        if (copyOnRead && copyOnWrite) {
            return copyStrategy.copyForWrite(element);
        } else {
            return element;
        }
    }

    /**
     * Wraps the Store instance passed in, should any copy occur
     * @param cacheStore the store
     * @param cacheConfiguration the cache config for that store
     * @return the wrapped Store if copying is required, or the Store instance passed in
     */
    public static Store wrapIfCopy(final Store cacheStore, final CacheConfiguration cacheConfiguration) {
        if (requiresCopy(cacheConfiguration)) {
            return wrap(cacheStore, cacheConfiguration);
        }
        return cacheStore;
    }

    /**
     * Wraps (always) with the proper configured CopyingCacheStore
     * @param cacheStore the store to wrap
     * @param cacheConfiguration the cache config backed by this store
     * @param <T> the Store type
     * @return the wrapped store
     */
    public static <T extends Store> CopyingCacheStore<T> wrap(final T cacheStore, final CacheConfiguration cacheConfiguration) {
        final ReadWriteCopyStrategy<Element> copyStrategyInstance = cacheConfiguration.getCopyStrategyConfiguration()
            .getCopyStrategyInstance();
        return new CopyingCacheStore<T>(cacheStore, cacheConfiguration.isCopyOnRead(), cacheConfiguration.isCopyOnWrite(), copyStrategyInstance);
    }

    /**
     * Checks whether copying and hence wrapping is required
     * @param cacheConfiguration the cache config
     * @return true is copying is required, otherwise false
     */
    public static boolean requiresCopy(final CacheConfiguration cacheConfiguration) {
        return cacheConfiguration.isCopyOnRead() || cacheConfiguration.isCopyOnWrite();
    }
}
