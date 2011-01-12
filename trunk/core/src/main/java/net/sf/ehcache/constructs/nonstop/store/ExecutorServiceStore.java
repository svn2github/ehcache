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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.concurrent.CacheLockProvider;
import net.sf.ehcache.concurrent.Sync;
import net.sf.ehcache.config.NonstopConfiguration;
import net.sf.ehcache.constructs.nonstop.ClusterOperation;
import net.sf.ehcache.constructs.nonstop.NonstopExecutorService;
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
 * This implementation executes all operations using a {@link NonstopExecutorService}. On Timeout, uses the
 * {@link NonstopTimeoutStoreResolver} to resolve the timeout behavior store and execute it.
 * <p/>
 * A {@link TerracottaStore} that takes another {@link TerracottaStore} as direct delegate, the {@link NonstopConfiguration},
 * {@link NonstopExecutorService} and {@link NonstopTimeoutStoreResolver}
 *
 * @author Abhishek Sanoujam
 *
 */
public class ExecutorServiceStore implements NonstopStore {

    private final TerracottaStore executeBehavior;
    private final NonstopExecutorService executorService;
    private final NonstopTimeoutStoreResolver timeoutBehaviorResolver;
    private final NonstopConfiguration nonstopConfiguration;
    private final AtomicBoolean clusterOffline = new AtomicBoolean();
    private final CacheLockProvider delegateCacheLockProvider;

    /**
     * Constructor accepting the direct delegate behavior, {@link NonstopConfiguration}, {@link NonstopExecutorService} and
     * {@link NonstopTimeoutStoreResolver}
     *
     */
    public ExecutorServiceStore(final TerracottaStore delegateStore, final NonstopConfiguration nonstopConfiguration,
            final NonstopExecutorService executorService, final NonstopTimeoutStoreResolver timeoutBehaviorResolver,
            CacheLockProvider delegateCacheLockProvider) {
        this.executeBehavior = delegateStore;
        this.nonstopConfiguration = nonstopConfiguration;
        this.executorService = executorService;
        this.timeoutBehaviorResolver = timeoutBehaviorResolver;
        this.delegateCacheLockProvider = delegateCacheLockProvider;
    }

    /**
     * Make the cluster offline as cluster rejoin is beginning
     */
    void clusterRejoinStarted() {
        clusterOffline.set(true);
        synchronized (clusterOffline) {
            clusterOffline.notifyAll();
        }
    }

    /**
     * Make the cluster online
     */
    void clusterRejoinComplete() {
        clusterOffline.set(false);
        synchronized (clusterOffline) {
            clusterOffline.notifyAll();
        }
    }

    private <V> V executeWithExecutor(final Callable<V> callable) throws CacheException, TimeoutException {
        return executeWithExecutor(callable, nonstopConfiguration.getTimeoutMillis());
    }

    private <V> V executeWithExecutor(final Callable<V> callable, long timeOutMills) throws CacheException, TimeoutException {
        final long start = System.nanoTime();
        while (clusterOffline.get()) {
            if (nonstopConfiguration.isImmediateTimeout()) {
                throw new TimeoutException("Cluster is currently offline (probably rejoin in progress)");
            }
            final long remaining = timeOutMills - TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
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
            throw new TimeoutException("Cluster is currently offline (probably rejoin in progress)");
        }
        try {
            final long remaining = timeOutMills - TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            return executorService.execute(callable, remaining);
        } catch (InterruptedException e) {
            // rethrow as CacheException
            throw new CacheException(e);
        }
    }

