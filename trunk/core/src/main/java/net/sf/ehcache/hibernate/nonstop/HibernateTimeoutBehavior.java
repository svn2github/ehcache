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

package net.sf.ehcache.hibernate.nonstop;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.store.NonstopStore;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.writer.CacheWriterManager;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link NonstopStore} that is used on timeout, used with Hibernate for nonstop cache feature.
 * On timeout, if {@link HibernateTimeoutBehavior#HIBERNATE_THROW_EXCEPTION_ON_TIMEOUT_PROPERTY} property is set, an exception is thrown
 * otherwise it goes to logs (depending on whether logging level is enabled)
 *
 * @author Abhishek Sanoujam
 *
 */
public final class HibernateTimeoutBehavior implements NonstopStore {

    /**
     * Property name which set as "true" will throw exceptions on timeout with hibernate
     */
    public static final String HIBERNATE_THROW_EXCEPTION_ON_TIMEOUT_PROPERTY = "ehcache.hibernate.propagateNonStopCacheException";
    private static final String HIBERNATE_NONSTOP_TIMEOUT_EXCEPTION_MSG = "Operation on the cache timed out";
    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateTimeoutBehavior.class);

    private static final HibernateTimeoutBehavior INSTANCE = new HibernateTimeoutBehavior();

    /**
     * Private constructor
     */
    private HibernateTimeoutBehavior() {
        // private constructor
    }

    /**
     * Returns the singleton instance
     *
     * @return the singleton instance
     */
    public static HibernateTimeoutBehavior getInstance() {
        return INSTANCE;
    }

    private void operationTimedout() {
        if (Boolean.getBoolean(HIBERNATE_THROW_EXCEPTION_ON_TIMEOUT_PROPERTY)) {
            throw new HibernateException(HIBERNATE_NONSTOP_TIMEOUT_EXCEPTION_MSG);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(HIBERNATE_NONSTOP_TIMEOUT_EXCEPTION_MSG);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#addStoreListener(net.sf.ehcache.store.StoreListener)
     */
    public void addStoreListener(StoreListener listener) {
        operationTimedout();
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#bufferFull()
     */
    public boolean bufferFull() {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#containsKeyInMemory(java.lang.Object)
     */
    public boolean containsKeyInMemory(Object key) {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#containsKeyOffHeap(java.lang.Object)
     */
    public boolean containsKeyOffHeap(Object key) {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#containsKeyOnDisk(java.lang.Object)
     */
    public boolean containsKeyOnDisk(Object key) {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#dispose()
     */
    public void dispose() {
        operationTimedout();

    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#executeQuery(net.sf.ehcache.store.StoreQuery)
     */
    public Results executeQuery(StoreQuery query) throws SearchException {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#expireElements()
     */
    public void expireElements() {
        operationTimedout();

    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#flush()
     */
    public void flush() throws IOException {
        operationTimedout();

    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#get(java.lang.Object)
     */
    public Element get(Object key) {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getInMemoryEvictionPolicy()
     */
    public Policy getInMemoryEvictionPolicy() {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getInMemorySize()
     */
    public int getInMemorySize() {
        operationTimedout();
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getInMemorySizeInBytes()
     */
    public long getInMemorySizeInBytes() {
        operationTimedout();
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getInternalContext()
     */
    public Object getInternalContext() {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getKeys()
     */
    public List getKeys() {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getMBean()
     */
    public Object getMBean() {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getOffHeapSize()
     */
    public int getOffHeapSize() {
        operationTimedout();
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getOffHeapSizeInBytes()
     */
    public long getOffHeapSizeInBytes() {
        operationTimedout();
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getOnDiskSize()
     */
    public int getOnDiskSize() {
        operationTimedout();
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getOnDiskSizeInBytes()
     */
    public long getOnDiskSizeInBytes() {
        operationTimedout();
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getQuiet(java.lang.Object)
     */
    public Element getQuiet(Object key) {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getSearchAttribute(java.lang.String)
     */
    public <T> Attribute<T> getSearchAttribute(String attributeName) {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getSize()
     */
    public int getSize() {
        operationTimedout();
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getStatus()
     */
    public Status getStatus() {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#getTerracottaClusteredSize()
     */
    public int getTerracottaClusteredSize() {
        operationTimedout();
        return 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#isCacheCoherent()
     */
    public boolean isCacheCoherent() {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#isClusterCoherent()
     */
    public boolean isClusterCoherent() throws TerracottaNotRunningException {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#isNodeCoherent()
     */
    public boolean isNodeCoherent() throws TerracottaNotRunningException {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#put(net.sf.ehcache.Element)
     */
    public boolean put(Element element) throws CacheException {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#putIfAbsent(net.sf.ehcache.Element)
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#putWithWriter(net.sf.ehcache.Element, net.sf.ehcache.writer.CacheWriterManager)
     */
    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#remove(java.lang.Object)
     */
    public Element remove(Object key) {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#removeAll()
     */
    public void removeAll() throws CacheException {
        operationTimedout();

    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#removeElement(net.sf.ehcache.Element, net.sf.ehcache.store.ElementValueComparator)
     */
    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#removeStoreListener(net.sf.ehcache.store.StoreListener)
     */
    public void removeStoreListener(StoreListener listener) {
        operationTimedout();

    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#removeWithWriter(java.lang.Object, net.sf.ehcache.writer.CacheWriterManager)
     */
    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#replace(net.sf.ehcache.Element, net.sf.ehcache.Element, net.sf.ehcache.store.ElementValueComparator)
     */
    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        operationTimedout();
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#replace(net.sf.ehcache.Element)
     */
    public Element replace(Element element) throws NullPointerException {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#setAttributeExtractors(java.util.Map)
     */
    public void setAttributeExtractors(Map<String, AttributeExtractor> extractors) {
        operationTimedout();

    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#setInMemoryEvictionPolicy(net.sf.ehcache.store.Policy)
     */
    public void setInMemoryEvictionPolicy(Policy policy) {
        operationTimedout();

    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#setNodeCoherent(boolean)
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException, TerracottaNotRunningException {
        operationTimedout();

    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.Store#waitUntilClusterCoherent()
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException, TerracottaNotRunningException {
        operationTimedout();

    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.constructs.nonstop.store.NonstopStore#executeClusterOperation(net.sf.ehcache.constructs.nonstop.ClusterOperation)
     */
    public <V> V executeClusterOperation(ClusterOperation<V> operation) {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.concurrent.CacheLockProvider#getAndWriteLockAllSyncForKeys(long, java.lang.Object[])
     */
    public Sync[] getAndWriteLockAllSyncForKeys(long timeout, Object... keys) throws TimeoutException {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.concurrent.CacheLockProvider#getAndWriteLockAllSyncForKeys(java.lang.Object[])
     */
    public Sync[] getAndWriteLockAllSyncForKeys(Object... keys) {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.concurrent.CacheLockProvider#getSyncForKey(java.lang.Object)
     */
    public Sync getSyncForKey(Object key) {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.concurrent.CacheLockProvider#unlockWriteLockForAllKeys(java.lang.Object[])
     */
    public void unlockWriteLockForAllKeys(Object... keys) {
        operationTimedout();

    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.TerracottaStore#getLocalKeys()
     */
    public Set getLocalKeys() {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.TerracottaStore#unlockedGet(java.lang.Object)
     */
    public Element unlockedGet(Object key) {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.TerracottaStore#unlockedGetQuiet(java.lang.Object)
     */
    public Element unlockedGetQuiet(Object key) {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.TerracottaStore#unsafeGet(java.lang.Object)
     */
    public Element unsafeGet(Object key) {
        operationTimedout();
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.store.TerracottaStore#unsafeGetQuiet(java.lang.Object)
     */
    public Element unsafeGetQuiet(Object key) {
        operationTimedout();
        return null;
    }

}
