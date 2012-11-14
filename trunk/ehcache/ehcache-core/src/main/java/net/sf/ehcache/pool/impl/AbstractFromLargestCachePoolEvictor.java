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
import net.sf.ehcache.pool.PoolParticipant;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstract pool evictor which always evicts from the store consuming the most resources.
 *
 * @author Ludovic Orban
 */
public abstract class AbstractFromLargestCachePoolEvictor implements PoolEvictor<PoolParticipant> {

    /**
     * {@inheritDoc}
     */
    public boolean freeSpace(Collection<PoolParticipant> from, long bytes) {
        if (from == null || from.isEmpty()) {
            return false;
        }

        long remainingSizeInBytes = bytes;
        Collection<PoolParticipant> tried = new ArrayList<PoolParticipant>();

        while (tried.size() != from.size()) {
            PoolParticipant largestPoolParticipant = findUntriedLargestPoolableStore(from, tried);

            long beforeEvictionSize = getSizeInBytes(largestPoolParticipant);
            if (!evict(1, bytes, largestPoolParticipant)) {
                tried.add(largestPoolParticipant);
                continue;
            }
            long afterEvictionSize = getSizeInBytes(largestPoolParticipant);

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
     * @param poolParticipant the store
     * @return true if eviction succeeded, ie: if there was enough evictable resource held by the store
     */
    protected abstract boolean evict(int count, long bytes, PoolParticipant poolParticipant);

    /**
     * Get a store size in bytes for a chosen resource
     *
     * @param poolParticipant the store
     * @return the size in bytes
     */
    protected abstract long getSizeInBytes(PoolParticipant poolParticipant);

    private PoolParticipant findUntriedLargestPoolableStore(Collection<PoolParticipant> from, Collection<PoolParticipant> tried) {
        PoolParticipant largestPoolParticipant = null;
        for (PoolParticipant poolParticipant : from) {
            if (alreadyTried(tried, poolParticipant)) {
                continue;
            }

            if (largestPoolParticipant == null || getSizeInBytes(poolParticipant) > getSizeInBytes(largestPoolParticipant)) {
                largestPoolParticipant = poolParticipant;
            }
        }
        return largestPoolParticipant;
    }

    private boolean alreadyTried(Collection<PoolParticipant> tried, PoolParticipant from) {
        for (PoolParticipant poolParticipant : tried) {
            if (poolParticipant == from) {
                return true;
            }
        }
        return false;
    }

}
