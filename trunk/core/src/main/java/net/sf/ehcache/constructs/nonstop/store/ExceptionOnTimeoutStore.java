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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.NonstopTimeoutBehaviorFactory;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * Implementation of {@link NonstopStore} that throws {@link NonStopCacheException} for all operations.
 *
 * @author Abhishek Sanoujam
 *
 */
public final class ExceptionOnTimeoutStore implements NonstopStore {

    /**
     * The {@link NonstopTimeoutBehaviorFactory} to create {@link ExceptionOnTimeoutStore} stores
     */
    public static final NonstopTimeoutBehaviorFactory FACTORY = new NonstopTimeoutBehaviorFactory() {
        public NonstopStore createNonstopTimeoutBehaviorStore(NonstopActiveDelegateHolder nonstopActiveDelegateHolder) {
            return ExceptionOnTimeoutStore.getInstance();
        }
    };

    /**
     * the singleton instance
     */
    private static final ExceptionOnTimeoutStore INSTANCE = new ExceptionOnTimeoutStore();

    /**
     * private constructor
     */
    private ExceptionOnTimeoutStore() {
        //
    }

    /**
     * returns the singleton instance
     *
     */
    public static ExceptionOnTimeoutStore getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Element get(final Object key) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("get for key - '" + key + "'  timed out");
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getQuiet for key - '" + key + "'  timed out");
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Element> getAllQuiet(Collection<Object> keys) {
        throw new NonStopCacheException("getAllQuiet for '" + keys.size() + "' keys timed out");
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Element> getAll(Collection<Object> keys) {
        throw new NonStopCacheException("getAll for '" + keys.size() + "' keys timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public List getKeys() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getKeys timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        throw new NonStopCacheException("put for element - '" + element + "' timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void putAll(final Collection<Element> elements) throws CacheException {
        throw new NonStopCacheException("putAll for " + elements.size() + " elements timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Element remove(final Object key) throws IllegalStateException {
        throw new NonStopCacheException("remove for key - '" + key + "' timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void removeAll(final Collection<Object> keys) throws IllegalStateException {
        throw new NonStopCacheException("removeAll for " + keys.size() + "  keys timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("removeAll timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void flush() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("flush timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Object getInternalContext() {
        throw new NonStopCacheException("getInternalContext timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public int getSize() throws IllegalStateException, CacheException {
        throw new NonStopCacheException("getSize timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        throw new NonStopCacheException("putIfAbsent timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Element replace(Element element) throws NullPointerException {
        throw new NonStopCacheException("replace timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void addStoreListener(StoreListener listener) {
        throw new NonStopCacheException("addStoreListener timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean bufferFull() {
        throw new NonStopCacheException("bufferFull timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean containsKey(Object key) {
        throw new NonStopCacheException("containsKey timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean containsKeyInMemory(Object key) {
        throw new NonStopCacheException("containsKeyInMemory timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean containsKeyOffHeap(Object key) {
        throw new NonStopCacheException("containsKeyOffHeap timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean containsKeyOnDisk(Object key) {
        throw new NonStopCacheException("containsKeyOnDisk timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void dispose() {
        throw new NonStopCacheException("dispose timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Results executeQuery(StoreQuery query) {
        throw new NonStopCacheException("executeQuery timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void expireElements() {
        throw new NonStopCacheException("expireElements timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Policy getInMemoryEvictionPolicy() {
        throw new NonStopCacheException("getInMemoryEvictionPolicy timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public int getInMemorySize() {
        throw new NonStopCacheException("getInMemorySize timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public long getInMemorySizeInBytes() {
        throw new NonStopCacheException("getInMemorySizeInBytes timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Object getMBean() {
        throw new NonStopCacheException("getMBean timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public int getOffHeapSize() {
        throw new NonStopCacheException("getOffHeapSize timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public long getOffHeapSizeInBytes() {
        throw new NonStopCacheException("getOffHeapSizeInBytes timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public int getOnDiskSize() {
        throw new NonStopCacheException("getOnDiskSize timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public long getOnDiskSizeInBytes() {
        throw new NonStopCacheException("getOnDiskSizeInBytes timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean hasAbortedSizeOf() {
        throw new NonStopCacheException("hasAbortedSizeOf timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Status getStatus() {
        throw new NonStopCacheException("getStatus timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public int getTerracottaClusteredSize() {
        throw new NonStopCacheException("getTerracottaClusteredSize timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean isCacheCoherent() {
        throw new NonStopCacheException("isCacheCoherent timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean isClusterCoherent() {
        throw new NonStopCacheException("isClusterCoherent timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean isNodeCoherent() {
        throw new NonStopCacheException("isNodeCoherent timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        throw new NonStopCacheException("putWithWriter timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        throw new NonStopCacheException("removeElement timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void removeStoreListener(StoreListener listener) {
        throw new NonStopCacheException("removeStoreListener timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        throw new NonStopCacheException("removeWithWriter timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        throw new NonStopCacheException("replace timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        throw new NonStopCacheException("setAttributeExtractors timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        throw new NonStopCacheException("setInMemoryEvictionPolicy timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        throw new NonStopCacheException("setNodeCoherent timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        throw new NonStopCacheException("waitUntilClusterCoherent timed out");
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Throws {@link NonStopCacheException}
     */
    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        throw new NonStopCacheException("getSearchAttribute timed out");
    }

    /**
     * {@inheritDoc}
     */
    public Set getLocalKeys() {
        throw new NonStopCacheException("getLocalKeys() timed out");
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGet(Object key) {
        throw new NonStopCacheException("unlockedGet() timed out");
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGetQuiet(Object key) {
        throw new NonStopCacheException("unlockedGetQuiet() timed out");
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGet(Object key) {
        throw new NonStopCacheException("unsafeGet() timed out");
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGetQuiet(Object key) {
        throw new NonStopCacheException("unsafeGetQuiet() timed out");
    }

    /**
     * {@inheritDoc}
     */
    public <V> V executeClusterOperation(ClusterOperation<V> operation) {
        return operation.performClusterOperationTimedOut(TimeoutBehaviorType.EXCEPTION);
    }
}
