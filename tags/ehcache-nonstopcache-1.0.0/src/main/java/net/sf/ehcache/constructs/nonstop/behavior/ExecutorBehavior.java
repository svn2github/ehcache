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

/**
 * A {@link NonStopCacheBehavior} that takes another {@link NonStopCacheBehavior} as direct delegate behavior, the
 * {@link NonStopCacheConfig}, {@link NonStopCacheExecutorService} and {@link NonStopCacheBehaviorResolver}
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class ExecutorBehavior implements NonStopCacheBehavior {

    private final NonStopCacheBehavior executeBehavior;
    private final NonStopCacheExecutorService executorService;
    private final NonStopCacheBehaviorResolver timeoutBehaviorResolver;
    private final NonStopCacheConfig nonStopCacheConfig;

    /**
     * Constructor accepting the direct delegate behavior, {@link NonStopCacheConfig}, {@link NonStopCacheExecutorService} and
     * {@link NonStopCacheBehaviorResolver}
     * 
     * @param executeBehavior
     * @param nonStopCacheConfig
     * @param executorService
     * @param timeoutBehaviorResolver
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
    public Element get(final Serializable key) throws IllegalStateException, CacheException {
        return get((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
    public Element getQuiet(final Serializable key) throws IllegalStateException, CacheException {
        return getQuiet((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
    public boolean remove(final Serializable key, final boolean doNotNotifyCacheReplicators) throws IllegalStateException {
        return this.remove((Object) key, doNotNotifyCacheReplicators);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
    public boolean remove(final Serializable key) throws IllegalStateException {
        return this.remove((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
    public boolean isElementInMemory(Serializable key) {
        return isElementInMemory((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
    public boolean isElementOnDisk(Serializable key) {
        return isElementOnDisk((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
    public boolean removeQuiet(Serializable key) throws IllegalStateException {
        return removeQuiet((Object) key);
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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

    /**
     * {@inheritDoc}.
     * <p>
     * Executes the direct delegate behavior with the {@link NonStopCacheExecutorService} using the timeout from the
     * {@link NonStopCacheConfig}. On timeout from the {@link NonStopCacheExecutorService}, uses the behavior resolved from the
     * {@link NonStopCacheBehaviorResolver} to execute the behavior
     */
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
