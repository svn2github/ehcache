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
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterScheme;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;
import net.sf.ehcache.constructs.nonstop.behavior.ClusterOfflineBehavior;
import net.sf.ehcache.constructs.nonstop.behavior.DirectDelegateBehavior;
import net.sf.ehcache.constructs.nonstop.behavior.ExecutorBehavior;
import net.sf.ehcache.constructs.nonstop.behavior.NonStopCacheBehaviorResolver;
import net.sf.ehcache.constructs.nonstop.util.OverrideCheck;

public class NonStopCache extends EhcacheDecoratorAdapter implements NonStopCacheConfig, NonStopCacheBehaviorResolver {

    static {
        // make sure non-stop cache overrides all methods of NonStopCacheBehavior
        OverrideCheck.check(NonStopCacheBehavior.class, NonStopCache.class);
    }

    private final NonStopCacheConfig nonStopCacheConfig;
    private final NonStopCacheExecutorService nonStopCacheExecutorService;
    private final ConcurrentMap<NonStopCacheBehaviorType, NonStopCacheBehavior> timeoutBehaviors;
    private final NonStopCacheBehavior executeWithExecutorBehavior;
    private final NonStopCacheBehavior clusterOfflineBehavior;
    private final CacheCluster cacheCluster;
    private final AtomicInteger pendingMutateOperationsCount = new AtomicInteger();

    /**
     * Constructor that accepts the cache to be decorated. A {@link NonStopCache} will be created with default config
     * 
     * @param decoratedCache
     *            the cache that needs to be decorated
     */
    public NonStopCache(final Ehcache decoratedCache) {
        this(decoratedCache, new NonStopCacheConfigImpl());
    }

    /**
     * Constructor that accepts the cache to be decorated and properties map containing config. See {@link NonStopCacheConfig} for which
     * keys and values to use in the {@link Properties}
     * 
     * @param decoratedCache
     * @param configProperties
     */
    public NonStopCache(final Ehcache decoratedCache, final Properties configProperties) {
        this(decoratedCache, new NonStopCacheConfigImpl(configProperties));
    }

    public NonStopCache(final Ehcache decoratedCache, final NonStopCacheConfig nonStopCacheConfig) {
        this(decoratedCache, nonStopCacheConfig, new NonStopCacheExecutorService(new ThreadFactory() {

            private final AtomicInteger count = new AtomicInteger();

            public Thread newThread(final Runnable runnable) {
                return new Thread(runnable, "NonStopCache [" + decoratedCache.getName() + "] Executor Thread-" + count.incrementAndGet());
            }
        }));
    }

    public NonStopCache(final Ehcache decoratedCache, final NonStopCacheConfig nonStopCacheConfig,
            final NonStopCacheExecutorService nonStopCacheExecutorService) {
        super(decoratedCache);
        this.nonStopCacheConfig = nonStopCacheConfig;
        this.nonStopCacheExecutorService = nonStopCacheExecutorService;
        this.cacheCluster = decoratedCache.getCacheManager().getCluster(ClusterScheme.TERRACOTTA);
        this.timeoutBehaviors = new ConcurrentHashMap<NonStopCacheBehaviorType, NonStopCacheBehavior>();
        this.executeWithExecutorBehavior = new ExecutorBehavior(new DirectDelegateBehavior(decoratedCache), nonStopCacheConfig,
                nonStopCacheExecutorService, this);
        this.clusterOfflineBehavior = new ClusterOfflineBehavior(nonStopCacheConfig, this, executeWithExecutorBehavior);
        this.nonStopCacheExecutorService.attachCache(this);
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
            behavior = nonStopCacheConfig.getTimeoutBehaviorType().newCacheBehavior(decoratedCache);
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

    public long getTimeoutValueInMillis() {
        return this.nonStopCacheConfig.getTimeoutValueInMillis();
    }

    public void setTimeoutValueInMillis(final long timeoutValueInMillis) {
        this.nonStopCacheConfig.setTimeoutValueInMillis(timeoutValueInMillis);
    }

    public boolean isImmediateTimeoutEnabled() {
        return nonStopCacheConfig.isImmediateTimeoutEnabled();
    }

    public void setImmediateTimeoutEnabled(final boolean immediateTimeoutEnabled) {
        this.nonStopCacheConfig.setImmediateTimeoutEnabled(immediateTimeoutEnabled);
    }

    public NonStopCacheBehaviorType getTimeoutBehaviorType() {
        return this.nonStopCacheConfig.getTimeoutBehaviorType();
    }

    public void setTimeoutBehaviorType(final NonStopCacheBehaviorType timeoutBehaviorType) {
        this.nonStopCacheConfig.setTimeoutBehaviorType(timeoutBehaviorType);
    }

}
