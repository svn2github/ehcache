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

package net.sf.ehcache.pool;

import java.util.Collection;

/**
 * Pools are used to track shared resource consumption. Each store participating in a pool creates an accessor
 * which it uses to tell the pool about its consumption. A SizeOf engine is used to calculate the size of the
 * objects added to the pool.
 *
 * @param <T> type of store that uses this pool
 *
 * @author Ludovic Orban
 */
public interface Pool<T> {

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
     * @param store the store which will use the created accessor.
     * @return a PoolAccessor whose consumption is tracked by this pool.
     */
    PoolAccessor<T> createPoolAccessor(T store);

    /**
     * Register an accessor implementation with this pool.
     *
     * @param accessor accessor to be registered
     */
    void registerPoolAccessor(PoolAccessor<? extends T> accessor);

    /**
     * Remove the supplied accessor from this pool.
     *
     * @param accessor accessor to be removed
     */
    void removePoolAccessor(PoolAccessor<?> accessor);

    /**
     * Return a PoolAccessor whose consumption is tracked by this pool, using a specific SizeOf engine.
     *
     * @param store the store which will use the created accessor.
     * @param sizeOfEngine the SizeOf engine used to measure the size of objects added through the created accessor.
     * @return a PoolAccessor whose consumption is tracked by this pool.
     */
    PoolAccessor<T> createPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine);

    /**
     * Return the stores accessing this pool.
     *
     * @return stores using this pool
     */
    Collection<T> getPoolableStores();

    /**
     * Return the pool evictor used by this pool.
     *
     * @return the pool evictor
     */
    PoolEvictor<T> getEvictor();

}
