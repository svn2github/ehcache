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

package net.sf.ehcache.pool;

import java.util.Collection;

/**
 * Pools are used to track shared resource consumption. Each store participating in a pool creates an accessor
 * which it uses to tell the pool about its consumption. A SizeOf engine is used to calculate the size of the
 * objects added to the pool.
 *
 * @author Ludovic Orban
 * @author Alex Snaps
 */
public interface Pool {

    /**
     * Return the used size of the pool.
     *
     * @return used size of the pool.
     */
    long getSize();

    /**
     * Return the maximum size of the pool.
     *
     * @return the maximum size of the pool.
     */
    long getMaxSize();

    /**
     * Change the maximum size of the pool.
     *
     * @param newSize the new pool size.
     */
    void setMaxSize(long newSize);

    /**
     * Return a PoolAccessor whose consumption is tracked by this pool, using a default SizeOf engine.
     *
     * @param participant the participant which will use the created accessor.
     * @param maxDepth maximum depth of the object graph to traverse
     * @param abortWhenMaxDepthExceeded true if the object traversal should be aborted when the max depth is exceeded
     * @return a PoolAccessor whose consumption is tracked by this pool.
     */
    public PoolAccessor createPoolAccessor(PoolParticipant participant, int maxDepth, boolean abortWhenMaxDepthExceeded);

    /**
     * Register an accessor implementation with this pool.
     *
     * @param accessor accessor to be registered
     */
    void registerPoolAccessor(PoolAccessor accessor);

    /**
     * Remove the supplied accessor from this pool.
     *
     * @param accessor accessor to be removed
     */
    void removePoolAccessor(PoolAccessor accessor);

    /**
     * Return a PoolAccessor whose consumption is tracked by this pool, using a specific SizeOf engine.
     *
     * @param participant the participant which will use the created accessor.
     * @param sizeOfEngine the SizeOf engine used to measure the size of objects added through the created accessor.
     * @return a PoolAccessor whose consumption is tracked by this pool.
     */
    PoolAccessor createPoolAccessor(PoolParticipant participant, SizeOfEngine sizeOfEngine);

    /**
     * Return the participants accessing this pool.
     *
     * @return participants using this pool
     */
    Collection<PoolAccessor> getPoolAccessors();

    /**
     * Return the pool evictor used by this pool.
     *
     * @return the pool evictor
     */
    PoolEvictor getEvictor();

}
