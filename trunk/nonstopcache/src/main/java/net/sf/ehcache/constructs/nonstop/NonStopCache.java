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

package net.sf.ehcache.constructs.nonstop;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.cluster.ClusterSchemeNotAvailableException;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;
import net.sf.ehcache.constructs.nonstop.behavior.ClusterOfflineBehavior;
import net.sf.ehcache.constructs.nonstop.behavior.DirectDelegateBehavior;
import net.sf.ehcache.constructs.nonstop.behavior.ExecutorBehavior;
import net.sf.ehcache.constructs.nonstop.behavior.NonStopCacheBehaviorResolver;
import net.sf.ehcache.constructs.nonstop.util.OverrideCheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NonStopCache extends EhcacheDecoratorAdapter implements NonStopCacheConfig, NonStopCacheBehavior, NonStopCacheBehaviorResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStopCache.class);

    static {
        // make sure non-stop cache overrides all methods of NonStopCacheBehavior (already implemented by EhcacheDecoratorAdapter)
        OverrideCheck.check(NonStopCacheBehavior.class, NonStopCache.class);
    }

    private final NonStopCacheConfig nonStopCacheConfig;
    private final NonStopCacheExecutorService nonStopCacheExecutorService;
    private final ConcurrentMap<NonStopCacheBehaviorType, NonStopCacheBehavior> timeoutBehaviors;
    private final NonStopCacheBehavior executeWithExecutorBehavior;
    private final NonStopCacheBehavior clusterOfflineBehavior;
    private final CacheCluster cacheCluster;

    /**
     * Constructor that accepts the cache to be decorated. A {@link NonStopCache} will be created with default config
     * 
     * @param underlyingCache
     *            the cache that needs to be decorated
     */
    public NonStopCache(final Ehcache underlyingCache) {
        this(underlyingCache, new NonStopCacheConfigImpl());
    }

    /**
     * Constructor that accepts the cache to be decorated and properties map containing config. See {@link NonStopCacheConfig} for which
     * keys and values to use in the {@link Properties}
     * 
     * @param underlyingCache
     * @param configProperties
     */
    public NonStopCache(final Ehcache underlyingCache, final Properties configProperties) {
        this(underlyingCache, new NonStopCacheConfigImpl(configProperties));
    }

    public NonStopCache(final Ehcache underlyingCache, final NonStopCacheConfig nonStopCacheConfig) {
        this(underlyingCache, nonStopCacheConfig, new NonStopCacheExecutorService(new ThreadFactory() {

            private final AtomicInteger count = new AtomicInteger();

            public Thread newThread(final Runnable runnable) {
                return new Thread(runnable, "NonStopCache [" + underlyingCache.getName() + "] Executor Thread-" + count.incrementAndGet());
            }
        }));
    }

    public NonStopCache(final Ehcache underlyingCache, final NonStopCacheConfig nonStopCacheConfig,
            final NonStopCacheExecutorService nonStopCacheExecutorService) {
        super(underlyingCache);
        this.nonStopCacheConfig = nonStopCacheConfig;
        this.nonStopCacheExecutorService = nonStopCacheExecutorService;
        this.timeoutBehaviors = new ConcurrentHashMap<NonStopCacheBehaviorType, NonStopCacheBehavior>();
        this.executeWithExecutorBehavior = new ExecutorBehavior(new DirectDelegateBehavior(underlyingCache), nonStopCacheConfig,
                nonStopCacheExecutorService, this);
        this.clusterOfflineBehavior = new ClusterOfflineBehavior(nonStopCacheConfig, this, executeWithExecutorBehavior);
        this.nonStopCacheExecutorService.attachCache(this);
        CacheCluster cluster;
        try {
            cluster = underlyingCache.getCacheManager().getCluster(ClusterScheme.TERRACOTTA);
        } catch (ClusterSchemeNotAvailableException e) {
            LOGGER.info("Terracotta ClusterScheme is not available, using ClusterScheme.NONE");
            cluster = underlyingCache.getCacheManager().getCluster(ClusterScheme.NONE);
        }
        this.cacheCluster = cluster;
    }

    public NonStopCacheConfig getNonStopCacheConfig() {
        return nonStopCacheConfig;
    }

    public NonStopCacheExecutorService getNonStopCacheExecutorService() {
        return nonStopCacheExecutorService;
    }

    public NonStopCacheBehavior resolveBehavior() {
        NonStopCacheBehavior behavior = timeoutBehaviors.get(nonStopCacheConfig.getTimeoutBehaviorType());
        if (behavior == null) {
            behavior = nonStopCacheConfig.getTimeoutBehaviorType().newCacheBehavior(underlyingCache);
            NonStopCacheBehavior prev = timeoutBehaviors.putIfAbsent(nonStopCacheConfig.getTimeoutBehaviorType(), behavior);
            if (prev != null) {
                behavior = prev;
            }
        }
        return behavior;
    }

    private boolean isClusterOffline() {
        return !cacheCluster.isClusterOnline();
    }

    @Override
    public Element get(final Object key) throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.get(key);
        }
        return executeWithExecutorBehavior.get(key);
    }

    @Override
    public Element get(final Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    @Override
    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getQuiet(key);
        }
        return executeWithExecutorBehavior.getQuiet(key);
    }

    @Override
    public Element getQuiet(final Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    @Override
    public List getKeys() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getKeys();
        }
        return executeWithExecutorBehavior.getKeys();
    }

    @Override
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getKeysNoDuplicateCheck();
        }
        return executeWithExecutorBehavior.getKeysNoDuplicateCheck();
    }

    @Override
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getKeysWithExpiryCheck();
        }
        return executeWithExecutorBehavior.getKeysWithExpiryCheck();
    }

    @Override
    public void put(final Element element, final boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.put(element, doNotNotifyCacheReplicators);
            return;
        }
        executeWithExecutorBehavior.put(element, doNotNotifyCacheReplicators);
    }

    @Override
    public void put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.put(element);
            return;
        }
        executeWithExecutorBehavior.put(element);
    }

    @Override
    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.putQuiet(element);
            return;
        }
        executeWithExecutorBehavior.putQuiet(element);
    }

    @Override
    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.putWithWriter(element);
            return;
        }
        executeWithExecutorBehavior.putWithWriter(element);
    }

    @Override
    public boolean remove(final Object key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.remove(key, doNotNotifyCacheReplicators);
        }
        return executeWithExecutorBehavior.remove(key, doNotNotifyCacheReplicators);
    }

    @Override
    public boolean remove(final Object key) throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.remove(key);
        }
        return executeWithExecutorBehavior.remove(key);
    }

    @Override
    public boolean remove(final Serializable key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return this.remove((Object) key, doNotNotifyCacheReplicators);
    }

    @Override
    public boolean remove(final Serializable key) throws IllegalStateException {
        return this.remove((Object) key);
    }

    @Override
    public void removeAll() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.removeAll();
            return;
        }
        executeWithExecutorBehavior.removeAll();
    }

    @Override
    public void removeAll(final boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.removeAll(doNotNotifyCacheReplicators);
            return;
        }
        executeWithExecutorBehavior.removeAll(doNotNotifyCacheReplicators);
    }

    @Override
    public boolean isKeyInCache(final Object key) {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.isKeyInCache(key);
        }
        return executeWithExecutorBehavior.isKeyInCache(key);
    }

    @Override
    public boolean isValueInCache(final Object value) {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.isValueInCache(value);
        }
        return executeWithExecutorBehavior.isValueInCache(value);
    }

    @Override
    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.calculateInMemorySize();
        }
        return executeWithExecutorBehavior.calculateInMemorySize();
    }

    @Override
    public void evictExpiredElements() {
        if (isClusterOffline()) {
            clusterOfflineBehavior.evictExpiredElements();
        }
        executeWithExecutorBehavior.evictExpiredElements();
    }

    @Override
    public void flush() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.flush();
        }
        executeWithExecutorBehavior.flush();
    }

    @Override
    public int getDiskStoreSize() throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getDiskStoreSize();
        }
        return executeWithExecutorBehavior.getDiskStoreSize();
    }

    @Override
    public Object getInternalContext() {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getInternalContext();

        }
        return executeWithExecutorBehavior.getInternalContext();
    }

    @Override
    public long getMemoryStoreSize() throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getMemoryStoreSize();
        }
        return executeWithExecutorBehavior.getMemoryStoreSize();
    }

    @Override
    public int getSize() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getSize();
        }
        return executeWithExecutorBehavior.getSize();
    }

    @Override
    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getSizeBasedOnAccuracy(statisticsAccuracy);
        }
        return executeWithExecutorBehavior.getSizeBasedOnAccuracy(statisticsAccuracy);
    }

    @Override
    public Statistics getStatistics() throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getStatistics();
        }
        return executeWithExecutorBehavior.getStatistics();
    }

    @Override
    public boolean isElementInMemory(Object key) {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.isElementInMemory(key);
        }
        return executeWithExecutorBehavior.isElementInMemory(key);
    }

    @Override
    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    @Override
    public boolean isElementOnDisk(Object key) {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.isElementOnDisk(key);
        }
        return executeWithExecutorBehavior.isElementOnDisk(key);
    }

    @Override
    public boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    @Override
    public Element putIfAbsent(Element element) throws NullPointerException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.putIfAbsent(element);
        }
        return executeWithExecutorBehavior.putIfAbsent(element);
    }

    @Override
    public boolean removeElement(Element element) throws NullPointerException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.removeElement(element);
        }
        return executeWithExecutorBehavior.removeElement(element);
    }

    @Override
    public boolean removeQuiet(Object key) throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.removeQuiet(key);
        }
        return executeWithExecutorBehavior.removeQuiet(key);
    }

    @Override
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return removeQuiet((Object) key);
    }

    @Override
    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.removeWithWriter(key);
        }
        return executeWithExecutorBehavior.removeWithWriter(key);
    }

    @Override
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.replace(old, element);
        }
        return executeWithExecutorBehavior.replace(old, element);
    }

    @Override
    public Element replace(Element element) throws NullPointerException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.replace(element);
        }
        return executeWithExecutorBehavior.replace(element);
    }

    public long getTimeoutMillis() {
        return this.nonStopCacheConfig.getTimeoutMillis();
    }

    public void setTimeoutMillis(final long timeoutMillis) {
        this.nonStopCacheConfig.setTimeoutMillis(timeoutMillis);
    }

    public boolean isImmediateTimeout() {
        return nonStopCacheConfig.isImmediateTimeout();
    }

    public void setImmediateTimeout(final boolean immediateTimeout) {
        this.nonStopCacheConfig.setImmediateTimeout(immediateTimeout);
    }

    public NonStopCacheBehaviorType getTimeoutBehaviorType() {
        return this.nonStopCacheConfig.getTimeoutBehaviorType();
    }

    public void setTimeoutBehaviorType(final NonStopCacheBehaviorType timeoutBehaviorType) {
        this.nonStopCacheConfig.setTimeoutBehaviorType(timeoutBehaviorType);
    }

}
