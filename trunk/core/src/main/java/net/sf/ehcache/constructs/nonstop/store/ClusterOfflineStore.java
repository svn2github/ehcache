/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.constructs.nonstop.store;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * Implementation of {@link TerracottaStore} which should be used with nonstop and when cluster is offline.
 *
 * @author Abhishek Sanoujam
 *
 */
public class ClusterOfflineStore implements TerracottaStore {

    private final NonstopConfiguration nonstopConfig;
    private final NonstopTimeoutStoreResolver nonstopStoreResolver;
    private final ExecutorServiceStore executorBehavior;
    private final AtomicInteger pendingMutateBufferSize = new AtomicInteger();

    /**
     * Constructor accepting the {@link NonstopConfiguration}, a {@link NonstopTimeoutStoreResolver} and {@link ExecutorServiceStore} which
     * will be invoked if immediateTimeout is false
     *
     * For every operation in {@link ExecutorServiceStore}, if the {@link NonstopConfiguration} is configured with immediateTimeout, the
     * behavior resolved from the {@link NonstopTimeoutStoreResolver} is invoked. Otherwise the executor service
     * is invoked
     *
     * @param nonstopConfig
     * @param nonstopStoreResolver
     * @param executorServiceStore
     */
    public ClusterOfflineStore(final NonstopConfiguration nonstopConfig, final NonstopTimeoutStoreResolver nonstopStoreResolver,
            final ExecutorServiceStore executorServiceStore) {
        this.nonstopConfig = nonstopConfig;
        this.nonstopStoreResolver = nonstopStoreResolver;
        this.executorBehavior = executorServiceStore;
    }

    private boolean shouldTimeoutImmediately() {
        return nonstopConfig.isImmediateTimeout();
    }

    /**
     * {@inheritDoc}
     */
    public void addStoreListener(StoreListener listener) {
        if (shouldTimeoutImmediately()) {
            nonstopStoreResolver.resolveTimeoutStore().addStoreListener(listener);
        } else {
            executorBehavior.addStoreListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().bufferFull();
        } else {
            return executorBehavior.bufferFull();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().containsKey(key);
        } else {
            return executorBehavior.containsKey(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().containsKeyInMemory(key);
        } else {
            return executorBehavior.containsKeyInMemory(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().containsKeyOffHeap(key);
        } else {
            return executorBehavior.containsKeyOffHeap(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().containsKeyOnDisk(key);
        } else {
            return executorBehavior.containsKeyOnDisk(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        if (shouldTimeoutImmediately()) {
            nonstopStoreResolver.resolveTimeoutStore().dispose();
        } else {
            executorBehavior.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Results executeQuery(StoreQuery query) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().executeQuery(query);
        } else {
            return executorBehavior.executeQuery(query);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        if (shouldTimeoutImmediately()) {
            nonstopStoreResolver.resolveTimeoutStore().expireElements();
        } else {
            executorBehavior.expireElements();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        if (shouldTimeoutImmediately()) {
            nonstopStoreResolver.resolveTimeoutStore().flush();
        } else {
            executorBehavior.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().get(key);
        } else {
            return executorBehavior.get(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getInMemoryEvictionPolicy();
        } else {
            return executorBehavior.getInMemoryEvictionPolicy();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getInMemorySize();
        } else {
            return executorBehavior.getInMemorySize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getInMemorySizeInBytes();
        } else {
            return executorBehavior.getInMemorySizeInBytes();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getInternalContext();
        } else {
            return executorBehavior.getInternalContext();
        }
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getKeys();
        } else {
            return executorBehavior.getKeys();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getMBean();
        } else {
            return executorBehavior.getMBean();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getOffHeapSize();
        } else {
            return executorBehavior.getOffHeapSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getOffHeapSizeInBytes();
        } else {
            return executorBehavior.getOffHeapSizeInBytes();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getOnDiskSize();
        } else {
            return executorBehavior.getOnDiskSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getOnDiskSizeInBytes();
        } else {
            return executorBehavior.getOnDiskSizeInBytes();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getQuiet(key);
        } else {
            return executorBehavior.getQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getSize();
        } else {
            return executorBehavior.getSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getStatus();
        } else {
            return executorBehavior.getStatus();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getTerracottaClusteredSize();
        } else {
            return executorBehavior.getTerracottaClusteredSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCacheCoherent() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().isCacheCoherent();
        } else {
            return executorBehavior.isCacheCoherent();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterCoherent() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().isClusterCoherent();
        } else {
            return executorBehavior.isClusterCoherent();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeCoherent() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().isNodeCoherent();
        } else {
            return executorBehavior.isNodeCoherent();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) throws CacheException {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().put(element);
        } else {
            return executorBehavior.put(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().putIfAbsent(element);
        } else {
            return executorBehavior.putIfAbsent(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().putWithWriter(element, writerManager);
        } else {
            return executorBehavior.putWithWriter(element, writerManager);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().remove(key);
        } else {
            return executorBehavior.remove(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        if (shouldTimeoutImmediately()) {
            nonstopStoreResolver.resolveTimeoutStore().removeAll();
        } else {
            executorBehavior.removeAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().removeElement(element, comparator);
        } else {
            return executorBehavior.removeElement(element, comparator);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeStoreListener(StoreListener listener) {
        if (shouldTimeoutImmediately()) {
            nonstopStoreResolver.resolveTimeoutStore().removeStoreListener(listener);
        } else {
            executorBehavior.removeStoreListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().removeWithWriter(key, writerManager);
        } else {
            return executorBehavior.removeWithWriter(key, writerManager);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().replace(old, element, comparator);
        } else {
            return executorBehavior.replace(old, element, comparator);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().replace(element);
        } else {
            return executorBehavior.replace(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        if (shouldTimeoutImmediately()) {
            nonstopStoreResolver.resolveTimeoutStore().setAttributeExtractors(extractors);
        } else {
            executorBehavior.setAttributeExtractors(extractors);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        if (shouldTimeoutImmediately()) {
            nonstopStoreResolver.resolveTimeoutStore().setInMemoryEvictionPolicy(policy);
        } else {
            executorBehavior.setInMemoryEvictionPolicy(policy);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        if (shouldTimeoutImmediately()) {
            nonstopStoreResolver.resolveTimeoutStore().setNodeCoherent(coherent);
        } else {
            executorBehavior.setNodeCoherent(coherent);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        if (shouldTimeoutImmediately()) {
            nonstopStoreResolver.resolveTimeoutStore().waitUntilClusterCoherent();
        } else {
            executorBehavior.waitUntilClusterCoherent();
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getSearchAttribute(attributeName);
        } else {
            return executorBehavior.getSearchAttribute(attributeName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set getLocalKeys() {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().getLocalKeys();
        } else {
            return executorBehavior.getLocalKeys();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGet(final Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().unlockedGet(key);
        } else {
            return executorBehavior.unlockedGet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGetQuiet(Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().unlockedGetQuiet(key);
        } else {
            return executorBehavior.unlockedGetQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGet(Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().unsafeGet(key);
        } else {
            return executorBehavior.unsafeGet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGetQuiet(Object key) {
        if (shouldTimeoutImmediately()) {
            return nonstopStoreResolver.resolveTimeoutStore().unsafeGetQuiet(key);
        } else {
            return executorBehavior.unsafeGetQuiet(key);
        }
    }
}
