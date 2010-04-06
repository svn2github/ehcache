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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.EhcacheDecoratorAdapter;
import net.sf.ehcache.constructs.nonstop.behavior.DelegatingNonStopCacheBehavior;
import net.sf.ehcache.constructs.nonstop.behavior.DelegatingNonStopCacheBehavior.DelegateHolder;
import net.sf.ehcache.loader.CacheLoader;

public class NonStopCache extends EhcacheDecoratorAdapter implements NonStopCacheConfig {

    private final NonStopCacheConfig timeoutCacheConfig;
    private final NonStopCacheExecutorService executorService;
    private final ConcurrentMap<NonStopCacheBehaviorType, NonStopCacheBehavior> timeoutBehaviors;
    private final NonStopCacheBehavior failFastBehavior;

    /**
     * Constructor that accepts only the cache to be decorated.
     * TimeoutCacheConfig is created using defaults
     * 
     * @param decoratedCache
     */
    public NonStopCache(Ehcache decoratedCache) {
        this(decoratedCache, new NonStopCacheConfigImpl());
    }

    /**
     * Constructor that accepts the cache to be decorated and properties map which is used to create a TimeoutCacheConfig
     * 
     * @param decoratedCache
     * @param configProperties
     */
    public NonStopCache(Ehcache decoratedCache, Properties configProperties) {
        this(decoratedCache, new NonStopCacheConfigImpl(configProperties));
    }

    public NonStopCache(Ehcache decoratedCache, NonStopCacheConfig timeoutCacheConfig) {
        super(decoratedCache);
        this.timeoutCacheConfig = timeoutCacheConfig;
        this.executorService = new NonStopCacheExecutorService(decoratedCache, timeoutCacheConfig);
        this.timeoutBehaviors = new ConcurrentHashMap<NonStopCacheBehaviorType, NonStopCacheBehavior>();
        this.failFastBehavior = new DelegatingNonStopCacheBehavior(new DelegateHolder() {

            public NonStopCacheBehavior getDelegate() {
                return getTimeoutCacheBehavior();
            }
        });
    }

    public NonStopCacheConfig getTimeoutCacheConfig() {
        return timeoutCacheConfig;
    }

    private NonStopCacheBehavior getTimeoutCacheBehavior() {
        NonStopCacheBehavior behavior = timeoutBehaviors.get(timeoutCacheConfig.getTimeoutBehaviorType());
        if (behavior == null) {
            behavior = timeoutCacheConfig.getTimeoutBehaviorType().newCacheBehavior(decoratedCache);
            NonStopCacheBehavior prev = timeoutBehaviors.putIfAbsent(timeoutCacheConfig.getTimeoutBehaviorType(), behavior);
            if (prev != null) {
                behavior = prev;
            }
        }
        return behavior;
    }

    private boolean failFast() {
        // TODO: add support for cluster events
        return timeoutCacheConfig.isFailFast(); // && !cacheCluster.isNodeConnected();
    }

