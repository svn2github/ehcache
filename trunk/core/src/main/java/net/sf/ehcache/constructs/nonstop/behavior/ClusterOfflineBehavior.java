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

package net.sf.ehcache.constructs.nonstop.behavior;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;
import net.sf.ehcache.constructs.nonstop.NonStopCacheConfig;

/**
 * Implementation of {@link NonStopCacheBehavior} which should be used when cluster is offline
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class ClusterOfflineBehavior implements NonStopCacheBehavior {

    private final NonStopCacheConfig nonStopCacheConfig;
    private final NonStopCacheBehaviorResolver timeoutBehaviorResolver;
    private final NonStopCacheBehavior executorBehavior;
    private final AtomicInteger pendingMutateBufferSize = new AtomicInteger();

    /**
     * Constructor accepting the {@link NonStopCacheConfig}, a {@link NonStopCacheBehaviorResolver} and {@link NonStopCacheBehavior} which
     * will be invoked if immediateTimeout is false
     * 
     * For every operation in {@link NonStopCacheBehavior}, if the {@link NonStopCacheConfig} is configured with immediateTimeout, the
     * behavior resolved from the {@link NonStopCacheBehaviorResolver} is invoked. Otherwise the executor service
     * {@link NonStopCacheBehavior} is invoked
     * 
     * @param nonStopCacheConfig
     * @param timeoutBehaviorResolver
     * @param executorBehavior
     */
    public ClusterOfflineBehavior(final NonStopCacheConfig nonStopCacheConfig, final NonStopCacheBehaviorResolver timeoutBehaviorResolver,
            final NonStopCacheBehavior executorBehavior) {
        this.nonStopCacheConfig = nonStopCacheConfig;
        this.timeoutBehaviorResolver = timeoutBehaviorResolver;
        this.executorBehavior = executorBehavior;
    }

    private boolean shouldTimeoutImmediately() {
        return nonStopCacheConfig.isImmediateTimeout();
    }

    /**
     * {@inheritDoc}
     */
    public Element get(final Object key) throws IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().get(key);
        } else {
            return executorBehavior.get(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove((Object) key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Serializable key) throws IllegalStateException {
        return remove((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().getQuiet(key);
        } else {
            return executorBehavior.getQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List getKeys() throws IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().getKeys();
        } else {
            return executorBehavior.getKeys();
        }
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().getKeysNoDuplicateCheck();
        } else {
            return executorBehavior.getKeysNoDuplicateCheck();
        }
    }

    /**
     * {@inheritDoc}
     */
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().getKeysWithExpiryCheck();
        } else {
            return executorBehavior.getKeysWithExpiryCheck();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isKeyInCache(final Object key) {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().isKeyInCache(key);
        } else {
            return executorBehavior.isKeyInCache(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValueInCache(final Object value) {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().isValueInCache(value);
        } else {
            return executorBehavior.isValueInCache(value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void put(final Element element, final boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            timeoutBehaviorResolver.resolveBehavior().put(element, doNotNotifyCacheReplicators);
        } else {
            executorBehavior.put(element, doNotNotifyCacheReplicators);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            timeoutBehaviorResolver.resolveBehavior().put(element);
        } else {
            executorBehavior.put(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            timeoutBehaviorResolver.resolveBehavior().putQuiet(element);
        } else {
            executorBehavior.putQuiet(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            timeoutBehaviorResolver.resolveBehavior().putWithWriter(element);
        } else {
            executorBehavior.putWithWriter(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Object key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().remove(key, doNotNotifyCacheReplicators);
        } else {
            return executorBehavior.remove(key, doNotNotifyCacheReplicators);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Object key) throws IllegalStateException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().remove(key);
        } else {
            return executorBehavior.remove(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll() throws IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            timeoutBehaviorResolver.resolveBehavior().removeAll();
        } else {
            executorBehavior.removeAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeAll(final boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            timeoutBehaviorResolver.resolveBehavior().removeAll(doNotNotifyCacheReplicators);
        } else {
            executorBehavior.removeAll(doNotNotifyCacheReplicators);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().calculateInMemorySize();
        } else {
            return executorBehavior.calculateInMemorySize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void evictExpiredElements() {
        if (shouldTimeoutImmediately()) {
            timeoutBehaviorResolver.resolveBehavior().evictExpiredElements();
        } else {
            executorBehavior.evictExpiredElements();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void flush() throws IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            timeoutBehaviorResolver.resolveBehavior().flush();
        } else {
            executorBehavior.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getDiskStoreSize() throws IllegalStateException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().getDiskStoreSize();
        } else {
            return executorBehavior.getDiskStoreSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getInternalContext() {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().getInternalContext();
        } else {
            return executorBehavior.getInternalContext();
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMemoryStoreSize() throws IllegalStateException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().getMemoryStoreSize();
        } else {
            return executorBehavior.getMemoryStoreSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() throws IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().getSize();
        } else {
            return executorBehavior.getSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getSizeBasedOnAccuracy(int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().getSizeBasedOnAccuracy(statisticsAccuracy);
        } else {
            return executorBehavior.getSizeBasedOnAccuracy(statisticsAccuracy);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Statistics getStatistics() throws IllegalStateException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().getStatistics();
        } else {
            return executorBehavior.getStatistics();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementInMemory(Object key) {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().isElementInMemory(key);
        } else {
            return executorBehavior.isElementInMemory(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementOnDisk(Object key) {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().isElementOnDisk(key);
        } else {
            return executorBehavior.isElementOnDisk(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    public Element putIfAbsent(Element element) throws NullPointerException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().putIfAbsent(element);
        } else {
            return executorBehavior.putIfAbsent(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeElement(Element element) throws NullPointerException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().removeElement(element);
        } else {
            return executorBehavior.removeElement(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeQuiet(Object key) throws IllegalStateException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().removeQuiet(key);
        } else {
            return executorBehavior.removeQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return removeQuiet((Object) key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeWithWriter(Object key) throws IllegalStateException, CacheException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().removeWithWriter(key);
        } else {
            return executorBehavior.removeWithWriter(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean replace(Element old, Element element) throws NullPointerException, IllegalArgumentException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().replace(old, element);
        } else {
            return executorBehavior.replace(old, element);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element replace(Element element) throws NullPointerException {
        if (shouldTimeoutImmediately()) {
            return timeoutBehaviorResolver.resolveBehavior().replace(element);
        } else {
            return executorBehavior.replace(element);
        }
    }

}
