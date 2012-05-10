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

package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.PoolableStore;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstract pool evictor which always evicts from the store consuming the most resources.
 *
 * @author Ludovic Orban
 */
public abstract class AbstractFromLargestCachePoolEvictor implements PoolEvictor<PoolableStore> {

    /**
     * {@inheritDoc}
     */
    public boolean freeSpace(Collection<PoolableStore> from, long bytes) {
        if (from == null || from.isEmpty()) {
            return false;
        }

        long remainingSizeInBytes = bytes;
        Collection<PoolableStore> tried = new ArrayList<PoolableStore>();

        while (tried.size() != from.size()) {
            PoolableStore largestPoolableStore = findUntriedLargestPoolableStore(from, tried);

            long beforeEvictionSize = getSizeInBytes(largestPoolableStore);
            if (!evict(1, bytes, largestPoolableStore)) {
                tried.add(largestPoolableStore);
                continue;
            }
            long afterEvictionSize = getSizeInBytes(largestPoolableStore);

            remainingSizeInBytes -= (beforeEvictionSize - afterEvictionSize);
            if (remainingSizeInBytes <= 0L) {
                return true;
            }
        }

        return false;
    }

    /**
     * Evict from a store for a chosen resource
     *
     * @param count the element count
     * @param bytes the bytes count
     * @param poolableStore the store
     * @return true if eviction succeeded, ie: if there was enough evictable resource held by the store
     */
    protected abstract boolean evict(int count, long bytes, PoolableStore poolableStore);

    /**
     * Get a store size in bytes for a chosen resource
     *
     * @param poolableStore the store
     * @return the size in bytes
     */
    protected abstract long getSizeInBytes(PoolableStore poolableStore);

    private PoolableStore findUntriedLargestPoolableStore(Collection<PoolableStore> from, Collection<PoolableStore> tried) {
        PoolableStore largestPoolableStore = null;
        for (PoolableStore poolableStore : from) {
            if (alreadyTried(tried, poolableStore)) {
                continue;
            }

            if (largestPoolableStore == null || getSizeInBytes(poolableStore) > getSizeInBytes(largestPoolableStore)) {
                largestPoolableStore = poolableStore;
            }
        }
        return largestPoolableStore;
    }

    private boolean alreadyTried(Collection<PoolableStore> tried, PoolableStore from) {
        for (PoolableStore poolableStore : tried) {
            if (poolableStore == from) {
                return true;
            }
        }
        return false;
    }

}
