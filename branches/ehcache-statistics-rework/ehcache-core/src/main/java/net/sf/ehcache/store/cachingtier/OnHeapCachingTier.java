/**
 *  Copyright Terracotta, Inc.
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
package net.sf.ehcache.store.cachingtier;

import net.sf.ehcache.Element;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;
import net.sf.ehcache.pool.sizeof.annotations.IgnoreSizeOf;
import net.sf.ehcache.store.CachingTier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import net.sf.ehcache.store.StoreOperationOutcomes.GetOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.PutOutcome;
import net.sf.ehcache.store.StoreOperationOutcomes.RemoveOutcome;
import org.terracotta.context.annotations.ContextChild;
import org.terracotta.statistics.Statistic;
import org.terracotta.statistics.observer.OperationObserver;

import static net.sf.ehcache.statistics.StatisticBuilder.*;

/**
 * An instance of this class will delegate the storage to the backing HeapCacheBackEnd.<br/>
 * <ul>Adding :
 * <li>making sure only a single thread populates the cache for a given key at a time</li>
 * <li>translate calls to the eviction listeners</li>
 * <li>Add all the crap about sizing and stuff</li>
 * </ul>
 *
 * @param <K> The key type
 * @param <V> the value type
 * @author Alex Snaps
 */
public class OnHeapCachingTier<K, V> implements CachingTier<K, V> {

    @ContextChild
    private final HeapCacheBackEnd<K, Object> backEnd;
    
    private final OperationObserver<GetOutcome> getObserver = operation(GetOutcome.class).named("get").of(this).tag("local-heap").build();
    private final OperationObserver<PutOutcome> putObserver = operation(PutOutcome.class).named("put").of(this).tag("local-heap").build();
    private final OperationObserver<RemoveOutcome> removeObserver = operation(RemoveOutcome.class).named("remove").of(this).tag("local-heap").build();


    private volatile List<Listener<K, V>> listeners = new CopyOnWriteArrayList<Listener<K, V>>();

    /**
     * A Constructor
     *
     * @param backEnd the HeapCacheBackEnd that will back this CachingTier
     */
    public OnHeapCachingTier(final HeapCacheBackEnd<K, Object> backEnd) {
        this.backEnd = backEnd;
        this.backEnd.registerEvictionCallback(new HeapCacheBackEnd.EvictionCallback<K, Object>() {
            @Override
            public void evicted(final K key, final Object value) {
                for (Listener<K, V> listener : listeners) {
                    listener.evicted(key, getValue(value));
                }
            }
        });
    }

    @Override
    public V get(final K key, final Callable<V> source, final boolean updateStats) {
        if (updateStats) getObserver.begin();
        Object cachedValue = backEnd.get(key);
        if (cachedValue == null) {
            if (updateStats) getObserver.end(GetOutcome.MISS);
            Fault<V> f = new Fault<V>(source);
            cachedValue = backEnd.putIfAbsent(key, f);
            if (cachedValue == null && !f.complete) {
                try {
                    V value = source.call();
                    if (value == null) {
                        backEnd.remove(key, f);
                    } else {
                        // This can fail if the fault was evicted
                        backEnd.replace(key, f, value);
                    }
                    f.complete(value);
                    return value;
                } catch (Exception e) {
                    backEnd.remove(key, f);
                    f.fail(e);
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException)e;
                    }
                    throw new RuntimeException(e);
                }
            }
        } else {
            if (updateStats) getObserver.end(GetOutcome.HIT);
        }

        return getValue(cachedValue);
    }

    @Override
    public V remove(final K key) {
        return getValue(backEnd.remove(key));
    }

    @Override
    public void clear() {
        backEnd.clear();
    }

    @Override
    public void addListener(final Listener<K, V> listener) {
        if (listener == null) {
            throw new NullPointerException("Listener can't be null!");
        }
        listeners.add(listener);
    }

    @Statistic(name = "size", tags = "local-heap")
    @Override
    public int getInMemorySize() {
        return backEnd.size();
    }

    @Override
    public int getOffHeapSize() {
        return 0;
    }

    @Override
    public boolean contains(final K key) {
        return backEnd.get(key) != null;
    }

    @Statistic(name = "size-in-bytes", tags = "local-heap")
    @Override
    public long getInMemorySizeInBytes() {
        long sizeInBytes;
        if (backEnd instanceof PooledBasedBackEnd<?, ?>) {
            sizeInBytes = ((PooledBasedBackEnd)backEnd).getSizeInBytes();
        } else {
            DefaultSizeOfEngine defaultSizeOfEngine = new DefaultSizeOfEngine(
                net.sf.ehcache.config.SizeOfPolicyConfiguration.DEFAULT_MAX_SIZEOF_DEPTH,
                SizeOfPolicyConfiguration.DEFAULT_MAX_DEPTH_EXCEEDED_BEHAVIOR == SizeOfPolicyConfiguration.MaxDepthExceededBehavior.ABORT
            );
            sizeInBytes = 0;
            for (Map.Entry<K, Object> entry : backEnd.entrySet()) {
                Element element = (Element)entry.getValue();
                if (element != null) {
                    // TODO this is a lie here! Should we add a dedicated method to BackEnd to return a container ?
                    Size size = defaultSizeOfEngine.sizeOf(element.getObjectKey(), element, null);
                    sizeInBytes += size.getCalculated();
                }
            }
        }
        return sizeInBytes;
    }

    @Override
    public long getOnDiskSizeInBytes() {
        return 0;
    }

    private V getValue(final Object cachedValue) {
        if (cachedValue instanceof Fault) {
            return ((Fault<V>)cachedValue).get();
        } else {
            return (V)cachedValue;
        }
    }

    /**
     * Document me
     *
     * @param <V>
     */
    @IgnoreSizeOf
    private static class Fault<V> {

        private final Callable<V> source;
        private final Thread owner = Thread.currentThread();
        private V value;
        private Throwable throwable;
        private boolean complete;

        public Fault(final Callable<V> source) {
            this.source = source;
        }

        private void complete(V value) {
            synchronized (this) {
                this.value = value;
                this.complete = true;
                notifyAll();
            }
        }

        private V get() {
            synchronized (this) {
                boolean interrupted = false;
                try {
                    if (Thread.currentThread() == owner & !complete) {
                        try {
                            complete(source.call());
                        } catch (Exception e) {
                            fail(e);
                        }
                    } else {
                        while (!complete) {
                            try {
                                wait();
                            } catch (InterruptedException e) {
                                interrupted = true;
                            }
                        }
                    }
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (throwable != null) {
                throw new RuntimeException("Faulting from repository failed on other thread", throwable);
            }

            return value;
        }

        private void fail(final Throwable t) {
            synchronized (this) {
                this.throwable = t;
                this.complete = true;
                notifyAll();
            }
        }
    }
}
