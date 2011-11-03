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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.concurrency.ExplicitLockingContextThreadLocal;
import net.sf.ehcache.constructs.nonstop.concurrency.NonstopCacheLockProvider;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * A {@link NonstopStore} implementation which does not block threads when the cluster goes down.
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopStoreImpl implements NonstopTimeoutBehaviorStoreResolver, RejoinAwareNonstopStore {

    private final NonstopActiveDelegateHolder nonstopActiveDelegateHolder;
    private final NonstopConfiguration nonstopConfig;
    private final ConcurrentMap<TimeoutBehaviorType, NonstopStore> timeoutBehaviors;
    private final ExecutorServiceStore executorServiceStore;
    private final ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal;
    private final CacheLockProvider nonstopCacheLockProvider;

    /**
     * Constructor accepting the {@link NonstopActiveDelegateHolder}, {@link CacheCluster} and {@link NonstopConfiguration}
     *
     */
    public NonstopStoreImpl(NonstopActiveDelegateHolder nonstopActiveDelegateHolder, CacheCluster cacheCluster,
            NonstopConfiguration nonstopConfig, CacheConfiguration.TransactionalMode transactionalMode,
            TransactionManagerLookup transactionManagerLookup) {
        this.nonstopActiveDelegateHolder = nonstopActiveDelegateHolder;
        this.nonstopConfig = nonstopConfig;
        this.explicitLockingContextThreadLocal = new ExplicitLockingContextThreadLocal();
        this.timeoutBehaviors = new ConcurrentHashMap<TimeoutBehaviorType, NonstopStore>();
        if (transactionalMode.equals(CacheConfiguration.TransactionalMode.XA_STRICT)) {
            executorServiceStore = new TransactionalExecutorServiceStore(nonstopActiveDelegateHolder, nonstopConfig, this, cacheCluster,
                    transactionManagerLookup, explicitLockingContextThreadLocal);
        } else {
            executorServiceStore = new ExecutorServiceStore(nonstopActiveDelegateHolder, nonstopConfig, this, cacheCluster,
                    explicitLockingContextThreadLocal);
        }
        this.nonstopCacheLockProvider = new NonstopCacheLockProvider(this, nonstopActiveDelegateHolder, explicitLockingContextThreadLocal);
    }

    /**
     * {@inheritDoc}
     */
    public NonstopStore resolveTimeoutBehaviorStore() {
        final TimeoutBehaviorType timeoutBehaviorType = nonstopConfig.getTimeoutBehavior().getTimeoutBehaviorType();
        NonstopStore timeoutStore = timeoutBehaviors.get(timeoutBehaviorType);
        if (timeoutStore == null) {
            timeoutStore = nonstopConfig.getTimeoutBehavior().getNonstopTimeoutBehaviorFactory()
                    .createNonstopTimeoutBehaviorStore(nonstopActiveDelegateHolder);
            NonstopStore prev = timeoutBehaviors.putIfAbsent(timeoutBehaviorType, timeoutStore);
            if (prev != null) {
                timeoutStore = prev;
            }
        }
        return timeoutStore;
    }

    /**
     * Package protected method - used in tests
     *
     * @return the underlying {@link TerracottaStore}
     */
    TerracottaStore getUnderlyingStore() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore();
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return nonstopCacheLockProvider;
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException
     */
    public void waitUntilClusterCoherent() throws InterruptedException {
        executorServiceStore.waitUntilClusterCoherent();
    }

    // -------------------------------------------------------
    // Methods below delegate directly to the underlying store
    // -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().bufferFull();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().containsKeyOffHeap(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().containsKeyOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getMBean();
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getOffHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getOffHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getOnDiskSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAbortedSizeOf() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().hasAbortedSizeOf();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().expireElements();
    }

    // -------------------------------------------------------
    // All methods below delegate to the clusterAwareStore
    // -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void unpinAll() {
        executorServiceStore.unpinAll();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPinned(Object key) {
        return executorServiceStore.isPinned(key);
    }

    /**
     * {@inheritDoc}
     */
    public void setPinned(Object key, boolean pinned) {
        executorServiceStore.setPinned(key, pinned);
    }

    /**
     * {@inheritDoc}
     */
    public void addStoreListener(StoreListener listener) {
        executorServiceStore.addStoreListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeStoreListener(StoreListener listener) {
        executorServiceStore.removeStoreListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        // this method should also go though to the underlying store(s) to avoid race conditions (DEV-5751)
        nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().setAttributeExtractors(extractors);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCacheCoherent() {
        return executorServiceStore.isCacheCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterCoherent() {
        return executorServiceStore.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeCoherent() {
        return executorServiceStore.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        executorServiceStore.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        executorServiceStore.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return executorServiceStore.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return executorServiceStore.containsKeyInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    public Results executeQuery(StoreQuery query) {
        return executorServiceStore.executeQuery(query);
    }

    /**
     * {@inheritDoc}
     */
    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        return executorServiceStore.getSearchAttribute(attributeName);
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        executorServiceStore.flush();
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        return executorServiceStore.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return executorServiceStore.getInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return executorServiceStore.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() {
        return executorServiceStore.getKeys();
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        return executorServiceStore.getQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Element> getAllQuiet(Collection<?> keys) {
        return executorServiceStore.getAllQuiet(keys);
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Element> getAll(Collection<?> keys) {
        return executorServiceStore.getAll(keys);
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return executorServiceStore.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        return executorServiceStore.getTerracottaClusteredSize();
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) throws CacheException {
        return executorServiceStore.put(element);
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Collection<Element> elements) throws CacheException {
        executorServiceStore.putAll(elements);
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        return executorServiceStore.putIfAbsent(element);
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return executorServiceStore.putWithWriter(element, writerManager);
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        return executorServiceStore.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(final Collection<?> keys) {
        executorServiceStore.removeAll(keys);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        executorServiceStore.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        return executorServiceStore.removeElement(element, comparator);
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return executorServiceStore.removeWithWriter(key, writerManager);
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        return executorServiceStore.replace(old, element, comparator);
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        return executorServiceStore.replace(element);
    }

    /**
     * {@inheritDoc}
     */
    public Set getLocalKeys() {
        return executorServiceStore.getLocalKeys();
    }

    /**
     * {@inheritDoc}
     */
    public CacheConfiguration.TransactionalMode getTransactionalMode() {
      return executorServiceStore.getTransactionalMode();
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGet(Object key) {
        return executorServiceStore.unlockedGet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGetQuiet(Object key) {
        return executorServiceStore.unlockedGetQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGet(Object key) {
        return executorServiceStore.unsafeGet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGetQuiet(Object key) {
        return executorServiceStore.unsafeGetQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public <V> V executeClusterOperation(ClusterOperation<V> operation) {
        return executorServiceStore.executeClusterOperation(operation);
    }

    /**
     * {@inheritDoc}
     */
    public void clusterRejoined() {
        executorServiceStore.clusterRejoined();
    }

    /**
     * {@inheritDoc}
     */
    public void recalculateSize(Object key) {
        throw new UnsupportedOperationException();
    }

}
