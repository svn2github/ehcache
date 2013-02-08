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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.ElementIdHelper;
import net.sf.ehcache.Status;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.util.LongSequence;
import net.sf.ehcache.writer.CacheWriterManager;
import org.terracotta.context.annotations.ContextChild;

/**
 * Store wrapper that assigns sequential IDs to elements as they are added to the underlying store
 *
 * @author teck
 */
public class ElementIdAssigningStore implements Store {

    @ContextChild
    private final Store delegate;
    private final LongSequence elementIdSequence;

    /**
     * Constructor
     *
     * @param delegate underlying Store
     * @param sequence id sequence
     */
    public ElementIdAssigningStore(Store delegate, LongSequence sequence) {
        this.delegate = delegate;
        this.elementIdSequence = sequence;
    }

    private void setId(Element element) {
        long id = elementIdSequence.next();
        if (id <= 0) {
            throw new CacheException("Element ID must be > 0");
        }

        ElementIdHelper.setId(element, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addStoreListener(StoreListener listener) {
        delegate.addStoreListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeStoreListener(StoreListener listener) {
        delegate.removeStoreListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean put(Element element) throws CacheException {
        setId(element);
        return delegate.put(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Collection<Element> elements) throws CacheException {
        for (Element e : elements) {
            setId(e);
        }
        delegate.putAll(elements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        setId(element);
        return delegate.putWithWriter(element, writerManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element get(Object key) {
        return delegate.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getQuiet(Object key) {
        return delegate.getQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List getKeys() {
        return delegate.getKeys();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element remove(Object key) {
        return delegate.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll(Collection<?> keys) {
        delegate.removeAll(keys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return delegate.removeWithWriter(key, writerManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() throws CacheException {
        delegate.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element putIfAbsent(Element element) throws NullPointerException {
        setId(element);
        return delegate.putIfAbsent(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        return delegate.removeElement(element, comparator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        setId(element);
        return delegate.replace(old, element, comparator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element replace(Element element) throws NullPointerException {
        setId(element);
        return delegate.replace(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        delegate.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() {
        return delegate.getSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInMemorySize() {
        return delegate.getInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOffHeapSize() {
        return delegate.getOffHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOnDiskSize() {
        return delegate.getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTerracottaClusteredSize() {
        return delegate.getTerracottaClusteredSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getInMemorySizeInBytes() {
        return delegate.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getOffHeapSizeInBytes() {
        return delegate.getOffHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getOnDiskSizeInBytes() {
        return delegate.getOnDiskSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasAbortedSizeOf() {
        return delegate.hasAbortedSizeOf();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status getStatus() {
        return delegate.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKeyOnDisk(Object key) {
        return delegate.containsKeyOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKeyOffHeap(Object key) {
        return delegate.containsKeyOffHeap(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKeyInMemory(Object key) {
        return delegate.containsKeyInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void expireElements() {
        delegate.expireElements();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean bufferFull() {
        return delegate.bufferFull();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Policy getInMemoryEvictionPolicy() {
        return delegate.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setInMemoryEvictionPolicy(Policy policy) {
        delegate.setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getInternalContext() {
        return delegate.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCacheCoherent() {
        return delegate.isCacheCoherent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClusterCoherent() throws TerracottaNotRunningException {
        return delegate.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNodeCoherent() throws TerracottaNotRunningException {
        return delegate.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException, TerracottaNotRunningException {
        delegate.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException, InterruptedException {
        delegate.waitUntilClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getMBean() {
        return delegate.getMBean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        delegate.setAttributeExtractors(extractors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Results executeQuery(StoreQuery query) throws SearchException {
        return delegate.executeQuery(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        return delegate.getSearchAttribute(attributeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Object, Element> getAllQuiet(Collection<?> keys) {
        return delegate.getAllQuiet(keys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Object, Element> getAll(Collection<?> keys) {
        return delegate.getAll(keys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recalculateSize(Object key) {
        delegate.recalculateSize(key);
    }

}
