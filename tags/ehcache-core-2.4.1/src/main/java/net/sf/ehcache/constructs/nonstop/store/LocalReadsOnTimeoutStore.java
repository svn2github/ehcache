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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.NonstopTimeoutBehaviorFactory;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.NullResults;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * A {@link NonstopStore} implementation that returns the local value in the VM, if present, for get operations and no-op for put,
 * remove and other operations
 *
 * @author Abhishek Sanoujam
 *
 */
public class LocalReadsOnTimeoutStore implements NonstopStore {

    /**
     * The {@link NonstopTimeoutBehaviorFactory} to create {@link LocalReadsOnTimeoutStore} stores
     */
    public static final NonstopTimeoutBehaviorFactory FACTORY = new NonstopTimeoutBehaviorFactory() {
        public NonstopStore createNonstopTimeoutBehaviorStore(NonstopActiveDelegateHolder nonstopActiveDelegateHolder) {
            return new LocalReadsOnTimeoutStore(nonstopActiveDelegateHolder);
        }
    };

    private final NonstopActiveDelegateHolder nonstopActiveDelegateHolder;

    /**
     * Constructor accepting the {@link NonstopActiveDelegateHolder}
     */
    public LocalReadsOnTimeoutStore(NonstopActiveDelegateHolder nonstopActiveDelegateHolder) {
        this.nonstopActiveDelegateHolder = nonstopActiveDelegateHolder;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public Element get(Object key) throws IllegalStateException, CacheException {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unsafeGet(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public List getKeys() throws IllegalStateException, CacheException {
        return Collections.unmodifiableList(new ArrayList(nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getLocalKeys()));
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Uses the underlying store to get the local value present in the VM
     */
    public Element getQuiet(Object key) throws IllegalStateException, CacheException {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unsafeGetQuiet(key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Element remove(Object key) throws IllegalStateException {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void removeAll() throws IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void flush() throws IllegalStateException, CacheException {
        // no-op
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op and always returns null
     */
    public Object getInternalContext() {
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op and always returns zero
     */
    public int getSize() throws IllegalStateException, CacheException {
        return getKeys().size();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Element replace(Element element) throws NullPointerException {
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void addStoreListener(StoreListener listener) {
        // no-op

    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean bufferFull() {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean containsKey(Object key) {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean containsKeyInMemory(Object key) {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean containsKeyOffHeap(Object key) {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean containsKeyOnDisk(Object key) {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void dispose() {
        // no-op

    }

    /**
     * {@inheritDoc}.
     */
    public Results executeQuery(StoreQuery query) {
        return NullResults.INSTANCE;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void expireElements() {
        // no-op

    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Policy getInMemoryEvictionPolicy() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public int getInMemorySize() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public long getInMemorySizeInBytes() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Object getMBean() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public int getOffHeapSize() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public long getOffHeapSizeInBytes() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public int getOnDiskSize() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public long getOnDiskSizeInBytes() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Status getStatus() {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public int getTerracottaClusteredSize() {
        // no-op
        return 0;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean isCacheCoherent() {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean isClusterCoherent() {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean isNodeCoherent() {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void removeStoreListener(StoreListener listener) {
        // no-op

    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        // no-op
        return null;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        // no-op
        return false;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        // no-op

    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        // no-op

    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        // no-op

    }

    /**
     * {@inheritDoc}.
     * <p>
     * This is a no-op
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        // no-op

    }

    /**
     * {@inheritDoc}.
     */
    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        return new Attribute<T>(attributeName);
    }

    /**
     * {@inheritDoc}
     */
    public Set getLocalKeys() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getLocalKeys();
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGet(Object key) {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unsafeGet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGetQuiet(Object key) {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unsafeGet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGet(Object key) {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unsafeGet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGetQuiet(Object key) {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unsafeGetQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public <V> V executeClusterOperation(ClusterOperation<V> operation) {
        return operation.performClusterOperationTimedOut(TimeoutBehaviorType.LOCAL_READS);
    }
}