    /**
     * {@inheritDoc}.
     */
    public void addStoreListener(final StoreListener listener) {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.addStoreListener(listener);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().addStoreListener(listener);
        }
    }

    /**
     * {@inheritDoc}.
     */
    public void removeStoreListener(final StoreListener listener) {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.removeStoreListener(listener);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().removeStoreListener(listener);
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
                    return executeBehavior.put(element);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().put(element);
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
                    return executeBehavior.putWithWriter(element, writerManager);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().putWithWriter(element, writerManager);
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
                    return executeBehavior.get(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().get(key);
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
                    return executeBehavior.getQuiet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getQuiet(key);
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
                    return executeBehavior.getKeys();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getKeys();
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
                    return executeBehavior.remove(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().remove(key);
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
                    return executeBehavior.removeWithWriter(key, writerManager);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().removeWithWriter(key, writerManager);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public void removeAll() throws CacheException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.removeAll();
                    return null;
                }
            }, nonstopConfiguration.getTimeoutMillis() * nonstopConfiguration.getBulkOpsTimeoutMultiplyFactor());
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().removeAll();
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
                    return executeBehavior.putIfAbsent(element);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().putIfAbsent(element);
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
                    return executeBehavior.removeElement(element, comparator);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().removeElement(element, comparator);
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
                    return executeBehavior.replace(old, element, comparator);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().replace(old, element, comparator);
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
                    return executeBehavior.replace(element);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().replace(element);
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public void dispose() {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.dispose();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().dispose();
        }
    }

    /**
     * {@inheritDoc}.
     */
    public int getSize() {
        int rv = 0;
        try {
            rv = executeWithExecutor(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return executeBehavior.getSize();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getSize();
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
                    return executeBehavior.getInMemorySize();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getInMemorySize();
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
                    return executeBehavior.getOffHeapSize();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getOffHeapSize();
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
                    return executeBehavior.getOnDiskSize();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getOnDiskSize();
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
                    return executeBehavior.getTerracottaClusteredSize();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getTerracottaClusteredSize();
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
                    return executeBehavior.getInMemorySizeInBytes();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getInMemorySizeInBytes();
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
                    return executeBehavior.getOffHeapSizeInBytes();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getOffHeapSizeInBytes();
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
                    return executeBehavior.getOnDiskSizeInBytes();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getOnDiskSizeInBytes();
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
                    return executeBehavior.getStatus();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getStatus();
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
                    return executeBehavior.containsKey(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().containsKey(key);
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
                    return executeBehavior.containsKeyOnDisk(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().containsKeyOnDisk(key);
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
                    return executeBehavior.containsKeyOffHeap(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().containsKeyOffHeap(key);
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
                    return executeBehavior.containsKeyInMemory(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().containsKeyInMemory(key);
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
                    executeBehavior.expireElements();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().expireElements();
        }
    }

    /**
     * {@inheritDoc}.
     */
    public void flush() throws IOException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.flush();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().flush();
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
                    return executeBehavior.bufferFull();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().bufferFull();
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
                    return executeBehavior.getInMemoryEvictionPolicy();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getInMemoryEvictionPolicy();
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
                    executeBehavior.setInMemoryEvictionPolicy(policy);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().setInMemoryEvictionPolicy(policy);
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
                    return executeBehavior.getInternalContext();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getInternalContext();
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
                    return executeBehavior.isCacheCoherent();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().isCacheCoherent();
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
                    return executeBehavior.isClusterCoherent();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().isClusterCoherent();
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
                    return executeBehavior.isNodeCoherent();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().isNodeCoherent();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public void setNodeCoherent(final boolean coherent) throws UnsupportedOperationException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.setNodeCoherent(coherent);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().setNodeCoherent(coherent);
        }
    }

    /**
     * {@inheritDoc}.
     */
    public void waitUntilClusterCoherent() throws UnsupportedOperationException {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.waitUntilClusterCoherent();
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().waitUntilClusterCoherent();
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
                    return executeBehavior.getMBean();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getMBean();
        }
        return rv;
    }

    /**
     * {@inheritDoc}.
     */
    public void setAttributeExtractors(final Map<String, AttributeExtractor> extractors) {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    executeBehavior.setAttributeExtractors(extractors);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().setAttributeExtractors(extractors);
        }
    }

    /**
     * {@inheritDoc}.
     */
    public Results executeQuery(final StoreQuery query) {
        Results rv = null;
        try {
            rv = executeWithExecutor(new Callable<Results>() {
                public Results call() throws Exception {
                    return executeBehavior.executeQuery(query);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().executeQuery(query);
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
                    return executeBehavior.getSearchAttribute(attributeName);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getSearchAttribute(attributeName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set getLocalKeys() {
        try {
            return executeWithExecutor(new Callable<Set>() {
                public Set call() throws Exception {
                    return executeBehavior.getLocalKeys();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getLocalKeys();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGet(final Object key) {
        try {
            return executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return executeBehavior.unlockedGet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().unlockedGet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unlockedGetQuiet(final Object key) {
        try {
            return executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return executeBehavior.unlockedGetQuiet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().unlockedGetQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGet(final Object key) {
        try {
            return executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return executeBehavior.unsafeGet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().unsafeGet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element unsafeGetQuiet(final Object key) {
        try {
            return executeWithExecutor(new Callable<Element>() {
                public Element call() throws Exception {
                    return executeBehavior.unsafeGetQuiet(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().unsafeGetQuiet(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Sync[] getAndWriteLockAllSyncForKeys(final long timeout, final Object... keys) throws TimeoutException {
        try {
            return executeWithExecutor(new Callable<Sync[]>() {
                public Sync[] call() throws Exception {
                    return delegateCacheLockProvider.getAndWriteLockAllSyncForKeys(timeout, keys);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getAndWriteLockAllSyncForKeys(timeout, keys);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Sync[] getAndWriteLockAllSyncForKeys(final Object... keys) {
        try {
            return executeWithExecutor(new Callable<Sync[]>() {
                public Sync[] call() throws Exception {
                    return delegateCacheLockProvider.getAndWriteLockAllSyncForKeys(keys);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getAndWriteLockAllSyncForKeys(keys);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Sync getSyncForKey(final Object key) {
        try {
            return executeWithExecutor(new Callable<Sync>() {
                public Sync call() throws Exception {
                    return delegateCacheLockProvider.getSyncForKey(key);
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().getSyncForKey(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unlockWriteLockForAllKeys(final Object... keys) {
        try {
            executeWithExecutor(new Callable<Void>() {
                public Void call() throws Exception {
                    delegateCacheLockProvider.unlockWriteLockForAllKeys(keys);
                    return null;
                }
            });
        } catch (TimeoutException e) {
            timeoutBehaviorResolver.resolveTimeoutStore().unlockWriteLockForAllKeys(keys);
        }
    }

    /**
     * {@inheritDoc}
     */
    public <V> V executeClusterOperation(final ClusterOperation<V> operation) {
        try {
            return executeWithExecutor(new Callable<V>() {
                public V call() throws Exception {
                    return operation.performClusterOperation();
                }
            });
        } catch (TimeoutException e) {
            return timeoutBehaviorResolver.resolveTimeoutStore().executeClusterOperation(operation);
        }
    }

}
