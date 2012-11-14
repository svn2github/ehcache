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

import net.sf.ehcache.pool.PoolParticipant;

/**
 * Pool evictor which always evicts from the store consuming the most disk resources.
 *
 * @author Ludovic Orban
 */
public class FromLargestCacheOnDiskPoolEvictor extends AbstractFromLargestCachePoolEvictor {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean evict(int count, long bytes, PoolParticipant largestPoolParticipant) {
        return largestPoolParticipant.evictFromOnDisk(count, bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long getSizeInBytes(PoolParticipant largestPoolParticipant) {
        return largestPoolParticipant.getOnDiskSizeInBytes();
    }

}
