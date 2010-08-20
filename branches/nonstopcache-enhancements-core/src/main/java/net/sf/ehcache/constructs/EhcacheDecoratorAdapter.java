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
import net.sf.ehcache.Statistics;
import net.sf.ehcache.Status;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.RegisteredEventListeners;
import net.sf.ehcache.exceptionhandler.CacheExceptionHandler;
import net.sf.ehcache.extension.CacheExtension;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.statistics.CacheUsageListener;
import net.sf.ehcache.statistics.LiveCacheStatistics;
import net.sf.ehcache.statistics.sampled.SampledCacheStatistics;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.writer.CacheWriter;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * Adapter class for Ehcache interface decorators. Implements all method in {@link Ehcache} by delegating all calls to the decorated
 * {@link Ehcache}. This class is provided as a convenience for easily creating {@link Ehcache} decorators by extending this class and
 * overriding only the methods of interest.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class EhcacheDecoratorAdapter implements Ehcache {

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
    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        return underlyingCache.calculateInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    public void clearStatistics() {
        underlyingCache.clearStatistics();
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
    public float getAverageGetTime() {
        return underlyingCache.getAverageGetTime();
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
        return this.getCacheExceptionHandler();
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
    public int getDiskStoreSize() throws IllegalStateException {
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
    public LiveCacheStatistics getLiveCacheStatistics() throws IllegalStateException {
        return underlyingCache.getLiveCacheStatistics();
    }

    /**
     * {@inheritDoc}
     */
    public long getMemoryStoreSize() throws IllegalStateException {
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
    public SampledCacheStatistics getSampledCacheStatistics() {
        return underlyingCache.getSampledCacheStatistics();
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
    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        return underlyingCache.getSizeBasedOnAccuracy(statisticsAccuracy);
    }

    /**
     * {@inheritDoc}
     */
    public Statistics getStatistics() throws IllegalStateException {
        return underlyingCache.getStatistics();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatisticsAccuracy() {
        return underlyingCache.getStatisticsAccuracy();
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
     */
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
     */
    public boolean isNodeCoherent() {
        return underlyingCache.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSampledStatisticsEnabled() {
        return underlyingCache.isSampledStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return underlyingCache.isStatisticsEnabled();
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
    public void registerCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException {
        underlyingCache.registerCacheUsageListener(cacheUsageListener);
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
    public void removeCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException {
        underlyingCache.removeCacheUsageListener(cacheUsageListener);
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
    public void setDiskStorePath(String diskStorePath) throws CacheException {
        underlyingCache.setDiskStorePath(diskStorePath);
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name) {
        underlyingCache.setName(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        underlyingCache.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     */
    public void setSampledStatisticsEnabled(boolean enableStatistics) {
        underlyingCache.setSampledStatisticsEnabled(enableStatistics);
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsAccuracy(int statisticsAccuracy) {
        underlyingCache.setStatisticsAccuracy(statisticsAccuracy);
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsEnabled(boolean enableStatistics) {
        underlyingCache.setStatisticsEnabled(enableStatistics);
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
     */
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

}
