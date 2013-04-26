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
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.util.concurrent.ConcurrentHashMap;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.statistics.observer.OperationObserver;

import net.sf.ehcache.store.StoreOperationOutcomes.GetOutcome;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.StatisticsManager;
import org.terracotta.statistics.derived.EventRateSimpleMovingAverage;
import org.terracotta.statistics.derived.OperationResultFilter;

import static net.sf.ehcache.statistics.StatisticBuilder.operation;

/**
 * A backend to a OnHeapCachingTier that will be cap'ed using a pool
 *
 * @param <K> the key type
 * @param <V> the value type
 *
 * @author Alex Snaps
 */
public class PooledBasedBackEnd<K, V> extends ConcurrentHashMap<K, V> implements HeapCacheBackEnd<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(CountBasedBackEnd.class.getName());

    private static final int MAX_EVICTIONS = 5;
    private static final float PUT_LOAD_THRESHOLD = 0.9f;

    private volatile Policy policy;
    private volatile EvictionCallback<K, V> evictionCallback;
    private final AtomicReference<PoolAccessor> poolAccessor = new AtomicReference<PoolAccessor>();

    private final OperationObserver<GetOutcome> getObserver = operation(GetOutcome.class).named("arc-get").of(this).tag("private").build();

    /**
     * Constructs a Pooled backend
     * @param memoryEvictionPolicy the policy it'll use to decide what to evict
     */
    public PooledBasedBackEnd(final Policy memoryEvictionPolicy) {
        setPolicy(memoryEvictionPolicy);
    }

    @Override
    public V putIfAbsent(final K key, final V value) {
        long delta = poolAccessor.get().add(key, value, FAKE_TREE_NODE, false);
        if (delta > -1) {
            final V previous = (V)super.internalPutIfAbsent(key, value, delta > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)delta);
            if (previous != null) {
                poolAccessor.get().delete(delta);
            }
            return previous;
        } else {
            evictionCallback.evicted(key, value);
            return null;
        }
    }

    @Override
    public V get(final Object key) {
        getObserver.begin();
        final V value = super.get(key);
        if (value != null) {
            getObserver.end(GetOutcome.HIT);
        } else {
            getObserver.end(GetOutcome.MISS);
        }
        return value;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V put(final K key, final V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(final Object key) {
        return super.remove(key);
    }

    @Override
    public boolean remove(final Object key, final Object value) {
        return super.remove(key, value);
    }

    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(final K key, final V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public boolean hasSpace() {
        PoolAccessor<?> accessor = poolAccessor.get();
        return accessor.getPoolOccupancy() < (PUT_LOAD_THRESHOLD * accessor.getPoolSize());
    }

    /**
     * tries to evict as many entries as specified
     * @param evictions amount of entries to be evicted
     * @return return true if exactly the right amount of evictions could happen, false otherwise
     */
    public boolean evict(int evictions) {
        while (evictions-- > 0) {
            final Element evictionCandidate = findEvictionCandidate();
            if (evictionCandidate != null) {
                remove(evictionCandidate.getObjectKey(), evictionCandidate, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        evictionCallback.evicted((K)evictionCandidate.getObjectKey(), (V)evictionCandidate);
                        return null;
                    }
                });
            } else {
                return false;
            }
        }
        return true;
    }

    private Element findEvictionCandidate() {
        List<V> values = getRandomValues(MAX_EVICTIONS);
        // this can return null. Let the cache get bigger by one.
        List<Element> elements = new ArrayList<Element>(values.size() * 2);
        for (V v : values) {
            if (v instanceof Element) {
                elements.add((Element)v);
            }
        }
        return policy.selectedBasedOnPolicy(elements.toArray(new Element[elements.size()]), null);
    }

    /**
     * Dynamic property to switch the policy out
     * @param policy the new eviction Policy to use
     */
    public void setPolicy(final Policy policy) {
        if (policy == null) {
            throw new NullPointerException("We need a Policy passed in here, null won't cut it!");
        }
        this.policy = policy;
    }

    @Override
    public void registerEvictionCallback(final EvictionCallback<K, V> callback) {
        this.evictionCallback = callback;
    }

    @Override
    public Policy getPolicy() {
        return policy;
    }

    /**
     * Registers the accessor with the backend. This can only happen once!
     * @param poolAccessor the pool accessor to use
     */
    public void registerAccessor(final PoolAccessor poolAccessor) {
        if (poolAccessor == null) {
            throw new NullPointerException("No null poolAccessor allowed here!");
        }
        if (!this.poolAccessor.compareAndSet(null, poolAccessor)) {
            throw new IllegalStateException("Can't set the poolAccessor multiple times!");
        }
        super.setPoolAccessor(poolAccessor);
    }

    /**
     * Returns the size in bytes
     * @return the amount of bytes for this backend
     */
    @Deprecated
    public long getSizeInBytes() {
        return poolAccessor.get().getSize();
    }

    /**
     * A pool participant to use with this Backend
     */
    public static class PoolParticipant implements net.sf.ehcache.pool.PoolParticipant {

        private final EventRateSimpleMovingAverage hitRate = new EventRateSimpleMovingAverage(1, TimeUnit.SECONDS);
        private final EventRateSimpleMovingAverage missRate = new EventRateSimpleMovingAverage(1, TimeUnit.SECONDS);
        private final PooledBasedBackEnd<Object, Object> pooledBasedBackEnd;

        /**
         * Creates a pool participant
         * @param pooledBasedBackEnd the backend this participant represents
         */
        public PoolParticipant(final PooledBasedBackEnd<Object, Object> pooledBasedBackEnd) {
            this.pooledBasedBackEnd = pooledBasedBackEnd;
            OperationStatistic<GetOutcome> getStatistic = StatisticsManager.getOperationStatisticFor(pooledBasedBackEnd.getObserver);
            getStatistic.addDerivedStatistic(new OperationResultFilter<GetOutcome>(EnumSet.of(GetOutcome.HIT), hitRate));
            getStatistic.addDerivedStatistic(new OperationResultFilter<GetOutcome>(EnumSet.of(GetOutcome.MISS), missRate));
        }

        @Override
        public boolean evict(final int count, final long size) {
            try {
                return pooledBasedBackEnd.evict(count);
            } catch (Throwable e) {
                LOG.warn("Caught throwable while evicting", e);
            }
            return false;
        }

        @Override
        public float getApproximateHitRate() {
            return hitRate.rateUsingSeconds().floatValue();
        }

        @Override
        public float getApproximateMissRate() {
            return missRate.rateUsingSeconds().floatValue();
        }

        @Override
        public long getApproximateCountSize() {
            return pooledBasedBackEnd.mappingCount();
        }
    }
}
