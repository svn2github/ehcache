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

import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.PoolParticipant;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Abstract pool evictor which always evicts from the store consuming the most resources.
 *
 * @author Ludovic Orban
 * @author Alex Snaps
 */
public class FromLargestCachePoolEvictor implements PoolEvictor<PoolParticipant> {

    /**
     * {@inheritDoc}
     */
    public boolean freeSpace(Collection<PoolAccessor<PoolParticipant>> from, long bytes) {
        if (from == null || from.isEmpty()) {
            return false;
        }

        long remainingSizeInBytes = bytes;
        Collection<PoolAccessor<PoolParticipant>> tried = new ArrayList<PoolAccessor<PoolParticipant>>();

        while (tried.size() != from.size()) {
            PoolAccessor<PoolParticipant> largestPoolAccessor = findUntriedLargestPoolableStore(from, tried);

            long beforeEvictionSize = largestPoolAccessor.getSize();
            if (!largestPoolAccessor.getParticipant().evict(1, bytes)) {
                tried.add(largestPoolAccessor);
                continue;
            }
            long afterEvictionSize = largestPoolAccessor.getSize();

            remainingSizeInBytes -= (beforeEvictionSize - afterEvictionSize);
            if (remainingSizeInBytes <= 0L) {
                return true;
            }
        }

        return false;
    }

    private PoolAccessor<PoolParticipant> findUntriedLargestPoolableStore(Collection<PoolAccessor<PoolParticipant>> from,
                                                                          Collection<PoolAccessor<PoolParticipant>> tried) {
        PoolAccessor<PoolParticipant> largestPoolAccessor = null;
        for (PoolAccessor<PoolParticipant> accessor : from) {
            if (alreadyTried(tried, accessor)) {
                continue;
            }

            if (largestPoolAccessor == null || accessor.getSize() > largestPoolAccessor.getSize()) {
                largestPoolAccessor = accessor;
            }
        }
        return largestPoolAccessor;
    }

    private boolean alreadyTried(Collection<PoolAccessor<PoolParticipant>> tried, PoolAccessor<PoolParticipant> from) {
        for (PoolAccessor accessor : tried) {
            if (accessor == from) {
                return true;
            }
        }
        return false;
    }

}
