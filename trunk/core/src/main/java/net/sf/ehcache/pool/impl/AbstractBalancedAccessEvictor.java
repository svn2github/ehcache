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

package net.sf.ehcache.pool.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.sf.ehcache.pool.PoolEvictor;

/**
 * Abstract implementation of a global 'cache value' maximizing pool eviction algorithm.
 * <p>
 *
 * @author Chris Dennis
 *
 * @param <T> type of store handled by this evictor
 */
public abstract class AbstractBalancedAccessEvictor<T> implements PoolEvictor<T> {

    private static final double ALPHA = 1.0;
    private static final int SAMPLE_SIZE = 5;

    /**
     * Comparator used to rank the stores in order of eviction 'cost'.
     */
    private final class EvictionCostComparator implements Comparator<T> {

        private final long unloadedSize;
        private final Map<T, Float> evictionCostCache;

        public EvictionCostComparator(long unloadedSize, int collectionSize) {
            this.unloadedSize = unloadedSize;
            this.evictionCostCache = new IdentityHashMap<T, Float>(collectionSize);
        }

        public int compare(T s1, T s2) {
            Float f1 = evictionCostCache.get(s1);
            if (f1 == null) {
              f1 = evictionCost(s1, unloadedSize);
              evictionCostCache.put(s1, f1);
            }
            Float f2 = evictionCostCache.get(s2);
            if (f2 == null) {
              f2 = evictionCost(s2, unloadedSize);
              evictionCostCache.put(s2, f2);
            }
            return Float.compare(f1, f2);
        }
    }

    /**
     * Evict the specified number of bytes or the hinted number of elements from the specified store
     *
     * @param store store to evict from
     * @param count number of elements to evict
     * @param bytes number of bytes to evict
     * @return {@code true} if the eviction succeeded
     */
    protected abstract boolean evict(T store, int count, long bytes);

    /**
     * Return the hit rate for the supplied store.
     *
     * @param store store to query
     * @return hit rate
     */
    protected abstract float hitRate(T store);

    /**
     * Return the miss rate for the supplied store.
     *
     * @param store store to query
     * @return miss rate
     */
    protected abstract float missRate(T store);

    /**
     * Return the number of mappings in the supplied store.
     *
     * @param store store to size
     * @return mapping count
     */
    protected abstract long countSize(T store);

    /**
     * Return the size in bytes of the supplied store.
     *
     * @param store store to size
     * @return size in bytes
     */
    protected abstract long byteSize(T store);

    /**
     * {@inheritDoc}
     */
    public boolean freeSpace(Collection<T> from, long bytes) {
        if (from == null || from.isEmpty()) {
            return false;
        }
        List<T> random = new ArrayList<T>(from);
        Collections.shuffle(random);

        for (int i = 0; i < random.size(); i += SAMPLE_SIZE) {
            List<T> sorted = random.subList(i, Math.min(SAMPLE_SIZE + i, random.size()));
            Collections.sort(sorted, new EvictionCostComparator(getDesiredUnloadedSize(sorted), sorted.size() + 1));

            for (T store : sorted) {
                int count;
                long byteSize = byteSize(store);
                long countSize = countSize(store);
                if (countSize == 0 || byteSize == 0) {
                    count = 1;
                } else {
                    count = (int) Math.max((bytes * countSize) / byteSize, 1L);
                }
                if (evict(store, count, bytes)) {
                    return true;
                }
            }
        }

        return false;
    }

    private float evictionCost(T store, long unloadedSize) {
        /*
         * The code below is a simplified version of this:
         *
         * float meanEntrySize = byteSize / countSize;
         * float accessRate = hitRate + missRate;
         * float fillLevel = hitRate / accessRate;
         * float deltaFillLevel = fillLevel / byteSize;
         *
         * return meanEntrySize * accessRate * deltaFillLevel * hitDistributionFunction(fillLevel);
         */

        float hitRate = hitRate(store);
        float missRate = missRate(store);
        long countSize = countSize(store);
        float accessRate = hitRate + missRate;

        if (accessRate == 0.0f) {
            if (byteSize(store) > unloadedSize) {
                return Float.NEGATIVE_INFINITY;
            } else {
                return Float.POSITIVE_INFINITY;
            }
        } else if (hitRate == 0.0f) {
            return Float.POSITIVE_INFINITY;
        } else {
            float cost = (hitRate / countSize) * hitDistributionFunction(hitRate / accessRate);
            if (Float.isNaN(cost)) {
                throw new AssertionError(String.format("NaN Eviction Cost [hit:%f miss:%f size:%d]", hitRate, missRate, countSize));
            } else {
                return cost;
            }
        }
    }

    private static float hitDistributionFunction(float fillLevel) {
        return (float) Math.pow(fillLevel, -ALPHA);
    }

    private long getDesiredUnloadedSize(Collection<T> from) {
        long unloadedSize = 0L;
        for (T s : from) {
            unloadedSize += byteSize(s);
        }
        return unloadedSize / from.size();
    }
}
