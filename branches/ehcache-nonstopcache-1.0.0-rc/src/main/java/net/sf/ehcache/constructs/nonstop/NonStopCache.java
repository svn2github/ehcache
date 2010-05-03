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

/**
 * An Ehcache decorator that can be configured and used with timeouts. Mainly useful for decorating Terracotta clustered caches to avoid
 * blocking on cache operations forever when the cluster goes down. This can also be used with other features like write-behind, synchronous
 * writes etc to meet certain SLA's.
 * 
 * The {@link NonStopCache} can be configured with certain timeouts and desired actions when the timeout happens. Currently supported
 * behaviors are:
 * <ul>
 * <li>noop - gets return null. Mutating operations such as put and removes are ignored.</li>
 * <li>exception - An unchecked exception, {@link NonStopCacheException}, which is a subtype of CacheException will be thrown.</li>
 * <li>localRead - currently Terracotta only. Returns data if held locally in memory in response to gets. Mutating operations such as put
 * and removed are ignored.</li>
 * </ul>
 * 
 * NOTE: localRead behavior works only with Cache instances which are clustered using Terracotta. One obvious disadvantage is that it cannot
 * be used to decorate unclustered Cache's. Another not so obvious disadvantage is that localRead cannot be used when decorating other
 * already decorated Caches like UnlockedReadsView
 * 
 * The timeout feature uses a SEDA style approach which utilises an Executor thread pool. By default one {@link NonStopCache} is associated
 * with one Executor thread pool ({@link NonStopCacheExecutorService}). The default NonStopCacheExecutorService has 10 threads, allowing 10
 * concurrent cache operations. Different NonStopCache's can use the same Executor thread pool if desired. It can be achieved by using the
 * NonStopCache constructor {{@link #NonStopCache(Ehcache, String, NonStopCacheConfig, NonStopCacheExecutorService)} that accepts the
 * NonStopCacheExecutorService. You can specify your own thread pool size for each NonStopCacheExecutorService in its constructor that
 * accepts threadPoolSize. The thread pool is shut down when the associated NonStopCache (or all of them, if multiple NonStopCache uses the
 * same NonStopCacheExecutorService) is disposed.
 * 
 * The {@link NonStopCache} can also be configured with immediateTimeout=true, which means that if the cluster is already offline, it will
 * timeout immediately without waiting for the timeoutMillis value
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class NonStopCache extends EhcacheDecoratorAdapter 
        implements NonStopCacheConfig, NonStopCacheBehavior, NonStopCacheBehaviorResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonStopCache.class);

    static {
        // make sure non-stop cache overrides all methods of NonStopCacheBehavior (already implemented by EhcacheDecoratorAdapter)
        OverrideCheck.check(NonStopCacheBehavior.class, NonStopCache.class);
    }

    private final String name;
    private final NonStopCacheConfig nonStopCacheConfig;
    private final NonStopCacheExecutorService nonStopCacheExecutorService;
    private final ConcurrentMap<NonStopCacheBehaviorType, NonStopCacheBehavior> timeoutBehaviors;
    private final NonStopCacheBehavior executeWithExecutorBehavior;
    private final NonStopCacheBehavior clusterOfflineBehavior;
    private final CacheCluster cacheCluster;

    /**
     * Constructor that accepts the cache to be decorated and the name of this decorated cache. A {@link NonStopCache} will be created with
     * default config
     * 
     * @param underlyingCache
     *            the cache that needs to be decorated
     * @param name
     *            Name for this decorated NonStopCache
     */
    public NonStopCache(final Ehcache underlyingCache, final String name) {
        this(underlyingCache, name, new NonStopCacheConfigImpl());
    }

    /**
     * Constructor that accepts the cache to be decorated, name and properties map containing config. See {@link NonStopCacheConfig} for
     * which keys and values to use in the {@link Properties}
     * 
     * @param underlyingCache
     *            the cache that needs to be decorated
     * @param name
     *            name of the decorated NonStopCache
     * @param configProperties
     *            {@link Properties} containing the NonStopCache config
     */
    public NonStopCache(final Ehcache underlyingCache, final String name, final Properties configProperties) {
        this(underlyingCache, name, new NonStopCacheConfigImpl(configProperties));
    }

    /**
     * Constructor that accepts the cache to be decorated, name and {@link NonStopCacheConfig}.
     * 
     * @param underlyingCache
     *            the cache that needs to be decorated
     * @param name
     *            name of the decorated NonStopCache
     * @param nonStopCacheConfig
     *            NonStopCache configuration
     */
    public NonStopCache(final Ehcache underlyingCache, final String name, final NonStopCacheConfig nonStopCacheConfig) {
        this(underlyingCache, name, nonStopCacheConfig, new NonStopCacheExecutorService(new ThreadFactory() {

            private final AtomicInteger count = new AtomicInteger();

            public Thread newThread(final Runnable runnable) {
                return new Thread(runnable, "NonStopCache [" + underlyingCache.getName() + "] Executor Thread-" + count.incrementAndGet());
            }
        }));
    }

    /**
     * Constructor that accepts the underlying cache, name, config and a {@link NonStopCacheExecutorService}. This constructor is useful to
     * make NonStopCache use the same thread pool
     * 
     * @param underlyingCache
     * @param name
     * @param nonStopCacheConfig
     * @param nonStopCacheExecutorService
     */
    public NonStopCache(final Ehcache underlyingCache, final String name, final NonStopCacheConfig nonStopCacheConfig,
            final NonStopCacheExecutorService nonStopCacheExecutorService) {
        super(underlyingCache);
        this.name = name;
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

    /**
     * returns the {@link NonStopCacheConfig} associated with this {@link NonStopCache}
     * 
     * @return the {@link NonStopCacheConfig} associated with this {@link NonStopCache}
     */
    public NonStopCacheConfig getNonStopCacheConfig() {
        return nonStopCacheConfig;
    }

    /**
     * Returns the {@link NonStopCacheExecutorService} associated with this {@link NonStopCache}
     * 
     * @return the {@link NonStopCacheExecutorService} associated with this {@link NonStopCache}
     */
    public NonStopCacheExecutorService getNonStopCacheExecutorService() {
        return nonStopCacheExecutorService;
    }

    /**
     * Method implementing {@link NonStopCacheBehaviorResolver#resolveBehavior()}. This will return the timeoutBehavior configured for this
     * {@link NonStopCache}. The timeoutBehavior associated with this NonStopCache can be changed directly using
     * {@link #setTimeoutBehaviorType(NonStopCacheBehaviorType)}.
     */
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

    /**
     * Returns the name for this NonStopCache
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element get(final Object key) throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.get(key);
        }
        return executeWithExecutorBehavior.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element get(final Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getQuiet(key);
        }
        return executeWithExecutorBehavior.getQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getQuiet(final Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List getKeys() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getKeys();
        }
        return executeWithExecutorBehavior.getKeys();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getKeysNoDuplicateCheck();
        }
        return executeWithExecutorBehavior.getKeysNoDuplicateCheck();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getKeysWithExpiryCheck();
        }
        return executeWithExecutorBehavior.getKeysWithExpiryCheck();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Element element, final boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.put(element, doNotNotifyCacheReplicators);
            return;
        }
        executeWithExecutorBehavior.put(element, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.put(element);
            return;
        }
        executeWithExecutorBehavior.put(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.putQuiet(element);
            return;
        }
        executeWithExecutorBehavior.putQuiet(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.putWithWriter(element);
            return;
        }
        executeWithExecutorBehavior.putWithWriter(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final Object key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.remove(key, doNotNotifyCacheReplicators);
        }
        return executeWithExecutorBehavior.remove(key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final Object key) throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.remove(key);
        }
        return executeWithExecutorBehavior.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final Serializable key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return this.remove((Object) key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final Serializable key) throws IllegalStateException {
        return this.remove((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.removeAll();
            return;
        }
        executeWithExecutorBehavior.removeAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll(final boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.removeAll(doNotNotifyCacheReplicators);
            return;
        }
        executeWithExecutorBehavior.removeAll(doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isKeyInCache(final Object key) {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.isKeyInCache(key);
        }
        return executeWithExecutorBehavior.isKeyInCache(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValueInCache(final Object value) {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.isValueInCache(value);
        }
        return executeWithExecutorBehavior.isValueInCache(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.calculateInMemorySize();
        }
        return executeWithExecutorBehavior.calculateInMemorySize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evictExpiredElements() {
        if (isClusterOffline()) {
            clusterOfflineBehavior.evictExpiredElements();
        }
        executeWithExecutorBehavior.evictExpiredElements();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            clusterOfflineBehavior.flush();
        }
        executeWithExecutorBehavior.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDiskStoreSize() throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getDiskStoreSize();
        }
        return executeWithExecutorBehavior.getDiskStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getInternalContext() {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getInternalContext();

        }
        return executeWithExecutorBehavior.getInternalContext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMemoryStoreSize() throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getMemoryStoreSize();
        }
        return executeWithExecutorBehavior.getMemoryStoreSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getSize();
        }
        return executeWithExecutorBehavior.getSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getSizeBasedOnAccuracy(statisticsAccuracy);
        }
        return executeWithExecutorBehavior.getSizeBasedOnAccuracy(statisticsAccuracy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statistics getStatistics() throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.getStatistics();
        }
        return executeWithExecutorBehavior.getStatistics();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isElementInMemory(Object key) {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.isElementInMemory(key);
        }
        return executeWithExecutorBehavior.isElementInMemory(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isElementOnDisk(Object key) {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.isElementOnDisk(key);
        }
        return executeWithExecutorBehavior.isElementOnDisk(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element putIfAbsent(Element element) throws NullPointerException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.putIfAbsent(element);
        }
        return executeWithExecutorBehavior.putIfAbsent(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeElement(Element element) throws NullPointerException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.removeElement(element);
        }
        return executeWithExecutorBehavior.removeElement(element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeQuiet(Object key) throws IllegalStateException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.removeQuiet(key);
        }
        return executeWithExecutorBehavior.removeQuiet(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return removeQuiet((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.removeWithWriter(key);
        }
        return executeWithExecutorBehavior.removeWithWriter(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.replace(old, element);
        }
        return executeWithExecutorBehavior.replace(old, element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element replace(Element element) throws NullPointerException {
        if (isClusterOffline()) {
            return clusterOfflineBehavior.replace(element);
        }
        return executeWithExecutorBehavior.replace(element);
    }

    /**
     * Returns the configured timeout value in millis. This can be changed dynamically
     */
    public long getTimeoutMillis() {
        return this.nonStopCacheConfig.getTimeoutMillis();
    }

    /**
     * Set the timeout value in millis
     */
    public void setTimeoutMillis(final long timeoutMillis) {
        this.nonStopCacheConfig.setTimeoutMillis(timeoutMillis);
    }

    /**
     * Returns whether this {@link NonStopCache} is configured or not
     */
    public boolean isImmediateTimeout() {
        return nonStopCacheConfig.isImmediateTimeout();
    }

    /**
     * Set the immediateTimeout property for this {@link NonStopCache}
     */
    public void setImmediateTimeout(final boolean immediateTimeout) {
        this.nonStopCacheConfig.setImmediateTimeout(immediateTimeout);
    }

    /**
     * Returns the configured timeout behavior type
     */
    public NonStopCacheBehaviorType getTimeoutBehaviorType() {
        return this.nonStopCacheConfig.getTimeoutBehaviorType();
    }

    /**
     * Set the timeout behavior type
     */
    public void setTimeoutBehaviorType(final NonStopCacheBehaviorType timeoutBehaviorType) {
        this.nonStopCacheConfig.setTimeoutBehaviorType(timeoutBehaviorType);
    }

}
