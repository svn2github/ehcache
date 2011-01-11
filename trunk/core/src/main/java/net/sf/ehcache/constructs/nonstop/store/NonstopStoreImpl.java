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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.NonstopExecutorService;
import net.sf.ehcache.constructs.nonstop.concurrency.NonstopSync;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * A nonstop {@link Store} implementation which does not block threads when the cluster goes down.
 *
 * @author Abhishek Sanoujam
 *
 */
public class NonstopStoreImpl implements NonstopTimeoutStoreResolver, NonstopStore {

    private final Store underlyingStore;
    private final NonstopConfiguration nonstopConfig;
    private final ConcurrentMap<NonstopTimeoutBehaviorType, NonstopStore> timeoutBehaviors;
    private final ClusterAwareStore clusterAwareStore;
    private final CacheLockProvider underlyingCacheLockProvider;

    /**
     * Constructor accepting the underlying {@link Store}, {@link CacheCluster}, {@link NonstopConfiguration} and
     * {@link NonstopExecutorService}
     *
     */
    public NonstopStoreImpl(TerracottaStore underlyingStore, CacheCluster cacheCluster, NonstopConfiguration nonstopConfig,
            NonstopExecutorService nonstopExecutorService) {
        this.underlyingStore = underlyingStore;
        this.nonstopConfig = nonstopConfig;
        this.timeoutBehaviors = new ConcurrentHashMap<NonstopTimeoutBehaviorType, NonstopStore>();

        Object context = underlyingStore.getInternalContext();
        if (context instanceof CacheLockProvider) {
            underlyingCacheLockProvider = (CacheLockProvider) context;
        } else {
            throw new AssertionError("TerracottaStore.getInternalContext() is not correct - "
                    + (context == null ? "NULL" : context.getClass().getName()));
        }

        ExecutorServiceStore clusterOnlineStore = new ExecutorServiceStore(underlyingStore, nonstopConfig, nonstopExecutorService, this,
                underlyingCacheLockProvider);
        ClusterOfflineStore clusterOfflineStore = new ClusterOfflineStore(nonstopConfig, this, clusterOnlineStore);
        clusterAwareStore = new ClusterAwareStore(cacheCluster, clusterOfflineStore, clusterOnlineStore);
    }

    /**
     * {@inheritDoc}
     */
    public NonstopStore resolveTimeoutStore() {
        NonstopStore timeoutStore = timeoutBehaviors.get(getTimeoutBehaviorStoreType());
        if (timeoutStore == null) {
            timeoutStore = getTimeoutBehaviorStoreType().newTimeoutStore(underlyingStore);
            NonstopStore prev = timeoutBehaviors.putIfAbsent(getTimeoutBehaviorStoreType(), timeoutStore);
            if (prev != null) {
                timeoutStore = prev;
            }
        }
        return timeoutStore;
    }

    private NonstopTimeoutBehaviorType getTimeoutBehaviorStoreType() {
        return NonstopTimeoutBehaviorType.getTypeFromConfigPropertyName(nonstopConfig.getTimeoutBehavior().getType());
    }

    /**
     * package-protected method. Use for tests only.
     *
     * @return The underlying store
     */
    Store getUnderlyingStore() {
        return underlyingStore;
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return this;
    }

    // -------------------------------------------------------
    // Methods below delegate directly to the underlying store
    // -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void addStoreListener(StoreListener listener) {
        underlyingStore.addStoreListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return underlyingStore.bufferFull();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        return underlyingStore.containsKeyOffHeap(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        return underlyingStore.containsKeyOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return underlyingStore.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return underlyingStore.getMBean();
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        return underlyingStore.getOffHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return underlyingStore.getOffHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return underlyingStore.getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return underlyingStore.getOnDiskSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return underlyingStore.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public void removeStoreListener(StoreListener listener) {
        underlyingStore.removeStoreListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        underlyingStore.setAttributeExtractors(extractors);
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        underlyingStore.setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        underlyingStore.waitUntilClusterCoherent();
    }

    // -------------------------------------------------------
    // All methods below delegate to the clusterAwareStore
    // -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean isCacheCoherent() {
        return clusterAwareStore.isCacheCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterCoherent() {
        return clusterAwareStore.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeCoherent() {
        return clusterAwareStore.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        clusterAwareStore.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        clusterAwareStore.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return clusterAwareStore.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return clusterAwareStore.containsKeyInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    public Results executeQuery(StoreQuery query) {
        return clusterAwareStore.executeQuery(query);
    }

    /**
     * {@inheritDoc}
     */
    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        return clusterAwareStore.getSearchAttribute(attributeName);
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        clusterAwareStore.expireElements();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        clusterAwareStore.flush();
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        return clusterAwareStore.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return clusterAwareStore.getInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return clusterAwareStore.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() {
        return clusterAwareStore.getKeys();
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        return clusterAwareStore.getQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return clusterAwareStore.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        return clusterAwareStore.getTerracottaClusteredSize();
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) throws CacheException {
        return clusterAwareStore.put(element);
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        return clusterAwareStore.putIfAbsent(element);
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return clusterAwareStore.putWithWriter(element, writerManager);
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        return clusterAwareStore.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        clusterAwareStore.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        return clusterAwareStore.removeElement(element, comparator);
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return clusterAwareStore.removeWithWriter(key, writerManager);
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        return clusterAwareStore.replace(old, element, comparator);
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        return clusterAwareStore.replace(element);
    }

    /**
     * Start cluster rejoin
     */
    public void clusterRejoinStarted() {
        clusterAwareStore.clusterRejoinStarted();
    }

    /**
     * Complete cluster rejoin
     */
    public void clusterRejoinComplete() {
        clusterAwareStore.clusterRejoinComplete();
    }

    /**
     * {@inheritDoc}
     */
    public Set getLocalKeys() {
        return clusterAwareStore.getLocalKeys();
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGet(Object key) {
        return clusterAwareStore.unlockedGet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGetQuiet(Object key) {
        return clusterAwareStore.unlockedGetQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGet(Object key) {
        return clusterAwareStore.unsafeGet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGetQuiet(Object key) {
        return clusterAwareStore.unsafeGetQuiet(key);
    }

    private Sync[] wrapWithNonstopSync(Sync[] syncs) {
        if (syncs == null) {
            return null;
        }
        for (int i = 0; i < syncs.length; i++) {
            Sync sync = syncs[i];
            syncs[i] = sync == null ? null : new NonstopSync(this, sync);
        }
        return syncs;
    }

    /**
     * {@inheritDoc}
     */
    public Sync[] getAndWriteLockAllSyncForKeys(long timeout, Object... keys) throws TimeoutException {
        return wrapWithNonstopSync(clusterAwareStore.getAndWriteLockAllSyncForKeys(timeout, keys));
    }

    /**
     * {@inheritDoc}
     */
    public Sync[] getAndWriteLockAllSyncForKeys(Object... keys) {
        return wrapWithNonstopSync(clusterAwareStore.getAndWriteLockAllSyncForKeys(keys));
    }

    /**
     * {@inheritDoc}
     */
    public Sync getSyncForKey(Object key) {
        Sync sync = clusterAwareStore.getSyncForKey(key);
        return sync == null ? null : new NonstopSync(this, sync);
    }

    /**
     * {@inheritDoc}
     */
    public void unlockWriteLockForAllKeys(Object... keys) {
        clusterAwareStore.unlockWriteLockForAllKeys(keys);
    }

    /**
     * {@inheritDoc}
     */
    public <V> V executeClusterOperation(ClusterOperation<V> operation) {
        return clusterAwareStore.executeClusterOperation(operation);
    }

}
