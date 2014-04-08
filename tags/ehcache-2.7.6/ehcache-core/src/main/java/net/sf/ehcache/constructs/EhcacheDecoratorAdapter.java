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

package net.sf.ehcache.constructs;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.attribute.DynamicAttributesExtractor;
import net.sf.ehcache.statistics.StatisticsGateway;
import net.sf.ehcache.terracotta.InternalEhcache;
import net.sf.ehcache.terracotta.TerracottaNotRunningException;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterManager;
import org.terracotta.statistics.StatisticsManager;

/**
 * Adapter class for Ehcache interface decorators. Implements all method in {@link Ehcache} by delegating all calls to the decorated
 * {@link Ehcache}. This class is provided as a convenience for easily creating {@link Ehcache} decorators by extending this class and
 * overriding only the methods of interest.
 *
 * @author Abhishek Sanoujam
 *
 */
public class EhcacheDecoratorAdapter implements InternalEhcache {

    /**
     * The decorated {@link Ehcache}, has protected visibility so that sub-classes can have access to it.
     */
    protected final Ehcache underlyingCache;

    /**
     * Constructor accepting the cache to be decorated
     *
     * @param underlyingCache
     */
    public EhcacheDecoratorAdapter(Ehcache underlyingCache) {
        StatisticsManager.associate(this).withParent(underlyingCache);
        this.underlyingCache = underlyingCache;
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) throws IllegalStateException, CacheException {
        return underlyingCache.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Map<Object, Element> getAll(Collection<?> keys) throws IllegalStateException, CacheException {
        return underlyingCache.getAll(keys);
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return underlyingCache.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) throws IllegalStateException, CacheException {
        return underlyingCache.getQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return underlyingCache.getQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException,
            CacheException {
        underlyingCache.put(element, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        underlyingCache.put(element);
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Collection<Element> elements) throws IllegalArgumentException, IllegalStateException, CacheException {
        underlyingCache.putAll(elements);
    }

    /**
     * {@inheritDoc}
     */
    public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        underlyingCache.putQuiet(element);
    }

    /**
     * {@inheritDoc}
     */
    public void putWithWriter(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        underlyingCache.putWithWriter(element);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return underlyingCache.remove(key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key) throws IllegalStateException {
        return underlyingCache.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(Collection<?> keys) throws IllegalStateException {
        underlyingCache.removeAll(keys);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(Collection<?> keys, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        underlyingCache.removeAll(keys, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return underlyingCache.remove(key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Serializable key) throws IllegalStateException {
        return underlyingCache.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        underlyingCache.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        underlyingCache.removeAll(doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public void bootstrap() {
        underlyingCache.bootstrap();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated public long calculateInMemorySize() throws IllegalStateException, CacheException {
        return underlyingCache.calculateInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated public long calculateOffHeapSize() throws IllegalStateException, CacheException {
        return underlyingCache.calculateOffHeapSize();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated public long calculateOnDiskSize() throws IllegalStateException, CacheException {
        return underlyingCache.calculateOnDiskSize();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasAbortedSizeOf() {
        return underlyingCache.hasAbortedSizeOf();
    }

    /**
     * {@inheritDoc}
     */
    public void disableDynamicFeatures() {
        underlyingCache.disableDynamicFeatures();
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() throws IllegalStateException {
        underlyingCache.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public void evictExpiredElements() {
        underlyingCache.evictExpiredElements();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IllegalStateException, CacheException {
        underlyingCache.flush();
    }

    /**
     * {@inheritDoc}
     */
    public Element getWithLoader(Object key, CacheLoader loader, Object loaderArgument) throws CacheException {
        return underlyingCache.getWithLoader(key, loader, loaderArgument);
    }

    /**
     * {@inheritDoc}
     */
    public Map getAllWithLoader(Collection keys, Object loaderArgument) throws CacheException {
        return underlyingCache.getAllWithLoader(keys, loaderArgument);
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheLoader(CacheLoader cacheLoader) {
        underlyingCache.registerCacheLoader(cacheLoader);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterCacheLoader(CacheLoader cacheLoader) {
        underlyingCache.unregisterCacheLoader(cacheLoader);
    }

    /**
     * {@inheritDoc}
     */
    public void load(Object key) throws CacheException {
        underlyingCache.load(key);
    }

    /**
     * {@inheritDoc}
     */
    public void loadAll(Collection keys, Object argument) throws CacheException {
        underlyingCache.loadAll(keys, argument);
    }

    /**
     * {@inheritDoc}
     */
    public BootstrapCacheLoader getBootstrapCacheLoader() {
        return underlyingCache.getBootstrapCacheLoader();
    }

    /**
     * {@inheritDoc}
     */
    public CacheConfiguration getCacheConfiguration() {
        return underlyingCache.getCacheConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    public RegisteredEventListeners getCacheEventNotificationService() {
        return underlyingCache.getCacheEventNotificationService();
    }

    /**
     * {@inheritDoc}
     */
    public CacheExceptionHandler getCacheExceptionHandler() {
        return underlyingCache.getCacheExceptionHandler();
    }

    /**
     * {@inheritDoc}
     */
    public CacheManager getCacheManager() {
        return underlyingCache.getCacheManager();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated public long getOffHeapStoreSize() throws IllegalStateException {
        return underlyingCache.getOffHeapStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated public int getDiskStoreSize() throws IllegalStateException {
        return underlyingCache.getDiskStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    public String getGuid() {
        return underlyingCache.getGuid();
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return underlyingCache.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() throws IllegalStateException, CacheException {
        return underlyingCache.getKeys();
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        return underlyingCache.getKeysNoDuplicateCheck();
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        return underlyingCache.getKeysWithExpiryCheck();
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated public long getMemoryStoreSize() throws IllegalStateException {
        return underlyingCache.getMemoryStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return underlyingCache.getName();
    }

    /**
     * {@inheritDoc}
     */
    public List<CacheExtension> getRegisteredCacheExtensions() {
        return underlyingCache.getRegisteredCacheExtensions();
    }

    /**
     * {@inheritDoc}
     */
    public List<CacheLoader> getRegisteredCacheLoaders() {
        return underlyingCache.getRegisteredCacheLoaders();
    }

    /**
     * {@inheritDoc}
     */
    public CacheWriter getRegisteredCacheWriter() {
        return underlyingCache.getRegisteredCacheWriter();
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() throws IllegalStateException, CacheException {
        return underlyingCache.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return underlyingCache.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public CacheWriterManager getWriterManager() {
        return underlyingCache.getWriterManager();
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() {
        underlyingCache.initialise();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Use {@link #isClusterBulkLoadEnabled()} instead
     */
    @Deprecated
    public boolean isClusterCoherent() {
        return underlyingCache.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDisabled() {
        return underlyingCache.isDisabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementInMemory(Object key) {
        return underlyingCache.isElementInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementInMemory(Serializable key) {
        return underlyingCache.isElementInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementOnDisk(Object key) {
        return underlyingCache.isElementOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementOnDisk(Serializable key) {
        return underlyingCache.isElementOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExpired(Element element) throws IllegalStateException, NullPointerException {
        return underlyingCache.isExpired(element);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isKeyInCache(Object key) {
        return underlyingCache.isKeyInCache(key);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Use {@link #isNodeBulkLoadEnabled()} instead
     */
    @Deprecated
    public boolean isNodeCoherent() {
        return underlyingCache.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValueInCache(Object value) {
        return underlyingCache.isValueInCache(value);
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheExtension(CacheExtension cacheExtension) {
        underlyingCache.registerCacheExtension(cacheExtension);
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheWriter(CacheWriter cacheWriter) {
        underlyingCache.registerCacheWriter(cacheWriter);
    }


    /**
     * {@inheritDoc}
     */
    public void registerDynamicAttributesExtractor(DynamicAttributesExtractor extractor) {
        underlyingCache.registerDynamicAttributesExtractor(extractor);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeQuiet(Object key) throws IllegalStateException {
        return underlyingCache.removeQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return underlyingCache.removeQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        return underlyingCache.removeWithWriter(key);
    }

    /**
     * {@inheritDoc}
     */
    public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) throws CacheException {
        underlyingCache.setBootstrapCacheLoader(bootstrapCacheLoader);
    }

    /**
     * {@inheritDoc}
     */
    public void setCacheExceptionHandler(CacheExceptionHandler cacheExceptionHandler) {
        underlyingCache.setCacheExceptionHandler(cacheExceptionHandler);
    }

    /**
     * {@inheritDoc}
     */
    public void setCacheManager(CacheManager cacheManager) {
        underlyingCache.setCacheManager(cacheManager);
    }

    /**
     * {@inheritDoc}
     */
    public void setDisabled(boolean disabled) {
        underlyingCache.setDisabled(disabled);
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name) {
        underlyingCache.setName(name);
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Use {@link #setNodeBulkLoadEnabled(boolean)} instead
     */
    @Deprecated
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        underlyingCache.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     */
    public void setTransactionManagerLookup(TransactionManagerLookup transactionManagerLookup) {
        underlyingCache.setTransactionManagerLookup(transactionManagerLookup);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterCacheExtension(CacheExtension cacheExtension) {
        underlyingCache.unregisterCacheExtension(cacheExtension);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterCacheWriter() {
        underlyingCache.unregisterCacheWriter();
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Use {@link #waitUntilClusterBulkLoadComplete()} instead
     */
    @Deprecated
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        underlyingCache.waitUntilClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        return underlyingCache.putIfAbsent(element);
    }

    @Override
    public Element putIfAbsent(final Element element, final boolean doNotNotifyCacheReplicators) throws NullPointerException {
        return underlyingCache.putIfAbsent(element, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeElement(Element element) throws NullPointerException {
        return underlyingCache.removeElement(element);
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        return underlyingCache.replace(old, element);
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        return underlyingCache.replace(element);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.Ehcache#addPropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        underlyingCache.addPropertyChangeListener(listener);
    }

    /**
     * {@inheritDoc}
     *
     * @see net.sf.ehcache.Ehcache#removePropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        underlyingCache.addPropertyChangeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return underlyingCache.toString();
    }

    /**
     * {@inheritDoc}
     */
    public Query createQuery() {
        return underlyingCache.createQuery();
    }

    /**
     * {@inheritDoc}
     */
    public <T> Attribute<T> getSearchAttribute(String attributeName) throws CacheException {
        return underlyingCache.getSearchAttribute(attributeName);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSearchable() {
        return underlyingCache.isSearchable();
    }

    /**
     * {@inheritDoc}
     */
    public void acquireReadLockOnKey(Object key) {
        underlyingCache.acquireReadLockOnKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public void acquireWriteLockOnKey(Object key) {
        underlyingCache.acquireWriteLockOnKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public void releaseReadLockOnKey(Object key) {
        underlyingCache.releaseReadLockOnKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public void releaseWriteLockOnKey(Object key) {
        underlyingCache.releaseWriteLockOnKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean tryReadLockOnKey(Object key, long timeout) throws InterruptedException {
        return underlyingCache.tryReadLockOnKey(key, timeout);
    }

    /**
     * {@inheritDoc}
     */
    public boolean tryWriteLockOnKey(Object key, long timeout) throws InterruptedException {
        return underlyingCache.tryWriteLockOnKey(key, timeout);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadLockedByCurrentThread(Object key) {
        return underlyingCache.isReadLockedByCurrentThread(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isWriteLockedByCurrentThread(Object key) {
        return underlyingCache.isWriteLockedByCurrentThread(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterBulkLoadEnabled() throws UnsupportedOperationException, TerracottaNotRunningException {
        return underlyingCache.isClusterBulkLoadEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeBulkLoadEnabled() throws UnsupportedOperationException, TerracottaNotRunningException {
        return underlyingCache.isNodeBulkLoadEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeBulkLoadEnabled(boolean enabledBulkLoad) throws UnsupportedOperationException, TerracottaNotRunningException {
        underlyingCache.setNodeBulkLoadEnabled(enabledBulkLoad);
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilClusterBulkLoadComplete() throws UnsupportedOperationException, TerracottaNotRunningException {
        underlyingCache.waitUntilClusterBulkLoadComplete();
    }

    private InternalEhcache asInternalEhcache() {
        return (InternalEhcache) underlyingCache;
    }

    /**
     * {@inheritDoc}
     */
    public Element removeAndReturnElement(Object key) throws IllegalStateException {
        if (underlyingCache instanceof InternalEhcache) {
            return asInternalEhcache().removeAndReturnElement(key);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void recalculateSize(Object key) {
        if (underlyingCache instanceof InternalEhcache) {
            asInternalEhcache().recalculateSize(key);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public StatisticsGateway getStatistics() throws IllegalStateException {
       return underlyingCache.getStatistics();
    }
}
