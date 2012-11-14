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
 * Balanced access evictor that makes on-disk eviction decisions.
 *
 * @author Chris Dennis
 */
public class BalancedAccessOnDiskPoolEvictor extends AbstractBalancedAccessEvictor<PoolParticipant> {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean evict(PoolParticipant store, int count, long size) {
        return store.evictFromOnDisk(count, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long countSize(PoolParticipant store) {
        return store.getApproximateDiskCountSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long byteSize(PoolParticipant store) {
        return store.getApproximateDiskByteSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float hitRate(PoolParticipant store) {
        return store.getApproximateDiskHitRate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float missRate(PoolParticipant store) {
        return store.getApproximateDiskMissRate();
    }
}
