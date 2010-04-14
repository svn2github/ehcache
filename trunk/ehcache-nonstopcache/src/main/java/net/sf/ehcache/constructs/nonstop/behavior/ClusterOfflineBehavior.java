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
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;
import net.sf.ehcache.constructs.nonstop.NonStopCacheConfig;

public class ClusterOfflineBehavior implements NonStopCacheBehavior {

    private final NonStopCacheConfig nonStopCacheConfig;
    private final NonStopCacheBehaviorResolver timeoutBehaviorResolver;
    private final NonStopCacheBehavior executorBehavior;
    private final AtomicInteger pendingMutateBufferSize = new AtomicInteger();

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

    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return remove((Object) key, doNotNotifyCacheReplicators);
    }

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

}
