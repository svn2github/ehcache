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
    protected final Ehcache decoratedCache;

    /**
     * Constructor accepting the cache to be decorated
     * 
     * @param decoratedCache
     */
    public EhcacheDecoratorAdapter(Ehcache decoratedCache) {
        this.decoratedCache = decoratedCache;
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Object key) throws IllegalStateException, CacheException {
        return decoratedCache.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return decoratedCache.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Object key) throws IllegalStateException, CacheException {
        return decoratedCache.getQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return decoratedCache.getQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element, boolean doNotNotifyCacheReplicators) throws IllegalArgumentException, IllegalStateException,
            CacheException {
        decoratedCache.put(element, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public void put(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        decoratedCache.put(element);
    }

    /**
     * {@inheritDoc}
     */
    public void putQuiet(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        decoratedCache.putQuiet(element);
    }

    /**
     * {@inheritDoc}
     */
    public void putWithWriter(Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        decoratedCache.putWithWriter(element);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return decoratedCache.remove(key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object key) throws IllegalStateException {
        return decoratedCache.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return decoratedCache.remove(key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Serializable key) throws IllegalStateException {
        return decoratedCache.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        decoratedCache.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        decoratedCache.removeAll(doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public void bootstrap() {
        decoratedCache.bootstrap();
    }

    /**
     * {@inheritDoc}
     */
    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        return decoratedCache.calculateInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    public void clearStatistics() {
        decoratedCache.clearStatistics();
    }

    /**
     * {@inheritDoc}
     */
    public void disableDynamicFeatures() {
        decoratedCache.disableDynamicFeatures();
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() throws IllegalStateException {
        decoratedCache.dispose();
    }

    /**
     * {@inheritDoc}
     */
    public void evictExpiredElements() {
        decoratedCache.evictExpiredElements();
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IllegalStateException, CacheException {
        decoratedCache.flush();
    }

    /**
     * {@inheritDoc}
     */
    public Element getWithLoader(Object key, CacheLoader loader, Object loaderArgument) throws CacheException {
        return decoratedCache.getWithLoader(key, loader, loaderArgument);
    }

    /**
     * {@inheritDoc}
     */
    public Map getAllWithLoader(Collection keys, Object loaderArgument) throws CacheException {
        return decoratedCache.getAllWithLoader(keys, loaderArgument);
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheLoader(CacheLoader cacheLoader) {
        decoratedCache.registerCacheLoader(cacheLoader);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterCacheLoader(CacheLoader cacheLoader) {
        decoratedCache.unregisterCacheLoader(cacheLoader);
    }

    /**
     * {@inheritDoc}
     */
    public void load(Object key) throws CacheException {
        decoratedCache.load(key);
    }

    /**
     * {@inheritDoc}
     */
    public void loadAll(Collection keys, Object argument) throws CacheException {
        decoratedCache.loadAll(keys, argument);
    }

    /**
     * {@inheritDoc}
     */
    public float getAverageGetTime() {
        return decoratedCache.getAverageGetTime();
    }

    /**
     * {@inheritDoc}
     */
    public BootstrapCacheLoader getBootstrapCacheLoader() {
        return decoratedCache.getBootstrapCacheLoader();
    }

    /**
     * {@inheritDoc}
     */
    public CacheConfiguration getCacheConfiguration() {
        return decoratedCache.getCacheConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    public RegisteredEventListeners getCacheEventNotificationService() {
        return decoratedCache.getCacheEventNotificationService();
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
        return decoratedCache.getCacheManager();
    }

    /**
     * {@inheritDoc}
     */
    public int getDiskStoreSize() throws IllegalStateException {
        return decoratedCache.getDiskStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    public String getGuid() {
        return decoratedCache.getGuid();
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        return decoratedCache.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() throws IllegalStateException, CacheException {
        return decoratedCache.getKeys();
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        return decoratedCache.getKeysNoDuplicateCheck();
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        return decoratedCache.getKeysWithExpiryCheck();
    }

    /**
     * {@inheritDoc}
     */
    public LiveCacheStatistics getLiveCacheStatistics() throws IllegalStateException {
        return decoratedCache.getLiveCacheStatistics();
    }

    /**
     * {@inheritDoc}
     */
    public long getMemoryStoreSize() throws IllegalStateException {
        return decoratedCache.getMemoryStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return decoratedCache.getName();
    }

    /**
     * {@inheritDoc}
     */
    public List<CacheExtension> getRegisteredCacheExtensions() {
        return decoratedCache.getRegisteredCacheExtensions();
    }

    /**
     * {@inheritDoc}
     */
    public List<CacheLoader> getRegisteredCacheLoaders() {
        return decoratedCache.getRegisteredCacheLoaders();
    }

    /**
     * {@inheritDoc}
     */
    public CacheWriter getRegisteredCacheWriter() {
        return decoratedCache.getRegisteredCacheWriter();
    }

    /**
     * {@inheritDoc}
     */
    public SampledCacheStatistics getSampledCacheStatistics() {
        return decoratedCache.getSampledCacheStatistics();
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() throws IllegalStateException, CacheException {
        return decoratedCache.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        return decoratedCache.getSizeBasedOnAccuracy(statisticsAccuracy);
    }

    /**
     * {@inheritDoc}
     */
    public Statistics getStatistics() throws IllegalStateException {
        return decoratedCache.getStatistics();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatisticsAccuracy() {
        return decoratedCache.getStatisticsAccuracy();
    }

    /**
     * {@inheritDoc}
     */
    public Status getStatus() {
        return decoratedCache.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public CacheWriterManager getWriterManager() {
        return decoratedCache.getWriterManager();
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() {
        decoratedCache.initialise();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClusterCoherent() {
        return decoratedCache.isClusterCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDisabled() {
        return decoratedCache.isDisabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementInMemory(Object key) {
        return decoratedCache.isElementInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementInMemory(Serializable key) {
        return decoratedCache.isElementInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementOnDisk(Object key) {
        return decoratedCache.isElementOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementOnDisk(Serializable key) {
        return decoratedCache.isElementOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExpired(Element element) throws IllegalStateException, NullPointerException {
        return decoratedCache.isExpired(element);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isKeyInCache(Object key) {
        return decoratedCache.isKeyInCache(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNodeCoherent() {
        return decoratedCache.isNodeCoherent();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSampledStatisticsEnabled() {
        return decoratedCache.isSampledStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStatisticsEnabled() {
        return decoratedCache.isStatisticsEnabled();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValueInCache(Object value) {
        return decoratedCache.isValueInCache(value);
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheExtension(CacheExtension cacheExtension) {
        decoratedCache.registerCacheExtension(cacheExtension);
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException {
        decoratedCache.registerCacheUsageListener(cacheUsageListener);
    }

    /**
     * {@inheritDoc}
     */
    public void registerCacheWriter(CacheWriter cacheWriter) {
        decoratedCache.registerCacheWriter(cacheWriter);
    }

    /**
     * {@inheritDoc}
     */
    public void removeCacheUsageListener(CacheUsageListener cacheUsageListener) throws IllegalStateException {
        decoratedCache.removeCacheUsageListener(cacheUsageListener);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeQuiet(Object key) throws IllegalStateException {
        return decoratedCache.removeQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return decoratedCache.removeQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        return decoratedCache.removeWithWriter(key);
    }

    /**
     * {@inheritDoc}
     */
    public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) throws CacheException {
        decoratedCache.setBootstrapCacheLoader(bootstrapCacheLoader);
    }

    /**
     * {@inheritDoc}
     */
    public void setCacheExceptionHandler(CacheExceptionHandler cacheExceptionHandler) {
        decoratedCache.setCacheExceptionHandler(cacheExceptionHandler);
    }

    /**
     * {@inheritDoc}
     */
    public void setCacheManager(CacheManager cacheManager) {
        decoratedCache.setCacheManager(cacheManager);
    }

    /**
     * {@inheritDoc}
     */
    public void setDisabled(boolean disabled) {
        decoratedCache.setDisabled(disabled);
    }

    /**
     * {@inheritDoc}
     */
    public void setDiskStorePath(String diskStorePath) throws CacheException {
        decoratedCache.setDiskStorePath(diskStorePath);
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name) {
        decoratedCache.setName(name);
    }

    /**
     * {@inheritDoc}
     */
    public void setNodeCoherent(boolean coherent) throws UnsupportedOperationException {
        decoratedCache.setNodeCoherent(coherent);
    }

    /**
     * {@inheritDoc}
     */
    public void setSampledStatisticsEnabled(boolean enableStatistics) {
        decoratedCache.setSampledStatisticsEnabled(enableStatistics);
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsAccuracy(int statisticsAccuracy) {
        decoratedCache.setStatisticsAccuracy(statisticsAccuracy);
    }

    /**
     * {@inheritDoc}
     */
    public void setStatisticsEnabled(boolean enableStatistics) {
        decoratedCache.setStatisticsEnabled(enableStatistics);
    }

    /**
     * {@inheritDoc}
     */
    public void setTransactionManagerLookup(TransactionManagerLookup transactionManagerLookup) {
        decoratedCache.setTransactionManagerLookup(transactionManagerLookup);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterCacheExtension(CacheExtension cacheExtension) {
        decoratedCache.unregisterCacheExtension(cacheExtension);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterCacheWriter() {
        decoratedCache.unregisterCacheWriter();
    }

    /**
     * {@inheritDoc}
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        decoratedCache.waitUntilClusterCoherent();
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
        return decoratedCache.putIfAbsent(element);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeElement(Element element) throws NullPointerException {
        return decoratedCache.removeElement(element);
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        return decoratedCache.replace(old, element);
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        return decoratedCache.replace(element);
    }

}
