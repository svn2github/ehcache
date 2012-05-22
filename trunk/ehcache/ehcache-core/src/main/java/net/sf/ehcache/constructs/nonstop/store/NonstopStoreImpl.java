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
import net.sf.ehcache.constructs.nonstop.NonstopThread;
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
        this.nonstopCacheLockProvider = new NonstopCacheLockProvider(this, nonstopActiveDelegateHolder, explicitLockingContextThreadLocal,
                nonstopConfig);
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

    private boolean isCurrentThreadNonstopThread() {
        return NonstopThread.isCurrentThreadNonstopThread();
    }

    // -------------------------------------------------------
    // All methods below delegate to the clusterAwareStore
    // -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void unpinAll() {
        if (isCurrentThreadNonstopThread()) {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unpinAll();
        } else {
            executorServiceStore.unpinAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPinned(Object key) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().isPinned(key);
        } else {
            return executorServiceStore.isPinned(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setPinned(Object key, boolean pinned) {
        if (isCurrentThreadNonstopThread()) {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().setPinned(key, pinned);
        } else {
            executorServiceStore.setPinned(key, pinned);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addStoreListener(StoreListener listener) {
        if (isCurrentThreadNonstopThread()) {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().addStoreListener(listener);
        } else {
            executorServiceStore.addStoreListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeStoreListener(StoreListener listener) {
        if (isCurrentThreadNonstopThread()) {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().removeStoreListener(listener);
        } else {
            executorServiceStore.removeStoreListener(listener);
        }
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
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().isCacheCoherent();
        } else {
            return executorServiceStore.isCacheCoherent();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterCoherent() {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().isClusterCoherent();
        } else {
            return executorServiceStore.isClusterCoherent();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeCoherent() {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().isNodeCoherent();
        } else {
            return executorServiceStore.isNodeCoherent();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        if (isCurrentThreadNonstopThread()) {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().setNodeCoherent(coherent);
        } else {
            executorServiceStore.setNodeCoherent(coherent);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        if (isCurrentThreadNonstopThread()) {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().dispose();
        } else {
            executorServiceStore.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().containsKey(key);
        } else {
            return executorServiceStore.containsKey(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().containsKeyInMemory(key);
        } else {
            return executorServiceStore.containsKeyInMemory(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Results executeQuery(StoreQuery query) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().executeQuery(query);
        } else {
            return executorServiceStore.executeQuery(query);
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getSearchAttribute(attributeName);
        } else {
            return executorServiceStore.getSearchAttribute(attributeName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        if (isCurrentThreadNonstopThread()) {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().flush();
        } else {
            executorServiceStore.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().get(key);
        } else {
            return executorServiceStore.get(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getInMemorySize();
        } else {
            return executorServiceStore.getInMemorySize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getInMemorySizeInBytes();
        } else {
            return executorServiceStore.getInMemorySizeInBytes();
        }
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getKeys();
        } else {
            return executorServiceStore.getKeys();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getQuiet(key);
        } else {
            return executorServiceStore.getQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Element> getAllQuiet(Collection<?> keys) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getAllQuiet(keys);
        } else {
            return executorServiceStore.getAllQuiet(keys);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Element> getAll(Collection<?> keys) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getAll(keys);
        } else {
            return executorServiceStore.getAll(keys);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getSize();
        } else {
            return executorServiceStore.getSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getTerracottaClusteredSize();
        } else {
            return executorServiceStore.getTerracottaClusteredSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) throws CacheException {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().put(element);
        } else {
            return executorServiceStore.put(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Collection<Element> elements) throws CacheException {
        if (isCurrentThreadNonstopThread()) {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().putAll(elements);
        } else {
            executorServiceStore.putAll(elements);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().putIfAbsent(element);
        } else {
            return executorServiceStore.putIfAbsent(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().putWithWriter(element, writerManager);
        } else {
            return executorServiceStore.putWithWriter(element, writerManager);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().remove(key);
        } else {
            return executorServiceStore.remove(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(final Collection<?> keys) {
        if (isCurrentThreadNonstopThread()) {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().removeAll(keys);
        } else {
            executorServiceStore.removeAll(keys);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        if (isCurrentThreadNonstopThread()) {
            nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().removeAll();
        } else {
            executorServiceStore.removeAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().removeElement(element, comparator);
        } else {
            return executorServiceStore.removeElement(element, comparator);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().removeWithWriter(key, writerManager);
        } else {
            return executorServiceStore.removeWithWriter(key, writerManager);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().replace(old, element, comparator);
        } else {
            return executorServiceStore.replace(old, element, comparator);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().replace(element);
        } else {
            return executorServiceStore.replace(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set getLocalKeys() {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getLocalKeys();
        } else {
            return executorServiceStore.getLocalKeys();
        }
    }

    /**
     * {@inheritDoc}
     */
    public CacheConfiguration.TransactionalMode getTransactionalMode() {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getTransactionalMode();
        } else {
            return executorServiceStore.getTransactionalMode();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGet(Object key) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unlockedGet(key);
        } else {
            return executorServiceStore.unlockedGet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGetQuiet(Object key) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unlockedGetQuiet(key);
        } else {
            return executorServiceStore.unlockedGetQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGet(Object key) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unsafeGet(key);
        } else {
            return executorServiceStore.unsafeGet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGetQuiet(Object key) {
        if (isCurrentThreadNonstopThread()) {
            return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unsafeGetQuiet(key);
        } else {
            return executorServiceStore.unsafeGetQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public <V> V executeClusterOperation(ClusterOperation<V> operation) {
        if (isCurrentThreadNonstopThread()) {
            try {
                return operation.performClusterOperation();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return executorServiceStore.executeClusterOperation(operation);
        }
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
