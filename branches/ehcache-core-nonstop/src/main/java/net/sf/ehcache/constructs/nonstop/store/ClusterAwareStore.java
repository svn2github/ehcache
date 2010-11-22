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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterTopologyListener;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.Store;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * A store implementation which is aware of the cluster status and delegates accordingly.
 *
 * @author Abhishek Sanoujam
 *
 */
public class ClusterAwareStore implements Store {

    private final ClusterOfflineStore clusterOfflineStore;
    private final ExecutorServiceStore clusterOnlineStore;
    private volatile Store delegate;

    /**
     * Constructor accepting the {@link CacheCluster}, {@link ClusterOfflineStore} (used when cluster is offline) and
     * {@link ExecutorServiceStore} (used when cluster is online)
     *
     * @param cacheCluster
     * @param clusterOfflineStore
     * @param clusterOnlineStore
     */
    public ClusterAwareStore(CacheCluster cacheCluster, ClusterOfflineStore clusterOfflineStore, ExecutorServiceStore clusterOnlineStore) {
        this.clusterOfflineStore = clusterOfflineStore;
        this.clusterOnlineStore = clusterOnlineStore;
        if (cacheCluster.isClusterOnline()) {
            delegate = clusterOnlineStore;
        } else {
            delegate = clusterOfflineStore;
        }
        cacheCluster.addTopologyListener(new ClusterStatusListener(this, cacheCluster));
    }

    private void clusterOffline() {
        delegate = clusterOfflineStore;
    }

    private void clusterOnline() {
        delegate = clusterOnlineStore;
    }

    /**
     * {@inheritDoc}
     */
    public void addStoreListener(StoreListener listener) {
        delegate.addStoreListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public boolean bufferFull() {
        return delegate.bufferFull();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyInMemory(Object key) {
        return delegate.containsKeyInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOffHeap(Object key) {
        return delegate.containsKeyOffHeap(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKeyOnDisk(Object key) {
        return delegate.containsKeyOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        delegate.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public Results executeQuery(StoreQuery query) {
        return delegate.executeQuery(query);
    }

    /**
     * {@inheritDoc}
     */
    public void expireElements() {
        delegate.expireElements();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IOException {
        delegate.flush();
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) {
        return delegate.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Policy getInMemoryEvictionPolicy() {
        return delegate.getInMemoryEvictionPolicy();
    }

    /**
     * {@inheritDoc}
     */
    public int getInMemorySize() {
        return delegate.getInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    public long getInMemorySizeInBytes() {
        return delegate.getInMemorySizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return delegate.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() {
        return delegate.getKeys();
    }

    /**
     * {@inheritDoc}
     */
    public Object getMBean() {
        return delegate.getMBean();
    }

    /**
     * {@inheritDoc}
     */
    public int getOffHeapSize() {
        return delegate.getOffHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getOffHeapSizeInBytes() {
        return delegate.getOffHeapSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public int getOnDiskSize() {
        return delegate.getOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public long getOnDiskSizeInBytes() {
        return delegate.getOnDiskSizeInBytes();
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) {
        return delegate.getQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return delegate.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return delegate.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public int getTerracottaClusteredSize() {
        return delegate.getTerracottaClusteredSize();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCacheCoherent() {
        return delegate.isCacheCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterCoherent() {
        return delegate.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeCoherent() {
        return delegate.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(Element element) throws CacheException {
        return delegate.put(element);
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        return delegate.putIfAbsent(element);
    }

    /**
     * {@inheritDoc}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return delegate.putWithWriter(element, writerManager);
    }

    /**
     * {@inheritDoc}
     */
    public Element remove(Object key) {
        return delegate.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws CacheException {
        delegate.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        return delegate.removeElement(element, comparator);
    }

    /**
     * {@inheritDoc}
     */
    public void removeStoreListener(StoreListener listener) {
        delegate.removeStoreListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return delegate.removeWithWriter(key, writerManager);
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        return delegate.replace(old, element, comparator);
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        return delegate.replace(element);
    }

    /**
     * {@inheritDoc}
     */
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        delegate.setAttributeExtractors(extractors);
    }

    /**
     * {@inheritDoc}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        delegate.setInMemoryEvictionPolicy(policy);
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        delegate.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        delegate.waitUntilClusterCoherent();
    }

    /**
     * A {@link ClusterTopologyListener} implementation that listens for cluster online/offline events
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class ClusterStatusListener implements ClusterTopologyListener {

        private final ClusterAwareStore clusterAwareStore;
        private final CacheCluster cacheCluster;

        public ClusterStatusListener(ClusterAwareStore clusterAwareStore, CacheCluster cacheCluster) {
            this.clusterAwareStore = clusterAwareStore;
            this.cacheCluster = cacheCluster;
        }

        /**
         * {@inheritDoc}
         */
        public void clusterOffline(ClusterNode node) {
            if (cacheCluster.getCurrentNode().equals(node)) {
                clusterAwareStore.clusterOffline();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void clusterOnline(ClusterNode node) {
            if (cacheCluster.getCurrentNode().equals(node)) {
                clusterAwareStore.clusterOnline();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void nodeJoined(ClusterNode node) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        public void nodeLeft(ClusterNode node) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
            // no-op
        }

    }
}
