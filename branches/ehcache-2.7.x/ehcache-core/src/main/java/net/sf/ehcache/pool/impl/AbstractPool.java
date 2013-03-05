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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolEvictor;
import net.sf.ehcache.pool.PoolParticipant;
import net.sf.ehcache.pool.SizeOfEngine;

/**
 * An abstract pool implementation.
 * <p>
 * This contains all the logic of a pool except for the actual creation of accessor instances.
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public abstract class AbstractPool implements Pool {

    private volatile long maximumPoolSize;
    private final PoolEvictor evictor;
    private final List<PoolAccessor> poolAccessors;
    private final List<PoolAccessor> poolAccessorsView;
    private final SizeOfEngine defaultSizeOfEngine;

    /**
     * Create an AbstractPool instance
     *
     * @param maximumPoolSize the maximum size of the pool, in bytes.
     * @param evictor the pool evictor, for cross-store eviction.
     * @param defaultSizeOfEngine the default SizeOf engine used by the accessors.
     */
    public AbstractPool(long maximumPoolSize, PoolEvictor evictor, SizeOfEngine defaultSizeOfEngine) {
        this.maximumPoolSize = maximumPoolSize;
        this.evictor = evictor;
        this.defaultSizeOfEngine = defaultSizeOfEngine;
        this.poolAccessors = new CopyOnWriteArrayList<PoolAccessor>();
        this.poolAccessorsView = Collections.unmodifiableList(poolAccessors);
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        long total = 0L;
        for (PoolAccessor poolAccessor : poolAccessors) {
            total += poolAccessor.getSize();
        }
        return total;
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxSize() {
        return maximumPoolSize;
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxSize(long newSize) {
        long oldSize = this.maximumPoolSize;
        this.maximumPoolSize = newSize;
        long sizeToEvict = oldSize - newSize;

        if (sizeToEvict > 0) {
            getEvictor().freeSpace(getPoolAccessors(), sizeToEvict);
        }
    }

    /**
     * {@inheritDoc}
     */
    public PoolAccessor createPoolAccessor(PoolParticipant participant, int maxDepth, boolean abortWhenMaxDepthExceeded) {
        return createPoolAccessor(participant, defaultSizeOfEngine.copyWith(maxDepth, abortWhenMaxDepthExceeded));
    }

    /**
     * {@inheritDoc}
     */
    public void registerPoolAccessor(PoolAccessor accessor) {
        poolAccessors.add(accessor);
    }

    /**
     * {@inheritDoc}
     */
    public void removePoolAccessor(PoolAccessor accessor) {
        poolAccessors.remove(accessor);
    }

    /**
     * {@inheritDoc}
     */
    public Collection<PoolAccessor> getPoolAccessors() {
        return poolAccessorsView;
    }

    /**
     * {@inheritDoc}
     */
    public PoolEvictor getEvictor() {
        return evictor;
    }

}
