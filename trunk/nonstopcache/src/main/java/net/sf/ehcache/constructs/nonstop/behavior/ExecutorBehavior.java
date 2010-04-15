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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Statistics;
import net.sf.ehcache.constructs.nonstop.NonStopCacheBehavior;
import net.sf.ehcache.constructs.nonstop.NonStopCacheConfig;
import net.sf.ehcache.constructs.nonstop.NonStopCacheExecutorService;

public class ExecutorBehavior implements NonStopCacheBehavior {

    private final NonStopCacheBehavior executeBehavior;
    private final NonStopCacheExecutorService executorService;
    private final NonStopCacheBehaviorResolver timeoutBehaviorResolver;
    private final NonStopCacheConfig nonStopCacheConfig;

    public ExecutorBehavior(final NonStopCacheBehavior executeBehavior, final NonStopCacheConfig nonStopCacheConfig,
            final NonStopCacheExecutorService executorService, final NonStopCacheBehaviorResolver timeoutBehaviorResolver) {
        this.executeBehavior = executeBehavior;
        this.nonStopCacheConfig = nonStopCacheConfig;
        this.executorService = executorService;
        this.timeoutBehaviorResolver = timeoutBehaviorResolver;
    }

    private <V> V executeWithExecutor(final Callable<V> callable) throws CacheException, TimeoutException {
        try {
            return executorService.execute(callable, nonStopCacheConfig.getTimeoutMillis());
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
    }

    public Element get(final Object key) throws IllegalStateException, CacheException {
        Element element = null;
        try {
            element = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return executeBehavior.get(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().get(key);
        }
        return element;
    }

    public Element get(final Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    public Element getQuiet(final Object key) throws IllegalStateException, CacheException {
        Element element = null;
        try {
            element = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return executeBehavior.getQuiet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().getQuiet(key);
        }
        return element;
    }

    public Element getQuiet(final Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    public List getKeys() throws IllegalStateException, CacheException {
        List keys = null;
        try {
            keys = executeWithExecutor(new Callable<List>() {
                public List call() throws Exception {
                    return executeBehavior.getKeys();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().getKeys();
        }
        return keys;
    }

    public List getKeysNoDuplicateCheck() throws IllegalStateException {
        List keys = null;
        try {
            keys = executeWithExecutor(new Callable<List>() {
                public List call() throws Exception {
                    return executeBehavior.getKeysNoDuplicateCheck();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().getKeysNoDuplicateCheck();
        }
        return keys;
    }

    public List getKeysWithExpiryCheck() throws IllegalStateException, CacheException {
        List keys = null;
        try {
            keys = executeWithExecutor(new Callable<List>() {
                public List call() throws Exception {
                    return executeBehavior.getKeysWithExpiryCheck();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().getKeysWithExpiryCheck();
        }
        return keys;
    }

    public void put(final Element element, final boolean doNotNotifyCacheReplicators) throws IllegalArgumentException,
            IllegalStateException, CacheException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.put(element, doNotNotifyCacheReplicators);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveBehavior().put(element, doNotNotifyCacheReplicators);
        }
    }

    public void put(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.put(element);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveBehavior().put(element);
        }
    }

    public void putQuiet(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.putQuiet(element);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveBehavior().putQuiet(element);
        }
    }

    public void putWithWriter(final Element element) throws IllegalArgumentException, IllegalStateException, CacheException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.putWithWriter(element);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveBehavior().putWithWriter(element);
        }
    }

    public boolean remove(final Object key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        boolean result = false;
        try {
            result = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return Boolean.valueOf(executeBehavior.remove(key, doNotNotifyCacheReplicators));
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().remove(key, doNotNotifyCacheReplicators);
        }
        return result;
    }

    public boolean remove(final Object key) throws IllegalStateException {
        boolean result = false;
        try {
            result = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return Boolean.valueOf(executeBehavior.remove(key));
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().remove(key);
        }
        return result;
    }

    public boolean remove(final Serializable key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return this.remove((Object) key, doNotNotifyCacheReplicators);
    }

    public boolean remove(final Serializable key) throws IllegalStateException {
        return this.remove((Object) key);
    }

    public void removeAll() throws IllegalStateException, CacheException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.removeAll();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveBehavior().removeAll();
        }
    }

    public void removeAll(final boolean doNotNotifyCacheReplicators) throws IllegalStateException, CacheException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.removeAll(doNotNotifyCacheReplicators);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveBehavior().removeAll(doNotNotifyCacheReplicators);
        }
    }

    public boolean isKeyInCache(final Object key) {
        boolean result = false;
        try {
            result = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return Boolean.valueOf(executeBehavior.isKeyInCache(key));
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().isKeyInCache(key);
        }
        return result;
    }

    public boolean isValueInCache(final Object value) {
        boolean result = false;
        try {
            result = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return Boolean.valueOf(executeBehavior.isValueInCache(value));
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().isValueInCache(value);
        }
        return result;
    }

    public long calculateInMemorySize() throws IllegalStateException, CacheException {
        long result = 0;
        try {
            result = executeWithExecutor(new Callable<Long>() {
                public Long call() throws Exception {
                    return Long.valueOf(executeBehavior.calculateInMemorySize());
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().calculateInMemorySize();
        }
        return result;
    }

    public void evictExpiredElements() {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.evictExpiredElements();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveBehavior().evictExpiredElements();
        }
    }

    public void flush() throws IllegalStateException, CacheException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.flush();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveBehavior().flush();
        }
    }

    public int getDiskStoreSize() throws IllegalStateException {
        int result = 0;
        try {
            result = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return Integer.valueOf(executeBehavior.getDiskStoreSize());
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().getDiskStoreSize();
        }
        return result;
    }

    public Object getInternalContext() {
        Object result = 0;
        try {
            result = executeWithExecutor(new Callable<Object>() {
                public Object call() throws Exception {
                    return executeBehavior.getInternalContext();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().getInternalContext();
        }
        return result;
    }

    public long getMemoryStoreSize() throws IllegalStateException {
        long result = 0;
        try {
            result = executeWithExecutor(new Callable<Long>() {
                public Long call() throws Exception {
                    return Long.valueOf(executeBehavior.getMemoryStoreSize());
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().getMemoryStoreSize();
        }
        return result;
    }

    public int getSize() throws IllegalStateException, CacheException {
        int result = 0;
        try {
            result = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return Integer.valueOf(executeBehavior.getSize());
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().getSize();
        }
        return result;
    }

    public int getSizeBasedOnAccuracy(final int statisticsAccuracy) throws IllegalArgumentException, IllegalStateException, CacheException {
        int result = 0;
        try {
            result = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return executeBehavior.getSizeBasedOnAccuracy(statisticsAccuracy);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().getSizeBasedOnAccuracy(statisticsAccuracy);
        }
        return result;
    }

    public Statistics getStatistics() throws IllegalStateException {
        Statistics result = null;
        try {
            result = executeWithExecutor(new Callable<Statistics>() {
                public Statistics call() throws Exception {
                    return executeBehavior.getStatistics();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().getStatistics();
        }
        return result;
    }

    public boolean isElementInMemory(final Object key) {
        boolean result = false;
        try {
            result = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return executeBehavior.isElementInMemory(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().isElementInMemory(key);
        }
        return result;
    }

    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    public boolean isElementOnDisk(final Object key) {
        boolean result = false;
        try {
            result = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return executeBehavior.isElementOnDisk(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().isElementOnDisk(key);
        }
        return result;
    }

    public boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    public Element putIfAbsent(final Element element) throws NullPointerException {
        Element result = null;
        try {
            result = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return executeBehavior.putIfAbsent(element);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().putIfAbsent(element);
        }
        return result;
    }

    public boolean removeElement(final Element element) throws NullPointerException {
        boolean result = false;
        try {
            result = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return executeBehavior.removeElement(element);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().removeElement(element);
        }
        return result;
    }

    public boolean removeQuiet(final Object key) throws IllegalStateException {
        boolean result = false;
        try {
            result = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return executeBehavior.removeQuiet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().removeQuiet(key);
        }
        return result;
    }

    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return removeQuiet((Object) key);
    }

    public boolean removeWithWriter(final Object key) throws IllegalStateException, CacheException {
        boolean result = false;
        try {
            result = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return executeBehavior.removeWithWriter(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().removeWithWriter(key);
        }
        return result;
    }

    public boolean replace(final Element old, final Element element) throws NullPointerException, IllegalArgumentException {
        boolean result = false;
        try {
            result = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return executeBehavior.replace(old, element);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().replace(old, element);
        }
        return result;
    }

    public Element replace(final Element element) throws NullPointerException {
        Element result = null;
        try {
            result = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return executeBehavior.replace(element);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveBehavior().replace(element);
        }
        return result;
    }
}
