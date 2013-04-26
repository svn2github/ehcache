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
import net.sf.ehcache.store.LruPolicy;
import net.sf.ehcache.store.Policy;
import net.sf.ehcache.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 * A backend to a OnHeapCachingTier that will be cap'ed based on the amount of entries
 *
 * @param <K> the key type
 * @param <V> the value type
 *
 * @author Alex Snaps
 */
public class CountBasedBackEnd<K, V> extends ConcurrentHashMap<K, V> implements HeapCacheBackEnd<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(CountBasedBackEnd.class.getName());

    private static final int MAX_EVICTIONS = 5;
    private static final int SAMPLING_SIZE = 30;

    private volatile long maxEntriesLocalHeap;
    private volatile Policy policy;
    private volatile EvictionCallback<K, V> evictionCallback;

    /**
     * Constructs a cap'ed backend
     * @param maxEntriesLocalHeap amount of mappings this should hold before it starts evicting
     */
    public CountBasedBackEnd(final long maxEntriesLocalHeap) {
        this(maxEntriesLocalHeap, new LruPolicy());
    }

    /**
     * Constructs a cap'ed backend
     * @param maxEntriesLocalHeap amount of mappings this should hold before it starts evicting
     * @param policy the policy it'll use to decide what to evict
     */
    public CountBasedBackEnd(final long maxEntriesLocalHeap, final Policy policy) {
        this.maxEntriesLocalHeap = maxEntriesLocalHeap;
        setPolicy(policy);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public V putIfAbsent(final K key, final V value) {
        final V v = super.putIfAbsent(key, value);
        if (v == null) {
            try {
                evictIfRequired(key, value);
            } catch (Throwable e) {
                LOG.warn("Caught throwable while evicting", e);
            }
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerEvictionCallback(final EvictionCallback<K, V> callback) {
        this.evictionCallback = callback;
    }

    @Override
    public void recalculateSize(final K key) {
        // NO OP!
    }

    @Override
    public Policy getPolicy() {
        return policy;
    }

    @Override
    public boolean hasSpace() {
        return maxEntriesLocalHeap == 0 || maxEntriesLocalHeap > mappingCount();
    }

    private void evictIfRequired(final K key, final V value) {
        if (maxEntriesLocalHeap == 0) {
            return;
        }
        int evictions = MAX_EVICTIONS;
        while (maxEntriesLocalHeap < mappingCount() && evictions-- > 0) {
            final Element evictionCandidate = findEvictionCandidate(key, value);
            if (evictionCandidate != null) {
                remove(evictionCandidate.getObjectKey(), evictionCandidate, new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                      if (evictionCallback != null) {
                        evictionCallback.evicted((K)evictionCandidate.getObjectKey(), (V)evictionCandidate);
                      }
                      return null;
                    }
                });
            }
        }
    }

    private Element findEvictionCandidate(final K key, final V value) {
        List<V> values = getRandomValues(SAMPLING_SIZE);
        // this can return null. Let the cache get bigger by one.
        List<Element> elements = new ArrayList<Element>(values.size() * 2);
        for (V v : values) {
            if (v instanceof Element && !((Element)v).getObjectKey().equals(key)) {
                elements.add((Element)v);
            }
        }
        return policy.selectedBasedOnPolicy(elements.toArray(new Element[elements.size()]),
            value instanceof Element ? (Element)value : null);
    }

    /**
     * Sets the capacity limit
     * @param maxEntriesLocalHeap the new limit
     */
    public void setMaxEntriesLocalHeap(final long maxEntriesLocalHeap) {
        this.maxEntriesLocalHeap = maxEntriesLocalHeap;
    }

    /**
     * Reads the current capacity limit
     * @return the capacity limit
     */
    public long getMaxEntriesLocalHeap() {
        return maxEntriesLocalHeap;
    }
}
