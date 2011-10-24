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

package net.sf.ehcache.constructs.nonstop.store;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.cluster.CacheCluster;
import net.sf.ehcache.cluster.ClusterNode;
import net.sf.ehcache.cluster.ClusterTopologyListener;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.NonstopActiveDelegateHolder;
import net.sf.ehcache.constructs.nonstop.concurrency.CacheOperationUnderExplicitLockCallable;
import net.sf.ehcache.constructs.nonstop.concurrency.ExplicitLockingContextThreadLocal;
import net.sf.ehcache.constructs.nonstop.concurrency.NonStopCacheKeySet;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.store.ElementValueComparator;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.store.StoreListener;
import net.sf.ehcache.store.StoreQuery;
import net.sf.ehcache.store.TerracottaStore;
import net.sf.ehcache.writer.CacheWriterManager;

/**
 * This implementation executes all operations using a NonstopExecutorService. On Timeout, uses the
 * {@link NonstopTimeoutBehaviorStoreResolver} to
 * resolve the timeout behavior store and execute it.
 * <p/>
 *
 * @author Abhishek Sanoujam
 *
 */
public class ExecutorServiceStore implements RejoinAwareNonstopStore {

    /**
     * The NonstopConfiguration of the cache using this store
     */
    protected final NonstopConfiguration nonstopConfiguration;
    private final NonstopActiveDelegateHolder nonstopActiveDelegateHolder;
    private final NonstopTimeoutBehaviorStoreResolver timeoutBehaviorResolver;
    private final AtomicBoolean clusterOffline = new AtomicBoolean();
    private final List<RejoinAwareBlockingOperation> rejoinAwareOperations = new CopyOnWriteArrayList<RejoinAwareBlockingOperation>();
    private final ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal;

    /**
     * Constructor accepting the {@link NonstopActiveDelegateHolder}, {@link NonstopConfiguration} and
     * {@link NonstopTimeoutBehaviorStoreResolver}
     *
     * @param explicitLockingContextThreadLocal
     *
     */
    public ExecutorServiceStore(final NonstopActiveDelegateHolder nonstopActiveDelegateHolder,
            final NonstopConfiguration nonstopConfiguration, final NonstopTimeoutBehaviorStoreResolver timeoutBehaviorResolver,
            CacheCluster cacheCluster, ExplicitLockingContextThreadLocal explicitLockingContextThreadLocal) {
        this.nonstopActiveDelegateHolder = nonstopActiveDelegateHolder;
        this.nonstopConfiguration = nonstopConfiguration;
        this.timeoutBehaviorResolver = timeoutBehaviorResolver;
        this.explicitLockingContextThreadLocal = explicitLockingContextThreadLocal;
        cacheCluster.addTopologyListener(new ClusterStatusListener(this, cacheCluster));
    }

    /**
     * Make the cluster offline as cluster rejoin is beginning
     */
    void clusterOffline() {
        clusterOffline.set(true);
        synchronized (clusterOffline) {
            clusterOffline.notifyAll();
        }
    }

    /**
     * Make the cluster online
     */
    void clusterOnline() {
        clusterOffline.set(false);
        synchronized (clusterOffline) {
            clusterOffline.notifyAll();
        }
    }

    private <V> V forceExecuteWithExecutor(final Callable<V> callable) throws CacheException, TimeoutException {
        return forceExecuteWithExecutor(callable, nonstopConfiguration.getTimeoutMillis());
    }

    private <V> V forceExecuteWithExecutor(final Callable<V> callable, final long timeoutMillis) throws CacheException, TimeoutException {
        return executeWithExecutor(callable, timeoutMillis, true);
    }

    /**
     * Execute call within NonStop executor
     *
     * @param callable
     * @param <V>
     * @throws CacheException
     * @throws TimeoutException
     * @return returns the result of the callable
     */
    protected <V> V executeWithExecutor(final Callable<V> callable) throws CacheException, TimeoutException {
        return executeWithExecutor(callable, nonstopConfiguration.getTimeoutMillis(), false);
    }

    /**
     * Execute call within NonStop executor
     *
     * @param callable
     * @param timeoutMillis
     * @param <V>
     * @throws CacheException
     * @throws TimeoutException
     * @return the result of the callable
     */
    protected <V> V executeWithExecutor(final Callable<V> callable, final long timeoutMillis) throws CacheException, TimeoutException {
        return executeWithExecutor(callable, timeoutMillis, false);
    }

