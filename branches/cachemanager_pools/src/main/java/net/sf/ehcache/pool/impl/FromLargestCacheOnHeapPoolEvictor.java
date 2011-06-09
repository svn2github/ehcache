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

import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.PoolableStore;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Pool evictor which always evicts from the store consuming the most resources.
 *
 * @author Ludovic Orban
 */
public class FromLargestCacheOnHeapPoolEvictor implements PoolEvictor<PoolableStore> {

    /**
     * {@inheritDoc}
     */
    public boolean freeSpace(Collection<PoolableStore> from, long bytes) {
        if (from == null || from.isEmpty()) {
            return false;
        }

        long remainingSizeInBytes = bytes;
        Collection<PoolableStore> tried = new ArrayList<PoolableStore>();

        while (true) {
            // if all stores have been tried, give up
            if (tried.size() == from.size()) {
                return false;
            }

            PoolableStore largestPoolableStore = null;

            for (PoolableStore poolableStore : from) {
                if (alreadyTried(tried, poolableStore)) {
                    continue;
                }

                if (largestPoolableStore == null || poolableStore.getInMemorySizeInBytes() > largestPoolableStore.getInMemorySizeInBytes()) {
                    largestPoolableStore = poolableStore;
                }
            } // for

            long beforeEvictionSize = largestPoolableStore.getInMemorySizeInBytes();
            if (!largestPoolableStore.evictFromOnHeap(1, bytes)) {
                tried.add(largestPoolableStore);
                continue;
            }
            long afterEvictionSize = largestPoolableStore.getInMemorySizeInBytes();

            remainingSizeInBytes -= (beforeEvictionSize - afterEvictionSize);
            if (remainingSizeInBytes <= 0L) {
                return true;
            }
        } // while
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