    @Override
    public Element get(final Object key) throws IllegalStateException, CacheException {
        if (failFast()) {
            return failFastBehavior.get(key);
        }
        Element element = null;
        try {
            element = executorService.execute(new Callable<Element>() {
                public Element call() throws Exception {
                    return NonStopCache.this.decoratedCache.get(key);
                }
            });
        } catch (TimeoutException e) {
            return getTimeoutCacheBehavior().get(key);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
        return element;
    }

    @Override
    public Element get(Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    @Override
    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        if (failFast()) {
            return failFastBehavior.getQuiet(key);
        }
        Element element = null;
        try {
            element = executorService.execute(new Callable<Element>() {
                public Element call() throws Exception {
                    return NonStopCache.this.decoratedCache.getQuiet(key);
                }
            });
        } catch (TimeoutException e) {
            return getTimeoutCacheBehavior().getQuiet(key);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
        return element;
    }

    @Override
    public Element getQuiet(Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    @Override
    public List getKeys() throws IllegalStateException, CacheException {
        if (failFast()) {
            return failFastBehavior.getKeys();
        }
        List keys = null;
        try {
            keys = executorService.execute(new Callable<List>() {
                public List call() throws Exception {
                    return NonStopCache.this.decoratedCache.getKeys();
                }
            });
        } catch (TimeoutException e) {
            return getTimeoutCacheBehavior().getKeys();
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
        return keys;
    }

    @Override
    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        if (failFast()) {
            return failFastBehavior.getKeysNoDuplicateCheck();
        }
        List keys = null;
        try {
            keys = executorService.execute(new Callable<List>() {
                public List call() throws Exception {
                    return NonStopCache.this.decoratedCache.getKeysNoDuplicateCheck();
                }
            });
        } catch (TimeoutException e) {
            return getTimeoutCacheBehavior().getKeysNoDuplicateCheck();
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
        return keys;
    }

    @Override
    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        if (failFast()) {
            return failFastBehavior.getKeysWithExpiryCheck();
        }
        List keys = null;
        try {
            keys = executorService.execute(new Callable<List>() {
                public List call() throws Exception {
                    return NonStopCache.this.decoratedCache.getKeysWithExpiryCheck();
                }
            });
        } catch (TimeoutException e) {
            return getTimeoutCacheBehavior().getKeysWithExpiryCheck();
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
        return keys;
    }

    @Override
    public void put(final Element element, final boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        if (failFast()) {
            failFastBehavior.put(element, doNotNotifyCacheReplicators);
            return;
        }
        try {
            executorService.execute(new Callable<Void>() {
                public Void call() throws Exception {
                    NonStopCache.this.decoratedCache.put(element, doNotNotifyCacheReplicators);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            getTimeoutCacheBehavior().put(element, doNotNotifyCacheReplicators);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
    }

    @Override
    public void put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (failFast()) {
            failFastBehavior.put(element);
            return;
        }
        try {
            executorService.execute(new Callable<Void>() {
                public Void call() throws Exception {
                    NonStopCache.this.decoratedCache.put(element);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            getTimeoutCacheBehavior().put(element);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
    }

    @Override
    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (failFast()) {
            failFastBehavior.putQuiet(element);
            return;
        }
        try {
            executorService.execute(new Callable<Void>() {
                public Void call() throws Exception {
                    NonStopCache.this.decoratedCache.putQuiet(element);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            getTimeoutCacheBehavior().putQuiet(element);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
    }

    @Override
    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        if (failFast()) {
            failFastBehavior.putWithWriter(element);
            return;
        }
        try {
            executorService.execute(new Callable<Void>() {
                public Void call() throws Exception {
                    NonStopCache.this.decoratedCache.putWithWriter(element);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            getTimeoutCacheBehavior().putWithWriter(element);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
    }

    @Override
    public boolean remove(final Object key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        if (failFast()) {
            return failFastBehavior.remove(key, doNotNotifyCacheReplicators);
        }
        boolean result = false;
        try {
            result = executorService.execute(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return Boolean.valueOf(NonStopCache.this.decoratedCache.remove(key, doNotNotifyCacheReplicators));
                }
            });
        } catch (TimeoutException e) {
            return getTimeoutCacheBehavior().remove(key, doNotNotifyCacheReplicators);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
        return result;
    }

    @Override
    public boolean remove(final Object key) throws IllegalStateException {
        if (failFast()) {
            return failFastBehavior.remove(key);
        }
        boolean result = false;
        try {
            result = executorService.execute(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return Boolean.valueOf(NonStopCache.this.decoratedCache.remove(key));
                }
            });
        } catch (TimeoutException e) {
            return getTimeoutCacheBehavior().remove(key);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
        return result;
    }

    @Override
    public boolean remove(Serializable key, boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return this.remove((Object) key, doNotNotifyCacheReplicators);
    }

    @Override
    public boolean remove(Serializable key) throws IllegalStateException {
        return this.remove((Object) key);
    }

    @Override
    public void removeAll() throws IllegalStateException, CacheException {
        if (failFast()) {
            failFastBehavior.removeAll();
            return;
        }
        try {
            executorService.execute(new Callable<Void>() {
                public Void call() throws Exception {
                    NonStopCache.this.decoratedCache.removeAll();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            getTimeoutCacheBehavior().removeAll();
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
    }

    @Override
    public void removeAll(final boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        if (failFast()) {
            failFastBehavior.removeAll(doNotNotifyCacheReplicators);
            return;
        }
        try {
            executorService.execute(new Callable<Void>() {
                public Void call() throws Exception {
                    NonStopCache.this.decoratedCache.removeAll(doNotNotifyCacheReplicators);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            getTimeoutCacheBehavior().removeAll(doNotNotifyCacheReplicators);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
    }

    @Override
    public Element getWithLoader(Object key, CacheLoader loader, Object loaderArgument) throws CacheException {
        throw new CacheException("This method is not appropriate for a Timeout Cache");
    }

    @Override
    public Map getAllWithLoader(Collection keys, Object loaderArgument) throws CacheException {
        throw new CacheException("This method is not appropriate for a Timeout Cache");
    }

    @Override
    public void registerCacheLoader(CacheLoader cacheLoader) {
        throw new CacheException("This method is not appropriate for a Timeout Cache");
    }

    @Override
    public void unregisterCacheLoader(CacheLoader cacheLoader) {
        throw new CacheException("This method is not appropriate for a Timeout Cache");
    }

    @Override
    public void load(Object key) throws CacheException {
        throw new CacheException("This method is not appropriate for a Timeout Cache");
    }

    @Override
    public void loadAll(Collection keys, Object argument) throws CacheException {
        throw new CacheException("This method is not appropriate for a Timeout Cache");
    }

    public long getTimeoutValueInMillis() {
        return this.timeoutCacheConfig.getTimeoutValueInMillis();
    }

    public void setTimeoutValueInMillis(long timeoutValueInMillis) {
        this.timeoutCacheConfig.setTimeoutValueInMillis(timeoutValueInMillis);
    }

    public boolean isFailFast() {
        return timeoutCacheConfig.isFailFast();
    }

    public void setFailFast(boolean failFast) {
        this.timeoutCacheConfig.setFailFast(failFast);
    }

    public NonStopCacheBehaviorType getTimeoutBehaviorType() {
        return this.timeoutCacheConfig.getTimeoutBehaviorType();
    }

    public void setTimeoutBehaviorType(NonStopCacheBehaviorType timeoutBehaviorType) {
        this.timeoutCacheConfig.setTimeoutBehaviorType(timeoutBehaviorType);
    }

}