    private <V> V executeWithExecutor(final Callable<V> callable, final long timeOutMills, final boolean force) throws CacheException,
            TimeoutException {
        Callable<V> effectiveCallable = callable;
        final long start = System.nanoTime();
        if (!force) {
            checkForClusterOffline(start, timeOutMills);
        }
        final boolean operationUnderExplicitLock = explicitLockingContextThreadLocal.areAnyExplicitLocksAcquired();
        if (operationUnderExplicitLock) {
            effectiveCallable = new CacheOperationUnderExplicitLockCallable<V>(
                    explicitLockingContextThreadLocal.getCurrentThreadLockContext(), nonstopConfiguration, callable);
        }
        try {
            final long remaining = timeOutMills - TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            return nonstopActiveDelegateHolder.getNonstopExecutorService().execute(effectiveCallable, remaining);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
    }

    /**
     * Get the underlying Terracotta store
     *
     * @return the underlying Terracotta store
     */
    protected TerracottaStore underlyingTerracottaStore() {
        return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore();
    }

    /**
     * Get the timeout behavior resolver NonstopStore
     *
     * @return the timeout behavior resolver NonstopStore
     */
    protected NonstopStore resolveTimeoutBehaviorStore() {
        return timeoutBehaviorResolver.resolveTimeoutBehaviorStore();
    }

    private void checkForClusterOffline(final long start, final long timeoutMills) throws TimeoutException {
        while (clusterOffline.get()) {
            if (nonstopConfiguration.isImmediateTimeout()) {
                throw new TimeoutException("Cluster is currently offline");
            }
            final long remaining = timeoutMills - TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            if (remaining <= 0) {
                break;
            }
            synchronized (clusterOffline) {
                try {
                    clusterOffline.wait(remaining);
                } catch (InterruptedException e) {
                    // rethrow as CacheException
                    throw new CacheException(e);
                }
            }
        }
        if (clusterOffline.get()) {
            // still cluster offline
            throw new TimeoutException("Cluster is currently offline");
        }
    }

    // /////////////////////////////////////////////////////////
    // methods below use the 'force' executeWithExecutor version
    // these are methods that are used during rejoin, as during rejoin
    // the cluster is offline and unless force is used, the methods
    // won't be executed at all
    // /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}.
     */
    public void dispose() {
        try {
            // always execute even when cluster offline
            forceExecuteWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().dispose();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutBehaviorStore().dispose();
        }
    }

    /**
     * {@inheritDoc}.
     * The timeout used by this method is {@link NonstopConfiguration#getBulkOpsTimeoutMultiplyFactor()} times the timeout value in the
     * config.
     */
    public void setNodeCoherent(final boolean coherent) throws UnsupportedOperationException {
        try {
            // always execute even when cluster offline
            forceExecuteWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().setNodeCoherent(coherent);
                    return null;
                }
            }, nonstopConfiguration.getTimeoutMillis() * nonstopConfiguration.getBulkOpsTimeoutMultiplyFactor());
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutBehaviorStore().setNodeCoherent(coherent);
        }
    }

    /**
     * {@inheritDoc}.
     */
    public void setAttributeExtractors(final Map<String, AttributeExtractor> extractors) {
        try {
            // always execute even when cluster offline
            forceExecuteWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().setAttributeExtractors(extractors);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutBehaviorStore().setAttributeExtractors(extractors);
        }
    }

    /**
     * {@inheritDoc}.
     */
    public void addStoreListener(final StoreListener listener) {
        try {
            // always execute even when cluster offline
            forceExecuteWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().addStoreListener(listener);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutBehaviorStore().addStoreListener(listener);
        }
    }

    // /////////////////////////////////////////////////////////
    // methods below use the normal executeWithExecutor version
    // /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}.
     */
    public void removeStoreListener(final StoreListener listener) {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().removeStoreListener(listener);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutBehaviorStore().removeStoreListener(listener);
        }
    }

    /**
     * {@inheritDoc}.
     */
    public boolean put(final Element element) throws CacheException {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().put(element);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().put(element);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public boolean putWithWriter(final Element element, final CacheWriterManager writerManager) throws CacheException {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().putWithWriter(element, writerManager);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().putWithWriter(element, writerManager);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public Element get(final Object key) {
        Element rv = null;
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().get(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().get(key);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public Element getQuiet(final Object key) {
        Element rv = null;
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getQuiet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getQuiet(key);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public List getKeys() {
        List rv = null;
        try {
            rv = executeWithExecutor(new Callable<List>() {
                public List call() throws Exception {
                    return new NonStopCacheKeySet(nonstopActiveDelegateHolder, nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getKeys());
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getKeys();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public Element remove(final Object key) {
        Element rv = null;
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().remove(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().remove(key);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public Element removeWithWriter(final Object key, final CacheWriterManager writerManager) throws CacheException {
        Element rv = null;
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().removeWithWriter(key, writerManager);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().removeWithWriter(key, writerManager);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     * The timeout used by this method is {@link NonstopConfiguration#getBulkOpsTimeoutMultiplyFactor()} times the timeout value in the
     * config.
     */
    public void removeAll() throws CacheException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().removeAll();
                    return null;
                }
            }, nonstopConfiguration.getTimeoutMillis() * nonstopConfiguration.getBulkOpsTimeoutMultiplyFactor());
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutBehaviorStore().removeAll();
        }
    }

    /**
     * {@inheritDoc}.
     */
    public Element putIfAbsent(final Element element) throws NullPointerException {
        Element rv = null;
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().putIfAbsent(element);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().putIfAbsent(element);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public Element removeElement(final Element element, final ElementValueComparator comparator) throws NullPointerException {
        Element rv = null;
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().removeElement(element, comparator);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().removeElement(element, comparator);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public boolean replace(final Element old, final Element element, final ElementValueComparator comparator) throws NullPointerException,
            IllegalArgumentException {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().replace(old, element, comparator);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().replace(old, element, comparator);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public Element replace(final Element element) throws NullPointerException {
        Element rv = null;
        try {
            rv = executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().replace(element);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().replace(element);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public int getSize() {
        int rv = 0;
        try {
            rv = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getSize();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getSize();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public int getInMemorySize() {
        int rv = 0;
        try {
            rv = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getInMemorySize();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getInMemorySize();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public int getOffHeapSize() {
        int rv = 0;
        try {
            rv = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getOffHeapSize();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getOffHeapSize();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public int getOnDiskSize() {
        int rv = 0;
        try {
            rv = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getOnDiskSize();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getOnDiskSize();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public int getTerracottaClusteredSize() {
        int rv = 0;
        try {
            rv = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getTerracottaClusteredSize();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getTerracottaClusteredSize();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public long getInMemorySizeInBytes() {
        long rv = 0;
        try {
            rv = executeWithExecutor(new Callable<Long>() {
                public Long call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getInMemorySizeInBytes();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getInMemorySizeInBytes();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public long getOffHeapSizeInBytes() {
        long rv = 0;
        try {
            rv = executeWithExecutor(new Callable<Long>() {
                public Long call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getOffHeapSizeInBytes();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getOffHeapSizeInBytes();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public long getOnDiskSizeInBytes() {
        long rv = 0;
        try {
            rv = executeWithExecutor(new Callable<Long>() {
                public Long call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getOnDiskSizeInBytes();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getOnDiskSizeInBytes();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public Status getStatus() {
        Status rv = null;
        try {
            rv = executeWithExecutor(new Callable<Status>() {
                public Status call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getStatus();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getStatus();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public boolean containsKey(final Object key) {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().containsKey(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().containsKey(key);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public boolean containsKeyOnDisk(final Object key) {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().containsKeyOnDisk(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().containsKeyOnDisk(key);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public boolean containsKeyOffHeap(final Object key) {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().containsKeyOffHeap(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().containsKeyOffHeap(key);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public boolean containsKeyInMemory(final Object key) {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().containsKeyInMemory(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().containsKeyInMemory(key);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public void expireElements() {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().expireElements();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutBehaviorStore().expireElements();
        }
    }

    /**
     * {@inheritDoc}.
     */
    public void flush() throws IOException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().flush();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutBehaviorStore().flush();
        }
    }

    /**
     * {@inheritDoc}.
     */
    public boolean bufferFull() {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().bufferFull();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().bufferFull();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public Policy getInMemoryEvictionPolicy() {
        Policy rv = null;
        try {
            rv = executeWithExecutor(new Callable<Policy>() {
                public Policy call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getInMemoryEvictionPolicy();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getInMemoryEvictionPolicy();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public void setInMemoryEvictionPolicy(final Policy policy) {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().setInMemoryEvictionPolicy(policy);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutBehaviorStore().setInMemoryEvictionPolicy(policy);
        }
    }

    /**
     * {@inheritDoc}.
     */
    public Object getInternalContext() {
        Object rv = null;
        try {
            rv = executeWithExecutor(new Callable<Object>() {
                public Object call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getInternalContext();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getInternalContext();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public boolean isCacheCoherent() {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().isCacheCoherent();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().isCacheCoherent();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public boolean isClusterCoherent() {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().isClusterCoherent();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().isClusterCoherent();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public boolean isNodeCoherent() {
        boolean rv = false;
        try {
            rv = executeWithExecutor(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().isNodeCoherent();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().isNodeCoherent();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     *
     * @throws InterruptedException
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException, InterruptedException {
        final RejoinAwareBlockingOperation<Void> operation = new RejoinAwareBlockingOperation<Void>(this, new Callable<Void>() {
            public Void call() throws Exception {
                nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().waitUntilClusterCoherent();
                return null;
            }
        });
        rejoinAwareOperations.add(operation);
        try {
            operation.call();
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                throw (InterruptedException) e;
            } else {
                throw new CacheException(e);
            }
        } finally {
            rejoinAwareOperations.remove(operation);
        }
    }

    /**
     * {@inheritDoc}.
     */
    public Object getMBean() {
        Object rv = null;
        try {
            rv = executeWithExecutor(new Callable<Object>() {
                public Object call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getMBean();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getMBean();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public Results executeQuery(final StoreQuery query) {
        Results rv = null;
        try {
            rv = executeWithExecutor(new Callable<Results>() {
                public Results call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().executeQuery(query);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().executeQuery(query);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public <T> Attribute<T> getSearchAttribute(final String attributeName) {
        try {
            return executeWithExecutor(new Callable<Attribute<T>>() {
                public Attribute<T> call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getSearchAttribute(attributeName);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getSearchAttribute(attributeName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set getLocalKeys() {
        try {
            return executeWithExecutor(new Callable<Set>() {
                public Set call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().getLocalKeys();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().getLocalKeys();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGet(final Object key) {
        try {
            return executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unlockedGet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().unlockedGet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGetQuiet(final Object key) {
        try {
            return executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unlockedGetQuiet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().unlockedGetQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGet(final Object key) {
        try {
            return executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unsafeGet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().unsafeGet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGetQuiet(final Object key) {
        try {
            return executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return nonstopActiveDelegateHolder.getUnderlyingTerracottaStore().unsafeGetQuiet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutBehaviorStore().unsafeGetQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public <V> V executeClusterOperation(final ClusterOperation<V> operation) {
        try {
            return executeWithExecutor(new ClusterOperationCallableImpl<V>(operation));
        } catch (TimeoutException e) {
            return operation.performClusterOperationTimedOut(this.nonstopConfiguration.getTimeoutBehavior().getTimeoutBehaviorType());
        }
    }

    /**
     * Executes the {@link ClusterOperation} parameter, but without any timeout. This call will block until the {@link ClusterOperation}
     * completes. The
     * {@link ClusterOperation#performClusterOperationTimedOut(net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType)} will
     * never be invoked for this
     *
     * @throws InterruptedException if the executing thread is interrupted before the {@link ClusterOperation} can complete
     */
    protected <V> V executeClusterOperationNoTimeout(final ClusterOperation<V> operation) throws InterruptedException {
        try {
            return executeWithExecutor(new ClusterOperationCallableImpl<V>(operation), Integer.MAX_VALUE, true);
        } catch (TimeoutException e) {
            throw new AssertionError("This should never happen as executed with no-timeout");
        } catch (CacheException e) {
            Throwable rootCause = getRootCause(e);
            if (rootCause instanceof InterruptedException) {
                throw (InterruptedException) rootCause;
            } else {
                throw e;
            }
        }
    }

    private Throwable getRootCause(final CacheException exception) {
        Throwable e = exception;
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e;
    }

    /**
     * A {@link ClusterTopologyListener} implementation that listens for cluster online/offline events
     *
     * @author Abhishek Sanoujam
     *
     */
    private static class ClusterStatusListener implements ClusterTopologyListener {

        private final ExecutorServiceStore executorServiceStore;
        private final CacheCluster cacheCluster;

        public ClusterStatusListener(ExecutorServiceStore executorServiceStore, CacheCluster cacheCluster) {
            this.executorServiceStore = executorServiceStore;
            this.cacheCluster = cacheCluster;
        }

        /**
         * {@inheritDoc}
         */
        public void clusterOffline(ClusterNode node) {
            if (cacheCluster.getCurrentNode().equals(node)) {
                executorServiceStore.clusterOffline();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void clusterOnline(ClusterNode node) {
            if (cacheCluster.getCurrentNode().equals(node)) {
                executorServiceStore.clusterOnline();
            }
        }

        /**
         * {@inheritDoc}
         */
        public void nodeJoined(ClusterNode node) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        public void nodeLeft(ClusterNode node) {
            // no-op
        }

        /**
         * {@inheritDoc}
         */
        public void clusterRejoined(ClusterNode oldNode, ClusterNode newNode) {
            // no-op
        }

    }

    /**
     * {@inheritDoc}
     */
    public void clusterRejoined() {
        for (RejoinAwareBlockingOperation operation : rejoinAwareOperations) {
            operation.clusterRejoined();
        }
    }
}
